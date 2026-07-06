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

baseline_surface_ids=(
  "create"
  "list"
  "detail"
  "edit"
  "delete"
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
    printf '{"surfaces":[]}\n' > "$output_file"
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

assert_surface_openapi_path() {
  local openapi_file="$1"
  local surface_path="$2"
  local method="$3"
  local normalized_method
  normalized_method="$(printf '%s' "$method" | tr '[:upper:]' '[:lower:]')"

  if ! jq -e --arg path "$surface_path" --arg method "$normalized_method" '
    .paths[$path][$method] != null
  ' "$openapi_file" >/dev/null; then
    echo "Expected OpenAPI path '${method} ${surface_path}' for published cockpit surface." >&2
    jq --arg path "$surface_path" '.paths[$path] // null' "$openapi_file" >&2
    return 1
  fi
}

assert_filtered_schema_contract() {
  local schema_path="$1"
  local output_file="$2"
  local surface_id="$3"

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
    echo "Filtered schema for surface '${surface_id}' did not return a recognizable schema envelope." >&2
    cat "$output_file" >&2
    return 1
  fi
}

summary_file="$TMPDIR_RUN/surface-contracts.jsonl"
: > "$summary_file"

baseline_filter="$(printf '%s\n' "${baseline_surface_ids[@]}" | jq -R . | jq -s .)"

for group in "${groups[@]}"; do
  openapi_file="$TMPDIR_RUN/${group}-openapi.json"
  get_required_json "/v3/api-docs/${group}" "$openapi_file"

  while IFS= read -r slug; do
    resource_key="${group}.${slug}"
    surfaces_file="$TMPDIR_RUN/${resource_key}.surfaces.json"
    get_optional_json "/schemas/surfaces?resource=${resource_key}" "$surfaces_file"

    surface_count="$(jq --argjson baseline "$baseline_filter" '
      [
        (.surfaces // [])[]
        | select((.id as $id | $baseline | index($id)) | not)
      ]
      | length
    ' "$surfaces_file")"
    if [[ "$surface_count" -eq 0 ]]; then
      continue
    fi

    while IFS=$'\t' read -r surface_id kind scope title description surface_path method schema_url response_cardinality; do
      if [[ -z "$surface_id" || -z "$kind" || -z "$scope" || -z "$title" || -z "$description" || -z "$surface_path" || -z "$method" || -z "$schema_url" || -z "$response_cardinality" ]]; then
        echo "Cockpit surface contract for ${resource_key} is incomplete." >&2
        jq --argjson baseline "$baseline_filter" '
          (.surfaces // [])
          | map(select((.id as $id | $baseline | index($id)) | not))
          | map({id, kind, scope, title, description, path, method, schemaUrl, responseCardinality})
        ' "$surfaces_file" >&2
        exit 1
      fi

      assert_surface_openapi_path "$openapi_file" "$surface_path" "$method"
      schema_file="$TMPDIR_RUN/${resource_key}.${surface_id}.schema.json"
      assert_filtered_schema_contract "$schema_url" "$schema_file" "$surface_id"

      jq -n \
        --arg group "$group" \
        --arg resourceKey "$resource_key" \
        --arg surfaceId "$surface_id" \
        --arg kind "$kind" \
        --arg scope "$scope" \
        --arg method "$method" \
        --arg path "$surface_path" \
        '{group: $group, resourceKey: $resourceKey, surfaceId: $surfaceId, kind: $kind, scope: $scope, method: $method, path: $path}' >> "$summary_file"
    done < <(jq -r --argjson baseline "$baseline_filter" '
      (.surfaces // [])[]
      | select((.id as $id | $baseline | index($id)) | not)
      | [.id, .kind, .scope, .title, .description, .path, .method, .schemaUrl, .responseCardinality]
      | @tsv
    ' "$surfaces_file")
  done < <(resource_slugs_for_group "$group" "$openapi_file")
done

jq -s '{
  status: "cockpit-surface-contracts-ready",
  backendUrl: env.BACKEND_URL,
  totalSemanticSurfaces: length,
  groups: (
    group_by(.group)
    | map({
        group: .[0].group,
        surfaceCount: length,
        resources: (map(.resourceKey) | unique)
      })
  ),
  surfaces: .
}' "$summary_file"
