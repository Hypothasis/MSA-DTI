package br.com.dti.msa.model;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonBackReference;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Data
@Entity
@Table(name = "host_metric_config")
@IdClass(HostMetricConfigId.class)
public class HostMetricConfig {

    
    @Id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "host_id")
    @JsonBackReference
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Host host;

    @Id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "metric_id")
    @JsonBackReference
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Metric metric;
    
    @Column(name = "zabbix_key", nullable = false)
    private String zabbixKey;

    
    public HostMetricConfig() {}

    public HostMetricConfig(Host host, Metric metric, String zabbixKey) {
        this.host = host;
        this.metric = metric;
        this.zabbixKey = zabbixKey;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        HostMetricConfig that = (HostMetricConfig) o;
        return Objects.equals(host, that.host) &&
               Objects.equals(metric, that.metric);
    }

    @Override
    public int hashCode() {
        return Objects.hash(host, metric);
    }
}