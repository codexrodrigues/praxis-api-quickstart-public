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

openapi_file_for_group() {
  local group="$1"
  local openapi_file="$TMPDIR_RUN/${group}-openapi.json"

  if [[ ! -f "$openapi_file" ]]; then
    get_required_json "/v3/api-docs/${group}" "$openapi_file"
  fi

  printf '%s\n' "$openapi_file"
}

assert_openapi_path() {
  local openapi_file="$1"
  local path="$2"
  local method="$3"
  local context="$4"
  local normalized_method
  normalized_method="$(printf '%s' "$method" | tr '[:upper:]' '[:lower:]')"

  if ! jq -e --arg path "$path" --arg method "$normalized_method" '
    .paths[$path][$method] != null
  ' "$openapi_file" >/dev/null; then
    echo "Expected OpenAPI path '${method} ${path}' for ${context}." >&2
    jq --arg path "$path" '.paths[$path] // null' "$openapi_file" >&2
    return 1
  fi
}

assert_schema_has_property() {
  local schema_file="$1"
  local property_name="$2"
  local context="$3"

  if ! jq -e --arg property "$property_name" '
    (.properties // {})[$property] != null
  ' "$schema_file" >/dev/null; then
    echo "Expected schema property '${property_name}' for ${context}." >&2
    jq '{description, properties: ((.properties // {}) | keys)}' "$schema_file" >&2
    return 1
  fi
}

assert_child_operation_path() {
  local openapi_file="$1"
  local child_path="$2"
  local operation="$3"
  local context="$4"

  case "$operation" in
    FILTER)
      assert_openapi_path "$openapi_file" "${child_path}/filter" "POST" "${context} child FILTER"
      ;;
    LIST)
      assert_openapi_path "$openapi_file" "${child_path}/all" "GET" "${context} child LIST"
      ;;
    CREATE)
      assert_openapi_path "$openapi_file" "$child_path" "POST" "${context} child CREATE"
      ;;
    UPDATE)
      assert_openapi_path "$openapi_file" "${child_path}/{id}" "PUT" "${context} child UPDATE"
      ;;
    DELETE)
      assert_openapi_path "$openapi_file" "${child_path}/{id}" "DELETE" "${context} child DELETE"
      ;;
    "")
      ;;
    *)
      echo "Unsupported child operation '${operation}' for ${context}." >&2
      return 1
      ;;
  esac
}

summary_file="$TMPDIR_RUN/related-resource-contracts.jsonl"
: > "$summary_file"

for group in "${groups[@]}"; do
  openapi_file="$(openapi_file_for_group "$group")"

  while IFS= read -r slug; do
    resource_key="${group}.${slug}"
    surfaces_file="$TMPDIR_RUN/${resource_key}.surfaces.json"
    get_optional_json "/schemas/surfaces?resource=${resource_key}" "$surfaces_file"

    related_count="$(jq '[ (.surfaces // [])[] | select(.relatedResource != null) ] | length' "$surfaces_file")"
    if [[ "$related_count" -eq 0 ]]; then
      continue
    fi

    while IFS=$'\t' read -r surface_id surface_path method schema_url parent_resource_key parent_id_path_variable child_resource_key child_resource_path child_parent_field selectable selection_key_field child_operations; do
      context="${resource_key}.${surface_id}"

      if [[ -z "$surface_id" || -z "$surface_path" || -z "$method" || -z "$schema_url" || -z "$parent_resource_key" || -z "$parent_id_path_variable" || -z "$child_resource_key" || -z "$child_resource_path" || -z "$child_parent_field" ]]; then
        echo "Related resource contract for ${context} is incomplete." >&2
        jq '.surfaces[] | select(.relatedResource != null) | {id, path, method, schemaUrl, relatedResource}' "$surfaces_file" >&2
        exit 1
      fi

      if [[ "$parent_resource_key" != "$resource_key" ]]; then
        echo "Related resource parent '${parent_resource_key}' does not match surface resource '${resource_key}' for ${context}." >&2
        exit 1
      fi

      if [[ "$surface_path" != *"{$parent_id_path_variable}"* ]]; then
        echo "Surface path '${surface_path}' does not contain parent path variable '{${parent_id_path_variable}}' for ${context}." >&2
        exit 1
      fi

      assert_openapi_path "$openapi_file" "$surface_path" "$method" "${context} related surface"

      schema_file="$TMPDIR_RUN/${resource_key}.${surface_id}.schema.json"
      get_required_json "$schema_url" "$schema_file"
      assert_schema_has_property "$schema_file" "$child_parent_field" "${context} related surface response"

      if [[ "$selectable" == "true" ]]; then
        if [[ -z "$selection_key_field" ]]; then
          echo "Selectable related resource ${context} must publish selectionKeyField." >&2
          exit 1
        fi
        assert_schema_has_property "$schema_file" "$selection_key_field" "${context} selectable related surface response"
      fi

      child_group="${child_resource_key%%.*}"
      child_openapi_file="$(openapi_file_for_group "$child_group")"
      if [[ -n "$child_operations" ]]; then
        IFS=',' read -r -a operations <<< "$child_operations"
        for operation in "${operations[@]}"; do
          assert_child_operation_path "$child_openapi_file" "$child_resource_path" "$operation" "$context"
        done
      fi

      jq -n \
        --arg group "$group" \
        --arg resourceKey "$resource_key" \
        --arg surfaceId "$surface_id" \
        --arg childResourceKey "$child_resource_key" \
        --arg childParentField "$child_parent_field" \
        --arg selectionKeyField "$selection_key_field" \
        --arg childOperations "$child_operations" \
        '{
          group: $group,
          resourceKey: $resourceKey,
          surfaceId: $surfaceId,
          childResourceKey: $childResourceKey,
          childParentField: $childParentField,
          selectionKeyField: $selectionKeyField,
          childOperations: (if $childOperations == "" then [] else ($childOperations | split(",")) end)
        }' >> "$summary_file"
    done < <(jq -r '
      (.surfaces // [])[]
      | select(.relatedResource != null)
      | [
          .id,
          .path,
          .method,
          .schemaUrl,
          .relatedResource.parentResourceKey,
          .relatedResource.parentIdPathVariable,
          .relatedResource.childResourceKey,
          .relatedResource.childResourcePath,
          .relatedResource.childParentField,
          (.relatedResource.selectable | tostring),
          (.relatedResource.selectionKeyField // ""),
          ((.relatedResource.childOperations // []) | join(","))
        ]
      | @tsv
    ' "$surfaces_file")
  done < <(resource_slugs_for_group "$group" "$openapi_file")
done

jq -s '{
  status: "cockpit-related-resource-contracts-ready",
  backendUrl: env.BACKEND_URL,
  totalRelatedSurfaces: length,
  groups: (
    group_by(.group)
    | map({
        group: .[0].group,
        relatedSurfaceCount: length,
        resources: (map(.resourceKey) | unique),
        childResources: (map(.childResourceKey) | unique)
      })
  ),
  relatedSurfaces: .
}' "$summary_file"
