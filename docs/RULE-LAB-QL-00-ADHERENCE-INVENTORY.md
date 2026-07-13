# Laboratório de regras — QL-00 Adherence Inventory

- Data-base: 2026-07-13
- Classificação: `arquitetural` e `transversal`
- Caso: Concessão de Benefício Corporativo Extraordinário
- Host de prova: `praxis-api-quickstart`
- Estado: `DISCOVERY_READY concluído`; implementação ainda bloqueada

## 1. Pergunta canônica

O que a plataforma já sabe e executa, o que está apenas parcialmente
materializado e quais lacunas reais impedem o Quickstart de provar um RuleSet
composto com complexidade comparável à futura migração Ergon?

Este inventário não cria contrato público. Ele orienta QL-01 e impede que o
Quickstart duplique semântica pertencente aos starters ou ao runtime.

## 2. Evidências consultadas

- `praxis-rules-engine` `v0.1.0-beta.6`, tag sobre o commit
  `133c91811b1fbb2b22a51ad4ba51e3c2d9ff78ae`; o workflow oficial de release
  `29270255302` concluiu com sucesso e a coordenada
  `io.github.codexrodrigues:praxis-rules-engine:0.1.0-beta.6` foi resolvida
  diretamente do Maven Central em repositório local temporário;
- `PraxisJsonLogicEngine`, `JsonLogicEvaluationOptions` e
  `PraxisJsonLogicOperatorRegistry`, incluindo roots autorizados, `nowUtc`,
  `userTimeZone`, limits e namespace obrigatório para operadores do host;
- `praxis-config-starter` Domain Rule definition, simulation, publication,
  materialization, timeline e lifecycle;
- `DomainRuleApprovalPolicyResolver`, `DomainRuleWorkflowActionPolicyResolver`,
  `DomainRuleOptionSourcePolicyResolver` e
  `DomainRuleBackendValidationPolicyResolver` no Quickstart;
- `EventosFolhaPilotIntegrationTest`, incluindo ETag, action, idempotência,
  transição e auditoria;
- resources, schemas, actions, surfaces e capabilities do Quickstart.

## 3. Inventário de aderência

| Necessidade | Evidência atual | Classificação | Decisão para o laboratório |
| --- | --- | --- | --- |
| Validar e avaliar JSON Logic em Java | engine, options, limits e diagnostics existem, mas ainda não estão materializados no laboratório | `ja-suportado-mal-nomeado-ou-mal-materializado` | consumir o artefato público; não criar evaluator local |
| Tempo determinístico | `nowUtc` e `userTimeZone` são explícitos, mas ainda não estão materializados nas fixtures do laboratório | `ja-suportado-mal-nomeado-ou-mal-materializado` | toda fixture e avaliação declara ambos |
| Missing distinto de `null` | tratado internamente pelo engine, ainda sem contrato de facts do laboratório | `ja-suportado-mal-nomeado-ou-mal-materializado` | facts declaram required/nullability separadamente |
| Registry e operators de host | registry introspectável e namespace obrigatório, ainda não integrados ao host | `ja-suportado-mal-nomeado-ou-mal-materializado` | regras Java entram no registry do host; sem class name em payload |
| Resultado JSON Logic | valor, truthiness e issues estáveis | `suportado-parcialmente` | reutilizar no binding; falta resultado consolidado de RuleSet |
| RuleSet, slots, bindings e policies | inexistentes no engine e Config Starter | `lacuna-real-de-contrato` | fechar owner e contratos em P2F-ADR-01–04 |
| DAG e plano compilado | inexistentes como contrato compartilhado | `lacuna-real-de-contrato` | publicar ordem resolvida; host não improvisa ordenação |
| Resultado composto em cinco estados | engine não modela decisão composta | `lacuna-real-de-contrato` | definir antes de QL-02 |
| Definition governada | tenant, environment, key/version, owner, condition, parameters e governance existem | `ja-suportado-mal-nomeado-ou-mal-materializado` | reutilizar; não confundir definition com snapshot |
| Aprovação/publicação | exige definition `approved`/`active` e autorização governada, ainda sem definição do caso do laboratório | `ja-suportado-mal-nomeado-ou-mal-materializado` | preservar diagnostics e timeline |
| Simulation do Config Starter | analisa coverage, targets, approvals e readiness | `ja-suportado-mal-nomeado-ou-mal-materializado` | usar para authoring; nunca reportar como avaliação de facts |
| Materialization governada | target, stable key, source hash, status e payload existem | `suportado-parcialmente` | falta target/schema canônico para RuleSet snapshot |
| Snapshot imutável e rollback | lifecycle genérico existe, contrato compilado não | `lacuna-real-de-contrato` | P2F-ADR-07 define snapshot e rollback |
| ETag de publicação de RuleSet | não comprovado no contrato Domain Rule focal | `suportado-parcialmente` | provar/criar no owner; não simular no host |
| Consumo no Quickstart | resolvers leem materialization `applied`, mas não materializam plano composto | `suportado-parcialmente` | útil para projeções simples, não para plano composto |
| Fail policy atual | ausência/erro de dados retorna `Optional.empty()` | `suportado-parcialmente` | inadequado para protected guard; nova boundary falha explicitamente |
| Tenant/environment no host | headers com default `default/dev` | `suportado-parcialmente` | RuleSet protegido exige scope explícito |
| Resource/action/discovery | baseline metadata-driven existe, ainda sem recurso concreto do laboratório | `ja-suportado-mal-nomeado-ou-mal-materializado` | reutilizar depois de `CONTRACT_READY` |
| ETag, idempotência e auditoria de command | Eventos de Folha possui prova real, ainda não aplicada ao novo slice | `ja-suportado-mal-nomeado-ou-mal-materializado` | reutilizar infraestrutura, sem copiar regra de folha |
| Recurso de benefício extraordinário | infraestrutura canônica existe; recurso concreto do laboratório é trabalho local pendente | `suportado-parcialmente` | criar slice separado somente em QL-04 |
| Dependência do Rules Engine | artefato público existe; integração no Quickstart está pendente | `suportado-parcialmente` | adicionar versão pública somente em QL-02 |
| Fact providers e registry Java | engine suporta registry; providers e composição do host estão pendentes | `suportado-parcialmente` | owner é host; interfaces aguardam contrato compartilhado |
| Shadow, observation e redaction | inexistentes para RuleSet composto | `lacuna-real-de-contrato` | fechar P2F-ADR-08/12 |
| Transformação tipada | inexistente | `lacuna-real-de-contrato` | estímulo de ADR; nunca mutação escondida em JSON Logic |

## 4. Decisões de convergência

1. O Quickstart será consumidor e prova operacional, não owner do meta-modelo.
2. `DomainRuleDefinition` continua sendo decisão governada; não é snapshot.
3. Simulation do Config Starter continua sendo authoring simulation. Avaliação
   de facts ocorre no host com o runtime publicado.
4. O engine executa bindings JSON Logic, mas não recebe Spring, banco,
   snapshot store, tenant resolution, workflow ou effects.
5. O benefício será separado de `EventosFolha`; folha será efeito downstream.
6. Protected guards não usarão fallback silencioso nem tenant default.
7. Nenhum DTO compartilhável nasce em `com.example.*`; o owner será decidido
   pelo P2F-ADR-01.

## 5. Lacunas bloqueantes

| Gap | Owner a decidir/canônico | ADR | Bloqueia |
| --- | --- | --- | --- |
| Contrato Java mínimo | P2F-ADR-01 | 01 | DTOs e runtime adapters |
| Identidade/lifecycle RuleSet e snapshot | Config + contrato compartilhado | 02/07 | publicação e loader |
| Slots, bindings e policies | contrato compartilhado | 03 | planner e composição |
| Stages, DAG, cardinalidade e transformação | contrato + host | 04 | ordem e stress Ergon-like |
| Result/error/fail policy | contrato compartilhado | 08 | HTTP, shadow e guards |
| Cache/hot reload | host runtime | 09 | snapshots v1/v2 |
| Observation/redaction | contrato + host | 12 | shadow e operação |

## 6. Gate QL-00

QL-00 está concluído porque capacidades e gaps foram classificados com owner.
QL-01 pode registrar contratos candidatos e goldens. QL-02 permanece bloqueado
até aprovação, no mínimo, de P2F-ADR-01, 02, 03, 04, 06 e 08. Esta conclusão
não abre Fase 9 nem autoriza RN Ergon real.

## 7. Reprodutibilidade

A disponibilidade pública do runtime foi comprovada sem instalar override no
Quickstart:

```powershell
mvn "-Dmaven.repo.local=<diretorio-temporario>" dependency:get `
  "-Dartifact=io.github.codexrodrigues:praxis-rules-engine:0.1.0-beta.6"
```

QL-02 deverá consumir essa coordenada pública. Um build baseado em
`0.0.1-SNAPSHOT`, `install` local ou `systemPath` não satisfaz o gate.
