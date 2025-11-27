-- Garante que estamos usando o banco de dados correto
USE msa;

-- Tabela principal para todos os hosts
CREATE TABLE hosts (
    id INT AUTO_INCREMENT PRIMARY KEY,
    public_id CHAR(36) NOT NULL UNIQUE,
    zabbix_id BIGINT NOT NULL UNIQUE,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    host_type VARCHAR(50) NOT NULL CHECK (host_type IN ('APPLICATION', 'SERVER', 'DATABASE')),
    status ENUM('ACTIVE', 'ALERT', 'INACTIVE') NOT NULL DEFAULT 'ACTIVE',
    status_description VARCHAR(255) DEFAULT 'Status pendente.',
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
);


-- Catálogo de todas as métricas possíveis
CREATE TABLE metrics (
    id INT AUTO_INCREMENT PRIMARY KEY,
    metric_key VARCHAR(255) NOT NULL UNIQUE, 
    name VARCHAR(255) NOT NULL,              
    unit VARCHAR(20)
);

-- Tabela para armazenar a chave Zabbix PADRÃO para cada métrica
CREATE TABLE default_zabbix_key (
    id INT AUTO_INCREMENT PRIMARY KEY,
    metric_id INT NOT NULL UNIQUE, -- Garante que cada métrica só tenha uma chave padrão
    zabbix_key VARCHAR(255) NOT NULL,
    FOREIGN KEY (metric_id) REFERENCES metrics(id) ON DELETE CASCADE
);

-- Tabela de ligação para definir quais métricas cada host monitora
CREATE TABLE host_metric_config (
    host_id INT NOT NULL,
    metric_id INT NOT NULL,
    zabbix_key VARCHAR(255) NOT NULL, -- <-- A CHAVE ZABBIX
    PRIMARY KEY (host_id, metric_id),
    FOREIGN KEY (host_id) REFERENCES hosts(id) ON DELETE CASCADE,
    FOREIGN KEY (metric_id) REFERENCES metrics(id) ON DELETE CASCADE
);

-- Tabela para guardar o histórico de valores de todas as métricas
CREATE TABLE metric_history (
    host_id INT NOT NULL,
    metric_id INT NOT NULL,
    `timestamp` DATETIME(6) NOT NULL,
    `value` DOUBLE PRECISION NOT NULL,
    PRIMARY KEY (host_id, metric_id, `timestamp`),
    FOREIGN KEY (host_id) REFERENCES hosts(id) ON DELETE CASCADE,
    FOREIGN KEY (metric_id) REFERENCES metrics(id) ON DELETE CASCADE
);

-- Tabela para guardar eventos/alertas
CREATE TABLE recent_events (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    host_id INT NOT NULL,
    `timestamp` DATETIME(6) NOT NULL,
    severity VARCHAR(50),
    name TEXT NOT NULL,
    details JSON,
    FOREIGN KEY (host_id) REFERENCES hosts(id) ON DELETE CASCADE
);

-- Índices para acelerar as consultas
CREATE INDEX idx_metric_history_timestamp ON metric_history (`timestamp` DESC);
CREATE INDEX idx_events_timestamp ON recent_events (`timestamp` DESC);

-- Tabela para registrar o status da conexão do coletor com o Zabbix
CREATE TABLE zabbix_connection_status (
    id INT AUTO_INCREMENT PRIMARY KEY,
    `timestamp` DATETIME(6) NOT NULL,
    status ENUM('SUCCESS', 'ERROR') NOT NULL,
    details TEXT
);

-- Tabela para guardar o valor ATUAL de métricas não-numéricas (texto/JSON)
CREATE TABLE metric_current_value (
    id INT AUTO_INCREMENT PRIMARY KEY,
    host_id INT NOT NULL,
    metric_id INT NOT NULL,
    current_value TEXT,
    last_updated DATETIME(6) NOT NULL,
    -- Garante um único valor por métrica/host
    UNIQUE KEY uk_host_metric (host_id, metric_id), 
    FOREIGN KEY (host_id) REFERENCES hosts(id) ON DELETE CASCADE,
    FOREIGN KEY (metric_id) REFERENCES metrics(id) ON DELETE CASCADE
);