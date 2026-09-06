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
