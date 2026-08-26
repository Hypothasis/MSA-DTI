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

    private String name;
    private String description;
    private String type;
    private String status;
    private String statusDescription;
    private String lastUpdateTime;

    private List<MetricValueDTO> availabilityHistory;
    private List<MetricValueDTO> latencyHistory;
    private List<MetricValueDTO> cpuUsageHistory;
    private List<MetricValueDTO> cpuContextSwitchesHistory;
    private List<MetricValueDTO> dataBandwidthInHistory;
    private List<MetricValueDTO> dataBandwidthOutHistory;

    private AvailabilityDTO globalAvailability;
    private OsInfoDTO osInfo;
    private ProcessInfoHistoryDTO processInfoHistory;
    private StorageDTO memoryData;
    private StorageDTO swapData;
    private StorageDTO storageRootData;
    private StorageDTO storageBootData;
    private String uptime;
    private List<EventDTO> recentEvents;

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
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StorageDTO {
        private double total;
        private double used;
        private double free;
        private double percentUsed;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OsInfoDTO {
        private String osName;
        private String arch;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProcessInfoHistoryDTO {
        private List<MetricValueDTO> current;
        private List<MetricValueDTO> max;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AvailabilityDTO {
        private double last48h;
        private double last24h;
        private double last12h;
        private double last6h;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EventDTO {
        private String timestamp;
        private String severity;
        private String name;
    }
}