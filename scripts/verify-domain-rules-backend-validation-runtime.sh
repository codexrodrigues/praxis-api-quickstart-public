#!/usr/bin/env bash
set -euo pipefail

BACKEND_URL="${BACKEND_URL:-http://localhost:8088}"
ORIGIN="${ORIGIN:-http://localhost:4003}"
TENANT_ID="${TENANT_ID:-domain-rules-backend-validation-$(date -u +%Y%m%d%H%M%S)}"
ENVIRONMENT="${ENVIRONMENT:-dev}"
SERVICE_KEY="${SERVICE_KEY:-praxis-api-quickstart}"
ADMIN_USERNAME="${ADMIN_USERNAME:-admin}"
ADMIN_PASSWORD="${ADMIN_PASSWORD:-${PRACTICE_TEMP_PASSWORD:-changeMe!}}"
RESOURCE_KEY="${RESOURCE_KEY:-procurement.purchase-orders}"
CONTEXT_KEY="${CONTEXT_KEY:-procurement}"
RULE_KEY="${RULE_KEY:-${RESOURCE_KEY}.rule.supplier-backend-validation.$(date -u +%Y%m%d%H%M%S)}"
BLOCKED_STATUSES_JSON="${BLOCKED_STATUSES_JSON:-[\"BLOCKED\"]}"
SUPPLIER_ID="${SUPPLIER_ID:-11}"
COMPANY_ID="${COMPANY_ID:-1}"
PRODUCT_ID="${PRODUCT_ID:-30}"

TMPDIR_RUN="$(mktemp -d "${TMPDIR:-/tmp}/praxis-backend-validation-runtime.XXXXXX")"
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

echo "Verifying backend_validation runtime enforcement."
echo "BACKEND_URL=${BACKEND_URL}"
echo "TENANT_ID=${TENANT_ID}"
echo "ENVIRONMENT=${ENVIRONMENT}"
echo "RESOURCE_KEY=${RESOURCE_KEY}"
echo "RULE_KEY=${RULE_KEY}"

cat > "$TMPDIR_RUN/definition.json" <<JSON
{
  "ruleKey": "${RULE_KEY}",
  "ruleType": "validation",
  "status": "approved",
  "contextKey": "${CONTEXT_KEY}",
  "resourceKey": "${RESOURCE_KEY}",
  "serviceKey": "${SERVICE_KEY}",
  "semanticOwner": "procurement-owner",
  "steward": "procurement-owner",
  "definition": {
    "summary": "Validar no backend que pedidos de compra nao usem fornecedor em status bloqueado.",
    "recommendedAuthoringFlow": "shared_rule_authoring"
  },
  "parameters": {
    "referenceResourceKey": "procurement.suppliers",
    "referenceField": "supplierId",
    "statusPropertyPath": "status",
    "blockedStatuses": ${BLOCKED_STATUSES_JSON},
    "validationMessageTemplate": "Fornecedor indisponivel para pedidos de compra"
  },
  "condition": {
    "in": [
      {"var": "supplier.status"},
      ${BLOCKED_STATUSES_JSON}
    ]
  },
  "governance": {
    "requiredApprovals": []
  }
}
JSON

echo
echo "Creating backend_validation shared rule definition."
post_config_json "/api/praxis/config/domain-rules/definitions" "$TMPDIR_RUN/definition.json" "$TMPDIR_RUN/definition-response.json"
definition_id="$(jq -r '.id // empty' "$TMPDIR_RUN/definition-response.json")"
if [[ -z "$definition_id" ]]; then
  echo "Invalid backend_validation definition response." >&2
  jq '{id, ruleKey, version, status}' "$TMPDIR_RUN/definition-response.json" >&2
  exit 1
fi
jq '{status: "definition-created", id, ruleKey, ruleType, resourceKey, statusValue: .status}' "$TMPDIR_RUN/definition-response.json"

cat > "$TMPDIR_RUN/publication.json" <<JSON
{
  "ruleDefinitionId": "${definition_id}",
  "applyEligibleMaterializations": true,
  "publishedByType": "human",
  "publishedBy": "procurement-owner"
}
JSON

echo
echo "Publishing backend_validation shared rule definition ${definition_id}."
post_config_json "/api/praxis/config/domain-rules/publications" "$TMPDIR_RUN/publication.json" "$TMPDIR_RUN/publication-response.json"
backend_validation_materialization_count="$(jq --arg resourceKey "$RESOURCE_KEY" '[
  (.materializations // [])[]
  | select((.targetLayer // "") == "backend_validation")
  | select((.targetArtifactType // "") == "resource-validation")
  | select((.targetArtifactKey // "") == $resourceKey)
  | select((.status // "") == "applied")
  | select((.materializedPayload.kind // "") == "resource_validation_policy")
] | length' "$TMPDIR_RUN/publication-response.json")"
if [[ "$backend_validation_materialization_count" -eq 0 ]]; then
  echo "Publication did not derive an applied backend_validation materialization for ${RESOURCE_KEY}." >&2
  jq '{publicationId, publicationStatus, publicationReadiness, ruleKey, definition, materializations, explainability}' "$TMPDIR_RUN/publication-response.json" >&2
  exit 1
fi
jq '{status: "publication-completed", publicationId, publicationStatus, publicationReadiness, definitionStatus: .definition.status, materializations: (.materializations | map({targetLayer, targetArtifactType, targetArtifactKey, status, kind: .materializedPayload.kind}))}' "$TMPDIR_RUN/publication-response.json"

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
  echo "Could not authenticate command probe with /auth/login (HTTP ${login_status})." >&2
  exit 1
fi

cat > "$TMPDIR_RUN/purchase-order.json" <<JSON
{
  "companyId": ${COMPANY_ID},
  "supplierId": ${SUPPLIER_ID},
  "productId": ${PRODUCT_ID},
  "orderDate": "2026-04-24",
  "currency": "BRL",
  "quantity": 1
}
JSON

echo
echo "Verifying purchase-order command is blocked by applied backend_validation policy."
curl -sS "${BACKEND_URL%/}/api/procurement/purchase-orders" \
  -b "$cookie_jar" \
  -c "$cookie_jar" \
  -H "Origin: ${ORIGIN}" \
  -H "X-Tenant-ID: ${TENANT_ID}" \
  -H "X-Env: ${ENVIRONMENT}" \
  -H "Content-Type: application/json" \
  --data-binary "@${TMPDIR_RUN}/purchase-order.json" \
  -o "$TMPDIR_RUN/purchase-order-response.json" \
  -w '%{http_code}' > "$TMPDIR_RUN/purchase-order-status.txt"
purchase_order_status="$(cat "$TMPDIR_RUN/purchase-order-status.txt")"
if [[ "$purchase_order_status" != "409" ]]; then
  echo "Published backend_validation policy was not reflected by the purchase-order command runtime (HTTP ${purchase_order_status})." >&2
  cat "$TMPDIR_RUN/purchase-order-response.json" >&2
  exit 1
fi

jq -n \
  --arg tenantId "$TENANT_ID" \
  --arg definitionId "$definition_id" \
  --arg resourceKey "$RESOURCE_KEY" \
  --arg supplierId "$SUPPLIER_ID" \
  --argjson materializations "$backend_validation_materialization_count" \
  '{
    status: "backend-validation-runtime-ready",
    tenantId: $tenantId,
    definitionId: $definitionId,
    resourceKey: $resourceKey,
    supplierId: $supplierId,
    backendValidationMaterializations: $materializations,
    rejectedWith: 409
  }'

echo
echo "Backend validation runtime enforcement verification completed."
