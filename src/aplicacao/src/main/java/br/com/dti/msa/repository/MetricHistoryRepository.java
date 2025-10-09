package br.com.dti.msa.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import br.com.dti.msa.model.MetricHistory;
import br.com.dti.msa.model.MetricHistoryId;
import org.springframework.transaction.annotation.Transactional;

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

    /**
     * Busca o valor MAIS recente de uma métrica (útil para gauges/valores únicos)
     */
    Optional<MetricHistory> findFirstByHostIdAndMetricMetricKeyOrderByTimestampDesc(Long hostId, String metricKey);

    /**
     * Busca TODO o histórico de uma métrica para um host APÓS uma data, ordenado do mais antigo para o mais novo
     */
    List<MetricHistory> findByHostIdAndMetricMetricKeyAndTimestampAfterOrderByTimestampAsc(Long hostId, String metricKey, LocalDateTime startTime);

    /**
     * Calcula a disponibilidade média (em %) para uma métrica de um host em um determinado período.
     */
    @Query("SELECT AVG(mh.value) * 100.0 FROM MetricHistory mh " +
           "WHERE mh.host.id = :hostId " +
           "AND mh.metric.metricKey = :metricKey " +
           "AND mh.timestamp >= :startTime")
    Double calculateAvailability(
        @Param("hostId") Long hostId, 
        @Param("metricKey") String metricKey, 
        @Param("startTime") LocalDateTime startTime
    );

    /**
     * Agrupa os dados de disponibilidade por dia e calcula a média para cada dia.
     */
    @Query(value = "SELECT DATE(mh.timestamp) as day, AVG(mh.value) * 100.0 as avg_value " +
                   "FROM metric_history mh " +
                   "JOIN metrics m ON mh.metric_id = m.id " +
                   "WHERE mh.host_id = :hostId AND m.metric_key = :metricKey AND mh.timestamp >= :startTime " +
                   "GROUP BY DATE(mh.timestamp) " +
                   "ORDER BY day ASC", nativeQuery = true)
    List<Object[]> getDailyAvailability(@Param("hostId") Long hostId, 
                                        @Param("metricKey") String metricKey, 
                                        @Param("startTime") LocalDateTime startTime);
}