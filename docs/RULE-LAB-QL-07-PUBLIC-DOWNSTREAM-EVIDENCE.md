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
| `io.github.codexrodrigues:praxis-rules-engine` | `0.1.0-beta.12` | `16f058cfb82eb8f8afb71b0f39a8a35ba4dffb0e0763d530847676329922dd99` |

O gate `scripts/workspace/Test-RuleLabQl07PublicMaven.ps1` registrou
`PUBLIC_MAVEN_VERIFIED`, Java `21.0.2`, 72 relatórios Surefire sem falha e host
`2.0.0-rc.9`. O JSON gerado fica em `target/ql07-public-maven-evidence.json` e não é versionado.

Em 2026-07-15, a coordenada do engine foi revalidada em repositório Maven
isolado após o workflow oficial publicar `beta.12`. O smoke focal consumiu o
engine contract `1.2` diretamente do Central e comprovou a proposta tipada do
P2F-ADR-11; os hashes dos starters acima permanecem da execução QL-07 original.

### Revalidação pública de 2026-07-16

Após a publicação oficial do config starter `0.1.0-rc.79`, o gate QL-07 foi
reexecutado integralmente contra `https://repo.maven.apache.org/maven2`, com
repositório local vazio e sem override de artefato do workspace. O resultado foi
`PUBLIC_MAVEN_VERIFIED`, com 89 relatórios Surefire sem falha, host
`2.0.0-rc.16` e as seguintes coordenadas efetivamente resolvidas:

| Artefato | Versão | SHA-256 do JAR resolvido |
| --- | --- | --- |
| `io.github.codexrodrigues:praxis-metadata-starter` | `8.0.0-rc.110` | `036d7fbe9d85369c8747ed9cbde9a55fbeeda463ebbb545af6b8f097f9bb74d0` |
| `io.github.codexrodrigues:praxis-config-starter` | `0.1.0-rc.79` | `f99e8801b107200224ce57acbbac9e55ab27def337e8d6d9cc7f7cd1e067a10b` |
| `io.github.codexrodrigues:praxis-rules-engine` | `0.1.0-beta.13` | `92a15ad4284a8dd4a07c61b0ec4ae0c9eba63015e8ffac8bcdb8626aa329aa33` |

Uma segunda revalidação em 2026-07-16 consumiu o config starter
`0.1.0-rc.82` de um repositório Maven novamente vazio e exclusivo do Central.
Esse corte inclui a validação estrutural segura do snapshot anterior durante
supersessão, sem recompilá-lo com uma baseline de engine posterior. O JAR
resolvido apresentou SHA-256
`067118683AD8CDF935FD9143BA8C1C2FAEF1F1098D778B1F0FC578258967B811`.
Os 40 testes focais de IAM, segurança servlet, facts autoritativos, lifecycle e
revalidação de apply passaram sem falha; o pacote do host também foi gerado
somente com dependências públicas.

Uma terceira revalidação em 2026-07-16 consumiu o config starter
`0.1.0-rc.83`, já publicado pelo workflow oficial, no mesmo mirror exclusivo do
Maven Central. O gate integral terminou como `PUBLIC_MAVEN_VERIFIED` com JDK
21.0.2 e host `2.0.0-rc.16`:

| Artefato | Versão | SHA-256 do JAR resolvido |
| --- | --- | --- |
| `io.github.codexrodrigues:praxis-metadata-starter` | `8.0.0-rc.113` | `f0ed130c0e725ae7a4b753ad3950bd203762779adae277a69f40b07b9fb3def9` |
| `io.github.codexrodrigues:praxis-config-starter` | `0.1.0-rc.83` | `80fe6cad633968b47b2cec5db1cc51c50da4b14f21cc23b163fb848ba4672529` |
| `io.github.codexrodrigues:praxis-rules-engine` | `0.1.0-beta.13` | `92a15ad4284a8dd4a07c61b0ec4ae0c9eba63015e8ffac8bcdb8626aa329aa33` |

Uma quarta revalidação em 2026-07-17 consumiu o engine
`0.1.0-beta.14`, publicado pelo workflow oficial com engine contract `1.4`,
no mirror exclusivo do Maven Central. O gate integral terminou como
`PUBLIC_MAVEN_VERIFIED` com JDK 21, 99 relatórios Surefire e 336 testes sem
falha ou erro. A nova validação encontrou e corrigiu no Rule Lab um
`EFFECT_INTENT` que não declarava dependência direta de decisão de negócio:
ele preserva a transformação requerida e passa a depender diretamente de
`budget.availability` (`POST_DECISION`), sem relaxar o gate do engine.

| Artefato | Versão | SHA-256 do JAR resolvido |
| --- | --- | --- |
| `io.github.codexrodrigues:praxis-metadata-starter` | `8.0.0-rc.113` | `f0ed130c0e725ae7a4b753ad3950bd203762779adae277a69f40b07b9fb3def9` |
| `io.github.codexrodrigues:praxis-config-starter` | `0.1.0-rc.83` | `80fe6cad633968b47b2cec5db1cc51c50da4b14f21cc23b163fb848ba4672529` |
| `io.github.codexrodrigues:praxis-rules-engine` | `0.1.0-beta.14` | `6e96a90da1d1d519f00ab4077737300bc3b175a1b8fb4a7e5164811157958331` |

### Revalidação pública de 2026-07-18

Após a correção da atestação do corpus público, o workflow oficial publicou o
engine `0.1.0-beta.15` com engine contract `1.4`. A linha `beta.14` permanece
como evidência histórica, mas não é recomendada para novo consumo porque o hash
declarado por ela não correspondia byte a byte ao corpus empacotado. O
`beta.15` restaura essa invariância e foi consumido transitivamente pelo config
starter `0.1.0-rc.85`, também publicado pelo workflow oficial.

O gate integral foi executado com JDK 21, mirror exclusivo de
`https://repo.maven.apache.org/maven2` e repositório Maven isolado, sem
`mvn install`, `file://` ou override de coordenada. O resultado foi
`PUBLIC_MAVEN_VERIFIED`: 100 relatórios Surefire, 339 testes, zero falha, zero
erro e 15 skips de cenários condicionais. A tentativa automática de detectar
Docker ocorreu somente em um teste opcional, que foi pulado; nenhum container
foi iniciado.

| Artefato | Versão | SHA-256 do JAR resolvido |
| --- | --- | --- |
| `io.github.codexrodrigues:praxis-metadata-starter` | `8.0.0-rc.113` | `f0ed130c0e725ae7a4b753ad3950bd203762779adae277a69f40b07b9fb3def9` |
| `io.github.codexrodrigues:praxis-config-starter` | `0.1.0-rc.85` | `044eb9a3e903dbe3ebbb9ddc58a12b56715bd84603ab972bf4934068b723ba7d` |
| `io.github.codexrodrigues:praxis-rules-engine` | `0.1.0-beta.15` | `c5de6e8b0288094f15dcd45ca4e9de220026836b87cd1499d152d18a2ee62bf4` |

Os testes que exercitam o registry Angular usaram uma geração limpa do commit
`865af855` de `praxis-ui-angular`, cujo JSON de ingestão apresentou SHA-256
`ABD12A4E8988E70F3E52E6BD6AE026B4E7CD30C251BD2BB2B3C7024D19F0B6B2`.
O corpus integral, com 105 componentes e chunks semanticamente segmentados,
passou pelo gate fail-closed do config starter. Uma segunda prova focal usa
somente o componente `praxis-table`, pois sua finalidade é verificar o slice de
affordances e repetir a ingestão integral no mesmo fork causava pressão de heap
sem ampliar a cobertura semântica.

## Snapshot governado usado

- rule set: `extraordinary-grant-eligibility`, versão `4`;
- snapshot key: `72bb3b86-7185-4ed3-a906-dd967750b882`;
- content hash: `BE505F0DEB811331F51E25CC857DF2D1D1AE65A507F16C7F6BC7973527148BC7`;
- activation revision: `4`;
- composition digest: `42E87B4F41C0206F761657BF218326A756D4375ED31EDF0142A2755B96C66879`.

A identidade acima é evidência operacional permitida. Facts, credenciais, payload integral do
snapshot e dados pessoais não são registrados.

### Supersessão maker-checker de 2026-07-16

O snapshot legado ativo chegou ao gate com `executionReady=false`, `governanceState=INVALID` e foi
rejeitado pelo catálogo Java atual. O host não o reescreve nem relaxa a admissão. O fluxo de
recuperação publica uma versão imutável superior usando o ETag forte do head, o manifest canônico e
duas aprovações persistidas de sujeitos autenticados distintos. Um terceiro sujeito publica o
digest inalterado. O laboratório local oferece três identidades opt-in apenas para essa prova; em
ambiente corporativo elas devem ser substituídas por IdP/IAM real.

O gate inclui duas negativas obrigatórias: o publisher recebe `403` ao tentar aprovar a composição
e um approver recebe `403` ao tentar publicar. Assim, `corporate-mode=false` não pode produzir uma
evidência maker-checker válida por acidente.

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
10. retenção da fixture fictícia aplicada, com prova escopada de exatamente uma requisição, um
    efeito, quatro execuções e três transições. A auditoria append-only não é apagada pelo gate;
    apenas o shadow, que não persiste, exige invariância global das contagens.

O resultado foi revalidado como `QL07_HTTP_VERIFIED` em `2026-07-16T20:29:20Z`. O JSON sanitizado fica em
`target/ql07-http-evidence.json` e não é versionado.

## Achados operacionais corrigidos

A primeira execução real encontrou duas lacunas que os testes isolados não revelavam:

- o timeout default de shadow de 250 ms produzia falso `TECHNICAL_ERROR` durante aquecimento do host
  corporativo; o default passou para 1 s, ainda limitado ao intervalo configurável de 1 ms a 5 s;
- o launcher e o smoke não exigiam `PRAXIS_RESOURCE_VERSION_ETAG_SECRET`, embora o recurso publique
  actions que dependem de ETags assinados. Os preflights e os exemplos de ambiente agora exigem um
  segredo independente do JWT e falham cedo quando ele está ausente.
- o host permitia genericamente `/api/praxis/config/**` antes das regras maker-checker; a transição
  de definição alcançava o guard de autoaprovação (`422`) em vez de falhar no IAM (`403`). As rotas
  de aprovação de definição, aprovação de composição e publicação agora exigem suas autoridades
  específicas antes do `permitAll` legado do restante do config store.
- os loaders de `.env.dev` sobrescreviam variáveis fornecidas pelo processo e podiam desativar um
  override operacional explícito. O launcher e os gates agora preservam a precedência do ambiente
  do processo e usam o arquivo somente para valores ausentes.

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
