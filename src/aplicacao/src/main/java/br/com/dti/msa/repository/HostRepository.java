package br.com.dti.msa.repository;

import br.com.dti.msa.model.Host;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface HostRepository extends JpaRepository<Host, Long> {

    /**
     * Busca hosts filtrando por um termo no nome ou descrição E/OU por uma lista de tipos.
     * JPQL (Java Persistence Query Language) nos dá flexibilidade para lidar com parâmetros opcionais.
     */
    @Query("SELECT h FROM Host h WHERE " +
           "(:term IS NULL OR LOWER(h.name) LIKE LOWER(CONCAT('%', :term, '%')) OR LOWER(h.description) LIKE LOWER(CONCAT('%', :term, '%'))) " +
           "AND (COALESCE(:types, NULL) IS NULL OR h.type IN :types)")
    List<Host> search(
        @Param("term") String term, 
        @Param("types") List<String> types
    );

    /**
     * Busca todas as métricas dos hosts, evitando o problema de N+1 queries.
     */
    @Query("SELECT h FROM Host h LEFT JOIN FETCH h.metrics")
    List<Host> findAllWithMetrics();

    /**
     * Busca o Host pela sua publicId.
     */
    Optional<Host> findByPublicId(String publicId);
}