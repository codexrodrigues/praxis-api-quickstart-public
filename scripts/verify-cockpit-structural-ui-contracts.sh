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
    | map(select(
        . != ""
        and . != "option-sources"
        and . != "options"
        and . != "stats"
        and . != "actions"
        and . != "export"
        and . != "batch"
        and . != "by-ids"
        and . != "locate"
        and . != "schemas"
        and . != "capabilities"
      ))
    | unique[]
  ' "$openapi_file"
}

has_openapi_method() {
  local openapi_file="$1"
  local path="$2"
  local method="$3"

  jq -e --arg path "$path" --arg method "$method" '
    .paths[$path][$method] != null
  ' "$openapi_file" >/dev/null
}

assert_filtered_schema_contract() {
  local schema_file="$1"
  local context="$2"

  if ! jq -e '
    type == "object"
    and (
      ((.properties // {}) | length) > 0
      or ((.fields // []) | length) > 0
      or ((.schema.properties // {}) | length) > 0
      or ((.data.properties // {}) | length) > 0
    )
  ' "$schema_file" >/dev/null; then
    echo "Filtered schema for ${context} did not expose materializable fields." >&2
    cat "$schema_file" >&2
    return 1
  fi
}

validate_filtered_schema() {
  local group="$1"
  local resource_key="$2"
  local operation_path="$3"
  local operation="$4"
  local schema_type="$5"
  local read_only="$6"
  local category="$7"
  local encoded_path
  local schema_file
  local property_count

  encoded_path="$(urlencode_path "$operation_path")"
  schema_file="$TMPDIR_RUN/$(printf '%s.%s.%s.%s' "$resource_key" "$category" "$operation" "$schema_type" | tr '/{}' '___').json"

  get_required_json "/schemas/filtered?path=${encoded_path}&operation=${operation}&schemaType=${schema_type}&idField=id&readOnly=${read_only}" "$schema_file"
  assert_filtered_schema_contract "$schema_file" "${resource_key} ${category}"

  property_count="$(jq '
    ((.properties // {}) | length)
    + ((.fields // []) | length)
    + ((.schema.properties // {}) | length)
    + ((.data.properties // {}) | length)
  ' "$schema_file")"

  jq -n \
    --arg group "$group" \
    --arg resourceKey "$resource_key" \
    --arg category "$category" \
    --arg method "$operation" \
    --arg path "$operation_path" \
    --arg schemaType "$schema_type" \
    --argjson propertyCount "$property_count" \
    '{
      group: $group,
      resourceKey: $resourceKey,
      category: $category,
      method: $method,
      path: $path,
      schemaType: $schemaType,
      propertyCount: $propertyCount
    }' >> "$schema_summary_file"
}

resources_file="$TMPDIR_RUN/resources.jsonl"
schema_summary_file="$TMPDIR_RUN/structural-ui-contracts.jsonl"
: > "$resources_file"
: > "$schema_summary_file"

for group in "${groups[@]}"; do
  openapi_file="$(openapi_file_for_group "$group")"

  while IFS= read -r slug; do
    resource_key="${group}.${slug}"
    resource_path="/api/${group}/${slug}"
    detail_path="${resource_path}/{id}"
    all_path="${resource_path}/all"
    filter_path="${resource_path}/filter"

    jq -n \
      --arg group "$group" \
      --arg resourceKey "$resource_key" \
      --arg path "$resource_path" \
      '{group: $group, resourceKey: $resourceKey, path: $path}' >> "$resources_file"

    if has_openapi_method "$openapi_file" "$filter_path" "post"; then
      validate_filtered_schema "$group" "$resource_key" "$filter_path" "post" "request" "false" "filter-request"
      validate_filtered_schema "$group" "$resource_key" "$filter_path" "post" "response" "true" "filter-table-response"
    fi

    if has_openapi_method "$openapi_file" "$all_path" "get"; then
      validate_filtered_schema "$group" "$resource_key" "$all_path" "get" "response" "true" "table-all-response"
    fi

    if has_openapi_method "$openapi_file" "$detail_path" "get"; then
      validate_filtered_schema "$group" "$resource_key" "$detail_path" "get" "response" "true" "detail-response"
    fi

    if has_openapi_method "$openapi_file" "$resource_path" "post"; then
      validate_filtered_schema "$group" "$resource_key" "$resource_path" "post" "request" "false" "create-form-request"
    fi

    if has_openapi_method "$openapi_file" "$detail_path" "put"; then
      validate_filtered_schema "$group" "$resource_key" "$detail_path" "put" "request" "false" "edit-form-request"
    fi
  done < <(resource_slugs_for_group "$group" "$openapi_file")
done

if [[ ! -s "$resources_file" ]]; then
  echo "No cockpit resources were discovered from OpenAPI groups." >&2
  exit 1
fi

if [[ ! -s "$schema_summary_file" ]]; then
  echo "No structural UI schemas were validated." >&2
  exit 1
fi

missing_read_file="$TMPDIR_RUN/missing-read.json"
jq -s --slurpfile schemas "$schema_summary_file" '
  [
    .[]
    | . as $resource
    | select(
        ($schemas
        | map(select(
            .resourceKey == $resource.resourceKey
            and (
              .category == "filter-table-response"
              or .category == "table-all-response"
              or .category == "detail-response"
            )
          ))
        | length) == 0
      )
  ]
' "$resources_file" > "$missing_read_file"

if jq -e 'length > 0' "$missing_read_file" >/dev/null; then
  echo "Some cockpit resources do not expose any materializable read schema." >&2
  cat "$missing_read_file" >&2
  exit 1
fi

jq -s --slurpfile resources "$resources_file" '{
  status: "cockpit-structural-ui-contracts-ready",
  backendUrl: env.BACKEND_URL,
  totalResources: ($resources | length),
  totalSchemas: length,
  categories: (
    group_by(.category)
    | map({
        category: .[0].category,
        schemaCount: length,
        resources: (map(.resourceKey) | unique | length),
        fields: (map(.propertyCount) | add)
      })
  ),
  groups: (
    group_by(.group)
    | map({
        group: .[0].group,
        schemaCount: length,
        resources: (map(.resourceKey) | unique | length)
      })
  )
}' "$schema_summary_file"
