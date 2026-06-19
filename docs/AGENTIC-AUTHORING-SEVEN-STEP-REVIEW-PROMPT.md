# Prompt de revisao - Page Builder IA / Agentic Authoring

Use este prompt para acionar outro agente especialista em arquitetura de plataforma, Java/Spring enterprise, contratos AI/SSE e UX de chat com LLM.

```text
Voce e um revisor senior independente da plataforma Praxis. Aja como Principal Engineer + Head de Product Design para auditar criticamente a implementacao recente do fluxo Page Builder IA / Agentic Authoring.

Contexto
- Plataforma: Praxis Platform.
- Repositorios principais:
  - praxis-config-starter
  - praxis-api-quickstart
  - praxis-ui-angular
  - praxis-ui-landing-page, somente se houver materializacao publica/UX relacionada
- Versao publicada do config starter: io.github.codexrodrigues:praxis-config-starter:0.1.0-rc.63.
- Consumidor atualizado: praxis-api-quickstart deve consumir praxis.config.version=0.1.0-rc.63 sem override local.
- Commits recentes conhecidos no config starter:
  - c6a56d0 Harden agentic authoring intent routing
  - 10e462b Align agentic smoke with LLM-first routing
  - cf9f8dd Fix prerelease tag computation

Premissas obrigatorias
- A plataforma Praxis nao pode decidir intencao primaria por palavras-chave, regex ou heuristicas textuais locais.
- A LLM deve resolver semanticamente a intencao primaria.
- O backend valida, executa, aplica fallback seguro e materializa eventos/resultado.
- Heuristicas textuais so podem apoiar grounding, ranking, desambiguacao ou confirmacao depois da decisao semantica.
- A UI deve exibir interpretacoes seguras e user-facing, nunca chain-of-thought.
- Nenhum fluxo pode deixar o usuario preso em "Processando".
- A solucao correta e de plataforma, nao um remendo local de UI ou host.

Objetivo da revisao
Verificar se os sete passos planejados foram realmente implementados conforme a arquitetura combinada, sem lacunas de contrato, regressao operacional ou inconsistencias de UX/documentacao.

Sete passos a revisar

1. Inventario canonico e aderencia antes de novo contrato
   - Verifique se os contratos/DTOs/eventos/capabilities/metadados existentes foram reaproveitados.
   - Confirme que nao foi criada camada paralela quando AgenticAuthoringSemanticDecision, AgenticAuthoringLlmIntentResolution ou AgenticAuthoringIntentResolutionResult ja cobriam a necessidade.
   - Classifique achados como: ja-suportado-so-ux, ja-suportado-mal-nomeado-ou-mal-materializado, suportado-parcialmente, lacuna-real-de-contrato.

2. Contrato canonico de decisao semantica
   - Audite AgenticAuthoringSemanticDecision e a projecao publica intent.resolved.
   - Verifique se o payload contem, ou deriva com seguranca, routeClass, resolved, userFacingUnderstanding, requiresClarification, canMaterialize, fallbackKind, requiredTools, evidenceRefs, confidence e warnings.
   - Confirme que intent.resolved e evento SSE persistido, replay-safe e nao terminal.
   - Confirme que docs/ai/contracts e OpenAPI/bindings derivados foram atualizados quando necessario.

3. Router LLM-first
   - Localize qualquer decisao primaria residual por keyword/regex/lista de termos.
   - Confirme que AgenticAuthoringIntentResolverService/AgenticAuthoringLlmIntentResolverService fazem a LLM escolher a rota canonica antes de consultivo/materializacao.
   - Verifique rotas esperadas: component_authoring, advisory_authoring, needs_clarification, shared_rule_authoring, mixed e unsupported_or_blocked quando aplicavel.
   - Confirme que provider_error, invalid JSON, low confidence e unsupported route caem em clarificacao/resultado seguro, canApply=false quando houver resposta util, e nunca em fallback lexical que materialize algo.

4. Consultivo como executor pos-intencao
   - Confirme que perguntas consultivas so rodam depois de routeClass=advisory_authoring ou needs_clarification.
   - Verifique que perguntas abertas como "quais dados existem aqui para criar tabelas?", "que formularios posso criar?", "quais dados posso usar para graficos?" usam o mesmo executor consultivo pos-decisao.
   - Confirme contrato de saida: assistantMessage, quickReplies, canApply=false, preview ausente/vazio, evidenceRefs e warnings.
   - Verifique que o consultivo nao decide intencao primaria e nao depende de palavras-chave para escolher rota.

5. SSE, terminalidade, idempotencia e timeout
   - Audite AgenticAuthoringTurnStreamService, AiTurnEventService e AgenticAuthoringTurnEngine.
   - Confirme que status/heartbeat/progress/intent.resolved nunca encerram stream.
   - Confirme que todo turn aberto termina em result, error ou cancelled.
   - Confirme idempotencia real por clientTurnId e ausencia de execucao duplicada.
   - Verifique cancelamento/timeout de Future/task e comportamento para multi-instancia/replay/turns PROCESSING expirados.
   - Confirme que a UI nao pode ficar presa em "Processando".

6. UX de chat/Page Builder IA
   - Avalie se userFacingUnderstanding aparece cedo para o usuario apos intent.resolved.
   - Verifique se mensagens sao claras, didaticas e acionaveis, semelhantes a boas experiencias de OpenAI/Gemini/Grok: resposta estruturada, opcoes claras, quick replies apropriadas, icones/cores quando aplicavel e sem texto interno vazando.
   - Confirme que erros de provedor ou baixa confianca explicam a situacao sem culpar o usuario e oferecem proximos passos.
   - Valide variacoes abertas de prompts, nao apenas frases exatas:
     - "posso criar tabelas com quais dados?"
     - "quais tabelas da para montar aqui?"
     - "que formularios eu posso criar?"
     - "entre os dados existentes, quais viram graficos?"
     - "quero um painel com os dados disponiveis"
     - "o que esse ambiente ja conhece?"

7. Integracao, release e consumidores
   - Confirme que praxis-config-starter 0.1.0-rc.63 foi publicado e resolve no Maven Central.
   - Confirme que praxis-api-quickstart consome praxis.config.version=0.1.0-rc.63 no pom.xml.
   - Confirme que README/docs do quickstart nao apontam para versao corrente stale.
   - Confirme que o jar do quickstart empacota BOOT-INF/lib/praxis-config-starter-0.1.0-rc.63.jar.
   - Verifique se workflows/smokes estao alinhados ao LLM-first e nao forcam provider deterministico que mascare falhas reais.

Arquivos e areas para auditar
- praxis-config-starter/src/main/java/org/praxisplatform/config/ai/authoring/**
- praxis-config-starter/src/main/java/org/praxisplatform/config/service/**, se houver provider/router relacionado
- praxis-config-starter/src/main/resources/praxis-config-defaults.properties
- praxis-config-starter/docs/ai/contracts/**
- praxis-config-starter/docs/ai/agentic-authoring-streaming.md
- praxis-config-starter/tools/Invoke-QuickstartAgenticAuthoring*.ps1
- praxis-config-starter/.github/workflows/agentic-authoring-smoke.yml
- praxis-config-starter/.github/workflows/release.yml
- praxis-api-quickstart/pom.xml
- praxis-api-quickstart/README.md
- praxis-ui-angular Page Builder IA: servicos de authoring, stream client, quick replies e estado de processamento

Validacoes minimas esperadas
- No praxis-config-starter:
  - testes focais de intent resolver, LLM intent resolver, consultivo e stream/terminalidade.
  - mvn -B -P ci-smoke-unit -T 1C clean verify, se o escopo for release.
- No praxis-api-quickstart:
  - mvn -B verify com praxis.config.version=0.1.0-rc.63.
  - confirmar jar tf target/praxis-api-quickstart-2.0.0-rc.9.jar | grep praxis-config-starter-0.1.0-rc.63.
- Smoke remoto/local:
  - Agentic Authoring HTTP/SSE smoke com provider real.
  - prompts abertos consultivos e cenarios de provider_error/timeout.

Formato da resposta
1. Veredito executivo: aprovado, aprovado com ressalvas ou bloqueado.
2. Tabela por passo 1..7:
   - status
   - evidencia
   - gaps
   - risco corporativo
   - recomendacao
3. Findings ordenados por severidade com arquivo/linha.
4. Lacunas de contrato reais, se houver, separadas de problemas de UX/materializacao.
5. Regressao potencial em release/consumer.
6. Testes que faltam ou precisam ser fortalecidos.
7. Decisao final: pode seguir para proximo corte/deploy ou precisa patch antes.

Importante
- Nao aceite heuristica textual como decisao primaria.
- Nao aceite "funciona para os prompts exatos" como evidencia suficiente.
- Nao aceite resposta consultiva antes da decisao semantica.
- Nao aceite stream sem terminalidade garantida.
- Nao aceite documentacao publica com versao stale.
- Se houver divergencia entre starter canonico e consumidor, recomende corrigir a fonte canonica ou o consumidor, nao criar workaround local.
```
