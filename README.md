# MSA - Monitoramento de Servidores e Aplicações 🚀

O **MSA (Monitoramento de Servidores e Aplicações)** é uma plataforma centralizada de observabilidade e monitoramento de infraestrutura de TI. Desenvolvido durante o estágio na Diretoria de Tecnologia da Informação (DTI) da **UNILAB**, o sistema atua como um *middleware* inteligente que consome dados da API do Zabbix, processa métricas heterogêneas e as exibe em painéis gerenciais em tempo real.

O objetivo principal do MSA é reduzir a sobrecarga cognitiva da equipe de operações, traduzindo dados brutos (como JSONs complexos de *Health Checks* ou *Headers* HTTP) em status visuais claros (`ACTIVE`, `ALERT`, `INACTIVE`) para servidores, bancos de dados e aplicações web críticas (como o SIGAA).

---

## 🎯 Principais Funcionalidades

* **Integração Dinâmica com Zabbix:** Consumo otimizado via JSON-RPC, filtrando apenas métricas relevantes configuradas por host.
* **Configuração Customizada de Métricas:** Arquitetura de banco de dados flexível que permite a cada *host* possuir chaves Zabbix personalizadas para a mesma métrica global.
* **Parsing Inteligente:** Algoritmos dedicados para interpretar respostas JSON, textos brutos de HTTP e valores numéricos em tempo real.
* **Segurança Centralizada:** Autenticação e autorização (RBAC) federadas utilizando **OAuth2/OIDC** com Keycloak.
* **Dashboard Interativo:** Interface responsiva com gráficos temporais (ApexCharts) e filtragem de ativos via *client-side*, sem necessidade de recarregar a página.
* **Agendamento Autônomo:** Coleta e limpeza periódica de métricas com rotinas otimizadas (`@Scheduled` e persistência assíncrona).

---

## 🛠️ Stack Tecnológica

| Camada | Tecnologias Utilizadas |
| :--- | :--- |
| **Backend** | Java 21, Spring Boot 3, Spring Data JPA, Spring Security |
| **Banco de Dados** | MySQL 8, Flyway (Migration), H2 (Testes) |
| **Frontend** | Thymeleaf, JavaScript (Vanilla), HTML5, CSS3, ApexCharts |
| **Infraestrutura** | Docker, Docker Compose, Kubernetes (K8s Manifests) |
| **Segurança** | Keycloak (Identity Provider), JWT, OAuth2 |
| **Integração** | Zabbix API, Spring RestClient, Jackson |

---

## 📂 Estrutura do Projeto

O repositório está organizado de forma modular para separar código-fonte, infraestrutura e documentação:

* `src/aplicacao/`: Contém todo o código-fonte Java (Spring Boot), recursos estáticos (CSS, JS, Imagens), templates Thymeleaf e scripts de migração do Flyway.
* `src/infraestrutura/`: Arquivos para orquestração de ambientes. Inclui os arquivos `docker-compose` (divididos por aplicação e infraestrutura) e manifestos do Kubernetes (`k8s/`).
* `src/scripts_bd/`: Scripts manuais de inicialização do banco de dados.
* `doc/`: Documentação oficial do projeto, manuais de configuração detalhados e arquivos exportados para visualização offline.
* `Postman/`: Collection do Postman contendo todas as rotas da API REST do MSA para testes locais.

---

## 📚 Documentação

Uma documentação extensiva está disponível na pasta `doc/`. Lá você encontrará:
* **Arquitetura MSA:** Detalhamento da estrutura de pacotes e diagramas de arquitetura de software.
* **Manual de Configuração:** Passo a passo com capturas de tela para configuração do ambiente, Keycloak e chaves do Zabbix.
* **Testes de Execução:** Relatórios de inicialização via Docker Compose.

---

## 👨‍💻 Autor

Desenvolvido por **Gabriel Oliveira dos Santos** como Projeto de Estágio e Trabalho de Conclusão de Curso (Engenharia de Computação) na Diretoria de Tecnologia da Informação - **UNILAB**.

---

# 🚀 Como Executar o Projeto (Guia de Deploy)

A arquitetura do MSA é totalmente containerizada e dividida em três frentes:

- **Infraestrutura Base** (MySQL, Keycloak e Zabbix)
- **Hosts Monitorados** (ambiente de teste)
- **Aplicação Principal (MSA)**

Siga a ordem abaixo para subir o ambiente localmente.

## 📋 Pré-requisitos

- Docker instalado
- Docker Compose instalado
- Identificar o **IP da sua máquina na rede local**

> ⚠️ Não utilize `localhost` nos arquivos de configuração. Os containers precisam se comunicar utilizando o IP da máquina hospedeira.

---

## Passo 1: Subindo a Infraestrutura Base

A infraestrutura contém:

- MySQL
- Keycloak
- Zabbix Server
- Zabbix Web

O Keycloak já está configurado para importar automaticamente o realm `dev` durante a inicialização.

1. Navegue até o diretório dos arquivos compose:

```bash
cd infraestrutura/docker-compose
```

2. Inicie a infraestrutura em segundo plano:

```bash
docker compose -f infra-compose.yml up -d
```

3. Verifique se todos os containers estão em execução:

```bash
docker ps
```

Após a inicialização, os serviços estarão disponíveis em:

| Serviço | URL |
|----------|------|
| Keycloak | http://<IP>:8080 |
| Zabbix | http://<IP>:8090 |

---

## Passo 2: Configurações de Segurança e Integração

Antes de iniciar o MSA, é necessário obter as credenciais geradas pela infraestrutura.

### 🔐 Keycloak

1. Acesse:

```text
http://<IP>:8080
```

2. Faça login:

```text
Usuário: admin
Senha: admin
```

3. Selecione o realm:

```text
dev
```

4. Navegue até:

```text
Clients → MSA → Credentials
```

5. Copie o valor de **Client Secret**.

6. Crie um usuário para acessar o portal do MSA:

```text
Users → Create User
```

7. Defina uma senha e atribua as roles necessárias.

---

### 📊 Zabbix

1. Acesse:

```text
http://<IP>:8090
```

2. Faça login:

```text
Usuário: Admin
Senha: zabbix
```

3. Navegue até:

```text
Administration → General → API Tokens
```

4. Crie um novo token para o usuário **Admin**.

5. Copie o token gerado.

#### Opcional

Cadastre hosts e configure os itens de monitoramento utilizando:

- Zabbix Agent
- Chaves personalizadas
- Monitoramento via HTTP Headers

---

## Passo 3: Subindo os Hosts Monitorados (Ambiente de Teste)

Para popular os dashboards do MSA com dados reais, suba os containers de teste.

1. Execute:

```bash
docker compose -f host-compose.yml up -d
```

2. Verifique os containers:

```bash
docker ps
```

Esses hosts simulam aplicações monitoradas e fornecem métricas para o Zabbix.

---

## Passo 4: Configurando e Subindo o MSA

Agora que a infraestrutura está operacional e as credenciais foram geradas, configure a aplicação principal.

### Configuração

Abra o arquivo:

```text
msa-compose.yml
```

ou o arquivo `.env` correspondente.

Substitua os seguintes valores:

#### Endereços IP

```text
<IP>
```

pelo IP da sua máquina.

#### Client Secret do Keycloak

```text
<CLIENT_SECRET>
```

pelo valor obtido no Passo 2.

#### Token da API do Zabbix

```text
ZABBIX_API_USER_TOKEN
```

pelo token gerado no Zabbix.

---

### Inicialização

Execute:

```bash
docker compose -f msa-compose.yml up -d --build
```

Verifique os containers:

```bash
docker ps
```

---

## 🌐 Acesso à Aplicação

Após a inicialização, o MSA estará disponível em:

```text
http://<IP>:8081
```

Utilize o usuário criado no Keycloak para realizar o login.

---

## 🔍 Verificando Logs

Caso seja necessário acompanhar a execução dos containers:

### Logs do MSA

```bash
docker compose -f msa-compose.yml logs -f
```

### Logs da Infraestrutura

```bash
docker compose -f infra-compose.yml logs -f
```

### Logs dos Hosts Monitorados

```bash
docker compose -f host-compose.yml logs -f
```

---

## 🛑 Encerrando os Serviços

### Infraestrutura

```bash
docker compose -f infra-compose.yml down
```

### Hosts Monitorados

```bash
docker compose -f host-compose.yml down
```

### MSA

```bash
docker compose -f msa-compose.yml down
```
