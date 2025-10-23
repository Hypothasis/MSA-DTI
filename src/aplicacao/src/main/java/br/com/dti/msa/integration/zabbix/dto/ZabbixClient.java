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
    private String sendRequest(ZabbixRequestDTO requestPayload)
            throws RestClientException, ZabbixApiException {

        // ===== 1️⃣ Validação local da estrutura =====
        if (requestPayload == null) {
            throw new ZabbixApiException("Payload nulo: o corpo da requisição não pode ser vazio.");
        }

        if (requestPayload.getMethod() == null || requestPayload.getMethod().isBlank()) {
            throw new ZabbixApiException("Campo 'method' ausente no payload Zabbix.");
        }

        if (requestPayload.getJsonrpc() == null || !requestPayload.getJsonrpc().equals("2.0")) {
            throw new ZabbixApiException("Campo 'jsonrpc' inválido ou ausente (deve ser '2.0').");
        }

        if (requestPayload.getId() <= 0) {
            throw new ZabbixApiException("Campo 'id' deve ser um número positivo.");
        }

        // ===== 2️⃣ Serialização manual do JSON =====
        String jsonBody;
        try {
            jsonBody = objectMapper.writeValueAsString(requestPayload);
        } catch (Exception e) {
            throw new ZabbixApiException("Falha ao serializar o payload em JSON: " + e.getMessage());
        }

        // ===== 3️⃣ Validação sintática do JSON gerado =====
        try {
            objectMapper.readTree(jsonBody); // tenta parsear o próprio JSON — se falhar, é inválido
        } catch (Exception e) {
            throw new ZabbixApiException("JSON gerado é inválido: " + e.getMessage() + "\nJSON: " + jsonBody);
        }

        // ===== 4️⃣ Cabeçalhos estritos =====
        HttpHeaders headers = new HttpHeaders();
        headers.set("Content-Type", "application/json"); // sem charset
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));
        headers.setBearerAuth(authToken);

        HttpEntity<String> requestEntity = new HttpEntity<>(jsonBody, headers);

        System.out.println("🔹 Enviando requisição ao Zabbix...");
        System.out.println("URL: " + zabbixApiUrl);
        System.out.println("JSON enviado: " + jsonBody);

        try {
            // ===== 5️⃣ Envio da requisição =====
            ResponseEntity<String> response = restTemplate.exchange(
                    zabbixApiUrl,
                    HttpMethod.POST,
                    requestEntity,
                    String.class
            );

            String jsonResponse = response.getBody();
            System.out.println("🔹 Resposta do Zabbix (" + response.getStatusCode() + "): " + jsonResponse);

            // ===== 6️⃣ Tratamento de erro no retorno =====
            if (jsonResponse == null || jsonResponse.isBlank()) {
                throw new ZabbixApiException("Resposta vazia da API Zabbix.");
            }

            JsonNode rootNode = objectMapper.readTree(jsonResponse);

            if (rootNode.has("error")) {
                String errorData = rootNode.path("error").path("data").asText("");
                String errorMessage = rootNode.path("error").path("message").asText("Erro desconhecido");
                throw new ZabbixApiException("Erro da API Zabbix: " + errorMessage + " - " + errorData);
            }

            if (!rootNode.has("result")) {
                throw new ZabbixApiException("Resposta inválida da API Zabbix: campo 'result' ausente.\nCorpo: " + jsonResponse);
            }

            return jsonResponse;

        } catch (RestClientException e) {
            System.err.println("❌ Erro de comunicação com o Zabbix: " + e.getMessage());
            throw new ZabbixApiException("Falha ao conectar à API do Zabbix: " + e.getMessage(), e);

        } catch (ZabbixApiException e) {
            // Repassa exceções tratadas internamente
            throw e;

        } catch (Exception e) {
            // Fallback genérico
            System.err.println("❌ Erro inesperado ao enviar requisição: " + e.getMessage());
            throw new ZabbixApiException("Erro inesperado ao processar requisição Zabbix: " + e.getMessage(), e);
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