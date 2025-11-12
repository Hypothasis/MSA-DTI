package br.com.dti.msa.dto;

import java.util.List;

import br.com.dti.msa.model.Host;
import lombok.Data;

@Data
public class PublicHostStatusDTO {
    private String publicId;
    private String name;
    private Host.HostStatus status;
    private String statusDescription;
    private String type;

    private HostDashboardDTO.AvailabilityDTO globalAvailability;
    private List<HostDashboardDTO.MetricValueDTO> availabilityHistory;

    public PublicHostStatusDTO(Host host) {
        this.publicId = host.getPublicId();
        this.name = host.getName();
        this.status = host.getStatus();
        this.type = host.getType();
        this.statusDescription = host.getStatusDescription();
    }
}