package br.com.dti.msa.controller;

import br.com.dti.msa.dto.AdminDashboardDTO;
import br.com.dti.msa.dto.CreateHostDTO;
import br.com.dti.msa.dto.HostDetailsDTO;
import br.com.dti.msa.dto.UpdateHostDTO;
import br.com.dti.msa.exception.ZabbixValidationException;
import br.com.dti.msa.model.Host;
import br.com.dti.msa.service.HostService;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/admin")
public class AdminController {

    @Autowired
    private HostService hostService;
    
    // --- MÉTODOS PARA RENDERIZAR AS PÁGINAS HTML (VIEWS) ---

    @GetMapping({"", "/"})
    public String showAdminIndex() {
        return "admin/index";
    }

    @GetMapping("/api/dashboard-stats")
    @ResponseBody
    public ResponseEntity<AdminDashboardDTO> getDashboardStats() {
        AdminDashboardDTO stats = hostService.getAdminDashboardStats();
        return ResponseEntity.ok(stats);
    }

    @GetMapping("/search")
    public String showSearchPage(Model model) {
        // Carrega a página de busca inicialmente com uma lista vazia.
        model.addAttribute("listaHosts", new ArrayList<Host>());
        model.addAttribute("initialLoadMessage", "Use a barra de busca para encontrar hosts.");
        return "admin/search";
    }

    @PostMapping("/search/host")
    public String processSearch(
        @RequestParam(value = "inputSearch", required = false) String searchTerm,
        @RequestParam(value = "APPLICATION", required = false) String typeApp,
        @RequestParam(value = "SERVER", required = false) String typeServer,
        @RequestParam(value = "DATABASE", required = false) String typeDb,
        Model model
    ) {
        // Monta uma lista apenas com os tipos que foram selecionados
        List<String> selectedTypes = new ArrayList<>();
        if (typeApp != null) selectedTypes.add("APPLICATION");
        if (typeServer != null) selectedTypes.add("SERVER");
        if (typeDb != null) selectedTypes.add("DATABASE");

        System.out.println("Termo de busca: " + searchTerm);
        System.out.println("Tipos selecionados: " + selectedTypes);
        
        // Chama o SERVICE para fazer a busca
        List<Host> filteredHosts = hostService.searchHosts(searchTerm, selectedTypes);
        
        model.addAttribute("listaHosts", filteredHosts);
        
        return "admin/search";
    }

    @GetMapping("/create")
    public String showCreatePage(Model model) {
        model.addAttribute("createHostDTO", new CreateHostDTO());
        return "admin/create";
    }

    // --- ENDPOINTS DA API RESTful PARA OPERAÇÕES CRUD ---

    /**
     * Endpoint para criar um novo host.
     * Usado pelo JavaScript via fetch.
     */
    @PostMapping("/api/hosts")
    @ResponseBody
    public ResponseEntity<?> createHost(@RequestBody CreateHostDTO createDto) {
        try {
            Host novoHost = hostService.createAndValidateHost(createDto);
            return ResponseEntity.status(HttpStatus.CREATED).body(novoHost);

        } catch (ZabbixValidationException e) {
            // Erro de validação customizado (ex: host não encontrado no Zabbix)
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));

        } catch (DataIntegrityViolationException e) {
            // NOVO CATCH: Captura o erro de duplicidade do banco de dados
            String errorMessage = "Erro de integridade dos dados.";
            if (e.getMessage().contains("hosts.zabbix_id")) {
                errorMessage = "Já existe um host cadastrado com este Zabbix ID.";
            }
            // Retorna HTTP 409 Conflict, que é o status ideal para duplicidade
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", errorMessage));

        } catch (Exception e) {
            // Erro genérico para qualquer outra falha
            return ResponseEntity.internalServerError().body(Map.of("error", "Ocorreu um erro inesperado."));
        }
    }
    
    /**
     * Busca um host específico por ID e retorna um DTO completo
     * com os dados do host, a lista de métricas individuais (para Read)
     * e a lista de checkboxes agrupados (para Update).
     * Rota: GET /admin/api/hosts/{id}
     */
    @GetMapping("/api/hosts/{id}")
    @ResponseBody
    public ResponseEntity<HostDetailsDTO> getHostById(@PathVariable Long id) {
        try {
            HostDetailsDTO hostDetails = hostService.getHostDetailsForUpdate(id);
            return ResponseEntity.ok(hostDetails);
        } catch (EntityNotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }
    
    /**
     * Endpoint para listar todos os hosts cadastrados.
     * Rota: GET /admin/api/hosts
     */
    @GetMapping("/api/hosts")
    @ResponseBody
    public ResponseEntity<List<Host>> getAllHosts() {
        try {
            // 1. Chama o service para buscar todos os hosts no banco de dados.
            List<Host> hosts = hostService.findAll();
            
            // 2. Retorna HTTP 200 OK com a lista de hosts no corpo da resposta.
            return ResponseEntity.ok(hosts);
            
        } catch (Exception e) {
            // Em caso de um erro inesperado no banco de dados, retorna um erro 500.
            System.err.println("Erro ao buscar todos os hosts: " + e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Endpoint para atualizar um host existente.
     * Recebe dados JSON via JavaScript (fetch).
     */
    @PutMapping("/api/hosts/{hostId}")
    public ResponseEntity<Void> updateHost(@PathVariable Long hostId, @RequestBody UpdateHostDTO updateData) {
        try {
            hostService.updateHost(hostId, updateData);
            return ResponseEntity.ok().build(); // Retorna 200 OK
        } catch (EntityNotFoundException e) {
            return ResponseEntity.notFound().build(); // Retorna 404 Not Found
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build(); // Retorna 500 Internal Server Error
        }
    }

    /**
     * Endpoint para deletar um host.
     * Chamado via JavaScript (fetch).
     */
    @DeleteMapping("/api/hosts/{hostId}")
    public ResponseEntity<Void> deleteHost(@PathVariable Long hostId) {
        try {
            hostService.deleteHost(hostId);
            return ResponseEntity.ok().build(); // Retorna 200 OK
        } catch (Exception e) {
            // Pode ser um EntityNotFoundException ou outro erro
            return ResponseEntity.internalServerError().build(); // Retorna 500
        }
    }
}