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
    private Integer zabbixId;
    private String name;
    private String description;
    private String type;
    
    // Lista para o modal de UPDATE (nomes dos checkboxes)
    private List<String> enabledCheckboxes; 
    
    // Lista para o modal de READ (métricas individuais)
    private List<MetricDTO> metrics; 

    /**
     * DTO aninhado para as métricas, para não expor a entidade JPA.
     */
    @Data
    @AllArgsConstructor
    public static class MetricDTO {
        private String metricKey;
        private String name;
        private String zabbixKey;
        private String unit;
    }

    /**
     * Construtor que preenche AMBAS as listas.
     */
    public HostDetailsDTO(Host host, List<String> enabledCheckboxes) {
        this.id = host.getId();
        this.publicId = host.getPublicId();
        this.zabbixId = host.getZabbixId();
        this.name = host.getName();
        this.description = host.getDescription();
        this.type = host.getType();
        
        // Preenche a lista para o modal de UPDATE
        this.enabledCheckboxes = enabledCheckboxes;
        
        // Preenche a lista para o modal de READ
        this.metrics = host.getMetrics().stream()
            .map(metric -> new MetricDTO(
                metric.getMetricKey(),
                metric.getName(),
                metric.getZabbixKey(),
                metric.getUnit()
            ))
            .collect(Collectors.toList());
    }
}