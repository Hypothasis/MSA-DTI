package br.com.dti.msa.controller;

import br.com.dti.msa.dto.HomepageHostDTO;
import br.com.dti.msa.dto.PublicHostStatusDTO;
import br.com.dti.msa.model.ZabbixConnectionStatus;
import br.com.dti.msa.repository.ZabbixConnectionStatusRepository;
import br.com.dti.msa.service.HostService;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/public") 
public class PublicApiController {

    @Autowired private ZabbixConnectionStatusRepository statusRepository;
    @Autowired private HostService hostService;

    /**
     * Retorna o status da última tentativa de conexão com o Zabbix.
     * URL: GET /api/public/status/zabbix
     */
    @GetMapping("/status/zabbix")
    public ResponseEntity<ZabbixConnectionStatus> getZabbixConnectionStatus() {
        return statusRepository.findTopByOrderByTimestampDesc()
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }


    /**
     * Retorna uma lista com o status atual de todos os hosts.
     * URL: GET /api/public/hosts/status
     */
    @GetMapping("/hosts/status")
    public ResponseEntity<List<PublicHostStatusDTO>> getAllHostStatuses() {
        List<PublicHostStatusDTO> statuses = hostService.getPublicHostStatuses();
        return ResponseEntity.ok(statuses);
    }

    /**
     * Retorna uma lista com o status atual de todos os hosts, com availabilityGlobal.
     * URL: GET /api/public/home/status
     */
    @GetMapping("/home/status")
    public ResponseEntity<List<HomepageHostDTO>> getHomepageHostStatus() {
        List<HomepageHostDTO> hosts = hostService.getHomepageHosts();
        return ResponseEntity.ok(hosts);
    }
}