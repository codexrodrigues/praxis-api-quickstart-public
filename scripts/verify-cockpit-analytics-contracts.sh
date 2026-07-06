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

stats_operations=(
  "group-by"
  "timeseries"
  "distribution"
)

get_required_json() {
  local path="$1"
  local output_file="$2"

  curl --retry 6 --retry-all-errors --retry-delay 5 --retry-max-time 180 --max-time 40 -fsS "${BACKEND_URL%/}${path}" \
    -H "Accept: application/json" \
    -o "$output_file"
}

get_optional_json() {
  local path="$1"
  local output_file="$2"

  local status
  status="$(curl --retry 6 --retry-all-errors --retry-delay 5 --retry-max-time 180 --max-time 20 -sS -w '%{http_code}' "${BACKEND_URL%/}${path}" \
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

urlencode_path() {
  jq -nr --arg value "$1" '$value | @uri'
}

openapi_file_for_group() {
  local group="$1"
  local openapi_file="$TMPDIR_RUN/${group}-openapi.json"

  if [[ ! -f "$openapi_file" ]]; then
    get_required_json "/v3/api-docs/${group}" "$openapi_file"
  fi

  printf '%s\n' "$openapi_file"
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

stats_paths_for_group() {
  local openapi_file="$1"

  jq -r '
    .paths
    | to_entries[]
    | select(.key | test("/stats/(group-by|timeseries|distribution)$"))
    | select(.value.post != null)
    | .key
  ' "$openapi_file"
}

assert_filtered_schema_contract() {
  local schema_file="$1"
  local context="$2"

  if ! jq -e '
    type == "object"
    and (
      has("properties")
      or has("fields")
      or has("schema")
      or has("data")
      or has("components")
    )
  ' "$schema_file" >/dev/null; then
    echo "Filtered schema for ${context} did not return a recognizable schema envelope." >&2
    cat "$schema_file" >&2
    return 1
  fi
}

assert_stats_request_schema() {
  local schema_file="$1"
  local stats_operation="$2"
  local context="$3"

  assert_filtered_schema_contract "$schema_file" "${context} request"

  if ! jq -e '
    (.properties // {}) | has("field")
  ' "$schema_file" >/dev/null; then
    echo "Stats request schema for ${context} must expose the analytic field selector." >&2
    jq '{properties: ((.properties // {}) | keys)}' "$schema_file" >&2
    return 1
  fi

  if [[ "$stats_operation" == "timeseries" ]]; then
    if ! jq -e '(.properties // {}) | has("granularity")' "$schema_file" >/dev/null; then
      echo "Timeseries request schema for ${context} must expose granularity." >&2
      jq '{properties: ((.properties // {}) | keys)}' "$schema_file" >&2
      return 1
    fi
  fi
}

assert_stats_response_schema() {
  local schema_file="$1"
  local stats_operation="$2"
  local context="$3"

  assert_filtered_schema_contract "$schema_file" "${context} response"

  case "$stats_operation" in
    group-by)
      if ! jq -e '(.properties // {}) | has("buckets")' "$schema_file" >/dev/null; then
        echo "Group-by response schema for ${context} must expose buckets." >&2
        jq '{properties: ((.properties // {}) | keys)}' "$schema_file" >&2
        return 1
      fi
      ;;
    timeseries)
      if ! jq -e '(.properties // {}) | has("points")' "$schema_file" >/dev/null; then
        echo "Timeseries response schema for ${context} must expose points." >&2
        jq '{properties: ((.properties // {}) | keys)}' "$schema_file" >&2
        return 1
      fi
      ;;
    distribution)
      if ! jq -e '(.properties // {}) | has("buckets")' "$schema_file" >/dev/null; then
        echo "Distribution response schema for ${context} must expose buckets." >&2
        jq '{properties: ((.properties // {}) | keys)}' "$schema_file" >&2
        return 1
      fi
      ;;
  esac
}

analytics_summary="$TMPDIR_RUN/analytics-contracts.jsonl"
chart_summary="$TMPDIR_RUN/chart-surfaces.jsonl"
: > "$analytics_summary"
: > "$chart_summary"

for group in "${groups[@]}"; do
  openapi_file="$(openapi_file_for_group "$group")"

  while IFS= read -r stats_path; do
    stats_operation="${stats_path##*/}"
    resource_slug="$(printf '%s' "$stats_path" | awk -F'/' '{print $4}')"
    resource_key="${group}.${resource_slug}"
    encoded_path="$(urlencode_path "$stats_path")"

    request_schema_file="$TMPDIR_RUN/${group}.${resource_slug}.${stats_operation}.request.json"
    response_schema_file="$TMPDIR_RUN/${group}.${resource_slug}.${stats_operation}.response.json"
    get_required_json "/schemas/filtered?path=${encoded_path}&operation=post&schemaType=request&idField=id&readOnly=false" "$request_schema_file"
    get_required_json "/schemas/filtered?path=${encoded_path}&operation=post&schemaType=response&idField=id&readOnly=false" "$response_schema_file"

    assert_stats_request_schema "$request_schema_file" "$stats_operation" "${resource_key}.${stats_operation}"
    assert_stats_response_schema "$response_schema_file" "$stats_operation" "${resource_key}.${stats_operation}"

    analytics_projection_count="$(jq '
      [
        (.["x-ui"].analytics.projections // [])[]
        | select(.source.kind == "praxis.stats")
      ]
      | length
    ' "$request_schema_file")"

    jq -n \
      --arg group "$group" \
      --arg resourceKey "$resource_key" \
      --arg path "$stats_path" \
      --arg operation "$stats_operation" \
      --argjson analyticsProjectionCount "$analytics_projection_count" \
      '{
        group: $group,
        resourceKey: $resourceKey,
        path: $path,
        operation: $operation,
        analyticsProjectionCount: $analyticsProjectionCount
      }' >> "$analytics_summary"
  done < <(stats_paths_for_group "$openapi_file")

  while IFS= read -r slug; do
    resource_key="${group}.${slug}"
    surfaces_file="$TMPDIR_RUN/${resource_key}.surfaces.json"
    get_optional_json "/schemas/surfaces?resource=${resource_key}" "$surfaces_file"

    while IFS=$'\t' read -r surface_id surface_path schema_url; do
      [[ -z "$surface_id" ]] && continue

      if [[ "$surface_path" != *"/stats/"* ]]; then
        echo "Chart surface ${resource_key}.${surface_id} must point to a stats endpoint." >&2
        jq --arg id "$surface_id" '.surfaces[] | select(.id == $id)' "$surfaces_file" >&2
        exit 1
      fi

      schema_file="$TMPDIR_RUN/${resource_key}.${surface_id}.chart-response.json"
      get_required_json "$schema_url" "$schema_file"
      assert_filtered_schema_contract "$schema_file" "${resource_key}.${surface_id} chart response"

      if ! jq -e '
        [
          (.["x-ui"].analytics.projections // [])[]
          | select(.source.kind == "praxis.stats")
        ]
        | length > 0
      ' "$schema_file" >/dev/null; then
        echo "Chart surface ${resource_key}.${surface_id} must publish x-ui.analytics projection sourced by praxis.stats." >&2
        jq '."x-ui".analytics // null' "$schema_file" >&2
        exit 1
      fi

      jq -n \
        --arg group "$group" \
        --arg resourceKey "$resource_key" \
        --arg surfaceId "$surface_id" \
        --arg path "$surface_path" \
        '{group: $group, resourceKey: $resourceKey, surfaceId: $surfaceId, path: $path}' >> "$chart_summary"
    done < <(jq -r '
      (.surfaces // [])[]
      | select(.kind == "CHART")
      | [.id, .path, .schemaUrl]
      | @tsv
    ' "$surfaces_file")
  done < <(resource_slugs_for_group "$group" "$openapi_file")
done

jq -n \
  --slurpfile analytics "$analytics_summary" \
  --slurpfile charts "$chart_summary" \
  '{
    status: "cockpit-analytics-contracts-ready",
    backendUrl: env.BACKEND_URL,
    totalStatsEndpoints: ($analytics | length),
    totalChartSurfaces: ($charts | length),
    groups: (
      $analytics
      | group_by(.group)
      | map({
          group: .[0].group,
          statsEndpointCount: length,
          resources: (map(.resourceKey) | unique),
          operations: (map(.operation) | unique)
        })
    ),
    chartSurfaces: $charts,
    statsEndpointsWithAnalyticsProjection: (
      $analytics
      | map(select(.analyticsProjectionCount > 0))
    )
  }'
