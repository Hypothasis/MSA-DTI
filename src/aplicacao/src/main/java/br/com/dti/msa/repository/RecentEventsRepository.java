package br.com.dti.msa.repository;

import br.com.dti.msa.model.RecentEvents;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface RecentEventsRepository extends JpaRepository<RecentEvents, Long> {

    /**
     * Busca os 5 eventos mais recentes de um host específico,
     * ordenados pela data e hora em ordem decrescente.
     * O Spring Data JPA cria a query automaticamente pelo nome do método.
     */
    //List<RecentEvents> findTop5ByHostIdOrderByTimestampDesc(Long hostId);

    /**
     * Deleta os eventos antigos deste host.
     */
    @Transactional
    void deleteByHostId(Long hostId);

    /**
     * Busca os eventos mais recentes com severidade alta ou desastre
     */
    @Query("SELECT re FROM RecentEvents re JOIN FETCH re.host " +
           "WHERE re.severity IN :severities " +
           "ORDER BY re.timestamp DESC")
    List<RecentEvents> findRecentCriticalEvents(@Param("severities") List<String> severities, Pageable pageable);

    /**
     * Busca os eventos mais recentes para um HOST ESPECÍFICO, 
     * filtrando por uma lista de severidades.
     */
    @Query("SELECT re FROM RecentEvents re " +
           "WHERE re.host.id = :hostId AND re.severity IN :severities " +
           "ORDER BY re.timestamp DESC")
    List<RecentEvents> findRecentCriticalEventsForHost(
        @Param("hostId") Long hostId, 
        @Param("severities") List<String> severities, 
        Pageable pageable
    );
}