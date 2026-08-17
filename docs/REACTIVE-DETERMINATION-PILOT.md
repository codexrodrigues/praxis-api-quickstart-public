# Reactive Determination pilot

Este piloto prova o uso de uma determinacao reativa corporativa em um host real sem promover o
formulario ou o schema publico a fonte da regra de negocio.

## Casos de referencia

O formulario de endereco observa `/cep` e invoca a capability autenticada
`determinePostalAddress`. A resposta determina `/logradouro`, `/bairro`, `/cidade` e `/estado`.
O endpoint e idempotente e nao persiste estado. `EnderecoService` revalida os mesmos fatos no
comando final de create/update, de modo que ignorar, cancelar ou adulterar a chamada reativa nao
contorna a integridade do dominio.

O diretorio de CEPs e pequeno, deterministico e ficticio por design. CEP desconhecido retorna
`422`; nao existe fallback inventado.

O segundo caso cobre uma cadeia financeira real. A primeira determinacao observa `/salarioBruto` e
`/totalDescontos` e produz `/salarioLiquido` com formula versionada e arredondamento explicito. A
segunda observa `/ano`, `/mes` e o `/salarioLiquido` ja determinado e produz `/dataPagamento`.
O runtime agenda a segunda capability somente depois do commit logico da primeira; create/update
reexecutam as duas determinacoes e rejeitam com `409` qualquer valor derivado manipulado.

`payroll-calendar-v1` e uma politica demonstrativa do host: usa o quinto dia de segunda a sexta do
mes seguinte (ou o ultimo dia da competencia quando o liquido e zero) e deliberadamente nao modela
feriados. Um host corporativo real deve substituir essa decisao por um provider governado com seu
calendario laboral, sem mover a regra para o formulario.

## Fronteiras provadas

- Metadata publica em `x-ui.reactiveDeterminations` somente o binding estrutural tenant-neutral;
- o host registra operationIds e JSON Pointers, nunca URL, headers ou script autorado;
- o Metadata Starter resolve method, href e schemas pelo OpenAPI canonico;
- a capability executora e uma avaliacao idempotente de negocio: permanece disponivel quando
  persistencia esta em `write-disabled`, mas exige o papel business `ADMIN`. Principals tecnicos de
  authoring, aprovacao ou publicacao nao recebem acesso ao data plane por transitividade;
- Config governa a cadeia de folha como um unico `DomainRuleSnapshot` imutavel, sem copiar seu
  conteudo para o schema publico e cacheavel. O RuleSet
  `human-resources.payroll.reactive-determinations` contem exatamente os dois passos e declara a
  dependencia de `payment-date` sobre `net-salary`;
- o host le esse aggregate pelo `PublishedRuleSnapshotHeadReader`, usando tenant/environment
  resolvidos pelo principal server-side. Owner, host contract, grafo completo, duas proveniencias,
  vigencia e hash compilado sao verificados antes de selecionar qualquer provider;
- a mesma requisicao HTTP fixa um unico snapshot/hash/ETag/revisao para os dois passos. Um refresh
  concorrente nao pode combinar salario v1 com calendario v2;
- head ausente, store Config indisponivel, hash divergente, provenance incompleta, escopo cruzado ou
  revisao reordenada falham com `503`; o piloto nao executa fallback local silencioso;
- v1 -> v2 e rollback v2 -> v1 sao aceitos apenas com `activationRevision` monotona e novo ETag
  opaco. O hash de conteudo nao e usado como ETag, fechando a corrida ABA;
- create e edit compartilham a mesma determinacao, mas recebem scopes exatos por operationId;
- schema de response nao publica o binding de formulario.

## Operacoes

| Papel | Metodo e path | operationId |
| --- | --- | --- |
| Form create | `POST /api/human-resources/enderecos` | `createAddress` |
| Form edit | `PUT /api/human-resources/enderecos/{id}` | `updateAddress` |
| Determinacao | `POST /api/human-resources/enderecos/determinations/postal-address` | `determinePostalAddress` |
| Form create de folha | `POST /api/human-resources/folhas-pagamento` | `createPayroll` |
| Form edit de folha | `PUT /api/human-resources/folhas-pagamento/{id}` | `updatePayroll` |
| Determinacao financeira | `POST /api/human-resources/folhas-pagamento/determinations/net-salary` | `determinePayrollNetSalary` |
| Determinacao de data | `POST /api/human-resources/folhas-pagamento/determinations/payment-date` | `determinePayrollPaymentDate` |

O metadata de create pode ser inspecionado em:

```text
GET /schemas/filtered?path=/api/human-resources/enderecos&operation=post&schemaType=request
```

## Evidencia automatizada

A projecao derivada para o Policy Studio fica em
`src/test/resources/policy-studio/payroll-reactive-determinations-policy-studio-projection.v1.json`.
Ela referencia o RuleSet, os bindings reativos e esta fronteira operacional por
digest; nao replica conditions, IDs Config nem autoridade. O teste de contrato
falha quando identidade, ordem, facts, bindings ou hashes deixam de representar
a implementacao host-owned.

O sandbox do Policy Studio resolve as duas `ruleKey` de folha por identidade canônica e captura o
mesmo plano ativo já admitido por `AppliedReactiveDeterminationResolver`. O candidato troca apenas
a condição do binding selecionado; o active, o head, o hash e a revisão permanecem imutáveis. Essa
lane compara a decisão determinística e não executa os providers financeiros, persiste folha ou
produz efeitos operacionais.

Os testes unitarios cobrem lookup, as duas decisoes financeiras, a revalidacao integral da cadeia,
tenant A/B, v1/v2, rollback anti-ABA, hash/provenance e pinagem do aggregate. O teste HTTP isolado
cobre os dois bindings em create e edit, ausencia no response schema, projecao sem tenant/headers e
execucao autenticada encadeando a resposta real da primeira capability na segunda. O teste HTTP de
escopo corporativo prova que headers conflitantes de tenant/environment nao vencem o principal
server-side e que ambos os providers dependem do mesmo head governado.

```bash
mvn -Dpraxis.core.version=8.0.0-rc.101 \
  -Dtest=PostalAddressDeterminationServiceTest,PayrollNetSalaryDeterminationServiceTest,PayrollPaymentDateDeterminationServiceTest,PayrollDeterminationChainServiceTest,AppliedReactiveDeterminationResolverTest,ReactiveDeterminationTenantScopeHttpTest test

mvn -Dpraxis.core.version=8.0.0-rc.101 \
  -Dtest=OpenApiGroupResolutionIsolatedIntegrationTest#shouldPublishAndExecutePostalAddressReactiveDetermination test

mvn -Dpraxis.core.version=8.0.0-rc.101 \
  -Dtest=OpenApiGroupResolutionIsolatedIntegrationTest#shouldPublishAndExecuteMultiInputPayrollDetermination test
```

O override de versao serve apenas para consumir o Metadata Starter instalado localmente durante o
desenvolvimento transversal; o `pom.xml` do host nao e alterado por esse fluxo.

## Prova hospedada descartavel

O entry point oficial
`scripts/workspace/Invoke-PayrollReactiveDeterminationHostedProof.sh` governa a lane inteira. Ele
exige confirmacao destrutiva explicita, commit Git completo, worktree limpa, workspace Render exato,
TTL maximo de duas horas e teto de custo de US$ 0,30. Antes de criar, remove somente recursos com os
nomes determinísticos do mesmo `SMOKE_RUN_ID` e prova sua ausencia. Depois cria PostgreSQL, proxy e
hosts A/B, fixa a branch no commit informado e espera estados terminais com timeout. O trap remove
o arquivo de secrets e solicita a remocao de todos os IDs registrados e da branch em sucesso,
falha, timeout ou sinal; o ledger so marca `deleted` depois de consultar o provider e preserva
`cleanup_failed` quando a ausencia nao foi comprovada.

O PostgreSQL descartavel recebe o dump versionado apenas no modo explicito
`hosted-public-demo-fixture` e somente quando o schema esta vazio. O restore e transacional,
serializado por advisory lock, valida o fingerprint `praxis-public-demo-2026-07-15` e depois executa
a trilha Flyway operacional completa. Esse dump e fixture de prova hospedada; nao e baseline de
producao corporativa. Os hosts iniciam com `ddl-auto=validate`, nunca `update`.

`Provision-PayrollReactiveDeterminationHostedFixture.py` cria ou retoma o aggregate v1 -> v2 de
cada tenant. O payload vem diretamente de `PayrollReactiveDeterminationRuleSet`; o provisioner nao
copia o grafo em Python. Em repeticao, ele verifica owner, tenant, environment, host contract, as
duas definicoes aprovadas, o RuleSet completo e o catalogo imutavel `[1,2]`, sem republicar conteudo.
No smoke de um laboratorio corporativo persistente, o modo opt-in
`HOSTED_FIXTURE_GOVERNANCE_LAB_BOOTSTRAP=true` deriva sessoes independentes de author e dos dois
composition approvers a partir da sessao publisher ja configurada no proprio laboratorio. O tenant
e o environment continuam sendo resolvidos pelo servidor; o runner nao os inventa nem transforma
headers de escopo em autoridade. Esse modo e exclusivo do governance lab e nao substitui IdP/IAM
em hosts corporativos.
Como esse laboratorio e persistente, um head cujo intervalo de validade terminou e renovado por uma
nova versao monotônica e imutavel do mesmo RuleSet, novamente ligada ao manifest e as duas
composition approvals. A prova descartavel preserva o contrato fechado `[1,2]`; somente o modo
persistente aceita a sequencia historica crescente e comprova que o catalogo contem o novo head.
Quando um head ja existe, o provisioner reutiliza somente os `definitionId` explicitamente ligados
a suas sources imutaveis. Sem head, ele cria novas versoes por maker-checker; nao promove uma
definition historica apenas porque seu status textual e `approved`, pois ela pode anteceder as
evidencias autenticadas e hash-bound exigidas pelo compositor atual.

Por fim, `Invoke-PayrollReactiveDeterminationHostedSmoke.sh` exige quatro sessoes
independentes: publisher A/B para a prova negativa e business admin A/B para as avaliacoes. Nunca
reutilize o cookie do publisher como se fosse um leitor de folha; o gate exige `403` para publisher
e `200` para o principal business antes de abrir a janela de falha.

O runner desabilita apenas o proxy do datasource Config. O datasource operacional permanece
conectado. Um `trap` restaura o proxy, encerra as quatro sessoes e remove os cookie jars mesmo em
falha. O output e um JSON redigido, sem cookies, credenciais, payload do snapshot ou mensagens de
excecao. O lifecycle oficial preserva o ledger de criacao/delecao e executa o cleanup externo; o
smoke continua sem autoridade para criar ou destruir infraestrutura por conta propria.

Variaveis obrigatorias:

- `BASE_URL_TENANT_A`, `BASE_URL_TENANT_B`, `ALLOWED_ORIGIN`;
- `BUSINESS_USERNAME_TENANT_A/B` e `BUSINESS_PASSWORD_TENANT_A/B`;
- `PUBLISHER_USERNAME_TENANT_A/B` e `PUBLISHER_PASSWORD_TENANT_A/B`;
- `TOXIPROXY_ADMIN_URL`, `TOXIPROXY_PROXY_NAME`, `HOSTED_PROOF_OUTPUT`.

Gate local antes de qualquer nova infraestrutura:

```bash
bash -n scripts/workspace/Invoke-PayrollReactiveDeterminationHostedProof.sh
bash -n scripts/workspace/Invoke-PayrollReactiveDeterminationHostedSmoke.sh
python3 -c 'compile(open("scripts/workspace/Provision-PayrollReactiveDeterminationHostedFixture.py").read(), "fixture", "exec")'
mvn -B -Dtest=OperationalDatasourceMigratorTest,OperationalDatasourceMigratorPostgresTest,OperationalDatasourceHostedBootstrapEmbeddedPostgresTest,PublicDemoOperationalBootstrapTest,PayrollReactiveDeterminationFixturePayloadTest,PayrollReactiveDeterminationHostedBootstrapContractTest,ReactiveDeterminationTenantScopeHttpTest,PayrollReactiveDeterminationHostedSmokeContractTest test
```

Exemplo de preflight autorizado (o comando cria recursos, portanto somente depois de merge, CI e
commit live confirmado):

```bash
SMOKE_RUN_ID=20260814T120000Z \
SOURCE_COMMIT=<sha-completo-live> \
RENDER_WORKSPACE_ID=<workspace-id> \
HOSTED_PROOF_CONFIRM=CREATE_DISPOSABLE_RENDER_RESOURCES \
scripts/workspace/Invoke-PayrollReactiveDeterminationHostedProof.sh
```

## Limites intencionais

Este piloto cobre determinacao autoritativa reativa, cadeia aciclica e selecao tenant-specific dos
providers canonicos de salario liquido e calendario de pagamento como uma unica unidade de
publicacao/ativacao/rollback.
Recomendacao probabilistica, side effect de refresh e validacao remota sao contratos diferentes.
Arrays/wildcards e capabilities com path variables falham fechado na primeira versao. A politica de
data continua implementada pelo provider backend do host; Config seleciona a composicao operacional
por tenant sem transportar a formula ou o calendario. O cache monotono desta prova e in-process; um
host distribuido deve manter o mesmo contrato por instancia e usar a telemetria/readiness de fleet
do control plane para detectar drift. Clientes HTTP
diretos de create/update devem enviar `salarioLiquido` e `dataPagamento` coerentes; a exigencia e
intencional e pode quebrar clientes que antes enviavam derivados arbitrarios. O Metadata permanece
responsavel somente pelo grafo estrutural tenant-neutral.
## Transient Config outage behavior

The aggregate runtime keeps only a bounded in-process last-known-good projection. Eligibility
requires a previously verified canonical head in the same tenant/environment scope, an unchanged
compiled content hash, an effective governed validity interval and freshness within
`PRAXIS_REACTIVE_DETERMINATIONS_LKG_MAX_STALENESS` (default `PT5M`). Recovery always re-reads and
verifies the control-plane head before activating a higher revision. Invalid candidates do not
replace the LKG. Expired or absent LKG fails closed; this cache is not an alternate source of truth
and is lost on restart.
