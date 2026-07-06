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

  curl --retry 6 --retry-all-errors --retry-delay 5 --retry-max-time 180 --max-time 40 -fsS "${BACKEND_URL%/}${path}" \
    -H "Accept: application/json" \
    -o "$output_file"
}

post_required_json() {
  local path="$1"
  local payload_file="$2"
  local output_file="$3"

  curl --retry 6 --retry-all-errors --retry-delay 5 --retry-max-time 180 --max-time 40 -fsS "${BACKEND_URL%/}${path}" \
    -H "Accept: application/json" \
    -H "Content-Type: application/json" \
    -d @"$payload_file" \
    -o "$output_file"
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

request_paths_for_group() {
  local group="$1"
  local openapi_file="$2"

  jq -r --arg group "$group" '
    .paths
    | to_entries[]
    | select(.key | startswith("/api/" + $group + "/"))
    | .key as $path
    | .value
    | to_entries[]
    | select(.key == "post" or .key == "put" or .key == "patch")
    | select($path | test("/(option-sources|options|stats|actions|export|batch|by-ids|locate|schemas|capabilities)(/|$)") | not)
    | [$path, .key]
    | @tsv
  ' "$openapi_file"
}

assert_openapi_path() {
  local group="$1"
  local concrete_path="$2"
  local method="$3"
  local context="$4"
  local openapi_file
  local normalized_method
  local templated_path

  openapi_file="$(openapi_file_for_group "$group")"
  normalized_method="$(printf '%s' "$method" | tr '[:upper:]' '[:lower:]')"
  templated_path="$(printf '%s' "$concrete_path" | sed -E 's#/option-sources/[^/]+/#/option-sources/{sourceKey}/#')"

  if ! jq -e --arg path "$templated_path" --arg method "$normalized_method" '
    .paths[$path][$method] != null
  ' "$openapi_file" >/dev/null; then
    echo "Expected OpenAPI path '${method} ${templated_path}' for ${context}." >&2
    jq --arg path "$templated_path" '.paths[$path] // null' "$openapi_file" >&2
    return 1
  fi
}

group_for_api_path() {
  local api_path="$1"
  printf '%s' "$api_path" | awk -F'/' '{print $3}'
}

assert_filter_response() {
  local response_file="$1"
  local context="$2"

  if ! jq -e '
    type == "object"
    and (
      (.content? | type == "array")
      or (.data.content? | type == "array")
      or (.items? | type == "array")
    )
  ' "$response_file" >/dev/null; then
    echo "Option source filter response for ${context} did not expose a pageable option collection." >&2
    cat "$response_file" >&2
    return 1
  fi
}

assert_by_ids_response() {
  local response_file="$1"
  local context="$2"

  if ! jq -e '
    type == "array"
    or (.content? | type == "array")
    or (.data? | type == "array")
    or (.data.content? | type == "array")
  ' "$response_file" >/dev/null; then
    echo "Option source by-ids response for ${context} did not expose an option array." >&2
    cat "$response_file" >&2
    return 1
  fi
}

payload_file="$TMPDIR_RUN/empty-filter.json"
printf '{}\n' > "$payload_file"

schema_sources_file="$TMPDIR_RUN/schema-option-sources.jsonl"
: > "$schema_sources_file"

for group in "${groups[@]}"; do
  openapi_file="$(openapi_file_for_group "$group")"

  while IFS=$'\t' read -r operation_path operation_method; do
    encoded_path="$(urlencode_path "$operation_path")"
    schema_file="$TMPDIR_RUN/${group}.$(printf '%s' "$operation_path" | tr '/{}' '___').${operation_method}.request.json"

    if ! get_required_json "/schemas/filtered?path=${encoded_path}&operation=${operation_method}&schemaType=request&idField=id&readOnly=false" "$schema_file" 2>/dev/null; then
      continue
    fi

    jq -c --arg group "$group" --arg operationPath "$operation_path" --arg operation "$operation_method" '
      [
        paths(objects) as $p
        | getpath($p) as $node
        | select($node["x-ui"].optionSource? != null)
        | {
            group: $group,
            operationPath: $operationPath,
            operation: $operation,
            fieldPath: ($p | join(".")),
            fieldName: ($node["x-ui"].name // ($p[-1] | tostring)),
            controlType: ($node["x-ui"].controlType // ""),
            optionSource: $node["x-ui"].optionSource
          }
      ][]
    ' "$schema_file" >> "$schema_sources_file"
  done < <(request_paths_for_group "$group" "$openapi_file")
done

if [[ ! -s "$schema_sources_file" ]]; then
  echo "No x-ui.optionSource contracts were discovered in filtered schemas." >&2
  exit 1
fi

unique_sources_file="$TMPDIR_RUN/unique-option-sources.json"
jq -s '
  unique_by([
    .optionSource.filterEndpoint,
    (.optionSource.byIdsEndpoint // ""),
    (.optionSource.key // ""),
    (.optionSource.type // "")
  ])
' "$schema_sources_file" > "$unique_sources_file"

summary_file="$TMPDIR_RUN/option-source-contracts.jsonl"
: > "$summary_file"

while IFS=$'\t' read -r group source_key source_type resource_path filter_endpoint by_ids_endpoint capabilities_filter capabilities_by_ids; do
  context="${source_type}:${source_key}:${filter_endpoint}"
  endpoint_group="$(group_for_api_path "$filter_endpoint")"

  if [[ -z "$source_key" || -z "$source_type" || -z "$filter_endpoint" ]]; then
    echo "Option source contract is missing key, type or filterEndpoint." >&2
    jq --arg endpoint "$filter_endpoint" '.[] | select(.optionSource.filterEndpoint == $endpoint)' "$unique_sources_file" >&2
    exit 1
  fi

  assert_openapi_path "$endpoint_group" "$filter_endpoint" "POST" "$context filter"

  filter_response_file="$TMPDIR_RUN/$(printf '%s' "$filter_endpoint" | tr '/{}?' '_____').filter.json"
  post_required_json "${filter_endpoint}?page=0&size=3" "$payload_file" "$filter_response_file"
  assert_filter_response "$filter_response_file" "$context"

  if [[ "$source_type" == "RESOURCE_ENTITY" ]]; then
    if [[ -z "$resource_path" || -z "$by_ids_endpoint" ]]; then
      echo "RESOURCE_ENTITY option source ${context} must publish resourcePath and byIdsEndpoint." >&2
      exit 1
    fi

    assert_openapi_path "$endpoint_group" "$by_ids_endpoint" "GET" "$context by-ids"

    by_ids_response_file="$TMPDIR_RUN/$(printf '%s' "$by_ids_endpoint" | tr '/{}?' '_____').by-ids.json"
    get_required_json "${by_ids_endpoint}?ids=1" "$by_ids_response_file"
    assert_by_ids_response "$by_ids_response_file" "$context"
  elif [[ -n "$by_ids_endpoint" && "$capabilities_by_ids" == "true" ]]; then
    assert_openapi_path "$endpoint_group" "$by_ids_endpoint" "GET" "$context by-ids"
  fi

  jq -n \
    --arg group "$endpoint_group" \
    --arg key "$source_key" \
    --arg type "$source_type" \
    --arg resourcePath "$resource_path" \
    --arg filterEndpoint "$filter_endpoint" \
    --arg byIdsEndpoint "$by_ids_endpoint" \
    --argjson filterCapable "$capabilities_filter" \
    --argjson byIdsCapable "$capabilities_by_ids" \
    '{
      group: $group,
      key: $key,
      type: $type,
      resourcePath: $resourcePath,
      filterEndpoint: $filterEndpoint,
      byIdsEndpoint: $byIdsEndpoint,
      capabilities: {
        filter: $filterCapable,
        byIds: $byIdsCapable
      }
    }' >> "$summary_file"
done < <(jq -r '
  .[]
  | [
      .group,
      (.optionSource.key // ""),
      (.optionSource.type // ""),
      (.optionSource.resourcePath // ""),
      (.optionSource.filterEndpoint // .optionSource.endpoint // ""),
      (.optionSource.byIdsEndpoint // ""),
      ((.optionSource.capabilities.filter // false) | tostring),
      ((.optionSource.capabilities.byIds // false) | tostring)
    ]
  | @tsv
' "$unique_sources_file")

jq -n \
  --slurpfile schemaSources "$schema_sources_file" \
  --slurpfile uniqueSources "$summary_file" \
  '{
    status: "cockpit-option-source-contracts-ready",
    backendUrl: env.BACKEND_URL,
    totalSchemaReferences: ($schemaSources | length),
    totalOptionSources: ($uniqueSources | length),
    groups: (
      $uniqueSources
      | group_by(.group)
      | map({
          group: .[0].group,
          optionSourceCount: length,
          types: (map(.type) | unique),
          keys: (map(.key) | unique)
        })
    ),
    resourceEntitySources: (
      $uniqueSources
      | map(select(.type == "RESOURCE_ENTITY"))
    )
  }'
