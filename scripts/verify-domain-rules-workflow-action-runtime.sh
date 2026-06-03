#!/usr/bin/env bash
set -euo pipefail

BACKEND_URL="${BACKEND_URL:-http://localhost:8088}"
ORIGIN="${ORIGIN:-http://localhost:4003}"
TENANT_ID="${TENANT_ID:-domain-rules-workflow-action-$(date -u +%Y%m%d%H%M%S)}"
ENVIRONMENT="${ENVIRONMENT:-dev}"
SERVICE_KEY="${SERVICE_KEY:-praxis-api-quickstart}"
ADMIN_USERNAME="${ADMIN_USERNAME:-admin}"
ADMIN_PASSWORD="${ADMIN_PASSWORD:-${PRACTICE_TEMP_PASSWORD:-changeMe!}}"
RESOURCE_KEY="${RESOURCE_KEY:-human-resources.folhas-pagamento}"
CONTEXT_KEY="${CONTEXT_KEY:-human-resources}"
ACTION_ID="${ACTION_ID:-mark-paid}"
TARGET_KEY="${TARGET_KEY:-${RESOURCE_KEY}:${ACTION_ID}}"
RULE_KEY="${RULE_KEY:-${RESOURCE_KEY}.rule.mark-paid-compliance.$(date -u +%Y%m%d%H%M%S)}"
RESOURCE_ID="${RESOURCE_ID:-${WORKFLOW_ACTION_FOLHA_ID:-2}}"
BLOCKED_STATES_JSON="${BLOCKED_STATES_JSON:-[\"PROGRAMADA\"]}"
POLICY_SUMMARY="${POLICY_SUMMARY:-Bloquear a action mark-paid enquanto a folha programada aguarda revisao governada de compliance.}"
POLICY_MESSAGE="${POLICY_MESSAGE:-Pagamento bloqueado por decisao governada ate revisao de compliance.}"
SEMANTIC_OWNER="${SEMANTIC_OWNER:-hr-operations-owner}"
STEWARD="${STEWARD:-payroll-compliance}"
COMMAND_PATH="${COMMAND_PATH:-/api/human-resources/folhas-pagamento/${RESOURCE_ID}/actions/${ACTION_ID}}"

TMPDIR_RUN="$(mktemp -d "${TMPDIR:-/tmp}/praxis-workflow-action-runtime.XXXXXX")"
trap 'rm -rf "$TMPDIR_RUN"' EXIT

post_config_json() {
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

echo "Verifying workflow_action runtime enforcement."
echo "BACKEND_URL=${BACKEND_URL}"
echo "TENANT_ID=${TENANT_ID}"
echo "ENVIRONMENT=${ENVIRONMENT}"
echo "TARGET_KEY=${TARGET_KEY}"
echo "COMMAND_PATH=${COMMAND_PATH}"
echo "RULE_KEY=${RULE_KEY}"

cat > "$TMPDIR_RUN/definition.json" <<JSON
{
  "ruleKey": "${RULE_KEY}",
  "ruleType": "workflow_action_policy",
  "status": "approved",
  "contextKey": "${CONTEXT_KEY}",
  "resourceKey": "${RESOURCE_KEY}",
  "serviceKey": "${SERVICE_KEY}",
  "semanticOwner": "${SEMANTIC_OWNER}",
  "steward": "${STEWARD}",
  "definition": {
    "summary": "${POLICY_SUMMARY}",
    "recommendedAuthoringFlow": "shared_rule_authoring"
  },
  "parameters": {
    "workflowAction": {
      "resourceKey": "${RESOURCE_KEY}",
      "actionId": "${ACTION_ID}"
    },
    "requiredStates": ${BLOCKED_STATES_JSON},
    "message": "${POLICY_MESSAGE}"
  },
  "condition": {
    "in": [
      {"var": "state"},
      ${BLOCKED_STATES_JSON}
    ]
  },
  "governance": {
    "requiredApprovals": []
  },
  "createdByType": "llm",
  "createdBy": "runtime-smoke"
}
JSON

echo
echo "Creating workflow_action shared rule definition."
post_config_json "/api/praxis/config/domain-rules/definitions" "$TMPDIR_RUN/definition.json" "$TMPDIR_RUN/definition-response.json"
definition_id="$(jq -r '.id // empty' "$TMPDIR_RUN/definition-response.json")"
if [[ -z "$definition_id" ]]; then
  echo "Invalid workflow_action definition response." >&2
  jq '{id, ruleKey, version, status}' "$TMPDIR_RUN/definition-response.json" >&2
  exit 1
fi
jq '{status: "definition-created", id, ruleKey, ruleType, resourceKey, statusValue: .status}' "$TMPDIR_RUN/definition-response.json"

cat > "$TMPDIR_RUN/publication.json" <<JSON
{
  "ruleDefinitionId": "${definition_id}",
  "applyEligibleMaterializations": true,
  "publishedByType": "human",
  "publishedBy": "${STEWARD}"
}
JSON

echo
echo "Publishing workflow_action shared rule definition ${definition_id}."
post_config_json "/api/praxis/config/domain-rules/publications" "$TMPDIR_RUN/publication.json" "$TMPDIR_RUN/publication-response.json"
workflow_action_materialization_count="$(jq --arg targetKey "$TARGET_KEY" '[
  (.materializations // [])[]
  | select((.targetLayer // "") == "workflow_action")
  | select((.targetArtifactType // "") == "resource-workflow-action")
  | select((.targetArtifactKey // "") == $targetKey)
  | select((.status // "") == "applied")
  | select((.materializedPayload.kind // "") == "workflow_action_policy")
  | select((.sourceHash // "") != "")
] | length' "$TMPDIR_RUN/publication-response.json")"
if [[ "$workflow_action_materialization_count" -eq 0 ]]; then
  echo "Publication did not derive an applied workflow_action materialization for ${TARGET_KEY}." >&2
  jq '{publicationId, publicationStatus, publicationReadiness, ruleKey, definition, materializations, explainability}' "$TMPDIR_RUN/publication-response.json" >&2
  exit 1
fi
jq '{status: "publication-completed", publicationId, publicationStatus, publicationReadiness, definitionStatus: .definition.status, materializations: (.materializations | map({targetLayer, targetArtifactType, targetArtifactKey, status, kind: .materializedPayload.kind, sourceHash}))}' "$TMPDIR_RUN/publication-response.json"

cookie_jar="$TMPDIR_RUN/cookies.txt"
cat > "$TMPDIR_RUN/login.json" <<JSON
{
  "username": "${ADMIN_USERNAME}",
  "password": "${ADMIN_PASSWORD}"
}
JSON
login_status="$(curl -sS "${BACKEND_URL%/}/auth/login" \
  -c "$cookie_jar" \
  -H "Origin: ${ORIGIN}" \
  -H "Content-Type: application/json" \
  --data-binary "@${TMPDIR_RUN}/login.json" \
  -o /dev/null \
  -w '%{http_code}')"
if [[ "$login_status" != "200" && "$login_status" != "204" ]]; then
  echo "Could not authenticate workflow_action probe with /auth/login (HTTP ${login_status})." >&2
  exit 1
fi

cat > "$TMPDIR_RUN/workflow-action.json" <<JSON
{
  "justificativa": "Runtime smoke must be blocked by governed workflow_action policy"
}
JSON

echo
echo "Verifying workflow action command is blocked by applied policy."
curl -sS "${BACKEND_URL%/}${COMMAND_PATH}" \
  -b "$cookie_jar" \
  -c "$cookie_jar" \
  -H "Origin: ${ORIGIN}" \
  -H "X-Tenant-ID: ${TENANT_ID}" \
  -H "X-Env: ${ENVIRONMENT}" \
  -H "Content-Type: application/json" \
  --data-binary "@${TMPDIR_RUN}/workflow-action.json" \
  -o "$TMPDIR_RUN/workflow-action-response.json" \
  -w '%{http_code}' > "$TMPDIR_RUN/workflow-action-status.txt"
workflow_action_status="$(cat "$TMPDIR_RUN/workflow-action-status.txt")"
if [[ "$workflow_action_status" != "409" ]]; then
  echo "Published workflow_action policy was not reflected by the runtime command (HTTP ${workflow_action_status})." >&2
  cat "$TMPDIR_RUN/workflow-action-response.json" >&2
  exit 1
fi

jq -n \
  --arg tenantId "$TENANT_ID" \
  --arg definitionId "$definition_id" \
  --arg resourceKey "$RESOURCE_KEY" \
  --arg actionId "$ACTION_ID" \
  --arg resourceId "$RESOURCE_ID" \
  --arg targetKey "$TARGET_KEY" \
  --argjson materializations "$workflow_action_materialization_count" \
  '{
    status: "workflow-action-runtime-ready",
    tenantId: $tenantId,
    definitionId: $definitionId,
    resourceKey: $resourceKey,
    actionId: $actionId,
    resourceId: $resourceId,
    targetKey: $targetKey,
    workflowActionMaterializations: $materializations,
    rejectedWith: 409
  }'

echo
echo "Workflow action runtime enforcement verification completed."
