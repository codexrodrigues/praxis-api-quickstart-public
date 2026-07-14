# Banco de Demonstracao - Universo dos Herois

Este projeto demonstra o uso do Spring Boot 3 (Java 21) integrado a um banco PostgreSQL 17, utilizando um dominio tematico inspirado em herois para exemplificar CRUDs, relacionamentos e regras de negocio. O objetivo e fornecer uma base rica de dados e endpoints REST para uso com as bibliotecas do ecossistema Praxis, incluindo o `praxis-metadata-starter` e, no front, o Praxis UI.

- Backend: Spring Boot 3.x, Spring Data JPA/Hibernate
- Banco: PostgreSQL 17
- Contratos: OpenAPI 3.1 + extensao x-ui gerada pelo starter

## Dominio e proposito

O dominio modela uma organizacao global de herois, contendo cadastros, missoes, bases operacionais, acordos internacionais e relatorios de desempenho. Cada entidade foi pensada para cobrir casos comuns de aplicacoes corporativas: relacionamentos 1-N e N-N, enums e filtros ricos.

### Modulos principais

| Modulo | Finalidade | Principais tabelas |
| --- | --- | --- |
| Recursos Humanos | Cadastro e gestao de herois, funcionarios, cargos, departamentos e historico | `funcionarios`, `cargos`, `departamentos`, `historicos_salariais`, `enderecos`, `dependentes` |
| Beneficios governados | Fila de solicitacoes elegiveis, lifecycle aprovativo e ledger de execucao | `extraordinary_benefit_request`, `extraordinary_benefit_grant_effect` |
| Habilidades & Identidades | Define habilidades, origens e codinomes | `habilidades`, `funcionario_habilidades`, `identidades_secretas` |
| Bases & Equipes | Bases de operacao, equipes e niveis de acesso | `bases`, `equipes`, `equipe_membros`, `base_acessos` |
| Missoes & Ameacas | Ameacas globais, missoes, participantes e eventos | `ameacas`, `missoes`, `missao_participantes`, `missao_eventos` |
| Logistica & Tecnologia | Equipamentos, veiculos e alocacoes | `equipamentos`, `veiculos`, `equipamento_alocacoes`, `veiculo_missao_usos` |
| Compliance & Incidentes | Acordos, licencas, incidentes e indenizacoes | `acordos_regulatorios`, `licencas_operacao`, `incidentes`, `indenizacoes` |
| Comunicacao & Midia | Sinais de socorro, reputacao e mencoes na imprensa | `sinais_socorro`, `reputacoes`, `mencoes_midia` |

Observacao: neste Quickstart, os recursos estao separados por dominio de rota, como `/api/human-resources/...`, `/api/operations/...`, `/api/assets/...`, `/api/risk-intelligence/...` e `/api/demo/...`.

## Endpoints REST

Padrao de rotas por recurso:

- GET `/{grupo}/{recurso}` - lista ou filtro paginado
- GET `/{grupo}/{recurso}/{id}` - detalhe
- POST `/{grupo}/{recurso}` - criacao
- PUT `/{grupo}/{recurso}/{id}` - atualizacao
- DELETE `/{grupo}/{recurso}/{id}` - exclusao

Endpoints utilitarios fornecidos pelo starter integram com o Praxis UI para geracao dinamica de telas:

- POST `{recurso}/filter` - pesquisa paginada com DTO completo
- POST `{recurso}/options/filter` - opcoes leves `{id,label}` por filtro
- GET `{recurso}/options/by-ids` - reidrata opcoes por IDs, preservando a ordem
- GET `/schemas/filtered` - JSON Schema enriquecido para a operacao alvo

## Estrutura de demonstracao para o Praxis

- Metadados ricos via `@UISchema` e Bean Validation
- Geracao automatica de telas no Praxis UI com base no contrato
- Suporte a dashboards e relatorios com views agregadas
- Contratos versionaveis e com ETag para cache eficiente

## Migrations operacionais

O banco de demonstracao tem duas fronteiras: o datasource operacional da API (`spring.datasource.*`) e o datasource do Praxis Config Starter (`config.datasource.*`). O Flyway do host aponta para o config/RAG store; mudancas do schema operacional da API sao versionadas separadamente em `db/operational-migrations`.

Consulte [`OPERATIONAL-DATASOURCE-MIGRATIONS.md`](OPERATIONAL-DATASOURCE-MIGRATIONS.md) para aplicar a trilha e rodar o drift check antes de usar o cockpit publicado como evidencia final.

## Tecnologias principais

- Spring Boot 3.x (Java 21), Spring Data JPA / Hibernate
- PostgreSQL 17
- OpenAPI 3.1 + Praxis Metadata Starter
- Praxis UI (Angular, 20+)

## Views e indicadores

| View | Descricao |
| --- | --- |
| `vw_perfil_heroi` | Perfil do heroi, incluindo cargo, equipe, habilidades e reputacao |
| `vw_resumo_missoes` | Resumo de missoes com contagem de eventos e participantes |
| `vw_ranking_reputacao` | Ranking de reputacao publica e governamental |
| `vw_indicadores_incidentes` | Indicadores financeiros e de severidade de incidentes |

As views sao mapeadas como entidades somente leitura ou DTOs, servindo de base para dashboards e relatorios.

## Cenarios de demonstracao

- CRUD completo: habilidades, equipes e missoes
- Relacionamentos complexos: equipes com membros; missoes com participantes e eventos
- Dashboards: incidentes e indenizacoes agregadas por severidade
- ABAC/RLS conceitual: niveis de acesso a bases e licencas de operacao

## Exemplos no Praxis UI

- Tabela: `<praxis-table resourcePath="/api/human-resources/funcionarios" />`
- CRUD inferido: `<praxis-crud [metadata]="{ component: 'praxis-crud', resource: { path: '/api/human-resources/indenizacoes', idField: 'id' }, actions: [{ action: 'create' }, { action: 'view' }, { action: 'edit' }, { action: 'delete' }] }" />`
- Dashboard: `<praxis-dashboard resourcePath="/api/risk-intelligence/vw-indicadores-incidentes" />`

## Integracao com o Praxis Metadata Starter

O backend publica automaticamente:

- `/schemas/filtered` - JSON Schema enriquecido com metadados UI (`x-ui`)
- `/filter` - busca paginada por DTO de filtro tipado
- `/options` - opcoes remotas para selects/autocomplete

Esses contratos sao consumidos pelo Praxis UI, que gera interfaces dinamicas declarativamente.

No baseline atual do runtime oficial:

- `/schemas/filtered` continua sendo a fonte estrutural de schema
- `capabilities.operations` governa a semantica canonica de `create`, `view`, `edit` e `delete`
- `_links` entram como camada operacional/contextual
- `DELETE /batch` e workflow actions destrutivas nao substituem o `delete` canonico item-level do recurso

Para uma visao ampla do ecossistema e como este Quickstart se encaixa, consulte o README principal.
