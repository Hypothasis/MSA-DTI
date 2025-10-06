-- Popula a tabela de catálogo com todas as métricas disponíveis no formulário
INSERT INTO metrics (metric_key, name, unit) VALUES
-- Métricas Comuns (presentes em todas as abas)
('disponibilidade-global', 'Disponibilidade Global (48h)', '%'),
('disponibilidade-especifica', 'Disponibilidade Específica', '%'),
('cpu-uso', 'Uso de CPU', '%'),
('memoria-ram', 'Uso de Memória RAM', '%'),
('armazenamento', 'Uso de Armazenamento', 'GB'),
('dados-banda-larga', 'Banda Larga de Dados', 'bps'),
('tempo-ativo', 'Tempo de Servidor Ativo', 'uptime'),
('eventos-recentes', 'Eventos Recentes', ''),

-- Métricas da Aba "Aplicação/Serviço"
('latencia-tempo-resposta', 'Tempo de Resposta', 's'),

-- Métricas da Aba "Servidores"
('sistema-operacional', 'Sistema Operacional', ''),
('cpu-processos', 'Processos da CPU', 'count'),
('cpu-troca-contextos', 'Troca de Contextos da CPU', 'cps'),
('memoria-swap', 'Uso de Memória SWAP', '%');