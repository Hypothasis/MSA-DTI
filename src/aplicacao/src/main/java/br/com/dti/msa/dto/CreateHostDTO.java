package br.com.dti.msa.dto;

import java.util.ArrayList;
import java.util.List;
import lombok.Data;

@Data
public class CreateHostDTO {
    private String hostName;
    private Long hostZabbixID;
    private String hostDescription;
    private String hostType;
    private List<String> enabledMetrics = new ArrayList<>();
}
