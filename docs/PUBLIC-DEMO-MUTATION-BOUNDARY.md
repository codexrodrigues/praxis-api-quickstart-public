# Fronteira de mutação da demo pública

## Decisão em preparação

A demo pública do Quickstart precisa preservar um dataset canônico, reproduzível e
adequado à apresentação. Ao mesmo tempo, a Prova Enterprise precisa permitir que
uma pessoa autenticada explore ações, drawers e formulários sem que um registro de
experimento permaneça visível para o próximo visitante.

Esta é uma decisão de arquitetura do host operacional. Não é responsabilidade da
landing, nem um comportamento a ser reconstruído por JSON ou por lógica Angular.

## Inventário de aderência

| Item existente | Aderência | Limite observado |
| --- | --- | --- |
| `praxis_demo_dataset_guard` e fingerprint do dump público | suportado-parcialmente | Protege seeds opt-in, mas não alterações posteriores no banco publicado. |
| `APP_SECURITY_WRITE_DISABLED` | ja-suportado-so-ux | Torna a demo totalmente somente leitura; não prova comandos autenticados. |
| Login de demonstração e ações tipadas | ja-suportado-mal-nomeado-ou-mal-materializado | Permite explorar fluxo real, mas hoje grava no dataset compartilhado. |
| E2E da landing | suportado-parcialmente | Exercita a jornada, porém não estabelece isolamento de dados nem reconciliação do ambiente. |

Portanto, não há justificativa para uma lista de e-mails, IDs ou aliases no
frontend que tente esconder resíduos. O problema é a fronteira operacional de
mutação, não o renderizador de tabela.

## Proposta canônica

1. **Dataset público canônico somente leitura.** O host publicado expõe leituras,
   schemas, capabilities, filtros, exports e superfícies a partir do dump guardado.
   A revisão de dados canônicos ocorre exclusivamente por seed versionado e por
   operador autenticado.
2. **Sessão de demonstração mutável isolada.** Um login de demonstração recebe um
   contexto de sandbox efêmero, com TTL, dono, correlação e escopo explícitos. As
   mutações nunca escrevem no dataset público compartilhado.
3. **Materialização de demonstração no host.** O Quickstart roteia o contexto de
   sandbox para um datasource/esquema isolado ou para uma cópia transacional
   descartável. O runtime Angular apenas consome as capabilities e outcomes
   publicados; não decide isolamento por rótulo, rota ou flag local.
4. **Encerramento e evidência.** Expiração, logout e limpeza controlada removem
   somente o escopo da sessão. A operação registra outcome técnico redigido e
   nunca remove dados fora da sandbox.

O `APP_SECURITY_WRITE_DISABLED=true` continua sendo uma postura válida para
apresentações estritamente read-only. Ele não substitui a sandbox quando a prova
precisa mostrar inclusão, alteração, exclusão ou workflow real.

## Limites de contrato

Ainda não existe uma lacuna comprovada em `x-ui`, `/schemas/filtered` ou nos
contratos exportados de `@praxisui/*`. A primeira implementação deve reutilizar
os contextos autenticados e as actions/capabilities existentes do Quickstart.
Somente depois do inventário do fluxo de autenticação e datasource será possível
decidir se um contrato público adicional é realmente necessário.

Qualquer indicador de sandbox publicado deve ser descritivo e de baixo risco
(por exemplo, `mode`, expiração e capacidade de persistência). Ele não pode expor
connection strings, chaves de limpeza, dados de outros visitantes ou a composição
do banco canônico.

## Descoberta técnica do Quickstart

O host já fornece os blocos corretos para compor a solução, mas nenhum deles
isola uma sessão ainda:

- `DataSourceConfig` mantém o domínio do Quickstart em uma única unidade JPA
  (`apiDataSource`, `apiEntityManagerFactory` e `apiTransactionManager`); o
  datasource do Config Starter é separado e não pode ser reutilizado para dados
  de RH;
- `AuthController` emite um JWT HttpOnly com `subject`, papel e authorities;
  `CookieJwtAuthenticationFilter` reconstrói o principal, mas não publica um
  identificador de sandbox nem TTL de dados;
- ações e CRUDs executam nos mesmos repositories transacionais que atendem a
  leitura pública. Uma troca local de `resourcePath`, flag da landing ou filtro
  não altera esse destino de escrita.

Isso elimina duas alternativas inadequadas: fazer overlay de dados no Angular ou
usar o datasource do Config Starter como banco temporário de negócio.

### Opções avaliadas

| Opção | Decisão | Motivo |
| --- | --- | --- |
| Flag `write-disabled` | manter apenas para modo read-only | não prova a jornada corporativa mutável |
| Limpeza por ID/e-mail após cada uso | rejeitada | não é escopada, pode apagar dado válido e não suporta concorrência |
| Schema compartilhado com reset global | rejeitada | um visitante pode observar ou perder a alteração de outro |
| Schema por sandbox no mesmo host | viável, com avaliação de operação | exige roteamento JPA, clonagem do snapshot e guardrails de schema por request |
| Instância temporária do Quickstart + banco restaurado por sandbox | recomendada | isola processo, datasource e dados; mantém o host e contratos de negócio sem forks |

O modelo recomendado é uma **instância efêmera por sandbox**, criada a partir da
mesma versão publicada do Quickstart e de um snapshot assinado do dump público.
O gateway entrega uma URL/cookie de sessão limitada; expiração encerra a
instância e seus dados. O dataset público não recebe comandos autenticados.

Antes de implementar, a plataforma deve decidir quem orquestra essa instância e
como o gateway associa o principal à sandbox. Essa é uma nova capacidade
operacional do host/infraestrutura, não uma semântica de formulário ou tabela.

## Mapa de impacto para implementação

| Dono | Responsabilidade | Evidência mínima |
| --- | --- | --- |
| `praxis-api-quickstart` | autenticação de demo, isolamento do datasource, TTL, logout/cleanup e auditoria redigida | integração autenticada: dois usuários não observam registros um do outro; expiração não altera o dump canônico |
| `praxis-metadata-starter` | somente se capabilities/contexto existentes não puderem explicar a postura de sandbox | schema/capabilities e testes downstream no Quickstart |
| `praxis-ui-angular` | materializar apenas capabilities/contexto que já sejam publicados | testes focais de CRUD/feedback e consumidor direto |
| `praxis-ui-landing-page` | revelar claramente o modo demonstrativo e manter E2E sem mutação do dataset compartilhado | E2E local e smoke hospedado |

Risco de breaking change: baixo enquanto a primeira fase mantiver as APIs de
negócio e apenas mudar o destino operacional das mutações autenticadas. Qualquer
novo header, capability ou contrato exportado deve ser tratado como
`contrato-publico` antes de implementação.

## Sequência de execução recomendada

1. Excluir manualmente, com autorização explícita e trilha de auditoria, os
   resíduos já identificados no dataset público.
2. Mapear a autenticação de demonstração, o datasource operacional e o lifecycle
   de cada comando que a Prova Enterprise expõe.
3. Implementar uma sandbox host-owned com cleanup estritamente escopado por
   sessão; não usar jobs que varram ou apaguem dados por padrões de texto.
4. Criar testes de isolamento, expiração, idempotência e restauração do dataset,
   depois atualizar a landing e os smokes hospedados.
5. Publicar a prova somente após o gate confirmar que o dump canônico permanece
   intacto depois de uma jornada autenticada completa.

## Situação atual

Esta proposta não executa limpeza, reset ou mutação de banco. Os registros já
presentes exigem autorização operacional específica para remoção. O documento
define a direção correta para evitar recorrência sem transformar a landing em
fonte de regras de negócio.
