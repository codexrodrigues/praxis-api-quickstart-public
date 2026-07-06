#!/usr/bin/env bash
set -euo pipefail

BACKEND_URL="${BACKEND_URL:-https://praxis-api-quickstart.onrender.com}"
export BACKEND_URL

TMPDIR_RUN="$(mktemp -d)"
cleanup() {
  rm -rf "$TMPDIR_RUN"
}
trap cleanup EXIT

groups=(
  "human-resources"
  "operations"
  "procurement"
  "assets"
  "risk-intelligence"
)

get_required_json() {
  local path="$1"
  local output_file="$2"

  curl --max-time 40 -fsS "${BACKEND_URL%/}${path}" \
    -H "Accept: application/json" \
    -o "$output_file"
}

get_optional_json() {
  local path="$1"
  local output_file="$2"

  local status
  status="$(curl --max-time 20 -sS -w '%{http_code}' "${BACKEND_URL%/}${path}" \
    -H "Accept: application/json" \
    -o "$output_file")"

  if [[ "$status" == "404" ]]; then
    printf '{"actions":[]}\n' > "$output_file"
    return 0
  fi

  if [[ "$status" -lt 200 || "$status" -ge 300 ]]; then
    echo "Unexpected HTTP ${status} for ${path}." >&2
    cat "$output_file" >&2 || true
    return 1
  fi
}

resource_slugs_for_group() {
  local group="$1"
  local openapi_file="$2"

  jq -r --arg group "$group" '
    .paths
    | keys
    | map(select(startswith("/api/" + $group + "/")))
    | map(split("/")[3])
    | unique[]
  ' "$openapi_file"
}

assert_action_openapi_path() {
  local openapi_file="$1"
  local action_path="$2"
  local method="$3"
  local normalized_method
  normalized_method="$(printf '%s' "$method" | tr '[:upper:]' '[:lower:]')"

  if ! jq -e --arg path "$action_path" --arg method "$normalized_method" '
    .paths[$path][$method] != null
  ' "$openapi_file" >/dev/null; then
    echo "Expected OpenAPI path '${method} ${action_path}' for published workflow action." >&2
    jq --arg path "$action_path" '.paths[$path] // null' "$openapi_file" >&2
    return 1
  fi
}

assert_filtered_schema_contract() {
  local schema_path="$1"
  local output_file="$2"
  local action_id="$3"
  local schema_type="$4"

  get_required_json "$schema_path" "$output_file"
  if ! jq -e '
    type == "object"
    and (
      has("fields")
      or has("properties")
      or has("schema")
      or has("data")
      or has("components")
    )
  ' "$output_file" >/dev/null; then
    echo "Filtered ${schema_type} schema for action '${action_id}' did not return a recognizable schema envelope." >&2
    cat "$output_file" >&2
    return 1
  fi
}

summary_file="$TMPDIR_RUN/action-contracts.jsonl"
: > "$summary_file"

for group in "${groups[@]}"; do
  openapi_file="$TMPDIR_RUN/${group}-openapi.json"
  get_required_json "/v3/api-docs/${group}" "$openapi_file"

  while IFS= read -r slug; do
    resource_key="${group}.${slug}"
    actions_file="$TMPDIR_RUN/${resource_key}.actions.json"
    get_optional_json "/schemas/actions?resource=${resource_key}" "$actions_file"

    action_count="$(jq '(.actions // []) | length' "$actions_file")"
    if [[ "$action_count" -eq 0 ]]; then
      continue
    fi

    while IFS=$'\t' read -r action_id action_path method request_schema_url response_schema_url; do
      if [[ -z "$action_id" || -z "$action_path" || -z "$method" || -z "$request_schema_url" || -z "$response_schema_url" ]]; then
        echo "Action contract for ${resource_key} is incomplete." >&2
        jq '.actions[] | {id, path, method, requestSchemaUrl, responseSchemaUrl}' "$actions_file" >&2
        exit 1
      fi

      assert_action_openapi_path "$openapi_file" "$action_path" "$method"
      request_schema_file="$TMPDIR_RUN/${resource_key}.${action_id}.request-schema.json"
      response_schema_file="$TMPDIR_RUN/${resource_key}.${action_id}.response-schema.json"
      assert_filtered_schema_contract "$request_schema_url" "$request_schema_file" "$action_id" "request"
      assert_filtered_schema_contract "$response_schema_url" "$response_schema_file" "$action_id" "response"

      jq -n \
        --arg group "$group" \
        --arg resourceKey "$resource_key" \
        --arg actionId "$action_id" \
        --arg method "$method" \
        --arg path "$action_path" \
        '{group: $group, resourceKey: $resourceKey, actionId: $actionId, method: $method, path: $path}' >> "$summary_file"
    done < <(jq -r '
      (.actions // [])[]
      | [.id, .path, .method, .requestSchemaUrl, .responseSchemaUrl]
      | @tsv
    ' "$actions_file")
  done < <(resource_slugs_for_group "$group" "$openapi_file")
done

jq -s '{
  status: "cockpit-action-contracts-ready",
  backendUrl: env.BACKEND_URL,
  totalActions: length,
  groups: (
    group_by(.group)
    | map({
        group: .[0].group,
        actionCount: length,
        resources: (map(.resourceKey) | unique)
      })
  ),
  actions: .
}' "$summary_file"
