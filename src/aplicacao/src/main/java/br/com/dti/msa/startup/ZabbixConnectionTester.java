package br.com.dti.msa.startup;

import br.com.dti.msa.integration.zabbix.dto.ZabbixClient;
import br.com.dti.msa.model.ZabbixConnectionStatus;
import br.com.dti.msa.repository.ZabbixConnectionStatusRepository;
import br.com.dti.msa.scheduler.SchedulerManager; // <-- Importe o manager

import java.time.LocalDateTime;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class ZabbixConnectionTester implements CommandLineRunner {

    @Autowired private ZabbixClient zabbixClient;
    @Autowired private SchedulerManager schedulerManager; 
    @Autowired private ZabbixConnectionStatusRepository statusRepository;

    @Override
    public void run(String... args) {
        System.out.println("--- INICIANDO ORQUESTRADOR DE STARTUP DO MSA ---");
        try {
            zabbixClient.testConnection(); 
            
            System.out.println("--- CONEXÃO COM ZABBIX VALIDADA COM SUCESSO ---");
            
            saveStatus(ZabbixConnectionStatus.Status.SUCCESS, "Coleta executada com sucesso.");
            schedulerManager.startMetricCollection();

        } catch (Exception e) {
            System.err.println("--- FALHA CRÍTICA AO CONECTAR COM O ZABBIX NA INICIALIZAÇÃO, O COLETOR DE MÉTRICAS NÃO SERÁ INICIADO. ---");
            System.err.println("Error: " + e.getMessage());
            System.exit(1); 
        }
    }

    // Método auxiliar para salvar o status
    private void saveStatus(ZabbixConnectionStatus.Status status, String details) {
        ZabbixConnectionStatus statusRecord = new ZabbixConnectionStatus();
        statusRecord.setTimestamp(LocalDateTime.now());
        statusRecord.setStatus(status);
        statusRecord.setDetails(details);
        statusRepository.save(statusRecord);
    }
}