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
     * Recebe dados de um formulário web padrão.
     */
    @PostMapping("/api/hosts")
    public String createHost(CreateHostDTO createDto, RedirectAttributes redirectAttributes) {
        try {
            hostService.createAndValidateHost(createDto);
            redirectAttributes.addFlashAttribute("successMessage", "Host '" + createDto.getHostName() + "' criado com sucesso!");
            // Em caso de sucesso, o ideal é redirecionar para a busca
            return "redirect:/admin/create";
            
        } catch (ZabbixValidationException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            // CORREÇÃO: Adicione a barra "/" inicial
            return "redirect:/admin/create";
            
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Ocorreu um erro inesperado: " + e.getMessage());
            // CORREÇÃO: Adicione a barra "/" inicial
            return "redirect:/admin/create";
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