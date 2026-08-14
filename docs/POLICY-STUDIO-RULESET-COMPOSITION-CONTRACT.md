# Policy Studio — contrato de composição host-owned de RuleSet

## Objetivo

Este contrato fecha a fronteira entre uma Definition promovida pelo Policy Studio
e o snapshot imutável executado pelo host. Ele não cria uma segunda persistência e
não transforma o browser em compositor.

O vocabulário Java framework-neutral desta borda pertence ao
`praxis-config-contracts` desde `0.1.0-beta.3`: `RuleSetCompositionCandidateRequest`,
`RuleSetCompositionCandidateCommand`, `RuleSetCompositionCandidate`,
`RuleSetCompositionSource`, `RuleSetCompositionPublication` e
`RuleSetCompositionAction`. O Quickstart não mantém DTOs paralelos. Ele apenas
implementa a composição própria do host e expõe esses contratos compartilhados.

- o Quickstart conhece os slots, bindings Java e JSON Logic e compõe o grafo completo;
- o Config continua sendo owner de definitions, fingerprints, manifest, approvals,
  snapshots, head/ETag, ativação e rollback;
- o Policy Studio recebe apenas identidade, digest e proveniência segura;
- condições, executores e o payload do snapshot não retornam ao browser.

As tabelas existentes do Config, no mesmo PostgreSQL/Neon configurado pelo host,
continuam armazenando approvals, snapshots, eventos e head. Este corte não adiciona
migration nem schema de banco.

## Fluxo HTTP

Base: `/api/praxis/policy-studio/rule-sets/{ruleSetKey}`.

### 1. Preparar candidato

`POST /candidate`, com `ROLE_RULE_SNAPSHOT_PUBLISHER` ou
`ROLE_RULE_COMPOSITION_APPROVER`:

```json
{
  "promotedDefinitionId": "00000000-0000-0000-0000-000000000000",
  "validFromUtc": "2026-08-14T00:00:00Z",
  "validUntilUtc": null
}
```

O host seleciona a definição promovida exata e a última versão `approved` ou
`active` de cada outra fonte obrigatória. Cobertura incompleta, fonte fora do
RuleSet, host contract incompatível ou status inadequado falham fechado.

A resposta contém `ruleSetKey`, próxima `ruleSetVersion`, digest canônico,
digest do catálogo de implementações, ETag atual, a lista segura de fontes e
`authorizedActions` calculadas a partir do principal autenticado. O publisher
recebe `PREPARE`/`PUBLISH`; o aprovador recebe `PREPARE`/`APPROVE`.
Ela não contém o `RuleSetDefinition`.

### 2. Aprovar composição

`POST /candidate/approvals`, com `ROLE_RULE_COMPOSITION_APPROVER`, repete os
campos de composição e inclui `expectedCompositionDigest`. O host recompõe o
candidato; se qualquer fonte, versão ou validade tiver mudado, o digest não
confere e o comando retorna conflito. Cada um dos dois aprovadores chama o endpoint
com sua própria sessão. O Config persiste a aprovação por principal autenticado.

### 3. Publicar snapshot

`POST /candidate/publish`, com `ROLE_RULE_SNAPSHOT_PUBLISHER`, usa o mesmo comando
digest-bound e os headers canônicos de concorrência:

- primeiro head: `If-None-Match: *`;
- supersessão: `If-Match: "<headEtag>"`.

O host recompõe e o Config revalida fontes, fingerprints, duas aprovações,
catálogo de implementações, digest e ETag antes de persistir e ativar o snapshot.
A resposta do adapter contém apenas identidade, hashes, revisão e tipo de ativação;
o payload do snapshot devolvido internamente pelo Config é removido antes da borda HTTP.

## Invariantes corporativas

- autor, aprovadores da composição e publisher continuam segregados pelo Config;
- duas aprovações distintas permanecem obrigatórias;
- a Definition promovida deve ter `lifecycleBoundary=REFERENCE_DRAFT_ONLY` e
  pertencer a um binding governado conhecido pelo host;
- nenhuma regra é selecionada por texto, label, regex ou heurística do browser;
- o endpoint não executa facts nem efeitos;
- conflito de digest exige preparar e revisar um novo candidato;
- conflito de ETag exige recarregar o head; não existe last-write-wins.
- o Studio não infere `APPROVE` ou `PUBLISH` pelo estado; projeta somente
  `authorizedActions` devolvidas pelo host e cada comando é revalidado.

## Escopo do primeiro adapter

O adapter atual reconhece somente `extraordinary-grant-eligibility`, porque o
compositor pertence ao host concreto. O Ergon deve implementar outro compositor
host-owned para seus RuleSets, consumindo os contratos compartilhados de
`praxis-config-contracts`, sem importar classes ou semântica do Quickstart.

## Ordem de adoção

1. publicar `praxis-config-contracts:0.1.0-beta.3`;
2. atualizar e validar o host consumidor;
3. só então liberar o host e o Policy Studio integrados.

Durante desenvolvimento local, `mvn install` pode provar a integração antes da
publicação. Esse artefato local não é evidência de release e não deve ser usado para
inverter a ordem acima.

## Prova mínima

- candidato usa a Definition promovida exata e as demais fontes aprovadas mais recentes;
- fonte obrigatória ausente bloqueia composição;
- digest alterado bloqueia approval/publication antes de qualquer mutação;
- segurança separa publisher de composition approver;
- os testes do compositor continuam garantindo cobertura exata e compatibilidade.
