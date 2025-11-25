package br.com.dti.msa.scheduler;

import br.com.dti.msa.integration.zabbix.dto.ZabbixClient;
import br.com.dti.msa.integration.zabbix.dto.ZabbixEventDTO;
import br.com.dti.msa.model.Host;
import br.com.dti.msa.model.HostMetricConfig;
import br.com.dti.msa.model.Metric;
import br.com.dti.msa.model.MetricCurrentValue;
import br.com.dti.msa.model.MetricHistory;
import br.com.dti.msa.model.RecentEvents;
import br.com.dti.msa.repository.HostRepository;
import br.com.dti.msa.repository.MetricCurrentValueRepository;
import br.com.dti.msa.repository.MetricHistoryRepository;
import br.com.dti.msa.repository.RecentEventsRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class MetricCollectorScheduler {

    @Autowired private HostRepository hostRepository;
    @Autowired private MetricHistoryRepository metricHistoryRepository;
    @Autowired private MetricCurrentValueRepository metricCurrentValueRepository; // Adicione o repositório de valores de texto
    @Autowired private ZabbixClient zabbixClient;
    @Autowired private RecentEventsRepository recentEventsRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();

    public static class StatusResult {
        final Host.HostStatus status;
        final String description;

        public StatusResult(Host.HostStatus status, String description) {
            this.status = status;
            this.description = description;
        }
    }

    @Transactional
    @Scheduled(fixedRate = 60000) // Executa a cada 60 segundos
    public void collectAllMetrics() {
        System.out.println("--- INICIANDO COLETA E ANÁLISE DE STATUS: " + LocalDateTime.now() + " ---");

        List<Host> hostsToMonitor = hostRepository.findAllWithMetrics();
        List<MetricHistory> historyBatch = new ArrayList<>();
        
        for (Host host : hostsToMonitor) {
            System.out.println("Coletando para o host: " + host.getName());
            Map<String, String> collectedItems = new HashMap<>();
            Set<HostMetricConfig> configs = host.getMetricConfigs();

            System.out.println("Todas as metricas configuradas: " + host.getMetricConfigs().stream().map(c -> c.getMetric().getMetricKey()).collect(Collectors.joining(", ")));

            for (HostMetricConfig config : configs) {
                Metric metric = config.getMetric();
                String zabbixKey = config.getZabbixKey();
                String metricKey = metric.getMetricKey();
                
                if (zabbixKey == null || zabbixKey.equalsIgnoreCase("zabbix_api")) continue; 
                
                String rawValue = zabbixClient.getSingleItemValue(host.getZabbixId(), zabbixKey); 

                if (rawValue != null) {
                    collectedItems.put(zabbixKey, rawValue);              
                    
                    Double numericValue = null;
                    boolean isText = false;

                    try {
                        // 1. Tenta converter para número (Ping, CPU, RAM)
                        numericValue = Double.parseDouble(rawValue);
                    } catch (NumberFormatException e) {
                        isText = true;
                        
                        // 2. Se falhar, verifica se é Health Check (JSON)
                        if (isJsonHealthMetric(metricKey)) {
                            numericValue = parseHealthCheckJson(rawValue);
                            isText = (numericValue == null); // Se parseou, vira número
                        }
                        // 3. Verifica se é HTTP Header (Texto)
                        else if (isHttpHeaderMetric(metricKey)) {
                            numericValue = parseHttpHeader(rawValue);
                            isText = (numericValue == null); // Se parseou, vira número
                        }
                    }

                    // 4. Salva o dado
                    if (!isText) {
                        MetricHistory historyRecord = new MetricHistory(host, metric, LocalDateTime.now(), numericValue);
                        historyBatch.add(historyRecord);
                        System.out.println("  > Métrica NUMÉRICA '" + metric.getName() + "' salva no histórico: " + numericValue);
                    } else {
                        System.out.println("  > Métrica de TEXTO '" + metric.getName() + "' detectada. Salvando valor atual.");
                        saveOrUpdateCurrentTextValue(host, metric, rawValue);
                    }
                } else {
                    System.err.println("  > Falha ao coletar métrica '" + metric.getName() + "' com chave '" + zabbixKey + "'.");
                }
            }

            // DETERMINA STATUS E SALVA
            StatusResult result = determineHostStatus(host, collectedItems);
            host.setStatus(result.status);
            host.setStatusDescription(result.description);
            System.out.println("  > Status: " + result.status + " (" + result.description + ")");

            // COLETA DE EVENTOS
            collectEventsForHost(host, configs);
        }

        if (!historyBatch.isEmpty()) {
            metricHistoryRepository.saveAll(historyBatch);
            System.out.println(historyBatch.size() + " registros de histórico salvos.");
        }
        System.out.println("--- COLETA FINALIZADA ---");
    }

    // ===================================================================
    // MÉTODOS DE DETERMINAÇÃO DE STATUS
    // ===================================================================
    StatusResult determineHostStatus(Host host, Map<String, String> collectedZabbixMetrics) {
        Set<HostMetricConfig> configs = host.getMetricConfigs();

        // 1.1 Verifica Health Check (JSON)
        Optional<HostMetricConfig> jsonConfig = configs.stream().filter(c -> isJsonHealthMetric(c.getMetric().getMetricKey())).findFirst();
        if (jsonConfig.isPresent()) {
            return evaluateJsonHealthStatus(jsonConfig.get(), collectedZabbixMetrics, host, configs);
        }

        // 1.2 Verifica HTTP Header Check (Texto)
        Optional<HostMetricConfig> headerConfig = configs.stream().filter(c -> isHttpHeaderMetric(c.getMetric().getMetricKey())).findFirst();
        if (headerConfig.isPresent()) {
            return evaluateHttpHeaderStatus(headerConfig.get(), collectedZabbixMetrics);
        }

        // 2. Verifica Ping Padrão
        Optional<HostMetricConfig> pingConfig = configs.stream().filter(c -> c.getMetric().getMetricKey().equals("disponibilidade-global")).findFirst();
        if (pingConfig.isPresent()) {
            String zabbixKey = pingConfig.get().getZabbixKey();
            Double val = parseDouble(collectedZabbixMetrics.get(zabbixKey));
            if (val == null || val == 0.0) {
                return new StatusResult(Host.HostStatus.INACTIVE, "Host parado! (Ping falhou)");
            }
        } else {
             // Se não tem nenhuma métrica de disponibilidade, assume INACTIVE se não houver CPU
             if (configs.stream().noneMatch(c -> c.getMetric().getMetricKey().equals("cpu-uso"))) {
                 return new StatusResult(Host.HostStatus.INACTIVE, "Host parado! (Sem métrica de disponibilidade)"); 
             }
        }

        // 3. Checa Recursos
        return checkResourceAlerts(host, configs, collectedZabbixMetrics);
    }
    
    /**
     * CORRIGIDO: Método auxiliar que agora retorna a 'StatusResult'
     */
    private StatusResult checkResourceAlerts(Host host, Set<HostMetricConfig> configs, Map<String, String> zabbixMetrics) {
        
        // Alerta de CPU
        Optional<HostMetricConfig> cpuConfig = configs.stream()
                .filter(c -> c.getMetric().getMetricKey().equals("cpu-uso"))
                .findFirst();
        if (cpuConfig.isPresent()) {
            String zabbixKey = cpuConfig.get().getZabbixKey();
            Double cpuValue = parseDouble(zabbixMetrics.get(zabbixKey));
            if (cpuValue != null && cpuValue > 90.0) {
                return new StatusResult(Host.HostStatus.ALERT, String.format(java.util.Locale.US, "Host com alto consumo de CPU (%.1f%%)", cpuValue));
            }
        }
        
        // Alerta de Memória RAM
        Optional<HostMetricConfig> memTotalConfig = configs.stream().filter(c -> c.getMetric().getMetricKey().equals("memoria-ram-total")).findFirst();
        Optional<HostMetricConfig> memAvailableConfig = configs.stream().filter(c -> c.getMetric().getMetricKey().equals("memoria-ram-disponivel")).findFirst();
        
        if (memTotalConfig.isPresent() && memAvailableConfig.isPresent()) {
            String totalKey = memTotalConfig.get().getZabbixKey();
            String availableKey = memAvailableConfig.get().getZabbixKey();
            
            Double total = parseDouble(zabbixMetrics.get(totalKey));
            Double available = parseDouble(zabbixMetrics.get(availableKey));
            
            if (total != null && available != null && total > 0) {
                double percentFree = (available / total) * 100;
                if (percentFree < 10.0) {
                     return new StatusResult(Host.HostStatus.ALERT, String.format(java.util.Locale.US, "Host com alto consumo de RAM (%.1f%% livre)", percentFree));
                }
            }
        }

        // Se passou por tudo, está OK
        return new StatusResult(Host.HostStatus.ACTIVE, "Tudo certo com o Host.");
    }

    /**
     * Salva ou atualiza o último valor de uma métrica de texto (não numérica)
     * na tabela 'metric_current_value'.
     *
     * @param host O host ao qual o valor pertence.
     * @param metric A métrica "conceitual" (ex: 'os-nome').
     * @param rawValue O valor de texto vindo do Zabbix (ex: "Linux...").
     */
    private void saveOrUpdateCurrentTextValue(Host host, Metric metric, String rawValue) {
        // 1. Procura se já existe um valor salvo para esta combinação de host/métrica
        MetricCurrentValue currentValue = metricCurrentValueRepository
            .findByHostIdAndMetricId(host.getId(), metric.getId())
            .orElse(new MetricCurrentValue()); // 2. Se não existir, cria um novo objeto

        // 3. Atualiza os dados do objeto
        currentValue.setHost(host);
        currentValue.setMetric(metric);
        currentValue.setCurrentValue(rawValue);
        currentValue.setLastUpdated(LocalDateTime.now());
        
        // 4. Salva no banco (INSERT se for novo, UPDATE se já existia)
        metricCurrentValueRepository.save(currentValue);
    }

    /**
     * método auxiliar para converter String para Double de forma segura,
     * tratando valores nulos ou não-numéricos.
     */
    private Double parseDouble(String rawValue) {
        if (rawValue == null) return null;
        try {
            return Double.parseDouble(rawValue);
        } catch (NumberFormatException e) {
            return null; // Retorna nulo se não for um número
        }
    }

    /**
     * Verifica se uma métrica é do tipo Health Check (JSON).
     */
    private boolean isHealthCheckMetric(String metricKey) {
        return metricKey.equals("disponibilidade-global-health") ||
               metricKey.equals("disponibilidade-especifica-health") ||
               metricKey.equals("disponibilidade-global-http-agente") ||
               metricKey.equals("disponibilidade-especifica-http-agente");
    }

    /**
     * Tenta parsear um JSON de Health Check (ex: {"status":"UP"})
     * e o converte para 1.0 (UP) ou 0.0 (DOWN).
     */
    private Double parseHealthCheckJson(String rawJson) {
        try {
            JsonNode root = objectMapper.readTree(rawJson);
            String status = root.path("status").asText();

            if (status.equalsIgnoreCase("UP")) {
                return 1.0;
            } else {
                return 0.0; // Qualquer coisa que não seja "UP" é considerado 0 (DOWN)
            }
        } catch (JsonProcessingException e) {
            System.err.println("  > Falha ao parsear JSON de Health Check: " + rawJson);
            return null; // Retorna nulo se o JSON for inválido
        }
    }

    // --- MÉTODOS DE AVALIAÇÃO ESPECÍFICOS ---

    /**
     * Avalia métricas que retornam JSON (Ex: {"status":"UP"})
     */
    private StatusResult evaluateJsonHealthStatus(HostMetricConfig config, Map<String, String> metrics, Host host, Set<HostMetricConfig> allConfigs) {
        String zabbixKey = config.getZabbixKey();
        String rawJson = metrics.get(zabbixKey);

        if (rawJson == null) return new StatusResult(Host.HostStatus.INACTIVE, "Host parado! (Sem dados do Health Check)");

        try {
            JsonNode root = objectMapper.readTree(rawJson);
            String status = root.path("status").asText();

            if (status.equalsIgnoreCase("UP")) {
                JsonNode dbStatus = root.path("deps").path("db");
                if (dbStatus.isMissingNode() || dbStatus.asText().equalsIgnoreCase("UP")) {
                    // Se JSON está OK, ainda verificamos CPU/RAM
                    return checkResourceAlerts(host, allConfigs, metrics);
                } else {
                    return new StatusResult(Host.HostStatus.ALERT, "Alerta: Aplicação UP, Banco de Dados DOWN.");
                }
            }
            return new StatusResult(Host.HostStatus.INACTIVE, "Host parado! (Status: " + status + ")");
        } catch (Exception e) {
            return new StatusResult(Host.HostStatus.INACTIVE, "Host parado! (JSON inválido)");
        }
    }

    /**
     * Avalia métricas que retornam Cabeçalhos HTTP (Texto Bruto)
     * Ex: "HTTP/1.1 200 OK ..."
     */
    private StatusResult evaluateHttpHeaderStatus(HostMetricConfig config, Map<String, String> metrics) {
        String zabbixKey = config.getZabbixKey();
        String rawHeaders = metrics.get(zabbixKey);

        if (rawHeaders == null) {
            return new StatusResult(Host.HostStatus.INACTIVE, "Host parado! (Sem resposta HTTP)");
        }

        // Verifica se contém o código 200 OK
        // (Pode ser aprimorado para aceitar 2xx)
        if (rawHeaders.contains("200 OK") || rawHeaders.contains("201 Created")) {
            // Se HTTP está OK, não checamos recursos aqui (ou poderíamos chamar checkResourceAlerts se quiséssemos)
            // Para simplificar, se o HTTP responde 200, consideramos OK.
            return new StatusResult(Host.HostStatus.ACTIVE, "Serviço HTTP respondendo (200 OK).");
        } 
        
        // Se retornou 404, 500, 403, etc.
        return new StatusResult(Host.HostStatus.INACTIVE, "Host parado! (Resposta HTTP inválida: " + getFirstLine(rawHeaders) + ")");
    }

    // --- MÉTODOS AUXILIARES ---

    private boolean isJsonHealthMetric(String key) {
        // Lista APENAS as métricas que retornam JSON
        return key.equals("disponibilidade-global-health") ||
               key.equals("disponibilidade-especifica-health");
    }

    private boolean isHttpHeaderMetric(String key) {
        // Lista APENAS as métricas que retornam Texto/Headers
        return key.equals("disponibilidade-global-http-agente") ||
               key.equals("disponibilidade-especifica-http-agente");
    }
    
    private String getFirstLine(String text) {
        if (text == null) return "";
        int idx = text.indexOf('\n');
        return idx > -1 ? text.substring(0, idx).trim() : text;
    }

    // ===================================================================
    // MÉTODOS DE PARSING E AUXILIARES
    // ===================================================================

    private Double parseHttpHeader(String rawHeaders) {
        if (rawHeaders == null) return null;
        return (rawHeaders.contains("200 OK") || rawHeaders.contains("201 Created")) ? 1.0 : 0.0;
    }

    private void collectEventsForHost(Host host, Set<HostMetricConfig> configs) {
        if (configs.stream().anyMatch(c -> c.getMetric().getMetricKey().equals("eventos-recentes"))) {
             recentEventsRepository.deleteByHostId(host.getId());
             List<ZabbixEventDTO> events = zabbixClient.getRecentEvents(host.getZabbixId());
             // ... (salva eventos) ...
             if (!events.isEmpty()) {
                 List<RecentEvents> toSave = events.stream().map(e -> {
                     RecentEvents re = new RecentEvents();
                     re.setHost(host);
                     re.setTimestamp(LocalDateTime.ofInstant(Instant.ofEpochSecond(e.getClock()), ZoneId.systemDefault()));
                     re.setSeverity(String.valueOf(e.getSeverity()));
                     re.setName(e.getName());
                     return re;
                 }).collect(Collectors.toList());
                 recentEventsRepository.saveAll(toSave);
             }
        }
    }
}