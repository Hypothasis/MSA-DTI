package br.com.dti.msa.service;

import br.com.dti.msa.dto.AdminDashboardDTO;
import br.com.dti.msa.dto.CreateHostDTO;
import br.com.dti.msa.dto.HomepageHostDTO;
import br.com.dti.msa.dto.HostDashboardDTO;
import br.com.dti.msa.dto.HostDetailsDTO;
import br.com.dti.msa.dto.HostSearchResultDTO;
import br.com.dti.msa.dto.PublicHostStatusDTO;
import br.com.dti.msa.dto.UpdateHostDTO;
import br.com.dti.msa.exception.ZabbixValidationException;
import br.com.dti.msa.integration.zabbix.dto.ZabbixClient;
import br.com.dti.msa.model.DefaultZabbixKey;
import br.com.dti.msa.model.Host;
import br.com.dti.msa.model.HostMetricConfig;
import br.com.dti.msa.model.Metric;
import br.com.dti.msa.model.MetricCurrentValue;
import br.com.dti.msa.model.MetricHistory;
import br.com.dti.msa.model.RecentEvents;
import br.com.dti.msa.repository.DefaultZabbixKeyRepository;
import br.com.dti.msa.repository.HostRepository;
import br.com.dti.msa.repository.MetricCurrentValueRepository;
import br.com.dti.msa.repository.MetricHistoryRepository;
import br.com.dti.msa.repository.MetricRepository;
import br.com.dti.msa.repository.RecentEventsRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class HostService {

    @Autowired private ZabbixClient zabbixClient;
    @Autowired private MetricCatalog metricCatalog;
    @Autowired private HostRepository hostRepository;
    @Autowired private MetricRepository metricRepository;
    @Autowired private MetricHistoryRepository metricHistoryRepository;
    @Autowired private RecentEventsRepository recentEventsRepository;
    @Autowired private DefaultZabbixKeyRepository defaultZabbixKeyRepository;

    /**
     * Retorna todos os hosts cadastrados.
     */
    public List<Host> findAll() {
        return hostRepository.findAll();
    }

    /**
     * Busca um host pelo seu ID. Lança uma exceção se não for encontrado.
     * AGORA USA A QUERY OTIMIZADA.
     */
    public Host findById(Long id) {
        // Altera a chamada para usar o novo método do repositório
        return hostRepository.findByIdWithFullMetrics(id) 
                .orElseThrow(() -> new EntityNotFoundException("Host não encontrado com ID: " + id));
    }
    
    /**
     * Busca um host pelo PublicID. Lança uma exceção se não for encontrado.
     * AGORA USA A QUERY OTIMIZADA.
     */
    public Host findByPublicId(String publicId) {
        // Altera a chamada para usar o novo método do repositório
        return hostRepository.findByPublicIdWithFullMetrics(publicId) 
                .orElseThrow(() -> new EntityNotFoundException("Host não encontrado com ID Público: " + publicId));
    }

    /**
     * Busca 5 hosts pelo nome, usado no Home
     */
    public List<HostSearchResultDTO> searchPublicHostsByName(String term) {
        if (term == null || term.trim().length() < 2) {
            return List.of(); // Retorna lista vazia se o termo for muito curto
        }
        
        List<Host> hosts = hostRepository.findTop5ByNameContainingIgnoreCase(term);
        
        // Mapeia a lista de entidades para a lista de DTOs leves
        return hosts.stream()
                    .map(host -> new HostSearchResultDTO(host.getPublicId(), host.getName()))
                    .collect(Collectors.toList());
    }
    
    /**
     * Obtém todos os dados dinâmicos (métricas) para o dashboard do host.
     */
    public HostDashboardDTO getHostDashboardData(String publicId) {
        Host host = findByPublicId(publicId);
        
        HostDashboardDTO dto = new HostDashboardDTO();
        dto.setName(host.getName());
        dto.setDescription(host.getDescription());
        dto.setType(host.getType());
        
        LocalDateTime startTime48h = LocalDateTime.now().minusHours(48);

        // Lista com as chaves individuais configuradas para o host (ex: "cpu-uso", "memoria-ram-total")
        Set<String> configuredMetricKeys = host.getMetricConfigs().stream()
                                            .map(config -> config.getMetric().getMetricKey())
                                            .collect(Collectors.toSet());

        // --- CÁLCULO DE STATUS ---
        Host.HostStatus statusEnum = host.getStatus(); // Pega o status salvo pelo Coletor

        String statusString;
        switch (statusEnum) {
            case ACTIVE:
                statusString = "OK";
                break;
            case ALERT:
                statusString = "ALERT";
                break;
            case INACTIVE:
            default:
                statusString = "PROBLEM";
                break;
        }
        dto.setStatus(statusString); // Define o status no DTO (tradução correta)

        // --- PREENCHIMENTO DO DTO ---

        // 1. Lógica de Prioridade para Disponibilidade ESPECÍFICA (Gráfico de 96 pontos)
        String specificKeyToUse = getPriorityKey(configuredMetricKeys, 
            "disponibilidade-especifica-health", 
            "disponibilidade-especifica-http-agente", 
            "disponibilidade-especifica"
        );
        if (specificKeyToUse != null) {
            List<HostDashboardDTO.MetricValueDTO> history = fetchMetricHistory(host.getId(), specificKeyToUse, startTime48h);
            history.forEach(point -> point.setY(point.getY() * 100.0)); // Converte 1.0 para 100.0
            dto.setAvailabilityHistory(history);
        }

        // 2. Lógica de Prioridade para Disponibilidade GLOBAL (Cards de 4 períodos)
        String globalKeyToUse = getPriorityKey(configuredMetricKeys,
            "disponibilidade-global-health",
            "disponibilidade-global-http-agente",
            "disponibilidade-global"
        );

        if (globalKeyToUse != null) {
            dto.setGlobalAvailability(calculateGlobalAvailability(host.getId(), globalKeyToUse));
        }

        if (configuredMetricKeys.contains("latencia")) {
            dto.setLatencyHistory(fetchMetricHistory(host.getId(), "latencia", startTime48h));
        }

        if (configuredMetricKeys.contains("cpu-uso")) {
            dto.setCpuUsageHistory(fetchMetricHistory(host.getId(), "cpu-uso", startTime48h));
        }

        if (configuredMetricKeys.contains("cpu-troca-contextos")) {
            dto.setCpuContextSwitchesHistory(fetchMetricHistory(host.getId(), "cpu-troca-contextos", startTime48h));
        }

        // 2. Métricas de Valor Único, Agregado ou Composto
        // Lógica para Sistema Operacional (Correto - desde que 'findLastValueAsString' seja corrigido)
        if (configuredMetricKeys.containsAll(metricCatalog.getMetricKeysForCheckbox("sistema-operacional"))) {
            findLastValueAsString(host.getId(), "os-nome").ifPresent(osName -> 
                findLastValueAsString(host.getId(), "os-arch").ifPresent(arch -> {
                    dto.setOsInfo(new HostDashboardDTO.OsInfoDTO(osName, arch));
                })
            );
        }

        // Lógica para Processos da CPU (Composto)
        if (configuredMetricKeys.containsAll(metricCatalog.getMetricKeysForCheckbox("cpu-processos"))) {
            
            List<HostDashboardDTO.MetricValueDTO> currentProcessesHistory = 
                fetchMetricHistory(host.getId(), "cpu-processos-atuais", startTime48h);

            List<HostDashboardDTO.MetricValueDTO> maxProcessesHistory = 
                fetchMetricHistory(host.getId(), "cpu-processos-max", startTime48h);

            dto.setProcessInfoHistory(
                new HostDashboardDTO.ProcessInfoHistoryDTO(currentProcessesHistory, maxProcessesHistory)
            );
        }
        
        // Lógica para Memória RAM (Composto)
        if (configuredMetricKeys.containsAll(metricCatalog.getMetricKeysForCheckbox("memoria-ram"))) {
            findLastValue(host.getId(), "memoria-ram-total").ifPresent(total -> 
                findLastValue(host.getId(), "memoria-ram-disponivel").ifPresent(available -> {
                    dto.setMemoryData(createStorageDtoFromAvailable(total, available));
                })
            );
        }
        
        // Lógica para Memória SWAP (Composto)
        if (configuredMetricKeys.containsAll(metricCatalog.getMetricKeysForCheckbox("memoria-swap"))) {
            findLastValue(host.getId(), "memoria-swap-total").ifPresent(total -> 
                findLastValue(host.getId(), "memoria-swap-livre").ifPresent(free -> {
                    dto.setSwapData(createStorageDto(total, total - free));
                })
            );
        }

        // Lógica para Armazenamento (Composto)
        if (configuredMetricKeys.containsAll(metricCatalog.getMetricKeysForCheckbox("armazenamento"))) {
        // Busca dados da partição ROOT (/)
        findLastValue(host.getId(), "armazenamento-root-total").ifPresent(total -> 
            findLastValue(host.getId(), "armazenamento-root-usado").ifPresent(used -> {
                dto.setStorageRootData(createStorageDto(total, used));
            })
        );
        // Busca dados da partição BOOT (/boot)
        findLastValue(host.getId(), "armazenamento-boot-total").ifPresent(total -> 
            findLastValue(host.getId(), "armazenamento-boot-usado").ifPresent(used -> {
                dto.setStorageBootData(createStorageDto(total, used));
            })
        );
    }

        // Lógica para Banda Larga (Composto)
        if (configuredMetricKeys.containsAll(metricCatalog.getMetricKeysForCheckbox("dados-banda-larga"))) {
            dto.setDataBandwidthInHistory(fetchMetricHistory(host.getId(), "dados-entrada", startTime48h));
            dto.setDataBandwidthOutHistory(fetchMetricHistory(host.getId(), "dados-saida", startTime48h));
        }
        
        // Lógica para Tempo Ativo (Simples)
        if (configuredMetricKeys.contains("tempo-ativo")) {
            findLastValue(host.getId(), "tempo-ativo").ifPresent(uptimeInSeconds -> {
                dto.setUptime(formatUptime(uptimeInSeconds.longValue()));
            });
        }

        // Lógica para Eventos Recentes
        if (configuredMetricKeys.contains("eventos-recentes")) {
            
            // Define as severidades que você quer mostrar (ex: "Alerta" ou superior)
            List<String> severitiesToShow = List.of("2", "3", "4", "5"); 
            Pageable pageable = PageRequest.of(0, 5); // Pede os 5 primeiros

            // Chama o novo método do repositório
            List<RecentEvents> events = recentEventsRepository.findRecentCriticalEventsForHost(
                host.getId(),
                severitiesToShow,
                pageable
            );
            
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy 'às' HH:mm");
            List<HostDashboardDTO.EventDTO> eventDtos = events.stream()
                .map(event -> {
                    HostDashboardDTO.EventDTO eventDto = new HostDashboardDTO.EventDTO();
                    eventDto.setSeverity(event.getSeverity());
                    eventDto.setName(event.getName());
                    eventDto.setTimestamp(event.getTimestamp().format(formatter));
                    return eventDto;
                })
                .collect(Collectors.toList());
                
            dto.setRecentEvents(eventDtos);
        }
        
        return dto;
    } 

    /**
     * Obtém uma lista de TODOS os hosts com seus status, disponibilidade global
     * e histórico de disponibilidade para a exibição pública.
     * Esta versão foi refatorada para suportar múltiplos tipos de métricas de disponibilidade.
     */
    @Transactional(readOnly = true)
    public List<PublicHostStatusDTO> getPublicHostStatuses() {
        
        List<Host> allHosts = hostRepository.findAllWithMetrics();
        LocalDateTime startTime48h = LocalDateTime.now().minusHours(48);
        
        return allHosts.stream()
            .map(host -> {
                // 1. Cria o DTO básico
                PublicHostStatusDTO dto = new PublicHostStatusDTO(host);
                
                // 2. Pega o conjunto de TODAS as chaves de métrica configuradas para este host
                // (Ex: "cpu-uso", "memoria-ram-total", "disponibilidade-global-health", etc.)
                Set<String> configuredMetricKeys = host.getMetricConfigs().stream()
                    .map(config -> config.getMetric().getMetricKey())
                    .collect(Collectors.toSet());

                // 3. Lógica de Prioridade para Disponibilidade GLOBAL (os 4 cards)
                String globalKeyToUse = null;
                if (configuredMetricKeys.contains("disponibilidade-global-health")) {
                    globalKeyToUse = "disponibilidade-global-health";
                } else if (configuredMetricKeys.contains("disponibilidade-global-http-agente")) {
                    globalKeyToUse = "disponibilidade-global-http-agente";
                } else if (configuredMetricKeys.contains("disponibilidade-global")) {
                    globalKeyToUse = "disponibilidade-global";
                }

                // 4. Se uma chave de disponibilidade global foi encontrada, calcula os dados
                if (globalKeyToUse != null) {
                    dto.setGlobalAvailability(calculateGlobalAvailability(host.getId(), globalKeyToUse));
                }

                // 5. Lógica de Prioridade para Disponibilidade ESPECÍFICA (o gráfico de 96 pontos)
                String specificKeyToUse = null;
                if (configuredMetricKeys.contains("disponibilidade-especifica-health")) {
                    specificKeyToUse = "disponibilidade-especifica-health";
                } else if (configuredMetricKeys.contains("disponibilidade-especifica-http-agente")) {
                    specificKeyToUse = "disponibilidade-especifica-http-agente";
                } else if (configuredMetricKeys.contains("disponibilidade-especifica")) {
                    specificKeyToUse = "disponibilidade-especifica";
                }

                // 6. Se uma chave de disponibilidade específica foi encontrada, busca o histórico
                if (specificKeyToUse != null) {
                    List<MetricHistory> historyData = metricHistoryRepository.findByHostIdAndMetricMetricKeyAndTimestampAfterOrderByTimestampAsc(
                        host.getId(), 
                        specificKeyToUse, 
                        startTime48h
                    );
                    
                    List<HostDashboardDTO.MetricValueDTO> history = historyData.stream()
                        .map(h -> new HostDashboardDTO.MetricValueDTO(h.getTimestamp(), h.getValue()))
                        .collect(Collectors.toList());
                    
                    dto.setAvailabilityHistory(history);
                }
                
                return dto;
            })
            .collect(Collectors.toList());
    }

    /**
     * Obtém status do host, junto com dados do availabilityGlobal para exibição pública (Home).
     * Refatorado para suportar múltiplos tipos de disponibilidade e a nova arquitetura.
     */
    public List<HomepageHostDTO> getHomepageHosts() {
        
        // 1. Busca todos os hosts
        List<Host> allHosts = hostRepository.findAll();
        LocalDateTime startTime48h = LocalDateTime.now().minusHours(48);
        
        return allHosts.stream()
            // 2. Filtra apenas os que NÃO estão com status ACTIVE
            .filter(host -> host.getStatus() != Host.HostStatus.ACTIVE)
            // 3. Mapeia para o DTO e busca os dados de disponibilidade para cada um
            .map(host -> {
                HomepageHostDTO dto = new HomepageHostDTO(host);
                
                // 4. Pega o conjunto de TODAS as chaves de métrica configuradas para este host
                Set<String> configuredMetricKeys = host.getMetricConfigs().stream()
                    .map(config -> config.getMetric().getMetricKey())
                    .collect(Collectors.toSet());

                // 5. Lógica de Prioridade para Disponibilidade GLOBAL (o card de %)
                String globalKeyToUse = null;
                if (configuredMetricKeys.contains("disponibilidade-global-health")) {
                    globalKeyToUse = "disponibilidade-global-health";
                } else if (configuredMetricKeys.contains("disponibilidade-global-http-agente")) {
                    globalKeyToUse = "disponibilidade-global-http-agente";
                } else if (configuredMetricKeys.contains("disponibilidade-global")) {
                    globalKeyToUse = "disponibilidade-global";
                }

                if (globalKeyToUse != null) {
                    // Calcula a disponibilidade (o método calculateAvailability já trata nulos)
                    Double availability = metricHistoryRepository.calculateAvailability(host.getId(), globalKeyToUse, startTime48h);
                    dto.setGlobalAvailability48h(availability != null ? availability : 0.0);
                }

                // 6. Lógica de Prioridade para Disponibilidade ESPECÍFICA (o gráfico de 96 pontos)
                String specificKeyToUse = null;
                if (configuredMetricKeys.contains("disponibilidade-especifica-health")) {
                    specificKeyToUse = "disponibilidade-especifica-health";
                } else if (configuredMetricKeys.contains("disponibilidade-especifica-http-agente")) {
                    specificKeyToUse = "disponibilidade-especifica-http-agente";
                } else if (configuredMetricKeys.contains("disponibilidade-especifica")) {
                    specificKeyToUse = "disponibilidade-especifica";
                }

                if (specificKeyToUse != null) {
                    // Busca o histórico bruto (NÃO agregado)
                    List<MetricHistory> historyData = metricHistoryRepository.findByHostIdAndMetricMetricKeyAndTimestampAfterOrderByTimestampAsc(
                        host.getId(), 
                        specificKeyToUse, 
                        startTime48h
                    );
                    
                    List<HostDashboardDTO.MetricValueDTO> history = historyData.stream()
                        .map(h -> new HostDashboardDTO.MetricValueDTO(h.getTimestamp(), h.getValue()))
                        .collect(Collectors.toList());
                    
                    dto.setAvailabilityHistory(history);
                }
                
                return dto;
            })
            .collect(Collectors.toList());
    }

    /**
     * Obtém status do host para o dashboard admin.
     */
    @Transactional(readOnly = true)
    public AdminDashboardDTO getAdminDashboardStats() {
        AdminDashboardDTO dto = new AdminDashboardDTO();

        // 1. Preenche os KPIs
        long totalHosts = hostRepository.count();
        long activeHosts = hostRepository.countByStatus(Host.HostStatus.ACTIVE);
        long alertHosts = hostRepository.countByStatus(Host.HostStatus.ALERT);
        long inactiveHosts = hostRepository.countByStatus(Host.HostStatus.INACTIVE);

        dto.setTotalHosts(totalHosts);
        dto.setActiveHosts(activeHosts);
        dto.setAlertHosts(alertHosts);
        dto.setInactiveHosts(inactiveHosts);

        // ===================================================================
        // CORREÇÃO: Calcula a disponibilidade com base nos KPIs
        // ===================================================================
        if (totalHosts > 0) {
            // Calcula a porcentagem de hosts que estão (Ativos OU em Alerta)
            double availableHosts = (double) (activeHosts + alertHosts);
            double availabilityPercent = (availableHosts / totalHosts) * 100.0;
            
            // Arredonda para 2 casas decimais para ficar bonito no gráfico
            BigDecimal bd = new BigDecimal(Double.toString(availabilityPercent));
            bd = bd.setScale(2, RoundingMode.HALF_UP);
            dto.setOverallAvailability(bd.doubleValue());
        } else {
            dto.setOverallAvailability(100.0); // Se não há hosts, a disponibilidade é 100%
        }
        // ===================================================================

        // 3. Busca Últimos Alertas Críticos (com JOIN FETCH)
        // (Esta parte do seu código já estava correta)
        List<String> criticalSeverities = List.of("3", "4", "5");
        Pageable pageable = PageRequest.of(0, 5); 
        List<RecentEvents> alerts = recentEventsRepository.findRecentCriticalEvents(criticalSeverities, pageable);
        
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM HH:mm");
        dto.setLatestAlerts(alerts.stream().map(event -> {
            AdminDashboardDTO.RecentEventDTO eventDto = new AdminDashboardDTO.RecentEventDTO();
            eventDto.setHostName(event.getHost().getName()); 
            eventDto.setEventName(event.getName());
            eventDto.setSeverity(event.getSeverity());
            eventDto.setTimestamp(event.getTimestamp().format(formatter));
            return eventDto;
        }).collect(Collectors.toList()));

        // 4. Busca Top Hosts com Problemas
        // (Esta parte do seu código também estava correta)
        List<Host.HostStatus> problemStatuses = List.of(Host.HostStatus.ALERT, Host.HostStatus.INACTIVE);
        List<Host> problemHosts = hostRepository.findByStatusIn(problemStatuses);
        dto.setTopProblemHosts(problemHosts.stream().limit(5).map(host -> {
            AdminDashboardDTO.ProblematicHostDTO hostDto = new AdminDashboardDTO.ProblematicHostDTO();
            hostDto.setPublicId(host.getPublicId());
            hostDto.setName(host.getName());
            hostDto.setStatus(host.getStatus());
            hostDto.setDescription(host.getDescription());
            return hostDto;
        }).collect(Collectors.toList()));

        return dto;
    }
    
    /**
     * Busca um host e retorna um DTO com os nomes dos checkboxes habilitados.
     */
    @Transactional(readOnly = true) // Adiciona @Transactional para garantir a sessão
    public HostDetailsDTO getHostDetailsForUpdate(Long hostId) {
        // 1. Esta chamada agora retorna o Host com as métricas já carregadas
        Host host = findById(hostId); 

        // 2. Pega as chaves individuais (ex: "memoria-ram-total")
        List<String> savedMetricKeys = host.getMetricConfigs().stream() 
                                            .map(config -> config.getMetric().getMetricKey()) // <-- ESTA LINHA AGORA FUNCIONA
                                            .collect(Collectors.toList());

        // 3. Traduz de volta para os nomes dos checkboxes (ex: "memoria-ram")
        List<String> enabledCheckboxes = metricCatalog.getCheckboxesForMetricKeys(savedMetricKeys);

        // 4. Retorna o DTO completo
        return new HostDetailsDTO(host, enabledCheckboxes);
    }

    /**
     * 
     * Realiza uma busca filtrando por termo e/ou por tipos de host.
     */
    public List<Host> searchHosts(String term, List<String> types) {
        // Garante que a lista de tipos seja nula se estiver vazia, para a query JPQL funcionar
        List<String> effectiveTypes = (types == null || types.isEmpty()) ? null : types;
        String effectiveTerm = (term == null || term.trim().isEmpty()) ? null : term;

        return hostRepository.search(effectiveTerm, effectiveTypes);
    }

    /**
     * Valida os dados contra o Zabbix e cria um novo host no banco de dados.
     * Esta versão está alinhada com a arquitetura de 3 tabelas (Host/Metric/HostMetricConfig).
     */
    @Transactional
    public Host createAndValidateHost(CreateHostDTO dto) throws ZabbixValidationException {
        // 1. VALIDAÇÃO DO HOST NO ZABBIX
        if (!zabbixClient.hostExists(dto.getHostZabbixID())) {
            throw new ZabbixValidationException("Host com Zabbix ID '" + dto.getHostZabbixID() + "' não encontrado no Zabbix.");
        }

        if (hostRepository.existsByZabbixId(dto.getHostZabbixID())) {
            throw new ZabbixValidationException("Já existe um host cadastrado no MSA com este Zabbix ID.");
        }

        // 2. CRIA A ENTIDADE HOST (ainda não salva)
        Host newHost = new Host();
        newHost.setName(dto.getHostName());
        newHost.setZabbixId(dto.getHostZabbixID());
        newHost.setDescription(dto.getHostDescription());
        newHost.setType(dto.getHostType());
        newHost.setPublicId(UUID.randomUUID().toString());

        // Prepara o set que vai guardar as novas configurações
        Set<HostMetricConfig> newConfigurations = new HashSet<>();
        
        // 3. PROCESSA AS MÉTRICAS SELECIONADAS
        // Itera sobre os NOMES DOS CHECKBOXES que vieram do formulário (ex: "memoria-ram")
        for (String checkboxName : dto.getEnabledMetrics()) {
            
            // a. Traduz o nome do checkbox para as chaves do banco (ex: "memoria-ram-total", "memoria-ram-disponivel")
            List<String> metricKeys = metricCatalog.getMetricKeysForCheckbox(checkboxName);

            if (metricKeys.isEmpty()) {
                throw new ZabbixValidationException("O checkbox '" + checkboxName + "' não foi encontrado no MetricCatalog.");
            }

            // b. Itera sobre as chaves de métrica individuais
            for (String metricKey : metricKeys) {
                
                // c. Busca a entidade Métrica (o "conceito")
                Metric metric = metricRepository.findByMetricKey(metricKey)
                    .orElseThrow(() -> new ZabbixValidationException("Métrica '" + metricKey + "' não encontrada no catálogo do banco."));

                // d. Determina qual Zabbix Key usar
                String zabbixKeyToUse = findZabbixKeyForMetric(metric, checkboxName, dto);

                // e. Valida a chave no Zabbix
                if (zabbixKeyToUse != null && !zabbixKeyToUse.equalsIgnoreCase("zabbix_api")) {
                    if (!zabbixClient.itemExistsOnHost(dto.getHostZabbixID(), zabbixKeyToUse)) {
                        throw new ZabbixValidationException("A chave Zabbix '" + zabbixKeyToUse + 
                            "' (para a métrica '" + metric.getName() + "') não foi encontrada no host.");
                    }
                }

                // f. Cria a entidade de "Contrato" (a "cola")
                HostMetricConfig config = new HostMetricConfig(newHost, metric, zabbixKeyToUse);
                newConfigurations.add(config);
            }
        }
        
        System.out.println("Validações OK! Salvando o host e " + newConfigurations.size() + " configurações de métrica...");

        // 4. PERSISTÊNCIA NO BANCO DE DADOS
        // Associa o Set de "contratos" ao Host
        newHost.getMetricConfigs().addAll(newConfigurations);
        
        // Salva o host. Devido ao "cascade = CascadeType.ALL" na entidade Host,
        // o JPA salvará automaticamente todas as 'HostMetricConfig' associadas.
        return hostRepository.save(newHost);
    }

    /**
     * Atualiza um host existente com base nos dados fornecidos.
     * Esta versão está alinhada com a arquitetura de 3 tabelas (Host/Metric/HostMetricConfig).
     */
    @Transactional
    public Host updateHost(Long hostId, UpdateHostDTO dto) throws ZabbixValidationException {
        
        // 1. Busca o host existente no banco (usando a query otimizada)
        Host existingHost = findById(hostId); // findById já chama findByIdWithFullMetrics

        // 2. Atualiza os campos básicos
        existingHost.setName(dto.getHostName());
        existingHost.setZabbixId(dto.getHostZabbixID());
        existingHost.setDescription(dto.getHostDescription());
        existingHost.setType(dto.getHostType());

        // 3. CONVERTE NOMES DOS CHECKBOXES (DTO) PARA CHAVES DE MÉTRICAS REAIS (BANCO)
        Set<HostMetricConfig> newConfigurations = new HashSet<>();
        for (String checkboxName : dto.getEnabledMetrics()) {
            
            List<String> metricKeys = metricCatalog.getMetricKeysForCheckbox(checkboxName);
            if (metricKeys.isEmpty()) {
                throw new ZabbixValidationException("O checkbox '" + checkboxName + "' não foi encontrado no MetricCatalog.");
            }

            for (String metricKey : metricKeys) {
                Metric metric = metricRepository.findByMetricKey(metricKey)
                    .orElseThrow(() -> new ZabbixValidationException("Métrica '" + metricKey + "' não encontrada no catálogo do banco."));

                // d. Determina qual Zabbix Key usar (usando o DTO de Update)
                String zabbixKeyToUse = findZabbixKeyForMetric(metric, checkboxName, dto);

                // e. Valida a chave no Zabbix
                if (zabbixKeyToUse != null && !zabbixKeyToUse.equalsIgnoreCase("zabbix_api")) {
                    if (!zabbixClient.itemExistsOnHost(dto.getHostZabbixID(), zabbixKeyToUse)) {
                        throw new ZabbixValidationException("A chave Zabbix '" + zabbixKeyToUse + 
                            "' (para a métrica '" + metric.getName() + "') não foi encontrada no host.");
                    }
                }

                HostMetricConfig config = new HostMetricConfig(existingHost, metric, zabbixKeyToUse);
                newConfigurations.add(config);
            }
        }

        // 4. ATUALIZA A COLEÇÃO (remove as antigas, adiciona as novas)
        existingHost.getMetricConfigs().clear();
        existingHost.getMetricConfigs().addAll(newConfigurations);
        
        // 5. Salva o host
        return hostRepository.save(existingHost);
    }

    /**
     * Deleta um host pelo seu ID.
     */
    @Transactional
    public void deleteHost(Long hostId) {
        // Verifica se o host existe antes de deletar para evitar erros
        if (!hostRepository.existsById(hostId)) {
            throw new EntityNotFoundException("Host não encontrado com ID: " + hostId);
        }
        hostRepository.deleteById(hostId);
    }

    // --- MÉTODOS AUXILIARES ---

    /**
     * Método auxiliar para o CREATE.
     * Descobre qual Zabbix Key usar:
     */
    private String findZabbixKeyForMetric(Metric metric, String checkboxName, CreateHostDTO dto) throws ZabbixValidationException {
        
        // Caso 1: Métrica customizada do Health Ready
        if (checkboxName.equals("disponibilidade-global-health") || 
            checkboxName.equals("disponibilidade-especifica-health")) {
            
            String httpMetric = dto.getHealthHttpMetric(); // <-- Busca no campo correto
            if (httpMetric == null || httpMetric.isBlank()) {
                throw new ZabbixValidationException("A Métrica de Disponibilidade Health Ready não pode estar vazia.");
            }
            return httpMetric;
        }

        // Caso 2: Métrica customizada HTTP Genérica
        if (checkboxName.equals("disponibilidade-global-http-agente") || 
            checkboxName.equals("disponibilidade-especifica-http-agente")) {
            
            String httpMetric = dto.getCustomHttpMetric(); // <-- Busca no campo correto
            if (httpMetric == null || httpMetric.isBlank()) {
                throw new ZabbixValidationException("A Métrica de Disponibilidade HTTP customizada não pode estar vazia.");
            }
            return httpMetric;
        }

        // Caso 3: Métrica Padrão (busca no banco)
        return defaultZabbixKeyRepository.findByMetric(metric)
            .map(DefaultZabbixKey::getZabbixKey)
            .orElseThrow(() -> new ZabbixValidationException("A chave Zabbix padrão para '" + metric.getMetricKey() + "' não foi encontrada no banco."));
    }
    
    /**
     * MÉTODO AUXILIAR (Sobrecarga) para o UpdateHostDTO
     */
    private String findZabbixKeyForMetric(Metric metric, String checkboxName, UpdateHostDTO dto) throws ZabbixValidationException {
        
        // Caso 1: Métrica de Eventos
        if (metric.getMetricKey().equals("eventos-recentes")) {
            return "zabbix_api";
        }

        // Caso 2: Métrica customizada do Health
        if (checkboxName.equals("disponibilidade-global-health") || 
            checkboxName.equals("disponibilidade-especifica-health")) {
            
            String httpMetric = dto.getHealthHttpMetric(); // <-- Mudei para 'sigaaHttpMetric'
            if (httpMetric == null || httpMetric.isBlank()) {
                throw new ZabbixValidationException("A Métrica de Disponibilidade Health não pode estar vazia.");
            }
            return httpMetric;
        }

        // Caso 3: Métrica customizada HTTP Genérica
        if (checkboxName.equals("disponibilidade-global-http-agente") || 
            checkboxName.equals("disponibilidade-especifica-http-agente")) {
            
            String httpMetric = dto.getCustomHttpMetric(); 
            if (httpMetric == null || httpMetric.isBlank()) {
                throw new ZabbixValidationException("A Métrica de Disponibilidade HTTP customizada não pode estar vazia.");
            }
            return httpMetric;
        }

        // Caso 4: Métrica Padrão (busca no banco)
        return defaultZabbixKeyRepository.findByMetric(metric)
            .map(DefaultZabbixKey::getZabbixKey)
            .orElseThrow(() -> new ZabbixValidationException("A chave Zabbix padrão para '" + metric.getMetricKey() + "' não foi encontrada no banco."));
    }

    private String getPriorityKey(Set<String> configuredKeys, String key1, String key2, String key3) {
        if (configuredKeys.contains(key1)) return key1;
        if (configuredKeys.contains(key2)) return key2;
        if (configuredKeys.contains(key3)) return key3;
        return null;
    }
    
    private List<HostDashboardDTO.MetricValueDTO> fetchMetricHistory(Long hostId, String metricKey, LocalDateTime startTime) {
        List<MetricHistory> history = metricHistoryRepository.findByHostIdAndMetricMetricKeyAndTimestampAfterOrderByTimestampAsc(hostId, metricKey, startTime);
        return history.stream()
                      .map(h -> new HostDashboardDTO.MetricValueDTO(h.getTimestamp(), h.getValue()))
                      .collect(Collectors.toList());
    }

    private Optional<Double> findLastValue(Long hostId, String metricKey) {
        return metricHistoryRepository.findFirstByHostIdAndMetricMetricKeyOrderByTimestampDesc(hostId, metricKey)
                .map(MetricHistory::getValue);
    }
    
    private Optional<String> findLastValueAsString(Long hostId, String metricKey) {
        // Supondo que o valor para OS seja salvo como String ou um código numérico
        return metricHistoryRepository.findFirstByHostIdAndMetricMetricKeyOrderByTimestampDesc(hostId, metricKey)
                .map(history -> String.valueOf(history.getValue()));
    }

    private HostDashboardDTO.StorageDTO createStorageDto(double totalBytes, double usedBytes) {
        HostDashboardDTO.StorageDTO dto = new HostDashboardDTO.StorageDTO();
        dto.setTotal(bytesToGB(totalBytes));
        dto.setUsed(bytesToGB(usedBytes));
        dto.setFree(dto.getTotal() - dto.getUsed());
        dto.setPercentUsed(totalBytes > 0 ? (usedBytes / totalBytes) * 100 : 0);
        return dto;
    }

    private HostDashboardDTO.StorageDTO createStorageDtoFromAvailable(double totalBytes, double availableBytes) {
        return createStorageDto(totalBytes, totalBytes - availableBytes);
    }
    
    private HostDashboardDTO.AvailabilityDTO calculateGlobalAvailability(Long hostId, String metricKey) {
        HostDashboardDTO.AvailabilityDTO dto = new HostDashboardDTO.AvailabilityDTO();
        Double av48h = metricHistoryRepository.calculateAvailability(hostId, metricKey, LocalDateTime.now().minusHours(48));
        Double av24h = metricHistoryRepository.calculateAvailability(hostId, metricKey, LocalDateTime.now().minusHours(24));
        Double av12h = metricHistoryRepository.calculateAvailability(hostId, metricKey, LocalDateTime.now().minusHours(12));
        Double av6h = metricHistoryRepository.calculateAvailability(hostId, metricKey, LocalDateTime.now().minusHours(6));

        dto.setLast48h(av48h != null ? av48h : 100.0);
        dto.setLast24h(av24h != null ? av24h : 100.0);
        dto.setLast12h(av12h != null ? av12h : 100.0);
        dto.setLast6h(av6h != null ? av6h : 100.0);
        return dto;
    }

    private String formatUptime(long totalSeconds) {
        long days = totalSeconds / 86400;
        long hours = (totalSeconds % 86400) / 3600;
        long minutes = ((totalSeconds % 86400) % 3600) / 60;
        return String.format("%d dias, %d horas, %d minutos", days, hours, minutes);
    }
    
    private double bytesToGB(double bytes) {
        return bytes / (1024 * 1024 * 1024);
    }
}