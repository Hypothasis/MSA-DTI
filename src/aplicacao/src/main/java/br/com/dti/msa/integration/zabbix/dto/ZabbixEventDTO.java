package br.com.dti.msa.integration.zabbix.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class ZabbixEventDTO {
    
    @JsonProperty("eventid")
    private String eventId;

    @JsonProperty("objectid")
    private String objectId;

    @JsonProperty("clock")
    private long clock; 

    @JsonProperty("name")
    private String name; 

    @JsonProperty("severity")
    private int severity;
    
    @JsonProperty("hosts")
    private List<HostInfo> hosts;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class HostInfo {
        @JsonProperty("hostid")
        private String hostId;
        
        @JsonProperty("name")
        private String name;
    }
}