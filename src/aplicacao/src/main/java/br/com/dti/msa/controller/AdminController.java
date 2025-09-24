package br.com.dti.msa.controller;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import br.com.dti.msa.model.Host;
import br.com.dti.msa.model.Metric;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;


@Controller
@RequestMapping("/admin")
public class AdminController {
    
    @GetMapping({"","/"})
    public String index() {
        return "admin/index";
    }

    @GetMapping({"search"})
    public String indexSearch( Model model) {

        // --- Métricas para o primeiro host ---
        List<Metric> sigaaMetrics = new ArrayList<>();
        sigaaMetrics.add(new Metric(1L, "system.cpu.util", "Uso de CPU", "%"));
        sigaaMetrics.add(new Metric(2L, "vm.memory.utilization", "Uso de Memória", "%"));

        List<Metric> seisipMetrics = new ArrayList<>();
        seisipMetrics.add(new Metric(3L, "system.cpu.util", "Uso de CPU", "%"));
        seisipMetrics.add(new Metric(4L, "vm.memory.utilization", "Uso de Memória", "%"));

        List<Metric> sigaaBDMetrics = new ArrayList<>();
        sigaaBDMetrics.add(new Metric(5L, "system.cpu.util", "Uso de CPU", "%"));
        sigaaBDMetrics.add(new Metric(6L, "vm.memory.utilization", "Uso de Memória", "%"));

        
        // 1. Crie uma lista de hosts falsos (mock data)
        List<Host> hostsFalsos = new ArrayList<>();
        hostsFalsos.add(new Host(123L, "10675awdsa", 10675, "Sigaa", "Aplicação SIGAA", "app", sigaaMetrics));
        hostsFalsos.add(new Host(124L, "10676qweqw", 10676 ,"SEI-SIP-01", "Aplicação SEI-SIP-01", "server", seisipMetrics));
        hostsFalsos.add(new Host(125L, "10677asdas", 10677, "BD_SIGAA", "Banco de Dados do SIGAA", "db", sigaaBDMetrics));
        
        // 2. Adicione a lista ao Model
        // "listaHosts" é o nome da variável que o Thymeleaf usará
        model.addAttribute("listaHosts", hostsFalsos);

        return "admin/search";
    }

    @PostMapping("search/host")
    public String searchHost(
        @RequestParam("inputSearch") String inputSearch,
        @RequestParam Map<String, String> allParams
    ) {

        // Filtra o mapa para pegar apenas os checkboxes do filtro (que não seja input)
        List<String> enabledFilters = allParams.keySet().stream()
            .filter(key ->  !key.equals("inputSearch") &&
                            !key.equals("_csrf"))
            .collect(Collectors.toList());

        System.out.println("Input: " + inputSearch);
        System.out.println("Filtro: " + enabledFilters);
        
        return "redirect:/admin/search";
    }
    

    @GetMapping({"create"})
    public String indexCreate() {
        return "admin/create";
    }

    @PostMapping("create/host")
    public String createHost(
        @RequestParam("host-name") String hostName,
        @RequestParam("host-zabbix-id") Long zabbixId,
        @RequestParam("host-description") String description,
        @RequestParam("host-type") String hostType,
        // Usamos um Map para pegar todos os outros parâmetros (os checkboxes)
        @RequestParam Map<String, String> allParams
    ) {
        
        // Filtra o mapa para pegar apenas os checkboxes (que não são os campos principais)
        List<String> enabledMetrics = allParams.keySet().stream()
            .filter(key -> !key.equals("host-name") && 
                        !key.equals("host-zabbix-id") &&
                        !key.equals("host-description") &&
                        !key.equals("host-type"))
            .collect(Collectors.toList());

        System.out.println("Nome do Host: " + hostName);
        System.out.println("Zabbix ID: " + zabbixId);
        System.out.println("Descrição: " + description);
        System.out.println("Tipo do Host: " + hostType);
        System.out.println("Métricas Selecionadas: " + enabledMetrics);


        return "redirect:/admin/create";
    }
    
    
}
