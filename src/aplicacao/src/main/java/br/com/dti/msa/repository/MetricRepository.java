package br.com.dti.msa.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import br.com.dti.msa.model.Metric;
import java.util.List;

public interface MetricRepository extends JpaRepository<Metric, Long> {
    
    // Método customizado para buscar uma lista de métricas por suas chaves
    // O Spring cria a query automaticamente pelo nome do método!
    List<Metric> findByMetricKeyIn(List<String> metricKeys);
}