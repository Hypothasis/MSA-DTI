package br.com.dti.msa.dto;

import br.com.dti.msa.model.Host;
import lombok.Data;
import java.util.List;

@Data
public class AdminDashboardDTO {
    private long totalHosts;
    private long activeHosts;
    private long alertHosts;
    private long inactiveHosts;

    private Double overallAvailability;

    private List<RecentEventDTO> latestAlerts;
    private List<ProblematicHostDTO> topProblemHosts;

    @Data
    public static class RecentEventDTO {
        private String hostName;
        private String eventName;
        private String timestamp;
        private String severity;
    }
    
    @Data
    public static class ProblematicHostDTO {
        private String publicId;
        private String name;
        private Host.HostStatus status;
        private String description;
    }
}