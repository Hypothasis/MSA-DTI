package br.com.dti.msa.service;

import br.com.dti.msa.dto.CreateHostDTO;
import br.com.dti.msa.dto.UpdateHostDTO;
import br.com.dti.msa.exception.ZabbixValidationException;
import br.com.dti.msa.integration.zabbix.dto.ZabbixClient;
import br.com.dti.msa.model.Host;
import br.com.dti.msa.model.Metric;
import br.com.dti.msa.repository.HostRepository;
import br.com.dti.msa.repository.MetricRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class HostService {

    @Autowired private ZabbixClient zabbixClient;
    @Autowired private MetricCatalog metricCatalog;
    @Autowired private HostRepository hostRepository;
    @Autowired private MetricRepository metricRepository;

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
        // 1. VALIDAÇÃO DO HOST NO ZABBIX
        if (!zabbixClient.hostExists(dto.getHostZabbixID())) {
            throw new ZabbixValidationException("Host com Zabbix ID '" + dto.getHostZabbixID() + "' não encontrado no Zabbix.");
        }

        // 2. VALIDAÇÃO DAS MÉTRICAS NO ZABBIX
        for (String metricNameFromForm : dto.getEnabledMetrics()) {
            List<String> zabbixKeys = metricCatalog.getZabbixKeysFor(metricNameFromForm);

            if (metricNameFromForm.equals("eventos-recentes")) {
                continue; // 'continue' ignora o resto do código e vai para o próximo item da lista
            }

            if (zabbixKeys.isEmpty()) {
                throw new ZabbixValidationException("Métrica '" + metricNameFromForm + "' não é reconhecida pelo sistema MSA.");
            }
            for (String key : zabbixKeys) {
                if (!zabbixClient.itemExistsOnHost(dto.getHostZabbixID(), key)) {
                     throw new ZabbixValidationException("A chave Zabbix '" + key + "' não foi encontrada no host.");
                }
            }
        }
        
        System.out.println("Salvando o host no banco de dados...");

        // 3. PERSISTÊNCIA NO BANCO DE DADOS
        List<Metric> selectedMetrics = metricRepository.findByMetricKeyIn(dto.getEnabledMetrics());
        

        // --- LOG DE DEPURAÇÃO 2: O que foi encontrado no banco? ---
        System.out.println("Número de métricas encontradas no banco: " + selectedMetrics.size());
        if(selectedMetrics.isEmpty() && !dto.getEnabledMetrics().isEmpty()) {
            System.err.println("ALERTA: Nenhuma métrica foi encontrada no banco de dados para as chaves fornecidas!");
        }

        Host newHost = new Host();
        newHost.setName(dto.getHostName());
        newHost.setZabbixId(dto.getHostZabbixID().intValue());
        newHost.setDescription(dto.getHostDescription());
        newHost.setType(dto.getHostType());
        newHost.setPublicId(UUID.randomUUID().toString());
        newHost.setMetrics(selectedMetrics);

        System.out.println(">>> VALOR A SER SALVO EM host_type: '" + newHost.getType() + "'");
        
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
}