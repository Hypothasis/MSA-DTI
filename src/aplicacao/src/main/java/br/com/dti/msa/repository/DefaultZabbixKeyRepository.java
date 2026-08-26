package br.com.dti.msa.repository;

import br.com.dti.msa.model.DefaultZabbixKey;
import br.com.dti.msa.model.Metric;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface DefaultZabbixKeyRepository extends JpaRepository<DefaultZabbixKey, Long> {
    
    Optional<DefaultZabbixKey> findByMetric(Metric metric);
}