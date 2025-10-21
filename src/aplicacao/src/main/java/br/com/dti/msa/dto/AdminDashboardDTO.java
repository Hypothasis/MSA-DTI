package br.com.dti.msa.dto;

import br.com.dti.msa.model.Host;
import lombok.Data;
import java.util.List;

@Data
public class AdminDashboardDTO {
    // Para os KPI Cards
    private long totalHosts;
    private long activeHosts;
    private long alertHosts;
    private long inactiveHosts;

    // Para o gráfico de Disponibilidade Média
    private Double overallAvailability;

    // Para os feeds
    private List<RecentEventDTO> latestAlerts;
    private List<ProblematicHostDTO> topProblemHosts;

    // DTO aninhado para os alertas
    @Data
    public static class RecentEventDTO {
        private String hostName;
        private String eventName;
        private String timestamp;
        private String severity;
    }
    
    // DTO aninhado para os hosts com problemas
    @Data
    public static class ProblematicHostDTO {
        private String publicId;
        private String name;
        private Host.HostStatus status;
        private String description;
    }
}