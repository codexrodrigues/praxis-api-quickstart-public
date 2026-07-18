#!/usr/bin/env bash
set -euo pipefail

BACKEND_URL="${BACKEND_URL:-http://localhost:8088}"
ORIGIN="${ORIGIN:-http://localhost:4003}"
TENANT_ID="${TENANT_ID:-domain-rules-approval-policy-$(date -u +%Y%m%d%H%M%S)}"
ENVIRONMENT="${ENVIRONMENT:-dev}"
SERVICE_KEY="${SERVICE_KEY:-praxis-api-quickstart}"
ADMIN_USERNAME="${ADMIN_USERNAME:-admin}"
ADMIN_PASSWORD="${ADMIN_PASSWORD:-${PRACTICE_TEMP_PASSWORD:-changeMe!}}"
RESOURCE_KEY="${RESOURCE_KEY:-human-resources.eventos-folha}"
CONTEXT_KEY="${CONTEXT_KEY:-human-resources}"
ACTION_ID="${ACTION_ID:-bulk-approve}"
TARGET_KEY="${TARGET_KEY:-${RESOURCE_KEY}:${ACTION_ID}}"
RULE_KEY="${RULE_KEY:-${RESOURCE_KEY}.rule.bulk-approve-approval.$(date -u +%Y%m%d%H%M%S)}"
EVENT_ID="${EVENT_ID:-1}"
REQUIRED_APPROVALS_JSON="${REQUIRED_APPROVALS_JSON:-[\"payroll-manager\"]}"
APPROVAL_GROUPS_JSON="${APPROVAL_GROUPS_JSON:-[\"hr-payroll\"]}"
APPROVER_CONTEXT="${APPROVER_CONTEXT:-payroll-events}"
POLICY_SUMMARY="${POLICY_SUMMARY:-Exigir aprovacao gerencial governada antes de aprovar eventos de folha em lote.}"
POLICY_MESSAGE="${POLICY_MESSAGE:-Aprovacao em massa exige decisao gerencial governada.}"
SEMANTIC_OWNER="${SEMANTIC_OWNER:-hr-operations-owner}"
STEWARD="${STEWARD:-payroll-manager}"
COMMAND_PATH="${COMMAND_PATH:-/api/human-resources/eventos-folha/actions/${ACTION_ID}}"

TMPDIR_RUN="$(mktemp -d "${TMPDIR:-/tmp}/praxis-approval-policy-runtime.XXXXXX")"
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

echo "Verifying approval_policy runtime enforcement."
echo "BACKEND_URL=${BACKEND_URL}"
echo "TENANT_ID=${TENANT_ID}"
echo "ENVIRONMENT=${ENVIRONMENT}"
echo "TARGET_KEY=${TARGET_KEY}"
echo "COMMAND_PATH=${COMMAND_PATH}"
echo "RULE_KEY=${RULE_KEY}"

cat > "$TMPDIR_RUN/definition.json" <<JSON
{
  "ruleKey": "${RULE_KEY}",
  "ruleType": "approval_policy",
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
    "approvalAction": {
      "resourceKey": "${RESOURCE_KEY}",
      "actionId": "${ACTION_ID}"
    },
    "requiredApprovals": ${REQUIRED_APPROVALS_JSON},
    "approvalGroups": ${APPROVAL_GROUPS_JSON},
    "approverContext": "${APPROVER_CONTEXT}",
    "message": "${POLICY_MESSAGE}"
  },
  "condition": {
    ">": [
      {"var": "selectedCount"},
      0
    ]
  },
  "governance": {
    "requiredApprovals": []
  }
}
JSON

echo
echo "Creating approval_policy shared rule definition."
post_config_json "/api/praxis/config/domain-rules/definitions" "$TMPDIR_RUN/definition.json" "$TMPDIR_RUN/definition-response.json"
definition_id="$(jq -r '.id // empty' "$TMPDIR_RUN/definition-response.json")"
if [[ -z "$definition_id" ]]; then
  echo "Invalid approval_policy definition response." >&2
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
echo "Publishing approval_policy shared rule definition ${definition_id}."
post_config_json "/api/praxis/config/domain-rules/publications" "$TMPDIR_RUN/publication.json" "$TMPDIR_RUN/publication-response.json"
approval_policy_materialization_count="$(jq --arg targetKey "$TARGET_KEY" '[
  (.materializations // [])[]
  | select((.targetLayer // "") == "approval_policy")
  | select((.targetArtifactType // "") == "resource-action-approval")
  | select((.targetArtifactKey // "") == $targetKey)
  | select((.status // "") == "applied")
  | select((.materializedPayload.kind // "") == "approval_policy")
  | select((.sourceHash // "") != "")
] | length' "$TMPDIR_RUN/publication-response.json")"
if [[ "$approval_policy_materialization_count" -eq 0 ]]; then
  echo "Publication did not derive an applied approval_policy materialization for ${TARGET_KEY}." >&2
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
  echo "Could not authenticate approval_policy probe with /auth/login (HTTP ${login_status})." >&2
  exit 1
fi

cat > "$TMPDIR_RUN/approval-action.json" <<JSON
{
  "ids": [${EVENT_ID}]
}
JSON

echo
echo "Verifying approval-gated command is blocked by applied policy."
curl -sS "${BACKEND_URL%/}${COMMAND_PATH}" \
  -b "$cookie_jar" \
  -c "$cookie_jar" \
  -H "Origin: ${ORIGIN}" \
  -H "X-Tenant-ID: ${TENANT_ID}" \
  -H "X-Env: ${ENVIRONMENT}" \
  -H "Content-Type: application/json" \
  --data-binary "@${TMPDIR_RUN}/approval-action.json" \
  -o "$TMPDIR_RUN/approval-action-response.json" \
  -w '%{http_code}' > "$TMPDIR_RUN/approval-action-status.txt"
approval_action_status="$(cat "$TMPDIR_RUN/approval-action-status.txt")"
if [[ "$approval_action_status" != "409" ]]; then
  echo "Published approval_policy was not reflected by the runtime command (HTTP ${approval_action_status})." >&2
  cat "$TMPDIR_RUN/approval-action-response.json" >&2
  exit 1
fi

jq -n \
  --arg tenantId "$TENANT_ID" \
  --arg definitionId "$definition_id" \
  --arg resourceKey "$RESOURCE_KEY" \
  --arg actionId "$ACTION_ID" \
  --arg eventId "$EVENT_ID" \
  --arg targetKey "$TARGET_KEY" \
  --argjson materializations "$approval_policy_materialization_count" \
  '{
    status: "approval-policy-runtime-ready",
    tenantId: $tenantId,
    definitionId: $definitionId,
    resourceKey: $resourceKey,
    actionId: $actionId,
    eventId: $eventId,
    targetKey: $targetKey,
    approvalPolicyMaterializations: $materializations,
    rejectedWith: 409
  }'

echo
echo "Approval policy runtime enforcement verification completed."
