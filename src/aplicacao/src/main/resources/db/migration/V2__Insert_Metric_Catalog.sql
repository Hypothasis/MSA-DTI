-- Popula a tabela de catálogo com todas as métricas disponíveis no formulário
INSERT INTO metrics (metric_key, name, unit) VALUES

-- ##### Métricas Comuns (presentes em todas as abas) #####

-- Métricas para Disponibilidade - 'zabbix[host,agent,available]'
('disponibilidade-global', 'Disponibilidade Global (48h)', '%'),
('disponibilidade-especifica', 'Disponibilidade Específica', '%'),

-- Métricas para Disponibilidade Health Ready
('disponibilidade-global-health', 'Disponibilidade Global (48h)', '%'),
('disponibilidade-especifica-health', 'Disponibilidade Específica', '%'),

-- Métricas para Disponibilidade HTTP Agent
('disponibilidade-global-http-agente', 'Disponibilidade Global (48h)', '%'),
('disponibilidade-especifica-http-agente', 'Disponibilidade Específica', '%'),

-- Métrica para Uso de CPU - 'system.cpu.util'
('cpu-uso', 'Uso de CPU', '%'),

-- Métricas para memória RAM - 'vm.memory.size[total]', 'vm.memory.size[available]'
('memoria-ram-total', 'Memória RAM Total', 'B'),
('memoria-ram-disponivel', 'Memória RAM Disponível', 'B'),

-- Métricas para armazenamento - 'vfs.fs.size[/,total]', 'vfs.fs.size[/,used]' , 'vfs.fs.size[/boot,total]', 'vfs.fs.size[/boot,used]'
('armazenamento-root-total', 'Armazenamento / Total', 'B'),
('armazenamento-root-usado', 'Armazenamento / Usado', 'B'),
('armazenamento-boot-total', 'Armazenamento /boot Total', 'B'),
('armazenamento-boot-usado', 'Armazenamento /boot Usado', 'B'),


-- Métricas para banda larga - 'net.if.in["eth0"]', 'net.if.out["eth0"]'
('dados-entrada', 'Banda Larga (Entrada)', 'bps'),
('dados-saida', 'Banda Larga (Saída)', 'bps'),

-- Métrica para Tempo Ativo - 'system.uptime'
('tempo-ativo', 'Tempo de Servidor Ativo', 'uptime'),
    -- A métrica "eventos-recentes" não entra aqui pois é tratada de forma diferente.

-- ##### Métricas da Aba "Aplicação/Serviço" #####

-- Métrica para Latencia - 'icmppingsec'
('latencia', 'Tempo de Resposta ICMP', 's'),

-- ##### Métricas da Aba "Servidores" #####

-- Métricas para Sistema Operacional - 'system.sw.os', 'system.sw.arch'
('os-nome', 'Sistema Operacional', ''),
('os-arch', 'Arquitetura do SO', ''),

-- Métricas para Processos de CPU - 'proc.num', 'kernel.maxproc'
('cpu-processos-atuais', 'Processos Atuais', 'count'),
('cpu-processos-max', 'Máximo de Processos', 'count'),

-- Métrica para Troca de contextos na CPU - 'system.cpu.switches'
('cpu-troca-contextos', 'Troca de Contextos', 'cps'),

-- Métricas para uso de SWAP - 'system.swap.size[,total]', 'system.swap.size[,free]'
('memoria-swap-total', 'Memória SWAP Total', 'B'),
('memoria-swap-livre', 'Memória SWAP Livre', 'B'),

('eventos-recentes', 'Eventos Recentes', '');


-- Insere as chaves Zabbix PADRÃO na nova tabela
INSERT INTO default_zabbix_key (metric_id, zabbix_key) VALUES
((SELECT id FROM metrics WHERE metric_key = 'disponibilidade-global'), 'zabbix[host,agent,available]'),
((SELECT id FROM metrics WHERE metric_key = 'disponibilidade-especifica'), 'zabbix[host,agent,available]'),
((SELECT id FROM metrics WHERE metric_key = 'cpu-uso'), 'system.cpu.util'),
((SELECT id FROM metrics WHERE metric_key = 'memoria-ram-total'), 'vm.memory.size[total]'),
((SELECT id FROM metrics WHERE metric_key = 'memoria-ram-disponivel'), 'vm.memory.size[available]'),
((SELECT id FROM metrics WHERE metric_key = 'armazenamento-root-total'), 'vfs.fs.size[/,total]'),
((SELECT id FROM metrics WHERE metric_key = 'armazenamento-root-usado'), 'vfs.fs.size[/,used]'),
((SELECT id FROM metrics WHERE metric_key = 'armazenamento-boot-total'), 'vfs.fs.size[/boot,total]'),
((SELECT id FROM metrics WHERE metric_key = 'armazenamento-boot-usado'), 'vfs.fs.size[/boot,used]'),
((SELECT id FROM metrics WHERE metric_key = 'dados-entrada'), 'net.if.in["eth0"]'),
((SELECT id FROM metrics WHERE metric_key = 'dados-saida'), 'net.if.out["eth0"]'),
((SELECT id FROM metrics WHERE metric_key = 'tempo-ativo'), 'system.uptime'),
((SELECT id FROM metrics WHERE metric_key = 'latencia'), 'icmppingsec'),
((SELECT id FROM metrics WHERE metric_key = 'os-nome'), 'system.sw.os'),
((SELECT id FROM metrics WHERE metric_key = 'os-arch'), 'system.sw.arch'),
((SELECT id FROM metrics WHERE metric_key = 'cpu-processos-atuais'), 'proc.num'),
((SELECT id FROM metrics WHERE metric_key = 'cpu-processos-max'), 'kernel.maxproc'),
((SELECT id FROM metrics WHERE metric_key = 'cpu-troca-contextos'), 'system.cpu.switches'),
((SELECT id FROM metrics WHERE metric_key = 'memoria-swap-total'), 'system.swap.size[,total]'),
((SELECT id FROM metrics WHERE metric_key = 'memoria-swap-livre'), 'system.swap.size[,free]'),
-- Métricas customizadas (health, HTTP) não têm chave padrão, então não são inseridas aqui.
-- 'eventos-recentes' é uma chave especial da API.
((SELECT id FROM metrics WHERE metric_key = 'eventos-recentes'), 'zabbix_api');