package br.com.dti.msa.controller;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;


@Controller
@RequestMapping("/admin")
public class AdminController {
    
    @GetMapping({"","/"})
    public String index() {
        return "admin/index";
    }

    @GetMapping({"search"})
    public String indexSearch() {
        return "admin/search";
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
