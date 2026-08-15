# Policy Studio — prova operacional CREATE/UPDATE V58

- Estado: implementado e validado localmente; Neon e Oracle/HADES permanecem gates de ambiente
- Data: 2026-08-14
- Classe: arquitetural, transversal e de contrato público

## Decisão

O sandbox do Policy Studio continua sem efeitos. A prova de persistência pertence ao host e é
executada pelo Quickstart sobre seu datasource operacional. O Praxis Config é o owner canônico do
Test Run, de sua idempotência e das políticas de evidência por estágio. O Studio apenas inicia a
operação autorizada e projeta o resultado governado.

Não existe banco, schema ou DTO paralelo no Studio. Config e domínio podem residir no mesmo projeto
Neon, mas mantêm schemas, migrations, credenciais e ownership independentes.

## Inventário de aderência fechado pelo V58

| Necessidade | Classificação inicial | Materialização V58 |
| --- | --- | --- |
| Baseline por cenário independente de candidate/active | `lacuna-real-de-contrato` | `DomainRuleTestBaselineResult` no contrato canônico |
| Retry sem duplicar Test Run | `lacuna-real-de-contrato` | `idempotencyKey` escopada e hash canônico do comando |
| Transporte host-neutral para Ergon/Quickstart | `lacuna-real-de-contrato` | records leves em `praxis-config-contracts`, sem importar o starter |
| Evidência CREATE/UPDATE sanitizada | `suportado-parcialmente` | producer host-owned, before/after/effects/cleanup e contador medido |
| Gate de evidência por lifecycle | `ja-suportado-mal-nomeado-ou-mal-materializado` | política server-owned por estágio; nenhuma exigência Oracle global |

## Fronteiras canônicas

```text
Policy Studio
  -> solicita sandbox/Test Run com chave idempotente
Quickstart ou outro host
  -> avalia candidate e active sem efeitos
  -> executa prova operacional descartável quando explicitamente solicitado
Praxis Config
  -> persiste Test Run imutável, baseline independente e evidência sanitizada
  -> calcula blockers/actions segundo a política governada do estágio
Rules Engine
  -> compila e avalia; não persiste Test Run e não acessa banco de negócio
```

O baseline sintético do Quickstart é um oracle de expectativa local, não paridade legada. O adapter
do Ergon deve publicar `LEGACY_ORACLE` somente após executar a autoridade Oracle/HADES real. A lane
`active` nunca é reutilizada ou renomeada como legado.

## Contrato e idempotência

`POST /api/praxis/policy-studio/sandbox/runs` exige `idempotencyKey` com 1 a 180 caracteres. O
Config normaliza o comando, calcula seu hash sem incluir a própria chave e aplica estas regras no
escopo tenant/environment/workspace:

- mesma chave e mesmo hash: devolve o Test Run original;
- mesma chave e payload diferente: `409 Conflict`;
- chave nova: cria um novo Test Run;
- gravação concorrente e submissão do workspace usam o mesmo lock pessimista.

O cliente conserva a chave quando a resposta falha de forma incerta e a troca somente depois de
um sucesso ou de uma mudança no workspace/cenários. Assim, retry de transporte não vira evidência
duplicada.

## Prova operacional host-owned

Para cada cenário, o host executa:

```text
capturar estado limpo esperado
  -> preparar o agregado descartável, quando UPDATE
  -> capturar before digest
  -> executar CREATE ou re-evaluate UPDATE
  -> capturar after digest e effect-ledger digest
  -> verificar mutação ou não mutação esperada
  -> limpar somente a identidade reservada da prova
  -> comparar o estado limpo final
  -> registrar a evidência V58 no Test Run
```

O adapter aceita somente digests SHA-256, modo, flags de verificação e contagem de baseline. Facts,
valores monetários, SQL, linhas, credenciais, referências pessoais e traces não atravessam a
fronteira. Cleanup divergente, mutação divergente ou contador inválido falham fechado e impedem o
registro.

`baselineCallCount` é obtido por um observer no host. O caller não informa mais esse número. No
Quickstart ele é zero porque o baseline local não chama legado; no Ergon o observer deve envolver a
chamada Oracle real e contar inclusive tentativas com falha.

## Matriz executável

| Caso | Operação | Resultado | Invariante operacional |
| --- | --- | --- | --- |
| elegível novo | CREATE | `ALLOW` | recurso criado durante a prova |
| inelegível novo | CREATE | `DENY` | nenhuma mutação |
| elegível alterado | UPDATE | `ALLOW` | mesma identidade, digest alterado |
| alteração inelegível | UPDATE | `DENY` | seed preservado até o cleanup |

O teste do workflow real executa os quatro quadrantes com facts autoritativos, snapshot ativo e
cleanup final. Um teste complementar usa PostgreSQL real descartável para persistir **um** Test Run
com quatro resultados, relê as quatro lanes e repete o mesmo comando sem criar outra linha. Eles são
provas complementares: o workflow integrado usa os datasources H2 do teste do host; o round-trip do
Config usa PostgreSQL nativo. Nenhum deles é apresentado como Neon ou Oracle.

Retries conservam juntos `idempotencyKey` e `evaluatedAtUtc`. O host consulta primeiro o receipt
escopado já persistido e não reexecuta sandbox nem provas operacionais quando o comando coincide;
reuso da chave com outra revisão/fingerprint do workspace, instante, timezone ou conjunto de
cenários falha com `409`. Criar ou alterar um cenário rotaciona a revisão no Config e invalida o run
anterior. O adapter Ergon deve aplicar a mesma guarda antes de qualquer DML ou chamada Oracle.

Os testes HTTP existentes continuam cobrindo conflito idempotente, `If-Match` obsoleto, autorização,
negação sem persistência e lifecycle. A prova compartilhada no Neon permanece necessária antes de
um corte de ambiente corporativo.

## Governança por estágio

O V58 não torna Oracle obrigatório para todos os clientes. A definição pode declarar em
`governance.testEvidencePolicy.stages.<STAGE>`:

- autoridade e elegibilidade de baseline requeridas;
- operações e decisões obrigatórias, cuja combinação forma a matriz cartesiana;
- cleanup obrigatório;
- paridade candidate × baseline obrigatória.

Sem política, não há gate extra universal. Política desconhecida ou malformada falha fechado. Neste
corte, `SUBMIT` vincula o Test Run aceito ao workspace e `PROMOTE` revalida exatamente essa evidência,
impedindo substituição posterior. Gates de `PUBLISH`, `SNAPSHOT` e `ACTIVATE` só devem ser ligados
quando esses estágios também carregarem uma referência imutável à evidência revisada.
Por isso, V58 aceita somente os nomes de estágio `SUBMIT` e `PROMOTE`; declarar antecipadamente ou
errar o nome de outro estágio invalida a política, em vez de criar uma falsa impressão de proteção.

Para RN-013, a política recomendada exige `LEGACY_ORACLE`, baseline `ELIGIBLE`, CREATE/UPDATE,
ALLOW/DENY, cleanup e paridade antes da promoção ou publicação que anteceda autoridade Java. A
governança pode permitir abrir revisão com evidência ainda pendente; isso não autoriza promoção.

## Banco e migrations

- migrations de domínio permanecem em `db/operational-migrations`;
- a V58 do Config persiste idempotência, hash, baseline independente e vínculo do run submetido;
- runtime não recebe DDL nem ownership;
- PostgreSQL local é descartável e não altera Neon;
- no Neon, o mesmo projeto pode hospedar os schemas, mas cada owner usa sua identidade e seu
  histórico de migration;
- cleanup sempre usa identidade reservada da execução; nunca `TRUNCATE` nem filtros amplos.

## O que está comprovado e o que falta

Comprovado localmente:

- contrato host-neutral V58;
- um run/quatro resultados e replay idempotente em PostgreSQL;
- CREATE/UPDATE × ALLOW/DENY pelo workflow real;
- contagem de baseline medida;
- cleanup e mutação fail-closed;
- baseline independente de candidate/active;
- gate server-owned por estágio no Config.

Ainda necessário para handoff corporativo:

1. publicar versões finais coordenadas de `praxis-config-contracts`, Config e Quickstart;
2. executar migration/smoke no Neon configurado, com `403`, `409`, `412` e retry;
3. disponibilizar a operação composta por uma capability/endpoint canônico quando o agente remoto
   precisar acioná-la fora do processo do host;
4. no Ergon, implementar somente o adapter Oracle/HADES, observer de chamadas, sanitização e
   cleanup específicos do host;
5. executar quatro canários autorizados e depois expandir para a matriz RN-013, mantendo
   `LEGACY_AUTHORITATIVE` até homologação.

O Quickstart é laboratório estrutural compatível; não é evidência de paridade Ergon. Oracle, HADES,
DML, readback e homologação da precedência real permanecem responsabilidade do host Ergon.
