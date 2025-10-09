package br.com.dti.msa.model;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "recent_events")
public class RecentEvents {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "host_id", nullable = false)
    private Host host;

    @Column(nullable = false)
    private LocalDateTime timestamp;

    private String severity;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String name;

    @JdbcTypeCode(SqlTypes.JSON)
    private String details;
}