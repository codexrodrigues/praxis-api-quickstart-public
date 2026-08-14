#!/usr/bin/env bash
set -euo pipefail

BACKEND_URL="${BACKEND_URL:-https://praxis-api-quickstart.onrender.com}"
ORIGIN="${ORIGIN:-https://praxisui.dev}"
ADMIN_USERNAME="${ADMIN_USERNAME:-admin}"
ADMIN_PASSWORD="${ADMIN_PASSWORD:-${PRACTICE_TEMP_PASSWORD:-}}"
PUBLISHER_USERNAME="${PUBLISHER_USERNAME:-${APP_AUTH_GOVERNANCE_PUBLISHER_USERNAME:-}}"
PUBLISHER_PASSWORD="${PUBLISHER_PASSWORD:-${APP_AUTH_GOVERNANCE_PUBLISHER_PASSWORD:-}}"
APPROVER_USERNAME="${APPROVER_USERNAME:-praxis-governance-approver-a}"
TENANT_ID="${TENANT_ID:-default}"
ENVIRONMENT="${ENVIRONMENT:-dev}"
AUTHOR_USER_ID="${AUTHOR_USER_ID:-reactive-determination-smoke-author}"
SMOKE_RUN_ID="${SMOKE_RUN_ID:-$(date -u +%Y%m%d%H%M%S)}"

if [[ -z "${ADMIN_PASSWORD}" ]]; then
  echo "ADMIN_PASSWORD or PRACTICE_TEMP_PASSWORD is required." >&2
  exit 1
fi
if [[ -z "${PUBLISHER_USERNAME}" || -z "${PUBLISHER_PASSWORD}" ]]; then
  echo "A governed publisher identity is required through PUBLISHER_USERNAME/PUBLISHER_PASSWORD or the APP_AUTH_GOVERNANCE_PUBLISHER_* variables." >&2
  exit 1
fi
if ! command -v jq >/dev/null 2>&1; then
  echo "jq is required." >&2
  exit 1
fi

run_dir="$(mktemp -d "${TMPDIR:-/tmp}/praxis-reactive-determinations.XXXXXX")"
trap 'rm -rf "${run_dir}"' EXIT
executor_cookie_jar="${run_dir}/executor-cookies.txt"
publisher_cookie_jar="${run_dir}/publisher-cookies.txt"
approver_cookie_jar="${run_dir}/approver-cookies.txt"

authenticate() {
  local label="$1"
  local username="$2"
  local password="$3"
  local target_cookie_jar="$4"
  local login_request="${run_dir}/${label}-login.json"
  local login_status

  jq -n --arg username "${username}" --arg password "${password}" \
    '{username: $username, password: $password}' > "${login_request}"
  login_status="$(curl -sS "${BACKEND_URL%/}/auth/login" \
    -c "${target_cookie_jar}" \
    -H "Origin: ${ORIGIN}" \
    -H 'Content-Type: application/json' \
    --data-binary "@${login_request}" \
    -o "${run_dir}/${label}-login-response.json" \
    -w '%{http_code}')"
  if [[ "${login_status}" != "200" && "${login_status}" != "204" ]]; then
    echo "${label} principal could not authenticate (HTTP ${login_status})." >&2
    exit 1
  fi
}

authenticate executor "${ADMIN_USERNAME}" "${ADMIN_PASSWORD}" "${executor_cookie_jar}"
authenticate publisher "${PUBLISHER_USERNAME}" "${PUBLISHER_PASSWORD}" "${publisher_cookie_jar}"

session_status="$(curl -sS "${BACKEND_URL%/}/auth/session" \
  -b "${publisher_cookie_jar}" \
  -c "${publisher_cookie_jar}" \
  -H "Origin: ${ORIGIN}" \
  -o /dev/null \
  -w '%{http_code}')"
if [[ "${session_status}" != "200" && "${session_status}" != "204" ]]; then
  echo "Authenticated session probe failed with HTTP ${session_status}." >&2
  exit 1
fi
cp "${publisher_cookie_jar}" "${approver_cookie_jar}"
switch_status="$(curl -sS -X POST "${BACKEND_URL%/}/auth/governance-lab/session/approver-a" \
  -b "${approver_cookie_jar}" \
  -c "${approver_cookie_jar}" \
  -H "Origin: ${ORIGIN}" \
  -o "${run_dir}/approver-switch-response.json" \
  -w '%{http_code}')"
if [[ "${switch_status}" != "200" && "${switch_status}" != "204" ]]; then
  echo "Governance approver session could not be established (HTTP ${switch_status})." >&2
  exit 1
fi

config_post() {
  local path="$1"
  local request_file="$2"
  local response_file="$3"
  local actual_status
  actual_status="$(curl -sS "${BACKEND_URL%/}${path}" \
    -b "${publisher_cookie_jar}" \
    -c "${publisher_cookie_jar}" \
    -H "Origin: ${ORIGIN}" \
    -H "X-Tenant-ID: ${TENANT_ID}" \
    -H "X-Env: ${ENVIRONMENT}" \
    -H "X-User-ID: ${AUTHOR_USER_ID}" \
    -H 'Accept: application/json' \
    -H 'Content-Type: application/json' \
    --data-binary "@${request_file}" \
    -o "${response_file}" \
    -w '%{http_code}')"
  if [[ "${actual_status}" != "200" && "${actual_status}" != "201" && "${actual_status}" != "202" ]]; then
    echo "Config publication step ${path} failed with HTTP ${actual_status}." >&2
    jq -c '{status, title, detail, message}' "${response_file}" 2>/dev/null >&2 || true
    exit 1
  fi
}

approve_definition() {
  local definition_id="$1"
  local request_file="$2"
  local response_file="$3"
  local publisher_status
  local approver_status

  publisher_status="$(curl -sS -X PATCH \
    "${BACKEND_URL%/}/api/praxis/config/domain-rules/definitions/${definition_id}/status" \
    -b "${publisher_cookie_jar}" \
    -H "Origin: ${ORIGIN}" \
    -H 'Accept: application/json' \
    -H 'Content-Type: application/json' \
    --data-binary "@${request_file}" \
    -o "${run_dir}/publisher-approval-denied.json" \
    -w '%{http_code}')"
  if [[ "${publisher_status}" != "403" ]]; then
    echo "Publisher maker-checker negative path expected HTTP 403, received ${publisher_status}." >&2
    exit 1
  fi

  approver_status="$(curl -sS -X PATCH \
    "${BACKEND_URL%/}/api/praxis/config/domain-rules/definitions/${definition_id}/status" \
    -b "${approver_cookie_jar}" \
    -c "${approver_cookie_jar}" \
    -H "Origin: ${ORIGIN}" \
    -H 'Accept: application/json' \
    -H 'Content-Type: application/json' \
    --data-binary "@${request_file}" \
    -o "${response_file}" \
    -w '%{http_code}')"
  if [[ "${approver_status}" != "200" ]]; then
    echo "Governance approver could not approve definition ${definition_id} (HTTP ${approver_status})." >&2
    jq -c '{status, title, detail, message}' "${response_file}" 2>/dev/null >&2 || true
    exit 1
  fi
}

publish_reactive_determination() {
  local case_id="$1"
  local determination_key="$2"
  local operation_id="$3"
  local inputs_json="$4"
  local outputs_json="$5"
  local rule_key="human-resources.folhas-pagamento.rule.${case_id}.${SMOKE_RUN_ID}"
  local definition_request="${run_dir}/${case_id}-definition-request.json"
  local definition_response="${run_dir}/${case_id}-definition-response.json"
  local approval_request="${run_dir}/${case_id}-approval-request.json"
  local approval_response="${run_dir}/${case_id}-approval-response.json"
  local publication_request="${run_dir}/${case_id}-publication-request.json"
  local publication_response="${run_dir}/${case_id}-publication-response.json"
  local applied_response="${run_dir}/${case_id}-applied-response.json"
  local encoded_target_key
  local applied_status
  local applied_count

  encoded_target_key="$(jq -nr --arg value "${determination_key}" '$value|@uri')"
  applied_status="$(curl -sS \
    "${BACKEND_URL%/}/api/praxis/config/domain-rules/materializations?targetLayer=backend_determination&targetArtifactType=resource-reactive-determination&targetArtifactKey=${encoded_target_key}&status=applied" \
    -b "${publisher_cookie_jar}" \
    -H "Origin: ${ORIGIN}" \
    -H 'Accept: application/json' \
    -o "${applied_response}" \
    -w '%{http_code}')"
  if [[ "${applied_status}" != "200" ]]; then
    echo "Applied materialization lookup for ${case_id} failed with HTTP ${applied_status}." >&2
    exit 1
  fi
  applied_count="$(jq \
    --arg targetKey "${determination_key}" \
    --arg operationId "${operation_id}" \
    '[.[]
      | select(.targetLayer == "backend_determination")
      | select(.targetArtifactType == "resource-reactive-determination")
      | select(.targetArtifactKey == $targetKey)
      | select(.status == "applied")
      | select(.materializedPayload.operationRef.operationId == $operationId)
    ] | length' "${applied_response}")"
  if [[ "${applied_count}" -eq 1 ]]; then
    return
  fi
  if [[ "${applied_count}" -gt 1 ]]; then
    echo "Expected at most one applied materialization for ${case_id}, found ${applied_count}." >&2
    exit 1
  fi

  jq -n \
    --arg ruleKey "${rule_key}" \
    --arg targetKey "${determination_key}" \
    --arg operationId "${operation_id}" \
    --arg approverUsername "${APPROVER_USERNAME}" \
    --argjson inputs "${inputs_json}" \
    --argjson outputs "${outputs_json}" \
    '{
      ruleKey: $ruleKey,
      ruleType: "calculation",
      status: "proposed",
      contextKey: "human-resources",
      resourceKey: "human-resources.folhas-pagamento",
      serviceKey: "praxis-api-quickstart",
      semanticOwner: "payroll-owner",
      steward: "payroll-steward",
      definition: {
        summary: "Select the versioned authoritative payroll provider used by the reference host.",
        materializationTargets: [{
          targetLayer: "backend_determination",
          targetArtifactType: "resource-reactive-determination",
          targetArtifactKey: $targetKey
        }]
      },
      parameters: {
        reactiveDetermination: {
          operationId: $operationId,
          idempotent: true,
          persistence: "none",
          finalCommandRevalidation: true,
          inputs: $inputs,
          outputs: $outputs
        }
      },
      governance: {
        requiredApprovals: [$approverUsername],
        authorizedApprovers: [$approverUsername]
      }
    }' > "${definition_request}"

  config_post '/api/praxis/config/domain-rules/definitions' \
    "${definition_request}" "${definition_response}"
  local definition_id
  definition_id="$(jq -r '.id // empty' "${definition_response}")"
  if [[ -z "${definition_id}" ]]; then
    echo "Config did not return a definition id for ${case_id}." >&2
    exit 1
  fi

  jq -n '{
    status: "approved",
    validationResult: {
      valid: true,
      checks: ["reactive-determination-contract-reviewed"]
    }
  }' > "${approval_request}"
  approve_definition "${definition_id}" "${approval_request}" "${approval_response}"
  if [[ "$(jq -r '.status // empty' "${approval_response}")" != "approved" ]]; then
    echo "Config did not return an approved definition for ${case_id}." >&2
    exit 1
  fi

  jq -n --arg definitionId "${definition_id}" \
    '{ruleDefinitionId: $definitionId, applyEligibleMaterializations: true}' \
    > "${publication_request}"
  config_post '/api/praxis/config/domain-rules/publications' \
    "${publication_request}" "${publication_response}"

  jq -e \
    --arg targetKey "${determination_key}" \
    --arg operationId "${operation_id}" \
    '.publicationStatus == "published"
      and .definition.status == "active"
      and ([.materializations[]
        | select(.targetLayer == "backend_determination")
        | select(.targetArtifactType == "resource-reactive-determination")
        | select(.targetArtifactKey == $targetKey)
        | select(.status == "applied")
        | select(.materializedPayload.operationRef.operationId == $operationId)
      ] | length) == 1' "${publication_response}" >/dev/null
}

publish_reactive_determination \
  'net-salary' \
  'human-resources.payroll.net-salary' \
  'determinePayrollNetSalary' \
  '[{"resourcePointer":"/salarioBruto","requestPointer":"/salarioBruto"},{"resourcePointer":"/totalDescontos","requestPointer":"/totalDescontos"}]' \
  '[{"responsePointer":"/salarioLiquido","resourcePointer":"/salarioLiquido"}]'

publish_reactive_determination \
  'payment-date' \
  'human-resources.payroll.payment-date' \
  'determinePayrollPaymentDate' \
  '[{"resourcePointer":"/ano","requestPointer":"/ano"},{"resourcePointer":"/mes","requestPointer":"/mes"},{"resourcePointer":"/salarioLiquido","requestPointer":"/salarioLiquido"}]' \
  '[{"responsePointer":"/dataPagamento","resourcePointer":"/dataPagamento"}]'

post_case() {
  local case_id="$1"
  local path="$2"
  local payload="$3"
  local expected_status="$4"
  local response_file="${run_dir}/${case_id}-response.json"
  local actual_status

  actual_status="$(curl -sS "${BACKEND_URL%/}${path}" \
    -b "${executor_cookie_jar}" \
    -c "${executor_cookie_jar}" \
    -H "Origin: ${ORIGIN}" \
    -H 'Accept: application/json' \
    -H 'Content-Type: application/json' \
    --data-binary "${payload}" \
    -o "${response_file}" \
    -w '%{http_code}')"
  if [[ "${actual_status}" != "${expected_status}" ]]; then
    echo "${case_id}: expected HTTP ${expected_status}, received ${actual_status}." >&2
    jq -c '{status, title, detail, message}' "${response_file}" 2>/dev/null >&2 || true
    exit 1
  fi
  printf '%s\t%s\n' "${case_id}" "${response_file}"
}

known_address="$(post_case address-known \
  '/api/human-resources/enderecos/determinations/postal-address' \
  '{"cep":"01310-100"}' 200 | cut -f2)"
jq -e '
  .logradouro == "Avenida Paulista"
  and .bairro == "Bela Vista"
  and .cidade == "Sao Paulo"
  and .estado == "SP"
  and .decisionVersion == "quickstart-postal-directory-v1"
' "${known_address}" >/dev/null

post_case address-unknown \
  '/api/human-resources/enderecos/determinations/postal-address' \
  '{"cep":"99999-999"}' 422 >/dev/null

net_salary="$(post_case payroll-net-salary \
  '/api/human-resources/folhas-pagamento/determinations/net-salary' \
  '{"salarioBruto":10000.00,"totalDescontos":2450.35}' 200 | cut -f2)"
jq -e '
  (.salarioLiquido | tostring) == "7549.65"
  and .decisionVersion == "payroll-net-v1"
' "${net_salary}" >/dev/null

payment_date="$(post_case payroll-payment-date \
  '/api/human-resources/folhas-pagamento/determinations/payment-date' \
  '{"ano":2026,"mes":4,"salarioLiquido":7549.65}' 200 | cut -f2)"
jq -e '
  .dataPagamento == "2026-05-07"
  and .decisionVersion == "payroll-calendar-v1"
' "${payment_date}" >/dev/null

post_case payroll-invalid-discounts \
  '/api/human-resources/folhas-pagamento/determinations/net-salary' \
  '{"salarioBruto":1000.00,"totalDescontos":1000.01}' 422 >/dev/null

echo "Authenticated reactive-determination runtime smoke OK: 2 applied Config decisions, 3 successes and 2 governed negative cases."
