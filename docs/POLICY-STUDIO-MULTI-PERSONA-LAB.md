# Laboratório multi-persona do Policy Studio

## Objetivo

Este laboratório prova a segregação de funções usada pelo Policy Studio sem transformar o login do
Quickstart em um IAM de produção. Ele é opt-in, fica desabilitado por padrão e publica seis sujeitos
técnicos distintos. Em um host corporativo, o IdP e o mapeamento IAM substituem integralmente essas
credenciais; as authorities e as capabilities server-owned continuam sendo o contrato observado
pelo Studio.

Classificação do inventário de aderência:

- enforcement e capabilities existentes: `ja-suportado-so-ux`;
- três identidades acumulando authoring, publicação e operação no laboratório anterior:
  `ja-suportado-mal-nomeado-ou-mal-materializado`;
- isolamento server-owned no mesmo PostgreSQL/schema: `ja-suportado-so-ux`;
- prova visual com o Studio real e isolamento cross-tenant no Neon: `suportado-parcialmente`;
- novo contrato público: não foi necessário.

## Matriz de responsabilidades

| Persona | Authorities do laboratório | Responsabilidade permitida | Negativas obrigatórias |
| --- | --- | --- | --- |
| author | definition reader/author, snapshot reader | criar workspace, editar draft, cenários e sandbox | não revisa, publica, ativa nem executa prova operacional |
| approver-a | definition reader/approver, composition approver, snapshot reader | revisar workspace e aprovar composição | não authora, publica nem opera |
| approver-b | mesmas authorities do approver-a, outro sujeito | segunda aprovação independente | mesmas negativas do approver-a |
| publisher | definition reader, snapshot publisher/reader | publicar Definition, composição e snapshot | não authora, aprova nem opera |
| operator | definition reader, snapshot operator/reader, operational-test operator | prova host-owned, activate, rollback e rollout | não authora, revisa nem publica |
| auditor | definition reader, snapshot reader | catálogo, timeline, evidências e snapshots | nenhuma mutation |

Nenhuma persona recebe a união preventiva de authorities. A mesma regra vale para o futuro
assistente LLM: ele só pode delegar um comando que a persona humana atual possui, e o servidor
revalida capability, segregação e concorrência.

## Configuração

Defina `APP_AUTH_GOVERNANCE_LAB_ENABLED=true` e forneça usuário e senha distintos para:

- `APP_AUTH_GOVERNANCE_AUTHOR_*`;
- `APP_AUTH_GOVERNANCE_APPROVER_A_*`;
- `APP_AUTH_GOVERNANCE_APPROVER_B_*`;
- `APP_AUTH_GOVERNANCE_PUBLISHER_*`;
- `APP_AUTH_GOVERNANCE_OPERATOR_*`;
- `APP_AUTH_GOVERNANCE_AUDITOR_*`.

Os nomes também devem ser diferentes do administrador local. Exemplos sem segredo estão em
`.env.dev.example` e `.env.prod.example`.

Uma sessão já autenticada no laboratório pode trocar de sujeito com
`POST /auth/governance-lab/session/{author|approver-a|approver-b|publisher|operator|auditor}`.
O browser não recebe as senhas configuradas. O endpoint não substitui impersonation corporativa,
deve permanecer desabilitado fora do laboratório e continua sujeito à proteção CSRF do host.

## Evidência executável

O teste `GovernanceLabPersonaSecurityIntegrationTest` percorre a cadeia real:

1. login do author com credencial configurada;
2. emissão e validação do JWT em cookie HttpOnly;
3. leitura governada e comandos positivos do author;
4. troca sucessiva para os dois approvers, publisher, operator e auditor;
5. comandos positivos de cada responsabilidade;
6. `403` cruzado para authoring, review, composição, publicação, operação e prova host-owned;
7. acesso anônimo negado mesmo com `read-open=true`.

Validação focal:

```bash
mvn -B \
  -Dtest=GovernanceLabIdentityServiceTest,GovernanceLabPersonaSecurityIntegrationTest,SecurityConfigDomainRuleReadPolicyTest,SecurityConfigRuleSnapshotReadPolicyTest,SecurityConfigSpaCsrfPolicyTest \
  test
```

Os provisionadores QL-07 e Payroll também consomem as seis credenciais. Definitions são criadas pelo
author, composições são aprovadas pelos dois approvers, publicação fica com publisher e qualquer
movimento de head ou prova operacional fica com operator.

### Isolamento no mesmo banco

`PolicyStudioOperationalHttpPostgresIntegrationTest` prova a fronteira de escopo com dois tenants
persistidos no mesmo PostgreSQL e no mesmo schema do Config:

1. o principal autenticado resolve `tenant=desenv` e `environment=local` no servidor;
2. cabeçalhos `X-Tenant-ID` e `X-Env` conflitantes não alteram o escopo persistido;
3. o retry com a mesma chave idempotente continua ligado ao escopo do principal;
4. um workspace de `tenant-b/prod` permanece invisível e retorna `404`, ainda que o cliente envie
   esses valores nos cabeçalhos;
5. a tentativa rejeitada não cria Test Run, resultados ou reserva operacional adicional.

Validação focal:

```bash
mvn -B -Dtest=PolicyStudioOperationalHttpPostgresIntegrationTest test
```

Essa é uma prova estrutural local contra PostgreSQL real embutido. Ela valida o modelo de isolamento
usado também por uma conexão Neon, mas não afirma que o gate foi repetido contra uma instância Neon
remota, suas credenciais ou políticas operacionais.

## O que esta prova não afirma

Este corte fecha a coerência `identidade configurada → JWT → authority → matcher HTTP`. Ele ainda
não afirma:

- jornada visual real do Studio em `4302` contra o Quickstart em `8088` para cada persona;
- isolamento entre dois tenants/environments no mesmo banco Neon;
- integração com IdP corporativo, MFA, provisioning ou recertificação de acesso;
- canários Oracle/HADES do Ergon;
- aprovação ou publicação autônoma por LLM.

O próximo gate combina esta matriz com capabilities retornadas pelo Config e verifica no browser
que cada comando aparece somente para a persona autorizada. Depois, a prova estrutural de isolamento
deve ser repetida no Neon com dois tenants, incluindo tentativas cross-tenant negadas e ETag stale.
