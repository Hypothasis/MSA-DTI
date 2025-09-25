package br.com.dti.msa.dto;

import java.util.List;
import lombok.Data;

@Data
public class CreateHostDTO {
    private String hostName;
    private Long hostZabbixID;
    private String hostDescription;
    private String hostType;
    private List<String> enabledMetrics;
}
