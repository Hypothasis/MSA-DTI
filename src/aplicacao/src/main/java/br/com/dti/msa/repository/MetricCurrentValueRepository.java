package br.com.dti.msa.repository;

import br.com.dti.msa.model.MetricCurrentValue;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface MetricCurrentValueRepository extends JpaRepository<MetricCurrentValue, Long> {

    /**
     * Encontra um registro de valor atual com base na combinação de Host e Métrica.
     * Usado pelo Scheduler para saber se deve criar um novo registro ou atualizar um existente.
     */
    Optional<MetricCurrentValue> findByHostIdAndMetricId(Long hostId, Long metricId);

    /**
     * Encontra um registro de valor atual com base no ID do Host e na Chave da Métrica (metric_key).
     * Usado pelo HostService para buscar os dados para o dashboard.
     */
    Optional<MetricCurrentValue> findByHostIdAndMetricMetricKey(Long hostId, String metricKey);
}