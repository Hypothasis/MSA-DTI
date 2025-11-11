package br.com.dti.msa.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.time.LocalDateTime;

/**
 * Representa a tabela 'metric_current_value'.
 * Armazena o último valor conhecido de métricas que não são séries temporais,
 * como métricas de texto (ex: nome do SO) ou JSONs (ex: health checks).
 */
@Data
@Entity
@Table(name = "metric_current_value",
    uniqueConstraints = {
        // Garante que só pode haver um valor por combinação de host/métrica
        @UniqueConstraint(columnNames = {"host_id", "metric_id"})
    }
)
public class MetricCurrentValue {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * O Host ao qual este valor pertence.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "host_id", nullable = false)
    @ToString.Exclude // Evita recursão no log
    @EqualsAndHashCode.Exclude // Evita recursão no equals
    private Host host;

    /**
     * A Métrica "conceitual" à qual este valor está associado.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "metric_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Metric metric;

    /**
     * O último valor de texto ou JSON coletado.
     */
    @Column(name = "current_value", columnDefinition = "TEXT")
    private String currentValue;

    /**
     * O timestamp de quando este valor foi coletado.
     */
    @Column(name = "last_updated", nullable = false)
    private LocalDateTime lastUpdated;
}