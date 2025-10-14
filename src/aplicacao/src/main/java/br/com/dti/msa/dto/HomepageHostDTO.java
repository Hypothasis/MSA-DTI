package br.com.dti.msa.dto;

import br.com.dti.msa.model.Host;
import lombok.Data;
import java.util.List;

@Data
public class HomepageHostDTO {
    private String publicId;
    private String name;
    private Host.HostStatus status;
    private Double globalAvailability48h;
    private List<HostDashboardDTO.MetricValueDTO> availabilityHistory;

    public HomepageHostDTO(Host host) {
        this.publicId = host.getPublicId();
        this.name = host.getName();
        this.status = host.getStatus();
    }
}