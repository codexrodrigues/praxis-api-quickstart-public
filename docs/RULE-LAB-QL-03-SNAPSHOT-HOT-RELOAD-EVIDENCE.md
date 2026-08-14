# Rule Lab QL-03 — snapshot, cache, rollback e hot reload

## Resultado

O Quickstart deixou de compilar `ExtraordinaryGrantRuleSetFactory` como plano
ativo no bootstrap. O único caminho runtime agora é:

1. `praxis-config-starter` seleciona o head governado por tenant, environment e
   `ruleSetKey`;
2. `PublishedRuleSnapshotHeadReader`, do artefato framework-neutral
   `praxis-config-contracts`, entrega snapshot imutável, hash de conteúdo, ETag
   opaco do head e `activationRevision` sem expor DTOs do Config Starter;
3. `ExtraordinaryGrantRuleSnapshotRuntime` valida owner, scope, identidade,
   validade e contrato de host;
4. o engine recompila o candidato com o registry Java executável do host;
5. somente depois de hash e compilação válidos a referência ativa muda de forma
   atômica;
6. avaliações em andamento continuam usando uma única referência imutável.

O factory local permanece apenas como definição de referência para fixtures e
testes do laboratório. Ele não é fallback de produção nem segunda fonte de
verdade do plano ativo.

## Invariantes corporativas provadas

- O host aceita apenas `ownerServiceKey=praxis-api-quickstart`,
  `ruleSetKey=extraordinary-grant-eligibility`, tenant/environment configurados
  e `requiredHostContractVersion=quickstart/1.0`.
- Implementações Java são resolvidas por chave, versão e atestação exatas no
  registry executável do host. Extensões Java de cliente exigem evidência
  externa e não podem autorizar a si mesmas pelo payload publicado.
- O hash retornado pelo control plane deve coincidir com o hash recompilado pelo
  engine.
- `activationRevision` impede que respostas reordenadas substituam um head mais
  novo.
- ETag igual é cache hit; não há recompilação nem troca da referência ativa.
- Falha de banco, snapshot incompatível, hash divergente ou candidato fora da
  validade preserva o last-known-good e publica diagnóstico seguro.
- Rollback não recompõe conteúdo. O mesmo snapshot imutável volta a ser ativo
  com `activationRevision` maior e novo ETag opaco, evitando ABA.
- Não existe execução de efeito, persistência de concessão ou autoridade Ergon
  nesta etapa. O binding de efeito continua produzindo apenas intent
  `PLANNED_NOT_EXECUTED`.

## Operação

O scope do laboratório é configuração explícita do host:

```properties
praxis.rule-lab.snapshot.enabled=${PRAXIS_RULE_LAB_SNAPSHOT_ENABLED:false}
praxis.rule-lab.snapshot.readiness-required=${PRAXIS_RULE_LAB_READINESS_REQUIRED:false}
praxis.rule-lab.snapshot.tenant-id=${PRAXIS_RULE_LAB_TENANT_ID:desenv}
praxis.rule-lab.snapshot.environment=${PRAXIS_RULE_LAB_ENVIRONMENT:local}
praxis.rule-lab.snapshot.initial-delay-ms=${PRAXIS_RULE_LAB_INITIAL_DELAY_MS:5000}
praxis.rule-lab.snapshot.refresh-delay-ms=${PRAXIS_RULE_LAB_REFRESH_DELAY_MS:30000}
```

O perfil `dev` habilita explicitamente o laboratório. O perfil base/prod o
mantém desligado até o ambiente provisionar o head governado e optar pela
capacidade. Esse switch é de ativação operacional do piloto, não mantém plano
legado nem contrato paralelo; quando desligado, nenhum fallback local é criado.

O primeiro load ocorre após o contexto estar pronto e a mesma fronteira pode ser
acionada por `refreshNow()` em testes/operação. Ausência inicial de head não
derruba o host, mas avaliações falham explicitamente até existir um snapshot
válido. O status seguro informa readiness, snapshot/hash/ETag ativos, revisão,
última tentativa, última ativação e último erro, sem payload de fatos, regra ou
credencial.

Quando o laboratório está habilitado, um `HealthIndicator` contribui estado
`UP` somente com snapshot efetivo. Ausência/expiração resulta em
`OUT_OF_SERVICE` com detalhes seguros de identidade, hash, ETag, revisão e
código de falha; fatos, expressões e mensagens internas não são expostos.

## Coordenadas públicas

- `io.github.codexrodrigues:praxis-rules-engine:0.1.0-beta.15`
- `io.github.codexrodrigues:praxis-config-contracts:0.1.0-beta.1`
- `io.github.codexrodrigues:praxis-config-starter:0.1.0-rc.99`

O Config Starter e o Quickstart exigem Java 21. Tanto o contrato standalone
quanto o adapter Spring são resolvidos das coordenadas públicas, sem override
Maven local.

## Validação focal

```powershell
mvn -Dtest=ExtraordinaryGrantRuleLabServiceTest,ExtraordinaryGrantRuleSnapshotRuntimeTest test
```

Os testes cobrem os cinco caminhos de negócio anteriores e acrescentam:

- ativação e cache hit pelo ETag;
- rejeição de contrato de host incompatível com retenção do last-known-good;
- rollback `v1 -> v2 -> v1` com novo head;
- indisponibilidade do control plane sem perda do plano ativo.
## Publicação com aprovação da composição

Publicação é um fluxo de duas etapas: o Config Starter canonicaliza a composição
e o catálogo host-governed; duas decisões `RULE_COMPOSITION_APPROVER` aprovam o
digest retornado; somente então o candidato inalterado pode avançar o head. O
Quickstart registra explicitamente as duas coordenadas Java admitidas e um teste
impede drift entre esse catálogo de planejamento e o registry executável.

No caso Policy Studio, o host compõe o grafo completo substituindo exatamente os
sete bindings JSON Logic autônomos pelas condições das sete definições aprovadas.
O compositor preserva slots, dependências, precedência, fail policy e bindings
Java do factory; rejeita fonte ausente, duplicada, não aprovada ou fora do
RuleSet/contrato do host. O manifesto do Config passa então a ligar o grafo
materializado aos hashes das definições que realmente forneceram as condições.
