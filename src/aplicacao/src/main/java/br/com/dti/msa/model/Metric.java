package br.com.dti.msa.model;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "metrics")
public class Metric {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "metric_key", nullable = false, unique = true)
    private String metricKey;

    @Column(nullable = false)
    private String name;

    private String unit;
}