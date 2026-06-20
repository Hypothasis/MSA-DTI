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

## 🚀 Como Executar o Projeto

O projeto foi projetado para rodar em containers Docker, facilitando a configuração do ambiente de desenvolvimento.

**Pré-requisitos:**
* Docker e Docker Compose instalados.
* Portas `8080` (Keycloak), `3306` (MySQL) e `8081` (MSA) disponíveis.

**Passo a Passo:**

1. Clone o repositório.
2. Navegue até o diretório de infraestrutura:
   `cd src/infraestrutura/docker compose/`
3. Suba primeiro a infraestrutura base (MySQL e Keycloak):
   `docker compose -f infra-compose.yml up -d`
4. Aguarde os serviços ficarem saudáveis e suba a aplicação MSA:
   `docker compose -f msa-compose.yml up -d --build`
5. Acesse a aplicação no navegador via `http://localhost:8081`.

**Nota sobre Autenticação:**
O *realm* de desenvolvimento do Keycloak (`dev-realm-MSA.json`) é importado automaticamente na inicialização do container de infraestrutura, garantindo que os usuários e *roles* de teste já estejam configurados.

---

## 📚 Documentação

Uma documentação extensiva está disponível na pasta `doc/`. Lá você encontrará:
* **Arquitetura MSA:** Detalhamento da estrutura de pacotes e diagramas de arquitetura de software.
* **Manual de Configuração:** Passo a passo com capturas de tela para configuração do ambiente, Keycloak e chaves do Zabbix.
* **Testes de Execução:** Relatórios de inicialização via Docker Compose.

---

## 👨‍💻 Autor

Desenvolvido por **Gabriel Oliveira dos Santos** como Projeto de Estágio e Trabalho de Conclusão de Curso (Engenharia de Computação) na Diretoria de Tecnologia da Informação - **UNILAB**.
