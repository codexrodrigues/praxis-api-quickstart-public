# Guia de grounding de negocio para hosts Praxis

Este guia descreve como um backend que usa os starters Praxis deve publicar
conhecimento suficiente para que a IA aprenda o negocio do host em runtime, sem
depender de codigo-fonte, nomes hardcoded ou memoria de exemplos do quickstart.

O quickstart e o host operacional de referencia. Os dominios de RH, operacoes,
procurement, ativos e risco existem para demonstrar, por HTTP real, o padrao
que qualquer outro negocio deve seguir.

Este guia nao e uma lista de anotacoes decorativas. Ele e um criterio de aceite:
um host Praxis so esta bem documentado quando o catalogo publicado, o contexto
persistido e os smokes conseguem provar que a IA entende o dominio por evidencias
governadas, nao por convencao local, memoria de exemplo ou patch direto de UI.

## Principio central

A IA deve aprender o negocio por superficies Praxis publicadas pelo proprio host:

- `@ApiResource(resourceKey=...)` para identidade publica do recurso;
- `@Operation(summary=..., description=...)` para finalidade de cada operacao;
- `@Schema(description=...)` para definicao de negocio de DTOs e campos;
- `@UISchema(...)` para comportamento visual derivado, nao para substituir semantica;
- `@DomainGovernance(...)` e `@AiUsagePolicy(...)` para classificacao, restricoes e uso por IA;
- option sources, stats, actions, capabilities e HATEOAS para capacidades operacionais;
- `GET /schemas/domain?resourceKey=...` para catalogo semantico emitido pelo host;
- `POST /api/praxis/config/domain-catalog/ingest` para persistir a release semantica;
- `GET /api/praxis/config/domain-catalog/context` para recuperar grounding de prompt;
- Project Knowledge e Domain Knowledge change sets para conhecimento governado adicional.

Se uma decisao de IA so funciona porque o starter conhece palavras como
`funcionario`, `salario`, `folha` ou `fornecedor`, o modelo esta errado. Essas
palavras podem existir no quickstart, mas devem chegar a IA pelo catalogo,
metadata, schemas e conhecimento governado do host.

## Fronteira canonica

Separe explicitamente a origem de cada semantica:

- `praxis-metadata-starter` publica o contrato estrutural e semantico do host:
  `x-ui`, `/schemas/filtered`, `/schemas/domain`, surfaces, actions,
  capabilities, HATEOAS, OpenAPI enriquecido e resolucao schema/operacao.
- `praxis-config-starter` governa persistencia, contexto recuperavel, catalogo
  ingerido, Project Knowledge, Domain Knowledge change sets, domain rules,
  simulacao, aprovacao, publicacao e materializacoes.
- `praxis-api-quickstart` prova a integracao downstream em um host real. Ele pode
  conter hooks de consumo das materializacoes aplicadas, mas nao deve virar fonte
  primaria da regra, do catalogo persistido ou da policy canonica.

Quando a documentacao do host contradiz essa fronteira, a documentacao esta
fraca mesmo que o endpoint funcione. O texto deve deixar claro se um trecho
emite semantica, persiste uma release, recupera contexto, authora uma decisao ou
apenas consome uma materializacao publicada.

## Checklist para cada recurso

Um recurso esta AI-ready quando responde bem a estas perguntas sem leitura de
codigo-fonte:

- Qual e o bounded context dono do recurso?
- Qual e o `resourceKey` canonico?
- Quais nomes humanos, aliases e termos de negocio apontam para esse recurso?
- O recurso e transacional, read-only, analitico, lookup, workflow ou action host?
- Quais operacoes existem e para que servem?
- Quais campos identificam, descrevem, filtram, agregam, ordenam ou conectam o recurso?
- Quais campos sao pessoais, financeiros, regulatorios, operacionais ou sensiveis?
- Quais campos podem ser usados pela IA para explicar, raciocinar, treinar ou authorar regras?
- Quais relacionamentos devem ser seguidos antes de sugerir uma tela, regra ou automacao?
- Quais option sources e stats existem para selecao, filtros e dashboards?
- Quais actions/workflows existem e quais materializacoes governadas podem afetar seu runtime?
- Quais evidencias sustentam a interpretacao semantica?
- Qual smoke prova que esse recurso foi entendido por catalogo/contexto em vez de
  hardcode?

## Camadas de anotacao esperadas

### Recurso

Use `@ApiResource` em todo controller publico de recurso. O `resourceKey` deve ser
estavel, legivel e orientado a dominio, por exemplo:

```java
@ApiResource(value = ApiPaths.Operations.MISSOES, resourceKey = "operations.missoes")
```

Esse valor e a chave que conecta schemas, catalogo, contexto de prompt,
domain-rules, materializacoes e smokes.

### Operacoes

Cada operacao relevante deve explicar sua finalidade em linguagem de negocio,
nao apenas repetir o verbo HTTP:

```java
@Operation(
    summary = "Filtrar missoes",
    description = "Lista missoes por status, prioridade, base, periodo e contexto para acompanhamento operacional, dashboards e composicao de detalhe."
)
```

Uma IA deve conseguir diferenciar uma busca operacional, uma view analitica, um
lookup de formulario e uma action de workflow lendo essas descricoes.

### DTOs e campos

`@Schema(description=...)` deve explicar significado, finalidade, limites e
relacionamentos do atributo. `@UISchema` deve ficar restrito a apresentacao e
comportamento de UI.

Bom:

```java
@Schema(description = "Status de ciclo de vida da missao, usado para governar acoes permitidas, paineis de acompanhamento e regras de atraso.")
@UISchema(label = "Status", controlType = FieldControlType.SELECT)
private MissaoStatus status;
```

Fraco:

```java
@Schema(description = "Status")
@UISchema(label = "Status")
private MissaoStatus status;
```

### Governanca e uso por IA

Campos sensiveis devem publicar governanca explicitamente:

```java
@DomainGovernance(
    kind = DomainGovernanceKind.PRIVACY,
    dataCategory = "personal_identifier",
    classification = "confidential",
    complianceTags = {"LGPD", "GDPR"},
    aiUsage = @AiUsagePolicy(
        visibility = AiUsageMode.MASK,
        trainingUse = AiUsageMode.DENY,
        ruleAuthoring = AiUsageMode.REVIEW_REQUIRED,
        reasoningUse = AiUsageMode.REVIEW_REQUIRED
    )
)
```

Sem essa camada, a IA deve tratar decisoes sensiveis como incertas e pedir
clarificacao ou revisao, em vez de inventar politica.

### Stats, dashboards e analytics

Recursos analiticos devem publicar campos agregaveis e dimensoes por
`StatsFieldRegistry`, alem de descricoes de operacao que deixem claro quando a
surface e propria para dashboard, ranking, time series ou comparacao.

Exemplos do quickstart:

- `human-resources.vw-analytics-folha-pagamento`;
- `risk-intelligence.vw-indicadores-incidentes`;
- `operations.vw-resumo-missoes`.

O starter generico nao deve conhecer esses nomes. Ele deve descobrir que um
recurso serve para analytics pelas capacidades publicadas.

### Option sources e selecao

Lookups devem publicar option sources com politica e descricao suficientes para
que a IA saiba quando propor selecao, filtro, bloqueio de elegibilidade ou
materializacao de `option_source`.

Quando uma regra governada afetar selecao, a decisao canonica pertence a
`domain_rule_definition`; o host apenas consome a materializacao aplicada.

### Actions, workflows e validacoes de backend

Actions operacionais e validacoes de backend devem seguir a mesma regra de
fronteira. O quickstart pode provar que uma materializacao aplicada bloqueia uma
action ou um comando mutavel, mas a decisao canonica deve nascer no fluxo
governado de `domain-rules`.

Exemplos do quickstart que servem como prova downstream:

- `selection_eligibility -> option_source` para fornecedores em procurement;
- `backend_validation -> resource_validation_policy` para pedidos de compra;
- `workflow_action -> workflow_action_policy` para actions de folha;
- `approval_policy -> resource-action-approval` para aprovacao operacional.

Esses exemplos devem ser documentados como consumo de decisao publicada. Nao os
descreva como regra local do service, regra de formulario ou workaround de UI.

## Fluxo recomendado para ensinar o negocio a IA

1. Anotar controllers com `@ApiResource` e operacoes com descricao de negocio.
2. Anotar DTOs e filtros com `@Schema` rico, `@UISchema` separado e governanca onde necessario.
3. Publicar option sources, stats, actions e capabilities quando o recurso suportar essas capacidades.
4. Validar `GET /schemas/domain?resourceKey=<resourceKey>`.
5. Ingerir o catalogo com `scripts/ensure-domain-catalog-context.sh`.
6. Validar contexto com `scripts/verify-domain-catalog-context.sh`.
7. Validar authoring com `scripts/verify-domain-catalog-authoring-runtime.sh`.
8. Para conhecimento adicional, usar Domain Knowledge change sets em vez de hardcode.
9. Para regras, usar intake, simulation, definitions, publications e materializations.
10. Validar runtime com os smokes locais adequados.

## Como o quickstart deve provar este guia

O projeto de exemplo e exemplar quando a documentacao aponta do principio para a
prova operacional. No minimo, revise estes vinculos:

| Exigencia do guia | Prova esperada no quickstart |
| --- | --- |
| Identidade canonica do recurso | Controllers com `@ApiResource(resourceKey=...)` e `ApiPaths` como ponto de sincronizacao contratual. |
| Definicao de negocio | DTOs, filtros e operacoes com `@Schema`/`@Operation` ricos, sem copiar apenas labels de UI. |
| Governanca e uso por IA | `@DomainGovernance`/`@AiUsagePolicy` publicados em `/schemas/domain` e persistidos em `/domain-catalog/items`. |
| Catalogo recuperavel | `scripts/ensure-domain-catalog-context.sh` e `scripts/verify-domain-catalog-context.sh`. |
| Authoring com grounding | `scripts/verify-domain-catalog-authoring-runtime.sh` e contrato `contextHints.domainCatalog`. |
| Regras governadas | `scripts/verify-domain-rules-runtime.sh` cobrindo intake, simulation, publication e materializations quando o runtime expuser essas surfaces. |
| Materializacoes consumidas pelo host | Smokes focados de `option_source`, `backend_validation`, `workflow_action` e `approval_policy`. |
| Conhecimento governado adicional | `scripts/verify-domain-knowledge-change-set-runtime.sh` para criar, validar, aprovar, aplicar, auditar e reverter evidencias. |

Se um README, guia ou runbook disser que o quickstart "demonstra IA" sem apontar
para uma dessas provas, a documentacao ainda esta incompleta. A narrativa deve
ser rastreavel ate uma superficie HTTP, script, teste isolado ou contrato
publicado.

## Como avaliar se o host esta pronto

Um host esta suficientemente documentado quando a IA consegue:

- identificar o recurso correto a partir de uma frase humana sem endpoints digitados;
- explicar por que escolheu aquele recurso, citando evidencias do catalogo;
- diferenciar formulario, tabela, dashboard, master-detail, lookup e action;
- sugerir campos existentes sem depender do usuario conhecer nomes tecnicos;
- pedir clarificacao quando houver ambiguidade real;
- respeitar governanca antes de usar campos sensiveis;
- propor decisoes como rascunho governado, nao como patch direto de UI;
- materializar apenas apos simulacao, aprovacao e publicacao quando o fluxo exigir.

Documentacao exemplar tambem deve permitir que um humano audite o mesmo caminho:

- onde a semantica foi emitida;
- onde a release foi persistida;
- qual contexto entrou no prompt ou no authoring;
- qual decisao foi simulada, aprovada e publicada;
- qual materializacao derivada foi aplicada;
- qual smoke prova o comportamento em runtime.

## Antipadroes que contaminam a plataforma

- Colocar nomes de dominio do quickstart dentro de um starter canonico.
- Mapear palavras humanas diretamente para endpoints fixos no starter.
- Gerar dashboards por nomes de campos conhecidos apenas por exemplo.
- Usar `/all` como resposta default para busca corporativa.
- Tratar `@UISchema(label=...)` como fonte primaria de significado.
- Transformar docs de exemplo em regra de runtime.
- Permitir que a IA invente recurso, campo, policy ou materializacao sem evidencia.
- Documentar o quickstart como "exemplo de CRUD" quando ele esta demonstrando
  grounding, governanca e materializacao de decisoes.
- Descrever hook de service como regra de negocio primaria quando ele apenas
  consome materializacao aplicada pelo `praxis-config-starter`.

## Regra para novos negocios

Ao criar um host Praxis para outro negocio, nao copie heuristicas de RH,
procurement ou missoes. Copie o padrao:

- identidade canonica por `resourceKey`;
- descricoes de negocio nas operacoes e DTOs;
- governanca explicita;
- capacidades declaradas;
- catalogo ingerido;
- contexto recuperavel;
- conhecimento adicional governado;
- smokes que provem que a IA aprende pelo runtime.

Se esse padrao nao for suficiente para a IA authorar corretamente, a melhoria
deve ser feita nos contratos e mecanismos Praxis, nao em atalhos de dominio no
starter.

## Criterio final de revisao

Ao revisar um novo host ou o proprio quickstart, considere a documentacao
exemplar somente se ela responder, com links ou comandos reproduziveis:

1. Qual e a fonte canonica de cada semantica?
2. Como a IA recupera essa semantica em runtime?
3. Como a governanca restringe raciocinio, treino, visibilidade e authoring?
4. Como uma decisao proposta vira draft, simulacao, aprovacao, publicacao e
   materializacao?
5. Como o host prova que consome a materializacao sem redefinir a regra?
6. Qual validacao local minima demonstra esse caminho?
