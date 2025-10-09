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
    private String objectId; // ID do trigger

    @JsonProperty("clock")
    private long clock; // Timestamp do evento

    @JsonProperty("name")
    private String name; // Descrição do problema

    @JsonProperty("severity")
    private int severity;
    
    // O campo "hosts" na resposta do Zabbix é um array
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