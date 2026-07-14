# Rule Lab QL-07 — prova downstream pública e HTTP operacional

## Estado

`IMPLEMENTED_VALIDATED` no laboratório Quickstart em 2026-07-14. A prova confirma o consumo de
artefatos públicos e o comportamento do host de referência; ela não significa `Shadow Running`,
`Shadow Passed`, promoção de autoridade Ergon ou abertura da Fase 9.

## Coordenadas públicas

O build foi executado com JDK 21 e repositório Maven isolado, apontando somente para Maven Central.
Não houve `mvn install`, repositório de arquivo ou override de coordenada.

| Artefato | Versão | SHA-256 do JAR resolvido |
| --- | --- | --- |
| `io.github.codexrodrigues:praxis-metadata-starter` | `8.0.0-rc.106` | `c3e5b58e307017a271908de73e81b2b460b726cbcecb672392dcd2a29f7aea00` |
| `io.github.codexrodrigues:praxis-config-starter` | `0.1.0-rc.75` | `689ec5a3aaa1f86a2740dc0f111164e22957f3d7b14ee0789ecf82288e104595` |
| `io.github.codexrodrigues:praxis-rules-engine` | `0.1.0-beta.11` | `fd2103ec4200dc636a57c0aca6e54f82d288ab6c87f436b33ab4eae6f2181c20` |

O gate `scripts/workspace/Test-RuleLabQl07PublicMaven.ps1` registrou
`PUBLIC_MAVEN_VERIFIED`, Java `21.0.2`, 72 relatórios Surefire sem falha e host
`2.0.0-rc.9`. O JSON gerado fica em `target/ql07-public-maven-evidence.json` e não é versionado.

## Snapshot governado usado

- rule set: `extraordinary-grant-eligibility`, versão `1`;
- snapshot key: `3cbaa4e6-a072-4beb-ac6a-873be0f22940`;
- content hash: `79425743DD00FBA32F11286A7AEC28A2C73E1C4F0A281D0C6CDB17B920B47303`;
- activation revision: `1`.

A identidade acima é evidência operacional permitida. Facts, credenciais, payload integral do
snapshot e dados pessoais não são registrados.

## Prova HTTP

O gate `scripts/workspace/Invoke-RuleLabQl07HttpProof.ps1` parte de uma sessão autenticada e prova:

1. `403` para `shadow-compare` sem sessão;
2. health e build info do host;
3. discovery de `evaluate` e `shadow-compare` em `/schemas/actions`;
4. request schema de `evaluate` e response schema sanitizado de `shadow-compare`, incluindo ETag e
   `X-Schema-Hash` coerentes;
5. leitura do head governado com os headers de tenant, ambiente e Origin exigidos pelo config starter;
6. shadow ALLOW e DENY com `MATCH`, sem referência da fixture na resposta;
7. invariância das contagens de request, effect, action execution e transition após o dual-run;
8. avaliação persistida `ALLOW` e lifecycle `EVALUATED -> SUBMITTED -> APPROVED -> APPLIED` com
   `ETag`, `If-Match` e `Idempotency-Key`;
9. efeito financeiro local exatamente uma vez;
10. remoção determinística da fixture e retorno das quatro contagens ao baseline, inclusive em falha.

O resultado final foi `QL07_HTTP_VERIFIED` em `2026-07-14T03:53:47Z`. O JSON sanitizado fica em
`target/ql07-http-evidence.json` e não é versionado.

## Achados operacionais corrigidos

A primeira execução real encontrou duas lacunas que os testes isolados não revelavam:

- o timeout default de shadow de 250 ms produzia falso `TECHNICAL_ERROR` durante aquecimento do host
  corporativo; o default passou para 1 s, ainda limitado ao intervalo configurável de 1 ms a 5 s;
- o launcher e o smoke não exigiam `PRAXIS_RESOURCE_VERSION_ETAG_SECRET`, embora o recurso publique
  actions que dependem de ETags assinados. Os preflights e os exemplos de ambiente agora exigem um
  segredo independente do JWT e falham cedo quando ele está ausente.

O leitor de headers do gate também foi tornado case-insensitive e compatível com headers
multivalorados. Nenhuma dessas correções altera outcome, snapshot, lifecycle ou semântica dos
starters.

## Validação focal

- `mvn "-Dtest=ExtraordinaryBenefitShadowComparisonServiceTest,ExtraordinaryBenefitRequestPilotIntegrationTest" test` com JDK 21: 15 testes, zero falha e zero erro;
- `mvn -DskipTests package` com JDK 21: `BUILD SUCCESS`;
- migrations operacionais V20260713_001 e V20260713_002 aplicadas atomicamente no banco de referência;
- drift check do datasource operacional executado com sucesso pelo usuário de serviço;
- prova Maven pública e prova HTTP descritas acima.

Uma repetição ampla posterior encontrou apenas um timeout transitório em
`FuncionarioEntityLookupIntegrationTest` ao materializar `/schemas/filtered`; o mesmo teste foi
reexecutado isoladamente no mirror Maven Central e passou 3/3. Essa flutuação de inicialização
OpenAPI não foi ocultada nem classificada como regressão do Rule Lab.

## Próximo gate

QL-08 deve produzir o stress report Ergon-like para transformação pré-DML, cardinalidade
statement-level, estado explícito de operação, visibilidade pós-write e falhas combinadas. QL-07 não
remove os blockers de owner, HADES real, baseline legado ou readiness global.
