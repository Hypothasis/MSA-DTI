package br.com.dti.msa.scheduler;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.scheduling.TaskScheduler;
import java.time.Duration;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
public class SchedulerManagerTest {

    @Mock
    private TaskScheduler taskScheduler;

    @Mock
    private MetricCollectorScheduler metricCollector;

    @InjectMocks
    private SchedulerManager schedulerManager;

    @Test
    public void testStartMetricCollection_SchedulesTaskCorrectly() {
        schedulerManager.startMetricCollection();

        verify(taskScheduler).scheduleAtFixedRate(any(Runnable.class), eq(Duration.ofMillis(60000)));
    }
}