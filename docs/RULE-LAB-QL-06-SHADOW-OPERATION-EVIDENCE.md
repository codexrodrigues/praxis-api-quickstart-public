# Rule Lab QL-06 — shadow comparison e operação segura

## Estado

`IMPLEMENTED_VALIDATED` no laboratório Quickstart e `Shadow Implemented` na taxonomia de migração.
Isso não significa `Shadow Running` nem `Shadow Passed` para uma regra Ergon: ainda não existe rota
legada real, amostragem de produção ou autorização para preflight.

## Fronteira

O recurso permanece `human-resources.extraordinary-benefit-requests`. A nova action administrativa
é:

```text
POST /api/human-resources/extraordinary-benefit-requests/actions/shadow-compare
```

O baseline sintético implementa diretamente a especificação congelada do laboratório. Ele não lê
RuleSet, snapshot, resultado candidato, banco ou recurso persistido. O candidato continua sendo o
snapshot Praxis selecionado atomicamente pelo host. Ambos recebem o mesmo DTO validado, conjunto de
permissões resolvido no servidor, instante UTC e timezone.

## Classificação

- `MATCH`: outcome, reason codes ordenados, valor/moeda e EffectIntent planejado equivalem;
- `MISMATCH`: os dois lados terminam com sucesso, mas qualquer dimensão semântica diverge;
- `INCONCLUSIVE`: ao menos um resultado de negócio é inconclusivo, sem falha técnica do runner;
- `TECHNICAL_ERROR`: timeout, exceção sanitizada ou outcome técnico em qualquer lado.

Timeout e falha são isolados por runner. O limite padrão é 1 s e pode variar entre 1 ms e 5 s
por `PRAXIS_RULE_LAB_SHADOW_TIMEOUT_MS`. O pool usa no máximo oito virtual threads e fila de 128
runners pendentes; saturação vira `TECHNICAL_ERROR` sanitizado. O timeout do shadow nunca altera a resposta de uma rota de
negócio autoritativa; nesta onda ele existe apenas na action de laboratório.

## Não mutação e redaction

A action não chama `ResourceActionExecutionService` nem serviços de lifecycle. Os testes verificam
contagem zero após ALLOW e DENY em:

- `extraordinary_benefit_request`;
- `extraordinary_benefit_grant_effect`;
- `praxis_resource_action_execution`;
- `praxis_resource_action_transition`.

A allowlist da observação contém somente identificador aleatório, instante, estados enumerados,
booleans de equivalência e identidade/hash do snapshot candidato. Não contém referência da
solicitação, facts, valores monetários, actor, authorities, mensagens de exceção, secrets ou payload
do snapshot. Logs seguem a mesma allowlist.

## Métricas corporativas

O host registra métricas Micrometer de baixa cardinalidade:

- `praxis.rule.shadow.comparisons`, com tags `result`, `baseline_status` e `candidate_status`;
- `praxis.rule.shadow.side.duration`, com tags `side` e `status`.

Nenhuma tag recebe rule facts, tenant, usuário, referência ou exception message. A retenção e a
exportação durável pertencem ao backend Micrometer/observabilidade configurado no ambiente; o host
não mantém uma segunda base de autoridade em memória.

P2F-ADR-12 formaliza essa separação: a resposta shadow transitória não é ledger
durável, enquanto atos administrativos e transformações materializadas seguem
retenção/legal hold governados conforme
`RULE-LAB-P2F-ADR-12-PRIVACY-RETENTION.md`.

## Gates de promoção, pausa e rollback

Para um futuro piloto corporativo, promoção só pode ser considerada após uma janela contínua de ao
menos 7 dias e 10.000 comparações válidas, cobrindo ALLOW, DENY, NOT_APPLICABLE, INCONCLUSIVE e cada
reason code do boundary. Além disso:

- todo `MISMATCH` precisa estar explicado e resolvido; divergência de outcome ou EffectIntent bloqueia
  promoção imediatamente;
- taxa de `TECHNICAL_ERROR` deve permanecer abaixo de 0,5% e sem burst acima de 1% por 15 minutos;
- `INCONCLUSIVE` deve permanecer abaixo de 2% e possuir causa/fato ausente conhecido;
- alterações de snapshot reiniciam a janela de observação da versão afetada;
- um único mismatch semântico em ALLOW/DENY pausa o candidato e mantém a baseline como autoridade;
- rollback do QL-06 significa desligar o dual-run/exportação; como shadow não decide, não existe
  reversão de dado ou efeito de negócio.

Esses thresholds são critérios mínimos do laboratório, não aprovação automática. Segurança,
precedência de erros, HADES, efeitos, fallback e evidência da rota baseline continuam gates separados.

## Validação focal

- `ExtraordinaryBenefitShadowComparisonServiceTest`: match, mismatch multidimensional,
  inconclusive, timeout, falha sanitizada e métricas;
- `ExtraordinaryBenefitRequestPilotIntegrationTest`: action/discovery/schema, ALLOW/DENY,
  redaction e não mutação dos quatro ledgers;
- testes QL-02/QL-03/QL-05 permanecem responsáveis por determinismo, snapshot e lifecycle.

## Próximo gate

QL-07 concluiu a prova via HTTP operacional e consumo das coordenadas Maven oficiais, sem override
local, conforme `RULE-LAB-QL-07-PUBLIC-DOWNSTREAM-EVIDENCE.md`. QL-08 fará o stress report
Ergon-like. Nenhum desses passos abre Fase 9 ou autoriza preflight/promoção de regra Ergon.
