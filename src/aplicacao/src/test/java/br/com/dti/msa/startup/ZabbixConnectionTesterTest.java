package br.com.dti.msa.startup;

import br.com.dti.msa.integration.zabbix.dto.ZabbixClient;
import br.com.dti.msa.model.ZabbixConnectionStatus;
import br.com.dti.msa.repository.ZabbixConnectionStatusRepository;
import br.com.dti.msa.scheduler.SchedulerManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ZabbixConnectionTesterTest {

    @Mock
    private ZabbixClient zabbixClient;

    @Mock
    private SchedulerManager schedulerManager;

    @Mock
    private ZabbixConnectionStatusRepository statusRepository;

    @InjectMocks
    private ZabbixConnectionTester zabbixConnectionTester;

    @Test
    public void testRun_SuccessfulConnection_SavesStatusAndStartsScheduler() throws Exception {
        doNothing().when(zabbixClient).testConnection();

        zabbixConnectionTester.run();

        verify(statusRepository, times(1)).save(argThat(status -> 
            status.getStatus() == ZabbixConnectionStatus.Status.SUCCESS &&
            status.getDetails().contains("Coleta executada com sucesso")
        ));

        verify(schedulerManager, times(1)).startMetricCollection();
    }

    @Test
    public void testRun_FailedConnection_AbortsStartupGracefully() throws Exception {
        doThrow(new RuntimeException("Connection Refused")).when(zabbixClient).testConnection();

        zabbixConnectionTester.run();

        verify(statusRepository, never()).save(any(ZabbixConnectionStatus.class));

        verify(schedulerManager, never()).startMetricCollection();
    }
}