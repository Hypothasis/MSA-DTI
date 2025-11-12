package br.com.dti.msa.scheduler;

import br.com.dti.msa.model.Host;
import br.com.dti.msa.model.HostMetricConfig;
import br.com.dti.msa.model.Metric;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;
import java.util.Map;
import java.util.Set;

// Importa a classe de resultado (você precisará torná-la 'public' ou 'default' (sem private))
import br.com.dti.msa.scheduler.MetricCollectorScheduler.StatusResult; 

import static org.junit.jupiter.api.Assertions.assertEquals;

class MetricCollectorSchedulerTest {

    // Cria uma instância real da classe que queremos testar
    @InjectMocks
    private MetricCollectorScheduler scheduler;

    // Métricas "falsas" que usaremos nos testes
    private Metric metricHealth;
    private Metric metricCpu;
    private Metric metricPing;

    @BeforeEach
    void setUp() {
        // Inicializa o 'scheduler'
        MockitoAnnotations.openMocks(this);
        
        // Configura nossos "conceitos" de métricas falsas
        metricHealth = new Metric();
        metricHealth.setMetricKey("disponibilidade-global-health");
        
        metricCpu = new Metric();
        metricCpu.setMetricKey("cpu-uso");
        
        metricPing = new Metric();
        metricPing.setMetricKey("disponibilidade-global");
    }

    @Test
    void testDetermineHostStatus_HealthCheckOK_DeveRetornarActive() {
        // 1. ARRANGE (Organizar)
        Host host = new Host();
        String zabbixKey = "sigaa.health.ready";
        
        // Cria a "configuração" que liga o host à métrica
        HostMetricConfig config = new HostMetricConfig(host, metricHealth, zabbixKey);
        host.setMetricConfigs(Set.of(config));

        // Cria o mapa de dados coletados (o que o ZabbixClient retornaria)
        Map<String, String> collectedItems = Map.of(
            zabbixKey, "{\"status\":\"UP\",\"deps\":{\"db\":\"UP\"}}"
        );

        // 2. ACT (Agir)
        StatusResult resultado = scheduler.determineHostStatus(host, collectedItems);

        // 3. ASSERT (Verificar)
        assertEquals(Host.HostStatus.ACTIVE, resultado.status);
        assertEquals("Tudo certo com o Host.", resultado.description);
    }
    
    @Test
    void testDetermineHostStatus_HealthCheckDBDown_DeveRetornarAlert() {
        // 1. ARRANGE
        Host host = new Host();
        String zabbixKey = "sigaa.health.ready";
        HostMetricConfig config = new HostMetricConfig(host, metricHealth, zabbixKey);
        host.setMetricConfigs(Set.of(config));
        
        Map<String, String> collectedItems = Map.of(
            zabbixKey, "{\"status\":\"UP\",\"deps\":{\"db\":\"DOWN\"}}"
        );

        // 2. ACT
        StatusResult resultado = scheduler.determineHostStatus(host, collectedItems);

        // 3. ASSERT
        assertEquals(Host.HostStatus.ALERT, resultado.status);
        assertEquals("Alerta: Aplicação 'UP', mas dependência 'db' está 'DOWN'.", resultado.description);
    }

    @Test
    void testDetermineHostStatus_PingFalhou_DeveRetornarInactive() {
        // 1. ARRANGE
        Host host = new Host();
        String zabbixKey = "zabbix[host,agent,available]";
        HostMetricConfig config = new HostMetricConfig(host, metricPing, zabbixKey);
        host.setMetricConfigs(Set.of(config));
        
        Map<String, String> collectedItems = Map.of(
            zabbixKey, "0.0" // Ping falhou
        );

        // 2. ACT
        StatusResult resultado = scheduler.determineHostStatus(host, collectedItems);

        // 3. ASSERT
        assertEquals(Host.HostStatus.INACTIVE, resultado.status);
        assertEquals("Host parado! (Ping falhou ou agente indisponível)", resultado.description);
    }
    
    @Test
    void testDetermineHostStatus_CPUAlta_DeveRetornarAlert() {
        // 1. ARRANGE
        Host host = new Host();
        String pingKey = "zabbix[host,agent,available]";
        String cpuKey = "system.cpu.util";
        
        HostMetricConfig pingConfig = new HostMetricConfig(host, metricPing, pingKey);
        HostMetricConfig cpuConfig = new HostMetricConfig(host, metricCpu, cpuKey);
        host.setMetricConfigs(Set.of(pingConfig, cpuConfig)); // Host tem 2 métricas
        
        Map<String, String> collectedItems = Map.of(
            pingKey, "1.0", // Ping OK
            cpuKey, "95.5"   // CPU Alta!
        );

        // 2. ACT
        StatusResult resultado = scheduler.determineHostStatus(host, collectedItems);

        // 3. ASSERT
        assertEquals(Host.HostStatus.ALERT, resultado.status);
        assertEquals("Host com alto consumo de CPU (95.5%)", resultado.description);
    }
}