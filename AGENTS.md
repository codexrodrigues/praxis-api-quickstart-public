AGENTS.md - Praxis API Quickstart

Escopo e Heranca
- Escopo: aplica-se a `praxis-api-quickstart` e subpastas.
- Herda: segue o `AGENTS.md` da raiz do monorepo. Este arquivo adiciona regras locais.
- Foco deste guia: explicar o papel canonico do quickstart no ecossistema Praxis, suas fronteiras com `praxis-metadata-starter` e `praxis-config-starter`, e a menor validacao confiavel para cada tipo de mudanca.
- Nao editar por padrao: `target/`, `BOOT-INF/`, `logs/`, `hs_err_pid*.log`, `docs/apidocs/` e outros artefatos gerados, salvo quando a tarefa for explicitamente sobre output gerado.

Premissa Local
- O quickstart deve ser tratado como host operacional de referencia para uma plataforma de decisoes semanticas authoradas por IA.
- Os dominios de exemplo devem demonstrar como a IA entende o dominio, encontra evidencias, authora decisoes governadas e materializa comportamento em superfícies reais; nao devem virar apenas exemplos de CRUD ou de configuracao de formulario.

Inventario Local de Aderencia Antes de Novo Contrato
- Antes de criar endpoint, DTO, path, regra de seguranca, exemplo de dominio ou adaptacao local para cobrir uma necessidade de plataforma, auditar primeiro o que os starters e o quickstart ja publicam em HTTP real.
- A pergunta obrigatoria e: o que `praxis-metadata-starter`, `praxis-config-starter` ou o proprio quickstart ja sabem por schema, capabilities, actions, config, AI context, headers, security policy, exemplos ou testes downstream, mas ainda nao esta sendo bem materializado?
- Classificar cada melhoria como `ja-suportado-so-ux`, `ja-suportado-mal-nomeado-ou-mal-materializado`, `suportado-parcialmente` ou `lacuna-real-de-contrato`.
- So `lacuna-real-de-contrato` autoriza novo contrato. Nesse caso, explicitar dado faltante, fonte canonica, consumidores impactados, artefatos derivados e validacao minima antes de implementar.
- Nao usar o quickstart para criar semantica paralela que deveria pertencer ao metadata starter, config starter ou runtime Angular. O quickstart prova integracao; nao redefine contrato canonico.

Classificacao Padrao da Mudanca
- `docs-apenas`: mudancas restritas a `AGENTS.md`, `README.md`, `docs/**` ou comentarios/Javadoc sem efeito em contrato ou comportamento.
- `local-pequena`: mudanca confinada ao host Spring Boot do quickstart, sem alterar semantica publica de `x-ui`, `/schemas/**`, `/capabilities` ou `/api/praxis/config/**`.
- `transversal`: mudanca que exige sincronizar mais de um dominio do quickstart, ou o quickstart mais algum consumidor/artefato derivado.
- `contrato-publico`: mudanca que altera endpoints publicados, `ApiPaths`, grupos OpenAPI, `@ApiResource`, schemas expostos, `_links`, discovery, headers, ETag ou qualquer comportamento visivel por cliente HTTP.
- `arquitetural`: mudanca que altera o papel do quickstart como host operacional de referencia, a fronteira entre host e starter, a politica de seguranca, ou a responsabilidade entre metadata/config/app.

Importancia do Quickstart no Ecossistema Praxis
- `praxis-api-quickstart` nao e apenas um exemplo de app. Ele e o host operacional de referencia do backend metadata-driven da plataforma.
- O projeto demonstra, em HTTP real, como contratos publicados pelos starters aparecem para consumidores reais, especialmente `praxis-ui-angular`, docs publicas, catálogos e clientes semanticos.
- O projeto tambem deve provar, sempre que possivel, como decisoes semanticas authoradas por IA percorrem grounding, governanca e materializacao em cenarios concretos.
- Quando existe duvida entre "o starter promete" e "o consumidor recebe", o quickstart e a prova operacional mais proxima do uso real.
- O quickstart tambem serve como baseline de modelagem para novos backends: paths canonicos, recursos resource-oriented, workflows explicitos, filtros, options, `_links`, surfaces, actions e capabilities.
- Em fase beta da plataforma, o quickstart deve refletir a semantica canonica atual e nao manter trilhas paralelas por conveniencia.
- Por ser um projeto de exemplo, publico e usado para apresentar como a plataforma Praxis deve ser aplicada, o codigo do quickstart deve privilegiar documentacao didatica e legivel para leitura humana.

Documentacao de domínio em DTOs e OpenAPI
- Cumprir no `AGENTS.md` da raiz do monorepo a secao **Diretriz de documentação de domínio em DTOs e OpenAPI**: nao usar scripts em lote que injetem `@Schema` a partir de rotulos de UI ou heurísticas; o processo canónico e investigar o recurso e preencher **cada propriedade** com texto de **negócio** (semântica, limites, relações), util como documentação de domínio e para superfícies que consomem OpenAPI.
- DTOs deste projecto são referência; descrições meramente técnicas ou copiadas de `label` não substituem esse preenchimento.
- **Inspiração (plataformas como SAP, resumo):** no dicionário ABAP, *domínio* carrega o **técnico** (tipo, valores); o *data element* carrega o **significado de negócio** reutilizável e a **documentação** (ex. ajuda contextual F1), separada dos **rótulos de ecrã**. Em CDS/OData, `@EndUserText` (label/quickInfo) alimenta metadados do serviço; anotações `@Semantics` e vocabulários OData descrevem **papel semântico estruturado** (moeda, quantidade). O governo formal de dados mestres (ex. MDG) vive **fora** da string do campo — no Praxis, o análogo é o **catálogo de domínio**, regras e `x-ui`/`@DomainGovernance`, com `@Schema` alinhado a essa verdade e nao colapsado no `label` do `@UISchema`.
- **O que fazer aqui:** tratar `@Schema(description=...)` como a **definição de negócio** do atributo (o quê, para quê, limites, relações com outros recursos); `@UISchema` como **rótulo/comportamento de UI**; quando o mesmo conceito muda de papel (entidade vs filtro vs vista), ajustar o texto por DTO ou operação (análogo a *data element supplements* por contexto). Ver `docs/SEMANTIC-DOMAIN-CATALOG-CONTRACT.md` para a fronteira entre vocabulário em runtime e regras executáveis.

Fronteira Canonica Local
- O quickstart e dono do host operacional:
  - bootstrap Spring Boot;
  - composicao de dependencias Maven;
  - configuracao de seguranca, CORS, CSRF e rate-limit;
  - domínios de exemplo/piloto, controllers concretos, DTOs, mappers e services;
  - validacao downstream dos contratos publicados pelos starters.
- O quickstart nao e a fonte canonica da semantica de metadata nem da semantica de config.
- O quickstart hospeda os starters e prova sua integracao. Se a semantica correta pertence ao starter, corrija no starter, nao com remendo local no host.

Relacao com `praxis-metadata-starter`
- `praxis-metadata-starter` e a fonte canonica de:
  - `x-ui`;
  - `/schemas/filtered`;
  - `/schemas/catalog`;
  - `/schemas/surfaces`;
  - `/schemas/actions`;
  - `GET {resource}/capabilities` e `GET {resource}/{id}/capabilities`;
  - `ETag`, `X-Schema-Hash` e resolucao OpenAPI/schema associada.
- No quickstart, essa semantica aparece concretamente via:
  - `src/main/java/com/example/praxis/apiquickstart/constants/ApiPaths.java`
  - controllers anotados com `@ApiResource`
  - DTOs/filtros com `@UISchema(endpoint=...)`
  - endpoints reais sob `/api/**`
  - testes de integracao que exercitam `/schemas/**`, `_links`, surfaces, actions e capabilities.
- Regra pratica:
  - se o problema for de semantica publicada por `/schemas/**`, `x-ui`, `_links`, surfaces/actions/capabilities ou resolucao OpenAPI, a causa raiz costuma estar no `praxis-metadata-starter`;
  - o quickstart deve ser usado para provar o impacto downstream, nao para redefinir localmente o contrato canonico.

Relacao com `praxis-config-starter`
- `praxis-config-starter` e a fonte canonica de `/api/praxis/config/**`.
- Isso inclui, entre outros:
  - `ui_user_config`;
  - `api_metadata`;
  - `ai_registry`;
  - endpoints de AI/context/patch/providers/templates;
  - semantica de headers de tenant/usuario/ambiente;
  - ETag de configuracao.
- No quickstart, essa superficie aparece como hosteada e protegida, nao redefinida:
  - `src/main/java/com/example/praxis/apiquickstart/config/SecurityConfig.java`
  - `src/main/java/com/example/praxis/apiquickstart/security/ConfigOriginRestrictionFilter.java`
  - properties e wiring do host.
- Regra pratica:
  - se o problema for de persistencia/config/AI catalog/template/context sob `/api/praxis/config/**`, a fonte canonica tende a ser o `praxis-config-starter`;
  - se o problema for de policy de exposicao, CORS, CSRF, cookie, `Origin` ou restricao operacional do host, a responsabilidade tende a ser do quickstart.
- Para authoring executavel e SSE, o quickstart deve ser tratado como host de prova downstream:
  - validar que o starter funciona em HTTP real;
  - validar que headers, `Origin`, identidade local, ETag, persistencia e cleanup funcionam no host;
  - nao redefinir manifests, validators, compile patch ou streaming dentro do quickstart.
- Quando `praxis.config.version` mudar, validar explicitamente que a versao resolve e que o host empacota sem override local.
- Para validar uma versao ainda nao publicada do starter, usar o fluxo local do `praxis-config-starter` que instala o starter no Maven local e empacota este quickstart contra essa versao.
- Para validar a release publicada, preferir este quickstart com a versao do `pom.xml` e executar `mvn -B verify`.

Ligacao Entre os Tres Projetos
- `praxis-metadata-starter` define o contrato estrutural e de discovery metadata-driven.
- `praxis-config-starter` define a superficie canonica de configuracao persistida, AI registry e orquestracao de contexto/patch.
- `praxis-api-quickstart` integra ambos no mesmo host Spring Boot, com seguranca real, banco real ou isolado, e dominios de exemplo que exercitam o baseline da plataforma.
- O quickstart e o ponto onde a plataforma prova que:
  - o contrato metadata-driven sai do starter e chega em HTTP real;
  - a superficie de config/AI funciona num host real;
  - os consumidores oficiais conseguem usar essas duas frentes sem semantica inventada localmente.

Arquivos e Fronteiras que Merecem Mais Cuidado
- Host e composicao:
  - `pom.xml`
  - `src/main/java/com/example/praxis/apiquickstart/ApiQuickstartApplication.java`
- Seguranca e exposure policy:
  - `src/main/java/com/example/praxis/apiquickstart/config/SecurityConfig.java`
  - `src/main/java/com/example/praxis/apiquickstart/security/ConfigOriginRestrictionFilter.java`
  - `src/main/java/com/example/praxis/apiquickstart/security/**`
- Paths e identidade publica dos recursos:
  - `src/main/java/com/example/praxis/apiquickstart/constants/ApiPaths.java`
- Recursos piloto e contratos publicados:
  - `src/main/java/com/example/praxis/apiquickstart/hr/**`
  - `src/main/java/com/example/praxis/apiquickstart/operations/**`
  - `src/main/java/com/example/praxis/apiquickstart/operationalassets/**`
  - `src/main/java/com/example/praxis/apiquickstart/riskintelligence/**`
- Testes downstream mais sensiveis:
  - `src/test/java/com/example/praxis/apiquickstart/config/QuickstartMetadataMigrationIntegrationTest.java`
  - `src/test/java/com/example/praxis/apiquickstart/config/EventosFolhaPilotIntegrationTest.java`
  - `src/test/java/com/example/praxis/apiquickstart/config/OpenApiGroupResolutionIsolatedIntegrationTest.java`
  - `src/test/java/com/example/praxis/apiquickstart/config/AiPatchSchemaResolutionIsolatedIntegrationTest.java`
  - `src/test/java/com/example/praxis/apiquickstart/config/SecurityConfig*.java`

Regras Locais Obrigatorias
- Releases publicas sao acionadas exclusivamente por `workflow_dispatch` com `create_tag=true`; o workflow persiste o `pom.xml` e cria a tag, e somente o push da tag publica o snapshot sanitizado. Nao publicar automaticamente em push de `main` nem criar tag manual como atalho.
- `RELEASE_PAT` e obrigatorio para encadear o workflow acionado pela tag, e `PUBLIC_RELEASE_SSH_KEY` deve gravar commit e a mesma tag no repositorio publico.
- Antes de uma tag, classificar explicitamente a linha de versao e exigir correspondencia entre POM fonte, tag fonte, POM publico, tag publica e `build.version` implantado.
- Trate `ApiPaths` como ponto de sincronizacao contratual, nao como simples helper de string.
- Nao espalhe literais de path quando houver constante canonica existente.
- Nao corrija no quickstart um problema que pertença semanticamente ao starter.
- Nao mova semantica de `metadata` para `config`, nem de `config` para `metadata`, por conveniencia.
- Se um endpoint `/api/praxis/config/**` estiver `permitAll` e ainda assim falhar, revise primeiro a politica de `Origin` no host antes de concluir que o starter esta errado.
- `_links`, `schemaUrl`, `requestSchemaUrl`, `responseSchemaUrl`, surfaces/actions/capabilities e headers de schema devem ser tratados como contrato vivo, nao como detalhe documental.
- Quando o quickstart expuser um workflow de negocio, prefira modelagem canonica por action/workflow; nao esconda comando de negocio como PATCH oportunista so para simplificar o app.
- Como este repositorio e exemplo publico de referencia, classes, controllers, DTOs, filtros, configuracoes e testes devem ser documentados de forma didatica quando a leitura nao for obvia.
- Prefira Javadoc, comentarios curtos e nomes claros que expliquem o papel de cada parte no uso da plataforma, nao apenas o comportamento local do Spring Boot.
- Ao introduzir fluxos relevantes para a plataforma, documente tambem a intencao pedagogica: o que aquele trecho demonstra sobre metadata-driven backend, config-store, discovery, workflow action, security policy ou integracao com o runtime oficial.

Quando Parar e Planejar Antes de Editar
- Mudancas em `ApiPaths`.
- Mudancas em `SecurityConfig` ou `ConfigOriginRestrictionFilter`.
- Mudancas em controllers/DTOs que alterem paths, grupos OpenAPI, `resourceKey`, `_links` ou schemas publicados.
- Mudancas que afetem `/schemas/**`, `/capabilities` ou `/api/praxis/config/**`.
- Mudancas que possam parecer locais, mas na verdade apontem defeito canonico em `praxis-metadata-starter` ou `praxis-config-starter`.

Validacao Minima por Escopo
- No Windows, prefira `mvn`. O `mvnw.cmd` deste projeto e apenas um stub para Maven instalado.
- Nao rode `clean verify` por reflexo. Escolha a menor suite confiavel para o write set.
- Mudancas so em docs/comentarios/Javadoc:
  - leitura final do arquivo alterado.
- Mudancas em seguranca/policy do host:
  - `mvn "-Dtest=SecurityConfigActuatorPolicyTest,SecurityConfigAiPatchPolicyTest,SecurityConfigReadOpenStatsPolicyTest,SecurityConfigSpaCsrfPolicyTest" test`
- Mudancas na integracao downstream do `praxis-metadata-starter`:
  - `mvn "-Dtest=OpenApiGroupResolutionIsolatedIntegrationTest,QuickstartMetadataMigrationIntegrationTest,EventosFolhaPilotIntegrationTest" test`
- Mudancas na integracao downstream do `praxis-config-starter`:
  - `mvn "-Dtest=AiPatchSchemaResolutionIsolatedIntegrationTest,SecurityConfigAiPatchPolicyTest" test`
- Mudancas em authoring/AI/SSE do `praxis-config-starter` validadas no quickstart:
  - no starter, executar `powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\tools\Invoke-QuickstartAgenticAuthoringHttpSmokeSuite.ps1 -Provider openai -QuickstartRoot ..\praxis-api-quickstart`
  - no quickstart, executar `mvn -B verify` quando a versao publicada no `pom.xml` for o alvo da validacao
- Mudancas em `pom.xml` que alterem `praxis.config.version`:
  - confirmar resolucao Maven da versao;
  - executar `mvn -B verify`;
  - se a mudanca for motivada por authoring/AI/SSE, rodar tambem o smoke suite do starter.
- Mudancas localizadas em um recurso piloto concreto:
  - prefira o teste focal daquele recurso em `src/test/java/com/example/praxis/apiquickstart/config/*PilotIntegrationTest.java`
- Validacao ampla do host:
  - `mvn test`

Artefatos Derivados e Sincronizacao
- Se a mudanca alterar superficie publica, revisar no minimo:
  - `README.md`
  - `docs/**` relevantes
  - payloads ou HTTP examples do proprio quickstart, se a tarefa tocar essas superficies
- Se a mudanca alterar a narrativa de integracao dos starters, revisar tambem:
  - `praxis-metadata-starter/AGENTS.md` e docs tecnicas relevantes quando a mudanca revelar nova fronteira canonica
  - `praxis-config-starter/README.md` ou docs locais quando a semantica real de config/AI mudar
- Se a mudanca for apenas do host e nao exigir sincronizacao derivada, diga isso explicitamente na resposta final.

Referencias Uteis
- `README.md`
- `src/main/java/com/example/praxis/apiquickstart/constants/ApiPaths.java`
- `src/main/java/com/example/praxis/apiquickstart/config/SecurityConfig.java`
- `src/main/java/com/example/praxis/apiquickstart/security/ConfigOriginRestrictionFilter.java`
- `src/test/java/com/example/praxis/apiquickstart/config/QuickstartMetadataMigrationIntegrationTest.java`
- `src/test/java/com/example/praxis/apiquickstart/config/EventosFolhaPilotIntegrationTest.java`
- `src/test/java/com/example/praxis/apiquickstart/config/OpenApiGroupResolutionIsolatedIntegrationTest.java`
- `../praxis-metadata-starter/AGENTS.md`
- `../praxis-metadata-starter/README.md`
- `../praxis-config-starter/README.md`

Regra de Pronto
- A tarefa so termina quando ficar claro:
  - se a mudanca pertence ao host, ao `praxis-metadata-starter` ou ao `praxis-config-starter`;
  - qual validacao minima foi executada;
  - quais artefatos derivados precisaram ser revisados, ou por que nao precisaram.
