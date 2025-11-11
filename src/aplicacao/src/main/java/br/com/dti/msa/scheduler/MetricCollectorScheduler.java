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

    @Transactional
    @Scheduled(fixedRate = 60000) // Executa a cada 60 segundos
    public void collectAllMetrics() {
        System.out.println("--- INICIANDO COLETA E ANÁLISE DE STATUS: " + LocalDateTime.now() + " ---");

        // O 'findAllWithMetrics' do repositório precisa ser atualizado para 'findAllWithMetricConfigs'
        // Por enquanto, 'findAll()' funciona pois a transação está aberta.
        List<Host> hostsToMonitor = hostRepository.findAll();
        List<MetricHistory> historyBatch = new ArrayList<>();
        List<RecentEvents> eventsBatch = new ArrayList<>();

        for (Host host : hostsToMonitor) {
            System.out.println("Coletando para o host: " + host.getName());

            // Inicializa o mapa para ESTE host
            Map<String, String> collectedItems = new HashMap<>();

            // Itera sobre as CONFIGURAÇÕES, não sobre as Métricas
            Set<HostMetricConfig> configs = host.getMetricConfigs();

            // --- COLETA DE MÉTRICAS (LÓGICA N+1) ---
            for (HostMetricConfig config : configs) {
                Metric metric = config.getMetric(); // Pega o "conceito" (ex: 'cpu-uso')
                String zabbixKey = config.getZabbixKey(); // Pega a chave Zabbix (ex: 'system.cpu.util')
                
                if (zabbixKey == null || zabbixKey.equalsIgnoreCase("zabbix_api")) {
                    continue; 
                }
                
                // FAZ UMA CHAMADA DE API PARA CADA MÉTRICA
                String rawValue = zabbixClient.getSingleItemValue(host.getZabbixId().longValue(), zabbixKey); 

                if (rawValue != null) {
                    // Salva o valor bruto (String) no mapa para usar no 'determineHostStatus'
                    collectedItems.put(zabbixKey, rawValue); // Salva para o 'determineHostStatus'                    
                    
                    Double numericValue = null;
                    boolean isText = false;

                    // Tenta salvar o valor no histórico se for numérico
                    try {
                        // 1. Tenta converter para número (ex: "13.5")
                        numericValue = Double.parseDouble(rawValue);
                        
                    } catch (NumberFormatException e) {
                        // 2. Se falhar, é TEXTO ou JSON.
                        isText = true;
                        
                        // 3. Verifica se é uma métrica de Health Check (JSON)
                        if (isHealthCheckMetric(metric.getMetricKey())) {
                            numericValue = parseHealthCheckJson(rawValue); // Tenta extrair 1.0 ou 0.0 do JSON
                            isText = (numericValue == null); // Se não conseguiu extrair, trata como texto
                        }
                    }

                    // 4. Salva o dado no local correto
                    if (!isText) {
                        // É um número (CPU, RAM, ou Health Check 'UP'/'DOWN')
                        MetricHistory historyRecord = new MetricHistory(host, metric, LocalDateTime.now(), numericValue);
                        historyBatch.add(historyRecord);
                        System.out.println("  > Métrica NUMÉRICA '" + metric.getName() + "' salva no histórico: " + numericValue);
                    } else {
                        // É um texto puro (ex: "Linux...")
                        System.out.println("  > Métrica de TEXTO '" + metric.getName() + "' detectada. Salvando valor atual: " + rawValue);
                        saveOrUpdateCurrentTextValue(host, metric, rawValue);
                    }
                } else {
                    System.err.println("  > Falha ao coletar métrica '" + metric.getName() + "' com chave '" + zabbixKey + "'.");
                }
            }

            // DETERMINA O NOVO STATUS DO HOST
            Host.HostStatus newStatus = determineHostStatus(host, collectedItems);
            host.setStatus(newStatus);
            System.out.println("  > Status do host '" + host.getName() + "' definido para: " + newStatus);

            // LÓGICA PARA COLETAR EVENTOS
            // CORREÇÃO: Verifica as configs
            boolean shouldCollectEvents = configs.stream()
                .anyMatch(config -> config.getMetric().getMetricKey().equals("eventos-recentes"));

            if (shouldCollectEvents) {
                // Deleta os eventos antigos deste host para inserir os novos
                recentEventsRepository.deleteByHostId(host.getId());

                List<ZabbixEventDTO> zabbixEvents = zabbixClient.getRecentEvents(host.getZabbixId().longValue());
                for (ZabbixEventDTO zabbixEvent : zabbixEvents) {
                    RecentEvents newEvent = new RecentEvents();
                    newEvent.setHost(host);
                    newEvent.setTimestamp(LocalDateTime.ofInstant(Instant.ofEpochSecond(zabbixEvent.getClock()), ZoneId.systemDefault()));
                    newEvent.setSeverity(String.valueOf(zabbixEvent.getSeverity()));
                    newEvent.setName(zabbixEvent.getName());
                    eventsBatch.add(newEvent);
                }
                if (!zabbixEvents.isEmpty()) {
                System.out.println("  > " + zabbixEvents.size() + " eventos recentes coletados.");
                }
            }
        }

        // Salva todos os lotes no banco de dados DE UMA SÓ VEZ
        if (!historyBatch.isEmpty()) {
            metricHistoryRepository.saveAll(historyBatch);
            System.out.println(historyBatch.size() + " registros de histórico salvos no banco.");
        }
        
        if (!eventsBatch.isEmpty()) {
            recentEventsRepository.saveAll(eventsBatch);
            System.out.println(eventsBatch.size() + " eventos recentes salvos no banco.");
        }

        System.out.println("--- COLETA E ANÁLISE FINALIZADA ---");
    }

    /**
     * Método auxiliar que agora entende os 3 tipos de disponibilidade.
     */
    private Host.HostStatus determineHostStatus(Host host, Map<String, String> collectedZabbixMetrics) {
        // Pega o conjunto de configurações do host UMA VEZ
        Set<HostMetricConfig> configs = host.getMetricConfigs();

        // ===================================================================
        // REGRA 1: CHECAGEM DE DISPONIBILIDADE (A Regra de Ouro)
        // ===================================================================
        
        // Tenta encontrar a métrica de disponibilidade prioritária (health ou HTTP)
        Optional<HostMetricConfig> customAvailability = configs.stream()
            .filter(c -> c.getMetric().getMetricKey().equals("disponibilidade-global-health") ||
                         c.getMetric().getMetricKey().equals("disponibilidade-global-http-agente"))
            .findFirst();

        if (customAvailability.isPresent()) {
            // --- LÓGICA DE HEALTH CHECK (JSON) ---
            String zabbixKey = customAvailability.get().getZabbixKey();
            String rawJson = collectedZabbixMetrics.get(zabbixKey);

            if (rawJson == null) {
                return Host.HostStatus.INACTIVE; // Falha ao coletar o JSON
            }
            try {
                JsonNode root = objectMapper.readTree(rawJson);
                String status = root.path("status").asText();

                if (status.equals("UP")) {
                    // Aplicação está UP. Verificamos as dependências.
                    JsonNode dbStatus = root.path("deps").path("db");
                    // Se o nó 'db' não existir OU se existir e for "UP", está tudo OK.
                    if (dbStatus.isMissingNode() || dbStatus.asText().equals("UP")) {
                         // A checagem de CPU/Memória (Regra 2) vai rodar
                        return checkResourceAlerts(host, configs, collectedZabbixMetrics);
                    } else {
                        // A aplicação está UP, mas o banco (deps) está DOWN.
                        return Host.HostStatus.ALERT; 
                    }
                } else {
                    // O JSON retornou status "DOWN" ou qualquer outra coisa
                    return Host.HostStatus.INACTIVE;
                }
            } catch (Exception e) {
                System.err.println("Erro ao parsear JSON de health check para o host " + host.getName() + ": " + rawJson);
                return Host.HostStatus.INACTIVE; // JSON inválido é um problema crítico
            }
            
        } else {
            // --- LÓGICA DE PING (Fallback) ---
            Optional<HostMetricConfig> defaultAvailability = configs.stream()
                    .filter(c -> c.getMetric().getMetricKey().equals("disponibilidade-global"))
                    .findFirst();
            
            if (defaultAvailability.isPresent()) {
                String zabbixKey = defaultAvailability.get().getZabbixKey();
                Double availabilityValue = parseDouble(collectedZabbixMetrics.get(zabbixKey));
                
                if (availabilityValue == null || availabilityValue == 0.0) {
                    return Host.HostStatus.INACTIVE; // Host está inacessível
                }
                // Se o ping for 1.0 (OK), passamos para a Regra 2
            } else {
                // Se NENHUMA métrica de disponibilidade estiver configurada, marca como inativo
                return Host.HostStatus.INACTIVE; 
            }
        }

        // ===================================================================
        // REGRA 2: CHECAGEM DE ALERTAS DE RECURSO
        // ===================================================================
        // Se chegamos aqui, o host é considerado "Acessível" (via JSON ou Ping)
        // Agora, verificamos se ele está sobrecarregado.
        return checkResourceAlerts(host, configs, collectedZabbixMetrics);
    }
    
    /**
     * Novo método auxiliar para checar CPU e Memória (Regras 2 e 3).
     * Só é chamado se o host for considerado ATIVO pela Regra 1.
     */
    private Host.HostStatus checkResourceAlerts(Host host, Set<HostMetricConfig> configs, Map<String, String> zabbixMetrics) {
        
        // Alerta de CPU (acima de 90%)
        Optional<HostMetricConfig> cpuConfig = configs.stream()
                .filter(c -> c.getMetric().getMetricKey().equals("cpu-uso"))
                .findFirst();
        if (cpuConfig.isPresent()) {
            String zabbixKey = cpuConfig.get().getZabbixKey();
            Double cpuValue = parseDouble(zabbixMetrics.get(zabbixKey));
            if (cpuValue != null && cpuValue > 90.0) {
                return Host.HostStatus.ALERT; // CPU acima de 90%
            }
        }
        
        // Alerta de Memória RAM (disponível abaixo de 10%)
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
                    return Host.HostStatus.ALERT; // Menos de 10% de RAM livre
                }
            }
        }

        // Se passou por todas as checagens de alerta, o host está ATIVO
        return Host.HostStatus.ACTIVE;
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
}