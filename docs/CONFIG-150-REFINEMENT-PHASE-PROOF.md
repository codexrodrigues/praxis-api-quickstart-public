# Config 150 — fase de refinamento

O host passa a consumir `praxis-config-starter:0.1.0-rc.150`. A correção canônica distingue `live_option_refinement` de `intent_fast` na telemetria. Não altera modelos, prompts, endpoints ou semântica dos recursos.

Config PR #465; tag `v0.1.0-rc.150`, commit `c50230f0f85840b290ee742c196d98dcd321588b`. Gate determinístico `34011912895` passou sem provider; publicação `34012131367` e CI `34012131731` passaram.

Checksums SHA-512 públicos do JAR e POM conferidos. O JAR Config aninhado no Quickstart é byte a byte igual ao Maven Central: SHA-256 `29926ea7efd2eb20e6d7c512238116ed5032901fee06875a20dc2e75438dd1b6`.

`mvn -B -U verify` local passou: 548 testes previstos, 526 executados, 22 ignorados, zero falhas/erros. Os ignorados dependem de Docker ou opt-in externo; não contam como prova live. O canário Landing tem 20 testes Node e TypeScript aprovados e verifica separadamente o modelo de refinamento.

A linha de release permanece RC compatível (`2.0.0-rc.48`). A prova no Render deve confirmar a versão implantada e executar uma jornada delimitada, sem repetir a LLM em diagnósticos isolados. Custo desconhecido permanece desconhecido no modo autorizado pelo limite de conta; teto numérico do runner exige estimativa completa antes de outra admissão.

Sem alterações de corpus, manifests ou exemplos visuais: este corte atualiza a dependência e sua evidência operacional.
