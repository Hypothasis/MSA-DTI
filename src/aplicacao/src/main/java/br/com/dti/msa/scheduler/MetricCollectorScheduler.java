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

@Component
public class MetricCollectorScheduler {

    @Autowired private HostRepository hostRepository;
    @Autowired private MetricHistoryRepository metricHistoryRepository;
    @Autowired private ZabbixClient zabbixClient;
    @Autowired private RecentEventsRepository recentEventsRepository;

    @Scheduled(fixedRate = 60000)
    @Transactional
    public void collectAllMetrics() {
        System.out.println("--- INICIANDO COLETA DE MÉTRICAS: " + LocalDateTime.now() + " ---");

        List<Host> hostsToMonitor = hostRepository.findAllWithMetrics();
        List<MetricHistory> historyBatch = new ArrayList<>();

        for (Host host : hostsToMonitor) {
            System.out.println("Coletando para o host: " + host.getName());

            // --- COLETA DE MÉTRICAS NUMÉRICAS ---
            for (Metric metric : host.getMetrics()) {
                String zabbixKey = metric.getZabbixKey();
                if (zabbixKey == null || zabbixKey.equalsIgnoreCase("zabbix_api")) {
                    continue;
                }
                Double value = zabbixClient.getItemValue(host.getZabbixId().longValue(), zabbixKey);
                if (value != null) {
                    MetricHistory historyRecord = new MetricHistory(host, metric, LocalDateTime.now(), value);
                    historyBatch.add(historyRecord);
                    System.out.println("  > Métrica '" + metric.getName() + "' (chave " + zabbixKey + ") coletada: " + value);
                } else {
                    System.err.println("  > Falha ao coletar métrica '" + metric.getName() + "' com chave '" + zabbixKey + "'");
                }
            }

            // ===================================================================
            // LÓGICA PARA COLETAR EVENTOS (ADICIONE/VERIFIQUE ESTE BLOCO)
            // ===================================================================
            boolean shouldCollectEvents = host.getMetrics().stream()
                .anyMatch(metric -> metric.getMetricKey().equals("eventos-recentes"));

            if (shouldCollectEvents) {
                // Deleta os eventos antigos deste host para inserir os novos
                recentEventsRepository.deleteByHostId(host.getId());

                // Busca os 5 eventos mais recentes na API do Zabbix
                List<ZabbixEventDTO> zabbixEvents = zabbixClient.getRecentEvents(host.getZabbixId().longValue());

                List<RecentEvents> eventsBatchForHost = new ArrayList<>();
                for (ZabbixEventDTO zabbixEvent : zabbixEvents) {
                    RecentEvents newEvent = new RecentEvents();
                    newEvent.setHost(host);
                    newEvent.setTimestamp(LocalDateTime.ofInstant(Instant.ofEpochSecond(zabbixEvent.getClock()), ZoneId.systemDefault()));
                    newEvent.setSeverity(String.valueOf(zabbixEvent.getSeverity()));
                    newEvent.setName(zabbixEvent.getName());
                    eventsBatchForHost.add(newEvent);
                }

                // Salva os novos eventos
                if (!eventsBatchForHost.isEmpty()) {
                    recentEventsRepository.saveAll(eventsBatchForHost);
                    System.out.println("  > " + eventsBatchForHost.size() + " eventos recentes salvos.");
                }
            }
        }

        // Salva o lote de métricas numéricas
        if (!historyBatch.isEmpty()) {
            metricHistoryRepository.saveAll(historyBatch);
            System.out.println(historyBatch.size() + " registros de histórico salvos no banco.");
        }

        System.out.println("--- COLETA DE MÉTRICAS FINALIZADA ---");
    }
}