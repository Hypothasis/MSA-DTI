package br.com.dti.msa.scheduler;

import br.com.dti.msa.integration.zabbix.dto.ZabbixClient;
import br.com.dti.msa.model.Host;
import br.com.dti.msa.model.HostMetricConfig;
import br.com.dti.msa.model.Metric;
import br.com.dti.msa.repository.HostRepository;
import br.com.dti.msa.repository.MetricCurrentValueRepository;
import br.com.dti.msa.repository.MetricHistoryRepository;
import br.com.dti.msa.repository.RecentEventsRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(MockitoExtension.class)
public class MetricCollectorSchedulerTest {

    @Mock private HostRepository hostRepository;
    @Mock private MetricHistoryRepository metricHistoryRepository;
    @Mock private MetricCurrentValueRepository metricCurrentValueRepository;
    @Mock private ZabbixClient zabbixClient;
    @Mock private RecentEventsRepository recentEventsRepository;

    @InjectMocks
    private MetricCollectorScheduler scheduler;

    private Host mockHost;
    private Set<HostMetricConfig> configs;

    @BeforeEach
    public void setUp() {
        mockHost = new Host();
        mockHost.setName("Servidor Linux 1");
        configs = new HashSet<>();
    }

    private void addMetricConfig(String metricKey, String zabbixKey) {
        Metric metric = new Metric();
        metric.setMetricKey(metricKey);
        
        HostMetricConfig config = new HostMetricConfig();
        config.setHost(mockHost);
        config.setMetric(metric);
        config.setZabbixKey(zabbixKey);
        
        configs.add(config);
    }

    @Test
    public void testDetermineHostStatus_CpuHigh_ReturnsAlert() {
        addMetricConfig("cpu-uso", "system.cpu.util");
        addMetricConfig("disponibilidade-global", "icmpping");
        mockHost.setMetricConfigs(configs);

        Map<String, String> collectedMetrics = new HashMap<>();
        collectedMetrics.put("system.cpu.util", "95.5");
        collectedMetrics.put("icmpping", "1"); // Ping OK

        MetricCollectorScheduler.StatusResult result = scheduler.determineHostStatus(mockHost, collectedMetrics);

        assertEquals(Host.HostStatus.ALERT, result.status);
        assertEquals("Host com alto consumo de CPU (95.5%)", result.description);
    }

    @Test
    public void testDetermineHostStatus_RamHigh_ReturnsAlert() {
        addMetricConfig("memoria-ram-total", "vm.memory.size[total]");
        addMetricConfig("memoria-ram-disponivel", "vm.memory.size[available]");
        addMetricConfig("disponibilidade-global", "icmpping");
        mockHost.setMetricConfigs(configs);

        Map<String, String> collectedMetrics = new HashMap<>();
        collectedMetrics.put("vm.memory.size[total]", "16000000000"); // 16GB
        collectedMetrics.put("vm.memory.size[available]", "1000000000"); // 1GB (6.25% livre)
        collectedMetrics.put("icmpping", "1");

        MetricCollectorScheduler.StatusResult result = scheduler.determineHostStatus(mockHost, collectedMetrics);

        assertEquals(Host.HostStatus.ALERT, result.status);
        assertEquals("Host com alto consumo de RAM (6.3% livre)", result.description);
    }

    @Test
    public void testDetermineHostStatus_PingFailed_ReturnsInactive() {
        addMetricConfig("disponibilidade-global", "icmpping");
        mockHost.setMetricConfigs(configs);

        Map<String, String> collectedMetrics = new HashMap<>();
        collectedMetrics.put("icmpping", "0"); // Ping falhou (0)

        MetricCollectorScheduler.StatusResult result = scheduler.determineHostStatus(mockHost, collectedMetrics);

        assertEquals(Host.HostStatus.INACTIVE, result.status);
        assertEquals("Host parado! (Ping falhou)", result.description);
    }

    @Test
    public void testDetermineHostStatus_AllOk_ReturnsActive() {
        addMetricConfig("cpu-uso", "system.cpu.util");
        addMetricConfig("disponibilidade-global", "icmpping");
        mockHost.setMetricConfigs(configs);

        // Ping OK e CPU em 40%
        Map<String, String> collectedMetrics = new HashMap<>();
        collectedMetrics.put("system.cpu.util", "40.0");
        collectedMetrics.put("icmpping", "1");

        MetricCollectorScheduler.StatusResult result = scheduler.determineHostStatus(mockHost, collectedMetrics);

        assertEquals(Host.HostStatus.ACTIVE, result.status);
        assertEquals("Tudo certo com o Host.", result.description);
    }
}