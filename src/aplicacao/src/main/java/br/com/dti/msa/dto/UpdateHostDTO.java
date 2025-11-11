package br.com.dti.msa.dto;

import java.util.List;
import lombok.Data;

@Data
public class UpdateHostDTO {
    
    private String hostName;
    private Long hostZabbixID;
    private String hostDescription;
    private String hostType;
    
    // Lista dos 'values' dos checkboxes marcados (ex: "memoria-ram", "disponibilidade-global-health")
    private List<String> enabledMetrics;
    
    /**
     * Recebe o valor do input name="health-http-metric"
     */
    private String healthHttpMetric; 
    
    /**
     * Recebe o valor do input name="custom-http-metric"
     */
    private String customHttpMetric;
}