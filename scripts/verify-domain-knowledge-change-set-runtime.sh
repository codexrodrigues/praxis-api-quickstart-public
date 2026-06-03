#!/usr/bin/env bash
set -euo pipefail

BACKEND_URL="${BACKEND_URL:-http://localhost:8088}"
TENANT_ID="${TENANT_ID:-desenv}"
ENVIRONMENT="${ENVIRONMENT:-local}"
ORIGIN="${ORIGIN:-https://praxisui-dev.web.app}"
RESOURCE_KEY="${RESOURCE_KEY:-human-resources.funcionarios}"
VERIFY_QUERY="${VERIFY_QUERY:-cpf}"
TARGET_CONCEPT_KEY="${TARGET_CONCEPT_KEY:-human-resources.funcionarios.field.cpf}"
AUTHOR_ID="${AUTHOR_ID:-codex:domain-knowledge-smoke}"
REVIEWER_ID="${REVIEWER_ID:-codex:reviewer}"
SMOKE_RUN_ID="${SMOKE_RUN_ID:-domain-knowledge-$(date +%Y%m%d%H%M%S)}"
ENSURE_DOMAIN_CATALOG_CONTEXT="${ENSURE_DOMAIN_CATALOG_CONTEXT:-true}"
CURL_CONNECT_TIMEOUT="${CURL_CONNECT_TIMEOUT:-10}"
CURL_MAX_TIME="${CURL_MAX_TIME:-120}"
REQUIRE_CHANGE_SET_TIMELINE="${REQUIRE_CHANGE_SET_TIMELINE:-auto}"
REQUIRE_EVIDENCE_REVERT="${REQUIRE_EVIDENCE_REVERT:-false}"
REQUIRE_EVIDENCE_SUPERSESSION="${REQUIRE_EVIDENCE_SUPERSESSION:-false}"
REQUIRE_PROJECT_KNOWLEDGE_RETRIEVAL="${REQUIRE_PROJECT_KNOWLEDGE_RETRIEVAL:-false}"
REQUIRE_PROJECT_KNOWLEDGE_VECTOR_RETRIEVAL="${REQUIRE_PROJECT_KNOWLEDGE_VECTOR_RETRIEVAL:-false}"
PROJECT_KNOWLEDGE_RETRIEVAL_CONCEPT_KEY="${PROJECT_KNOWLEDGE_RETRIEVAL_CONCEPT_KEY:-page-builder.e2e.project-knowledge.identity-card}"
AUTHORING_STREAM_MAX_TIME="${AUTHORING_STREAM_MAX_TIME:-120}"
DEFAULT_TARGET_CONCEPT_KEY="human-resources.funcionarios.field.cpf"

if [[ "$REQUIRE_PROJECT_KNOWLEDGE_VECTOR_RETRIEVAL" == "true" ]]; then
  REQUIRE_PROJECT_KNOWLEDGE_RETRIEVAL="true"
fi
if [[ "$REQUIRE_PROJECT_KNOWLEDGE_RETRIEVAL" == "true" && "$TARGET_CONCEPT_KEY" == "$DEFAULT_TARGET_CONCEPT_KEY" ]]; then
  TARGET_CONCEPT_KEY="$PROJECT_KNOWLEDGE_RETRIEVAL_CONCEPT_KEY"
fi
if [[ "$REQUIRE_PROJECT_KNOWLEDGE_RETRIEVAL" == "true" ]]; then
  REQUIRE_EVIDENCE_REVERT="true"
fi
if [[ "$REQUIRE_EVIDENCE_SUPERSESSION" == "true" ]]; then
  REQUIRE_EVIDENCE_REVERT="true"
  REQUIRE_CHANGE_SET_TIMELINE="true"
fi

usage() {
  cat >&2 <<'USAGE'
Usage:
  scripts/verify-domain-knowledge-change-set-runtime.sh

Examples:
  BACKEND_URL=http://localhost:8088 scripts/verify-domain-knowledge-change-set-runtime.sh
  TENANT_ID=desenv ENVIRONMENT=local RESOURCE_KEY=human-resources.funcionarios scripts/verify-domain-knowledge-change-set-runtime.sh
  ENSURE_DOMAIN_CATALOG_CONTEXT=false BACKEND_URL=http://localhost:8088 scripts/verify-domain-knowledge-change-set-runtime.sh

This smoke proves the governed Domain Knowledge write lifecycle over HTTP:
  1. ensure persisted domain catalog context for the target resource;
  2. create an LLM-authored change set with a safe add_evidence operation;
  3. revalidate the persisted change set;
  4. approve through the governed status endpoint;
  5. apply through the separate application boundary;
  6. read back the applied, safe response projection;
  7. validate safe change-set timeline when the runtime exposes it.
  8. optionally create, validate, approve, apply and timeline-check a governed
     revert_evidence change set for the evidence created by this smoke.

It assumes the quickstart backend was packaged with a config-starter version
that includes the Domain Knowledge change-set endpoints and that Domain
Knowledge projection is enabled in the running backend.

Set ENSURE_DOMAIN_CATALOG_CONTEXT=false only for diagnostics when the persisted
context was already prepared and the goal is to isolate the change-set lifecycle.

Timeline compatibility:
  - REQUIRE_CHANGE_SET_TIMELINE=auto  -> validate timeline when the endpoint
                                         exists; continue with a warning on
                                         404/405 for older starter versions
  - REQUIRE_CHANGE_SET_TIMELINE=true  -> fail when timeline is absent or invalid
  - REQUIRE_CHANGE_SET_TIMELINE=false -> skip timeline validation on purpose

Revert compatibility:
  - REQUIRE_EVIDENCE_REVERT=true -> prove revert_evidence lifecycle and safe
                                    evidence.reverted timeline event
  - REQUIRE_EVIDENCE_SUPERSESSION=true -> create active replacement evidence and
                                          prove replacement-backed revert emits
                                          safe evidence.superseded timeline event
  - REQUIRE_PROJECT_KNOWLEDGE_RETRIEVAL=true -> seed a governed Project
                                                Knowledge concept, prove it is
                                                retrieved after add_evidence,
                                                then prove it disappears from
                                                authoring retrieval after plain
                                                revert_evidence; with
                                                REQUIRE_EVIDENCE_SUPERSESSION=true,
                                                prove the replacement keeps the
                                                concept retrievable
  - REQUIRE_PROJECT_KNOWLEDGE_VECTOR_RETRIEVAL=true -> additionally require the
                                                       derived project_knowledge
                                                       vector document to exist
                                                       after add_evidence and be
                                                       removed or retained
                                                       correctly after
                                                       revert/supersession
USAGE
}

urlencode() {
  jq -nr --arg value "$1" '$value|@uri'
}

request_headers=(
  -H "Origin: ${ORIGIN}"
  -H "X-Tenant-ID: ${TENANT_ID}"
  -H "X-Env: ${ENVIRONMENT}"
)

json_headers=(
  "${request_headers[@]}"
  -H "Content-Type: application/json"
)

require_command() {
  if ! command -v "$1" >/dev/null 2>&1; then
    echo "Missing required command: $1" >&2
    exit 2
  fi
}

require_backend() {
  curl -fsS --max-time 10 "${BACKEND_URL%/}/actuator/health" >/dev/null
}

post_json() {
  local url="$1"
  local payload_file="$2"
  local output_file="$3"
  curl -fsS -X POST "$url" \
    --connect-timeout "$CURL_CONNECT_TIMEOUT" \
    --max-time "$CURL_MAX_TIME" \
    "${json_headers[@]}" \
    --data-binary @"$payload_file" \
    -o "$output_file"
}

patch_json() {
  local url="$1"
  local payload_file="$2"
  local output_file="$3"
  curl -fsS -X PATCH "$url" \
    --connect-timeout "$CURL_CONNECT_TIMEOUT" \
    --max-time "$CURL_MAX_TIME" \
    "${json_headers[@]}" \
    --data-binary @"$payload_file" \
    -o "$output_file"
}

get_json_allow_status() {
  local url="$1"
  local output_file="$2"
  local status_file="$3"
  curl -sS "$url" \
    --connect-timeout "$CURL_CONNECT_TIMEOUT" \
    --max-time "$CURL_MAX_TIME" \
    "${request_headers[@]}" \
    -o "$output_file" \
    -w '%{http_code}' > "$status_file"
}

validate_change_set_timeline() {
  local change_set_id="$1"
  local timeline_response="$2"
  local timeline_status_file="$3"
  local expected_operation_type="${4:-add_evidence}"
  local required_event_type="${5:-}"
  local forbidden_event_type="${6:-}"

  if [[ "$REQUIRE_CHANGE_SET_TIMELINE" == "false" ]]; then
    return 0
  fi

  echo "Validating safe Domain Knowledge change-set timeline..."
  get_json_allow_status \
    "${BACKEND_URL%/}/api/praxis/config/domain-knowledge/change-sets/${change_set_id}/timeline" \
    "$timeline_response" \
    "$timeline_status_file"
  timeline_status="$(cat "$timeline_status_file")"

  if [[ "$timeline_status" == "404" || "$timeline_status" == "405" ]]; then
    if [[ "$REQUIRE_CHANGE_SET_TIMELINE" == "true" ]]; then
      echo "Runtime does not expose Domain Knowledge change-set timeline yet." >&2
      cat "$timeline_response" >&2 || true
      exit 1
    fi
    echo "Warning: runtime does not expose Domain Knowledge change-set timeline yet; continuing without timeline proof." >&2
    return 0
  fi

  if [[ "$timeline_status" != "200" ]]; then
    echo "Unexpected Domain Knowledge change-set timeline response (HTTP ${timeline_status})." >&2
    cat "$timeline_response" >&2
    exit 1
  fi

  timeline_change_set_id="$(jq -r '.changeSetId // empty' "$timeline_response")"
  timeline_event_count="$(jq '(.events // []) | length' "$timeline_response")"
  timeline_created_count="$(jq '[.events[]? | select((.eventType // "") == "change_set.created") | select((.visibility // "") == "safe")] | length' "$timeline_response")"
  timeline_validation_count="$(jq '[.events[]? | select((.eventType // "") == "validation.completed") | select((.visibility // "") == "safe") | select((.validationStatus // "") == "valid")] | length' "$timeline_response")"
  timeline_review_count="$(jq '[.events[]? | select((.eventType // "") == "review.approved") | select((.visibility // "") == "safe")] | length' "$timeline_response")"
  timeline_applied_count="$(jq '[.events[]? | select((.eventType // "") == "change_set.applied") | select((.visibility // "") == "safe")] | length' "$timeline_response")"
  timeline_operation_count="$(jq --arg targetConceptKey "$TARGET_CONCEPT_KEY" --arg operationType "$expected_operation_type" '[.events[]? | select((.operationTypes // []) | index($operationType)) | select((.targetConceptKeys // []) | index($targetConceptKey))] | length' "$timeline_response")"
  timeline_required_event_count="1"
  if [[ -n "$required_event_type" ]]; then
    timeline_required_event_count="$(jq --arg eventType "$required_event_type" '[.events[]? | select((.eventType // "") == $eventType) | select((.visibility // "") == "safe")] | length' "$timeline_response")"
  fi
  timeline_forbidden_event_count="0"
  if [[ -n "$forbidden_event_type" ]]; then
    timeline_forbidden_event_count="$(jq --arg eventType "$forbidden_event_type" '[.events[]? | select((.eventType // "") == $eventType)] | length' "$timeline_response")"
  fi
  timeline_unsafe_count="$(jq '[.events[]? | select((.visibility // "") != "safe")] | length' "$timeline_response")"
  timeline_leak_count="$(jq '[
    (paths as $path | ($path | map(tostring) | join("."))),
    (paths(scalars) as $path | getpath($path) | tostring)
    | select(test("sourcePointer|sourceUri|patchHash|raw prompt|chat history|CPF is personal data|purpose, minimization|praxis-runtime-smoke://domain-knowledge-change-set"))
  ] | length' "$timeline_response")"

  if [[ "$timeline_change_set_id" != "$change_set_id" \
    || "$timeline_event_count" -lt 4 \
    || "$timeline_created_count" -eq 0 \
    || "$timeline_validation_count" -eq 0 \
    || "$timeline_review_count" -eq 0 \
    || "$timeline_applied_count" -eq 0 \
    || "$timeline_operation_count" -eq 0 \
    || "$timeline_required_event_count" -eq 0 \
    || "$timeline_forbidden_event_count" -ne 0 \
    || "$timeline_unsafe_count" -ne 0 \
    || "$timeline_leak_count" -ne 0 ]]; then
    echo "Invalid safe Domain Knowledge change-set timeline response." >&2
    jq '{changeSetId, changeSetKey, status, events}' "$timeline_response" >&2
    exit 1
  fi

  jq -n \
    --arg status "domain-knowledge-change-set-timeline-ready" \
    --arg changeSetId "$change_set_id" \
    --argjson eventCount "$timeline_event_count" \
    '{status: $status, changeSetId: $changeSetId, eventCount: $eventCount}'
}

runtime_classpath_file() {
  local classpath_file="$1"
  if [[ ! -f "$classpath_file" ]]; then
    mvn -q dependency:build-classpath \
      -Dmdep.outputFile=target/runtime-classpath.txt \
      -DincludeScope=runtime >/dev/null
  fi
  if [[ ! -s "$classpath_file" ]]; then
    echo "Could not resolve quickstart runtime classpath for Project Knowledge fixture." >&2
    exit 2
  fi
}

seed_project_knowledge_concept_fixture() {
  if [[ "$REQUIRE_PROJECT_KNOWLEDGE_RETRIEVAL" != "true" && "$REQUIRE_PROJECT_KNOWLEDGE_VECTOR_RETRIEVAL" != "true" ]]; then
    return 0
  fi
  require_command javac
  require_command java
  require_command mvn

  local fixture_source="scripts/ProjectKnowledgeFixture.java"
  local target_dir="target/scripts"
  local classpath_file="target/runtime-classpath.txt"
  if [[ ! -f "$fixture_source" ]]; then
    echo "Missing Project Knowledge fixture source: $fixture_source" >&2
    exit 2
  fi
  mkdir -p "$target_dir"
  runtime_classpath_file "$classpath_file"
  local classpath
  classpath="$(cat "$classpath_file")"
  javac -cp "$classpath" -d "$target_dir" "$fixture_source"
  java -cp "${target_dir}:${classpath}" ProjectKnowledgeFixture \
    seed-concept \
    "$TENANT_ID" \
    "$ENVIRONMENT" \
    "human-resources" \
    "$RESOURCE_KEY" >/dev/null
}

assert_project_knowledge_vector_document_count() {
  local phase="$1"
  local evidence_key_arg="$2"
  local expected_count="$3"

  if [[ "$REQUIRE_PROJECT_KNOWLEDGE_VECTOR_RETRIEVAL" != "true" ]]; then
    return 0
  fi
  require_command java
  require_command javac
  require_command mvn

  local fixture_source="scripts/ProjectKnowledgeFixture.java"
  local target_dir="target/scripts"
  local classpath_file="target/runtime-classpath.txt"
  mkdir -p "$target_dir"
  runtime_classpath_file "$classpath_file"
  local classpath
  classpath="$(cat "$classpath_file")"
  javac -cp "$classpath" -d "$target_dir" "$fixture_source"

  echo "Validating Project Knowledge derived vector document (${phase})..."
  java -cp "${target_dir}:${classpath}" ProjectKnowledgeFixture \
    assert-vector-document-count \
    "$TENANT_ID" \
    "$ENVIRONMENT" \
    "$TARGET_CONCEPT_KEY" \
    "$evidence_key_arg" \
    "$expected_count"
}

write_authoring_turn_payload() {
  local output_file="$1"
  local phase="$2"
  jq -n \
    --arg prompt "Crie um formulario de funcionarios usando o contexto governado do projeto." \
    --arg clientTurnId "${SMOKE_RUN_ID}-${phase}" \
    --arg resourceKey "$RESOURCE_KEY" \
    '{
      userPrompt: $prompt,
      targetApp: "praxis-api-quickstart",
      targetComponentId: "domain-knowledge-retrieval-smoke",
      currentRoute: "/funcionarios",
      provider: "mock",
      model: "deterministic-smoke",
      clientTurnId: $clientTurnId,
      contextHints: {
        domainCatalog: {
          schemaVersion: "praxis.ai.context-hints.domain-catalog/v0.2",
          serviceKey: "praxis-service",
          contextKey: "human-resources",
          resourceKey: $resourceKey,
          nodeType: "concept",
          type: "resource",
          intent: "authoring",
          query: "funcionarios project knowledge",
          limit: 6
        }
      }
    }' > "$output_file"
}

validate_project_knowledge_retrieval() {
  local phase="$1"
  local expected_presence="$2"
  local payload_file="$tmp_dir/authoring-${phase}.json"
  local start_response="$tmp_dir/authoring-${phase}-start.json"
  local sse_response="$tmp_dir/authoring-${phase}.sse"
  local events_jsonl="$tmp_dir/authoring-${phase}-events.jsonl"

  if [[ "$REQUIRE_PROJECT_KNOWLEDGE_RETRIEVAL" != "true" ]]; then
    return 0
  fi

  echo "Validating Project Knowledge authoring retrieval (${phase})..."
  write_authoring_turn_payload "$payload_file" "$phase"
  post_json "${BACKEND_URL%/}/api/praxis/config/ai/authoring/turn/stream/start" "$payload_file" "$start_response"

  local stream_id
  local access_token
  stream_id="$(jq -r '.streamId // empty' "$start_response")"
  access_token="$(jq -r '.streamAccessToken // empty' "$start_response")"
  if [[ -z "$stream_id" ]]; then
    echo "Authoring stream did not return streamId." >&2
    jq . "$start_response" >&2
    exit 1
  fi

  local stream_url="${BACKEND_URL%/}/api/praxis/config/ai/authoring/turn/stream/${stream_id}"
  if [[ -n "$access_token" && "$access_token" != "null" ]]; then
    stream_url="${stream_url}?accessToken=$(urlencode "$access_token")"
  fi
  curl -fsS -N \
    --connect-timeout "$CURL_CONNECT_TIMEOUT" \
    --max-time "$AUTHORING_STREAM_MAX_TIME" \
    "$stream_url" \
    "${request_headers[@]}" \
    -o "$sse_response"
  awk '/^data:/ { sub(/^data:[[:space:]]*/, ""); print }' "$sse_response" > "$events_jsonl"
  if [[ ! -s "$events_jsonl" ]]; then
    echo "Authoring stream returned no JSON events." >&2
    cat "$sse_response" >&2 || true
    exit 1
  fi

  local error_count
  local result_count
  local concept_retrieval_count
  error_count="$(jq -s '[.[] | select((.type // "") == "error")] | length' "$events_jsonl")"
  result_count="$(jq -s '[.[] | select((.type // "") == "result")] | length' "$events_jsonl")"
  concept_retrieval_count="$(jq -s --arg conceptKey "$PROJECT_KNOWLEDGE_RETRIEVAL_CONCEPT_KEY" '[
    .[]
    | select((.type // "") == "thought.step")
    | select((.payload.phase // "") == "projectKnowledge.retrieve")
    | select((.payload.diagnostics.conceptKeys // []) | index($conceptKey))
  ] | length' "$events_jsonl")"

  if [[ "$error_count" -ne 0 || "$result_count" -eq 0 ]]; then
    echo "Authoring stream did not complete successfully for Project Knowledge retrieval proof." >&2
    jq -s '[.[] | {type, payload}]' "$events_jsonl" >&2
    exit 1
  fi
  if [[ "$expected_presence" == "present" && "$concept_retrieval_count" -eq 0 ]]; then
    echo "Expected Project Knowledge concept was not retrieved after active evidence was applied." >&2
    jq -s '[.[] | select((.type // "") == "thought.step") | {type, payload}]' "$events_jsonl" >&2
    exit 1
  fi
  if [[ "$expected_presence" == "absent" && "$concept_retrieval_count" -ne 0 ]]; then
    echo "Reverted Project Knowledge evidence still influenced authoring retrieval." >&2
    jq -s '[.[] | select((.type // "") == "thought.step") | {type, payload}]' "$events_jsonl" >&2
    exit 1
  fi

  jq -n \
    --arg status "project-knowledge-authoring-retrieval-ready" \
    --arg phase "$phase" \
    --arg expected "$expected_presence" \
    --arg conceptKey "$PROJECT_KNOWLEDGE_RETRIEVAL_CONCEPT_KEY" \
    --argjson retrievalCount "$concept_retrieval_count" \
    '{status: $status, phase: $phase, expected: $expected, conceptKey: $conceptKey, retrievalCount: $retrievalCount}'
}

if [[ "${1:-}" == "-h" || "${1:-}" == "--help" ]]; then
  usage
  exit 0
fi

require_command curl
require_command jq
require_backend

script_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ensure_script="${script_dir}/ensure-domain-catalog-context.sh"
if [[ ! -x "$ensure_script" ]]; then
  echo "Missing executable dependency: ${ensure_script}" >&2
  exit 2
fi

tmp_dir="$(mktemp -d "${TMPDIR:-/tmp}/praxis-domain-knowledge-change-set.XXXXXX")"
trap 'rm -rf "$tmp_dir"' EXIT

seed_project_knowledge_concept_fixture

create_payload="$tmp_dir/create.json"
create_response="$tmp_dir/create-response.json"
validate_response="$tmp_dir/validate-response.json"
status_payload="$tmp_dir/status.json"
status_response="$tmp_dir/status-response.json"
apply_response="$tmp_dir/apply-response.json"
readback_response="$tmp_dir/readback-response.json"
list_response="$tmp_dir/list-response.json"
timeline_response="$tmp_dir/timeline-response.json"
timeline_status_file="$tmp_dir/timeline-status.txt"
revert_payload="$tmp_dir/revert.json"
revert_response="$tmp_dir/revert-response.json"
revert_validate_response="$tmp_dir/revert-validate-response.json"
revert_status_payload="$tmp_dir/revert-status.json"
revert_status_response="$tmp_dir/revert-status-response.json"
revert_apply_response="$tmp_dir/revert-apply-response.json"
revert_readback_response="$tmp_dir/revert-readback-response.json"
revert_timeline_response="$tmp_dir/revert-timeline-response.json"
revert_timeline_status_file="$tmp_dir/revert-timeline-status.txt"

change_set_key="project-knowledge:${RESOURCE_KEY}:cpf-guidance:${SMOKE_RUN_ID}"
evidence_key="llm-proposal:${RESOURCE_KEY}:cpf-guidance:${SMOKE_RUN_ID}"
replacement_evidence_key="llm-proposal:${RESOURCE_KEY}:cpf-guidance-replacement:${SMOKE_RUN_ID}"
revert_change_set_key="project-knowledge:${RESOURCE_KEY}:revert-cpf-guidance:${SMOKE_RUN_ID}"

if [[ "$ENSURE_DOMAIN_CATALOG_CONTEXT" == "true" ]]; then
  echo "Ensuring Domain Catalog context for ${RESOURCE_KEY} (${VERIFY_QUERY})..."
  BACKEND_URL="$BACKEND_URL" \
  TENANT_ID="$TENANT_ID" \
  ENVIRONMENT="$ENVIRONMENT" \
  ORIGIN="$ORIGIN" \
  VERIFY_QUERY="$VERIFY_QUERY" \
  CURL_CONNECT_TIMEOUT="$CURL_CONNECT_TIMEOUT" \
  CURL_MAX_TIME="$CURL_MAX_TIME" \
  "$ensure_script" "$RESOURCE_KEY"
else
  echo "Skipping Domain Catalog context ensure (ENSURE_DOMAIN_CATALOG_CONTEXT=false)."
fi

jq -n \
  --arg changeSetKey "$change_set_key" \
  --arg authorId "$AUTHOR_ID" \
  --arg intent "Improve governed CPF guidance for employee registration" \
  --arg reason "The runtime smoke proposes safe evidence for a governed Domain Knowledge concept." \
  --arg operationId "op-add-cpf-guidance-evidence" \
  --arg targetTenant "$TENANT_ID" \
  --arg targetEnvironment "$ENVIRONMENT" \
  --arg targetConceptKey "$TARGET_CONCEPT_KEY" \
  --arg evidenceKey "$evidence_key" \
  --arg replacementEvidenceKey "$replacement_evidence_key" \
  --argjson requireSupersession "$([[ "$REQUIRE_EVIDENCE_SUPERSESSION" == "true" ]] && echo true || echo false)" \
  '{
    changeSetKey: $changeSetKey,
    status: "proposed",
    authorType: "llm",
    authorId: $authorId,
    intent: $intent,
    reason: $reason,
    patch: ([
        {
          operationId: $operationId,
          operationType: "add_evidence",
          target: {
            tenantId: $targetTenant,
            environment: $targetEnvironment,
            subjectType: "concept",
            conceptKey: $targetConceptKey
          },
          reason: "Connect reviewed Project Knowledge guidance to the canonical concept.",
          evidenceRefs: ["domain-catalog:human-resources:v2026-04-30"],
          confidence: 0.82,
          payload: {
            evidenceKey: $evidenceKey,
            evidenceType: "llm_proposal",
            sourceUri: "praxis-runtime-smoke://domain-knowledge-change-set",
            sourcePointer: "/patch/0",
            summary: "CPF is personal data and guidance should explain purpose, minimization and review provenance.",
            aiVisibility: "summarize_only",
            evidenceSafety: "reviewed"
          }
        }
      ] + (
      if $requireSupersession then
        [
          {
            operationId: "op-add-replacement-cpf-guidance-evidence",
            operationType: "add_evidence",
            target: {
              tenantId: $targetTenant,
              environment: $targetEnvironment,
              subjectType: "concept",
              conceptKey: $targetConceptKey
            },
            reason: "Prepare reviewed replacement evidence for governed supersession proof.",
            evidenceRefs: ["domain-catalog:human-resources:v2026-04-30"],
            confidence: 0.87,
            payload: {
              evidenceKey: $replacementEvidenceKey,
              evidenceType: "llm_proposal",
              sourceUri: "praxis-runtime-smoke://domain-knowledge-change-set",
              sourcePointer: "/patch/1",
              summary: "Replacement CPF guidance was reviewed and should become the active authoring influence.",
              aiVisibility: "summarize_only",
              evidenceSafety: "reviewed"
            }
          }
        ]
      else
        []
      end
    ))
  }' > "$create_payload"

echo "Creating governed Domain Knowledge change set..."
post_json "${BACKEND_URL%/}/api/praxis/config/domain-knowledge/change-sets" "$create_payload" "$create_response"
change_set_id="$(jq -r '.id // empty' "$create_response")"
created_status="$(jq -r '.status // empty' "$create_response")"
created_validation="$(jq -r '.validationStatus // empty' "$create_response")"
if [[ -z "$change_set_id" || "$created_status" != "proposed" || "$created_validation" != "valid" ]]; then
  echo "Unexpected create response." >&2
  jq '{id, status, validationStatus, operationCount, safeOperationSummary}' "$create_response" >&2
  exit 1
fi

echo "Revalidating persisted change set ${change_set_id}..."
curl -fsS -X POST \
  --connect-timeout "$CURL_CONNECT_TIMEOUT" \
  --max-time "$CURL_MAX_TIME" \
  "${BACKEND_URL%/}/api/praxis/config/domain-knowledge/change-sets/${change_set_id}/validate" \
  "${request_headers[@]}" \
  -o "$validate_response"
if [[ "$(jq -r '.valid // false' "$validate_response")" != "true" ]]; then
  echo "Persisted validation failed." >&2
  jq . "$validate_response" >&2
  exit 1
fi

jq -n \
  --arg reviewerId "$REVIEWER_ID" \
  '{
    status: "approved",
    reviewerId: $reviewerId,
    reason: "Runtime smoke reviewer approved the valid add_evidence proposal."
  }' > "$status_payload"

echo "Approving change set..."
patch_json "${BACKEND_URL%/}/api/praxis/config/domain-knowledge/change-sets/${change_set_id}/status" "$status_payload" "$status_response"
if [[ "$(jq -r '.status // empty' "$status_response")" != "approved" ]]; then
  echo "Unexpected approval response." >&2
  jq '{id, status, reviewerId, reviewedAt}' "$status_response" >&2
  exit 1
fi

echo "Applying approved add_evidence change set..."
curl -fsS -X POST \
  --connect-timeout "$CURL_CONNECT_TIMEOUT" \
  --max-time "$CURL_MAX_TIME" \
  "${BACKEND_URL%/}/api/praxis/config/domain-knowledge/change-sets/${change_set_id}/apply" \
  "${request_headers[@]}" \
  -o "$apply_response"
if [[ "$(jq -r '.status // empty' "$apply_response")" != "applied" || "$(jq -r '.appliedAt // empty' "$apply_response")" == "" ]]; then
  echo "Unexpected apply response." >&2
  jq '{id, status, validationStatus, appliedAt, safeOperationSummary}' "$apply_response" >&2
  exit 1
fi

echo "Reading back applied change set..."
curl -fsS \
  --connect-timeout "$CURL_CONNECT_TIMEOUT" \
  --max-time "$CURL_MAX_TIME" \
  "${BACKEND_URL%/}/api/praxis/config/domain-knowledge/change-sets/${change_set_id}" \
  "${request_headers[@]}" \
  -o "$readback_response"
readback_valid="$(jq -r '.validationStatus // empty' "$readback_response")"
readback_status="$(jq -r '.status // empty' "$readback_response")"
operation_type="$(jq -r '.safeOperationSummary[0].operationType // empty' "$readback_response")"
if [[ "$readback_status" != "applied" || "$readback_valid" != "valid" || "$operation_type" != "add_evidence" ]]; then
  echo "Unexpected readback response." >&2
  jq '{id, status, validationStatus, operationCount, safeOperationSummary, appliedAt}' "$readback_response" >&2
  exit 1
fi

validate_change_set_timeline "$change_set_id" "$timeline_response" "$timeline_status_file"
assert_project_knowledge_vector_document_count "after-add" "$evidence_key" "1"
if [[ "$REQUIRE_EVIDENCE_SUPERSESSION" == "true" ]]; then
  assert_project_knowledge_vector_document_count "after-add-replacement" "$replacement_evidence_key" "1"
fi
validate_project_knowledge_retrieval "after-add" "present"

revert_change_set_id=""
if [[ "$REQUIRE_EVIDENCE_REVERT" == "true" ]]; then
  jq -n \
    --arg changeSetKey "$revert_change_set_key" \
    --arg authorId "$AUTHOR_ID" \
    --arg intent "Revert governed CPF guidance evidence after review" \
    --arg reason "The runtime smoke proves governed evidence lifecycle reversal without deleting audit history." \
    --arg operationId "op-revert-cpf-guidance-evidence" \
    --arg targetTenant "$TENANT_ID" \
    --arg targetEnvironment "$ENVIRONMENT" \
    --arg targetConceptKey "$TARGET_CONCEPT_KEY" \
    --arg evidenceKey "$evidence_key" \
    --arg replacementEvidenceKey "$replacement_evidence_key" \
    --argjson requireSupersession "$([[ "$REQUIRE_EVIDENCE_SUPERSESSION" == "true" ]] && echo true || echo false)" \
    '{
      changeSetKey: $changeSetKey,
      status: "proposed",
      authorType: "llm",
      authorId: $authorId,
      intent: $intent,
      reason: $reason,
      patch: [
        {
          operationId: $operationId,
          operationType: "revert_evidence",
          target: {
            tenantId: $targetTenant,
            environment: $targetEnvironment,
            subjectType: "concept",
            conceptKey: $targetConceptKey
          },
          reason: "Revert the governed smoke evidence through the canonical lifecycle boundary.",
          evidenceRefs: ["domain-catalog:human-resources:v2026-04-30"],
          confidence: 0.91,
          payload: ({
              evidenceKey: $evidenceKey,
              revertReason: "The smoke-created evidence is reverted to prove governed lifecycle reversal without deleting history.",
              visibilityAfterRevert: "deny"
            } + (
            if $requireSupersession then
              {
                replacementEvidenceKey: $replacementEvidenceKey
              }
            else
              {}
            end
          ))
        }
      ]
    }' > "$revert_payload"

  echo "Creating governed Domain Knowledge revert change set..."
  post_json "${BACKEND_URL%/}/api/praxis/config/domain-knowledge/change-sets" "$revert_payload" "$revert_response"
  revert_change_set_id="$(jq -r '.id // empty' "$revert_response")"
  revert_created_status="$(jq -r '.status // empty' "$revert_response")"
  revert_created_validation="$(jq -r '.validationStatus // empty' "$revert_response")"
  revert_operation_type="$(jq -r '.safeOperationSummary[0].operationType // empty' "$revert_response")"
  if [[ -z "$revert_change_set_id" || "$revert_created_status" != "proposed" || "$revert_created_validation" != "valid" || "$revert_operation_type" != "revert_evidence" ]]; then
    echo "Unexpected revert create response." >&2
    jq '{id, status, validationStatus, operationCount, safeOperationSummary}' "$revert_response" >&2
    exit 1
  fi

  echo "Revalidating persisted revert change set ${revert_change_set_id}..."
  curl -fsS -X POST \
    --connect-timeout "$CURL_CONNECT_TIMEOUT" \
    --max-time "$CURL_MAX_TIME" \
    "${BACKEND_URL%/}/api/praxis/config/domain-knowledge/change-sets/${revert_change_set_id}/validate" \
    "${request_headers[@]}" \
    -o "$revert_validate_response"
  if [[ "$(jq -r '.valid // false' "$revert_validate_response")" != "true" ]]; then
    echo "Persisted revert validation failed." >&2
    jq . "$revert_validate_response" >&2
    exit 1
  fi

  jq -n \
    --arg reviewerId "$REVIEWER_ID" \
    '{
      status: "approved",
      reviewerId: $reviewerId,
      reason: "Runtime smoke reviewer approved the valid revert_evidence proposal."
    }' > "$revert_status_payload"

  echo "Approving revert change set..."
  patch_json "${BACKEND_URL%/}/api/praxis/config/domain-knowledge/change-sets/${revert_change_set_id}/status" "$revert_status_payload" "$revert_status_response"
  if [[ "$(jq -r '.status // empty' "$revert_status_response")" != "approved" ]]; then
    echo "Unexpected revert approval response." >&2
    jq '{id, status, reviewerId, reviewedAt}' "$revert_status_response" >&2
    exit 1
  fi

  echo "Applying approved revert_evidence change set..."
  curl -fsS -X POST \
    --connect-timeout "$CURL_CONNECT_TIMEOUT" \
    --max-time "$CURL_MAX_TIME" \
    "${BACKEND_URL%/}/api/praxis/config/domain-knowledge/change-sets/${revert_change_set_id}/apply" \
    "${request_headers[@]}" \
    -o "$revert_apply_response"
  if [[ "$(jq -r '.status // empty' "$revert_apply_response")" != "applied" || "$(jq -r '.appliedAt // empty' "$revert_apply_response")" == "" ]]; then
    echo "Unexpected revert apply response." >&2
    jq '{id, status, validationStatus, appliedAt, safeOperationSummary}' "$revert_apply_response" >&2
    exit 1
  fi

  echo "Reading back applied revert change set..."
  curl -fsS \
    --connect-timeout "$CURL_CONNECT_TIMEOUT" \
    --max-time "$CURL_MAX_TIME" \
    "${BACKEND_URL%/}/api/praxis/config/domain-knowledge/change-sets/${revert_change_set_id}" \
    "${request_headers[@]}" \
    -o "$revert_readback_response"
  revert_readback_valid="$(jq -r '.validationStatus // empty' "$revert_readback_response")"
  revert_readback_status="$(jq -r '.status // empty' "$revert_readback_response")"
  revert_readback_operation_type="$(jq -r '.safeOperationSummary[0].operationType // empty' "$revert_readback_response")"
  if [[ "$revert_readback_status" != "applied" || "$revert_readback_valid" != "valid" || "$revert_readback_operation_type" != "revert_evidence" ]]; then
    echo "Unexpected revert readback response." >&2
    jq '{id, status, validationStatus, operationCount, safeOperationSummary, appliedAt}' "$revert_readback_response" >&2
    exit 1
  fi

  validate_change_set_timeline \
    "$revert_change_set_id" \
    "$revert_timeline_response" \
    "$revert_timeline_status_file" \
    "revert_evidence" \
    "$([[ "$REQUIRE_EVIDENCE_SUPERSESSION" == "true" ]] && echo "evidence.superseded" || echo "evidence.reverted")" \
    "$([[ "$REQUIRE_EVIDENCE_SUPERSESSION" == "true" ]] && echo "evidence.reverted" || echo "")"
  validate_project_knowledge_retrieval \
    "after-revert" \
    "$([[ "$REQUIRE_EVIDENCE_SUPERSESSION" == "true" ]] && echo "present" || echo "absent")"
  assert_project_knowledge_vector_document_count "after-revert-old-evidence" "$evidence_key" "0"
  if [[ "$REQUIRE_EVIDENCE_SUPERSESSION" == "true" ]]; then
    assert_project_knowledge_vector_document_count "after-revert-replacement-evidence" "$replacement_evidence_key" "1"
  fi
fi

encoded_status="$(urlencode applied)"
curl -fsS \
  --connect-timeout "$CURL_CONNECT_TIMEOUT" \
  --max-time "$CURL_MAX_TIME" \
  "${BACKEND_URL%/}/api/praxis/config/domain-knowledge/change-sets?status=${encoded_status}" \
  "${request_headers[@]}" \
  -o "$list_response"
if [[ "$(jq --arg id "$change_set_id" '[.[] | select(.id == $id)] | length' "$list_response")" -eq 0 ]]; then
  echo "Applied change set was not present in status-filtered list." >&2
  jq --arg id "$change_set_id" '{expectedId: $id, count: length, ids: [.[] | .id]}' "$list_response" >&2
  exit 1
fi

jq -n \
  --arg status "domain-knowledge-change-set-runtime-ready" \
  --arg backendUrl "$BACKEND_URL" \
  --arg tenantId "$TENANT_ID" \
  --arg environment "$ENVIRONMENT" \
  --arg resourceKey "$RESOURCE_KEY" \
  --arg targetConceptKey "$TARGET_CONCEPT_KEY" \
  --arg changeSetId "$change_set_id" \
  --arg changeSetKey "$change_set_key" \
  --arg evidenceKey "$evidence_key" \
  --arg replacementEvidenceKey "$replacement_evidence_key" \
  --arg revertChangeSetId "$revert_change_set_id" \
  --argjson revertChecked "$([[ "$REQUIRE_EVIDENCE_REVERT" == "true" ]] && echo true || echo false)" \
  --argjson supersessionChecked "$([[ "$REQUIRE_EVIDENCE_SUPERSESSION" == "true" ]] && echo true || echo false)" \
  --argjson projectKnowledgeRetrievalChecked "$([[ "$REQUIRE_PROJECT_KNOWLEDGE_RETRIEVAL" == "true" ]] && echo true || echo false)" \
  --argjson projectKnowledgeVectorRetrievalChecked "$([[ "$REQUIRE_PROJECT_KNOWLEDGE_VECTOR_RETRIEVAL" == "true" ]] && echo true || echo false)" \
  '{
    status: $status,
    backendUrl: $backendUrl,
    tenantId: $tenantId,
    environment: $environment,
    resourceKey: $resourceKey,
    targetConceptKey: $targetConceptKey,
    changeSetId: $changeSetId,
    changeSetKey: $changeSetKey,
    evidenceKey: $evidenceKey,
    replacementEvidenceKey: (if $supersessionChecked then $replacementEvidenceKey else "" end),
    revertChangeSetId: $revertChangeSetId,
    revertChecked: $revertChecked,
    supersessionChecked: $supersessionChecked,
    projectKnowledgeRetrievalChecked: $projectKnowledgeRetrievalChecked,
    projectKnowledgeVectorRetrievalChecked: $projectKnowledgeVectorRetrievalChecked,
    lifecycle: (
      ["created", "validated", "approved", "applied", "readback-confirmed", "timeline-confirmed-or-skipped"]
      + (if $revertChecked then ["revert-created", "revert-validated", "revert-approved", "revert-applied", "revert-timeline-confirmed"] else [] end)
      + (if $supersessionChecked then ["replacement-evidence-created", "supersession-timeline-confirmed"] else [] end)
      + (
        if $projectKnowledgeRetrievalChecked then
          ["project-knowledge-retrieved-after-add"]
          + (if $supersessionChecked then ["project-knowledge-retained-after-supersession"] else ["project-knowledge-absent-after-revert"] end)
        else
          []
        end
      )
      + (
        if $projectKnowledgeVectorRetrievalChecked then
          ["project-knowledge-vector-index-confirmed-after-add"]
          + (if $supersessionChecked then ["project-knowledge-vector-index-retained-for-replacement"] else ["project-knowledge-vector-index-absent-after-revert"] end)
        else
          []
        end
      )
    )
  }'
