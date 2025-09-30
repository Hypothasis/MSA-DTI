package br.com.dti.msa.controller;

import br.com.dti.msa.dto.CreateHostDTO;
import br.com.dti.msa.dto.UpdateHostDTO;
import br.com.dti.msa.exception.ZabbixValidationException;
import br.com.dti.msa.model.Host;
import br.com.dti.msa.service.HostService;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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
        @RequestParam Map<String, String> allParams,
        Model model
    ) {
        // Filtra os parâmetros para pegar apenas os tipos de host selecionados no formulário
        List<String> selectedTypes = allParams.keySet().stream()
            .filter(key -> key.equals("app") || key.equals("server") || key.equals("db"))
            .collect(Collectors.toList());
        
        // Chama o SERVICE para fazer a busca real no banco de dados
        List<Host> filteredHosts = hostService.searchHosts(searchTerm, selectedTypes);
        
        // Adiciona os resultados ao model para o Thymeleaf renderizar
        model.addAttribute("listaHosts", filteredHosts);
        
        // Retorna a mesma view, mas agora com os dados da busca
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
     * Recebe dados de um formulário web padrão.
     */
    @PostMapping("api/hosts") // Rota corresponde ao th:action do formulário de criação
    public String createHost(CreateHostDTO createDto, RedirectAttributes redirectAttributes) {
        try {
            hostService.createAndValidateHost(createDto);
            redirectAttributes.addFlashAttribute("successMessage", "Host '" + createDto.getHostName() + "' criado com sucesso!");
            return "admin/create";  // Redireciona para a busca para ver o novo host
        } catch (ZabbixValidationException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "admin/create";  // Volta para a criação com a mensagem de erro
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Ocorreu um erro inesperado: " + e.getMessage());
            return "admin/create"; 
        }
    }

    /**
     * Endpoint para buscar os dados completos de um host por ID.
     * Usado pelo JavaScript para popular os modais de Read e Update.
     */
    @GetMapping("/api/hosts/{id}")
    @ResponseBody // Indica que o retorno é um corpo de resposta (JSON), não o nome de uma view
    public ResponseEntity<Host> getHostById(@PathVariable Long id) {
        try {
            Host host = hostService.findById(id);
            return ResponseEntity.ok(host);
        } catch (EntityNotFoundException e) {
            return ResponseEntity.notFound().build();
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