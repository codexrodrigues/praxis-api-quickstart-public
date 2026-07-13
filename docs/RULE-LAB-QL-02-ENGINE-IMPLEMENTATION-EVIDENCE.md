# Rule Lab QL-02 — evidência do core e consumo público

- Data: 2026-07-13
- Classificação: `arquitetural`, `transversal` e `contrato-publico`
- Estado: `QL_02_SERVICE_LEVEL_VALIDATED`

## Resultado

O owner canônico `praxis-rules-engine` publicou os contratos runtime-neutros,
planner determinístico e evaluator service-level definidos pelos ADRs
P2F-01–04, 06 e 08. A primeira publicação, `0.1.0-beta.7`, revelou no desenho
do piloto uma consolidação incorreta: um `ALLOW` intermediário podia elevar um
ramo terminal `NOT_APPLICABLE`. Ela não deve ser adotada por hosts.

A correção limpa foi publicada como `0.1.0-beta.8`, ainda no canal beta ativo,
com engine contract `1.1`. O Quickstart consome essa coordenada pública sem
`mvn install`, repositório de arquivo ou override local e mantém a fronteira
service-level: sem endpoint, persistência, snapshot loader ou execução de
efeitos nesta onda.

## Contratos e invariantes provados no engine

- identidade versionada de RuleSet, slots, bindings e roots permitidos;
- cardinalidade e agregação explícitas (`SINGLE_RESULT` e `DENY_OVERRIDES`);
- composição de cliente compatível com a policy de override do produto;
- DAG limitado, referências fechadas, ordenação topológica e digest canônico;
- baseline exato de engine contract, dialect e SHA-256 do corpus;
- executores Java por chave namespaced e versão exata;
- decisões `ALLOW`, `DENY`, `NOT_APPLICABLE`, `INCONCLUSIVE` e
  `TECHNICAL_ERROR`, sem converter falha técnica em negativa de negócio;
- distinção entre fact ausente e `null`, tempo/timezone explícitos e
  propagação de dependência sem ocultar `DENY` independente;
- consolidação global calculada pelos bindings terminais do DAG, sem elevar
  `NOT_APPLICABLE` por causa de guards intermediários;
- limites estruturais para facts e outputs Java;
- dependência explícita e bloqueio de `EFFECT_INTENT` antes de decisão
  consolidável como `ALLOW`;
- `planDigest`, `factsDigest`, compatibilidade e implementações utilizadas no
  resultado, sem facts integrais ou duração não determinística.

## Cobertura dos goldens de QL-01 nesta onda

| Golden | Evidência do core |
| --- | --- |
| QLG-01 | determinismo, allow, cálculo Java puro e BRL |
| QLG-02 | protected guard nega e impede cálculo |
| QLG-03 | override de cliente incompatível é rejeitado |
| QLG-07 | missing produz inconclusive e `null` continua presente |
| QLG-08 | ciclo, referência ausente e dependência para stage futuro são rejeitados |
| QLG-10 | implementação Java ausente ou com versão incompatível é rejeitada |
| QLG-15 | facts não atravessam o resultado; permanece apenas digest canônico |

QLG-04/05 exigem contratos tipados de replacement/parâmetro além do core
mínimo. QLG-09/11 pertencem ao loader/snapshot de QL-03. QLG-12 pertence ao
shadow de QL-06. QLG-13/14 pertencem a command/effects de QL-05.

## Validação executada

No `praxis-rules-engine`, com Java 21 e Maven 3.9+:

```text
mvn clean verify
Tests run: 29, Failures: 0, Errors: 0, Skipped: 0
JAR, sources e Javadocs gerados
```

Após completar a documentação Javadoc da nova API, `mvn javadoc:jar` foi
executado sem warnings. A publicação oficial da `0.1.0-beta.8` concluiu com
sucesso e a coordenada ficou disponível no Maven Central.

No `praxis-api-quickstart`, o cache local não continha a `beta.8` antes do
primeiro build. O Maven resolveu a coordenada pública e o gate focal passou:

```text
mvn -Dtest=ExtraordinaryGrantRuleLabServiceTest,RuleLabGoldenContractTest test
Tests run: 6, Failures: 0, Errors: 0, Skipped: 0
io.github.codexrodrigues:praxis-rules-engine:jar:0.1.0-beta.8:compile
```

## Prova de negócio no host

O laboratório monta um RuleSet de concessão extraordinária com 10 slots e 11
bindings: autorização protegida, elegibilidade do vínculo, duplicidade,
aplicabilidade do programa, restrições de produto e cliente, calendário de
pagamento, parâmetros, cálculo Java puro, orçamento e plano de efeito Java
puro. Os dois executores Java são registrados por chave namespaced e versão
exata; o último produz apenas `PLANNED_NOT_EXECUTED`.

Os testes comprovam:

1. caminho complexo `ALLOW`, valor BRL determinístico, facts imutáveis e efeito
   somente planejado;
2. `DENY` protegido com short-circuit antes do cálculo;
3. `NOT_APPLICABLE` terminal não elevado por guards anteriores;
4. fact obrigatório ausente preservado como `INCONCLUSIVE`, com proveniência
   da propagação e efeito bloqueado;
5. orçamento insuficiente negando após cálculo puro e antes do efeito;
6. round-trip dos 15 goldens contratuais de QL-01.

## Próximo gate

QL-02 está concluído no limite service-level. O próximo passo é QL-03:
definir P2F-ADR-07 e integrar snapshot imutável, ETag, compatibilidade,
hot reload e rollback no owner apropriado (`praxis-config-starter` + adapter do
host), sem promover ainda endpoint de negócio, persistência de concessão ou
execução de efeitos.
