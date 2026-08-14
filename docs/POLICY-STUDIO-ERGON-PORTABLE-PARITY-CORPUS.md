# Golden corpus sintético de riscos de migração

> A prova operacional CREATE/UPDATE, incluindo a semântica de reavaliação e o
> mapeamento para Test Run V57, está definida em
> [`POLICY-STUDIO-OPERATIONAL-EVIDENCE-PLAN.md`](POLICY-STUDIO-OPERATIONAL-EVIDENCE-PLAN.md).
> Os rótulos CREATE/UPDATE deste corpus continuam sendo contexto sintético até
> que o adapter host-owned execute aquela matriz sobre PostgreSQL real.

O Quickstart publica um golden corpus executável e neutro no arquivo histórico
`src/test/resources/policy-studio/ergon-portable-parity-corpus.v1.json`. Ele
permite desenvolver o Policy Studio sem Oracle, mas preserva os riscos que a
migração Ergon precisa provar antes de promover uma regra. O nome físico contém
`parity` por compatibilidade do corte beta.3; o artefato não é uma prova nem um
adapter de paridade.

## Fronteira

- autoridade local: `SYNTHETIC_BASELINE` do Quickstart;
- autoridade no Ergon: rota legada aprovada nos artefatos da Parte 1;
- candidato: RuleSet compilado e avaliado pelo Praxis Rules Engine;
- facts e efeitos: responsabilidade do host;
- drafts, cenários, Test Runs e lifecycle: responsabilidade do Config;
- Studio: experience plane, sem segunda cópia executável.

O corpus não compara candidato com Oracle, snapshot ativo ou evidence legado. O
teste executa um único snapshot sintético e compara seu resultado com a expectativa
do próprio JSON. Um adapter Ergon futuro deve usar casos ligados à Parte 1 e
registrar `MATCH`, `MISMATCH`, `INCONCLUSIVE` ou `TECHNICAL_ERROR` para cada lane.

## Cobertura do primeiro corte

Os 14 casos exercitam create e update como contextos de comando, caminho feliz,
limites inclusivos, valor imediatamente acima/abaixo do limite, `null` explícito
avaliado separadamente de fact ausente, `NOT_APPLICABLE`, regras simultaneamente violadas e a precedência
determinística entre autorização, elegibilidade, duplicidade, aplicabilidade,
restrição de cliente, calendário, parâmetro e orçamento.

Esses rótulos de comando não executam uma mutação CREATE/UPDATE. Eles provam a
avaliação pura do mesmo conjunto de riscos nos dois contextos; a prova de escrita,
readback, identidade pública, rollback e cleanup continua pertencendo ao host.

`mutationExpected=false` é metadado do fixture. O teste comprova apenas que o
objeto de facts fornecido ao engine não foi modificado; ele não inspeciona banco,
outbox, effect intent, readback ou cleanup. Efeito planejado não significa efeito
executado. A ausência de mutação operacional continua sendo gate do host.

## Gate para o agente Ergon

Para cada operação/tela, o agente deve anexar o caso ao handoff aprovado da
Parte 1 e registrar:

1. identidade da tela e operação;
2. origem e ordem das regras legadas;
3. payload/facts efetivamente observados;
4. resultado e erro estruturado da autoridade legada;
5. resultado, reasons e trace redigido do candidato;
6. prova de ausência de mutação em shadow;
7. classificação de divergência e evidência ausente;
8. fallback e regra explícita de retorno à Parte 1.

Sem baseline Parte 1 aprovado, o caso deve permanecer `INCONCLUSIVE`; ele não pode
autorizar preflight, promoção ou contenção do legado. O Test Run canônico aceita
proveniência e eligibility do baseline desde a migração Config `V57`, mas este
corpus continua sintético e registra `SYNTHETIC_EXPECTED`, nunca `LEGACY_ORACLE`.
Somente o adapter da fábrica, apoiado na Parte 1, pode registrar a autoridade Ergon.

## Encaixe com a RN-013 real

O baseline de referência é `ERGadm00036 — Regras de frequência`, CREATE e UPDATE
da rota `/api/administracao-pessoal/regras-frequencia`, classificado pela fábrica
como `WRITE_DB_BACKED_REQUIRED`, `LEGACY_AUTHORITATIVE` e `Ready with adjustments`.
DELETE, a regra ERG-08393 e a RN-017 ficam fora deste corte.

A fábrica já executa uma matriz DB-backed de 38 casos: 14 negativas em CREATE e
UPDATE, limites inclusivos, semântica de `null` e três colisões de precedência em
ambas as operações. Esse corpus é evidência específica do cliente e permanece na
fábrica. O Quickstart não replica códigos ERG nem payloads Oracle no seu domínio
neutro.

As 38 linhas não têm necessariamente a mesma profundidade de correlação com o
baseline; a fábrica deve preservar por caso quais provas existem e quais estão
pendentes.

O Config `V57` já possui a base governada para receber uma referência/digest do
baseline e evidência operacional sanitizada de `CREATE`/`UPDATE`: before/after,
mutação ou não mutação, cleanup, ledger de efeito e contagem de chamadas ao
baseline. O sandbox cobre candidato × ativo e candidato × resultado esperado e
permanece sem efeitos. A lacuna agora é de integração host-owned: a fábrica Ergon
deve adaptar suas evidências DB-backed a esse contrato sem enviar linhas Oracle,
SQL, credenciais ou payloads sensíveis ao Config.

## Validação focal

```bash
./mvnw -B -Dtest=PolicyStudioErgonPortableParityCorpusTest,PolicyStudioSandboxServiceTest test
```
