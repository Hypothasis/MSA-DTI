-- Popula a tabela de catálogo com todas as métricas disponíveis no formulário
INSERT INTO metrics (metric_key, zabbix_key, name, unit) VALUES

-- ##### Métricas Comuns (presentes em todas as abas) #####

-- Métricas para Disponibilidade 
('disponibilidade-global', 'zabbix[host,agent,available]', 'Disponibilidade Global (48h)', '%'),
('disponibilidade-especifica', 'zabbix[host,agent,available]', 'Disponibilidade Específica', '%'),

-- Métrica para Uso de 
('cpu-uso', 'system.cpu.util', 'Uso de CPU', '%'),

-- Métricas para memória RAM
('memoria-ram-total', 'vm.memory.size[total]', 'Memória RAM Total', 'B'),
('memoria-ram-disponivel', 'vm.memory.size[available]', 'Memória RAM Disponível', 'B'),

-- Métricas para armazenamento
('armazenamento-root-total', 'vfs.fs.size[/,total]', 'Armazenamento / Total', 'B'),
('armazenamento-root-usado', 'vfs.fs.size[/,used]', 'Armazenamento / Usado', 'B'),
('armazenamento-boot-total', 'vfs.fs.size[/boot,total]', 'Armazenamento /boot Total', 'B'),
('armazenamento-boot-usado', 'vfs.fs.size[/boot,used]', 'Armazenamento /boot Usado', 'B'),


-- Métricas para banda larga
('dados-entrada', 'net.if.in["eth0"]', 'Banda Larga (Entrada)', 'bps'),
('dados-saida', 'net.if.out["eth0"]', 'Banda Larga (Saída)', 'bps'),

('tempo-ativo', 'system.uptime', 'Tempo de Servidor Ativo', 'uptime'),
    -- A métrica "eventos-recentes" não entra aqui pois é tratada de forma diferente.

-- ##### Métricas da Aba "Aplicação/Serviço" #####
('latencia', 'icmppingsec', 'Tempo de Resposta ICMP', 's'),

-- ##### Métricas da Aba "Servidores" #####
('os-nome', 'system.sw.os', 'Sistema Operacional', ''),
('os-arch', 'system.sw.arch', 'Arquitetura do SO', ''),

('cpu-processos-atuais', 'proc.num', 'Processos Atuais', 'count'),
('cpu-processos-max', 'kernel.maxproc', 'Máximo de Processos', 'count'),

('cpu-troca-contextos', 'system.cpu.switches', 'Troca de Contextos', 'cps'),

('memoria-swap-total', 'system.swap.size[,total]', 'Memória SWAP Total', 'B'),
('memoria-swap-livre', 'system.swap.size[,free]', 'Memória SWAP Livre', 'B'),

('eventos-recentes', 'zabbix_api', 'Eventos Recentes', '');