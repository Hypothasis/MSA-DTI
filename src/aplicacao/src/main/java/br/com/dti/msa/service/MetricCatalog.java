package br.com.dti.msa.service;

import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

@Component
public class MetricCatalog {

    // Mapeia o NOME do checkbox do formulário para as METRIC_KEYs do nosso banco
    private static final Map<String, List<String>> CHECKBOX_TO_METRIC_KEYS = Map.ofEntries(
        Map.entry("disponibilidade-global", List.of("disponibilidade-global")),
        Map.entry("disponibilidade-especifica", List.of("disponibilidade-especifica")),
        Map.entry("latencia-tempo-resposta", List.of("latencia")),
        Map.entry("sistema-operacional", List.of("os-nome", "os-arch")),
        Map.entry("cpu-uso", List.of("cpu-uso")),
        Map.entry("cpu-processos", List.of("cpu-processos-atuais", "cpu-processos-max")),
        Map.entry("cpu-troca-contextos", List.of("cpu-troca-contextos")),
        Map.entry("memoria-ram", List.of("memoria-ram-total", "memoria-ram-disponivel")),
        Map.entry("memoria-swap", List.of("memoria-swap-total", "memoria-swap-livre")),
        Map.entry("armazenamento", List.of("armazenamento-root-total", "armazenamento-root-usado", "armazenamento-boot-total", "armazenamento-boot-usado")),
        Map.entry("dados-banda-larga", List.of("dados-entrada", "dados-saida")),
        Map.entry("tempo-ativo", List.of("tempo-ativo")),
        Map.entry("eventos-recentes", List.of("eventos-recentes"))
    );

    public List<String> getMetricKeysForCheckbox(String checkboxName) {
        return CHECKBOX_TO_METRIC_KEYS.getOrDefault(checkboxName, List.of());
    }
}