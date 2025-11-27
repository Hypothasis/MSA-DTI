package br.com.dti.msa.integration.zabbix.dto;

import lombok.Data;

@Data
public class ZabbixCountResponseDTO {
    private String jsonrpc;
    private Integer result;
    private int id;
}