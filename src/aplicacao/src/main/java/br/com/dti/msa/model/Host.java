package br.com.dti.msa.model;

import jakarta.persistence.*;
import lombok.Data;
import java.util.List;
import java.util.ArrayList;

@Data
@Entity
@Table(name = "hosts")
public class Host {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "public_id", nullable = false, unique = true)
    private String publicId;

    @Column(name = "zabbix_id", nullable = false, unique = true)
    private Integer zabbixId;

    @Column(nullable = false)
    private String name;

    private String description;

    @Column(name = "host_type", nullable = false)
    private String type; // O nome do campo agora é 'type'

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private HostStatus status = HostStatus.ACTIVE;

    // RELACIONAMENTO MUITOS-PARA-MUITOS
    @ManyToMany(fetch = FetchType.EAGER) // EAGER para carregar as métricas junto com o host
    @JoinTable(
        name = "host_metric_config", // Nome da tabela de ligação
        joinColumns = @JoinColumn(name = "host_id"), // Coluna que aponta para esta entidade (Host)
        inverseJoinColumns = @JoinColumn(name = "metric_id") // Coluna que aponta para a outra (Metric)
    )
    private List<Metric> metrics = new ArrayList<>();

    public enum HostStatus {
        ACTIVE,  
        ALERT,  
        INACTIVE
    }
}