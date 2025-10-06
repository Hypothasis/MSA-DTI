package br.com.dti.msa.service;

import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

@Component
public class MetricCatalog {

    private static final Map<String, List<String>> METRIC_MAP = Map.ofEntries(
        Map.entry("disponibilidade-global", List.of("zabbix[host,agent,available]")),
        Map.entry("disponibilidade-especifica", List.of("zabbix[host,agent,available]")),
        Map.entry("latencia-tempo-resposta", List.of("icmppingsec")),
        Map.entry("sistema-operacional", List.of("system.sw.os", "system.sw.arch")),
        Map.entry("cpu-uso", List.of("system.cpu.util")),
        Map.entry("cpu-processos", List.of("proc.num", "kernel.maxproc")),
        Map.entry("cpu-troca-contextos", List.of("system.cpu.switches")),
        Map.entry("memoria-ram", List.of("vm.memory.size[available]", "vm.memory.utilization")),
        Map.entry("memoria-swap", List.of("vm.memory.size[swap]", "vm.memory.size[swpfree]")),
        Map.entry("armazenamento", List.of("vfs.fs.size[/,total]", "vfs.fs.size[/,used]", "vfs.fs.size[/boot,total]", "vfs.fs.size[/boot,used]")),
        Map.entry("dados-banda-larga", List.of("net.if.in[\"eth0\"]", "net.if.out[\"eth0\"]")),
        Map.entry("tempo-ativo", List.of("system.uptime"))
    );

    public List<String> getZabbixKeysFor(String formMetricName) {
        return METRIC_MAP.getOrDefault(formMetricName, List.of());
    }
}