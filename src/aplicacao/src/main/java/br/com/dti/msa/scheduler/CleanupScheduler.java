package br.com.dti.msa.scheduler;

import br.com.dti.msa.model.MetricHistory;
import br.com.dti.msa.repository.MetricHistoryRepository;
import br.com.dti.msa.repository.ZabbixConnectionStatusRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import java.time.LocalDateTime;
import java.util.Optional;

@Component
public class CleanupScheduler {

    @Autowired private MetricHistoryRepository metricHistoryRepository;
    @Autowired private ZabbixConnectionStatusRepository zabbixStatusRepository;

    // Define o número máximo de registros a manter por métrica/host
    private static final int MAX_RECORDS_PER_METRIC = 2880;

    // Executa a cada hora, no minuto 0.
    // Ex: "0 0 4 * * ?" para rodar todo dia às 4 da manhã.
    @Scheduled(cron = "0 0 * * * ?") 
    public void cleanupOldData() {
        System.out.println("--- INICIANDO JOB DE LIMPEZA DE DADOS ANTIGOS ---");

        // --- TAREFA 1: Limpar Histórico de Métricas (Limpeza Rápida por Tempo) ---
        LocalDateTime cutoffDate = LocalDateTime.now().minusHours(48);
        System.out.println("Apagando registros de histórico de métricas anteriores a: " + cutoffDate);
        metricHistoryRepository.deleteOlderThan(cutoffDate);

        // --- TAREFA 2: Limpar Logs de Conexão (Limpeza Rápida por Tempo) ---
        LocalDateTime statusCutoffDate = LocalDateTime.now().minusHours(48);
        System.out.println("Apagando registros de status de conexão anteriores a: " + statusCutoffDate);
        zabbixStatusRepository.deleteOlderThan(statusCutoffDate);

        // --- TAREFA 3: Limpar Excesso de Registros (Garantia de Quantidade) ---
        System.out.println("Garantindo que nenhuma métrica tenha mais que " + MAX_RECORDS_PER_METRIC + " registros...");
        metricHistoryRepository.enforceCountBasedRetention(MAX_RECORDS_PER_METRIC);

        System.out.println("--- JOB DE LIMPEZA FINALIZADO ---");
    }
}