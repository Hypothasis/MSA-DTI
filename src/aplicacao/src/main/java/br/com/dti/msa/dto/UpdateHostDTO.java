package br.com.dti.msa.dto;

import java.util.List;
import lombok.Data;

@Data
public class UpdateHostDTO {
    
    private String hostName;
    private Long hostZabbixID;
    private String hostDescription;
    private String hostType;
    
    // Lista dos 'values' dos checkboxes marcados (ex: "memoria-ram", "disponibilidade-global-sigaa")
    private List<String> enabledMetrics;
    
    // --- CAMPO ADICIONADO ---
    // Este campo recebe o valor do input de texto para a chave Zabbix customizada
    // (name="http-agent-metric" no seu HTML)
    private String httpAgentMetric; 
}