package br.com.dti.msa.dto;

import br.com.dti.msa.model.Host;
import lombok.AllArgsConstructor;
import lombok.Data;
import java.util.List;
import java.util.stream.Collectors;

@Data
public class HostDetailsDTO {
    private Long id;
    private String publicId;
    private Long zabbixId;
    private String name;
    private String description;
    private String type;
    
    private List<String> enabledCheckboxes; 
    
    private List<MetricDTO> metrics; 

    @Data
    @AllArgsConstructor
    public static class MetricDTO {
        private String metricKey;
        private String name;
        private String zabbixKey;
        private String unit;
    }

    public HostDetailsDTO(Host host, List<String> enabledCheckboxes) {
        this.id = host.getId();
        this.publicId = host.getPublicId();
        this.zabbixId = host.getZabbixId();
        this.name = host.getName();
        this.description = host.getDescription();
        this.type = host.getType();
        
        this.enabledCheckboxes = enabledCheckboxes;
        
        this.metrics = host.getMetricConfigs().stream()
            .map(config -> new MetricDTO(
                config.getMetric().getMetricKey(), 
                config.getMetric().getName(),
                config.getZabbixKey(), 
                config.getMetric().getUnit()
            ))
            .collect(Collectors.toList());
    }
}