# Config rc.149: integração de authoring

Classificação: `transversal`. O Config é o owner da política de provider; Quickstart atualiza somente
sua composição de dependências e prova o host. Aderência: semântica já existente mal materializada.
Nenhum endpoint, schema, header, ETag, manifesto ou modelo de provider é alterado pelo host.
A linha de release permanece prerelease `2.0.0-rc`, com preparação/tag/publicação pelo workflow oficial.

Plano: resolver Config `0.1.0-rc.149` publicado; executar `mvn verify` sem override de dependência;
publicar a próxima RC do host; confirmar correspondência de POM/tag fonte, POM/tag público e
`build.version` no Render; executar uma prova Page Builder contra o JAR publicado. Consumidores
Angular/Landing usam os mesmos contratos. Corpus HTTP e docs estruturais não exigem regeneração.

O gate Config [34005676687](https://github.com/codexrodrigues/praxis-config-starter/actions/runs/34005676687)
passou 3/3 testes e first-pass com um único pedido, apply/persistência, execução de ação, duplicação
409, refresh e reload. Esse gate usa source-checkout e80b93fd; complementos de interrupção/cancelamento
foram validados depois por testes focais e CI. O recibo publicado no owner conserva essa distinção.
Não tratar o gate de source-checkout como prova do antigo artefato Maven 148.

O modelo de produto/gate permanece `gpt-5-mini`. Timeouts preliminares e resposta incompleta ainda
aparecem no ledger do gate aprovado; custo total permanece desconhecido. A eficiência dessas fases
é trabalho separado da atualização de dependência. A prova publicada será registrada ao concluir.


## Validação local do artefato publicado

`mvn -B -U verify` passou com a dependência 149 do POM, sem override: 548 testes contabilizados,
526 executados, zero falhas/erros. Dezesseis testes exigem Docker indisponível neste host; seis smokes
externos exigem opt-in PRAXIS_EXTERNAL_SMOKE_TESTS. Não foram habilitados por reflexo.

O SHA-512 do JAR local confere com Maven Central. O JAR aninhado no Quickstart também é byte-idêntico
(SHA-256 71c905af2b7a8d7f265d14f30484c0718275d69c0774fc2e044c6b0c002ecfe1).
A versão do app antes da preparação oficial permanece 2.0.0-rc.46; a próxima release será 2.0.0-rc.47.
A release Config 149 veio da tag/commit 78109b4e e workflow 34006472352, concluído com sucesso.

## Publicação oficial e implantação

O CI [34007211212](https://github.com/codexrodrigues/praxis-api-quickstart/actions/runs/34007211212)
passou: 548 testes contabilizados, zero falhas/erros e oito ignorados (540 executados).
A PR #269 foi integrada em bcba80f8ae9bab9911a1f64197def5c5843e291a.

A preparação oficial [34007755714](https://github.com/codexrodrigues/praxis-api-quickstart/actions/runs/34007755714)
e a publicação [34008247036](https://github.com/codexrodrigues/praxis-api-quickstart/actions/runs/34008247036)
passaram. A tag fonte `v2.0.0-rc.47` aponta para 50d096bf7aa85b34dd8a6f13cb962dbe947bd11d;
a tag pública correspondente aponta para 7ba8d7d2c9de61482ac8da46be443e91021ffcae.
Ambos os POMs declaram Quickstart `2.0.0-rc.47` e Config `0.1.0-rc.149`.

O deploy automático Render `dep-daedibk9v7es73b3m130` implantou o commit fonte da tag.
O painel autenticado registrou `Deploy succeeded | Live`. O aviso de pagamento do workspace
continua visível, mas não bloqueou esse deploy; nenhuma configuração financeira foi alterada.
A prova HTTP e o gate do artefato publicado são evidências distintas, registradas abaixo ao fechar o corte.

Em 2026-09-06T03:14:39.670892+00:00, `/actuator/info` devolveu `2.0.0-rc.47`, com build em
`2026-09-06T03:08:49.001Z`; `/actuator/health` devolveu `UP`. O status Config com Origin
`https://praxisui.dev` confirmou provider OpenAI, `gpt-5-mini`, chave disponível e origem `env`.
Essa verificação não realizou inferência paga nem revelou o valor da chave.

## Gate do artefato Maven Central

O [gate 34008305555](https://github.com/codexrodrigues/praxis-config-starter/actions/runs/34008305555)
passou first-pass com 3/3 testes, um pedido humano e zero retries Playwright. O JAR 149 resolvido
do Maven Central e o JAR aninhado no Quickstart fixado na tag 47 são byte-idênticos ao hash acima.
Apply/persistência, comando, duplicação 409, refresh e reload passaram; limpeza confirmada.
A jornada levou 76.603 ms. Uma repetição interna de pre-intent expirou sem usage; custo total
permanece desconhecido. UI do gate: a9436025 (9.0.64), distinta da Landing publicada com 9.0.65.
A prova detalhada e o recibo ficam no owner Config em `docs/ai/generative-ui-platform/PUBLISHED-PROVIDER-CUT-2026-09-06.*`.

## Escopo real da landing

A jornada livre no Render encontrou `demo/landing` sem Domain Catalog, embora o smoke operacional
de `default/dev` tivesse catálogo. Aderência: `ja-suportado-mal-nomeado-ou-mal-materializado`;
correção operacional transversal, sem novo contrato. Owner Config recebe a projeção canônica de
`/schemas/domain`; o script existente do Quickstart publica no escopo do consumidor Landing.
Não foram criados bindings manuais nem dispensadas verificações semânticas.

Foi executado o fluxo oficial com `BACKEND_URL=https://praxis-api-quickstart.onrender.com`,
`TENANT_ID=demo`, `ENVIRONMENT=landing`, `ORIGIN=https://praxisui.dev`:
`bash scripts/ensure-domain-catalog-context.sh human-resources.funcionarios human-resources.cargos human-resources.departamentos`.
Os três recursos ficaram `PUBLISHED`, com contadores RAG reconciliados 762/762, 105/105 e 90/90.
A correção exige repetir essa reconciliação no escopo do consumidor quando o catálogo fonte mudar;
um smoke de outro tenant/ambiente não substitui essa prova.

O pedido livre anterior terminou `DONE`/`canApply=false`, sem página aplicada. Os três registros
descartáveis foram removidos. O teste teve falhas no coletor EventSource/cancelamento assinado;
a recuperação foi somente leitura por stream ID no config-store, sem alterar estado de turnos.
O modelo geral permaneceu mini, com pre-intent Luna conforme a política existente. A prova livre
após a ingestão não foi executada; ela não pode ser inferida do sucesso do gate canônico de missões.
