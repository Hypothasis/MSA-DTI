package br.com.dti.msa.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "metric_history")
@IdClass(MetricHistoryId.class)
public class MetricHistory {

    // --- Início da Chave Primária Composta ---

    @Id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "host_id")
    private Host host;

    @Id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "metric_id")
    private Metric metric;

    @Id
    @Column(name = "timestamp")
    private LocalDateTime timestamp;
    
    // --- Fim da Chave Primária Composta ---

    @Column(name = "value", nullable = false)
    private Double value;
}
