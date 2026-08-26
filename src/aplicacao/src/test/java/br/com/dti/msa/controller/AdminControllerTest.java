package br.com.dti.msa.controller;

import br.com.dti.msa.dto.AdminDashboardDTO;
import br.com.dti.msa.dto.CreateHostDTO;
import br.com.dti.msa.dto.HostDetailsDTO;
import br.com.dti.msa.dto.UpdateHostDTO;
import br.com.dti.msa.exception.ZabbixValidationException;
import br.com.dti.msa.model.Host;
import br.com.dti.msa.service.HostService;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.ui.Model;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.assertArg;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminControllerTest {

    @Mock
    private HostService hostService;

    @Mock
    private Model model;

    @InjectMocks
    private AdminController adminController;

    @BeforeEach
    void setUp() {
    }

    @Test
    void shouldReturnAdminIndexView() {
        String result = adminController.showAdminIndex();

        assertEquals("admin/index", result);
    }

    @Test
    void shouldReturnDashboardStats() {
        AdminDashboardDTO stats = mock(AdminDashboardDTO.class);

        when(hostService.getAdminDashboardStats()).thenReturn(stats);

        ResponseEntity<AdminDashboardDTO> response =
                adminController.getDashboardStats();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertSame(stats, response.getBody());

        verify(hostService).getAdminDashboardStats();
    }

    @Test
    void shouldReturnSearchPageWithEmptyHostList() {
        String result = adminController.showSearchPage(model);

        assertEquals("admin/search", result);

        verify(model).addAttribute(
                eq("listaHosts"),
                anyList()
        );

        verify(model).addAttribute(
                "initialLoadMessage",
                "Use a barra de busca para encontrar hosts."
        );
    }

    @Test
    void shouldSearchHostsWithSelectedTypes() {
        String searchTerm = "SIGAA";

        List<Host> hosts = List.of(mock(Host.class));

        when(hostService.searchHosts(
                eq(searchTerm),
                eq(List.of("APPLICATION", "SERVER", "DATABASE"))
        )).thenReturn(hosts);

        String result = adminController.processSearch(
                searchTerm,
                "on",
                "on",
                "on",
                model
        );

        assertEquals("admin/search", result);

        verify(hostService).searchHosts(
                searchTerm,
                List.of("APPLICATION", "SERVER", "DATABASE")
        );

        verify(model).addAttribute("listaHosts", hosts);
    }

    @Test
    void shouldSearchHostsWithoutSelectedTypes() {
        String searchTerm = "SIGAA";

        List<Host> hosts = List.of(mock(Host.class));

        when(hostService.searchHosts(
                searchTerm,
                List.of()
        )).thenReturn(hosts);

        String result = adminController.processSearch(
                searchTerm,
                null,
                null,
                null,
                model
        );

        assertEquals("admin/search", result);

        verify(hostService).searchHosts(
                searchTerm,
                List.of()
        );

        verify(model).addAttribute("listaHosts", hosts);
    }

    @Test
    void shouldReturnCreatePage() {
        String result = adminController.showCreatePage(model);

        assertEquals("admin/create", result);

        verify(model).addAttribute(
                eq("createHostDTO"),
                any(CreateHostDTO.class)
        );
    }

    @Test
    void shouldCreateHostSuccessfully() throws ZabbixValidationException {
        CreateHostDTO dto = mock(CreateHostDTO.class);
        Host host = mock(Host.class);

        when(hostService.createAndValidateHost(dto))
                .thenReturn(host);

        ResponseEntity<?> response =
                adminController.createHost(dto);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertSame(host, response.getBody());

        verify(hostService).createAndValidateHost(dto);
    }

    @Test
    void shouldReturnBadRequestWhenZabbixValidationFails() throws ZabbixValidationException {
        CreateHostDTO dto = mock(CreateHostDTO.class);

        ZabbixValidationException exception =
                mock(ZabbixValidationException.class);

        when(exception.getMessage())
                .thenReturn("Host não encontrado no Zabbix");

        when(hostService.createAndValidateHost(dto))
                .thenThrow(exception);

        ResponseEntity<?> response =
                adminController.createHost(dto);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());

        assertEquals(
                Map.of("error", "Host não encontrado no Zabbix"),
                response.getBody()
        );

        verify(hostService).createAndValidateHost(dto);
    }

    @Test
    void shouldReturnConflictWhenZabbixIdAlreadyExists() throws ZabbixValidationException {
        CreateHostDTO dto = mock(CreateHostDTO.class);

        DataIntegrityViolationException exception =
                new DataIntegrityViolationException(
                        "Duplicate entry for hosts.zabbix_id"
                );

        when(hostService.createAndValidateHost(dto))
                .thenThrow(exception);

        ResponseEntity<?> response =
                adminController.createHost(dto);

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());

        assertEquals(
                Map.of(
                        "error",
                        "Já existe um host cadastrado com este Zabbix ID."
                ),
                response.getBody()
        );

        verify(hostService).createAndValidateHost(dto);
    }

    @Test
    void shouldReturnInternalServerErrorWhenCreateHostFails() throws ZabbixValidationException {
        CreateHostDTO dto = mock(CreateHostDTO.class);

        when(hostService.createAndValidateHost(dto))
                .thenThrow(new RuntimeException("Erro inesperado"));

        ResponseEntity<?> response =
                adminController.createHost(dto);

        assertEquals(
                HttpStatus.INTERNAL_SERVER_ERROR,
                response.getStatusCode()
        );

        assertEquals(
                Map.of("error", "Ocorreu um erro inesperado."),
                response.getBody()
        );

        verify(hostService).createAndValidateHost(dto);
    }

    @Test
    void shouldReturnHostDetails() {
        Long hostId = 10L;

        HostDetailsDTO details = mock(HostDetailsDTO.class);

        when(hostService.getHostDetailsForUpdate(hostId))
                .thenReturn(details);

        ResponseEntity<HostDetailsDTO> response =
                adminController.getHostById(hostId);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertSame(details, response.getBody());

        verify(hostService).getHostDetailsForUpdate(hostId);
    }

    @Test
    void shouldReturnNotFoundWhenHostDoesNotExist() {
        Long hostId = 10L;

        when(hostService.getHostDetailsForUpdate(hostId))
                .thenThrow(new EntityNotFoundException());

        ResponseEntity<HostDetailsDTO> response =
                adminController.getHostById(hostId);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNull(response.getBody());

        verify(hostService).getHostDetailsForUpdate(hostId);
    }

    @Test
    void shouldReturnAllHosts() {
        List<Host> hosts = List.of(
                mock(Host.class),
                mock(Host.class)
        );

        when(hostService.findAll()).thenReturn(hosts);

        ResponseEntity<List<Host>> response =
                adminController.getAllHosts();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertSame(hosts, response.getBody());

        verify(hostService).findAll();
    }

    @Test
    void shouldReturnInternalServerErrorWhenFindAllFails() {
        when(hostService.findAll())
                .thenThrow(new RuntimeException("Erro no banco"));

        ResponseEntity<List<Host>> response =
                adminController.getAllHosts();

        assertEquals(
                HttpStatus.INTERNAL_SERVER_ERROR,
                response.getStatusCode()
        );

        assertNull(response.getBody());

        verify(hostService).findAll();
    }

    @Test
    void shouldUpdateHostSuccessfully() throws Exception {
        Long hostId = 1L;
        UpdateHostDTO updateData = new UpdateHostDTO();

        Host updatedHost = new Host();

        when(hostService.updateHost(hostId, updateData))
                .thenReturn(updatedHost);

        ResponseEntity<Void> response =
                adminController.updateHost(hostId, updateData);

        assertEquals(HttpStatus.OK, response.getStatusCode());

        verify(hostService).updateHost(hostId, updateData);
    }

    @Test
    void shouldReturnNotFoundWhenUpdateHostDoesNotExist() throws ZabbixValidationException {
        Long hostId = 10L;
        UpdateHostDTO dto = mock(UpdateHostDTO.class);

        doThrow(new EntityNotFoundException())
                .when(hostService)
                .updateHost(hostId, dto);

        ResponseEntity<Void> response =
                adminController.updateHost(hostId, dto);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());

        verify(hostService).updateHost(hostId, dto);
    }

    @Test
    void shouldReturnInternalServerErrorWhenUpdateHostFails() throws ZabbixValidationException {
        Long hostId = 10L;
        UpdateHostDTO dto = mock(UpdateHostDTO.class);

        doThrow(new RuntimeException("Erro"))
                .when(hostService)
                .updateHost(hostId, dto);

        ResponseEntity<Void> response =
                adminController.updateHost(hostId, dto);

        assertEquals(
                HttpStatus.INTERNAL_SERVER_ERROR,
                response.getStatusCode()
        );

        verify(hostService).updateHost(hostId, dto);
    }

    @Test
    void shouldDeleteHostSuccessfully() {
        Long hostId = 10L;

        doNothing()
                .when(hostService)
                .deleteHost(hostId);

        ResponseEntity<Void> response =
                adminController.deleteHost(hostId);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNull(response.getBody());

        verify(hostService).deleteHost(hostId);
    }

    @Test
    void shouldReturnInternalServerErrorWhenDeleteHostFails() {
        Long hostId = 10L;

        doThrow(new RuntimeException("Erro"))
                .when(hostService)
                .deleteHost(hostId);

        ResponseEntity<Void> response =
                adminController.deleteHost(hostId);

        assertEquals(
                HttpStatus.INTERNAL_SERVER_ERROR,
                response.getStatusCode()
        );

        verify(hostService).deleteHost(hostId);
    }
}