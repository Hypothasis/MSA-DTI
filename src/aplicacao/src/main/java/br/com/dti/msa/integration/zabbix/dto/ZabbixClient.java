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
    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * MÉTODO CENTRAL: Envia a requisição para a API do Zabbix com o Bearer Token.
     */
    private String sendRequest(ZabbixRequestDTO requestPayload) throws RestClientException, ZabbixApiException, JsonProcessingException {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(authToken); // Adiciona 'Authorization: Bearer seu_token'

        // Envia o DTO diretamente. O RestTemplate cuidará da serialização.
        HttpEntity<ZabbixRequestDTO> requestEntity = new HttpEntity<>(requestPayload, headers);
        
        ResponseEntity<String> response = restTemplate.postForEntity(zabbixApiUrl, requestEntity, String.class);
        String jsonResponse = response.getBody();

        if (jsonResponse != null) {
            JsonNode rootNode = objectMapper.readTree(jsonResponse);
            if (rootNode.has("error")) {
                 String errorMessage = rootNode.path("error").path("data").asText("Erro desconhecido na API Zabbix");
                 throw new ZabbixApiException("Erro da API Zabbix: " + errorMessage);
            }
        }
        return jsonResponse;
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
     * Busca o último valor de UM item (métrica) específico.
     * Esta é a chamada N+1 que o Scheduler usará.
     */
    public String getSingleItemValue(Long zabbixHostId, String itemKey) {
                
        Map<String, Object> params = Map.of(
            "hostids", zabbixHostId,
            "output", new String[]{"key_", "lastvalue"},
            "search", Map.of("key_", itemKey), // 'search' funciona para uma única chave
            "limit", 1
        );
        ZabbixRequestDTO request = new ZabbixRequestDTO("item.get", params, 3);

        try {
            String jsonResponse = sendRequest(request); // Usa o método de envio com Bearer Token
            
            JsonNode resultNode = objectMapper.readTree(jsonResponse).get("result");
            if (resultNode == null || !resultNode.isArray() || resultNode.isEmpty()) {
                System.err.println("  > Item '" + itemKey + "' não encontrado no host " + zabbixHostId);
                return null;
            }

            ZabbixItem item = objectMapper.treeToValue(resultNode.get(0), ZabbixItem.class);
            return item.getLastValue();

        } catch (Exception e) {
            System.err.println("Erro crítico ao buscar valor do item '" + itemKey + "': " + e.getMessage());
            return null;
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