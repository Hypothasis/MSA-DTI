package br.com.dti.msa.integration.zabbix.dto;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import br.com.dti.msa.exception.ZabbixApiException;
import lombok.Data;

import org.springframework.web.client.RestClientException;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;



@Component
public class ZabbixClient {
    
    @JsonIgnoreProperties(ignoreUnknown = true)
    @Data
    static class ZabbixItem {
        @JsonProperty("key_")
        private String key;
        
        @JsonProperty("lastvalue")
        private String lastValue;
    }

    // Injeta as propriedades do application.properties
    @Value("${zabbix.api.url}")
    private String zabbixApiUrl;

    @Value("${zabbix.api.token}")
    private String authToken;

    // Cria uma instância do RestTemplate para fazer as chamadas HTTP
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final RestTemplate restTemplate;

    @Autowired
    public ZabbixClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    /**
     * MÉTODO CENTRAL: Envia a requisição para a API do Zabbix com o Bearer Token.
     */
    private String sendRequest(ZabbixRequestDTO requestPayload) throws RestClientException {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));
        headers.setBearerAuth(authToken);

        // Converte o corpo manualmente para JSON puro
        Map<String, Object> requestMap = new HashMap<>();
        requestMap.put("jsonrpc", "2.0");
        requestMap.put("method", requestPayload.getMethod());
        requestMap.put("params", requestPayload.getParams());
        requestMap.put("id", requestPayload.getId());

        try {
            String jsonBody = objectMapper.writeValueAsString(requestMap); // garante JSON válido

            HttpEntity<String> requestEntity = new HttpEntity<>(jsonBody, headers);

            ResponseEntity<String> response = restTemplate.postForEntity(zabbixApiUrl, requestEntity, String.class);
            return response.getBody();

        } catch (JsonProcessingException e) {
            throw new RuntimeException("Erro ao serializar JSON para o Zabbix", e);
        }
    }

    /**
     * Verifica se um host existe no Zabbix usando a API host.get.
     */
    public boolean hostExists(Long zabbixId) {
        System.out.println("VALIDANDO no Zabbix se o host com ID " + zabbixId + " existe...");
        Map<String, Object> params = Map.of("hostids", zabbixId, "countOutput", true);
        ZabbixRequestDTO request = new ZabbixRequestDTO("host.get", params, 1);
        try {
            String jsonResponse = sendRequest(request);
            JsonNode rootNode = objectMapper.readTree(jsonResponse);
            
            if (rootNode.has("error")) {
                System.err.println("Erro da API Zabbix ao verificar host: " + rootNode.get("error").get("data").asText());
                return false;
            }
            return rootNode.get("result").asInt() > 0;
        } catch (Exception e) {
            System.err.println("Erro crítico ao verificar host " + zabbixId + ": " + e.getMessage());
            return false;
        }
    }

    /**
     * Verifica se um item (métrica) com uma chave específica existe em um host.
     */
    public boolean itemExistsOnHost(Long zabbixId, String itemKey) {
        System.out.println("VALIDANDO no Zabbix se o item '" + itemKey + "' existe no host " + zabbixId);
        Map<String, Object> params = Map.of("hostids", zabbixId, "search", Map.of("key_", itemKey), "countOutput", true);
        ZabbixRequestDTO request = new ZabbixRequestDTO("item.get", params, 2);
        try {
            String jsonResponse = sendRequest(request);
            JsonNode rootNode = objectMapper.readTree(jsonResponse);
            if (rootNode.has("error")) {
                System.err.println("Erro da API Zabbix ao verificar item '" + itemKey + "': " + rootNode.get("error").get("data").asText());
                return false;
            }
            return rootNode.get("result").asInt() > 0;
        } catch (Exception e) {
            System.err.println("Erro crítico ao verificar item '" + itemKey + "' no host " + zabbixId + ": " + e.getMessage());
            return false;
        }
    }

    /**
     * Busca o último valor numérico de um item (métrica) em um host específico.
     */
    public Double getItemValue(Long zabbixHostId, String itemKey) {
        Map<String, Object> params = Map.of(
            "hostids", zabbixHostId, "output", new String[]{"key_", "lastvalue"},
            "search", Map.of("key_", itemKey), "limit", 1
        );
        ZabbixRequestDTO request = new ZabbixRequestDTO("item.get", params, 3);
        try {
            String jsonResponse = sendRequest(request);
            JsonNode rootNode = objectMapper.readTree(jsonResponse);
            if (rootNode.has("error")) {
                System.err.println("Erro da API Zabbix ao buscar valor para '" + itemKey + "': " + rootNode.get("error").get("data").asText());
                return null;
            }
            ZabbixItem[] items = objectMapper.treeToValue(rootNode.get("result"), ZabbixItem[].class);
            if (items != null && items.length > 0 && items[0].getLastValue() != null) {
                return Double.parseDouble(items[0].getLastValue());
            }
            return null;
        } catch (Exception e) {
            System.err.println("Erro crítico ao buscar valor do item '" + itemKey + "': " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Busca TODOS os itens e seus últimos valores para um host específico.
     * Retorna um Mapa de [chave_do_item -> valor].
     */
    public Map<String, Double> getAllItemValuesForHost(Long zabbixHostId) {
        System.out.println("Buscando TODOS os itens para o host " + zabbixHostId);
        Map<String, Object> params = Map.of("hostids", zabbixHostId, "output", new String[]{"key_", "lastvalue"});
        ZabbixRequestDTO request = new ZabbixRequestDTO("item.get", params, 4);

        try {
            String jsonResponse = sendRequest(request);
            JsonNode resultNode = objectMapper.readTree(jsonResponse).get("result");
            ZabbixItem[] items = objectMapper.treeToValue(resultNode, ZabbixItem[].class);

            return Arrays.stream(items)
                .filter(item -> item.getLastValue() != null && !item.getLastValue().isEmpty())
                .collect(Collectors.toMap(
                    ZabbixItem::getKey,
                    item -> {
                        try {
                            return Double.parseDouble(item.getLastValue());
                        } catch (NumberFormatException e) {
                            return null;
                        }
                    },
                    (val1, val2) -> val1 // Em caso de chaves duplicadas
                ))
                .entrySet().stream()
                .filter(entry -> entry.getValue() != null)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        } catch (Exception e) {
            System.err.println("Erro ao buscar todos os itens para o host " + zabbixHostId + ": " + e.getMessage());
            return Map.of();
        }
    }

    /**
     * Busca os 5 eventos (problemas) mais recentes de um host.
     */
    public List<ZabbixEventDTO> getRecentEvents(Long zabbixHostId) {
        Map<String, Object> params = Map.of(
            "hostids", zabbixHostId, "output", "extend", "selectHosts", "extend",
            "sortfield", new String[]{"clock"}, "sortorder", "DESC", "limit", 5, "value", 1
        );
        ZabbixRequestDTO request = new ZabbixRequestDTO("event.get", params, 5);
        try {
            String jsonResponse = sendRequest(request);
            JsonNode rootNode = objectMapper.readTree(jsonResponse);
            if (rootNode.has("error")) {
                System.err.println("Erro da API Zabbix ao buscar eventos: " + rootNode.get("error").get("data").asText());
                return Collections.emptyList();
            }
            ZabbixEventDTO[] events = objectMapper.treeToValue(rootNode.get("result"), ZabbixEventDTO[].class);
            return Arrays.asList(events);
        } catch (Exception e) {
            System.err.println("Erro crítico ao buscar eventos para o host " + zabbixHostId + ": " + e.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * Faz uma chamada simples à API para verificar a conexão.
     * Lança uma exceção se a conexão falhar.
     */ 
    public void testConnection() throws Exception {
        System.out.println("--- INICIANDO TESTE DE CONEXÃO COM A API DO ZABBIX ---");
        System.out.println("URL da API do Zabbix: " + zabbixApiUrl);

        // Monta o JSON exatamente como JSON Puro
        String jsonBody = """
            {
            "jsonrpc": "2.0",
            "method": "apiinfo.version",
            "params": [],
            "id": 99
            }
            """;

        System.out.println("JSON enviado: " + jsonBody);

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setAccept(List.of(MediaType.APPLICATION_JSON));

            HttpEntity<String> entity = new HttpEntity<>(jsonBody, headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    zabbixApiUrl,
                    HttpMethod.POST,
                    entity,
                    String.class
            );

            System.out.println("Resposta completa: " + response);

            if (response.getStatusCode().is2xxSuccessful() &&
                response.getBody() != null &&
                response.getBody().contains("\"result\"")) {
                System.out.println("✅ Teste de conexão bem-sucedido: " + response.getBody());
            } else {
                throw new RuntimeException("❌ Resposta inválida da API do Zabbix: " +
                        (response.getBody() != null ? response.getBody() : "Corpo nulo"));
            }

        } catch (Exception e) {
            System.err.println("FALHA no teste de conexão com o Zabbix: " + e.getMessage());
            throw new RuntimeException("Não foi possível conectar à API do Zabbix. Verifique a URL.", e);
        }
    }

}