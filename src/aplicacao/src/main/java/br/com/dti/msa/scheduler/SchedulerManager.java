package br.com.dti.msa.scheduler;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Service;
import java.time.Duration;

@Service
public class SchedulerManager {

    private final TaskScheduler taskScheduler;
    private final MetricCollectorScheduler metricCollector;

    @Autowired
    public SchedulerManager(TaskScheduler taskScheduler, MetricCollectorScheduler metricCollector) {
        this.taskScheduler = taskScheduler;
        this.metricCollector = metricCollector;
    }

    public void startMetricCollection() {
        System.out.println("Agendando o coletor de métricas para rodar a cada 60 segundos.");
        
        // Esta linha é o equivalente programático de @Scheduled(fixedRate = 60000)
        taskScheduler.scheduleAtFixedRate(
            metricCollector::collectAllMetrics, // O método que será executado
            Duration.ofMillis(60000)            // O intervalo
        );
    }
}