-- Tabela principal para todos os hosts, unificando os tipos
CREATE TABLE hosts (
    id SERIAL PRIMARY KEY,
    public_id UUID NOT NULL UNIQUE DEFAULT gen_random_uuid(),
    zabbix_id BIGINT NOT NULL UNIQUE,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    host_type VARCHAR(50) NOT NULL CHECK (host_type IN ('APPLICATION', 'SERVER', 'DATABASE')),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Catálogo de todas as métricas possíveis
CREATE TABLE metrics (
    id SERIAL PRIMARY KEY,
    metric_key VARCHAR(255) NOT NULL UNIQUE,
    name VARCHAR(255) NOT NULL,
    unit VARCHAR(20)
);

-- Tabela de ligação para definir quais métricas cada host monitora
CREATE TABLE host_metric_config (
    host_id INT NOT NULL REFERENCES hosts(id) ON DELETE CASCADE,
    metric_id INT NOT NULL REFERENCES metrics(id) ON DELETE CASCADE,
    PRIMARY KEY (host_id, metric_id) -- Chave primária composta
);

-- Tabela para guardar o histórico de valores numéricos de todas as métricas
CREATE TABLE metric_history (
    -- Esta tabela não precisa de um 'id' próprio, a combinação das colunas já é única
    host_id INT NOT NULL REFERENCES hosts(id) ON DELETE CASCADE,
    metric_id INT NOT NULL REFERENCES metrics(id) ON DELETE CASCADE,
    "timestamp" TIMESTAMPTZ NOT NULL,
    "value" DOUBLE PRECISION NOT NULL,
    PRIMARY KEY (host_id, metric_id, "timestamp") -- Chave primária composta
);

-- Tabela para guardar eventos/alertas (dados não-numéricos)
CREATE TABLE recent_events (
    id BIGSERIAL PRIMARY KEY,
    host_id INT NOT NULL REFERENCES hosts(id) ON DELETE CASCADE,
    "timestamp" TIMESTAMPTZ NOT NULL,
    severity VARCHAR(50),
    name TEXT NOT NULL,
    details JSONB
);

-- Índices para acelerar as consultas de séries temporais
CREATE INDEX idx_metric_history_timestamp ON metric_history ("timestamp" DESC);
CREATE INDEX idx_events_timestamp ON recent_events ("timestamp" DESC);
