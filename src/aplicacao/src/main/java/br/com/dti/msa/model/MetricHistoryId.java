package br.com.dti.msa.model;

import java.io.Serializable;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MetricHistoryId implements Serializable {
    private Long host;
    private Long metric;
    private LocalDateTime timestamp;
}