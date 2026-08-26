package br.com.dti.msa.controller;

import br.com.dti.msa.dto.ZabbixHealthCheckResponse;
import br.com.dti.msa.model.ZabbixConnectionStatus;
import br.com.dti.msa.repository.ZabbixConnectionStatusRepository;
import br.com.dti.msa.service.HostService;
import br.com.dti.msa.service.ZabbixConnectionTesterService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PublicApiController.class)
@AutoConfigureMockMvc(addFilters = false)
public class PublicApiControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ZabbixConnectionStatusRepository statusRepository;

    @MockBean
    private ZabbixConnectionTesterService zabbixTesterService;

    @MockBean
    private HostService hostService;

    @Test
    public void testGetZabbixConnectionStatus_Found() throws Exception {
        ZabbixConnectionStatus mockStatus = new ZabbixConnectionStatus();
        when(statusRepository.findTopByOrderByTimestampDesc()).thenReturn(Optional.of(mockStatus));

        mockMvc.perform(get("/api/public/zabbix/status"))
                .andExpect(status().isOk());
    }

    @Test
    public void testCheckZabbixHealth_OkStatus() throws Exception {
        ZabbixHealthCheckResponse mockResponse = new ZabbixHealthCheckResponse("OK", "Conexão bem-sucedida");
        
        when(zabbixTesterService.testConnection()).thenReturn(mockResponse);

        mockMvc.perform(get("/api/public/zabbix/health-check"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("OK"));
    }

    @Test
    public void testCheckZabbixHealth_FailStatus() throws Exception {
        ZabbixHealthCheckResponse mockResponse = new ZabbixHealthCheckResponse("FAIL", "Falha na conexão");
        
        when(zabbixTesterService.testConnection()).thenReturn(mockResponse);

        mockMvc.perform(get("/api/public/zabbix/health-check"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.status").value("FAIL"));
    }
}