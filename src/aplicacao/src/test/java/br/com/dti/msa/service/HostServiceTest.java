package br.com.dti.msa.service;

import br.com.dti.msa.dto.HostSearchResultDTO;
import br.com.dti.msa.exception.ZabbixValidationException;
import br.com.dti.msa.integration.zabbix.dto.ZabbixClient;
import br.com.dti.msa.model.Host;
import br.com.dti.msa.repository.*;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class HostServiceTest {

    @Mock private ZabbixClient zabbixClient;
    @Mock private MetricCatalog metricCatalog;
    @Mock private HostRepository hostRepository;
    @Mock private MetricRepository metricRepository;
    @Mock private MetricHistoryRepository metricHistoryRepository;
    @Mock private RecentEventsRepository recentEventsRepository;
    @Mock private DefaultZabbixKeyRepository defaultZabbixKeyRepository;
    @Mock private MetricCurrentValueRepository metricCurrentValueRepository;

    @InjectMocks
    private HostService hostService;

    @Test
    public void testFindAll_ReturnsHostList() {
        Host mockHost = new Host();
        mockHost.setName("Servidor de Teste");
        when(hostRepository.findAll()).thenReturn(List.of(mockHost));

        List<Host> result = hostService.findAll();

        assertFalse(result.isEmpty());
        assertEquals(1, result.size());
        assertEquals("Servidor de Teste", result.get(0).getName());
        verify(hostRepository, times(1)).findAll();
    }

    @Test
    public void testFindById_Success() {
        Host mockHost = new Host();
        mockHost.setId(1L);
        when(hostRepository.findByIdWithFullMetrics(1L)).thenReturn(Optional.of(mockHost));

        Host result = hostService.findById(1L);

        assertNotNull(result);
        assertEquals(1L, result.getId());
    }

    @Test
    public void testFindById_NotFound_ThrowsException() {
        when(hostRepository.findByIdWithFullMetrics(99L)).thenReturn(Optional.empty());

        EntityNotFoundException exception = assertThrows(EntityNotFoundException.class, () -> {
            hostService.findById(99L);
        });

        assertEquals("Host não encontrado com ID: 99", exception.getMessage());
    }

    @Test
    public void testSearchPublicHostsByName_TermTooShort_ReturnsEmpty() {
        List<HostSearchResultDTO> result = hostService.searchPublicHostsByName("A");
        
        assertTrue(result.isEmpty());
        verify(hostRepository, never()).findTop5ByNameContainingIgnoreCase(anyString());
    }

    @Test
    public void testSearchPublicHostsByName_ValidTerm_ReturnsResults() {
        Host mockHost = new Host();
        mockHost.setPublicId("uuid-123");
        mockHost.setName("Sistema Acadêmico");
        
        when(hostRepository.findTop5ByNameContainingIgnoreCase("Sis")).thenReturn(List.of(mockHost));

        List<HostSearchResultDTO> result = hostService.searchPublicHostsByName("Sis");

        assertFalse(result.isEmpty());
        assertEquals("uuid-123", result.get(0).getPublicId());
        assertEquals("Sistema Acadêmico", result.get(0).getName());
    }

    @Test
    public void testDeleteHost_Success() {
        when(hostRepository.existsById(1L)).thenReturn(true);
        
        assertDoesNotThrow(() -> hostService.deleteHost(1L));
        verify(hostRepository, times(1)).deleteById(1L);
    }

    @Test
    public void testDeleteHost_NotFound_ThrowsException() {
        when(hostRepository.existsById(99L)).thenReturn(false);

        EntityNotFoundException exception = assertThrows(EntityNotFoundException.class, () -> {
            hostService.deleteHost(99L);
        });

        assertEquals("Host não encontrado com ID: 99", exception.getMessage());
        verify(hostRepository, never()).deleteById(anyLong());
    }
}