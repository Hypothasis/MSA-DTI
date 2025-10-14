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

    // Executa a cada hora, no minuto 0.
    // Ex: "0 0 4 * * ?" para rodar todo dia às 4 da manhã.
    @Scheduled(cron = "0 0 * * * ?") 
    public void cleanupOldMetricHistory() {
        System.out.println("--- INICIANDO JOB DE LIMPEZA DE MÉTRICAS ANTIGAS ---");

        // VERIFICA O ÚLTIMO REGISTRO
        Optional<MetricHistory> latestRecord = metricHistoryRepository.findFirstByOrderByTimestampDesc();

        if (latestRecord.isPresent()) {
            LocalDateTime lastEntryTime = latestRecord.get().getTimestamp();
            LocalDateTime now = LocalDateTime.now();

            // VERIFICA SE A COLETA ESTÁ ATIVA RECENTEMENTE (ex: nos últimos 2 minutos)
            if (lastEntryTime.isBefore(now.minusMinutes(2))) {
                // Se o último registro for muito antigo, aborta a limpeza.
                System.out.println(" >> Limpeza abortada. O último registro de métrica é antigo (" + lastEntryTime + "). " +
                                   "A aplicação pode ter acabado de reiniciar após um período offline.");
                System.out.println("--- JOB DE LIMPEZA FINALIZADO (SEM AÇÃO) ---");
                return;
            }
        }

        // --- TAREFA 1: Limpar Histórico de Métricas  ---
        // SE A COLETA ESTIVER OK, PROSSEGUE COM A LIMPEZA
        LocalDateTime cutoffDate = LocalDateTime.now().minusHours(48);
        System.out.println("Apagando registros de histórico anteriores a: " + cutoffDate);
        metricHistoryRepository.deleteOlderThan(cutoffDate);

        // --- TAREFA 2: Limpar Logs de Conexão com o Zabbix ---
        // Vamos manter os logs dos últimas 48 horas.
        LocalDateTime statusCutoffDate = LocalDateTime.now().minusHours(48);
        System.out.println("Apagando registros de status de conexão anteriores a: " + statusCutoffDate);
        zabbixStatusRepository.deleteOlderThan(statusCutoffDate);

        System.out.println("--- JOB DE LIMPEZA FINALIZADO ---");
    }
}