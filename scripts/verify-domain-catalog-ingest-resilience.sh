#!/usr/bin/env bash
set -euo pipefail

BACKEND_URL="${BACKEND_URL:-http://localhost:8088}"
TENANT_ID="${TENANT_ID:-default}"
ENVIRONMENT="${ENVIRONMENT:-dev}"
ORIGIN="${ORIGIN:-https://praxisui-dev.web.app}"
SERVICE_KEY="${SERVICE_KEY:-praxis-service}"
RESOURCE_KEYS_INPUT="${RESOURCE_KEYS:-}"
REQUIRE_GOVERNANCE="${REQUIRE_GOVERNANCE:-true}"
REQUIRE_SOURCE_HASH="${REQUIRE_SOURCE_HASH:-true}"
MAX_IDEMPOTENT_INGEST_MS="${MAX_IDEMPOTENT_INGEST_MS:-10000}"
MAX_INITIAL_INGEST_MS="${MAX_INITIAL_INGEST_MS:-0}"

DEFAULT_RESOURCE_KEYS=(
  "human-resources.funcionarios"
  "operations.missoes"
)

usage() {
  cat >&2 <<'USAGE'
Usage:
  scripts/verify-domain-catalog-ingest-resilience.sh [resourceKey...]

Examples:
  BACKEND_URL=http://localhost:8088 scripts/verify-domain-catalog-ingest-resilience.sh
  BACKEND_URL=https://praxis-api-quickstart.onrender.com scripts/verify-domain-catalog-ingest-resilience.sh human-resources.funcionarios operations.missoes
  RESOURCE_KEYS="human-resources.funcionarios,operations.missoes" scripts/verify-domain-catalog-ingest-resilience.sh

The script posts the current /schemas/domain payload twice for each resource and
verifies that stable catalogs are idempotent:
  1. both POST /api/praxis/config/domain-catalog/ingest calls return the same release;
  2. the repeated ingest stays within MAX_IDEMPOTENT_INGEST_MS;
  3. persisted governance items remain queryable through /items.

Set MAX_INITIAL_INGEST_MS to a positive value to also bound the first POST.
USAGE
}

default_verify_query_for() {
  case "$1" in
    human-resources.funcionarios)
      echo "cpf"
      ;;
    human-resources.folhas-pagamento)
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

curl_json() {
  local method="$1"
  local url="$2"
  local body_file="$3"
  local output_file="$4"
  local meta_file="$TMP_DIR/curl-meta.$RANDOM.txt"
  local http_code
  local time_total

  if [[ -n "$body_file" ]]; then
    curl -sS -X "$method" "$url" \
      -H "Origin: ${ORIGIN}" \
      -H "Content-Type: application/json" \
      -H "X-Tenant-ID: ${TENANT_ID}" \
      -H "X-Env: ${ENVIRONMENT}" \
      --data-binary @"$body_file" \
      -o "$output_file" \
      -w "%{http_code} %{time_total}" >"$meta_file"
  else
    curl -sS -X "$method" "$url" \
      -H "Origin: ${ORIGIN}" \
      -H "X-Tenant-ID: ${TENANT_ID}" \
      -H "X-Env: ${ENVIRONMENT}" \
      -o "$output_file" \
      -w "%{http_code} %{time_total}" >"$meta_file"
  fi

  read -r http_code time_total <"$meta_file"
  CURL_TIME_MS="$(awk -v seconds="$time_total" 'BEGIN { printf "%d", seconds * 1000 }')"

  if [[ "$http_code" -lt 200 || "$http_code" -ge 300 ]]; then
    echo "HTTP ${http_code} from ${method} ${url}" >&2
    cat "$output_file" >&2
    return 1
  fi
}

fetch_catalog() {
  local resource_key="$1"
  local output_file="$2"
  local encoded_resource_key
  encoded_resource_key="$(urlencode "$resource_key")"

  curl_json "GET" "${BACKEND_URL%/}/schemas/domain?resourceKey=${encoded_resource_key}" "" "$output_file"

  local schema_version
  local release_key
  local source_hash
  local governance_count
  schema_version="$(jq -r '.schemaVersion // empty' "$output_file")"
  release_key="$(jq -r '.release.releaseKey // empty' "$output_file")"
  source_hash="$(jq -r '.release.sourceHash // empty' "$output_file")"
  governance_count="$(jq '.governance | if type == "array" then length else 0 end' "$output_file")"

  if [[ "$schema_version" != "praxis.domain-catalog/v0.2" || -z "$release_key" ]]; then
    echo "Invalid domain catalog payload from /schemas/domain for resourceKey=${resource_key}." >&2
    jq '{schemaVersion, release, service}' "$output_file" >&2
    return 1
  fi

  if [[ "$REQUIRE_SOURCE_HASH" == "true" && -z "$source_hash" ]]; then
    echo "Domain catalog release is missing sourceHash for resourceKey=${resource_key}." >&2
    jq '{schemaVersion, release}' "$output_file" >&2
    return 1
  fi

  if [[ "$REQUIRE_GOVERNANCE" == "true" && "$governance_count" -eq 0 ]]; then
    echo "Domain catalog has no governance items for resourceKey=${resource_key}." >&2
    jq '{schemaVersion, release, service, counts: {contexts:(.contexts|length), nodes:(.nodes|length), governance:(.governance|length)}}' "$output_file" >&2
    return 1
  fi
}

ingest_catalog() {
  local catalog_file="$1"
  local output_file="$2"
  curl_json "POST" "${BACKEND_URL%/}/api/praxis/config/domain-catalog/ingest" "$catalog_file" "$output_file"
}

fetch_governance_items() {
  local release_key="$1"
  local verify_query="$2"
  local output_file="$3"
  local encoded_release_key
  local encoded_query
  encoded_release_key="$(urlencode "$release_key")"
  encoded_query="$(urlencode "$verify_query")"

  curl_json "GET" "${BACKEND_URL%/}/api/praxis/config/domain-catalog/items?releaseKey=${encoded_release_key}&type=governance&q=${encoded_query}&limit=5" "" "$output_file"
}

assert_same_release() {
  local resource_key="$1"
  local first_file="$2"
  local second_file="$3"
  local first_release_id
  local second_release_id
  local first_release_key
  local second_release_key
  local first_item_count
  local second_item_count

  first_release_id="$(jq -r '.releaseId // empty' "$first_file")"
  second_release_id="$(jq -r '.releaseId // empty' "$second_file")"
  first_release_key="$(jq -r '.releaseKey // empty' "$first_file")"
  second_release_key="$(jq -r '.releaseKey // empty' "$second_file")"
  first_item_count="$(jq -r '.itemCount // empty' "$first_file")"
  second_item_count="$(jq -r '.itemCount // empty' "$second_file")"

  if [[ -z "$first_release_id" || -z "$second_release_id" || "$first_release_id" != "$second_release_id" || "$first_release_key" != "$second_release_key" || "$first_item_count" != "$second_item_count" ]]; then
    echo "Repeated ingest was not idempotent for resourceKey=${resource_key}." >&2
    jq -n --slurpfile first "$first_file" --slurpfile second "$second_file" '{first: $first[0], second: $second[0]}' >&2
    return 1
  fi
}

if [[ "${1:-}" == "-h" || "${1:-}" == "--help" ]]; then
  usage
  exit 0
fi

for command in curl jq awk; do
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

TMP_DIR="$(mktemp -d "${TMPDIR:-/tmp}/praxis-domain-catalog-resilience.XXXXXX")"
trap 'rm -rf "$TMP_DIR"' EXIT
CURL_TIME_MS=0

echo "Verifying idempotent domain catalog ingestion resilience."
echo "BACKEND_URL=${BACKEND_URL}"
echo "SERVICE_KEY=${SERVICE_KEY}"
echo "TENANT_ID=${TENANT_ID}"
echo "ENVIRONMENT=${ENVIRONMENT}"
echo "MAX_IDEMPOTENT_INGEST_MS=${MAX_IDEMPOTENT_INGEST_MS}"

failures=0
for resource_key in "${resource_keys[@]}"; do
  if [[ -z "$resource_key" ]]; then
    continue
  fi

  verify_query="${VERIFY_QUERY:-$(default_verify_query_for "$resource_key")}"
  catalog_file="$TMP_DIR/catalog.${resource_key}.json"
  first_file="$TMP_DIR/ingest-first.${resource_key}.json"
  second_file="$TMP_DIR/ingest-second.${resource_key}.json"
  items_file="$TMP_DIR/items.${resource_key}.json"

  echo
  echo "==> ${resource_key} (VERIFY_QUERY=${verify_query})"

  if ! fetch_catalog "$resource_key" "$catalog_file"; then
    failures=$((failures + 1))
    continue
  fi

  release_key="$(jq -r '.release.releaseKey' "$catalog_file")"
  source_hash="$(jq -r '.release.sourceHash // empty' "$catalog_file")"

  if ! ingest_catalog "$catalog_file" "$first_file"; then
    failures=$((failures + 1))
    continue
  fi
  first_elapsed_ms="$CURL_TIME_MS"

  if [[ "$MAX_INITIAL_INGEST_MS" -gt 0 && "$first_elapsed_ms" -gt "$MAX_INITIAL_INGEST_MS" ]]; then
    echo "Initial ingest for resourceKey=${resource_key} exceeded MAX_INITIAL_INGEST_MS=${MAX_INITIAL_INGEST_MS}: ${first_elapsed_ms}ms" >&2
    failures=$((failures + 1))
    continue
  fi

  if ! ingest_catalog "$catalog_file" "$second_file"; then
    failures=$((failures + 1))
    continue
  fi
  second_elapsed_ms="$CURL_TIME_MS"

  if ! assert_same_release "$resource_key" "$first_file" "$second_file"; then
    failures=$((failures + 1))
    continue
  fi

  if [[ "$MAX_IDEMPOTENT_INGEST_MS" -gt 0 && "$second_elapsed_ms" -gt "$MAX_IDEMPOTENT_INGEST_MS" ]]; then
    echo "Repeated ingest for resourceKey=${resource_key} exceeded MAX_IDEMPOTENT_INGEST_MS=${MAX_IDEMPOTENT_INGEST_MS}: ${second_elapsed_ms}ms" >&2
    failures=$((failures + 1))
    continue
  fi

  if ! fetch_governance_items "$release_key" "$verify_query" "$items_file"; then
    failures=$((failures + 1))
    continue
  fi

  item_result_count="$(jq 'length' "$items_file")"
  if [[ "$item_result_count" -eq 0 ]]; then
    echo "No persisted governance items returned for releaseKey=${release_key} query=${verify_query}" >&2
    failures=$((failures + 1))
    continue
  fi

  jq -n \
    --arg status "idempotent-ingest-ready" \
    --arg resourceKey "$resource_key" \
    --arg releaseKey "$release_key" \
    --arg sourceHash "$source_hash" \
    --arg query "$verify_query" \
    --argjson firstElapsedMs "$first_elapsed_ms" \
    --argjson secondElapsedMs "$second_elapsed_ms" \
    --argjson maxIdempotentIngestMs "$MAX_IDEMPOTENT_INGEST_MS" \
    --argjson itemCount "$(jq '.itemCount' "$second_file")" \
    --argjson governanceResultCount "$item_result_count" \
    '{
      status: $status,
      resourceKey: $resourceKey,
      releaseKey: $releaseKey,
      sourceHash: $sourceHash,
      query: $query,
      itemCount: $itemCount,
      firstElapsedMs: $firstElapsedMs,
      secondElapsedMs: $secondElapsedMs,
      maxIdempotentIngestMs: $maxIdempotentIngestMs,
      governanceResultCount: $governanceResultCount
    }'
done

if [[ "$failures" -gt 0 ]]; then
  echo
  echo "Domain catalog ingest resilience verification failed for ${failures} resource(s)." >&2
  exit 1
fi

echo
echo "Domain catalog ingest resilience verification completed."
