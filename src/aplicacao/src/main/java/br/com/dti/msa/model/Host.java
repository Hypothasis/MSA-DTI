package br.com.dti.msa.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonManagedReference;

import java.util.HashSet;

@Data
@Entity
@Table(name = "hosts")
@EqualsAndHashCode(exclude = "metricConfigs") // Evita loops de referência
public class Host {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "public_id", nullable = false, unique = true)
    private String publicId;

    @Column(name = "zabbix_id", nullable = false, unique = true)
    private Long zabbixId;

    @Column(nullable = false)
    private String name;

    private String description;

    @Column(name = "host_type", nullable = false)
    private String type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private HostStatus status = HostStatus.ACTIVE;

    @Column(name = "status_description")
    private String statusDescription;

    // Um Host tem MUITAS Configurações de Métrica
    @OneToMany(
        mappedBy = "host", // Mapeado pelo campo "host" na HostMetricConfig
        cascade = CascadeType.ALL, // Salva/deleta as configs junto com o host
        orphanRemoval = true,      // Remove configs que não estão mais na lista
        fetch = FetchType.EAGER      // Carrega as configs junto com o host
    )
    @JsonManagedReference
    private Set<HostMetricConfig> metricConfigs = new HashSet<>();

    public enum HostStatus {
        ACTIVE,  
        ALERT,  
        INACTIVE
    }
}