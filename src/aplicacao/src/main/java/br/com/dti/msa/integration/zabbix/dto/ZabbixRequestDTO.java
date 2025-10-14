package br.com.dti.msa.integration.zabbix.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.Data;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Data
public class ZabbixRequestDTO {
    private String jsonrpc = "2.0";
    private String method;
    private Object params;
    private int id;

    public ZabbixRequestDTO(String method, Object params, int id) {
        this.method = method;
        this.params = params;
        this.id = id;
    }
}