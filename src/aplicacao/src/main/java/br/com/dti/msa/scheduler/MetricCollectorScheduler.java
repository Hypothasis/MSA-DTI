package br.com.dti.msa.scheduler;

import br.com.dti.msa.integration.zabbix.dto.ZabbixClient;
import br.com.dti.msa.integration.zabbix.dto.ZabbixEventDTO;
import br.com.dti.msa.model.Host;
import br.com.dti.msa.model.Metric;
import br.com.dti.msa.model.MetricHistory;
import br.com.dti.msa.model.RecentEvents;
import br.com.dti.msa.repository.HostRepository;
import br.com.dti.msa.repository.MetricHistoryRepository;
import br.com.dti.msa.repository.RecentEventsRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
public class MetricCollectorScheduler {

    @Autowired private HostRepository hostRepository;
    @Autowired private MetricHistoryRepository metricHistoryRepository;
    @Autowired private ZabbixClient zabbixClient;
    @Autowired private RecentEventsRepository recentEventsRepository;

    @Transactional
    public void collectAllMetrics() {
        System.out.println("--- INICIANDO COLETA E ANÁLISE DE STATUS: " + LocalDateTime.now() + " ---");

        List<Host> hostsToMonitor = hostRepository.findAllWithMetrics();
        List<MetricHistory> historyBatch = new ArrayList<>();
        List<RecentEvents> eventsBatch = new ArrayList<>(); // Use a single batch for events

        for (Host host : hostsToMonitor) {
            System.out.println("Coletando para o host: " + host.getName());

            Map<String, String> allZabbixItems = zabbixClient.getAllItemValuesForHost(host.getZabbixId().longValue());

            if (allZabbixItems.isEmpty()) {
                System.err.println("  > Não foi possível obter itens do Zabbix para o host " + host.getName());
            }

            // --- COLETA DE MÉTRICAS ---
            for (Metric metric : host.getMetrics()) {
                String zabbixKey = metric.getZabbixKey();
                if (zabbixKey == null || zabbixKey.equalsIgnoreCase("zabbix_api")) {
                    continue; // Pula métricas especiais como "eventos-recentes"
                }
                
                String rawValue = allZabbixItems.get(zabbixKey); 

                if (rawValue != null) {
                    // TENTA CONVERTER O VALOR BRUTO PARA NÚMERO
                    try {
                        Double value = Double.parseDouble(rawValue);
                        // Se for um número, salva no histórico
                        MetricHistory historyRecord = new MetricHistory(host, metric, LocalDateTime.now(), value);
                        historyBatch.add(historyRecord);
                        System.out.println("  > Métrica NUMÉRICA '" + metric.getName() + "' (chave " + zabbixKey + ") coletada: " + value);
                    } catch (NumberFormatException e) {
                        System.out.println("  > Métrica de TEXTO '" + metric.getName() + "' (chave " + zabbixKey + ") detectada: " + rawValue);
                    }
                } else {
                    System.err.println("  > Falha ao coletar métrica '" + metric.getName() + "' com chave '" + zabbixKey + "' (valor não encontrado na resposta do Zabbix).");
                }
            }

            // DETERMINA O NOVO STATUS DO HOST (agora passando o mapa de Strings)
            Host.HostStatus newStatus = determineHostStatus(host, allZabbixItems);
            host.setStatus(newStatus);
            System.out.println("  > Status do host '" + host.getName() + "' definido para: " + newStatus);;

            // LÓGICA PARA COLETAR EVENTOS
            boolean shouldCollectEvents = host.getMetrics().stream()
                .anyMatch(metric -> metric.getMetricKey().equals("eventos-recentes"));

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
     * Método auxiliar que agora recebe um Mapa de Strings.
     */
    private Host.HostStatus determineHostStatus(Host host, Map<String, String> zabbixMetrics) {
        // Regra 1: INATIVO
        Optional<Metric> availabilityMetric = host.getMetrics().stream()
                .filter(m -> m.getMetricKey().equals("disponibilidade-global"))
                .findFirst();
        
        if (availabilityMetric.isPresent()) {
            // Converte o valor de disponibilidade (que é "0.0" ou "1.0") para Double
            Double availabilityValue = parseDouble(zabbixMetrics.get(availabilityMetric.get().getZabbixKey()));
            if (availabilityValue != null && availabilityValue == 0.0) {
                return Host.HostStatus.INACTIVE; // Host está inacessível
            }
        } else {
            return Host.HostStatus.INACTIVE; // Se não monitora disponibilidade, marca como inativo
        }

        // Regra 2: ALERTA (CPU)
        Optional<Metric> cpuMetric = host.getMetrics().stream()
                .filter(m -> m.getMetricKey().equals("cpu-uso"))
                .findFirst();
        if (cpuMetric.isPresent()) {
            Double cpuValue = parseDouble(zabbixMetrics.get(cpuMetric.get().getZabbixKey()));
            if (cpuValue != null && cpuValue > 90.0) {
                return Host.HostStatus.ALERT; // CPU acima de 90%
            }
        }
        
        // Regra 3: ALERTA (Memória)
        Optional<Metric> memTotalMetric = host.getMetrics().stream().filter(m -> m.getMetricKey().equals("memoria-ram-total")).findFirst();
        Optional<Metric> memAvailableMetric = host.getMetrics().stream().filter(m -> m.getMetricKey().equals("memoria-ram-disponivel")).findFirst();
        
        if (memTotalMetric.isPresent() && memAvailableMetric.isPresent()) {
            Double total = parseDouble(zabbixMetrics.get(memTotalMetric.get().getZabbixKey()));
            Double available = parseDouble(zabbixMetrics.get(memAvailableMetric.get().getZabbixKey()));
            
            if (total != null && available != null && total > 0) {
                double percentFree = (available / total) * 100;
                if (percentFree < 10.0) {
                    return Host.HostStatus.ALERT; // Menos de 10% de RAM livre
                }
            }
        }

        return Host.HostStatus.ACTIVE; // Se passou por tudo, está OK
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

}