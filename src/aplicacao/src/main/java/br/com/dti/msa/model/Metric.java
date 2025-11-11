package br.com.dti.msa.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.util.Set;
import java.util.HashSet;

@Data
@Entity
@Table(name = "metrics")
@EqualsAndHashCode(exclude = "hostConfigs") // Evita loops de referência
public class Metric {
    
    @Id 
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "metric_key", unique = true, nullable = false)
    private String metricKey;

    private String name;
    private String unit;

    // --- RELACIONAMENTO CORRIGIDO ---
    // Uma Métrica (conceito) pode estar em MUITAS Configurações de Host
    @OneToMany(mappedBy = "metric", fetch = FetchType.LAZY)
    private Set<HostMetricConfig> hostConfigs = new HashSet<>();
}