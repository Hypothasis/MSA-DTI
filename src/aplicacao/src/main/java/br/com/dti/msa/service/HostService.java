package br.com.dti.msa.service;

import br.com.dti.msa.dto.CreateHostDTO;
import br.com.dti.msa.dto.HomepageHostDTO;
import br.com.dti.msa.dto.HostDashboardDTO;
import br.com.dti.msa.dto.HostSearchResultDTO;
import br.com.dti.msa.dto.PublicHostStatusDTO;
import br.com.dti.msa.dto.UpdateHostDTO;
import br.com.dti.msa.exception.ZabbixValidationException;
import br.com.dti.msa.integration.zabbix.dto.ZabbixClient;
import br.com.dti.msa.model.Host;
import br.com.dti.msa.model.Metric;
import br.com.dti.msa.model.MetricHistory;
import br.com.dti.msa.model.RecentEvents;
import br.com.dti.msa.repository.HostRepository;
import br.com.dti.msa.repository.MetricHistoryRepository;
import br.com.dti.msa.repository.MetricRepository;
import br.com.dti.msa.repository.RecentEventsRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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

    /**
     * Retorna todos os hosts cadastrados.
     */
    public List<Host> findAll() {
        return hostRepository.findAll();
    }

    /**
     * Busca um host pelo seu ID. Lança uma exceção se não for encontrado.
     */
    public Host findById(Long id) {
        return hostRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Host não encontrado com ID: " + id));
    }
    
    /**
     * Busca um host pelo PublicID. Lança uma exceção se não for encontrado.
     */
    public Host findByPublicId(String publicId) {
        return hostRepository.findByPublicId(publicId)
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

        // Lista com as chaves individuais configuradas para o host (ex: "cpu-uso", "memoria-ram-total", ...)
        List<String> configuredMetricKeys = host.getMetrics().stream()
                                                .map(Metric::getMetricKey)
                                                .collect(Collectors.toList());

        // --- CÁLCULO DE STATUS ---
        // Determina o status geral do host (OK, ALERT, PROBLEM) e o define no DTO
        dto.setStatus(determineHostStatus(host, configuredMetricKeys));


        // --- PREENCHIMENTO DO DTO ---

        // 1. Métricas de Histórico (para gráficos de linha/área)
        if (configuredMetricKeys.contains("disponibilidade-especifica")) {

            // Use o método que busca o histórico completo, sem agregar.
            List<HostDashboardDTO.MetricValueDTO> history =
                fetchMetricHistory(host.getId(), "disponibilidade-especifica", startTime48h);

            // Opcional, mas recomendado: Converte os valores de 1.0 para 100.0
            // para que o gráfico mostre 100% em vez de 1.
            history.forEach(point -> point.setY(point.getY() * 100.0));

            dto.setAvailabilityHistory(history);
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

        // Lógica para Disponibilidade Global (Agregado)
        if (configuredMetricKeys.containsAll(metricCatalog.getMetricKeysForCheckbox("disponibilidade-global"))) {
            dto.setGlobalAvailability(calculateGlobalAvailability(host.getId(), "disponibilidade-global"));
        }
        
        // Lógica para Sistema Operacional (Composto)
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
            List<RecentEvents> events = recentEventsRepository.findTop5ByHostIdOrderByTimestampDesc(host.getId());
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
     * Obtém lista de todos os hosts com seus status
     */
    public List<PublicHostStatusDTO> getPublicHostStatuses() {
        return hostRepository.findAll().stream()
                .map(PublicHostStatusDTO::new)
                .collect(Collectors.toList());
    }

    /**
     * Obtém status do host, junto com dados do availabilityGlobal para exibição pública (Home).
     */
    public List<HomepageHostDTO> getHomepageHosts() {
        // 1. Busca todos os hosts
        List<Host> allHosts = hostRepository.findAllWithMetrics();
        LocalDateTime startTime48h = LocalDateTime.now().minusHours(48);
        
        return allHosts.stream()
            // 2. Filtra apenas os que NÃO estão com status ACTIVE
            .filter(host -> host.getStatus() != Host.HostStatus.ACTIVE)
            // 3. Mapeia para o DTO e busca os dados de disponibilidade para cada um
            .map(host -> {
                HomepageHostDTO dto = new HomepageHostDTO(host);
                
                // Verifica se a métrica de disponibilidade está configurada
                boolean hasAvailability = host.getMetrics().stream()
                    .anyMatch(m -> m.getMetricKey().equals("disponibilidade-global"));

                if (hasAvailability) {
                    // Calcula a disponibilidade global das últimas 48h
                    Double availability = metricHistoryRepository.calculateAvailability(host.getId(), "disponibilidade-global", startTime48h);
                    dto.setGlobalAvailability48h(availability != null ? availability : 0.0);
                    
                    // Busca o histórico de pontos para o gráfico
                    List<Object[]> dailyData = metricHistoryRepository.getDailyAvailability(host.getId(), "disponibilidade-especifica", startTime48h);
                    List<HostDashboardDTO.MetricValueDTO> history = dailyData.stream()
                        .map(row -> new HostDashboardDTO.MetricValueDTO(((java.sql.Date) row[0]).toLocalDate().atStartOfDay(), (Double) row[1]))
                        .collect(Collectors.toList());
                    dto.setAvailabilityHistory(history);
                }
                return dto;
            })
            .collect(Collectors.toList());
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
     */
    @Transactional
    public Host createAndValidateHost(CreateHostDTO dto) throws ZabbixValidationException {
        // VALIDAÇÃO DO HOST NO ZABBIX
        if (!zabbixClient.hostExists(dto.getHostZabbixID())) {
            throw new ZabbixValidationException("Host com Zabbix ID '" + dto.getHostZabbixID() + "' não encontrado no Zabbix.");
        }

        // CONVERTE NOMES DOS CHECKBOXES (DTO) PARA CHAVES DE MÉTRICAS REAIS (BANCO)
        //    Usando o método correto: getMetricKeysForCheckbox
        List<String> metricKeysToEnable = dto.getEnabledMetrics().stream()
                .flatMap(checkboxName -> metricCatalog.getMetricKeysForCheckbox(checkboxName).stream())
                .distinct()
                .collect(Collectors.toList());

        if (metricKeysToEnable.isEmpty() && !dto.getEnabledMetrics().isEmpty()) {
            throw new ZabbixValidationException("Nenhuma métrica correspondente encontrada no catálogo para as opções selecionadas.");
        }
        
        // Busca no banco todas as entidades Metric que o usuário realmente selecionou
        List<Metric> selectedMetrics = metricRepository.findByMetricKeyIn(metricKeysToEnable);

        // VALIDAÇÃO DAS MÉTRICAS NO ZABBIX (USANDO OS DADOS VINDOS DO BANCO)
        for (Metric metric : selectedMetrics) {
            // Pula métricas que não são do Zabbix (como eventos-recentes)
            if (metric.getZabbixKey() == null || metric.getZabbixKey().equalsIgnoreCase("zabbix_api")) {
                continue;
            }

            if (!zabbixClient.itemExistsOnHost(dto.getHostZabbixID(), metric.getZabbixKey())) {
                throw new ZabbixValidationException("A chave Zabbix '" + metric.getZabbixKey() + 
                    "' (para a métrica '" + metric.getName() + "') não foi encontrada no host.");
            }
        }
        
        System.out.println("Validações OK! Salvando o host no banco de dados...");

        // PERSISTÊNCIA NO BANCO DE DADOS
        Host newHost = new Host();
        newHost.setName(dto.getHostName());
        newHost.setZabbixId(dto.getHostZabbixID().intValue());
        newHost.setDescription(dto.getHostDescription());
        newHost.setType(dto.getHostType());
        newHost.setPublicId(UUID.randomUUID().toString());
        
        // Associa as métricas (já buscadas do banco) ao novo host
        newHost.setMetrics(selectedMetrics);
        
        // Salva o host e as associações na tabela host_metric_config
        return hostRepository.save(newHost);
    }

    /**
     * Atualiza um host existente com base nos dados fornecidos.
     */
    @Transactional
    public Host updateHost(Long hostId, UpdateHostDTO dto) {
        // 1. Busca o host existente no banco
        Host existingHost = findById(hostId);

        // 2. Atualiza os campos básicos
        existingHost.setName(dto.getHostName());
        existingHost.setZabbixId(dto.getHostZabbixID().intValue());
        existingHost.setDescription(dto.getHostDescription());
        existingHost.setType(dto.getHostType());

        // 3. Atualiza as métricas associadas
        List<Metric> selectedMetrics = metricRepository.findByMetricKeyIn(dto.getEnabledMetrics());
        existingHost.setMetrics(selectedMetrics);
        
        // 4. Salva o host atualizado (o JPA entende que é um update por causa do ID)
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

    private String determineHostStatus(Host host, List<String> configuredMetricKeys) {
        // NÍVEL 1: CHECAGEM CRÍTICA (PROBLEM)
        // Se a métrica de disponibilidade estiver configurada, verificamos primeiro.
        if (configuredMetricKeys.contains("disponibilidade-global")) {
            Optional<Double> lastAvailability = findLastValue(host.getId(), "disponibilidade-global");
            
            // Se o valor for 0, o host está com um problema crítico.
            if (lastAvailability.isPresent() && lastAvailability.get() == 0.0) {
                return "PROBLEM";
            }
            // Se não houver dados, não podemos determinar o status.
            if (lastAvailability.isEmpty()) {
                return "UNKNOWN";
            }
        }

        // NÍVEL 2: CHECAGEM DE ALERTAS (ALERT)
        
        // Alerta de CPU alta (ex: acima de 90%)
        if (configuredMetricKeys.contains("cpu-uso")) {
            Optional<Double> lastCpu = findLastValue(host.getId(), "cpu-uso");
            if (lastCpu.isPresent() && lastCpu.get() > 90.0) {
                return "ALERT";
            }
        }

        // Alerta de pouca memória RAM (ex: menos de 10% livre)
        if (configuredMetricKeys.contains("memoria-ram-total") && configuredMetricKeys.contains("memoria-ram-disponivel")) {
            Optional<Double> totalMem = findLastValue(host.getId(), "memoria-ram-total");
            Optional<Double> availableMem = findLastValue(host.getId(), "memoria-ram-disponivel");
            if (totalMem.isPresent() && availableMem.isPresent() && totalMem.get() > 0) {
                double percentFree = (availableMem.get() / totalMem.get()) * 100;
                if (percentFree < 10.0) {
                    return "ALERT";
                }
            }
        }
        
        // NÍVEL 3: SE NADA ACIMA OCORREU, ESTÁ TUDO OK
        return "OK";
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