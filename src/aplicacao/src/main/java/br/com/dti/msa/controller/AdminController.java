package br.com.dti.msa.controller;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import br.com.dti.msa.model.Host;
import br.com.dti.msa.model.Metric;
import br.com.dti.msa.dto.UpdateHostDTO;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;


@Controller
@RequestMapping("/admin")
public class AdminController {
    private static final List<Metric> sigaaMetrics = List.of(
        new Metric(1L, "system.cpu.util", "Uso de CPU", "%"),
        new Metric(2L, "vm.memory.utilization", "Uso de Memória", "%")
    );

    private static final List<Metric> seiSipMetrics = List.of(
        new Metric(3L, "system.cpu.util", "Uso de CPU", "%"),
        new Metric(4L, "svm.memory.utilization", "Uso de Memória RAM", "%"),
        new Metric(5L, "vfs.fs.size[/,total]", "Uso de Armazenamento", "%")
    );

    private static final List<Metric> sigaaBDMetrics = List.of(
        new Metric(3L, "system.cpu.util", "Uso de CPU", "%"),
        new Metric(4L, "vm.memory.utilization", "Uso de Memória RAM", "%"),
        new Metric(5L, "vfs.fs.size[/,total]", "Uso de Armazenamento", "%"),
        new Metric(6L, "net.if.in['eth0']", "Banda Larga Recebimento de Dados", "")
    );

    private static final List<Host> mockHostDatabase = List.of(
        new Host(123L, "10675awdsa", 10675, "Sigaa", "Aplicação SIGAA", "app", sigaaMetrics),
        new Host(124L, "10676qweqw", 10676, "SEI-SIP-01", "Aplicação SEI-SIP-01", "server", seiSipMetrics),
        new Host(125L, "10677asdas", 10677, "BD_SIGAA", "Banco de Dados do SIGAA", "db", sigaaBDMetrics)
    );
    
    @GetMapping({"","/"})
    public String index() {
        return "admin/index";
    }

    @GetMapping({"search"})
    public String indexSearch( Model model) {

        // Hosts Mockados
        model.addAttribute("listaHosts", new ArrayList<Host>());

        model.addAttribute("initialLoad", true);

        return "admin/search";
    }

     @PostMapping("/search/host")
    public String processSearch(
        @RequestParam(value = "inputSearch", required = false) String searchTerm,
        @RequestParam(value = "app", required = false) String typeApp,
        @RequestParam(value = "server", required = false) String typeServer,
        @RequestParam(value = "db", required = false) String typeDb,
        Model model
    ) {
        // 1. Cria uma stream a partir da nossa base de dados mockada
        Stream<Host> hostStream = mockHostDatabase.stream();

        // 2. Filtra pelo termo de busca (se não for nulo ou vazio)
        if (searchTerm != null && !searchTerm.trim().isEmpty()) {
            hostStream = hostStream.filter(host -> 
                host.getName().toLowerCase().contains(searchTerm.toLowerCase()) ||
                host.getDescription().toLowerCase().contains(searchTerm.toLowerCase())
            );
        }

        // 3. Monta uma lista com os tipos selecionados
        List<String> selectedTypes = new ArrayList<>();
        if (typeApp != null) selectedTypes.add(typeApp);
        if (typeServer != null) selectedTypes.add(typeServer);
        if (typeDb != null) selectedTypes.add(typeDb);

        // 4. Filtra pelos tipos (se algum foi selecionado)
        if (!selectedTypes.isEmpty()) {
            hostStream = hostStream.filter(host -> selectedTypes.contains(host.getType()));
        }

        // 5. Coleta os resultados
        List<Host> filteredHosts = hostStream.collect(Collectors.toList());

        // 6. Envia a lista FILTRADA de volta para o template
        model.addAttribute("listaHosts", filteredHosts);

        // 7. Renderiza a MESMA página, mas agora com os dados filtrados
        return "admin/search";
    }

    @GetMapping({"create"})
    public String indexCreate() {
        return "admin/create";
    }

    // --- API Restful ---
    
    /**
     * Cria um novo host.
     * Rota: POST /admin/api/hosts
     */
    @PostMapping("api/host")
    public String createHost(
        @RequestParam("hostName") String hostName,
        @RequestParam("hostZabbixID") Long zabbixId,
        @RequestParam("hostDescription") String description,
        @RequestParam("hostType") String hostType,
        // Usamos um Map para pegar todos os outros parâmetros (os checkboxes)
        @RequestParam Map<String, String> allParams
    ) {
        
        // Filtra o mapa para pegar apenas os checkboxes (que não são os campos principais)
        List<String> enabledMetrics = allParams.keySet().stream()
            .filter(key -> !key.equals("_csrf") && 
                        !key.equals("hostName") && 
                        !key.equals("hostZabbixID") &&
                        !key.equals("hostDescription") &&
                        !key.equals("hostType"))
            .collect(Collectors.toList());

        System.out.println("Nome do Host: " + hostName);
        System.out.println("Zabbix ID: " + zabbixId);
        System.out.println("Descrição: " + description);
        System.out.println("Tipo do Host: " + hostType);
        System.out.println("Métricas Selecionadas: " + enabledMetrics);


        return "redirect:/admin/create";
    }

    /**
     * Busca um host específico por ID (usado para popular o modal de update).
     * Rota: GET /admin/api/hosts/{id}
     */
    @GetMapping("/api/hosts/{id}")
    @ResponseBody
    public ResponseEntity<Host> getHostById(@PathVariable Long id) {
        System.out.println("Buscando host com ID (mock): " + id);

        // 3. Simula a busca no banco de dados (procurando na nossa lista estática)
        Optional<Host> hostEncontrado = mockHostDatabase.stream()
                .filter(host -> host.getId().equals(id))
                .findFirst();

        // 4. Retorna o host se encontrado, ou um erro 404 (Not Found) se não encontrado
        if (hostEncontrado.isPresent()) {
            // Retorna HTTP 200 OK com o objeto host no corpo da resposta
            return ResponseEntity.ok(hostEncontrado.get());
        } else {
            // Retorna HTTP 404 Not Found, que é a resposta correta quando o recurso não existe
            return ResponseEntity.notFound().build();
        }
    }
    
    
    /**
     * Atualiza um host existente.
     * Rota: PUT /admin/api/hosts/{id}
     */
    @PutMapping("/api/hosts/{hostId}")
    public ResponseEntity<Void> updateHost(
        @PathVariable Long hostId, 
        @RequestBody UpdateHostDTO updateData
    ) {
        System.out.println("--- ATUALIZANDO HOST ---");
        System.out.println("Host ID (da URL): " + hostId);
        System.out.println("Novo Nome: " + updateData.getHostName());
        System.out.println("Novo Zabbix ID: " + updateData.getHostZabbixID());
        System.out.println("Novo Tipo: " + updateData.getHostType());
        System.out.println("Novas Métricas: " + updateData.getEnabledMetrics());
        
        // LÓGICA PARA ATUALIZAR NO BANCO...
        return ResponseEntity.ok().build();
    }

    /**
     * Deleta um host.
     * Rota: DELETE /admin/api/hosts/{id}
     */
    @DeleteMapping("/api/hosts/{hostId}")
    public ResponseEntity<Void> deleteHost(@PathVariable Long hostId) {
        System.out.println("--- DELETANDO HOST ---");
        System.out.println("Host ID para deletar: " + hostId);
        
        // LÓGICA PARA DELETAR DO BANCO...
        return ResponseEntity.ok().build();
    }
}
