# Rule Lab QL-04 — action HTTP de avaliação de benefício

## Resultado

O Quickstart publica o recurso exclusivamente orientado a comandos
`human-resources.extraordinary-benefit-requests`. Nesta etapa não existe coleção
consultável nem solicitação persistida: a operação real é somente:

```text
POST /api/human-resources/extraordinary-benefit-requests/actions/evaluate
```

O controller herda de `AbstractCollectionCommandResourceController`, cuja fonte
canônica é o `praxis-metadata-starter:8.0.0-rc.106`. Assim o host obtém
`/actions`, `/capabilities`, execução governada, envelope e links de schema sem
simular CRUD ou duplicar discovery localmente.

## Caso de negócio exercitado

A action avalia uma ajuda extraordinária a colaborador combinando:

- autorização resolvida pelo servidor;
- situação funcional e conflito por concessão duplicada;
- vigência e teto do programa;
- restrição adicional do cliente, inclusive ausência que produz `INCONCLUSIVE`;
- calendário de pagamento;
- cálculo puro do valor recomendado;
- disponibilidade orçamentária pós-decisão;
- planejamento puro de efeito, sempre `PLANNED_NOT_EXECUTED` no QL-04.

O host congela um único `Instant`, fuso IANA, fatos e referência do snapshot. A
resposta devolve `snapshotKey`, hash do snapshot, `activationRevision`, versão do
RuleSet, `factsDigest` e `planDigest`. Essa evidência vem da mesma referência
atômica usada na avaliação; um hot reload concorrente não mistura resultado de
um plano com identidade de outro.

## Semântica HTTP

- `200`: o engine concluiu a avaliação. `ALLOW`, `DENY`, `NOT_APPLICABLE`,
  `INCONCLUSIVE` e `TECHNICAL_ERROR` são resultados de negócio, não códigos de
  transporte.
- `400`: Bean Validation ou fuso IANA inválido.
- `401/403`: sessão ausente ou action indisponível pela política do host.
- `412`: não existe snapshot governado ativo e vigente.

`persisted=false` e `effectExecuted=false` são invariantes públicos desta etapa.
Uma decisão `ALLOW` apenas informa que a solicitação pode avançar para o fluxo
persistente e aprovativo planejado em QL-05.

## Discovery e schema

```text
GET /schemas/actions?resource=human-resources.extraordinary-benefit-requests
GET /api/human-resources/extraordinary-benefit-requests/actions
GET /api/human-resources/extraordinary-benefit-requests/capabilities
GET /schemas/filtered?path=/api/human-resources/extraordinary-benefit-requests/actions/evaluate&operation=post&schemaType=request
GET /schemas/filtered?path=/api/human-resources/extraordinary-benefit-requests/actions/evaluate&operation=post&schemaType=response
```

O request documenta propriedade por propriedade a semântica de domínio e publica
`x-ui` para valor monetário, datas, toggles, lista de calendário, motivo e fuso.
O campo `customerAdditionalEligible` é intencionalmente opcional: omiti-lo prova
que o runtime não inventa `false` nem `true` e termina de forma inconclusiva.

## Exemplo operacional

```bash
curl -X POST \
  http://localhost:8080/api/human-resources/extraordinary-benefit-requests/actions/evaluate \
  -H 'Content-Type: application/json' \
  -H 'Cookie: SESSION=<jwt-do-host>' \
  -d '{
    "requestReference":"BEN-2026-000184",
    "reasonCode":"FAMILY_HARDSHIP",
    "eventDate":"2026-07-13",
    "requestedAmount":2500.00,
    "workerStatus":"ACTIVE",
    "duplicateGrant":false,
    "programActive":true,
    "programMaximumAmount":5000.00,
    "customerAdditionalEligible":true,
    "requestedPaymentDate":"2026-07-20",
    "allowedPaymentDates":["2026-07-20","2026-08-05"],
    "availableBudgetAmount":100000.00,
    "userTimeZone":"America/Sao_Paulo"
  }'
```

## Validação

```powershell
mvn -Dtest=ExtraordinaryGrantRuleLabServiceTest,ExtraordinaryGrantRuleSnapshotRuntimeTest test
mvn -Dtest=ExtraordinaryBenefitRequestPilotIntegrationTest test
```

Os testes cobrem `ALLOW`, negação por orçamento, autorização resolvida pelo host,
fato ausente inconclusivo, hashes/evidência atômica, ausência de persistência e
efeitos, autenticação HTTP, action catalog, capabilities e schema filtrado com
`x-ui`.

## Próximo gate

QL-05 deve introduzir identidade persistida, estados e comandos idempotentes de
submissão/aprovação. Somente nessa etapa o recurso deve migrar para uma base com
leitura por item. O effect intent continuará separado da transação e exigirá
outbox, idempotência e auditoria antes de qualquer integração externa.
