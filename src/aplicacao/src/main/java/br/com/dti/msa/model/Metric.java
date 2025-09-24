package br.com.dti.msa.model;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class Metric {
    private Long id;
    private String metricKey; // A chave do Zabbix, ex: "system.cpu.util"
    private String name;      // O nome amigável, ex: "Uso de CPU"
    private String unit;      // A unidade de medida, ex: "%"
}