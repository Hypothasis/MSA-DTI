package br.com.dti.msa.repository;

import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import br.com.dti.msa.model.MetricHistory;
import br.com.dti.msa.model.MetricHistoryId;
import jakarta.transaction.Transactional;

// Chave composta como ID
public interface MetricHistoryRepository extends JpaRepository<MetricHistory, MetricHistoryId> {

    /**
     * Deleta registros de métricas acima de 48h, desde a ativição do servidor.
     */
    @Modifying
    @Transactional
    @Query("DELETE FROM MetricHistory mh WHERE mh.timestamp < :cutoffDate")
    void deleteOlderThan(LocalDateTime cutoffDate);

    /**
     * Encontra o primeiro registro ordenando pelo timestamp de forma decrescente
     */
    Optional<MetricHistory> findFirstByOrderByTimestampDesc();
}