# Policy Studio V57 — handoff operacional para Studio e Ergon

> Documento histórico. As lacunas de transporte, baseline e idempotência descritas aqui foram
> evoluídas pelos cortes V58/V59. Para o estado executável atual, consulte
> [`POLICY-STUDIO-OPERATIONAL-EVIDENCE-PLAN.md`](POLICY-STUDIO-OPERATIONAL-EVIDENCE-PLAN.md).

## Estado do corte

O Quickstart possui agora a cadeia interna necessária para produzir evidência operacional V57 sem
transferir autoridade ao browser:

1. o sandbox prepara candidate versus active uma única vez e sem persistência;
2. bindings host-owned associam explicitamente cada `scenarioId` a `CREATE` ou `UPDATE`;
3. o workflow autoritativo executa o comando com facts readquiridos;
4. o probe produz somente digests de before/after/ledger e o host comprova cleanup após rollback;
5. o recorder delega a gravação ao `DomainRuleTestRunService` do Config;
6. o Policy Studio consome a projeção persistida, sem SQL, credenciais ou payload operacional.

O corte está integrado na `main` pelos PRs `#180`, `#181` e `#182`.

## Evidência executada

- `CREATE` e `UPDATE` reais sobre o piloto de benefício extraordinário;
- identidade preservada no `UPDATE`, reaquisição de facts e mutação somente para `ALLOW`;
- ETag e idempotência na action HTTP pública de reavaliação;
- falha fechada quando mutação observada diverge da expectativa do cenário;
- referência descartável restrita a `policy-studio-proof-*`;
- probe e rollback-only em PostgreSQL real-processo descartável;
- round-trip `record/list` do V57 em PostgreSQL real com repositórios JPA do Config;
- uma única avaliação candidate/active e uma persistência de Test Run no teste focal atual.

## O que ainda não foi comprovado

- execução em branch Neon efêmera;
- adapter Ergon executando as matrizes Oracle/HADES reais;
- baseline legado independente ligado ao mesmo Test Run;
- efeitos externos, DML Oracle, readback e cleanup Ergon;
- endpoint/capability para disparar a lane operacional remotamente.
- client/transport público V57 consumível pelo adapter Ergon sem importar o starter inteiro;
- idempotência do registro V57 por retry;
- baseline legado independente por cenário — o contrato atual guarda provenance no nível do run e
  resultados `candidate` versus `active`;
- gate de evidência por estágio; a V57 atual valida/persiste o shape, mas não bloqueia sozinha
  promoção, publicação, rollout ou ativação quando baseline e cleanup estiverem inelegíveis;
- um único Test Run operacional com os quatro resultados canônicos: `CREATE ALLOW`, `CREATE DENY`,
  `UPDATE ALLOW` e `UPDATE DENY`.

Por isso este corte é apto para integração inicial de adapter, mas não constitui homologação de
paridade Ergon nem autorização para promoção produtiva.

## Gate de evidência por estágio

A ausência de evidência operacional não deve bloquear `SUBMIT` universalmente. A decisão pertence
à governance server-owned da Definition/RuleSet:

- `SUBMIT` pode abrir revisão técnica com baseline `PENDING` quando a política da regra permitir,
  preservando o blocker explícito;
- `PROMOTE` deve bloquear, para RN-013, quando o baseline `LEGACY_ORACLE` requerido não estiver
  `ELIGIBLE`, quando a matriz requerida estiver incompleta, quando houver mutação indevida ou quando
  cleanup não estiver comprovado;
- `PUBLISH`, composição de snapshot, rollout e `ACTIVATE` devem bloquear enquanto qualquer evidência
  obrigatória estiver inelegível;
- uma regra pode exigir evidência antes de revisão, mas esse requisito deve vir da governance
  canônica, não de uma inferência do Quickstart ou do Policy Studio.

Até esse contrato existir, um Test Run V57 persistido é evidência consultiva e não prova, sozinho,
que o gate de promoção/publicação foi satisfeito.

## Contrato para o adapter Ergon

O agente Ergon deve implementar bindings explícitos por cenário e fornecer:

- `scenarioId` governado;
- modo `CREATE` ou `UPDATE`;
- comando host-owned e referência de fixture exclusiva;
- expectativa de mutação;
- contagem real de chamadas ao baseline legado;
- digests sanitizados de before/after/effect ledger;
- confirmação de no-mutation e cleanup;
- provenance do baseline independente.

Nesta fase, o agente pode implementar interfaces, sanitização, observers e testes com fakes. O
adapter HTTP final deve aguardar o client/transport público V57, o contrato de baseline por cenário
e a chave de idempotência no owner Config. É incorreto importar o `praxis-config-starter` pesado ou
duplicar os DTOs com JSON ad hoc no host Ergon.

Não deve reutilizar os DTOs do benefício extraordinário, inferir operação pelo nome do cenário,
enviar SQL ao Studio ou tratar o corpus sintético Quickstart como oracle Ergon.

## Gate Neon seguro

A prova gerenciada deve usar uma branch Neon efêmera ou um schema exclusivo criado por tooling
autenticado. São obrigatórios:

1. nome/identidade exclusivos da execução;
2. TLS e conexão direta PostgreSQL;
3. migrations versionadas do Quickstart/Config;
4. fixtures exclusivamente sintéticas;
5. `record/list` V57 e verificação de digests;
6. rollback transacional que preserve ledgers append-only e comprove ausência de estado ativo;
7. remoção da branch/schema ao final;
8. evidência sanitizada sem URL, usuário, senha ou payload de banco.

É proibido apontar testes `create-drop`, `truncate` ou deletes amplos para o banco Neon
compartilhado. Nesta máquina, em 2026-08-14, não havia `neonctl`, `psql`, variável JDBC nem conexão
efêmera autenticada; portanto nenhum Neon foi tocado neste corte.

## Próxima decisão de release

O tag Quickstart `v2.0.0-rc.27` antecede a lane V57 descrita aqui, embora o POM ainda declare essa
versão. O consumidor deve fixar o commit `main` comprovado ou aguardar um novo corte; não deve usar
somente o tag `rc.27` como evidência de que o laboratório está disponível.

Após fechar os contratos pendentes e tornar a lane Neon verde:

1. publicar a evidência de execução sanitizada;
2. validar a leitura no Policy Studio com persona autorizada e não autorizada;
3. entregar este contrato ao agente Ergon;
4. executar um único Test Run idempotente com `CREATE ALLOW`, `CREATE DENY/no-mutation`,
   `UPDATE ALLOW` e `UPDATE DENY/no-mutation`;
5. somente depois ampliar para a matriz RN-013 e baseline Oracle real.
