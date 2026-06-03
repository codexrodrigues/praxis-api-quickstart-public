# 🦸‍♂️ Banco de Demonstração – Universo dos Heróis

Este projeto demonstra o uso do Spring Boot 3 (Java 21) integrado a um banco PostgreSQL 17, utilizando um domínio temático inspirado em super‑heróis para exemplificar CRUDs, relacionamentos e regras de negócio. O objetivo é fornecer uma base rica de dados e endpoints REST para uso com as bibliotecas do ecossistema Praxis — incluindo o `praxis-metadata-starter` e, no front, o Praxis UI.

- Backend: Spring Boot 3.x, Spring Data JPA/Hibernate
- Banco: PostgreSQL 17 (compatível com serviços como Neon)
- Contratos: OpenAPI 3.1 + extensão x‑ui (gerada pelo Starter)

## 🧩 Domínio e propósito

O domínio modela uma organização global de heróis, contendo cadastros, missões, bases operacionais, acordos internacionais e relatórios de desempenho. Cada entidade foi pensada para cobrir casos comuns de aplicações corporativas (relacionamentos 1‑N e N‑N, enums, filtros ricos).

### Módulos principais

| Módulo                  | Finalidade                                                                 | Principais tabelas                                                                                             |
|-------------------------|-----------------------------------------------------------------------------|-----------------------------------------------------------------------------------------------------------------|
| Recursos Humanos        | Cadastro e gestão de heróis (funcionários), cargos, departamentos, histórico | `funcionarios`, `cargos`, `departamentos`, `historicos_salariais`, `enderecos`, `dependentes`                  |
| Habilidades & Identidades | Define poderes, origens e codinomes                                         | `habilidades`, `funcionario_habilidades`, `identidades_secretas`                                               |
| Bases & Equipes        | Bases de operação, equipes, níveis de acesso                                 | `bases`, `equipes`, `equipe_membros`, `base_acessos`                                                           |
| Missões & Ameaças      | Ameaças globais, missões, participantes e eventos                             | `ameacas`, `missoes`, `missao_participantes`, `missao_eventos`                                                 |
| Logística & Tecnologia | Equipamentos, veículos e alocações                                           | `equipamentos`, `veiculos`, `equipamento_alocacoes`, `veiculo_missao_usos`                                     |
| Compliance & Incidentes| Acordos, licenças, incidentes e indenizações                                  | `acordos_regulatorios`, `licencas_operacao`, `incidentes`, `indenizacoes`                                      |
| Comunicação & Mídia    | Sinais de socorro, reputação e menções na imprensa                            | `sinais_socorro`, `reputacoes`, `mencoes_midia`                                                                |

Observação: neste Quickstart, os recursos estão sob o prefixo único ` /api/human-resources/... ` por praticidade, mesmo cobrindo múltiplos “módulos” temáticos.

## 🌐 Endpoints REST

Padrão de rotas por recurso (RESTful):

- GET    `/{grupo}/{recurso}`        → lista ou filtro paginado
- GET    `/{grupo}/{recurso}/{id}`   → detalhe
- POST   `/{grupo}/{recurso}`        → criação
- PUT    `/{grupo}/{recurso}/{id}`   → atualização
- DELETE `/{grupo}/{recurso}/{id}`   → exclusão

Além disso, endpoints utilitários fornecidos pelo Starter integram com o Praxis UI para geração dinâmica de telas:

- POST `{recurso}/filter` — pesquisa paginada (DTO completo)
- POST `{recurso}/options/filter` — opções leves `{id,label}` por filtro (combos/autocomplete)
- GET  `{recurso}/options/by-ids` — reidrata opções por IDs, preservando a ordem
- GET  `/schemas/filtered` — JSON Schema enriquecido para a operação alvo (request/response)

## 🧠 Estrutura de demonstração para o Praxis

- Metadados ricos via `@UISchema` e Bean Validation (tipos, validações, hints, labels)
- Geração automática de telas no Praxis UI (tabela/formulário) com base no contrato
- Suporte a dashboards e relatórios (vistas agregadas/materalizadas)
- Contratos versionáveis e com ETag para cache eficiente

## 🧰 Tecnologias principais

- Spring Boot 3.x (Java 21), Spring Data JPA / Hibernate
- PostgreSQL 17 (Neon ou compatível)
- OpenAPI 3.1 + Praxis Metadata Starter
- Praxis UI (Angular, 20+) — Quickstart em breve

## 🗂️ Views e indicadores

| View                  | Descrição                                                                                  |
|-----------------------|----------------------------------------------------------------------------------------------|
| `vw_perfil_heroi`     | Perfil do herói (cargo, equipe, habilidades, reputação)                                      |
| `vw_resumo_missoes`   | Resumo de missões com contagem de eventos e participantes                                     |
| `vw_ranking_reputacao`| Ranking de reputação pública e governamental                                                 |
| `vw_indicadores_incidentes` | Indicadores financeiros e de severidade de incidentes                                  |

As views são mapeadas como entidades somente‑leitura (ou DTOs), servindo de base para dashboards e relatórios.

## 📈 Cenários de demonstração

- CRUD completo: habilidades, equipes, missões
- Relacionamentos complexos: equipes com membros; missões com participantes e eventos
- Dashboards: incidentes e indenizações agregadas por severidade
- ABAC/RLS (conceitual): níveis de acesso a bases e licenças de operação

## 📖 Exemplos no Praxis UI (conceituais)

- Tabela: `<praxis-table resourcePath="/api/human-resources/funcionarios" />`
- CRUD inferido: `<praxis-crud [metadata]="{ component: 'praxis-crud', resource: { path: '/api/human-resources/indenizacoes', idField: 'id' }, actions: [{ action: 'create' }, { action: 'view' }, { action: 'edit' }, { action: 'delete' }] }" />`
- Dashboard: `<praxis-dashboard resourcePath="/api/risk-intelligence/vw-indicadores-incidentes" />`

## 🔌 Integração com o Praxis Metadata Starter

O backend publica automaticamente:

- `/schemas/filtered` — JSON Schema enriquecido com metadados UI (x‑ui)
- `/filter` — busca paginada por DTO de filtro tipado
- `/options` — opções remotas para selects/autocomplete

Esses contratos são consumidos pelo Praxis UI, que gera interfaces dinâmicas declarativamente.

No baseline atual do runtime oficial:

- `/schemas/filtered` continua sendo a fonte estrutural de schema
- `capabilities.operations` governa a semântica canônica de `create`, `view`, `edit` e `delete`
- `_links` entram como camada operacional/contextual
- `DELETE /batch` e workflow actions destrutivas não substituem o `delete` canônico item-level do recurso

---

Para uma visão ampla do ecossistema e como este Quickstart se encaixa, consulte o README principal.
