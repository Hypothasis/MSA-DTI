package br.com.dti.msa.dto;

import br.com.dti.msa.model.Host;
import lombok.Data;

@Data
public class PublicHostStatusDTO {
    private String publicId;
    private String name;
    private Host.HostStatus status;

    public PublicHostStatusDTO(Host host) {
        this.publicId = host.getPublicId();
        this.name = host.getName();
        this.status = host.getStatus();
    }
}