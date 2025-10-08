package br.com.dti.msa.startup;

import br.com.dti.msa.integration.zabbix.dto.ZabbixClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class ZabbixConnectionTester implements CommandLineRunner {

    @Autowired
    private ZabbixClient zabbixClient;

    @Override
    public void run(String... args) throws Exception {
        zabbixClient.testConnection();
    }
}