#!/usr/bin/env bash
set -euo pipefail

BACKEND_URL="${BACKEND_URL:-http://localhost:8088}"
TENANT_ID="${TENANT_ID:-tenant-federation-smoke}"
ENVIRONMENT="${ENVIRONMENT:-local-e2e}"
ORIGIN="${ORIGIN:-https://praxisui-dev.web.app}"
SMOKE_RUN_ID="${SMOKE_RUN_ID:-$(date -u +%Y%m%d%H%M%S)}"

usage() {
  cat >&2 <<'USAGE'
Usage:
  scripts/verify-domain-federation-runtime.sh

Examples:
  BACKEND_URL=http://localhost:8088 scripts/verify-domain-federation-runtime.sh
  TENANT_ID=tenant-e2e ENVIRONMENT=local-e2e BACKEND_URL=http://localhost:8088 scripts/verify-domain-federation-runtime.sh

The script writes and activates a small federated domain knowledge release
through the config-starter domain-federation APIs.

Requirements:
  - runtime with /api/praxis/config/domain-federation/** available;
  - config database migrated through V21__create_domain_federation_read_model.sql;
  - praxis.domain-federation.persistence.enabled=true in the runtime;
  - no Flyway action is executed by this script.

Use SMOKE_RUN_ID to make repeated runs traceable. The script uses a tenant and
environment scope, then activates the candidate release for that scope.
USAGE
}

post_json() {
  local path="$1"
  local input_file="$2"
  local output_file="$3"

  curl -fsS "${BACKEND_URL%/}${path}" \
    -H "Origin: ${ORIGIN}" \
    -H "X-Tenant-ID: ${TENANT_ID}" \
    -H "X-Env: ${ENVIRONMENT}" \
    -H "Content-Type: application/json" \
    --data-binary "@${input_file}" \
    -o "$output_file"
}

get_json() {
  local path="$1"
  local output_file="$2"

  curl -fsS "${BACKEND_URL%/}${path}" \
    -H "Origin: ${ORIGIN}" \
    -H "X-Tenant-ID: ${TENANT_ID}" \
    -H "X-Env: ${ENVIRONMENT}" \
    -o "$output_file"
}

urlencode() {
  jq -nr --arg value "$1" '$value|@uri'
}

if [[ "${1:-}" == "-h" || "${1:-}" == "--help" ]]; then
  usage
  exit 0
fi

for command in curl jq; do
  if ! command -v "$command" >/dev/null 2>&1; then
    echo "Missing required command: $command" >&2
    exit 2
  fi
done

payload_file="$(mktemp "${TMPDIR:-/tmp}/praxis-domain-federation-payload.XXXXXX.json")"
dry_run_response="$(mktemp "${TMPDIR:-/tmp}/praxis-domain-federation-dry-run.XXXXXX.json")"
ingest_response="$(mktemp "${TMPDIR:-/tmp}/praxis-domain-federation-ingest.XXXXXX.json")"
candidate_list_response="$(mktemp "${TMPDIR:-/tmp}/praxis-domain-federation-candidate-list.XXXXXX.json")"
validation_response="$(mktemp "${TMPDIR:-/tmp}/praxis-domain-federation-validation.XXXXXX.json")"
activation_response="$(mktemp "${TMPDIR:-/tmp}/praxis-domain-federation-activation.XXXXXX.json")"
active_list_response="$(mktemp "${TMPDIR:-/tmp}/praxis-domain-federation-active-list.XXXXXX.json")"
context_operations_response="$(mktemp "${TMPDIR:-/tmp}/praxis-domain-federation-context-operations.XXXXXX.json")"
context_hr_response="$(mktemp "${TMPDIR:-/tmp}/praxis-domain-federation-context-hr.XXXXXX.json")"
trap 'rm -f "$payload_file" "$dry_run_response" "$ingest_response" "$candidate_list_response" "$validation_response" "$activation_response" "$active_list_response" "$context_operations_response" "$context_hr_response"' EXIT

echo "Verifying federated domain runtime APIs."
echo "BACKEND_URL=${BACKEND_URL}"
echo "TENANT_ID=${TENANT_ID}"
echo "ENVIRONMENT=${ENVIRONMENT}"
echo "SMOKE_RUN_ID=${SMOKE_RUN_ID}"

jq -n \
  --arg tenantId "$TENANT_ID" \
  --arg environment "$ENVIRONMENT" \
  --arg smokeRunId "$SMOKE_RUN_ID" \
  '{
    schemaVersion: "praxis.domain-federation/v0.1",
    tenantId: $tenantId,
    environment: $environment,
    sources: [
      {
        sourceKey: "source:human-resources",
        sourceType: "microservice",
        serviceKey: "praxis-api-quickstart",
        serviceName: "Praxis Quickstart HR",
        tenantId: $tenantId,
        environment: $environment,
        semanticOwner: "RH",
        technicalOwner: "platform-team",
        trustLevel: "authoritative",
        status: "active",
        latestReleaseKey: "domain-catalog:human-resources:v0.1",
        evidence: {kind: "runtime-smoke", runId: $smokeRunId}
      },
      {
        sourceKey: "source:security",
        sourceType: "microservice",
        serviceKey: "praxis-api-quickstart",
        serviceName: "Praxis Quickstart Security",
        tenantId: $tenantId,
        environment: $environment,
        semanticOwner: "Seguranca",
        technicalOwner: "platform-team",
        trustLevel: "authoritative",
        status: "active",
        latestReleaseKey: "domain-catalog:security:v0.1",
        evidence: {kind: "runtime-smoke", runId: $smokeRunId}
      },
      {
        sourceKey: "source:operations",
        sourceType: "microservice",
        serviceKey: "praxis-api-quickstart",
        serviceName: "Praxis Quickstart Operations",
        tenantId: $tenantId,
        environment: $environment,
        semanticOwner: "Operacoes",
        technicalOwner: "platform-team",
        trustLevel: "curated",
        status: "active",
        latestReleaseKey: "domain-catalog:operations:v0.1",
        evidence: {kind: "runtime-smoke", runId: $smokeRunId}
      },
      {
        sourceKey: "source:assets",
        sourceType: "microservice",
        serviceKey: "praxis-api-quickstart",
        serviceName: "Praxis Quickstart Assets",
        tenantId: $tenantId,
        environment: $environment,
        semanticOwner: "Patrimonio",
        technicalOwner: "platform-team",
        trustLevel: "curated",
        status: "active",
        latestReleaseKey: "domain-catalog:assets:v0.1",
        evidence: {kind: "runtime-smoke", runId: $smokeRunId}
      }
    ],
    contexts: [
      {
        contextKey: "human-resources.funcionarios",
        sourceKey: "source:human-resources",
        contextType: "bounded_context",
        label: "Funcionarios",
        description: "Cadastro e ciclo de vida de funcionarios.",
        semanticOwner: "RH",
        technicalOwner: "platform-team",
        tenantId: $tenantId,
        environment: $environment,
        status: "active",
        latestReleaseKey: "domain-catalog:human-resources.funcionarios:v0.1",
        evidence: {terms: ["funcionario", "colaborador", "employee"]}
      },
      {
        contextKey: "security.usuarios",
        sourceKey: "source:security",
        contextType: "bounded_context",
        label: "Usuarios",
        description: "Identidades, usuarios de acesso e principals.",
        semanticOwner: "Seguranca",
        technicalOwner: "platform-team",
        tenantId: $tenantId,
        environment: $environment,
        status: "active",
        latestReleaseKey: "domain-catalog:security.usuarios:v0.1",
        evidence: {terms: ["usuario", "user", "principal"]}
      },
      {
        contextKey: "operations.missoes",
        sourceKey: "source:operations",
        contextType: "bounded_context",
        label: "Missoes",
        description: "Planejamento operacional de missoes de campo.",
        semanticOwner: "Operacoes",
        technicalOwner: "platform-team",
        tenantId: $tenantId,
        environment: $environment,
        status: "active",
        latestReleaseKey: "domain-catalog:operations.missoes:v0.1",
        evidence: {terms: ["missao", "ordem de campo"]}
      },
      {
        contextKey: "assets.veiculos",
        sourceKey: "source:assets",
        contextType: "bounded_context",
        label: "Veiculos",
        description: "Frota, veiculos e disponibilidade para alocacao.",
        semanticOwner: "Patrimonio",
        technicalOwner: "platform-team",
        tenantId: $tenantId,
        environment: $environment,
        status: "active",
        latestReleaseKey: "domain-catalog:assets.veiculos:v0.1",
        evidence: {terms: ["veiculo", "frota", "asset"]}
      }
    ],
    contracts: [
      {
        contractKey: "contract:security-user-lookup:v0.1",
        contractType: "rest_endpoint",
        providerSourceKey: "source:security",
        providerContextKey: "security.usuarios",
        consumerContextKey: "human-resources.funcionarios",
        resourceKey: "security.users",
        operationKey: "GET /api/security/users/{id}",
        schemaRef: "openapi:security.users:v0.1",
        compatibility: "backward_compatible",
        visibility: "internal",
        status: "active",
        evidence: {reason: "Funcionario referencia usuario de acesso para permissoes."}
      },
      {
        contractKey: "contract:vehicle-allocation-changed:v0.1",
        contractType: "event_schema",
        providerSourceKey: "source:assets",
        providerContextKey: "assets.veiculos",
        consumerContextKey: "operations.missoes",
        resourceKey: "assets.vehicle-allocation",
        operationKey: "VehicleAllocationChanged",
        schemaRef: "asyncapi:assets.vehicle-allocation:v0.1",
        compatibility: "backward_compatible",
        visibility: "internal",
        status: "active",
        evidence: {reason: "Missoes consomem disponibilidade de veiculos."}
      }
    ],
    contextRelationships: [
      {
        relationshipKey: "relationship:funcionarios-references-usuarios",
        sourceContextKey: "human-resources.funcionarios",
        targetContextKey: "security.usuarios",
        relationshipType: "references",
        contractKey: "contract:security-user-lookup:v0.1",
        direction: "source_to_target",
        ownership: "source_owned",
        confidence: 0.96,
        status: "active",
        evidence: {join: "funcionario.usuarioId -> usuario.id"}
      },
      {
        relationshipKey: "relationship:missoes-depends-on-veiculos",
        sourceContextKey: "operations.missoes",
        targetContextKey: "assets.veiculos",
        relationshipType: "depends_on",
        contractKey: "contract:vehicle-allocation-changed:v0.1",
        direction: "source_to_target",
        ownership: "target_owned",
        confidence: 0.91,
        status: "active",
        evidence: {event: "VehicleAllocationChanged"}
      }
    ],
    resolutions: [
      {
        resolutionKey: "resolution:funcionario-same-as-employee",
        sourceConceptKey: "human-resources.funcionarios.funcionario",
        targetConceptKey: "human-resources.funcionarios.employee",
        sourceContextKey: "human-resources.funcionarios",
        targetContextKey: "human-resources.funcionarios",
        resolutionType: "same_as",
        confidence: 0.99,
        status: "approved",
        reviewOwner: "RH",
        evidence: {reason: "Sinonimos usados por equipes tecnica e negocio."}
      },
      {
        resolutionKey: "resolution:usuario-maps-to-principal",
        sourceConceptKey: "security.usuarios.usuario",
        targetConceptKey: "security.usuarios.principal",
        sourceContextKey: "security.usuarios",
        targetContextKey: "security.usuarios",
        resolutionType: "maps_to",
        confidence: 0.87,
        status: "review_required",
        reviewOwner: "Seguranca",
        evidence: {reason: "Principal pode representar usuario humano ou integracao."}
      }
    ]
  }' > "$payload_file"

echo
echo "Running federation dry-run validation."
if ! post_json "/api/praxis/config/domain-federation/ingest?dryRun=true" "$payload_file" "$dry_run_response"; then
  echo "Could not run federation dry-run. Verify origin policy, runtime route and V21 availability." >&2
  exit 1
fi

dry_run_valid="$(jq -r '.valid // false' "$dry_run_response")"
dry_run_errors="$(jq -r '.validation.errorCount // -1' "$dry_run_response")"
dry_run_warnings="$(jq -r '.validation.warningCount // -1' "$dry_run_response")"
dry_run_preview_count="$(jq -r '.previewCount // -1' "$dry_run_response")"

if [[ "$dry_run_valid" != "true" || "$dry_run_errors" -ne 0 || "$dry_run_warnings" -ne 0 || "$dry_run_preview_count" -ne 4 ]]; then
  echo "Invalid federation dry-run response." >&2
  jq '{valid, validation, previewCount}' "$dry_run_response" >&2
  exit 1
fi

jq '{status: "dry-run-valid", valid, errorCount: .validation.errorCount, warningCount: .validation.warningCount, previewCount}' "$dry_run_response"

echo
echo "Persisting federation candidate release."
if ! post_json "/api/praxis/config/domain-federation/ingest?dryRun=false" "$payload_file" "$ingest_response"; then
  echo "Could not persist federation release. Verify praxis.domain-federation.persistence.enabled=true." >&2
  exit 1
fi

release_key="$(jq -r '.releaseKey // empty' "$ingest_response")"
release_status="$(jq -r '.status // empty' "$ingest_response")"
ingest_valid="$(jq -r '.valid // false' "$ingest_response")"

if [[ -z "$release_key" || "$release_status" != "candidate" || "$ingest_valid" != "true" ]]; then
  echo "Invalid federation ingest response." >&2
  jq '{valid, releaseKey, status, persistedCounts, validation}' "$ingest_response" >&2
  exit 1
fi

jq '{
  status: "candidate-created",
  releaseKey,
  payloadHash,
  persistedCounts,
  validation
}' "$ingest_response"

encoded_release_key="$(urlencode "$release_key")"

echo
echo "Reading candidate release audit entry."
get_json "/api/praxis/config/domain-federation/releases?status=candidate&limit=10" "$candidate_list_response"
candidate_found="$(jq --arg releaseKey "$release_key" '[.[] | select(.releaseKey == $releaseKey and .status == "candidate")] | length' "$candidate_list_response")"
if [[ "$candidate_found" -eq 0 ]]; then
  echo "Candidate release was not found in audit list." >&2
  jq --arg releaseKey "$release_key" '{releaseKey: $releaseKey, releases: [.[] | {releaseKey, status, tenantId, environment}]}' "$candidate_list_response" >&2
  exit 1
fi

jq --arg releaseKey "$release_key" '{
  status: "candidate-audited",
  matchingReleaseCount: ([.[] | select(.releaseKey == $releaseKey)] | length),
  latest: .[0] | {releaseKey, status, tenantId, environment, createdAt}
}' "$candidate_list_response"

echo
echo "Reading persisted validation report."
get_json "/api/praxis/config/domain-federation/releases/${encoded_release_key}/validation" "$validation_response"
validation_valid="$(jq -r '.validationReport.valid // false' "$validation_response")"
validation_errors="$(jq -r '.validationReport.errorCount // -1' "$validation_response")"
validation_warnings="$(jq -r '.validationReport.warningCount // -1' "$validation_response")"
if [[ "$validation_valid" != "true" || "$validation_errors" -ne 0 || "$validation_warnings" -ne 0 ]]; then
  echo "Invalid persisted validation report." >&2
  jq '{releaseKey, status, validationReport}' "$validation_response" >&2
  exit 1
fi

jq '{status: "validation-audited", releaseKey, releaseStatus: .status, validationReport}' "$validation_response"

echo
echo "Activating federation release."
curl -fsS -X POST "${BACKEND_URL%/}/api/praxis/config/domain-federation/releases/${encoded_release_key}/activate" \
  -H "Origin: ${ORIGIN}" \
  -H "X-Tenant-ID: ${TENANT_ID}" \
  -H "X-Env: ${ENVIRONMENT}" \
  -o "$activation_response"

activated_status="$(jq -r '.status // empty' "$activation_response")"
activated_at="$(jq -r '.activatedAt // empty' "$activation_response")"
if [[ "$activated_status" != "active" || -z "$activated_at" ]]; then
  echo "Invalid activation response." >&2
  jq '{releaseKey, status, activatedAt}' "$activation_response" >&2
  exit 1
fi

jq '{status: "release-activated", releaseKey, releaseStatus: .status, activatedAt}' "$activation_response"

echo
echo "Reading active release audit entry."
get_json "/api/praxis/config/domain-federation/releases?status=active&limit=10" "$active_list_response"
active_found="$(jq --arg releaseKey "$release_key" '[.[] | select(.releaseKey == $releaseKey and .status == "active")] | length' "$active_list_response")"
if [[ "$active_found" -eq 0 ]]; then
  echo "Activated release was not found in active release list." >&2
  jq --arg releaseKey "$release_key" '{releaseKey: $releaseKey, releases: [.[] | {releaseKey, status, tenantId, environment, activatedAt}]}' "$active_list_response" >&2
  exit 1
fi

jq --arg releaseKey "$release_key" '{
  status: "federation-runtime-ready",
  releaseKey: $releaseKey,
  activeReleaseCount: ([.[] | select(.status == "active")] | length),
  latest: .[0] | {releaseKey, status, tenantId, environment, activatedAt}
}' "$active_list_response"

echo
echo "Reading active federated context with contracts and resolutions."
get_json "/api/praxis/config/domain-federation/context?contextKey=operations.missoes&relationshipType=depends_on&limit=20&policyProfile=authoring" "$context_operations_response"
ops_release_key="$(jq -r '.context.release.releaseKey // empty' "$context_operations_response")"
ops_contract_count="$(jq -r '(.contracts // []) | length' "$context_operations_response")"
ops_resolution_count="$(jq -r '(.resolutions // []) | length' "$context_operations_response")"
ops_relationship_count="$(jq -r '(.relationships // []) | length' "$context_operations_response")"
if [[ "$ops_release_key" != "$release_key" || "$ops_relationship_count" -lt 1 || "$ops_contract_count" -lt 1 ]]; then
  echo "Operations federated context did not expose the active release artifacts." >&2
  jq '{releaseKey: .context.release.releaseKey, relationshipCount: ((.relationships // []) | length), contractCount: ((.contracts // []) | length), resolutionCount: ((.resolutions // []) | length)}' "$context_operations_response" >&2
  exit 1
fi

jq '{
  status: "context-operations-ready",
  releaseKey: .context.release.releaseKey,
  contextCount: ((.context.items // []) | length),
  relationshipCount: ((.relationships // []) | length),
  contractCount: ((.contracts // []) | length),
  resolutionCount: ((.resolutions // []) | length),
  firstContract: (.contracts[0].itemKey // null)
}' "$context_operations_response"

echo
get_json "/api/praxis/config/domain-federation/context?contextKey=human-resources.funcionarios&relationshipType=references&limit=20&policyProfile=authoring" "$context_hr_response"
hr_release_key="$(jq -r '.context.release.releaseKey // empty' "$context_hr_response")"
hr_contract_count="$(jq -r '(.contracts // []) | length' "$context_hr_response")"
hr_resolution_count="$(jq -r '(.resolutions // []) | length' "$context_hr_response")"
if [[ "$hr_release_key" != "$release_key" || "$hr_contract_count" -lt 1 || "$hr_resolution_count" -lt 1 ]]; then
  echo "HR federated context did not expose contracts and resolutions from the active release." >&2
  jq '{releaseKey: .context.release.releaseKey, contractCount: ((.contracts // []) | length), resolutionCount: ((.resolutions // []) | length), firstResolution: (.resolutions[0].itemKey // null)}' "$context_hr_response" >&2
  exit 1
fi

jq '{
  status: "context-hr-ready",
  releaseKey: .context.release.releaseKey,
  contextCount: ((.context.items // []) | length),
  relationshipCount: ((.relationships // []) | length),
  contractCount: ((.contracts // []) | length),
  resolutionCount: ((.resolutions // []) | length),
  firstContract: (.contracts[0].itemKey // null),
  firstResolution: (.resolutions[0].itemKey // null)
}' "$context_hr_response"

echo
echo "Domain federation runtime verification completed."
