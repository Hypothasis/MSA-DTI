package br.com.dti.msa.repository;

import br.com.dti.msa.model.ZabbixConnectionStatus;
import jakarta.transaction.Transactional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.Optional;

public interface ZabbixConnectionStatusRepository extends JpaRepository<ZabbixConnectionStatus, Long> {
    
    // Busca o registro de status mais recente
    Optional<ZabbixConnectionStatus> findTopByOrderByTimestampDesc();

    @Modifying
    @Transactional
    @Query("DELETE FROM ZabbixConnectionStatus zcs WHERE zcs.timestamp < :cutoffDate")
    void deleteOlderThan(LocalDateTime cutoffDate);
}