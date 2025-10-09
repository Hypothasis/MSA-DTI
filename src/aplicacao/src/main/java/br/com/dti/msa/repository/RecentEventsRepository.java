package br.com.dti.msa.repository;

import br.com.dti.msa.model.RecentEvents;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface RecentEventsRepository extends JpaRepository<RecentEvents, Long> {

    /**
     * Busca os 5 eventos mais recentes de um host específico,
     * ordenados pela data e hora em ordem decrescente.
     * O Spring Data JPA cria a query automaticamente pelo nome do método.
     */
    List<RecentEvents> findTop5ByHostIdOrderByTimestampDesc(Long hostId);

    /**
     * Deleta os eventos antigos deste host.
     */
    @Transactional
    void deleteByHostId(Long hostId);
}