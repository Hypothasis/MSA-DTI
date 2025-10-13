package br.com.dti.msa.startup;

import br.com.dti.msa.integration.zabbix.dto.ZabbixClient;
import br.com.dti.msa.scheduler.SchedulerManager; // <-- Importe o manager
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class ZabbixConnectionTester implements CommandLineRunner {

    @Autowired
    private ZabbixClient zabbixClient;

    @Autowired
    private SchedulerManager schedulerManager; // <-- Injete o manager

    @Override
    public void run(String... args) {
        System.out.println("--- INICIANDO ORQUESTRADOR DE STARTUP DO MSA ---");
        try {
            zabbixClient.testConnection(); 
            
            System.out.println("--- CONEXÃO COM ZABBIX VALIDADA COM SUCESSO ---");
            
            schedulerManager.startMetricCollection();

        } catch (Exception e) {
            System.err.println("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
            System.err.println("!!! FALHA CRÍTICA AO CONECTAR COM O ZABBIX NA INICIALIZAÇÃO !!!");
            System.err.println("!!! O COLETOR DE MÉTRICAS NÃO SERÁ INICIADO.               !!!");
            System.err.println("!!! Erro: " + e.getMessage());
            System.err.println("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");

            System.exit(1); 
        }
    }
}