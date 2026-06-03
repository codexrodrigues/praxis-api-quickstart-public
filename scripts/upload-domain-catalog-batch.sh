#!/usr/bin/env bash
set -euo pipefail

RESOURCE_KEYS_INPUT="${RESOURCE_KEYS:-}"
FORCE_INGEST="${FORCE_INGEST:-false}"
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
  scripts/upload-domain-catalog-batch.sh [resourceKey...]

Examples:
  BACKEND_URL=http://localhost:8088 scripts/upload-domain-catalog-batch.sh
  BACKEND_URL=http://localhost:8088 scripts/upload-domain-catalog-batch.sh human-resources.funcionarios operations.missoes
  RESOURCE_KEYS="human-resources.funcionarios,operations.missoes" scripts/upload-domain-catalog-batch.sh

By default the script delegates to scripts/ensure-domain-catalog-context.sh,
which is idempotent and verifies persisted governance through /items.

Set FORCE_INGEST=true to delegate each resource to scripts/upload-domain-catalog.sh
and always POST a new release.
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

script_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
single_upload_script="${script_dir}/upload-domain-catalog.sh"
ensure_script="${script_dir}/ensure-domain-catalog-context.sh"

if [[ "$FORCE_INGEST" != "true" ]]; then
  if [[ ! -x "$ensure_script" ]]; then
    echo "Missing executable dependency: ${ensure_script}" >&2
    exit 2
  fi
else
  if [[ ! -x "$single_upload_script" ]]; then
    echo "Missing executable dependency: ${single_upload_script}" >&2
    exit 2
  fi
fi

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

if [[ "${1:-}" == "-h" || "${1:-}" == "--help" ]]; then
  usage
  exit 0
fi

if [[ "${#resource_keys[@]}" -eq 0 ]]; then
  usage
  exit 2
fi

echo "Publishing ${#resource_keys[@]} domain catalog resource(s)."
echo "BACKEND_URL=${BACKEND_URL:-http://localhost:8088}"
echo "TENANT_ID=${TENANT_ID:-default}"
echo "ENVIRONMENT=${ENVIRONMENT:-dev}"
echo "FORCE_INGEST=${FORCE_INGEST}"

if [[ "$FORCE_INGEST" != "true" ]]; then
  "$ensure_script" "${resource_keys[@]}"
  exit 0
fi

for resource_key in "${resource_keys[@]}"; do
  if [[ -z "$resource_key" ]]; then
    continue
  fi

  verify_query="${VERIFY_QUERY:-$(default_verify_query_for "$resource_key")}"

  echo
  echo "==> ${resource_key} (VERIFY_QUERY=${verify_query})"
  VERIFY_QUERY="$verify_query" "$single_upload_script" "$resource_key"
done

echo
echo "Domain catalog batch ingestion completed."
