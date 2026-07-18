# Rule Lab QL-09 — extensão Java de cliente protegida

## Resultado

O caso de concessão de benefício extraordinário agora prova uma extensão Java
`CUSTOMER` sem permitir que o documento publicado autorize o próprio código.
A política `customer.additional-eligibility` continua sendo apenas restritiva:
ela pode negar uma concessão aceita pelo produto, mas não contorna os guards
protegidos nem amplia elegibilidade.

O fluxo operacional é:

1. o Config Starter recebe o RuleSet candidato;
2. `DomainRuleImplementationCatalog` resolve, fora do payload, as coordenadas
   admitidas para tenant, ambiente e owner service;
3. o engine exige `RuleExtensionTrust` para todo binding `CUSTOMER + JAVA`;
4. o plano compilado incorpora chave, versão, hash do artefato, identidade do
   assinante, política de confiança e hash da evidência;
5. o host recompila o snapshot com seu registry executável e rejeita qualquer
   divergência antes de chamar código do cliente.

O catálogo permanece disponível mesmo antes de o loader do laboratório ser
ativado, permitindo a sequência corporativa `publicar → aprovar → habilitar`.
Isso não abre execução: a resposta é vazia fora do tenant, ambiente e owner
service configurados, e o runtime continua indisponível enquanto o loader está
desligado.

## Prova de negócio

O executor `customer:extraordinary-grant-additional-eligibility:1.0.0` lê apenas
`customer.additionalEligible`. Valor verdadeiro permite que o fluxo continue;
valor falso retorna `DENY/CUSTOMER_POLICY_RESTRICTED`. A regra permanece no slot
`customer.additional-eligibility`, cuja composição é `RESTRICT` e cuja
agregação é `DENY_OVERRIDES`.

Os guards `request.authorization-integrity` e `worker.legal-eligibility`
continuam `PROTECTED_GUARD + FORBIDDEN`. Uma extensão de cliente não pode ser
publicada nesses slots, mesmo que possua uma assinatura válida.

## Limite honesto do laboratório

Os dois SHA-256 e a identidade `lab-fixture:customer-policy-signer` são uma
atestação determinística de fixture. Eles exercitam o contrato e detectam
substituição entre planejamento e execução, mas não afirmam que o Quickstart
possui uma PKI corporativa.

Em ambiente corporativo, o bean de catálogo deve ser alimentado por um pipeline
externo que:

- calcule o SHA-256 do JAR exato após o build reproduzível;
- valide assinatura e cadeia do assinante contra allowlist por cliente;
- produza evidência imutável e auditável, com política versionada;
- publique/revogue a coordenada por tenant, ambiente e serviço;
- impeça classificar código de cliente como `PRODUCT`;
- monitore divergência de atestação e bloqueie execução antes do classloader.

Carregamento isolado de plugins, sandbox, distribuição de artefatos e revogação
online continuam responsabilidades da infraestrutura do host. O engine é puro:
ele valida a atestação recebida e a vincula ao plano, mas não faz rede, I/O,
verificação criptográfica nem carregamento dinâmico.

## Coordenadas públicas

- `io.github.codexrodrigues:praxis-rules-engine:0.1.0-beta.13`
- `io.github.codexrodrigues:praxis-config-starter:0.1.0-rc.78`

O Quickstart consome somente Maven Central; não existe `install` local,
`systemPath` ou override de repositório nesta prova.

O `praxis-config-starter:0.1.0-rc.77` chegou a ser publicado, mas o preflight
downstream da tag detectou ambiguidade de construtor causada por component scan
e auto-configuração concorrentes. O PR canônico `praxis-config-starter#255`
removeu a segunda rota de criação do bean; `rc.78` é a primeira versão válida
para este gate e supersede explicitamente `rc.77`.

## Validação mínima

```powershell
mvn "-Dtest=ExtraordinaryGrantRuleLabServiceTest,ExtraordinaryGrantRuleSnapshotRuntimeTest,ExtraordinaryBenefitRequestPilotIntegrationTest" test
```

O gate focal comprova decisão de negócio, escopo exato do catálogo, presença da
atestação no resultado, publicação governada e recompilação no registry do host.

Resultado em 2026-07-15, com Java 21:

- gate focal de regra/runtime: 19 testes, zero falhas;
- piloto HTTP/transacional: 28 testes, zero falhas;
- `mvn -U -B verify`: 274 testes em 80 suites, zero falhas, zero erros e
  7 skips declarados pelo corpus;
- a POM pública de `rc.78` respondeu HTTP 200 no Maven Central antes do verify;
- o verify público foi executado sem repositório Maven local customizado.
