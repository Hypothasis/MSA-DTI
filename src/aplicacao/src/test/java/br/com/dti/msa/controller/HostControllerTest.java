package br.com.dti.msa.controller;

import br.com.dti.msa.dto.HostDashboardDTO;
import br.com.dti.msa.model.Host;
import br.com.dti.msa.service.HostService;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(HostController.class)
@AutoConfigureMockMvc(addFilters = false)
public class HostControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private HostService hostService;

    @Test
    public void testGetHostPage_ApplicationType_ReturnsApplicationView() throws Exception {
        Host mockHost = new Host();
        mockHost.setType("APPLICATION");
        
        when(hostService.findByPublicId("123")).thenReturn(mockHost);

        mockMvc.perform(get("/host/123"))
                .andExpect(status().isOk())
                .andExpect(model().attributeExists("host"))
                .andExpect(view().name("host/application"));
    }

    @Test
    public void testGetHostDashboardData_Success() throws Exception {
        HostDashboardDTO mockDto = new HostDashboardDTO();
        when(hostService.getHostDashboardData("123")).thenReturn(mockDto);

        mockMvc.perform(get("/host/api/123"))
                .andExpect(status().isOk());
    }

    @Test
    public void testGetHostDashboardData_NotFound() throws Exception {
        when(hostService.getHostDashboardData("999")).thenThrow(new EntityNotFoundException());

        mockMvc.perform(get("/host/api/999"))
                .andExpect(status().isNotFound());
    }

    @Test
    public void testSearchHosts() throws Exception {
        when(hostService.searchPublicHostsByName("test")).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/host/api/search").param("term", "test"))
                .andExpect(status().isOk());
    }
}