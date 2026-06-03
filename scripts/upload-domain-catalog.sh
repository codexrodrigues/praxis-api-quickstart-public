#!/usr/bin/env bash
set -euo pipefail

BACKEND_URL="${BACKEND_URL:-http://localhost:8088}"
RESOURCE_KEY="${1:-${RESOURCE_KEY:-}}"
TENANT_ID="${TENANT_ID:-default}"
ENVIRONMENT="${ENVIRONMENT:-dev}"
ORIGIN="${ORIGIN:-https://praxisui-dev.web.app}"
REQUIRE_GOVERNANCE="${REQUIRE_GOVERNANCE:-true}"
VERIFY_QUERY="${VERIFY_QUERY:-cpf}"

if [[ -z "$RESOURCE_KEY" ]]; then
  echo "Usage: BACKEND_URL=http://localhost:8088 $0 <resourceKey>" >&2
  echo "Example: $0 human-resources.funcionarios" >&2
  exit 2
fi

for command in curl jq; do
  if ! command -v "$command" >/dev/null 2>&1; then
    echo "Missing required command: $command" >&2
    exit 2
  fi
done

catalog_file="$(mktemp "${TMPDIR:-/tmp}/praxis-domain-catalog.XXXXXX.json")"
trap 'rm -f "$catalog_file"' EXIT

curl -fsS \
  "${BACKEND_URL%/}/schemas/domain?resourceKey=${RESOURCE_KEY}" \
  -o "$catalog_file"

schema_version="$(jq -r '.schemaVersion // empty' "$catalog_file")"
release_key="$(jq -r '.release.releaseKey // empty' "$catalog_file")"
service_key="$(jq -r '.service.serviceKey // "praxis-service"' "$catalog_file")"
governance_count="$(jq '.governance | if type == "array" then length else 0 end' "$catalog_file")"

if [[ -z "$schema_version" || -z "$release_key" ]]; then
  echo "Invalid domain catalog payload from /schemas/domain." >&2
  jq '{schemaVersion, release, service}' "$catalog_file" >&2
  exit 1
fi

if [[ "$REQUIRE_GOVERNANCE" == "true" && "$governance_count" -eq 0 ]]; then
  echo "Domain catalog has no governance items. Set REQUIRE_GOVERNANCE=false to ingest anyway." >&2
  jq '{schemaVersion, release, service, counts: {contexts:(.contexts|length), nodes:(.nodes|length), governance:(.governance|length)}}' "$catalog_file" >&2
  exit 1
fi

echo "Catalog summary:"
jq '{schemaVersion, service: .service, release: .release, counts: {contexts:(.contexts|length), nodes:(.nodes|length), edges:(.edges|length), bindings:(.bindings|length), evidence:(.evidence|length), governance:(.governance|length)}}' "$catalog_file"

echo
echo "Ingesting release ${release_key}..."
curl -fsS -X POST "${BACKEND_URL%/}/api/praxis/config/domain-catalog/ingest" \
  -H "Origin: ${ORIGIN}" \
  -H "Content-Type: application/json" \
  -H "X-Tenant-ID: ${TENANT_ID}" \
  -H "X-Env: ${ENVIRONMENT}" \
  --data-binary @"$catalog_file" | jq .

if [[ -n "$VERIFY_QUERY" ]]; then
  echo
  echo "Verifying persisted governance context for query '${VERIFY_QUERY}'..."
  curl -fsS \
    "${BACKEND_URL%/}/api/praxis/config/domain-catalog/context?serviceKey=${service_key}&resourceKey=${RESOURCE_KEY}&type=governance&q=${VERIFY_QUERY}&limit=5" \
    -H "Origin: ${ORIGIN}" \
    -H "X-Tenant-ID: ${TENANT_ID}" \
    -H "X-Env: ${ENVIRONMENT}" \
    | jq '{schemaVersion, release: .release.releaseKey, guidance: .retrievalGuidance, items: [.items[] | {itemKey, classification: .payload.classification, dataCategory: .payload.dataCategory, complianceTags: .payload.complianceTags, aiUsage: .payload.aiUsage}]}'
fi
