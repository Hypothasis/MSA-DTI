package br.com.dti.msa.integration.zabbix.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class ZabbixItemResponseDTO {
    @JsonProperty("itemid")
    private String itemId;

    @JsonProperty("name")
    private String name;

    @JsonProperty("key_")
    private String key;

    @JsonProperty("lastvalue")
    private String lastValue;
}