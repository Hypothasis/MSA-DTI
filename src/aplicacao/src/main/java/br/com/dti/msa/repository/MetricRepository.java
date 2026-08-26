package br.com.dti.msa.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import br.com.dti.msa.model.Metric;
import java.util.List;
import java.util.Optional;

public interface MetricRepository extends JpaRepository<Metric, Long> {
    
    List<Metric> findByMetricKeyIn(List<String> metricKeys);

    Optional<Metric> findByMetricKey(String metricKey);
    
}