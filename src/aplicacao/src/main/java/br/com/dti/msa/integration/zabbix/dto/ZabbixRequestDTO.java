package br.com.dti.msa.integration.zabbix.dto;

import lombok.Data;

@Data
public class ZabbixRequestDTO {
    private String jsonrpc = "2.0";
    private String method;
    private Object params;
    private String auth;
    private int id;

    public ZabbixRequestDTO(String method, Object params, String auth, int id) {
        this.method = method;
        this.params = params;
        this.auth = auth;
        this.id = id;
    }
}