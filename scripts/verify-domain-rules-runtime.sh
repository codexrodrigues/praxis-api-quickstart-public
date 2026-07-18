#!/usr/bin/env bash
set -euo pipefail

BACKEND_URL="${BACKEND_URL:-http://localhost:8088}"
TENANT_ID="${TENANT_ID:-default}"
ENVIRONMENT="${ENVIRONMENT:-dev}"
ORIGIN="${ORIGIN:-https://praxisui-dev.web.app}"
SERVICE_KEY="${SERVICE_KEY:-praxis-api-quickstart}"
CONTEXT_KEY="${CONTEXT_KEY:-human-resources}"
RESOURCE_KEY="${RESOURCE_KEY:-human-resources.funcionarios}"
SMOKE_RUN_ID="${SMOKE_RUN_ID:-$(date -u +%Y%m%d%H%M%S)}"
RULE_KEY="${RULE_KEY:-human-resources.funcionarios.rule.lgpd-cpf-guidance.${SMOKE_RUN_ID}}"
MATERIALIZATION_KEY="${MATERIALIZATION_KEY:-funcionarios-form-demo:formRules:lgpd-cpf-guidance:${SMOKE_RUN_ID}}"
TARGET_ARTIFACT_KEY="${TARGET_ARTIFACT_KEY:-funcionarios-form-demo}"
TARGET_RELEASE_KEY="${TARGET_RELEASE_KEY:-page-builder-demo@local}"
REQUIRE_SIMULATION="${REQUIRE_SIMULATION:-auto}"
REQUIRE_PUBLICATION="${REQUIRE_PUBLICATION:-auto}"
REQUIRE_BACKEND_VALIDATION="${REQUIRE_BACKEND_VALIDATION:-auto}"
REQUIRE_WORKFLOW_ACTION="${REQUIRE_WORKFLOW_ACTION:-auto}"
REQUIRE_APPROVAL_POLICY="${REQUIRE_APPROVAL_POLICY:-auto}"
REQUIRE_TIMELINE="${REQUIRE_TIMELINE:-auto}"
ADMIN_USERNAME="${ADMIN_USERNAME:-admin}"
ADMIN_PASSWORD="${ADMIN_PASSWORD:-${PRACTICE_TEMP_PASSWORD:-}}"
PUBLICATION_TENANT_ID="${PUBLICATION_TENANT_ID:-domain-rules-publication-smoke-${SMOKE_RUN_ID}}"
PUBLICATION_ENVIRONMENT="${PUBLICATION_ENVIRONMENT:-${ENVIRONMENT}}"
PUBLICATION_CONTEXT_KEY="${PUBLICATION_CONTEXT_KEY:-procurement}"
PUBLICATION_RESOURCE_KEY="${PUBLICATION_RESOURCE_KEY:-procurement.suppliers}"
PUBLICATION_RULE_KEY="${PUBLICATION_RULE_KEY:-procurement.suppliers.rule.selection-eligibility.publication.${SMOKE_RUN_ID}}"
PUBLICATION_OPTION_SOURCE_KEY="${PUBLICATION_OPTION_SOURCE_KEY:-supplier}"
PUBLICATION_RUNTIME_PROBE_BLOCKED_STATUSES_JSON="${PUBLICATION_RUNTIME_PROBE_BLOCKED_STATUSES_JSON:-[\"ACTIVE\"]}"
BACKEND_VALIDATION_RESOURCE_KEY="${BACKEND_VALIDATION_RESOURCE_KEY:-procurement.purchase-orders}"
BACKEND_VALIDATION_RULE_KEY="${BACKEND_VALIDATION_RULE_KEY:-procurement.purchase-orders.rule.supplier-backend-validation.${SMOKE_RUN_ID}}"
BACKEND_VALIDATION_BLOCKED_STATUSES_JSON="${BACKEND_VALIDATION_BLOCKED_STATUSES_JSON:-[\"BLOCKED\"]}"
BACKEND_VALIDATION_SUPPLIER_ID="${BACKEND_VALIDATION_SUPPLIER_ID:-11}"
BACKEND_VALIDATION_COMPANY_ID="${BACKEND_VALIDATION_COMPANY_ID:-1}"
BACKEND_VALIDATION_PRODUCT_ID="${BACKEND_VALIDATION_PRODUCT_ID:-30}"
WORKFLOW_ACTION_RESOURCE_KEY="${WORKFLOW_ACTION_RESOURCE_KEY:-human-resources.folhas-pagamento}"
WORKFLOW_ACTION_CONTEXT_KEY="${WORKFLOW_ACTION_CONTEXT_KEY:-human-resources}"
WORKFLOW_ACTION_ID="${WORKFLOW_ACTION_ID:-mark-paid}"
WORKFLOW_ACTION_TARGET_KEY="${WORKFLOW_ACTION_TARGET_KEY:-${WORKFLOW_ACTION_RESOURCE_KEY}:${WORKFLOW_ACTION_ID}}"
WORKFLOW_ACTION_RULE_KEY="${WORKFLOW_ACTION_RULE_KEY:-human-resources.folhas-pagamento.rule.mark-paid-compliance.${SMOKE_RUN_ID}}"
WORKFLOW_ACTION_RESOURCE_ID="${WORKFLOW_ACTION_RESOURCE_ID:-${WORKFLOW_ACTION_FOLHA_ID:-2}}"
WORKFLOW_ACTION_RESOURCE_ID_LABEL="${WORKFLOW_ACTION_RESOURCE_ID_LABEL:-resourceId}"
WORKFLOW_ACTION_COMMAND_PATH="${WORKFLOW_ACTION_COMMAND_PATH:-/api/human-resources/folhas-pagamento/${WORKFLOW_ACTION_RESOURCE_ID}/actions/${WORKFLOW_ACTION_ID}}"
WORKFLOW_ACTION_BLOCKED_STATES_JSON="${WORKFLOW_ACTION_BLOCKED_STATES_JSON:-[\"PROGRAMADA\"]}"
WORKFLOW_ACTION_POLICY_SUMMARY="${WORKFLOW_ACTION_POLICY_SUMMARY:-Bloquear a action mark-paid enquanto a folha programada aguarda revisao governada de compliance.}"
WORKFLOW_ACTION_POLICY_MESSAGE="${WORKFLOW_ACTION_POLICY_MESSAGE:-Pagamento bloqueado por decisao governada ate revisao de compliance.}"
WORKFLOW_ACTION_SEMANTIC_OWNER="${WORKFLOW_ACTION_SEMANTIC_OWNER:-hr-operations-owner}"
WORKFLOW_ACTION_STEWARD="${WORKFLOW_ACTION_STEWARD:-payroll-compliance}"
APPROVAL_POLICY_RESOURCE_KEY="${APPROVAL_POLICY_RESOURCE_KEY:-human-resources.eventos-folha}"
APPROVAL_POLICY_ACTION_ID="${APPROVAL_POLICY_ACTION_ID:-bulk-approve}"
APPROVAL_POLICY_TARGET_KEY="${APPROVAL_POLICY_TARGET_KEY:-${APPROVAL_POLICY_RESOURCE_KEY}:${APPROVAL_POLICY_ACTION_ID}}"
APPROVAL_POLICY_RULE_KEY="${APPROVAL_POLICY_RULE_KEY:-human-resources.eventos-folha.rule.bulk-approve-approval.${SMOKE_RUN_ID}}"
APPROVAL_POLICY_EVENT_ID="${APPROVAL_POLICY_EVENT_ID:-1}"
AUTHOR_USER_ID="${AUTHOR_USER_ID:-domain-rule-smoke-author}"
REVIEWER_USER_ID="${REVIEWER_USER_ID:-domain-rule-smoke-reviewer}"

if [[ "$AUTHOR_USER_ID" == "$REVIEWER_USER_ID" ]]; then
  echo "AUTHOR_USER_ID and REVIEWER_USER_ID must be distinct for maker-checker validation." >&2
  exit 2
fi

usage() {
  cat >&2 <<'USAGE'
Usage:
  scripts/verify-domain-rules-runtime.sh

Examples:
  BACKEND_URL=http://localhost:8088 scripts/verify-domain-rules-runtime.sh
  BACKEND_URL=https://praxis-api-quickstart.onrender.com scripts/verify-domain-rules-runtime.sh

The script first tries to simulate a governed LGPD rule draft through the
shared rule APIs. When the runtime already exposes
POST /api/praxis/config/domain-rules/simulations, the script validates the
simulation result before creating the persisted definition and the derived
FormConfig materialization. It can also validate governed publication through
POST /api/praxis/config/domain-rules/publications with a procurement
selection_eligibility rule that should materialize to option_source.

Requirements:
  - runtime with /api/praxis/config/domain-rules/** available;
  - config database migrated through V20__create_domain_shared_rule_layer.sql;
  - no Flyway action is executed by this script.

Simulation compatibility:
  - REQUIRE_SIMULATION=auto  -> validate simulation when the endpoint exists;
                                continue with a warning when the runtime still
                                returns 404/405 for /simulations
  - REQUIRE_SIMULATION=true  -> fail when the simulation endpoint is absent
  - REQUIRE_SIMULATION=false -> skip simulation on purpose

Publication compatibility:
  - REQUIRE_PUBLICATION=auto  -> validate publication when the endpoint exists;
                                 continue with a warning when the runtime still
                                 returns 404/405 for /publications
  - REQUIRE_PUBLICATION=true  -> fail when the publication endpoint is absent
  - REQUIRE_PUBLICATION=false -> skip publication on purpose

Backend validation compatibility:
  - REQUIRE_BACKEND_VALIDATION=auto  -> validate backend_validation when the
                                        publication derives a resource validation
                                        materialization and the command endpoint
                                        can be exercised
  - REQUIRE_BACKEND_VALIDATION=true  -> fail when backend_validation cannot be
                                        derived or exercised
  - REQUIRE_BACKEND_VALIDATION=false -> skip backend_validation on purpose

Workflow action compatibility:
  - REQUIRE_WORKFLOW_ACTION=auto  -> validate workflow_action when the
                                     publication derives a resource workflow
                                     action materialization and the action
                                     endpoint can be exercised
  - REQUIRE_WORKFLOW_ACTION=true  -> fail when workflow_action cannot be
                                     derived or exercised
  - REQUIRE_WORKFLOW_ACTION=false -> skip workflow_action on purpose

Approval policy compatibility:
  - REQUIRE_APPROVAL_POLICY=auto  -> validate approval_policy when the
                                     publication derives a resource action
                                     approval materialization and the action
                                     endpoint can be exercised
  - REQUIRE_APPROVAL_POLICY=true  -> fail when approval_policy cannot be
                                     derived or exercised
  - REQUIRE_APPROVAL_POLICY=false -> skip approval_policy on purpose

Governed timeline compatibility:
  - REQUIRE_TIMELINE=auto  -> validate definition timeline when the endpoint
                              exists; continue with a warning on older runtimes
  - REQUIRE_TIMELINE=true  -> fail when the timeline endpoint is absent or
                              does not expose safe lifecycle events; when the
                              governed approval path runs from intake, also
                              require persisted intake.received,
                              simulation.requested, simulation.completed,
                              approval.requested and approval.completed; when
                              the publication path runs, also require persisted
                              publication.requested and publication.completed
  - REQUIRE_TIMELINE=false -> skip timeline validation on purpose

Authenticated command probe:
  - ADMIN_USERNAME defaults to admin.
  - ADMIN_PASSWORD or PRACTICE_TEMP_PASSWORD is required when
    REQUIRE_BACKEND_VALIDATION=true, REQUIRE_WORKFLOW_ACTION=true or
    REQUIRE_APPROVAL_POLICY=true reaches the mutable command probe, because
    these endpoints are protected.

Use SMOKE_RUN_ID, RULE_KEY, MATERIALIZATION_KEY, PUBLICATION_TENANT_ID,
PUBLICATION_RULE_KEY or PUBLICATION_OPTION_SOURCE_KEY to make repeated runs
deterministic. By default, the script appends a UTC timestamp to rule keys and
also isolates the publication flow in a tenant derived from SMOKE_RUN_ID to
avoid colliding with previous persisted smoke records.
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
    -H "X-User-ID: ${AUTHOR_USER_ID}" \
    -H "Content-Type: application/json" \
    --data-binary "@${input_file}" \
    -o "$output_file"
}

post_json_scoped() {
  local path="$1"
  local input_file="$2"
  local output_file="$3"
  local tenant_id="$4"
  local environment="$5"

  curl -fsS "${BACKEND_URL%/}${path}" \
    -H "Origin: ${ORIGIN}" \
    -H "X-Tenant-ID: ${tenant_id}" \
    -H "X-Env: ${environment}" \
    -H "X-User-ID: ${AUTHOR_USER_ID}" \
    -H "Content-Type: application/json" \
    --data-binary "@${input_file}" \
    -o "$output_file"
}

patch_json() {
  local path="$1"
  local input_file="$2"
  local output_file="$3"

  curl -fsS -X PATCH "${BACKEND_URL%/}${path}" \
    -H "Origin: ${ORIGIN}" \
    -H "X-Tenant-ID: ${TENANT_ID}" \
    -H "X-Env: ${ENVIRONMENT}" \
    -H "X-User-ID: ${AUTHOR_USER_ID}" \
    -H "Content-Type: application/json" \
    --data-binary "@${input_file}" \
    -o "$output_file"
}

patch_json_as_reviewer() {
  local path="$1"
  local input_file="$2"
  local output_file="$3"

  curl -fsS -X PATCH "${BACKEND_URL%/}${path}" \
    -H "Origin: ${ORIGIN}" \
    -H "X-Tenant-ID: ${TENANT_ID}" \
    -H "X-Env: ${ENVIRONMENT}" \
    -H "X-User-ID: ${REVIEWER_USER_ID}" \
    -H "Content-Type: application/json" \
    --data-binary "@${input_file}" \
    -o "$output_file"
}

post_json_allow_status() {
  local path="$1"
  local input_file="$2"
  local output_file="$3"
  local status_file="$4"

  curl -sS "${BACKEND_URL%/}${path}" \
    -H "Origin: ${ORIGIN}" \
    -H "X-Tenant-ID: ${TENANT_ID}" \
    -H "X-Env: ${ENVIRONMENT}" \
    -H "Content-Type: application/json" \
    --data-binary "@${input_file}" \
    -o "$output_file" \
    -w '%{http_code}' > "$status_file"
}

post_json_allow_status_scoped() {
  local path="$1"
  local input_file="$2"
  local output_file="$3"
  local status_file="$4"
  local tenant_id="$5"
  local environment="$6"

  curl -sS "${BACKEND_URL%/}${path}" \
    -H "Origin: ${ORIGIN}" \
    -H "X-Tenant-ID: ${tenant_id}" \
    -H "X-Env: ${environment}" \
    -H "Content-Type: application/json" \
    --data-binary "@${input_file}" \
    -o "$output_file" \
    -w '%{http_code}' > "$status_file"
}

post_json_allow_status_scoped_authenticated() {
  local path="$1"
  local input_file="$2"
  local output_file="$3"
  local status_file="$4"
  local tenant_id="$5"
  local environment="$6"
  local csrf_args=()

  if [[ -n "${AUTH_CSRF_TOKEN:-}" ]]; then
    csrf_args=(-H "X-XSRF-TOKEN: ${AUTH_CSRF_TOKEN}")
  fi

  curl -sS "${BACKEND_URL%/}${path}" \
    -b "$auth_cookie_jar" \
    -c "$auth_cookie_jar" \
    -H "Origin: ${ORIGIN}" \
    -H "X-Tenant-ID: ${tenant_id}" \
    -H "X-Env: ${environment}" \
    -H "Content-Type: application/json" \
    ${csrf_args[@]+"${csrf_args[@]}"} \
    --data-binary "@${input_file}" \
    -o "$output_file" \
    -w '%{http_code}' > "$status_file"
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

get_json_scoped() {
  local path="$1"
  local output_file="$2"
  local tenant_id="$3"
  local environment="$4"

  curl -fsS "${BACKEND_URL%/}${path}" \
    -H "Origin: ${ORIGIN}" \
    -H "X-Tenant-ID: ${tenant_id}" \
    -H "X-Env: ${environment}" \
    -o "$output_file"
}

get_json_allow_status_scoped() {
  local path="$1"
  local output_file="$2"
  local status_file="$3"
  local tenant_id="$4"
  local environment="$5"

  curl -sS "${BACKEND_URL%/}${path}" \
    -H "Origin: ${ORIGIN}" \
    -H "X-Tenant-ID: ${tenant_id}" \
    -H "X-Env: ${environment}" \
    -o "$output_file" \
    -w '%{http_code}' > "$status_file"
}

validate_definition_timeline() {
  local definition_id="$1"
  local tenant_id="$2"
  local environment="$3"
  local label="$4"
  local expected_target_layer="${5:-}"
  local expected_target_key="${6:-}"
  local require_publication_events="${7:-false}"
  local require_approval_events="${8:-false}"
  local require_intake_simulation_events="${9:-false}"

  if [[ "$REQUIRE_TIMELINE" == "false" ]]; then
    return 0
  fi

  echo
  echo "Validating governed timeline for ${label} definition ${definition_id}."
  get_json_allow_status_scoped \
    "/api/praxis/config/domain-rules/definitions/${definition_id}/timeline" \
    "$timeline_response" \
    "$timeline_status_file" \
    "$tenant_id" \
    "$environment"
  timeline_status="$(cat "$timeline_status_file")"

  if [[ "$timeline_status" == "404" || "$timeline_status" == "405" ]]; then
    if [[ "$REQUIRE_TIMELINE" == "true" ]]; then
      echo "Runtime does not expose /api/praxis/config/domain-rules/definitions/{definitionId}/timeline yet." >&2
      cat "$timeline_response" >&2 || true
      exit 1
    fi
    echo "Warning: runtime does not expose domain rule timeline yet; continuing without timeline proof." >&2
    return 0
  fi

  if [[ "$timeline_status" != "200" ]]; then
    echo "Unexpected domain rule timeline response (HTTP ${timeline_status})." >&2
    cat "$timeline_response" >&2
    exit 1
  fi

  timeline_definition_id="$(jq -r '.ruleDefinitionId // empty' "$timeline_response")"
  timeline_event_count="$(jq '(.events // []) | length' "$timeline_response")"
  timeline_created_count="$(jq '[.events[]? | select((.eventType // "") == "definition.created") | select((.visibility // "") == "safe")] | length' "$timeline_response")"
  timeline_unsafe_count="$(jq '[.events[]? | select((.visibility // "") != "safe")] | length' "$timeline_response")"
  timeline_leak_count="$(jq '[
    (paths as $path | ($path | map(tostring) | join("."))),
    (paths(scalars) as $path | getpath($path) | tostring)
    | select(test("sourcePrompt|assistantMessage|condition|parameters|materializedPayload|raw prompt|assistant draft|sensitive operational message|CPF e dado pessoal regulado"))
  ] | length' "$timeline_response")"

  if [[ "$timeline_definition_id" != "$definition_id" || "$timeline_event_count" -eq 0 || "$timeline_created_count" -eq 0 || "$timeline_unsafe_count" -ne 0 || "$timeline_leak_count" -ne 0 ]]; then
    echo "Invalid governed timeline response for ${label}." >&2
    jq '{ruleDefinitionId, ruleKey, ruleType, resourceKey, events}' "$timeline_response" >&2
    exit 1
  fi

  if [[ -n "$expected_target_layer" || -n "$expected_target_key" ]]; then
    timeline_materialization_count="$(jq \
      --arg targetLayer "$expected_target_layer" \
      --arg targetKey "$expected_target_key" \
      '[
        .events[]?
        | select((.eventType // "") == "materialization.applied")
        | select($targetLayer == "" or (.targetLayer // "") == $targetLayer)
        | select($targetKey == "" or (.targetArtifactKey // "") == $targetKey)
        | select((.sourceHash // "") != "")
        | select((.visibility // "") == "safe")
      ] | length' "$timeline_response")"
    if [[ "$timeline_materialization_count" -eq 0 ]]; then
      echo "Governed timeline did not expose the expected materialization event for ${label}." >&2
      jq --arg targetLayer "$expected_target_layer" --arg targetKey "$expected_target_key" \
        '{expectedTargetLayer: $targetLayer, expectedTargetKey: $targetKey, events}' "$timeline_response" >&2
      exit 1
    fi
  fi

  if [[ "$require_publication_events" == "true" ]]; then
    timeline_publication_requested_count="$(jq '[.events[]? | select((.eventType // "") == "publication.requested") | select((.visibility // "") == "safe")] | length' "$timeline_response")"
    timeline_publication_completed_count="$(jq '[.events[]? | select((.eventType // "") == "publication.completed") | select((.visibility // "") == "safe")] | length' "$timeline_response")"
    if [[ "$timeline_publication_requested_count" -eq 0 || "$timeline_publication_completed_count" -eq 0 ]]; then
      if [[ "$REQUIRE_TIMELINE" == "true" ]]; then
        echo "Governed timeline did not expose the expected publication events for ${label}." >&2
        jq '{ruleDefinitionId, ruleKey, ruleType, resourceKey, events}' "$timeline_response" >&2
        exit 1
      fi
      echo "Warning: governed timeline does not expose publication events for ${label} yet." >&2
    fi
  fi

  if [[ "$require_approval_events" == "true" ]]; then
    timeline_approval_requested_count="$(jq '[.events[]? | select((.eventType // "") == "approval.requested") | select((.visibility // "") == "safe")] | length' "$timeline_response")"
    timeline_approval_completed_count="$(jq '[.events[]? | select((.eventType // "") == "approval.completed") | select((.visibility // "") == "safe")] | length' "$timeline_response")"
    if [[ "$timeline_approval_requested_count" -eq 0 || "$timeline_approval_completed_count" -eq 0 ]]; then
      if [[ "$REQUIRE_TIMELINE" == "true" ]]; then
        echo "Governed timeline did not expose the expected approval events for ${label}." >&2
        jq '{ruleDefinitionId, ruleKey, ruleType, resourceKey, events}' "$timeline_response" >&2
        exit 1
      fi
      echo "Warning: governed timeline does not expose approval events for ${label} yet." >&2
    fi
  fi

  if [[ "$require_intake_simulation_events" == "true" ]]; then
    timeline_intake_count="$(jq '[.events[]? | select((.eventType // "") == "intake.received") | select((.visibility // "") == "safe")] | length' "$timeline_response")"
    timeline_simulation_requested_count="$(jq '[.events[]? | select((.eventType // "") == "simulation.requested") | select((.visibility // "") == "safe")] | length' "$timeline_response")"
    timeline_simulation_completed_count="$(jq '[.events[]? | select((.eventType // "") == "simulation.completed") | select((.visibility // "") == "safe")] | length' "$timeline_response")"
    if [[ "$timeline_intake_count" -eq 0 || "$timeline_simulation_requested_count" -eq 0 || "$timeline_simulation_completed_count" -eq 0 ]]; then
      if [[ "$REQUIRE_TIMELINE" == "true" ]]; then
        echo "Governed timeline did not expose the expected intake/simulation events for ${label}." >&2
        jq '{ruleDefinitionId, ruleKey, ruleType, resourceKey, events}' "$timeline_response" >&2
        exit 1
      fi
      echo "Warning: governed timeline does not expose intake/simulation events for ${label} yet." >&2
    fi
  fi

  jq -n \
    --arg status "domain-rule-timeline-ready" \
    --arg label "$label" \
    --arg definitionId "$definition_id" \
    --arg tenantId "$tenant_id" \
    --argjson eventCount "$timeline_event_count" \
    '{status: $status, label: $label, definitionId: $definitionId, tenantId: $tenantId, eventCount: $eventCount}'
}

authenticate_command_probe() {
  local login_request="$1"
  local login_status_file="$2"
  local session_response_file="$3"

  if [[ -z "$ADMIN_PASSWORD" ]]; then
    AUTH_FAILURE_REASON="missing-admin-password"
    export AUTH_FAILURE_REASON
    return 1
  fi

  jq -n \
    --arg username "$ADMIN_USERNAME" \
    --arg password "$ADMIN_PASSWORD" \
    '{username: $username, password: $password}' > "$login_request"

  curl -sS "${BACKEND_URL%/}/auth/login" \
    -c "$auth_cookie_jar" \
    -H "Origin: ${ORIGIN}" \
    -H "Content-Type: application/json" \
    --data-binary "@${login_request}" \
    -o /dev/null \
    -w '%{http_code}' > "$login_status_file"

  login_status="$(cat "$login_status_file")"
  if [[ "$login_status" != "200" && "$login_status" != "204" ]]; then
    AUTH_FAILURE_REASON="login-http-${login_status}"
    export AUTH_FAILURE_REASON
    return 1
  fi

  curl -sS "${BACKEND_URL%/}/auth/session" \
    -b "$auth_cookie_jar" \
    -c "$auth_cookie_jar" \
    -H "Origin: ${ORIGIN}" \
    -o "$session_response_file" \
    -w '%{http_code}' >/dev/null || true

  AUTH_CSRF_TOKEN="$(awk '$6 == "XSRF-TOKEN" { token = $7 } END { print token }' "$auth_cookie_jar")"
  export AUTH_CSRF_TOKEN
  AUTH_FAILURE_REASON=""
  export AUTH_FAILURE_REASON
  return 0
}

urlencode() {
  jq -nr --arg value "$1" '$value|@uri'
}

if [[ "${1:-}" == "-h" || "${1:-}" == "--help" ]]; then
  usage
  exit 0
fi

if [[ "$REQUIRE_WORKFLOW_ACTION" == "true" && "$REQUIRE_PUBLICATION" == "false" ]]; then
  echo "REQUIRE_WORKFLOW_ACTION=true requires REQUIRE_PUBLICATION=auto or true because workflow_action is derived by /domain-rules/publications." >&2
  exit 2
fi

if [[ "$REQUIRE_APPROVAL_POLICY" == "true" && "$REQUIRE_PUBLICATION" == "false" ]]; then
  echo "REQUIRE_APPROVAL_POLICY=true requires REQUIRE_PUBLICATION=auto or true because approval_policy is derived by /domain-rules/publications." >&2
  exit 2
fi

for command in curl jq; do
  if ! command -v "$command" >/dev/null 2>&1; then
    echo "Missing required command: $command" >&2
    exit 2
  fi
done

intake_request="$(mktemp "${TMPDIR:-/tmp}/praxis-rule-intake-request.XXXXXX.json")"
intake_response="$(mktemp "${TMPDIR:-/tmp}/praxis-rule-intake-response.XXXXXX.json")"
simulation_request="$(mktemp "${TMPDIR:-/tmp}/praxis-rule-simulation-request.XXXXXX.json")"
simulation_response="$(mktemp "${TMPDIR:-/tmp}/praxis-rule-simulation-response.XXXXXX.json")"
simulation_status_file="$(mktemp "${TMPDIR:-/tmp}/praxis-rule-simulation-status.XXXXXX.txt")"
materialization_request="$(mktemp "${TMPDIR:-/tmp}/praxis-rule-materialization-request.XXXXXX.json")"
materialization_response="$(mktemp "${TMPDIR:-/tmp}/praxis-rule-materialization-response.XXXXXX.json")"
definition_transition_request="$(mktemp "${TMPDIR:-/tmp}/praxis-rule-definition-transition-request.XXXXXX.json")"
definition_transition_response="$(mktemp "${TMPDIR:-/tmp}/praxis-rule-definition-transition-response.XXXXXX.json")"
materialization_transition_request="$(mktemp "${TMPDIR:-/tmp}/praxis-rule-materialization-transition-request.XXXXXX.json")"
materialization_transition_response="$(mktemp "${TMPDIR:-/tmp}/praxis-rule-materialization-transition-response.XXXXXX.json")"
publication_definition_request="$(mktemp "${TMPDIR:-/tmp}/praxis-rule-publication-definition-request.XXXXXX.json")"
publication_definition_response="$(mktemp "${TMPDIR:-/tmp}/praxis-rule-publication-definition-response.XXXXXX.json")"
publication_request="$(mktemp "${TMPDIR:-/tmp}/praxis-rule-publication-request.XXXXXX.json")"
publication_response="$(mktemp "${TMPDIR:-/tmp}/praxis-rule-publication-response.XXXXXX.json")"
publication_status_file="$(mktemp "${TMPDIR:-/tmp}/praxis-rule-publication-status.XXXXXX.txt")"
publication_option_runtime_request="$(mktemp "${TMPDIR:-/tmp}/praxis-rule-publication-option-runtime-request.XXXXXX.json")"
publication_option_runtime_response="$(mktemp "${TMPDIR:-/tmp}/praxis-rule-publication-option-runtime-response.XXXXXX.json")"
backend_validation_definition_request="$(mktemp "${TMPDIR:-/tmp}/praxis-rule-backend-validation-definition-request.XXXXXX.json")"
backend_validation_definition_response="$(mktemp "${TMPDIR:-/tmp}/praxis-rule-backend-validation-definition-response.XXXXXX.json")"
backend_validation_publication_request="$(mktemp "${TMPDIR:-/tmp}/praxis-rule-backend-validation-publication-request.XXXXXX.json")"
backend_validation_publication_response="$(mktemp "${TMPDIR:-/tmp}/praxis-rule-backend-validation-publication-response.XXXXXX.json")"
backend_validation_publication_status_file="$(mktemp "${TMPDIR:-/tmp}/praxis-rule-backend-validation-publication-status.XXXXXX.txt")"
backend_validation_materializations_list="$(mktemp "${TMPDIR:-/tmp}/praxis-rule-backend-validation-materializations-list.XXXXXX.json")"
backend_validation_command_request="$(mktemp "${TMPDIR:-/tmp}/praxis-rule-backend-validation-command-request.XXXXXX.json")"
backend_validation_command_response="$(mktemp "${TMPDIR:-/tmp}/praxis-rule-backend-validation-command-response.XXXXXX.json")"
backend_validation_command_status_file="$(mktemp "${TMPDIR:-/tmp}/praxis-rule-backend-validation-command-status.XXXXXX.txt")"
workflow_action_definition_request="$(mktemp "${TMPDIR:-/tmp}/praxis-rule-workflow-action-definition-request.XXXXXX.json")"
workflow_action_definition_response="$(mktemp "${TMPDIR:-/tmp}/praxis-rule-workflow-action-definition-response.XXXXXX.json")"
workflow_action_publication_request="$(mktemp "${TMPDIR:-/tmp}/praxis-rule-workflow-action-publication-request.XXXXXX.json")"
workflow_action_publication_response="$(mktemp "${TMPDIR:-/tmp}/praxis-rule-workflow-action-publication-response.XXXXXX.json")"
workflow_action_publication_status_file="$(mktemp "${TMPDIR:-/tmp}/praxis-rule-workflow-action-publication-status.XXXXXX.txt")"
workflow_action_materializations_list="$(mktemp "${TMPDIR:-/tmp}/praxis-rule-workflow-action-materializations-list.XXXXXX.json")"
workflow_action_command_request="$(mktemp "${TMPDIR:-/tmp}/praxis-rule-workflow-action-command-request.XXXXXX.json")"
workflow_action_command_response="$(mktemp "${TMPDIR:-/tmp}/praxis-rule-workflow-action-command-response.XXXXXX.json")"
workflow_action_command_status_file="$(mktemp "${TMPDIR:-/tmp}/praxis-rule-workflow-action-command-status.XXXXXX.txt")"
approval_policy_definition_request="$(mktemp "${TMPDIR:-/tmp}/praxis-rule-approval-policy-definition-request.XXXXXX.json")"
approval_policy_definition_response="$(mktemp "${TMPDIR:-/tmp}/praxis-rule-approval-policy-definition-response.XXXXXX.json")"
approval_policy_publication_request="$(mktemp "${TMPDIR:-/tmp}/praxis-rule-approval-policy-publication-request.XXXXXX.json")"
approval_policy_publication_response="$(mktemp "${TMPDIR:-/tmp}/praxis-rule-approval-policy-publication-response.XXXXXX.json")"
approval_policy_publication_status_file="$(mktemp "${TMPDIR:-/tmp}/praxis-rule-approval-policy-publication-status.XXXXXX.txt")"
approval_policy_materializations_list="$(mktemp "${TMPDIR:-/tmp}/praxis-rule-approval-policy-materializations-list.XXXXXX.json")"
approval_policy_command_request="$(mktemp "${TMPDIR:-/tmp}/praxis-rule-approval-policy-command-request.XXXXXX.json")"
approval_policy_command_response="$(mktemp "${TMPDIR:-/tmp}/praxis-rule-approval-policy-command-response.XXXXXX.json")"
approval_policy_command_status_file="$(mktemp "${TMPDIR:-/tmp}/praxis-rule-approval-policy-command-status.XXXXXX.txt")"
auth_cookie_jar="$(mktemp "${TMPDIR:-/tmp}/praxis-rule-auth-cookies.XXXXXX.txt")"
auth_login_request="$(mktemp "${TMPDIR:-/tmp}/praxis-rule-auth-login-request.XXXXXX.json")"
auth_login_status_file="$(mktemp "${TMPDIR:-/tmp}/praxis-rule-auth-login-status.XXXXXX.txt")"
auth_session_response="$(mktemp "${TMPDIR:-/tmp}/praxis-rule-auth-session-response.XXXXXX.txt")"
definitions_list="$(mktemp "${TMPDIR:-/tmp}/praxis-rule-definitions-list.XXXXXX.json")"
materializations_list="$(mktemp "${TMPDIR:-/tmp}/praxis-rule-materializations-list.XXXXXX.json")"
timeline_response="$(mktemp "${TMPDIR:-/tmp}/praxis-rule-timeline-response.XXXXXX.json")"
timeline_status_file="$(mktemp "${TMPDIR:-/tmp}/praxis-rule-timeline-status.XXXXXX.txt")"
trap 'rm -f "$intake_request" "$intake_response" "$simulation_request" "$simulation_response" "$simulation_status_file" "$materialization_request" "$materialization_response" "$definition_transition_request" "$definition_transition_response" "$materialization_transition_request" "$materialization_transition_response" "$publication_definition_request" "$publication_definition_response" "$publication_request" "$publication_response" "$publication_status_file" "$publication_option_runtime_request" "$publication_option_runtime_response" "$backend_validation_definition_request" "$backend_validation_definition_response" "$backend_validation_publication_request" "$backend_validation_publication_response" "$backend_validation_publication_status_file" "$backend_validation_materializations_list" "$backend_validation_command_request" "$backend_validation_command_response" "$backend_validation_command_status_file" "$workflow_action_definition_request" "$workflow_action_definition_response" "$workflow_action_publication_request" "$workflow_action_publication_response" "$workflow_action_publication_status_file" "$workflow_action_materializations_list" "$workflow_action_command_request" "$workflow_action_command_response" "$workflow_action_command_status_file" "$approval_policy_definition_request" "$approval_policy_definition_response" "$approval_policy_publication_request" "$approval_policy_publication_response" "$approval_policy_publication_status_file" "$approval_policy_materializations_list" "$approval_policy_command_request" "$approval_policy_command_response" "$approval_policy_command_status_file" "$auth_cookie_jar" "$auth_login_request" "$auth_login_status_file" "$auth_session_response" "$definitions_list" "$materializations_list" "$timeline_response" "$timeline_status_file"' EXIT

echo "Verifying shared domain rule runtime APIs."
echo "BACKEND_URL=${BACKEND_URL}"
echo "TENANT_ID=${TENANT_ID}"
echo "ENVIRONMENT=${ENVIRONMENT}"
echo "PUBLICATION_TENANT_ID=${PUBLICATION_TENANT_ID}"
echo "PUBLICATION_ENVIRONMENT=${PUBLICATION_ENVIRONMENT}"
echo "RESOURCE_KEY=${RESOURCE_KEY}"
echo "SMOKE_RUN_ID=${SMOKE_RUN_ID}"
echo "REQUIRE_SIMULATION=${REQUIRE_SIMULATION}"
echo "REQUIRE_PUBLICATION=${REQUIRE_PUBLICATION}"
echo "REQUIRE_BACKEND_VALIDATION=${REQUIRE_BACKEND_VALIDATION}"
echo "REQUIRE_WORKFLOW_ACTION=${REQUIRE_WORKFLOW_ACTION}"
echo "REQUIRE_APPROVAL_POLICY=${REQUIRE_APPROVAL_POLICY}"
echo "REQUIRE_TIMELINE=${REQUIRE_TIMELINE}"
echo "AUTHOR_USER_ID=${AUTHOR_USER_ID}"
echo "REVIEWER_USER_ID=${REVIEWER_USER_ID}"

jq -n \
  --arg ruleKey "$RULE_KEY" \
  --arg contextKey "$CONTEXT_KEY" \
  --arg resourceKey "$RESOURCE_KEY" \
  --arg serviceKey "$SERVICE_KEY" \
  --arg reviewerUserId "$REVIEWER_USER_ID" \
  '{
    prompt: "Avisar o analista quando CPF estiver presente no formulario de funcionarios.",
    assistantMessage: "Abrirei uma decisao semantica governada para que a plataforma materialize apenas projecoes derivadas.",
    ruleKey: $ruleKey,
    ruleType: "visual_guidance",
    contextKey: $contextKey,
    resourceKey: $resourceKey,
    serviceKey: $serviceKey,
    definition: {
      summary: "Avisar o analista quando CPF estiver presente no formulario de funcionarios.",
      recommendedAuthoringFlow: "shared_rule_authoring",
      derivedMaterializationOperation: "rule.visualBlockGuidance.add",
      rationale: "CPF possui governanca LGPD/GDPR e exige revisao antes de materializar uma projecao visual derivada criada por IA."
    },
    parameters: {
      field: "cpf",
      visualBlockId: "lgpd-notice",
      messageNodeId: "message"
    },
    condition: {
      "!=": [
        {"var": "cpf"},
        null
      ]
    },
    governance: {
      complianceTags: ["LGPD", "GDPR"],
      classification: "confidential",
      ruleAuthoring: "review_required",
      requiredApprovals: [$reviewerUserId],
      authorizedApprovers: [$reviewerUserId],
      aiUsage: {
        visibility: "mask",
        trainingUse: "deny"
      }
    }
  }' > "$intake_request"

jq -n \
  --arg ruleKey "$PUBLICATION_RULE_KEY" \
  --arg contextKey "$PUBLICATION_CONTEXT_KEY" \
  --arg resourceKey "$PUBLICATION_RESOURCE_KEY" \
  --arg serviceKey "$SERVICE_KEY" \
  --arg optionSourceKey "$PUBLICATION_OPTION_SOURCE_KEY" \
  --argjson blockedStatuses "$PUBLICATION_RUNTIME_PROBE_BLOCKED_STATUSES_JSON" \
  '{
    ruleKey: $ruleKey,
    ruleType: "selection_eligibility",
    status: "approved",
    contextKey: $contextKey,
    resourceKey: $resourceKey,
    serviceKey: $serviceKey,
    semanticOwner: "procurement-owner",
    steward: "procurement-owner",
    definition: {
      summary: "Publicacao governada de elegibilidade de selecao para fornecedores bloqueados ou inativos.",
      recommendedAuthoringFlow: "shared_rule_authoring"
    },
    parameters: {
      optionSourceKey: $optionSourceKey,
      validationMessageTemplate: "Fornecedor indisponivel para compras"
    },
    condition: {
      in: [
        {"var": "status"},
        $blockedStatuses
      ]
    },
    governance: {
      requiredApprovals: []
    }
  }' > "$publication_definition_request"

jq -n \
  --arg ruleKey "$BACKEND_VALIDATION_RULE_KEY" \
  --arg contextKey "$PUBLICATION_CONTEXT_KEY" \
  --arg resourceKey "$BACKEND_VALIDATION_RESOURCE_KEY" \
  --arg serviceKey "$SERVICE_KEY" \
  --argjson blockedStatuses "$BACKEND_VALIDATION_BLOCKED_STATUSES_JSON" \
  '{
    ruleKey: $ruleKey,
    ruleType: "validation",
    status: "approved",
    contextKey: $contextKey,
    resourceKey: $resourceKey,
    serviceKey: $serviceKey,
    semanticOwner: "procurement-owner",
    steward: "procurement-owner",
    definition: {
      summary: "Validar no backend que pedidos de compra nao usem fornecedor em status bloqueado.",
      recommendedAuthoringFlow: "shared_rule_authoring"
    },
    parameters: {
      referenceResourceKey: "procurement.suppliers",
      referenceField: "supplierId",
      statusPropertyPath: "status",
      blockedStatuses: $blockedStatuses,
      validationMessageTemplate: "Fornecedor indisponivel para pedidos de compra"
    },
    condition: {
      in: [
        {"var": "supplier.status"},
        $blockedStatuses
      ]
    },
    governance: {
      requiredApprovals: []
    }
  }' > "$backend_validation_definition_request"

jq -n \
  --arg ruleKey "$WORKFLOW_ACTION_RULE_KEY" \
  --arg contextKey "$WORKFLOW_ACTION_CONTEXT_KEY" \
  --arg resourceKey "$WORKFLOW_ACTION_RESOURCE_KEY" \
  --arg serviceKey "$SERVICE_KEY" \
  --arg actionId "$WORKFLOW_ACTION_ID" \
  --arg semanticOwner "$WORKFLOW_ACTION_SEMANTIC_OWNER" \
  --arg steward "$WORKFLOW_ACTION_STEWARD" \
  --arg summary "$WORKFLOW_ACTION_POLICY_SUMMARY" \
  --arg message "$WORKFLOW_ACTION_POLICY_MESSAGE" \
  --argjson blockedStates "$WORKFLOW_ACTION_BLOCKED_STATES_JSON" \
  '{
    ruleKey: $ruleKey,
    ruleType: "workflow_action_policy",
    status: "approved",
    contextKey: $contextKey,
    resourceKey: $resourceKey,
    serviceKey: $serviceKey,
    semanticOwner: $semanticOwner,
    steward: $steward,
    definition: {
      summary: $summary,
      recommendedAuthoringFlow: "shared_rule_authoring"
    },
    parameters: {
      workflowAction: {
        resourceKey: $resourceKey,
        actionId: $actionId
      },
      requiredStates: $blockedStates,
      message: $message
    },
    condition: {
      in: [
        {"var": "state"},
        $blockedStates
      ]
    },
    governance: {
      requiredApprovals: []
    }
  }' > "$workflow_action_definition_request"

jq -n \
  --arg ruleKey "$APPROVAL_POLICY_RULE_KEY" \
  --arg contextKey "human-resources" \
  --arg resourceKey "$APPROVAL_POLICY_RESOURCE_KEY" \
  --arg serviceKey "$SERVICE_KEY" \
  --arg actionId "$APPROVAL_POLICY_ACTION_ID" \
  '{
    ruleKey: $ruleKey,
    ruleType: "approval_policy",
    status: "approved",
    contextKey: $contextKey,
    resourceKey: $resourceKey,
    serviceKey: $serviceKey,
    semanticOwner: "hr-operations-owner",
    steward: "payroll-manager",
    definition: {
      summary: "Exigir aprovacao gerencial governada antes de aprovar eventos de folha em lote.",
      recommendedAuthoringFlow: "shared_rule_authoring"
    },
    parameters: {
      approvalAction: {
        resourceKey: $resourceKey,
        actionId: $actionId
      },
      requiredApprovals: ["payroll-manager"],
      approvalGroups: ["hr-payroll"],
      approverContext: "payroll-events",
      message: "Aprovacao em massa exige decisao gerencial governada."
    },
    condition: {
      ">": [
        {"var": "selectedCount"},
        0
      ]
    },
    governance: {
      requiredApprovals: []
    }
  }' > "$approval_policy_definition_request"

echo
echo "Opening governed shared rule intake ${RULE_KEY}."
if ! post_json "/api/praxis/config/domain-rules/intake" "$intake_request" "$intake_response"; then
  echo "Could not open governed shared rule intake. Verify that the runtime includes the domain-rules intake API and that V20 is applied." >&2
  exit 1
fi

definition_id="$(jq -r '.definition.id // empty' "$intake_response")"
definition_version="$(jq -r '.definition.version // empty' "$intake_response")"
definition_status="$(jq -r '.definition.status // empty' "$intake_response")"
definition_created_by_type="$(jq -r '.definition.createdByType // empty' "$intake_response")"
definition_created_by="$(jq -r '.definition.createdBy // empty' "$intake_response")"
intake_grounding_definition_id="$(jq -r '.grounding.ruleDefinitionId // empty' "$intake_response")"
intake_decision_stage="$(jq -r '.grounding.decisionDiagnostics.decisionStage // empty' "$intake_response")"
intake_decision_source="$(jq -r '.grounding.decisionDiagnostics.decisionSource // empty' "$intake_response")"

if [[ -z "$definition_id" || -z "$definition_version" || "$definition_status" != "draft" || "$definition_created_by_type" != "authenticated" || "$definition_created_by" != "$AUTHOR_USER_ID" || "$intake_grounding_definition_id" != "$definition_id" || "$intake_decision_stage" != "intake" || "$intake_decision_source" != "persisted_definition" ]]; then
  echo "Invalid governed intake response." >&2
  jq '{intakeId, ruleKey, status, grounding, definition: {id: .definition.id, version: .definition.version, status: .definition.status}}' "$intake_response" >&2
  exit 1
fi

jq '{status: "intake-received", intakeId, ruleKey, ruleType, resourceKey, serviceKey, grounding, definition: {id: .definition.id, version: .definition.version, status: .definition.status, governance: .definition.governance}}' "$intake_response"

jq -n \
  --arg ruleDefinitionId "$definition_id" \
  --arg ruleKey "$RULE_KEY" \
  --arg contextKey "$CONTEXT_KEY" \
  --arg resourceKey "$RESOURCE_KEY" \
  --arg serviceKey "$SERVICE_KEY" \
  '{
    ruleDefinitionId: $ruleDefinitionId,
    ruleKey: $ruleKey,
    ruleType: "visual_guidance",
    contextKey: $contextKey,
    resourceKey: $resourceKey,
    serviceKey: $serviceKey,
    definition: {
      summary: "Avisar o analista quando CPF estiver presente no formulario de funcionarios.",
      recommendedAuthoringFlow: "shared_rule_authoring",
      derivedMaterializationOperation: "rule.visualBlockGuidance.add",
      rationale: "CPF possui governanca LGPD/GDPR e exige revisao antes de materializar uma projecao visual derivada criada por IA."
    },
    parameters: {
      field: "cpf",
      visualBlockId: "lgpd-notice",
      messageNodeId: "message"
    },
    condition: {
      "!=": [
        {"var": "cpf"},
        null
      ]
    },
    governance: {
      complianceTags: ["LGPD", "GDPR"],
      classification: "confidential",
      ruleAuthoring: "review_required",
      aiUsage: {
        visibility: "mask",
        trainingUse: "deny"
      }
    }
  }' > "$simulation_request"

if [[ "$REQUIRE_SIMULATION" != "false" ]]; then
  echo
  echo "Simulating persisted shared rule draft ${RULE_KEY}."
  post_json_allow_status "/api/praxis/config/domain-rules/simulations" "$simulation_request" "$simulation_response" "$simulation_status_file"
  simulation_status="$(cat "$simulation_status_file")"

  if [[ "$simulation_status" == "200" || "$simulation_status" == "201" ]]; then
    simulation_result="$(jq -r '.result // empty' "$simulation_response")"
    simulation_resource_key="$(jq -r '.resourceKey // empty' "$simulation_response")"
    predicted_count="$(jq '(.predictedMaterializations // []) | length' "$simulation_response")"
    explainability_summary="$(jq -r '.explainability.summary // empty' "$simulation_response")"
    explainability_action="$(jq -r '.explainability.recommendedAction // empty' "$simulation_response")"
    explainability_readiness="$(jq -r '.explainability.publicationReadiness // empty' "$simulation_response")"
    next_steps_count="$(jq '(.explainability.nextSteps // []) | length' "$simulation_response")"
    if [[ -z "$simulation_result" || -z "$simulation_resource_key" || "$predicted_count" -eq 0 || -z "$explainability_summary" || -z "$explainability_action" || -z "$explainability_readiness" || "$next_steps_count" -eq 0 ]]; then
      echo "Invalid domain rule simulation response." >&2
      jq '{simulationId, ruleKey, result, resourceKey, predictedMaterializations, requiredApprovals, warnings, explainability}' "$simulation_response" >&2
      exit 1
    fi

    jq '{status: "simulation-completed", simulationId, ruleKey, result, resourceKey, predictedMaterializations, requiredApprovals, warnings, explainability}' "$simulation_response"
  elif [[ "$simulation_status" == "404" || "$simulation_status" == "405" ]]; then
    if [[ "$REQUIRE_SIMULATION" == "true" ]]; then
      echo "Runtime does not expose /api/praxis/config/domain-rules/simulations yet." >&2
      jq -r '.message // .error // empty' "$simulation_response" >&2 || true
      exit 1
    fi
    echo "Warning: runtime does not expose /api/praxis/config/domain-rules/simulations yet; continuing with definition/materialization smoke." >&2
  else
    echo "Unexpected domain rule simulation response (HTTP ${simulation_status})." >&2
    cat "$simulation_response" >&2
    exit 1
  fi
fi

echo
jq -n \
  '{
    status: "approved",
    validationResult: {
      review: "approved",
      checks: ["definition-governance-reviewed"]
    }
  }' > "$definition_transition_request"

echo
echo "Approving shared rule definition ${definition_id}."
if ! patch_json_as_reviewer "/api/praxis/config/domain-rules/definitions/${definition_id}/status" "$definition_transition_request" "$definition_transition_response"; then
  echo "Could not approve shared rule definition. Verify that the runtime includes rule status transition APIs." >&2
  exit 1
fi

definition_approved_status="$(jq -r '.status // empty' "$definition_transition_response")"
definition_approved_at="$(jq -r '.approvedAt // empty' "$definition_transition_response")"
definition_approved_by="$(jq -r '.approvedBy // empty' "$definition_transition_response")"
if [[ "$definition_approved_status" != "approved" || -z "$definition_approved_at" || "$definition_approved_by" != "$REVIEWER_USER_ID" ]]; then
  echo "Invalid approved definition response." >&2
  jq '{id, ruleKey, status, approvedAt, activatedAt, validationResult}' "$definition_transition_response" >&2
  exit 1
fi

jq '{status: "definition-approved", id, ruleKey, approvedBy, approvedAt, activatedAt, validationResult}' "$definition_transition_response"

jq -n \
  '{
    status: "active",
    validationResult: {
      review: "approved",
      checks: ["definition-governance-reviewed", "definition-activation-reviewed"]
    }
  }' > "$definition_transition_request"

echo
echo "Activating shared rule definition ${definition_id}."
if ! patch_json_as_reviewer "/api/praxis/config/domain-rules/definitions/${definition_id}/status" "$definition_transition_request" "$definition_transition_response"; then
  echo "Could not activate shared rule definition. Verify that the runtime includes rule status transition APIs." >&2
  exit 1
fi

definition_active_status="$(jq -r '.status // empty' "$definition_transition_response")"
definition_activated_at="$(jq -r '.activatedAt // empty' "$definition_transition_response")"
if [[ "$definition_active_status" != "active" || -z "$definition_activated_at" ]]; then
  echo "Invalid activated definition response." >&2
  jq '{id, ruleKey, status, approvedAt, activatedAt, validationResult}' "$definition_transition_response" >&2
  exit 1
fi

jq '{status: "definition-activated", id, ruleKey, approvedBy, approvedAt, activatedAt, validationResult}' "$definition_transition_response"

jq -n \
  --arg ruleDefinitionId "$definition_id" \
  --arg materializationKey "$MATERIALIZATION_KEY" \
  --arg targetArtifactKey "$TARGET_ARTIFACT_KEY" \
  --arg targetReleaseKey "$TARGET_RELEASE_KEY" \
  '{
    ruleDefinitionId: $ruleDefinitionId,
    materializationKey: $materializationKey,
    targetLayer: "form_config",
    targetArtifactType: "praxis-dynamic-form",
    targetArtifactKey: $targetArtifactKey,
    targetPointer: "/formRules/-",
    targetReleaseKey: $targetReleaseKey,
    materializedRuleId: "lgpd-cpf-guidance",
    status: "pending_review",
    materializedPayload: {
      id: "lgpd-cpf-guidance",
      operation: "rule.visualBlockGuidance.add",
      metadata: {
        origin: "llm",
        reviewStatus: "pending"
      },
      effect: {
        condition: {
          "!=": [
            {"var": "cpf"},
            null
          ]
        },
        targetBlockId: "lgpd-notice",
        properties: {
          severity: "warning",
          message: "CPF e dado pessoal regulado por LGPD/GDPR. Revise finalidade, mascara e permissao antes de prosseguir."
        }
      }
    },
    sourceHash: "runtime-smoke-lgpd-cpf",
    appliedByType: "llm",
    appliedBy: "runtime-smoke"
  }' > "$materialization_request"

echo
echo "Creating FormConfig materialization ${MATERIALIZATION_KEY}."
if ! post_json "/api/praxis/config/domain-rules/materializations" "$materialization_request" "$materialization_response"; then
  echo "Could not create rule materialization. Verify that the definition exists and V20 is applied." >&2
  exit 1
fi

materialization_id="$(jq -r '.id // empty' "$materialization_response")"
if [[ -z "$materialization_id" ]]; then
  echo "Invalid rule materialization response." >&2
  jq '{id, materializationKey, targetLayer, targetArtifactKey, status}' "$materialization_response" >&2
  exit 1
fi

jq '{status: "materialization-created", id, materializationKey, ruleKey, ruleVersion, targetLayer, targetArtifactType, targetArtifactKey, targetPointer, materializedRuleId}' "$materialization_response"

jq -n \
  '{
    status: "applied",
    decidedByType: "human",
    decidedBy: "privacy-office",
    validationResult: {
      review: "approved",
      checks: ["form-rule-materialization-compatible"]
    }
  }' > "$materialization_transition_request"

echo
echo "Applying FormConfig materialization ${materialization_id}."
if ! patch_json "/api/praxis/config/domain-rules/materializations/${materialization_id}/status" "$materialization_transition_request" "$materialization_transition_response"; then
  echo "Could not apply rule materialization. Verify that the runtime includes rule status transition APIs." >&2
  exit 1
fi

materialization_applied_status="$(jq -r '.status // empty' "$materialization_transition_response")"
materialization_applied_at="$(jq -r '.appliedAt // empty' "$materialization_transition_response")"
if [[ "$materialization_applied_status" != "applied" || -z "$materialization_applied_at" ]]; then
  echo "Invalid applied materialization response." >&2
  jq '{id, materializationKey, status, appliedByType, appliedBy, appliedAt, validationResult}' "$materialization_transition_response" >&2
  exit 1
fi

jq '{status: "materialization-applied", id, materializationKey, appliedByType, appliedBy, appliedAt, validationResult}' "$materialization_transition_response"

validate_definition_timeline "$definition_id" "$TENANT_ID" "$ENVIRONMENT" "form_config" "form_config" "$TARGET_ARTIFACT_KEY" "false" "true" "true"

if [[ "$REQUIRE_PUBLICATION" != "false" ]]; then
  echo
  echo "Creating publication-ready shared rule definition ${PUBLICATION_RULE_KEY}."
  if ! post_json_scoped "/api/praxis/config/domain-rules/definitions" "$publication_definition_request" "$publication_definition_response" "$PUBLICATION_TENANT_ID" "$PUBLICATION_ENVIRONMENT"; then
    echo "Could not create publication-ready shared rule definition." >&2
    exit 1
  fi

  publication_definition_id="$(jq -r '.id // empty' "$publication_definition_response")"
  if [[ -z "$publication_definition_id" ]]; then
    echo "Invalid publication-ready rule definition response." >&2
    jq '{id, ruleKey, version, status}' "$publication_definition_response" >&2
    exit 1
  fi

  jq -n \
    --arg ruleDefinitionId "$publication_definition_id" \
    '{
      ruleDefinitionId: $ruleDefinitionId,
      applyEligibleMaterializations: true,
      publishedByType: "human",
      publishedBy: "procurement-owner"
    }' > "$publication_request"

  echo "Publishing shared rule definition ${publication_definition_id}."
  post_json_allow_status_scoped "/api/praxis/config/domain-rules/publications" "$publication_request" "$publication_response" "$publication_status_file" "$PUBLICATION_TENANT_ID" "$PUBLICATION_ENVIRONMENT"
  publication_status="$(cat "$publication_status_file")"

  if [[ "$publication_status" == "200" || "$publication_status" == "201" ]]; then
    publication_result="$(jq -r '.publicationStatus // empty' "$publication_response")"
    publication_readiness="$(jq -r '.publicationReadiness // empty' "$publication_response")"
    published_definition_status="$(jq -r '.definition.status // empty' "$publication_response")"
    published_materialization_count="$(jq '(.materializations // []) | length' "$publication_response")"
    published_target_layer="$(jq -r '.materializations[0].targetLayer // empty' "$publication_response")"
    published_target_type="$(jq -r '.materializations[0].targetArtifactType // empty' "$publication_response")"
    published_target_key="$(jq -r '.materializations[0].targetArtifactKey // empty' "$publication_response")"
    published_materialization_status="$(jq -r '.materializations[0].status // empty' "$publication_response")"
    published_payload_kind="$(jq -r '.materializations[0].materializedPayload.kind // empty' "$publication_response")"
    published_status_property="$(jq -r '.materializations[0].materializedPayload.selectionPolicy.statusPropertyPath // empty' "$publication_response")"
    published_blocked_statuses_count="$(jq '(.materializations[0].materializedPayload.selectionPolicy.blockedStatuses // []) | length' "$publication_response")"
    if [[ "$publication_result" != "published" || "$publication_readiness" != "ready_to_publish" || "$published_definition_status" != "active" || "$published_materialization_count" -eq 0 || "$published_target_layer" != "option_source" || "$published_target_type" != "resource-option-source" || "$published_target_key" != "$PUBLICATION_OPTION_SOURCE_KEY" || "$published_materialization_status" != "applied" || "$published_payload_kind" != "lookup_selection_policy" || -z "$published_status_property" || "$published_blocked_statuses_count" -eq 0 ]]; then
      echo "Invalid shared rule publication response." >&2
      jq '{publicationId, publicationStatus, publicationReadiness, ruleKey, definition, materializations, explainability}' "$publication_response" >&2
      exit 1
    fi

    jq '{status: "publication-completed", publicationId, publicationStatus, publicationReadiness, ruleKey, definitionStatus: .definition.status, materializations: (.materializations | map({targetLayer, targetArtifactType, targetArtifactKey, status, kind: .materializedPayload.kind, selectionPolicy: .materializedPayload.selectionPolicy})), explainability}' "$publication_response"

    validate_definition_timeline "$publication_definition_id" "$PUBLICATION_TENANT_ID" "$PUBLICATION_ENVIRONMENT" "option_source" "option_source" "$PUBLICATION_OPTION_SOURCE_KEY" "true"

    echo "Verifying published option_source policy against supplier lookup runtime."
    jq -n '{}' > "$publication_option_runtime_request"
    post_json_scoped "/api/procurement/suppliers/option-sources/${PUBLICATION_OPTION_SOURCE_KEY}/options/filter?page=0&size=25" "$publication_option_runtime_request" "$publication_option_runtime_response" "$PUBLICATION_TENANT_ID" "$PUBLICATION_ENVIRONMENT"
    runtime_blocked_option="$(jq --argjson blockedStatuses "$PUBLICATION_RUNTIME_PROBE_BLOCKED_STATUSES_JSON" '[.content[] | select((.extra.status // "") as $status | ($blockedStatuses | index($status)) != null)] | .[0] // null' "$publication_option_runtime_response")"
    runtime_option_selectable="$(jq -r 'if has("extra") and (.extra | has("selectable")) then (.extra.selectable | tostring) else "" end' <<<"$runtime_blocked_option")"
    runtime_option_status="$(jq -r '.extra.status // empty' <<<"$runtime_blocked_option")"
    runtime_option_id="$(jq -r '.id // empty' <<<"$runtime_blocked_option")"
    if [[ "$runtime_blocked_option" == "null" || "$runtime_option_selectable" != "false" ]]; then
      echo "Published option_source policy was not reflected by the supplier lookup runtime." >&2
      jq '{expectedTenant: "'"$PUBLICATION_TENANT_ID"'", expectedBlockedStatuses: '"$PUBLICATION_RUNTIME_PROBE_BLOCKED_STATUSES_JSON"', optionCount: (.content | length), options: [.content[] | {id, label, status: .extra.status, selectable: .extra.selectable}]}' "$publication_option_runtime_response" >&2
      exit 1
    fi
    jq -n \
      --arg status "publication-option-source-runtime-ready" \
      --arg tenantId "$PUBLICATION_TENANT_ID" \
      --arg optionSourceKey "$PUBLICATION_OPTION_SOURCE_KEY" \
      --arg supplierId "$runtime_option_id" \
      --arg supplierStatus "$runtime_option_status" \
      --argjson selectable "$runtime_option_selectable" \
      '{status: $status, tenantId: $tenantId, optionSourceKey: $optionSourceKey, supplierId: $supplierId, supplierStatus: $supplierStatus, selectable: $selectable}'

    if [[ "$REQUIRE_BACKEND_VALIDATION" != "false" ]]; then
      echo
      echo "Creating backend_validation shared rule definition ${BACKEND_VALIDATION_RULE_KEY}."
      if ! post_json_scoped "/api/praxis/config/domain-rules/definitions" "$backend_validation_definition_request" "$backend_validation_definition_response" "$PUBLICATION_TENANT_ID" "$PUBLICATION_ENVIRONMENT"; then
        echo "Could not create backend_validation shared rule definition." >&2
        exit 1
      fi

      backend_validation_definition_id="$(jq -r '.id // empty' "$backend_validation_definition_response")"
      if [[ -z "$backend_validation_definition_id" ]]; then
        echo "Invalid backend_validation rule definition response." >&2
        jq '{id, ruleKey, version, status}' "$backend_validation_definition_response" >&2
        exit 1
      fi

      jq -n \
        --arg ruleDefinitionId "$backend_validation_definition_id" \
        '{
          ruleDefinitionId: $ruleDefinitionId,
          applyEligibleMaterializations: true,
          publishedByType: "human",
          publishedBy: "procurement-owner"
        }' > "$backend_validation_publication_request"

      echo "Publishing backend_validation shared rule definition ${backend_validation_definition_id}."
      post_json_allow_status_scoped "/api/praxis/config/domain-rules/publications" "$backend_validation_publication_request" "$backend_validation_publication_response" "$backend_validation_publication_status_file" "$PUBLICATION_TENANT_ID" "$PUBLICATION_ENVIRONMENT"
      backend_validation_publication_status="$(cat "$backend_validation_publication_status_file")"

      if [[ "$backend_validation_publication_status" == "200" || "$backend_validation_publication_status" == "201" ]]; then
        backend_validation_materialization_count="$(jq '[
          (.materializations // [])[]
          | select((.targetLayer // "") == "backend_validation")
          | select((.targetArtifactType // "") == "resource-validation")
          | select((.targetArtifactKey // "") == "'"$BACKEND_VALIDATION_RESOURCE_KEY"'")
          | select((.status // "") == "applied")
          | select((.materializedPayload.kind // "") == "resource_validation_policy")
        ] | length' "$backend_validation_publication_response")"

        if [[ "$backend_validation_materialization_count" -eq 0 ]]; then
          encoded_backend_validation_target_layer="$(urlencode "backend_validation")"
          encoded_backend_validation_target_type="$(urlencode "resource-validation")"
          encoded_backend_validation_target_key="$(urlencode "$BACKEND_VALIDATION_RESOURCE_KEY")"
          encoded_backend_validation_status="$(urlencode "applied")"
          if get_json_scoped "/api/praxis/config/domain-rules/materializations?targetLayer=${encoded_backend_validation_target_layer}&targetArtifactType=${encoded_backend_validation_target_type}&targetArtifactKey=${encoded_backend_validation_target_key}&status=${encoded_backend_validation_status}" "$backend_validation_materializations_list" "$PUBLICATION_TENANT_ID" "$PUBLICATION_ENVIRONMENT"; then
            backend_validation_materialization_count="$(jq '[
              .[]
              | select((.materializedPayload.kind // "") == "resource_validation_policy")
            ] | length' "$backend_validation_materializations_list")"
          fi
        fi

        if [[ "$backend_validation_materialization_count" -eq 0 ]]; then
          if [[ "$REQUIRE_BACKEND_VALIDATION" == "true" ]]; then
            echo "Publication did not derive backend_validation materialization for ${BACKEND_VALIDATION_RESOURCE_KEY}." >&2
            jq '{publicationId, publicationStatus, publicationReadiness, ruleKey, definition, materializations, explainability}' "$backend_validation_publication_response" >&2
            exit 1
          fi
          echo "Warning: publication did not derive backend_validation materialization yet; skipping purchase-order command probe." >&2
        else
          echo "Verifying published backend_validation policy against purchase-order command runtime."
          if ! authenticate_command_probe "$auth_login_request" "$auth_login_status_file" "$auth_session_response"; then
            if [[ "$REQUIRE_BACKEND_VALIDATION" == "true" ]]; then
              echo "Purchase-order command probe could not authenticate with /auth/login (${AUTH_FAILURE_REASON:-unknown-auth-failure}). Configure ADMIN_PASSWORD or PRACTICE_TEMP_PASSWORD with the runtime admin password." >&2
              exit 1
            fi
            echo "Warning: command probe authentication failed (${AUTH_FAILURE_REASON:-unknown-auth-failure}); skipping backend_validation command probe." >&2
          else
            jq -n \
              --argjson companyId "$BACKEND_VALIDATION_COMPANY_ID" \
              --argjson supplierId "$BACKEND_VALIDATION_SUPPLIER_ID" \
              --argjson productId "$BACKEND_VALIDATION_PRODUCT_ID" \
              '{
                companyId: $companyId,
                supplierId: $supplierId,
                productId: $productId,
                orderDate: "2026-04-24",
                currency: "BRL",
                quantity: 1
              }' > "$backend_validation_command_request"

            post_json_allow_status_scoped_authenticated "/api/procurement/purchase-orders" "$backend_validation_command_request" "$backend_validation_command_response" "$backend_validation_command_status_file" "$PUBLICATION_TENANT_ID" "$PUBLICATION_ENVIRONMENT"
            backend_validation_command_status="$(cat "$backend_validation_command_status_file")"
            if [[ "$backend_validation_command_status" == "409" ]]; then
              validate_definition_timeline "$backend_validation_definition_id" "$PUBLICATION_TENANT_ID" "$PUBLICATION_ENVIRONMENT" "backend_validation" "backend_validation" "$BACKEND_VALIDATION_RESOURCE_KEY"
              jq -n \
                --arg status "publication-backend-validation-runtime-ready" \
                --arg tenantId "$PUBLICATION_TENANT_ID" \
                --arg resourceKey "$BACKEND_VALIDATION_RESOURCE_KEY" \
                --arg supplierId "$BACKEND_VALIDATION_SUPPLIER_ID" \
                '{status: $status, tenantId: $tenantId, resourceKey: $resourceKey, supplierId: $supplierId, rejectedWith: 409}'
            elif [[ "$backend_validation_command_status" == "401" || "$backend_validation_command_status" == "403" ]]; then
              if [[ "$REQUIRE_BACKEND_VALIDATION" == "true" ]]; then
                echo "Purchase-order command endpoint requires authentication; backend_validation probe could not be completed." >&2
                cat "$backend_validation_command_response" >&2
                exit 1
              fi
              echo "Warning: purchase-order command endpoint requires authentication; skipping backend_validation command probe." >&2
            else
              echo "Published backend_validation policy was not reflected by the purchase-order command runtime (HTTP ${backend_validation_command_status})." >&2
              cat "$backend_validation_command_response" >&2
              exit 1
            fi
          fi
        fi
      elif [[ "$backend_validation_publication_status" == "404" || "$backend_validation_publication_status" == "405" ]]; then
        if [[ "$REQUIRE_BACKEND_VALIDATION" == "true" ]]; then
          echo "Runtime does not expose /api/praxis/config/domain-rules/publications for backend_validation yet." >&2
          jq -r '.message // .error // empty' "$backend_validation_publication_response" >&2 || true
          exit 1
        fi
        echo "Warning: runtime does not expose backend_validation publication yet; backend_validation rollout is still pending." >&2
      else
        echo "Unexpected backend_validation publication response (HTTP ${backend_validation_publication_status})." >&2
        cat "$backend_validation_publication_response" >&2
        exit 1
      fi
    fi

    if [[ "$REQUIRE_WORKFLOW_ACTION" != "false" ]]; then
      echo
      echo "Creating workflow_action shared rule definition ${WORKFLOW_ACTION_RULE_KEY}."
      if ! post_json_scoped "/api/praxis/config/domain-rules/definitions" "$workflow_action_definition_request" "$workflow_action_definition_response" "$PUBLICATION_TENANT_ID" "$PUBLICATION_ENVIRONMENT"; then
        echo "Could not create workflow_action shared rule definition." >&2
        exit 1
      fi

      workflow_action_definition_id="$(jq -r '.id // empty' "$workflow_action_definition_response")"
      if [[ -z "$workflow_action_definition_id" ]]; then
        echo "Invalid workflow_action rule definition response." >&2
        jq '{id, ruleKey, version, status}' "$workflow_action_definition_response" >&2
        exit 1
      fi

      jq -n \
        --arg ruleDefinitionId "$workflow_action_definition_id" \
        '{
          ruleDefinitionId: $ruleDefinitionId,
          applyEligibleMaterializations: true,
          publishedByType: "human",
          publishedBy: "payroll-compliance"
        }' > "$workflow_action_publication_request"

      echo "Publishing workflow_action shared rule definition ${workflow_action_definition_id}."
      post_json_allow_status_scoped "/api/praxis/config/domain-rules/publications" "$workflow_action_publication_request" "$workflow_action_publication_response" "$workflow_action_publication_status_file" "$PUBLICATION_TENANT_ID" "$PUBLICATION_ENVIRONMENT"
      workflow_action_publication_status="$(cat "$workflow_action_publication_status_file")"

      if [[ "$workflow_action_publication_status" == "200" || "$workflow_action_publication_status" == "201" ]]; then
        workflow_action_materialization_count="$(jq '[
          (.materializations // [])[]
          | select((.targetLayer // "") == "workflow_action")
          | select((.targetArtifactType // "") == "resource-workflow-action")
          | select((.targetArtifactKey // "") == "'"$WORKFLOW_ACTION_TARGET_KEY"'")
          | select((.status // "") == "applied")
          | select((.materializedPayload.kind // "") == "workflow_action_policy")
          | select((.sourceHash // "") != "")
        ] | length' "$workflow_action_publication_response")"

        if [[ "$workflow_action_materialization_count" -eq 0 ]]; then
          encoded_workflow_action_target_layer="$(urlencode "workflow_action")"
          encoded_workflow_action_target_type="$(urlencode "resource-workflow-action")"
          encoded_workflow_action_target_key="$(urlencode "$WORKFLOW_ACTION_TARGET_KEY")"
          encoded_workflow_action_status="$(urlencode "applied")"
          if get_json_scoped "/api/praxis/config/domain-rules/materializations?targetLayer=${encoded_workflow_action_target_layer}&targetArtifactType=${encoded_workflow_action_target_type}&targetArtifactKey=${encoded_workflow_action_target_key}&status=${encoded_workflow_action_status}" "$workflow_action_materializations_list" "$PUBLICATION_TENANT_ID" "$PUBLICATION_ENVIRONMENT"; then
            workflow_action_materialization_count="$(jq '[
              .[]
              | select((.materializedPayload.kind // "") == "workflow_action_policy")
              | select((.sourceHash // "") != "")
            ] | length' "$workflow_action_materializations_list")"
          fi
        fi

        if [[ "$workflow_action_materialization_count" -eq 0 ]]; then
          if [[ "$REQUIRE_WORKFLOW_ACTION" == "true" ]]; then
            echo "Publication did not derive workflow_action materialization for ${WORKFLOW_ACTION_TARGET_KEY}." >&2
            jq '{publicationId, publicationStatus, publicationReadiness, ruleKey, definition, materializations, explainability}' "$workflow_action_publication_response" >&2
            exit 1
          fi
          echo "Warning: publication did not derive workflow_action materialization yet; skipping workflow action probe." >&2
        else
          echo "Verifying published workflow_action policy against ${WORKFLOW_ACTION_COMMAND_PATH}."
          if ! authenticate_command_probe "$auth_login_request" "$auth_login_status_file" "$auth_session_response"; then
            if [[ "$REQUIRE_WORKFLOW_ACTION" == "true" ]]; then
              echo "Workflow action probe could not authenticate with /auth/login (${AUTH_FAILURE_REASON:-unknown-auth-failure}). Configure ADMIN_PASSWORD or PRACTICE_TEMP_PASSWORD with the runtime admin password." >&2
              exit 1
            fi
            echo "Warning: workflow action probe authentication failed (${AUTH_FAILURE_REASON:-unknown-auth-failure}); skipping workflow action probe." >&2
          else
            jq -n \
              '{
                justificativa: "Runtime smoke must be blocked by governed workflow_action policy"
              }' > "$workflow_action_command_request"

            post_json_allow_status_scoped_authenticated "$WORKFLOW_ACTION_COMMAND_PATH" "$workflow_action_command_request" "$workflow_action_command_response" "$workflow_action_command_status_file" "$PUBLICATION_TENANT_ID" "$PUBLICATION_ENVIRONMENT"
            workflow_action_command_status="$(cat "$workflow_action_command_status_file")"
            if [[ "$workflow_action_command_status" == "409" ]]; then
              validate_definition_timeline "$workflow_action_definition_id" "$PUBLICATION_TENANT_ID" "$PUBLICATION_ENVIRONMENT" "workflow_action" "workflow_action" "$WORKFLOW_ACTION_TARGET_KEY"
              jq -n \
                --arg status "publication-workflow-action-runtime-ready" \
                --arg tenantId "$PUBLICATION_TENANT_ID" \
                --arg resourceKey "$WORKFLOW_ACTION_RESOURCE_KEY" \
                --arg actionId "$WORKFLOW_ACTION_ID" \
                --arg resourceId "$WORKFLOW_ACTION_RESOURCE_ID" \
                --arg resourceIdLabel "$WORKFLOW_ACTION_RESOURCE_ID_LABEL" \
                '{status: $status, tenantId: $tenantId, resourceKey: $resourceKey, actionId: $actionId, resourceId: $resourceId, resourceIdLabel: $resourceIdLabel, rejectedWith: 409}'
            elif [[ "$workflow_action_command_status" == "401" || "$workflow_action_command_status" == "403" ]]; then
              if [[ "$REQUIRE_WORKFLOW_ACTION" == "true" ]]; then
                echo "Workflow action endpoint requires authentication; workflow_action probe could not be completed." >&2
                cat "$workflow_action_command_response" >&2
                exit 1
              fi
              echo "Warning: workflow action endpoint requires authentication; skipping workflow_action command probe." >&2
            else
              echo "Published workflow_action policy was not reflected by the payroll action runtime (HTTP ${workflow_action_command_status})." >&2
              cat "$workflow_action_command_response" >&2
              exit 1
            fi
          fi
        fi
      elif [[ "$workflow_action_publication_status" == "404" || "$workflow_action_publication_status" == "405" ]]; then
        if [[ "$REQUIRE_WORKFLOW_ACTION" == "true" ]]; then
          echo "Runtime does not expose /api/praxis/config/domain-rules/publications for workflow_action yet." >&2
          jq -r '.message // .error // empty' "$workflow_action_publication_response" >&2 || true
          exit 1
        fi
        echo "Warning: runtime does not expose workflow_action publication yet; workflow_action rollout is still pending." >&2
      else
        echo "Unexpected workflow_action publication response (HTTP ${workflow_action_publication_status})." >&2
        cat "$workflow_action_publication_response" >&2
        exit 1
      fi
    fi

    if [[ "$REQUIRE_APPROVAL_POLICY" != "false" ]]; then
      echo
      echo "Creating approval_policy shared rule definition ${APPROVAL_POLICY_RULE_KEY}."
      if ! post_json_scoped "/api/praxis/config/domain-rules/definitions" "$approval_policy_definition_request" "$approval_policy_definition_response" "$PUBLICATION_TENANT_ID" "$PUBLICATION_ENVIRONMENT"; then
        echo "Could not create approval_policy shared rule definition." >&2
        exit 1
      fi

      approval_policy_definition_id="$(jq -r '.id // empty' "$approval_policy_definition_response")"
      if [[ -z "$approval_policy_definition_id" ]]; then
        echo "Invalid approval_policy rule definition response." >&2
        jq '{id, ruleKey, version, status}' "$approval_policy_definition_response" >&2
        exit 1
      fi

      jq -n \
        --arg ruleDefinitionId "$approval_policy_definition_id" \
        '{
          ruleDefinitionId: $ruleDefinitionId,
          applyEligibleMaterializations: true,
          publishedByType: "human",
          publishedBy: "payroll-manager"
        }' > "$approval_policy_publication_request"

      echo "Publishing approval_policy shared rule definition ${approval_policy_definition_id}."
      post_json_allow_status_scoped "/api/praxis/config/domain-rules/publications" "$approval_policy_publication_request" "$approval_policy_publication_response" "$approval_policy_publication_status_file" "$PUBLICATION_TENANT_ID" "$PUBLICATION_ENVIRONMENT"
      approval_policy_publication_status="$(cat "$approval_policy_publication_status_file")"

      if [[ "$approval_policy_publication_status" == "200" || "$approval_policy_publication_status" == "201" ]]; then
        approval_policy_materialization_count="$(jq '[
          (.materializations // [])[]
          | select((.targetLayer // "") == "approval_policy")
          | select((.targetArtifactType // "") == "resource-action-approval")
          | select((.targetArtifactKey // "") == "'"$APPROVAL_POLICY_TARGET_KEY"'")
          | select((.status // "") == "applied")
          | select((.materializedPayload.kind // "") == "approval_policy")
          | select((.sourceHash // "") != "")
        ] | length' "$approval_policy_publication_response")"

        if [[ "$approval_policy_materialization_count" -eq 0 ]]; then
          encoded_approval_policy_target_layer="$(urlencode "approval_policy")"
          encoded_approval_policy_target_type="$(urlencode "resource-action-approval")"
          encoded_approval_policy_target_key="$(urlencode "$APPROVAL_POLICY_TARGET_KEY")"
          encoded_approval_policy_status="$(urlencode "applied")"
          if get_json_scoped "/api/praxis/config/domain-rules/materializations?targetLayer=${encoded_approval_policy_target_layer}&targetArtifactType=${encoded_approval_policy_target_type}&targetArtifactKey=${encoded_approval_policy_target_key}&status=${encoded_approval_policy_status}" "$approval_policy_materializations_list" "$PUBLICATION_TENANT_ID" "$PUBLICATION_ENVIRONMENT"; then
            approval_policy_materialization_count="$(jq '[
              .[]
              | select((.materializedPayload.kind // "") == "approval_policy")
              | select((.sourceHash // "") != "")
            ] | length' "$approval_policy_materializations_list")"
          fi
        fi

        if [[ "$approval_policy_materialization_count" -eq 0 ]]; then
          if [[ "$REQUIRE_APPROVAL_POLICY" == "true" ]]; then
            echo "Publication did not derive approval_policy materialization for ${APPROVAL_POLICY_TARGET_KEY}." >&2
            jq '{publicationId, publicationStatus, publicationReadiness, ruleKey, definition, materializations, explainability}' "$approval_policy_publication_response" >&2
            exit 1
          fi
          echo "Warning: publication did not derive approval_policy materialization yet; skipping payroll-events approval action probe." >&2
        else
          echo "Verifying published approval_policy against payroll-events bulk-approve runtime."
          if ! authenticate_command_probe "$auth_login_request" "$auth_login_status_file" "$auth_session_response"; then
            if [[ "$REQUIRE_APPROVAL_POLICY" == "true" ]]; then
              echo "Approval policy probe could not authenticate with /auth/login (${AUTH_FAILURE_REASON:-unknown-auth-failure}). Configure ADMIN_PASSWORD or PRACTICE_TEMP_PASSWORD with the runtime admin password." >&2
              exit 1
            fi
            echo "Warning: approval policy probe authentication failed (${AUTH_FAILURE_REASON:-unknown-auth-failure}); skipping payroll-events approval action probe." >&2
          else
            jq -n \
              --argjson eventId "$APPROVAL_POLICY_EVENT_ID" \
              '{
                ids: [$eventId]
              }' > "$approval_policy_command_request"

            post_json_allow_status_scoped_authenticated "/api/human-resources/eventos-folha/actions/${APPROVAL_POLICY_ACTION_ID}" "$approval_policy_command_request" "$approval_policy_command_response" "$approval_policy_command_status_file" "$PUBLICATION_TENANT_ID" "$PUBLICATION_ENVIRONMENT"
            approval_policy_command_status="$(cat "$approval_policy_command_status_file")"
            if [[ "$approval_policy_command_status" == "409" ]]; then
              validate_definition_timeline "$approval_policy_definition_id" "$PUBLICATION_TENANT_ID" "$PUBLICATION_ENVIRONMENT" "approval_policy" "approval_policy" "$APPROVAL_POLICY_TARGET_KEY"
              jq -n \
                --arg status "publication-approval-policy-runtime-ready" \
                --arg tenantId "$PUBLICATION_TENANT_ID" \
                --arg resourceKey "$APPROVAL_POLICY_RESOURCE_KEY" \
                --arg actionId "$APPROVAL_POLICY_ACTION_ID" \
                --arg eventId "$APPROVAL_POLICY_EVENT_ID" \
                '{status: $status, tenantId: $tenantId, resourceKey: $resourceKey, actionId: $actionId, eventId: $eventId, rejectedWith: 409}'
            elif [[ "$approval_policy_command_status" == "401" || "$approval_policy_command_status" == "403" ]]; then
              if [[ "$REQUIRE_APPROVAL_POLICY" == "true" ]]; then
                echo "Payroll-events action endpoint requires authentication; approval_policy probe could not be completed." >&2
                cat "$approval_policy_command_response" >&2
                exit 1
              fi
              echo "Warning: payroll-events action endpoint requires authentication; skipping approval_policy command probe." >&2
            else
              echo "Published approval_policy was not reflected by the payroll-events action runtime (HTTP ${approval_policy_command_status})." >&2
              cat "$approval_policy_command_response" >&2
              exit 1
            fi
          fi
        fi
      elif [[ "$approval_policy_publication_status" == "404" || "$approval_policy_publication_status" == "405" ]]; then
        if [[ "$REQUIRE_APPROVAL_POLICY" == "true" ]]; then
          echo "Runtime does not expose /api/praxis/config/domain-rules/publications for approval_policy yet." >&2
          jq -r '.message // .error // empty' "$approval_policy_publication_response" >&2 || true
          exit 1
        fi
        echo "Warning: runtime does not expose approval_policy publication yet; approval_policy rollout is still pending." >&2
      else
        echo "Unexpected approval_policy publication response (HTTP ${approval_policy_publication_status})." >&2
        cat "$approval_policy_publication_response" >&2
        exit 1
      fi
    fi
  elif [[ "$publication_status" == "404" || "$publication_status" == "405" ]]; then
    if [[ "$REQUIRE_PUBLICATION" == "true" || "$REQUIRE_WORKFLOW_ACTION" == "true" || "$REQUIRE_APPROVAL_POLICY" == "true" ]]; then
      echo "Runtime does not expose /api/praxis/config/domain-rules/publications yet." >&2
      jq -r '.message // .error // empty' "$publication_response" >&2 || true
      exit 1
    fi
    echo "Warning: runtime does not expose /api/praxis/config/domain-rules/publications yet; publication rollout is still pending." >&2
  else
    echo "Unexpected domain rule publication response (HTTP ${publication_status})." >&2
    cat "$publication_response" >&2
    exit 1
  fi
fi

encoded_resource_key="$(urlencode "$RESOURCE_KEY")"
encoded_rule_type="$(urlencode "visual_guidance")"
get_json "/api/praxis/config/domain-rules/definitions?resourceKey=${encoded_resource_key}&ruleType=${encoded_rule_type}" "$definitions_list"

definition_count="$(jq --arg ruleKey "$RULE_KEY" '[.[] | select((.ruleKey // "") == $ruleKey)] | length' "$definitions_list")"
if [[ "$definition_count" -eq 0 ]]; then
  echo "Created definition was not returned by filtered lookup." >&2
  jq '[.[] | {id, ruleKey, version, status, resourceKey, ruleType}]' "$definitions_list" >&2
  exit 1
fi

encoded_target_layer="$(urlencode "form_config")"
encoded_target_artifact_type="$(urlencode "praxis-dynamic-form")"
encoded_target_artifact_key="$(urlencode "$TARGET_ARTIFACT_KEY")"
encoded_status="$(urlencode "applied")"
get_json "/api/praxis/config/domain-rules/materializations?targetLayer=${encoded_target_layer}&targetArtifactType=${encoded_target_artifact_type}&targetArtifactKey=${encoded_target_artifact_key}&status=${encoded_status}" "$materializations_list"

materialization_count="$(jq --arg materializationKey "$MATERIALIZATION_KEY" '[.[] | select((.materializationKey // "") == $materializationKey)] | length' "$materializations_list")"
if [[ "$materialization_count" -eq 0 ]]; then
  echo "Created materialization was not returned by filtered lookup." >&2
  jq '[.[] | {id, materializationKey, ruleKey, ruleVersion, targetLayer, targetArtifactType, targetArtifactKey, status}]' "$materializations_list" >&2
  exit 1
fi

echo
jq -n \
  --arg definitionId "$definition_id" \
  --arg materializationId "$materialization_id" \
  --arg ruleKey "$RULE_KEY" \
  --arg materializationKey "$MATERIALIZATION_KEY" \
  --argjson definitionCount "$definition_count" \
  --argjson materializationCount "$materialization_count" \
  '{
    status: "shared-rule-runtime-ready",
    definitionId: $definitionId,
    materializationId: $materializationId,
    ruleKey: $ruleKey,
    materializationKey: $materializationKey,
    definitionStatus: "active",
    materializationStatus: "applied",
    filteredDefinitionMatches: $definitionCount,
    filteredMaterializationMatches: $materializationCount
  }'

echo
echo "Shared domain rule runtime verification completed."
