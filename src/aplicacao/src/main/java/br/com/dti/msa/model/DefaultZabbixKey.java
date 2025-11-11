package br.com.dti.msa.model;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "default_zabbix_key")
public class DefaultZabbixKey {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Relacionamento 1-para-1: Uma Métrica (conceito) tem uma chave padrão.
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "metric_id", nullable = false)
    private Metric metric;

    @Column(name = "zabbix_key", nullable = false)
    private String zabbixKey;
}