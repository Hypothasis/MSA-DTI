package br.com.dti.msa.controller;

import br.com.dti.msa.dto.HostDashboardDTO;
import br.com.dti.msa.dto.HostSearchResultDTO;
import br.com.dti.msa.model.Host;
import br.com.dti.msa.service.HostService;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/host")
public class HostController {

    @Autowired
    private HostService hostService;

    /**
     * SERVE A PÁGINA HTML "ESQUELETO".
     * Rota: GET /host/{publicId}
     */
    @GetMapping("/{publicId}")
    public String getHostPage(@PathVariable String publicId, Model model) {
        Host host = hostService.findByPublicId(publicId);

        // ADICIONE O HOST AO MODELO
        model.addAttribute("host", host);

        switch (host.getType()) {
            case "APPLICATION": return "host/application";
            case "SERVER": return "host/server";
            case "DATABASE": return "host/database";
            default: return "error"; 
        }
    }

    /**
     * ENDPOINT DA API: Retorna todos os dados dinâmicos para a página do host.
     * Rota: GET /host/api/{publicId}
     */
    @GetMapping("/api/{publicId}")
    @ResponseBody
    public ResponseEntity<?> getHostDashboardData(@PathVariable String publicId) {
        try {
            HostDashboardDTO responseDto = hostService.getHostDashboardData(publicId);
            return ResponseEntity.ok(responseDto);
        } catch (EntityNotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * ENDPOINT PARA HOME: Retorna 5 hosts pelo nome
     * Rota: GET /host/api/search
     */
    @GetMapping("/api/search")
    public ResponseEntity<List<HostSearchResultDTO>> searchHosts(@RequestParam("term") String term) {
        List<HostSearchResultDTO> results = hostService.searchPublicHostsByName(term);
        return ResponseEntity.ok(results);
    }
}