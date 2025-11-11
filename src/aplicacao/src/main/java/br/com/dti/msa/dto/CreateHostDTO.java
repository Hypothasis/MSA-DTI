package br.com.dti.msa.dto;

import java.util.ArrayList;
import java.util.List;
import lombok.Data;

@Data
public class CreateHostDTO {
    private String hostName;
    private Long hostZabbixID;
    private String hostDescription;
    private String hostType = "APPLICATION"; // Padrão como APPLICATION
    private List<String> enabledMetrics = new ArrayList<>();
    
    // Recebe o valor do input name="sigaa-http-metric"
    private String sigaaHttpMetric; 
    
    // Recebe o valor do input name="custom-http-metric"
    private String customHttpMetric;
}
