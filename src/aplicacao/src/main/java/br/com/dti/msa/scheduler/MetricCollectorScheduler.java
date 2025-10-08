package br.com.dti.msa.scheduler;

import br.com.dti.msa.integration.zabbix.dto.ZabbixClient;
import br.com.dti.msa.model.Host;
import br.com.dti.msa.model.Metric;
import br.com.dti.msa.model.MetricHistory;
import br.com.dti.msa.repository.HostRepository;
import br.com.dti.msa.repository.MetricHistoryRepository;
import br.com.dti.msa.service.MetricCatalog;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
public class MetricCollectorScheduler {

    @Autowired private HostRepository hostRepository;
    @Autowired private MetricHistoryRepository metricHistoryRepository;
    @Autowired private ZabbixClient zabbixClient;
    @Autowired private MetricCatalog metricCatalog;

    // Executa a cada 1 minuto. 60000 milissegundos = 1 minuto.
    @Scheduled(fixedRate = 60000)
    public void collectAllMetrics() {
        System.out.println("--- INICIANDO COLETA DE MÉTRICAS OTIMIZADA: " + LocalDateTime.now() + " ---");

        List<Host> hostsToMonitor = hostRepository.findAllWithMetrics();
        List<MetricHistory> historyBatch = new ArrayList<>();

        for (Host host : hostsToMonitor) {
            System.out.println("Coletando para o host: " + host.getName());

            // FAZ UMA ÚNICA CHAMADA À API para buscar todos os itens do host
            Map<String, Double> allZabbixItems = zabbixClient.getAllItemValuesForHost(host.getZabbixId().longValue());

            if (allZabbixItems.isEmpty()) {
                System.err.println("  > Não foi possível obter itens do Zabbix para o host " + host.getName());
                continue; // Pula para o próximo host
            }

            // Itera sobre as métricas que estão CONFIGURADAS no MSA para este host
            for (Metric metric : host.getMetrics()) {
                
                // Obtém as chaves Zabbix que o MSA espera para esta métrica
                List<String> expectedZabbixKeys = metricCatalog.getZabbixKeysFor(metric.getMetricKey());
                
                for (String expectedKey : expectedZabbixKeys) {
                    
                    // VERIFICA se o valor para a chave esperada foi retornado pela API
                    if (allZabbixItems.containsKey(expectedKey)) {
                        Double value = allZabbixItems.get(expectedKey);
                        
                        // Cria o registro de histórico e adiciona ao lote
                        MetricHistory historyRecord = new MetricHistory();
                        historyRecord.setHost(host);
                        historyRecord.setMetric(metric);
                        historyRecord.setTimestamp(LocalDateTime.now());
                        historyRecord.setValue(value);
                        
                        historyBatch.add(historyRecord);
                        System.out.println("  > Métrica '" + metric.getName() + "' (chave " + expectedKey + ") coletada: " + value);
                    } else {
                        System.err.println("  > Chave esperada '" + expectedKey + "' para a métrica '" + metric.getName() + "' não encontrada nos itens do host no Zabbix.");
                    }
                }
            }
        }

        if (!historyBatch.isEmpty()) {
            metricHistoryRepository.saveAll(historyBatch);
            System.out.println(historyBatch.size() + " registros de histórico salvos no banco.");
        }

        System.out.println("--- COLETA DE MÉTRICAS FINALIZADA ---");
    }
}