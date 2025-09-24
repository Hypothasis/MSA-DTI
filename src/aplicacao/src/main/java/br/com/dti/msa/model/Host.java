package br.com.dti.msa.model;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor // Construtor com todos os argumentos para facilitar a criação
public class Host {
    private Long id;
    private String publicId;
    private Integer zabbixId;
    private String name;
    private String description;
    private String type;
    private List<Metric> metrics;
}