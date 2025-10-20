package br.com.dti.msa.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.*;
import com.fasterxml.jackson.databind.JsonNode; // Dependência Jackson é necessária

import br.com.dti.msa.dto.ZabbixHealthCheckResponse;

@Service
public class ZabbixConnectionTesterService {

    private static final Logger log = LoggerFactory.getLogger(ZabbixConnectionTesterService.class);

    @Value("${zabbix.api.url}")
    private String zabbixApiUrl;

    private final RestTemplate restTemplate;

    public ZabbixConnectionTesterService() {
        this.restTemplate = new RestTemplate(); // Para simplicidade. O ideal é injetar como um Bean.
    }

    public ZabbixHealthCheckResponse testConnection() {
        try {
            log.info("Iniciando teste de conexão em tempo real com o Zabbix na URL: {}", zabbixApiUrl);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            // Payload simples para a chamada 'apiinfo.version' do Zabbix
            String requestBody = """
            {
            "jsonrpc": "2.0",
            "method": "apiinfo.version",
            "params": [],
            "id": 99
            }
            """;

            HttpEntity<String> requestEntity = new HttpEntity<>(requestBody, headers);

            // Executa a chamada POST
            ResponseEntity<JsonNode> response = restTemplate.postForEntity(zabbixApiUrl, requestEntity, JsonNode.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null && response.getBody().has("result")) {
                String zabbixVersion = response.getBody().get("result").asText();
                log.info("Conexão com Zabbix bem-sucedida. Versão: {}", zabbixVersion);
                return new ZabbixHealthCheckResponse("OK", "Conexão com o Zabbix estabelecida com sucesso.", zabbixVersion);
            } else {
                log.error("Resposta inesperada do Zabbix. Código: {}, Corpo: {}", response.getStatusCode(), response.getBody());
                return new ZabbixHealthCheckResponse("ERROR", "Resposta inesperada do servidor Zabbix: " + response.getStatusCode());
            }

        } catch (Exception e) {
            // Captura qualquer erro de conexão, timeout, DNS, etc.
            log.error("Falha ao conectar com o Zabbix: {}", e.getMessage());
            return new ZabbixHealthCheckResponse("ERROR", "Não foi possível conectar ao Zabbix: " + e.getMessage());
        }
    }
}