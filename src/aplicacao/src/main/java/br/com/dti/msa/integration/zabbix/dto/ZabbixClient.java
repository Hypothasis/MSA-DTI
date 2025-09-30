package br.com.dti.msa.integration.zabbix.dto;

import br.com.dti.msa.integration.zabbix.dto.ZabbixCountResponseDTO;
import br.com.dti.msa.integration.zabbix.dto.ZabbixRequestDTO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.RestClientException;

import java.util.Map;

@Component
public class ZabbixClient {

    // 1. Injeta as propriedades do application.properties
    @Value("${zabbix.api.url}")
    private String zabbixApiUrl;

    @Value("${zabbix.api.token}")
    private String authToken;

    // 2. Cria uma instância do RestTemplate para fazer as chamadas HTTP
    private final RestTemplate restTemplate = new RestTemplate();

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
}