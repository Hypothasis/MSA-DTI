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
     * Busca todos os hosts e já carrega (FETCH) suas configurações de métrica
     * E as métricas (conceitos) associadas a essas configurações.
     * Isso resolve o LazyInitializationException.
     */
    @Query("SELECT h FROM Host h " +
           "LEFT JOIN FETCH h.metricConfigs mc " +
           "LEFT JOIN FETCH mc.metric")
    List<Host> findAllWithMetrics();

    /**
     * Busca um Host pelo ID e carrega (FETCH) todas as suas configurações
     * E as métricas (conceitos) associadas a essas configurações.
     * Isso resolve o LazyInitializationException.
     */
    @Query("SELECT h FROM Host h " +
           "LEFT JOIN FETCH h.metricConfigs mc " +
           "LEFT JOIN FETCH mc.metric " + // <-- A MÁGICA ESTÁ AQUI
           "WHERE h.id = :hostId")
    Optional<Host> findByIdWithFullMetrics(@Param("hostId") Long hostId);

    /**
     * Busca o Host pela sua publicId e carrega (FETCH) todas as suas configurações
     * E as métricas (conceitos) associadas a essas configurações.
     */
    @Query("SELECT h FROM Host h " +
           "LEFT JOIN FETCH h.metricConfigs mc " +
           "LEFT JOIN FETCH mc.metric " + 
           "WHERE h.publicId = :publicId")
    Optional<Host> findByPublicIdWithFullMetrics(@Param("publicId") String publicId);

    /**
     * Busca os 5 primeiros hosts cujo nome contém o termo (ignorando maiúsculas/minúsculas)
     */
    List<Host> findTop5ByNameContainingIgnoreCase(String name);

    // Contagem otimizada por status
    long countByStatus(Host.HostStatus status);
    
    /**
     * Busca hosts que não estão ativos
     */
    List<Host> findByStatusIn(List<Host.HostStatus> statuses);

    boolean existsByZabbixId(Long zabbixId);
}