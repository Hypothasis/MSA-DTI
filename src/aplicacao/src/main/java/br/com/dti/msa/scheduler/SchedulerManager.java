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
        
        taskScheduler.scheduleAtFixedRate(
            metricCollector::collectAllMetrics, 
            Duration.ofMillis(60000)          
        );
    }
}