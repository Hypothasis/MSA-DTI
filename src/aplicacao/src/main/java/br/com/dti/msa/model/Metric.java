package br.com.dti.msa.model;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "metrics")
public class Metric {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "metric_key", unique = true, nullable = false)
    private String metricKey;

    @Column(name = "zabbix_key", nullable = false)
    private String zabbixKey;

    @Column(nullable = false)
    private String name;
    
    private String unit;
}