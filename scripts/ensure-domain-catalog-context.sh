#!/usr/bin/env bash
set -euo pipefail

BACKEND_URL="${BACKEND_URL:-http://localhost:8088}"
TENANT_ID="${TENANT_ID:-default}"
ENVIRONMENT="${ENVIRONMENT:-dev}"
ORIGIN="${ORIGIN:-https://praxisui-dev.web.app}"
SERVICE_KEY="${SERVICE_KEY:-praxis-service}"
RESOURCE_KEYS_INPUT="${RESOURCE_KEYS:-}"
RELEASE_LIMIT="${RELEASE_LIMIT:-50}"
REQUIRE_GOVERNANCE="${REQUIRE_GOVERNANCE:-true}"
CURL_CONNECT_TIMEOUT="${CURL_CONNECT_TIMEOUT:-10}"
CURL_MAX_TIME="${CURL_MAX_TIME:-120}"

DEFAULT_RESOURCE_KEYS=(
  "human-resources.funcionarios"
  "human-resources.cargos"
  "human-resources.departamentos"
  "human-resources.folhas-pagamento"
  "human-resources.vw-analytics-folha-pagamento"
  "operations.missoes"
  "operations.acordos-regulatorios"
  "procurement.suppliers"
)

usage() {
  cat >&2 <<'USAGE'
Usage:
  scripts/ensure-domain-catalog-context.sh [resourceKey...]

Examples:
  BACKEND_URL=http://localhost:8088 scripts/ensure-domain-catalog-context.sh
  BACKEND_URL=https://praxis-api-quickstart.onrender.com scripts/ensure-domain-catalog-context.sh operations.missoes
  RESOURCE_KEYS="human-resources.funcionarios,operations.missoes" scripts/ensure-domain-catalog-context.sh

The script is idempotent. For each resource it:
  1. reads persisted releases from /api/praxis/config/domain-catalog/releases;
  2. validates governance items through /api/praxis/config/domain-catalog/items;
  3. ingests /schemas/domain only when the persisted context is missing or incomplete;
  4. validates again through /items.

It does not call /api/praxis/config/domain-catalog/context, because /items is the
deterministic persistence contract used by E2E gates.
USAGE
}

default_verify_query_for() {
  case "$1" in
    human-resources.funcionarios)
      echo "cpf"
      ;;
    human-resources.cargos)
      echo "cargo"
      ;;
    human-resources.departamentos)
      echo "departamento"
      ;;
    human-resources.folhas-pagamento)
      echo "salario"
      ;;
    human-resources.vw-analytics-folha-pagamento)
      echo "salario"
      ;;
    operations.missoes | operations.acordos-regulatorios | procurement.suppliers)
      echo "status"
      ;;
    *)
      echo "cpf"
      ;;
  esac
}

urlencode() {
  jq -nr --arg value "$1" '$value|@uri'
}

fetch_releases() {
  local output_file="$1"
  local encoded_service_key
  encoded_service_key="$(urlencode "$SERVICE_KEY")"
  curl -fsS \
    --connect-timeout "$CURL_CONNECT_TIMEOUT" \
    --max-time "$CURL_MAX_TIME" \
    "${BACKEND_URL%/}/api/praxis/config/domain-catalog/releases?serviceKey=${encoded_service_key}&limit=${RELEASE_LIMIT}" \
    -H "Origin: ${ORIGIN}" \
    -H "X-Tenant-ID: ${TENANT_ID}" \
    -H "X-Env: ${ENVIRONMENT}" \
    -o "$output_file"
}

find_release_key_for() {
  local releases_file="$1"
  local resource_key="$2"
  jq -r --arg resourceKey "$resource_key" '
    [
      .[]
      | select(
          (.resourceKey // "") == $resourceKey
          or ((.releaseKey // "") | contains(":" + $resourceKey + ":"))
        )
    ][0].releaseKey // empty
  ' "$releases_file"
}

fetch_governance_items() {
  local release_key="$1"
  local verify_query="$2"
  local output_file="$3"
  local encoded_release_key
  local encoded_query
  encoded_release_key="$(urlencode "$release_key")"
  encoded_query="$(urlencode "$verify_query")"
  curl -fsS \
    --connect-timeout "$CURL_CONNECT_TIMEOUT" \
    --max-time "$CURL_MAX_TIME" \
    "${BACKEND_URL%/}/api/praxis/config/domain-catalog/items?releaseKey=${encoded_release_key}&type=governance&q=${encoded_query}&limit=5" \
    -H "Origin: ${ORIGIN}" \
    -H "X-Tenant-ID: ${TENANT_ID}" \
    -H "X-Env: ${ENVIRONMENT}" \
    -o "$output_file"
}

validate_persisted_context() {
  local resource_key="$1"
  local release_key="$2"
  local verify_query="$3"
  local items_file="$4"

  if [[ -z "$release_key" ]]; then
    return 1
  fi

  fetch_governance_items "$release_key" "$verify_query" "$items_file"
  local item_count
  item_count="$(jq 'length' "$items_file")"
  [[ "$item_count" -gt 0 ]]
}

ingest_resource() {
  local resource_key="$1"
  local catalog_file="$2"
  local verify_query="$3"

  curl -fsS \
    --connect-timeout "$CURL_CONNECT_TIMEOUT" \
    --max-time "$CURL_MAX_TIME" \
    "${BACKEND_URL%/}/schemas/domain?resourceKey=${resource_key}" \
    -o "$catalog_file"

  local schema_version
  local release_key
  local governance_count
  schema_version="$(jq -r '.schemaVersion // empty' "$catalog_file")"
  release_key="$(jq -r '.release.releaseKey // empty' "$catalog_file")"
  governance_count="$(jq '.governance | if type == "array" then length else 0 end' "$catalog_file")"

  if [[ "$schema_version" != "praxis.domain-catalog/v0.2" || -z "$release_key" ]]; then
    echo "Invalid domain catalog payload from /schemas/domain for resourceKey=${resource_key}." >&2
    jq '{schemaVersion, release, service}' "$catalog_file" >&2
    return 1
  fi

  if [[ "$REQUIRE_GOVERNANCE" == "true" && "$governance_count" -eq 0 ]]; then
    echo "Domain catalog has no governance items for resourceKey=${resource_key}." >&2
    jq '{schemaVersion, release, service, counts: {contexts:(.contexts|length), nodes:(.nodes|length), governance:(.governance|length)}}' "$catalog_file" >&2
    return 1
  fi

  jq --arg resourceKey "$resource_key" --arg query "$verify_query" '{
    resourceKey: $resourceKey,
    query: $query,
    releaseKey: .release.releaseKey,
    counts: {
      contexts:(.contexts|length),
      nodes:(.nodes|length),
      edges:(.edges|length),
      bindings:(.bindings|length),
      evidence:(.evidence|length),
      governance:(.governance|length)
    }
  }' "$catalog_file"

  local ingest_response
  local ingest_status_file
  local ingest_status
  ingest_response="$(mktemp "${TMPDIR:-/tmp}/praxis-domain-catalog-ingest.XXXXXX.json")"
  ingest_status_file="$(mktemp "${TMPDIR:-/tmp}/praxis-domain-catalog-ingest-status.XXXXXX.txt")"

  curl -sS -X POST "${BACKEND_URL%/}/api/praxis/config/domain-catalog/ingest" \
    --connect-timeout "$CURL_CONNECT_TIMEOUT" \
    --max-time "$CURL_MAX_TIME" \
    -H "Origin: ${ORIGIN}" \
    -H "Content-Type: application/json" \
    -H "X-Tenant-ID: ${TENANT_ID}" \
    -H "X-Env: ${ENVIRONMENT}" \
    --data-binary @"$catalog_file" \
    -o "$ingest_response" \
    -w '%{http_code}' > "$ingest_status_file"
  ingest_status="$(cat "$ingest_status_file")"

  if [[ "$ingest_status" == "409" ]]; then
    echo "Domain catalog ingest returned 409; treating as existing release and revalidating persisted context." >&2
    rm -f "$ingest_response" "$ingest_status_file"
    return 0
  fi
  if [[ "$ingest_status" != "200" && "$ingest_status" != "201" && "$ingest_status" != "202" ]]; then
    echo "Unexpected domain catalog ingest response (HTTP ${ingest_status})." >&2
    cat "$ingest_response" >&2 || true
    rm -f "$ingest_response" "$ingest_status_file"
    return 1
  fi

  jq '{releaseId, releaseKey, itemCount}' "$ingest_response"
  rm -f "$ingest_response" "$ingest_status_file"
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

resource_keys=()
if [[ "$#" -gt 0 ]]; then
  resource_keys=("$@")
elif [[ -n "$RESOURCE_KEYS_INPUT" ]]; then
  normalized="${RESOURCE_KEYS_INPUT//,/ }"
  # shellcheck disable=SC2206
  resource_keys=($normalized)
else
  resource_keys=("${DEFAULT_RESOURCE_KEYS[@]}")
fi

if [[ "${#resource_keys[@]}" -eq 0 ]]; then
  usage
  exit 2
fi

releases_file="$(mktemp "${TMPDIR:-/tmp}/praxis-domain-catalog-releases.XXXXXX.json")"
items_file="$(mktemp "${TMPDIR:-/tmp}/praxis-domain-catalog-items.XXXXXX.json")"
catalog_file="$(mktemp "${TMPDIR:-/tmp}/praxis-domain-catalog.XXXXXX.json")"
trap 'rm -f "$releases_file" "$items_file" "$catalog_file"' EXIT

echo "Ensuring persisted domain catalog governance context."
echo "BACKEND_URL=${BACKEND_URL}"
echo "SERVICE_KEY=${SERVICE_KEY}"
echo "TENANT_ID=${TENANT_ID}"
echo "ENVIRONMENT=${ENVIRONMENT}"
echo "CURL_MAX_TIME=${CURL_MAX_TIME}s"

failures=0
for resource_key in "${resource_keys[@]}"; do
  if [[ -z "$resource_key" ]]; then
    continue
  fi

  verify_query="${VERIFY_QUERY:-$(default_verify_query_for "$resource_key")}"
  echo
  echo "==> ${resource_key} (VERIFY_QUERY=${verify_query})"

  fetch_releases "$releases_file"
  release_key="$(find_release_key_for "$releases_file" "$resource_key")"

  if validate_persisted_context "$resource_key" "$release_key" "$verify_query" "$items_file"; then
    jq --arg releaseKey "$release_key" --arg resourceKey "$resource_key" --arg query "$verify_query" '{
      status: "ready",
      resourceKey: $resourceKey,
      releaseKey: $releaseKey,
      query: $query,
      itemCount: length
    }' "$items_file"
    continue
  fi

  echo "Persisted governance context missing or incomplete. Ingesting ${resource_key}..."
  if ! ingest_resource "$resource_key" "$catalog_file" "$verify_query"; then
    failures=$((failures + 1))
    continue
  fi

  fetch_releases "$releases_file"
  release_key="$(find_release_key_for "$releases_file" "$resource_key")"
  if validate_persisted_context "$resource_key" "$release_key" "$verify_query" "$items_file"; then
    jq --arg releaseKey "$release_key" --arg resourceKey "$resource_key" --arg query "$verify_query" '{
      status: "ingested",
      resourceKey: $resourceKey,
      releaseKey: $releaseKey,
      query: $query,
      itemCount: length
    }' "$items_file"
  else
    echo "Persisted governance context still missing after ingestion for resourceKey=${resource_key}" >&2
    failures=$((failures + 1))
  fi
done

if [[ "$failures" -gt 0 ]]; then
  echo
  echo "Domain catalog context ensure failed for ${failures} resource(s)." >&2
  exit 1
fi

echo
echo "Domain catalog context is ready."
