package br.com.dti.msa.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import java.io.Serializable;

/**
 * Representa a Chave Primária Composta da tabela host_metric_config.
 * Os nomes dos campos aqui (host, metric) DEVEM ser iguais aos nomes
 * dos campos de entidade @Id na classe HostMetricConfig.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class HostMetricConfigId implements Serializable {
    private Long host;    // Corresponde ao campo 'private Host host;'
    private Long metric;  // Corresponde ao campo 'private Metric metric;'
}