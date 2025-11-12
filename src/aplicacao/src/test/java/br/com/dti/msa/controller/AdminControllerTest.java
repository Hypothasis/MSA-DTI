package br.com.dti.msa.controller;

import br.com.dti.msa.dto.CreateHostDTO;
import br.com.dti.msa.dto.HostDetailsDTO;
import br.com.dti.msa.dto.UpdateHostDTO;
import br.com.dti.msa.model.Host;
import br.com.dti.msa.service.HostService;
import jakarta.persistence.EntityNotFoundException;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
// Importação correta (que agora funciona graças ao seu pom.xml)
import org.springframework.security.test.context.support.WithMockUser; 
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

@SpringBootTest
@AutoConfigureMockMvc
class AdminControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private HostService hostService;

    // =======================================
    // TESTES DE CREATE (POST /)
    // =======================================

    @Test
    @WithMockUser(authorities = "ADMIN_CREATE")
    void createHost_ServidorPadrao_DeveRetornar201Created() throws Exception {
        
        // 1. ARRANGE (Organizar)
        CreateHostDTO dto = new CreateHostDTO();
        dto.setHostName("Servidor Linux Principal");
        dto.setHostZabbixID(10800L);
        dto.setHostDescription("Servidor de produção principal (ex: web)");
        dto.setHostType("SERVER");
        
        // Lista de NOMES DE CHECKBOX (do MetricCatalog) que o usuário marcaria
        dto.setEnabledMetrics(List.of(
            "disponibilidade-global",      // Checkbox de Disponibilidade (Padrão)
            "disponibilidade-especifica", // Checkbox de Disponibilidade (Padrão)
            "cpu-uso",                   // Checkbox de CPU
            "memoria-ram",               // Checkbox de Memória
            "sistema-operacional"        // Checkbox de SO
        ));
        
        // Campos customizados ficam vazios/nulos, pois não foram usados
        dto.setHealthHttpMetric(null);
        dto.setCustomHttpMetric(null);
        
        // Simula o host que será retornado pelo serviço
        Host hostSalvo = new Host();
        hostSalvo.setId(1L);
        hostSalvo.setName("Servidor Linux Principal");

        when(hostService.createAndValidateHost(any(CreateHostDTO.class))).thenReturn(hostSalvo);

        // 2. ACT & 3. ASSERT
        mockMvc.perform(post("/admin/api/hosts")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Servidor Linux Principal"));
    }

    @Test
    @WithMockUser(authorities = "ADMIN_CREATE")
    void createHost_AplicacaoHostAgentPadrao_DeveRetornar201Created() throws Exception {
        
        // 1. ARRANGE (Organizar)
        CreateHostDTO dto = new CreateHostDTO();
        dto.setHostName("Portal Interno");
        dto.setHostZabbixID(10803L);
        dto.setHostDescription("Aplicação interna de RH");
        dto.setHostType("APPLICATION"); // Define o tipo
        
        // Lista de NOMES DE CHECKBOX (do MetricCatalog)
        dto.setEnabledMetrics(List.of(
            "disponibilidade-global",      // <-- Usa o grupo de disponibilidade PADRÃO
            "disponibilidade-especifica",
            "cpu-uso",                   
            "memoria-ram",
            "eventos-recentes"
        ));
        
        // Campos customizados ficam nulos, pois este host não usa Health Check
        dto.setHealthHttpMetric(null);
        dto.setCustomHttpMetric(null);
        
        // Simula o host que será retornado
        Host hostSalvo = new Host();
        hostSalvo.setId(4L); // ID Fictício
        hostSalvo.setName(dto.getHostName());

        when(hostService.createAndValidateHost(any(CreateHostDTO.class))).thenReturn(hostSalvo);

        // 2. ACT & 3. ASSERT
        mockMvc.perform(post("/admin/api/hosts")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Portal Interno"));
    }

    @Test
    @WithMockUser(authorities = "ADMIN_CREATE")
    void createHost_AplicacaoSigaaHealth_DeveRetornar201Created() throws Exception {
        
        // 1. ARRANGE (Organizar)
        CreateHostDTO dto = new CreateHostDTO();
        dto.setHostName("SIGAA_stg");
        dto.setHostZabbixID(10841L);
        dto.setHostDescription("SIGAA de homologação com HTTP Agent");
        dto.setHostType("APPLICATION");
        
        // Lista de NOMES DE CHECKBOX (do MetricCatalog)
        dto.setEnabledMetrics(List.of(
            // Checkboxes de Disponibilidade "Health"
            "disponibilidade-global-health",
            "disponibilidade-especifica-health",
            // Outras métricas
            "cpu-uso",
            "memoria-ram"
        ));
        
        // Preenche o campo de texto customizado "Health Ready"
        dto.setHealthHttpMetric("sigaastg.health.ready"); 
        
        // O outro campo customizado fica vazio
        dto.setCustomHttpMetric(null);
        
        // Simula o host que será retornado
        Host hostSalvo = new Host();
        hostSalvo.setId(2L);
        hostSalvo.setName("SIGAA_stg");

        when(hostService.createAndValidateHost(any(CreateHostDTO.class))).thenReturn(hostSalvo);

        // 2. ACT & 3. ASSERT
        mockMvc.perform(post("/admin/api/hosts")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("SIGAA_stg"));
    }
    
    @Test
    @WithMockUser(authorities = "ADMIN_CREATE")
    void createHost_BancoDeDados_DeveRetornar201Created() throws Exception {
        
        // 1. ARRANGE (Organizar)
        CreateHostDTO dto = new CreateHostDTO();
        dto.setHostName("Banco de Dados (MySQL)");
        dto.setHostZabbixID(10802L);
        dto.setHostDescription("Banco de dados relacional de produção");
        dto.setHostType("DATABASE"); // Define o tipo
        
        // Lista de NOMES DE CHECKBOX (do MetricCatalog)
        dto.setEnabledMetrics(List.of(
            "disponibilidade-global",      // Disponibilidade Padrão
            "disponibilidade-especifica",
            "cpu-uso",                   
            "memoria-ram",               
            "armazenamento"              // Foco principal de um DB
        ));
        
        // Campos customizados ficam nulos
        dto.setHealthHttpMetric(null);
        dto.setCustomHttpMetric(null);
        
        // Simula o host que será retornado
        Host hostSalvo = new Host();
        hostSalvo.setId(3L); // ID Fictício
        hostSalvo.setName(dto.getHostName());

        when(hostService.createAndValidateHost(any(CreateHostDTO.class))).thenReturn(hostSalvo);

        // 2. ACT & 3. ASSERT
        mockMvc.perform(post("/admin/api/hosts")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Banco de Dados (MySQL)"));
    }

    // =======================================
    // TESTES DE READ (GET /{id})
    // =======================================

    @Test
    @WithMockUser(authorities = "ADMIN_READ") // Assumindo a permissão
    void getHostById_HostExiste_DeveRetornar200OkComHostDetailsDTO() throws Exception {
        // 1. ARRANGE
        Host hostMock = new Host(); // Simula o host do banco
        hostMock.setId(1L);
        hostMock.setName("Host Teste DTO");
        
        // Simula o DTO que o serviço vai retornar
        HostDetailsDTO dto = new HostDetailsDTO(hostMock, List.of("cpu-uso")); 
        
        // "Ensina" o mock: quando o serviço chamar 'getHostDetailsForUpdate' com ID 1, retorne o DTO
        when(hostService.getHostDetailsForUpdate(1L)).thenReturn(dto);

        // 2. ACT & 3. ASSERT
        mockMvc.perform(get("/admin/api/hosts/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Host Teste DTO"))
                .andExpect(jsonPath("$.enabledCheckboxes[0]").value("cpu-uso"));
    }

    @Test
    @WithMockUser(authorities = "ADMIN_READ")
    void getHostById_HostNaoExiste_DeveRetornar404NotFound() throws Exception {
        // 1. ARRANGE
        // "Ensina" o mock: quando o serviço chamar com ID 99, lance a exceção
        when(hostService.getHostDetailsForUpdate(99L)).thenThrow(new EntityNotFoundException("Host não encontrado"));

        // 2. ACT & 3. ASSERT
        mockMvc.perform(get("/admin/api/hosts/99"))
                .andExpect(status().isNotFound());
    }

    // =======================================
    // TESTES DE UPDATE (PUT /{id})
    // =======================================

    @Test
    @WithMockUser(authorities = "ADMIN_UPDATE") // Permissão de Update
    void updateHost_ComDadosValidos_DeveRetornar200Ok() throws Exception {
        // 1. ARRANGE
        UpdateHostDTO dto = new UpdateHostDTO();
        dto.setHostName("Host Atualizado");
        dto.setHostZabbixID(10841L);
        dto.setHostType("SERVER");
        
        // "Ensina" o mock: quando 'updateHost' for chamado, retorne um host (não importa qual)
        when(hostService.updateHost(eq(1L), any(UpdateHostDTO.class))).thenReturn(new Host());

        // 2. ACT & 3. ASSERT
        mockMvc.perform(put("/admin/api/hosts/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(authorities = "ADMIN_UPDATE")
    void updateHost_HostNaoExiste_DeveRetornar404NotFound() throws Exception {
        // 1. ARRANGE
        UpdateHostDTO dto = new UpdateHostDTO();
        
        // "Ensina" o mock: quando 'updateHost' for chamado com ID 99, lance a exceção
        when(hostService.updateHost(eq(99L), any(UpdateHostDTO.class))).thenThrow(new EntityNotFoundException());

        // 2. ACT & 3. ASSERT
        mockMvc.perform(put("/admin/api/hosts/99")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isNotFound());
    }

    // =======================================
    // TESTES DE DELETE (DELETE /{id})
    // =======================================
    
    @Test
    @WithMockUser(authorities = "ADMIN_DELETE") // Permissão de Delete
    void deleteHost_HostExiste_DeveRetornar200Ok() throws Exception {
        // 1. ARRANGE
        // "Ensina" o mock: quando 'deleteHost(1L)' for chamado, não faça nada (sucesso)
        doNothing().when(hostService).deleteHost(1L);

        // 2. ACT & 3. ASSERT
        mockMvc.perform(delete("/admin/api/hosts/1"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(authorities = "ADMIN_DELETE")
    void deleteHost_HostNaoExiste_DeveRetornar500() throws Exception {
        // 1. ARRANGE
        // "Ensina" o mock: quando 'deleteHost(99L)' for chamado, lance uma exceção
        // (O controller trata como 500 genérico)
        doThrow(new EntityNotFoundException()).when(hostService).deleteHost(99L);

        // 2. ACT & 3. ASSERT
        mockMvc.perform(delete("/admin/api/hosts/99"))
                .andExpect(status().isInternalServerError());
    }

    // =======================================
    // TESTES DE USUÁRIO SEM PERMISSÃO
    // =======================================
    @Test
    @WithMockUser(authorities = "USER_NORMAL") // CORREÇÃO: @WithMockUser
    public void createHost_SemPermissao_DeveRetornar403Forbidden() throws Exception { // CORREÇÃO: Assinatura completa
        // 1. ARRANGE
        CreateHostDTO dto = new CreateHostDTO(); // (não importa o conteúdo)

        // 2. ACT
        mockMvc.perform(post("/admin/api/hosts")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
        // 3. ASSERT
                .andExpect(status().isForbidden());
    }
}