package br.com.dti.msa.scheduler;

import br.com.dti.msa.repository.MetricHistoryRepository;
import br.com.dti.msa.repository.ZabbixConnectionStatusRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
public class CleanupSchedulerTest {

    @Mock
    private MetricHistoryRepository metricHistoryRepository;

    @Mock
    private ZabbixConnectionStatusRepository zabbixStatusRepository;

    @InjectMocks
    private CleanupScheduler cleanupScheduler;

    @Test
    public void testCleanupOldData_CallsRepositoriesCorrectly() {
        cleanupScheduler.cleanupOldData();

        verify(metricHistoryRepository, times(1)).deleteOlderThan(any(LocalDateTime.class));

        verify(zabbixStatusRepository, times(1)).deleteOlderThan(any(LocalDateTime.class));

        verify(metricHistoryRepository, times(1)).enforceCountBasedRetention(2880);
    }
}