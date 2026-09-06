# Config rc.151 — política de modelo por provider e linhagem

O host consome o corte canônico com duas correções: refinamento de campos preserva a decisão de authoring já resolvida, e modelo por fase é aplicado após resolver provider explícito/salvo/default. Modelos configurados não foram alterados.

Gate fonte Config [34035509733](https://github.com/codexrodrigues/praxis-config-starter/actions/runs/34035509733), SHA `559a047a4be44563c46bf78147b4e06442afed9e`: 3/3 cenários, um turno humano, zero retries, nove verificações de master/detail, comando, conflito, atualização e reload; cleanup confirmado. Pre-intent Luna passou. Intenção rápida mini expirou; passe completo mini concluiu. Custo total desconhecido. Esse gate não é certificação universal nem comprova sozinho a jornada livre de funcionários no Render.

Validação local anterior: 333 testes focais Config; Quickstart contra JAR fonte passou 526 testes com 22 skips. O host contra os bytes públicos rc.151 passou `mvn -U verify`: 548 previstos, 526 executados, 22 skips, zero falhas/erros, exit 0. JAR aninhado idêntico ao Central, SHA-256 `1cbcd54312c0db7cb45aa11ee589dd20e3d7261b20f1f91835df5cce02902242`; SHA-512 do JAR/POM público também conferido. A aplicação atual permanece rc.48 até a publicação oficial seguinte.

Impacto: pin de dependência do host e esta evidência operacional. Sem DTO/endpoint novo no host; frontend, corpus e manifests não mudam. Contrato Java interno do starter requer recompilação; o host usa builders.
