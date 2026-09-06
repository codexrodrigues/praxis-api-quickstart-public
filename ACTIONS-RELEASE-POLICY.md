# Actions no fechamento de versões


O padrão durante desenvolvimento é zero execuções remotas. Valide localmente o escopo alterado; commits, PRs, documentação interna e conclusão de tarefas não são motivos para iniciar Actions. Use os workflows manuais somente no fechamento autorizado de uma versão/publicação ou na prova necessária do host já implantado. Não use `[skip ci]` como mecanismo principal nem desabilite checks/proteções para economizar.

Antes de push, tag ou dispatch, confira os gatilhos reais de `.github/workflows/`. Tags de release publicam artefatos: não criá-las para testar a automação. Diagnostique localmente antes de repetir um job; conserve a evidência da revisão e dos artefatos usados. Monitores operacionais explicitamente mantidos são independentes do CI de commits. Consulte [ACTIONS-RELEASE-POLICY.md](ACTIONS-RELEASE-POLICY.md) para os pontos de entrada e recuperação.

## Origem privada e espelho público

No espelho público, apenas o CI independente da tag é executado; preparação/publicação são operadas no repositório privado `praxis-api-quickstart`. Os procedimentos abaixo de preparação e sincronização pertencem ao privado. O teste privado de orquestração é excluído pelo sanitizador; `PublicSnapshotWorkflowContractTest` valida o workflow independente nos dois lados.

## Fluxo deste repositório

Execute `publish-public-release.yml` via dispatch em `main`, com `create_tag=true`. A preparação grava a versão no POM e envia commit/tag atomicamente. A tag `v*`, inclusive criada diretamente, precisa passar pelo `ci-java.yml` reutilizável antes da publicação do snapshot: Maven verify, scripts, sanitizador e dependências empacotadas. Dependência ainda ausente no Maven Central causa falha rápida; confirme sua disponibilidade localmente antes de iniciar o corte.

O sanitizador exclui todos os workflows privados e projeta explicitamente `.github/public-workflows/ci-java.yml`. Esse CI público independente verifica a tag pública, sem chamar o sanitizador excluído ou repetir cron/smoke do privado. Atualize o template no privado, nunca diretamente no espelho. O sanitizador também rejeita testes públicos que referenciem workflows privados excluídos; as verificações exclusivas da orquestração privada ficam em `PublicReleaseWorkflowContractTest`.

A projeção inclui as migrações operacionais versionadas exigidas pelo POM e as três fixtures fictícias revisadas de `db/demo-seeds/public-demo/` (absence-analytics, employee-address-coherence e employee-dependent-coherence). Dumps arbitrários e outros diretórios de seeds continuam excluídos. Valide o snapshot gerado com Maven antes do corte; validar apenas a árvore privada não prova a integridade do pacote público.

Publicar o snapshot não confirma deployment no Render. Como não há sinal canônico de conclusão do deploy conectado a estes workflows, a publicação registra a prova de runtime como pendente e não inicia espera automática. Após o Render concluir o deploy, dispare `domain-catalog-runtime-smoke.yml` na tag de origem, com `expected_version` igual à versão publicada e `rollout_timeout=0`. O gate exige health UP e `build.version` exata antes de ingerir o catálogo. O modo reutilizável fica disponível para um orquestrador que já possua confirmação do deploy. Espera explícita, se necessária para um rollout já iniciado, é limitada a 600 segundos mais os timeouts curtos de HTTP.

Esse smoke único substitui `sync-published-domain-catalog.yml` e o cron diário: preserva ingestão, contexto, resiliência, authoring e smokes funcionais, cobrindo os cinco resource keys antes divididos entre os fluxos. Falha no host se recupera pelo smoke da tag, sem republicar snapshot nem criar nova versão.
