#!/usr/bin/env bash
set -euo pipefail

BACKEND_URL="${BACKEND_URL:-http://localhost:8088}"
TENANT_ID="${TENANT_ID:-default}"
ENVIRONMENT="${ENVIRONMENT:-dev}"
ORIGIN="${ORIGIN:-https://praxisui-dev.web.app}"
SERVICE_KEY="${SERVICE_KEY:-praxis-service}"
RESOURCE_KEYS_INPUT="${RESOURCE_KEYS:-}"
RELEASE_LIMIT="${RELEASE_LIMIT:-50}"

DEFAULT_RESOURCE_KEYS=(
  "human-resources.funcionarios"
  "human-resources.folhas-pagamento"
  "operations.missoes"
  "operations.acordos-regulatorios"
  "procurement.suppliers"
)

usage() {
  cat >&2 <<'USAGE'
Usage:
  scripts/verify-domain-catalog-context.sh [resourceKey...]

Examples:
  BACKEND_URL=http://localhost:8088 scripts/verify-domain-catalog-context.sh
  BACKEND_URL=http://localhost:8088 scripts/verify-domain-catalog-context.sh human-resources.funcionarios operations.missoes
  RESOURCE_KEYS="human-resources.funcionarios,operations.missoes" scripts/verify-domain-catalog-context.sh

The script reads persisted domain catalog releases and verifies that each
known resource has governance items for a representative query. It does not
ingest data or run database migrations.
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
trap 'rm -f "$releases_file" "$items_file"' EXIT

encoded_service_key="$(urlencode "$SERVICE_KEY")"
curl -fsS \
  "${BACKEND_URL%/}/api/praxis/config/domain-catalog/releases?serviceKey=${encoded_service_key}&limit=${RELEASE_LIMIT}" \
  -H "Origin: ${ORIGIN}" \
  -H "X-Tenant-ID: ${TENANT_ID}" \
  -H "X-Env: ${ENVIRONMENT}" \
  -o "$releases_file"

echo "Verifying persisted domain catalog governance context."
echo "BACKEND_URL=${BACKEND_URL}"
echo "SERVICE_KEY=${SERVICE_KEY}"
echo "TENANT_ID=${TENANT_ID}"
echo "ENVIRONMENT=${ENVIRONMENT}"

failures=0
for resource_key in "${resource_keys[@]}"; do
  if [[ -z "$resource_key" ]]; then
    continue
  fi

  verify_query="${VERIFY_QUERY:-$(default_verify_query_for "$resource_key")}"
  release_key="$(jq -r --arg needle ":${resource_key}:" '[.[] | select((.releaseKey // "") | contains($needle))][0].releaseKey // empty' "$releases_file")"

  echo
  echo "==> ${resource_key} (VERIFY_QUERY=${verify_query})"
  if [[ -z "$release_key" ]]; then
    echo "No persisted release found for resourceKey=${resource_key}" >&2
    failures=$((failures + 1))
    continue
  fi

  encoded_release_key="$(urlencode "$release_key")"
  encoded_query="$(urlencode "$verify_query")"
  curl -fsS \
    "${BACKEND_URL%/}/api/praxis/config/domain-catalog/items?releaseKey=${encoded_release_key}&type=governance&q=${encoded_query}&limit=5" \
    -H "Origin: ${ORIGIN}" \
    -H "X-Tenant-ID: ${TENANT_ID}" \
    -H "X-Env: ${ENVIRONMENT}" \
    -o "$items_file"

  item_count="$(jq 'length' "$items_file")"
  if [[ "$item_count" -eq 0 ]]; then
    echo "No governance items returned for releaseKey=${release_key} query=${verify_query}" >&2
    failures=$((failures + 1))
    continue
  fi

  jq --arg releaseKey "$release_key" --arg query "$verify_query" '{
    releaseKey: $releaseKey,
    query: $query,
    itemCount: length,
    items: [.[] | {
      itemKey,
      classification: .payload.classification,
      dataCategory: .payload.dataCategory,
      complianceTags: .payload.complianceTags,
      aiUsage: .payload.aiUsage
    }]
  }' "$items_file"
done

if [[ "$failures" -gt 0 ]]; then
  echo
  echo "Domain catalog context verification failed for ${failures} resource(s)." >&2
  exit 1
fi

echo
echo "Domain catalog context verification completed."
