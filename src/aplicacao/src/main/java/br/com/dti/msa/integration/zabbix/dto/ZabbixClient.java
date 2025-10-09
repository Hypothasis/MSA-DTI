package br.com.dti.msa.integration.zabbix.dto;

import org.springframework.beans.factory.annotation.Value;
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
     * Verifica se um host existe no Zabbix usando a API host.get.
     */
    public boolean hostExists(Long zabbixId) {
        System.out.println("VALIDANDO no Zabbix se o host com ID " + zabbixId + " existe...");
        
        // Parâmetros para a chamada: queremos apenas saber a contagem de hosts com este ID.
        Map<String, Object> params = Map.of(
            "hostids", zabbixId,
            "countOutput", true // Pede para a API retornar apenas o número de resultados
        );

        // Monta o corpo da requisição
        ZabbixRequestDTO request = new ZabbixRequestDTO("host.get", params, authToken, 1);

        try {
            // Faz a chamada POST para a API
            ZabbixCountResponseDTO response = restTemplate.postForObject(zabbixApiUrl, request, ZabbixCountResponseDTO.class);
            
            // Retorna true se a contagem for maior que 0
            return response != null && response.getResult() > 0;
        } catch (RestClientException e) {
            System.err.println("Erro ao conectar com a API do Zabbix: " + e.getMessage());
            return false;
        }
    }

    /**
     * Verifica se um item (métrica) com uma chave específica existe em um host.
     */
    public boolean itemExistsOnHost(Long zabbixId, String itemKey) {
        System.out.println("VALIDANDO no Zabbix se o item '" + itemKey + "' existe no host " + zabbixId);
        
        // Parâmetros para a chamada: filtra por hostid e busca pela chave exata.
        Map<String, Object> params = Map.of(
            "hostids", zabbixId,
            "search", Map.of("key_", itemKey), // 'search' permite buscar por um valor exato na chave ('key_')
            "countOutput", true
        );

        // Monta o corpo da requisição
        ZabbixRequestDTO request = new ZabbixRequestDTO("item.get", params, authToken, 1);

        try {
            ZabbixCountResponseDTO response = restTemplate.postForObject(zabbixApiUrl, request, ZabbixCountResponseDTO.class);
            
            // Retorna true se encontrou pelo menos 1 item com aquela chave
            return response != null && response.getResult() > 0;
        } catch (RestClientException e) {
            System.err.println("Erro ao conectar com a API do Zabbix: " + e.getMessage());
            return false;
        }
    }

    /**
     * Busca o último valor numérico de um item (métrica) em um host específico.
     */
    public Double getItemValue(Long zabbixHostId, String itemKey) {
        System.out.println("Buscando valor para o item '" + itemKey + "' no host " + zabbixHostId);
        
        Map<String, Object> params = Map.of(
            "hostids", zabbixHostId,
            "output", new String[]{"itemid", "name", "key_", "lastvalue"},
            "search", Map.of("key_", itemKey),
            "limit", 1
        );
        ZabbixRequestDTO request = new ZabbixRequestDTO("item.get", params, authToken, 3);

        try {
            // 1. Recebe a resposta como uma String bruta
            String jsonResponse = restTemplate.postForObject(zabbixApiUrl, request, String.class);
            if (jsonResponse == null) {
                System.err.println("  > A resposta da API do Zabbix foi nula para o item '" + itemKey + "'.");
                return null;
            }

            // 2. Analisa o JSON
            JsonNode rootNode = objectMapper.readTree(jsonResponse);

            // 3. VERIFICA SE HÁ UM ERRO na resposta do Zabbix
            if (rootNode.has("error")) {
                String errorMessage = rootNode.get("error").get("data").asText();
                System.err.println("Erro da API Zabbix ao buscar item '" + itemKey + "': " + errorMessage);
                return null;
            }

            // 4. Se não houver erro, converte o resultado para o DTO
            ZabbixItemResponseDTO[] response = objectMapper.treeToValue(rootNode.get("result"), ZabbixItemResponseDTO[].class);

            // 5. Continua com a lógica original
            if (response != null && response.length > 0 && response[0].getLastValue() != null && !response[0].getLastValue().isEmpty()) {
                return Double.parseDouble(response[0].getLastValue());
            } else {
                System.err.println("  > Item '" + itemKey + "' não encontrado ou sem valor no host " + zabbixHostId);
                return null;
            }
        } catch (Exception e) {
            System.err.println("Erro crítico ao processar resposta para o item '" + itemKey + "': " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Busca TODOS os itens e seus últimos valores para um host específico.
     * Retorna um Mapa de [chave_do_item -> valor].
     */
    public Map<String, Double> getAllItemValuesForHost(Long zabbixHostId) {
        System.out.println("Buscando TODOS os itens para o host " + zabbixHostId);
        
        Map<String, Object> params = Map.of(
            "hostids", zabbixHostId,
            "output", new String[]{"key_", "lastvalue"}
        );
        ZabbixRequestDTO request = new ZabbixRequestDTO("item.get", params, authToken, 4);

        try {
            String jsonResponse = restTemplate.postForObject(zabbixApiUrl, request, String.class);
            if (jsonResponse == null) {
                throw new IllegalStateException("A resposta da API do Zabbix foi nula.");
            }
            
            JsonNode rootNode = objectMapper.readTree(jsonResponse);

            if (rootNode.has("error")) {
                // Imprime o erro detalhado vindo do Zabbix
                String errorMessage = rootNode.get("error").get("data").asText();
                throw new ZabbixApiException("Erro da API Zabbix: " + errorMessage);
            }

            JsonNode resultNode = rootNode.get("result");
            ZabbixItem[] items = objectMapper.treeToValue(resultNode, ZabbixItem[].class);

            return Arrays.stream(items)
                .filter(item -> item.getLastValue() != null && !item.getLastValue().isEmpty())
                .map(item -> {
                    try {
                        // Tenta converter o valor para Double
                        Double value = Double.parseDouble(item.getLastValue());
                        // Retorna um par simples [chave, valor]
                        return Map.entry(item.getKey(), value);
                    } catch (NumberFormatException e) {
                        // Se a conversão falhar, retorna null
                        return null;
                    }
                })
                // Filtra todos os pares que falharam na conversão (ou seja, que retornaram null)
                .filter(entry -> entry != null)
                // Coleta o resultado final em um mapa, lidando com chaves duplicadas
                .collect(Collectors.toMap(
                    Map.Entry::getKey,
                    Map.Entry::getValue,
                    (existingValue, newValue) -> existingValue // Mantém o valor existente se houver chaves duplicadas
                ));

        } catch (JsonProcessingException e) {
            // Este erro acontece se o JSON retornado for inválido
            System.err.println("Erro de parsing do JSON para o host " + zabbixHostId + ": " + e.getMessage());
        } catch (RestClientException | ZabbixApiException e) {
            // Este erro pega falhas de conexão ou erros retornados pela API
            System.err.println("Erro ao buscar todos os itens para o host " + zabbixHostId + ": " + e.getMessage());
        }
        return Map.of();
    }

    /**
     * Busca os 5 eventos (problemas) mais recentes de um host.
     */
    public List<ZabbixEventDTO> getRecentEvents(Long zabbixHostId) {
        System.out.println("Buscando 5 eventos recentes para o host " + zabbixHostId);

        Map<String, Object> params = Map.of(
            "hostids", zabbixHostId,
            "output", "extend",
            "selectHosts", "extend",
            "sortfield", new String[]{"clock"}, // Ordena pelo timestamp
            "sortorder", "DESC", // Do mais recente para o mais antigo
            "limit", 5, // Limita a 5 resultados
            "value", 1 // Apenas eventos que estão no estado de "PROBLEMA"
        );

        ZabbixRequestDTO request = new ZabbixRequestDTO("event.get", params, authToken, 5);

        try {
            String jsonResponse = restTemplate.postForObject(zabbixApiUrl, request, String.class);
            if (jsonResponse == null) return Collections.emptyList();

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
     * Faz uma chamada simples à API para verificar a conexão e autenticação.
     * Lança uma exceção se a conexão falhar.
     */
    public void testConnection() {
        System.out.println("--- INICIANDO TESTE DE CONEXÃO COM A API DO ZABBIX ---");
        
        List<Object> params = List.of();

        ZabbixRequestDTO request = new ZabbixRequestDTO("apiinfo.version", params, null, 1);

        try {
            ResponseEntity<String> response = restTemplate.postForEntity(zabbixApiUrl, request, String.class);
            
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null && response.getBody().contains("\"result\"")) {
                System.out.println("Teste de conexão com o Zabbix bem-sucedido! Versão da API: " + response.getBody());
            } else {
                throw new RuntimeException("Resposta inválida da API do Zabbix: " + response.getBody());
            }
        } catch (RestClientException e) {
            System.err.println("FALHA no teste de conexão com a API do Zabbix: " + e.getMessage());
            throw new RuntimeException("Não foi possível conectar à API do Zabbix. Verifique a URL.", e);
        }
    }
}