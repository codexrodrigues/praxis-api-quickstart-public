#!/usr/bin/env bash
set -euo pipefail

BACKEND_URL="${BACKEND_URL:-http://localhost:8088}"
TENANT_ID="${TENANT_ID:-default}"
USER_ID="${USER_ID:-codex-smoke}"
ENVIRONMENT="${ENVIRONMENT:-dev}"
ORIGIN="${ORIGIN:-https://praxisui-dev.web.app}"
SERVICE_KEY="${SERVICE_KEY:-praxis-service}"
AUTHORING_QUERY="${AUTHORING_QUERY:-dashboard de folha de pagamento com salario}"
AUTHORING_ARTIFACT_KIND="${AUTHORING_ARTIFACT_KIND:-dashboard}"
FORM_RULE_AUTHORING_QUERY="${FORM_RULE_AUTHORING_QUERY:-formulario LGPD para funcionarios com orientacao visual de privacidade}"
FORM_RULE_AUTHORING_ARTIFACT_KIND="${FORM_RULE_AUTHORING_ARTIFACT_KIND:-form}"
AUTHORING_LIMIT="${AUTHORING_LIMIT:-5}"
REQUIRE_AUTHORING_FLOW="${REQUIRE_AUTHORING_FLOW:-auto}"
REQUIRE_GOVERNED_CONTEXT="${REQUIRE_GOVERNED_CONTEXT:-auto}"
PROVIDER="${PROVIDER:-openai}"
MODEL="${MODEL:-${PRAXIS_AI_OPENAI_MODEL:-${SPRING_AI_OPENAI_CHAT_OPTIONS_MODEL:-gpt-5-mini}}}"
CONTRACT_SCHEMA_PATH="${CONTRACT_SCHEMA_PATH:-docs/contracts/domain-authoring-context-hints.schema.json}"
CONTRACT_EXAMPLE_PATH="${CONTRACT_EXAMPLE_PATH:-payloads/domain_authoring_context_hints.example.json}"

usage() {
  cat >&2 <<'USAGE'
Usage:
  scripts/verify-domain-catalog-authoring-runtime.sh

Examples:
  BACKEND_URL=http://localhost:8088 scripts/verify-domain-catalog-authoring-runtime.sh
  BACKEND_URL=https://praxis-api-quickstart.onrender.com scripts/verify-domain-catalog-authoring-runtime.sh

The script is read-only. It verifies the runtime LLM context path:
  1. /api/praxis/config/domain-catalog/context is scoped by resourceKey;
  2. sensitive governance items remain exposed as governed summaries;
  3. /api/praxis/config/ai/authoring/resource-candidates returns quick replies
     with contextHints.domainCatalog minimum contract fields and relationship
     retrieval hints;
  4. form/LGPD authoring candidates carry intent, itemTypes and the shared-rule
     authoring flow hint for reviewed semantic rule drafts. In auto rollout
     mode, a legacy recommendedOperation hint is tolerated only as evidence
     that the published host has not been upgraded yet; it is not the
     canonical authoring path.
  5. when REQUIRE_GOVERNED_CONTEXT=true, /api/praxis/config/ai/authoring/intent-resolution
     must expose llmDiagnostics.request.contextBundle.governedDomainContext
     with resolutionStatus=resolved. In auto mode this check runs only when
     a provider API key is available.

It does not ingest catalogs or run database migrations.
USAGE
}

provider_api_key() {
  case "$PROVIDER" in
    openai)
      printf '%s' "${PRAXIS_AI_OPENAI_API_KEY:-${SPRING_AI_OPENAI_API_KEY:-${OPENAI_API_KEY:-}}}"
      ;;
    gemini)
      printf '%s' "${PRAXIS_AI_GEMINI_API_KEY:-${GOOGLE_API_KEY:-}}"
      ;;
    *)
      printf '%s' ""
      ;;
  esac
}

urlencode() {
  jq -nr --arg value "$1" '$value|@uri'
}

fetch_context() {
  local resource_key="$1"
  local query="$2"
  local output_file="$3"
  local encoded_service_key
  local encoded_resource_key
  local encoded_query

  encoded_service_key="$(urlencode "$SERVICE_KEY")"
  encoded_resource_key="$(urlencode "$resource_key")"
  encoded_query="$(urlencode "$query")"

  curl -fsS \
    "${BACKEND_URL%/}/api/praxis/config/domain-catalog/context?serviceKey=${encoded_service_key}&resourceKey=${encoded_resource_key}&type=governance&q=${encoded_query}&limit=5" \
    -H "Origin: ${ORIGIN}" \
    -H "X-Tenant-ID: ${TENANT_ID}" \
    -H "X-Env: ${ENVIRONMENT}" \
    -o "$output_file"
}

validate_context() {
  local resource_key="$1"
  local query="$2"
  local require_governed_summary="$3"
  local output_file="$4"

  fetch_context "$resource_key" "$query" "$output_file"

  local release_key
  local item_count
  local expected_release
  local governed_summary_count

  release_key="$(jq -r '.release.releaseKey // empty' "$output_file")"
  item_count="$(jq '.items | length' "$output_file")"
  expected_release="$(jq -r --arg resourceKey "$resource_key" '.release.releaseKey | contains($resourceKey)' "$output_file")"
  governed_summary_count="$(jq '[.items[] | select((.payload.payloadMode // "") == "governed-summary")] | length' "$output_file")"

  if [[ -z "$release_key" || "$item_count" -eq 0 || "$expected_release" != "true" ]]; then
    echo "Invalid resource-scoped context for resourceKey=${resource_key} query=${query}" >&2
    jq '{release: .release.releaseKey, itemCount: (.items|length)}' "$output_file" >&2
    return 1
  fi

  if [[ "$require_governed_summary" == "true" && "$governed_summary_count" -eq 0 ]]; then
    echo "Expected governed-summary payload for resourceKey=${resource_key} query=${query}" >&2
    jq '{release: .release.releaseKey, items: [.items[] | {itemKey, payloadMode: .payload.payloadMode, aiUsage: .payload.aiUsage}]}' "$output_file" >&2
    return 1
  fi

  jq --arg resourceKey "$resource_key" --arg query "$query" '{
    status: "context-ready",
    resourceKey: $resourceKey,
    query: $query,
    release: .release.releaseKey,
    itemCount: (.items | length),
    governedSummaryCount: ([.items[] | select((.payload.payloadMode // "") == "governed-summary")] | length)
  }' "$output_file"
}

validate_authoring_candidates() {
  local output_file="$1"
  local authoring_query="$2"
  local artifact_kind="$3"
  local expected_resource_key="$4"
  local expected_authoring_flow="${5:-}"
  local expected_item_type="${6:-}"
  local legacy_materialization_operation="${7:-}"

  jq -n \
    --arg retrievalQuery "$authoring_query" \
    --arg artifactKind "$artifact_kind" \
    --argjson limit "$AUTHORING_LIMIT" \
    '{retrievalQuery: $retrievalQuery, artifactKind: $artifactKind, limit: $limit}' \
    | curl -fsS "${BACKEND_URL%/}/api/praxis/config/ai/authoring/resource-candidates" \
        -H "Origin: ${ORIGIN}" \
        -H "X-Tenant-ID: ${TENANT_ID}" \
        -H "X-User-ID: ${USER_ID}" \
        -H "X-Env: ${ENVIRONMENT}" \
        -H "Content-Type: application/json" \
        --data-binary @- \
        -o "$output_file"

  local valid
  local quick_reply_count
  local resource_key_count
  local contract_shape_count
  local payroll_resource_count
  local relationship_count
  local intent_count
  local expected_authoring_flow_count
  local legacy_materialization_operation_count
  local expected_item_type_count

  valid="$(jq -r '.valid // false' "$output_file")"
  quick_reply_count="$(jq '.quickReplies | length' "$output_file")"
  resource_key_count="$(jq '[.quickReplies[] | select((.contextHints.domainCatalog.resourceKey // "") != "")] | length' "$output_file")"
  contract_shape_count="$(jq '[.quickReplies[] | select(
    (.contextHints.domainCatalog.serviceKey // "") != "" and
    (.contextHints.domainCatalog.resourceKey // "") != "" and
    (.contextHints.domainCatalog.type // "") != "" and
    (.contextHints.domainCatalog.query // "") != "" and
    ((.contextHints.domainCatalog.limit // 0) > 0)
  )] | length' "$output_file")"
  payroll_resource_count="$(jq --arg resourceKey "$expected_resource_key" '[.quickReplies[] | select((.contextHints.domainCatalog.resourceKey // "") == $resourceKey)] | length' "$output_file")"
  relationship_count="$(jq '[.quickReplies[] | select((.contextHints.domainCatalog.relationships.enabled // false) == true and (.contextHints.domainCatalog.relationships.federated // false) == true)] | length' "$output_file")"
  intent_count="$(jq '[.quickReplies[] | select((.contextHints.domainCatalog.intent // "") == "authoring")] | length' "$output_file")"
  expected_authoring_flow_count="$(jq --arg flow "$expected_authoring_flow" 'if $flow == "" then 1 else [.quickReplies[] | select((.contextHints.domainCatalog.recommendedAuthoringFlow // "") == $flow)] | length end' "$output_file")"
  legacy_materialization_operation_count="$(jq --arg operation "$legacy_materialization_operation" 'if $operation == "" then 0 else [.quickReplies[] | select((.contextHints.domainCatalog.recommendedOperation // "") == $operation)] | length end' "$output_file")"
  expected_item_type_count="$(jq --arg itemType "$expected_item_type" 'if $itemType == "" then 1 else [.quickReplies[] | select((.contextHints.domainCatalog.itemTypes // []) | index($itemType))] | length end' "$output_file")"

  local authoring_flow_ready="true"
  if [[ -n "$expected_authoring_flow" ]]; then
    case "$REQUIRE_AUTHORING_FLOW" in
      true)
        [[ "$expected_authoring_flow_count" -gt 0 ]] || authoring_flow_ready="false"
        ;;
      false)
        authoring_flow_ready="true"
        ;;
      auto)
        if [[ "$expected_authoring_flow_count" -eq 0 && "$legacy_materialization_operation_count" -eq 0 ]]; then
          authoring_flow_ready="false"
        fi
        ;;
      *)
        echo "Invalid REQUIRE_AUTHORING_FLOW=$REQUIRE_AUTHORING_FLOW (expected auto|true|false)." >&2
        return 2
        ;;
    esac
  fi

  if [[ "$valid" != "true" || "$quick_reply_count" -eq 0 || "$resource_key_count" -eq 0 || "$contract_shape_count" -ne "$quick_reply_count" || "$relationship_count" -eq 0 || "$intent_count" -ne "$quick_reply_count" || "$authoring_flow_ready" != "true" || "$expected_item_type_count" -eq 0 ]]; then
    echo "Invalid authoring resource candidates response." >&2
    jq '{valid, quickReplyCount: (.quickReplies|length), contractShapeCount: ([.quickReplies[] | select((.contextHints.domainCatalog.serviceKey // "") != "" and (.contextHints.domainCatalog.resourceKey // "") != "" and (.contextHints.domainCatalog.type // "") != "" and (.contextHints.domainCatalog.query // "") != "" and ((.contextHints.domainCatalog.limit // 0) > 0))] | length), quickReplies: [.quickReplies[] | {id, resourcePath: .contextHints.resourcePath, domainCatalog: .contextHints.domainCatalog}]}' "$output_file" >&2
    return 1
  fi

  if [[ "$payroll_resource_count" -eq 0 ]]; then
    echo "Expected at least one quick reply for resourceKey=${expected_resource_key}." >&2
    jq '{quickReplies: [.quickReplies[] | {id, resourcePath: .contextHints.resourcePath, resourceKey: .contextHints.domainCatalog.resourceKey}]}' "$output_file" >&2
    return 1
  fi

  jq '{
    status: "authoring-ready",
    valid,
    retrievalQuery,
    artifactKind,
    candidateCount: (.candidates | length),
    quickReplyCount: (.quickReplies | length),
    contractShapeCount: ([.quickReplies[] | select((.contextHints.domainCatalog.serviceKey // "") != "" and (.contextHints.domainCatalog.resourceKey // "") != "" and (.contextHints.domainCatalog.type // "") != "" and (.contextHints.domainCatalog.query // "") != "" and ((.contextHints.domainCatalog.limit // 0) > 0))] | length),
    resourceKeys: ([.quickReplies[].contextHints.domainCatalog.resourceKey] | unique),
    relationshipHintCount: ([.quickReplies[] | select((.contextHints.domainCatalog.relationships.enabled // false) == true and (.contextHints.domainCatalog.relationships.federated // false) == true)] | length),
    authoringIntentCount: ([.quickReplies[] | select((.contextHints.domainCatalog.intent // "") == "authoring")] | length),
    recommendedAuthoringFlows: ([.quickReplies[].contextHints.domainCatalog.recommendedAuthoringFlow // empty] | unique),
    legacyRecommendedOperations: ([.quickReplies[].contextHints.domainCatalog.recommendedOperation // empty] | unique),
    itemTypes: ([.quickReplies[].contextHints.domainCatalog.itemTypes[]?] | unique)
  }' "$output_file"
}

validate_governed_context_intent_resolution() {
  local candidates_file="$1"
  local output_file="$2"
  local api_key
  api_key="$(provider_api_key)"

  case "$REQUIRE_GOVERNED_CONTEXT" in
    true|false|auto)
      ;;
    *)
      echo "Invalid REQUIRE_GOVERNED_CONTEXT=$REQUIRE_GOVERNED_CONTEXT (expected auto|true|false)." >&2
      return 2
      ;;
  esac

  if [[ "$REQUIRE_GOVERNED_CONTEXT" == "false" ]]; then
    jq -n '{status: "governed-context-skipped", domainContextGovernanceAuthoringSeen: false, reason: "disabled"}'
    return 0
  fi

  if [[ -z "$api_key" ]]; then
    if [[ "$REQUIRE_GOVERNED_CONTEXT" == "true" ]]; then
      echo "Provider API key is required for governed context intent-resolution proof." >&2
      return 1
    fi
    jq -n --arg provider "$PROVIDER" '{
      status: "governed-context-skipped",
      domainContextGovernanceAuthoringSeen: false,
      provider: $provider,
      reason: "provider-api-key-not-set"
    }'
    return 0
  fi

  local domain_catalog
  domain_catalog="$(jq -c --arg resourceKey "human-resources.funcionarios" '
    [.quickReplies[]
      | select((.contextHints.domainCatalog.resourceKey // "") == $resourceKey)
      | .contextHints.domainCatalog][0] // empty
  ' "$candidates_file")"

  if [[ -z "$domain_catalog" || "$domain_catalog" == "null" ]]; then
    echo "Could not find domainCatalog context hints for human-resources.funcionarios." >&2
    return 1
  fi

  jq -n \
    --arg userPrompt "$FORM_RULE_AUTHORING_QUERY" \
    --arg provider "$PROVIDER" \
    --arg model "$MODEL" \
    --arg apiKey "$api_key" \
    --argjson domainCatalog "$domain_catalog" \
    '{
      userPrompt: $userPrompt,
      targetApp: "praxis-ui-angular",
      targetComponentId: "praxis-dynamic-page-builder",
      currentRoute: "/page-builder-ia",
      currentPage: {},
      provider: $provider,
      model: $model,
      apiKey: $apiKey,
      sessionId: "domain-context-governance-authoring-smoke",
      clientTurnId: "domain-context-governance-authoring-smoke-1",
      contextHints: {
        includeLlmDiagnostics: true,
        domainCatalog: $domainCatalog
      }
    }' \
    | curl -fsS "${BACKEND_URL%/}/api/praxis/config/ai/authoring/intent-resolution" \
        -H "Origin: ${ORIGIN}" \
        -H "X-Tenant-ID: ${TENANT_ID}" \
        -H "X-User-ID: ${USER_ID}" \
        -H "X-Env: ${ENVIRONMENT}" \
        -H "Content-Type: application/json" \
        --data-binary @- \
        -o "$output_file"

  local resolution_status
  local available
  local policy_profile
  local requested_present
  local prompt_block_length
  resolution_status="$(jq -r '.llmDiagnostics.request.contextBundle.governedDomainContext.resolutionStatus // empty' "$output_file")"
  available="$(jq -r '.llmDiagnostics.request.contextBundle.governedDomainContext.available // false' "$output_file")"
  policy_profile="$(jq -r '.llmDiagnostics.request.contextBundle.governedDomainContext.policyProfile // empty' "$output_file")"
  requested_present="$(jq -r '.llmDiagnostics.request.contextBundle.governedDomainContext.requested.present // false' "$output_file")"
  prompt_block_length="$(jq -r '(.llmDiagnostics.request.contextBundle.governedDomainContext.promptBlock // "") | length' "$output_file")"

  if [[ "$resolution_status" != "resolved" || "$available" != "true" || "$requested_present" != "true" || "$prompt_block_length" -le 0 ]]; then
    echo "Governed domain context was not resolved in intent-resolution diagnostics." >&2
    jq '{
      valid,
      warnings,
      failureCodes,
      governedDomainContext: .llmDiagnostics.request.contextBundle.governedDomainContext
    }' "$output_file" >&2
    return 1
  fi

  jq --arg policyProfile "$policy_profile" --arg resolutionStatus "$resolution_status" '{
    status: "governed-context-ready",
    domainContextGovernanceAuthoringSeen: true,
    policyProfile: $policyProfile,
    resolutionStatus: $resolutionStatus,
    available: .llmDiagnostics.request.contextBundle.governedDomainContext.available,
    requested: .llmDiagnostics.request.contextBundle.governedDomainContext.requested,
    promptBlockLength: ((.llmDiagnostics.request.contextBundle.governedDomainContext.promptBlock // "") | length),
    warnings,
    failureCodes
  }' "$output_file"
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

jq empty "$CONTRACT_SCHEMA_PATH" "$CONTRACT_EXAMPLE_PATH"

context_file="$(mktemp "${TMPDIR:-/tmp}/praxis-domain-catalog-context.XXXXXX.json")"
authoring_file="$(mktemp "${TMPDIR:-/tmp}/praxis-authoring-candidates.XXXXXX.json")"
governed_context_file="$(mktemp "${TMPDIR:-/tmp}/praxis-governed-context-authoring.XXXXXX.json")"
trap 'rm -f "$context_file" "$authoring_file" "$governed_context_file"' EXIT

echo "Verifying domain catalog runtime context and authoring hints."
echo "BACKEND_URL=${BACKEND_URL}"
echo "SERVICE_KEY=${SERVICE_KEY}"
echo "TENANT_ID=${TENANT_ID}"
echo "ENVIRONMENT=${ENVIRONMENT}"
echo "REQUIRE_AUTHORING_FLOW=${REQUIRE_AUTHORING_FLOW}"
echo "REQUIRE_GOVERNED_CONTEXT=${REQUIRE_GOVERNED_CONTEXT}"
echo "PROVIDER=${PROVIDER}"
echo "MODEL=${MODEL}"

echo
validate_context "human-resources.funcionarios" "cpf" "true" "$context_file"

echo
validate_context "human-resources.folhas-pagamento" "salario" "true" "$context_file"

echo
validate_context "operations.missoes" "status" "false" "$context_file"

echo
validate_authoring_candidates "$authoring_file" "$AUTHORING_QUERY" "$AUTHORING_ARTIFACT_KIND" "human-resources.folhas-pagamento"

echo
validate_authoring_candidates "$authoring_file" "$FORM_RULE_AUTHORING_QUERY" "$FORM_RULE_AUTHORING_ARTIFACT_KIND" "human-resources.funcionarios" "shared_rule_authoring" "governance" "rule.visualBlockGuidance.add"

echo
validate_governed_context_intent_resolution "$authoring_file" "$governed_context_file"

echo
echo "Domain catalog runtime authoring verification completed."
