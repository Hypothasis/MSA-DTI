package br.com.dti.msa.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL) 
public class ZabbixHealthCheckResponse {

    private String status;
    private String message;
    private String zabbixVersion;

    public ZabbixHealthCheckResponse(String status, String message) {
        this.status = status;
        this.message = message;
    }
    
    public ZabbixHealthCheckResponse(String status, String message, String zabbixVersion) {
        this.status = status;
        this.message = message;
        this.zabbixVersion = zabbixVersion;
    }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public String getZabbixVersion() { return zabbixVersion; }
    public void setZabbixVersion(String zabbixVersion) { this.zabbixVersion = zabbixVersion; }
}