# Config rc.151 — política de modelo por provider e linhagem

O host consome o corte canônico com duas correções: refinamento de campos preserva a decisão de authoring já resolvida, e modelo por fase é aplicado após resolver provider explícito/salvo/default. Modelos configurados não foram alterados.

Gate fonte Config [34035509733](https://github.com/codexrodrigues/praxis-config-starter/actions/runs/34035509733), SHA `559a047a4be44563c46bf78147b4e06442afed9e`: 3/3 cenários, um turno humano, zero retries, nove verificações de master/detail, comando, conflito, atualização e reload; cleanup confirmado. Pre-intent Luna passou. Intenção rápida mini expirou; passe completo mini concluiu. Custo total desconhecido. Esse gate não é certificação universal nem comprova sozinho a jornada livre de funcionários no Render.

Validação local anterior: 333 testes focais Config; Quickstart contra JAR fonte passou 526 testes com 22 skips. O host contra os bytes públicos rc.151 passou `mvn -U verify`: 548 previstos, 526 executados, 22 skips, zero falhas/erros, exit 0. JAR aninhado idêntico ao Central, SHA-256 `1cbcd54312c0db7cb45aa11ee589dd20e3d7261b20f1f91835df5cce02902242`; SHA-512 do JAR/POM público também conferido. Quickstart rc.49 foi publicado e confirmado UP no Render em 2026-09-06T14:14:36Z.

Impacto: pin de dependência do host e esta evidência operacional. Sem DTO/endpoint novo no host; frontend, corpus e manifests não mudam. Contrato Java interno do starter requer recompilação; o host usa builders.


Release: preparação 34037734563 e publicação 34038369120 aprovadas. Tag fonte f5315d2094fbd25db3b700969a846c7726fe3035, snapshot público a2eeb4dd5fb16f6fdee2d5ebe5bded77c2e42026; POMs fonte/público confirmam rc.49/Config151. CI do pin 34037036192 passou (540 executados, 8 skips); CI da versão 34038368893 também passou.

A jornada livre no Render confirmou planejamento/refinamento Luna e preservação de linhagem, mas apply permaneceu bloqueado: verifyDomainOperation retornou operational-grounding-binding-required, levando a semantic-preview-resource-workspace-grounding-required. Um turno humano, nenhum apply, três registros sintéticos removidos e cleanup confirmado. Próximo passo: auditar bindings elegíveis de Domain Knowledge em demo/landing e sua projeção; catálogo RAG reconciliado não comprova esses bindings.

O smoke automático 34038403312 falhou em RH por curl56. A mesma verificação HTTP de leitura passou localmente; o workflow completo não foi repetido nem declarado aprovado. Nenhuma configuração de modelo foi alterada para Astra.
