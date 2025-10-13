package br.com.dti.msa.dto;

import lombok.Data;
import lombok.AllArgsConstructor; 
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
public class HostDashboardDTO {

    // --- Dados Básicos do Host (preenchidos a partir da entidade Host) ---
    private String name;
    private String description;
    private String type;
    private String status;
    private String lastUpdateTime;

    // --- Dados para Gráficos e Displays ---

    // Métricas de Histórico (para gráficos de linha/área)
    private List<MetricValueDTO> availabilityHistory;
    private List<MetricValueDTO> latencyHistory;
    private List<MetricValueDTO> cpuUsageHistory;
    private List<MetricValueDTO> cpuContextSwitchesHistory;
    private List<MetricValueDTO> dataBandwidthInHistory;
    private List<MetricValueDTO> dataBandwidthOutHistory;

    // Métricas de Estado Atual (para gauges, radiais ou valores únicos)
    private AvailabilityDTO globalAvailability;
    private OsInfoDTO osInfo;
    private ProcessInfoHistoryDTO processInfoHistory;
    private StorageDTO memoryData;
    private StorageDTO swapData;
    private StorageDTO storageRootData;
    private StorageDTO storageBootData;
    private String uptime;
    private List<EventDTO> recentEvents;

    // --- Classes Aninhadas para Estruturação ---

    // Para séries temporais [timestamp, valor]
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MetricValueDTO {
        private long x; // Timestamp em milissegundos para ApexCharts
        private double y; // Valor

        public MetricValueDTO(LocalDateTime timestamp, Double value) {
            this.x = timestamp.atZone(java.time.ZoneId.of("America/Fortaleza"))
                          .toInstant()
                          .toEpochMilli();
            this.y = value != null ? value : 0.0;
        }
    }
    
    // Para métricas de armazenamento/memória
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StorageDTO {
        private double total;
        private double used;
        private double free;
        private double percentUsed;
    }

    // Para informações do SO
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OsInfoDTO {
        private String osName;
        private String arch;
    }

    // Para informações de processos
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProcessInfoHistoryDTO {
        private List<MetricValueDTO> current;
        private List<MetricValueDTO> max;
    }
    
    // Para cálculos de disponibilidade
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AvailabilityDTO {
        private double last48h;
        private double last24h;
        private double last12h;
        private double last6h;
    }

    // Para Eventos recentes
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EventDTO {
        private String timestamp;
        private String severity;
        private String name;
    }
}