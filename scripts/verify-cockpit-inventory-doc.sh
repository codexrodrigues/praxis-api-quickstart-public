#!/usr/bin/env bash
set -euo pipefail

BACKEND_URL="${BACKEND_URL:-https://praxis-api-quickstart.onrender.com}"
DOC_PATH="${DOC_PATH:-docs/COCKPIT-QUICKSTART-REFERENCE.md}"

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
    printf '{}\n' > "$output_file"
    return 0
  fi

  if [[ "$status" -lt 200 || "$status" -ge 300 ]]; then
    echo "Unexpected HTTP ${status} for ${path}." >&2
    cat "$output_file" >&2 || true
    return 1
  fi
}

doc_inventory_for_group() {
  local group="$1"

  awk -F'|' -v group="\\\`${group}\\\`" '
    index($0, group) {
      resources=$4
      surfaces=$5
      actions=$6
      gsub(/[[:space:]]/, "", resources)
      gsub(/[[:space:]]/, "", surfaces)
      gsub(/[[:space:]]/, "", actions)
      print resources "\t" surfaces "\t" actions
    }
  ' "$DOC_PATH"
}

count_group_resources() {
  local group="$1"
  local openapi_file="$TMPDIR_RUN/${group}-openapi.json"

  get_required_json "/v3/api-docs/${group}" "$openapi_file"
  jq --arg group "$group" '
    .paths
    | keys
    | map(select(startswith("/api/" + $group + "/")))
    | map(split("/")[3])
    | unique
    | length
  ' "$openapi_file"
}

resource_slugs_for_group() {
  local group="$1"
  local openapi_file="$TMPDIR_RUN/${group}-openapi.json"

  if [[ ! -f "$openapi_file" ]]; then
    get_required_json "/v3/api-docs/${group}" "$openapi_file"
  fi

  jq -r --arg group "$group" '
    .paths
    | keys
    | map(select(startswith("/api/" + $group + "/")))
    | map(split("/")[3])
    | unique[]
  ' "$openapi_file"
}

count_semantic_surfaces_for_resource() {
  local resource_key="$1"
  local output_file="$TMPDIR_RUN/${resource_key}.surfaces.json"

  get_optional_json "/schemas/surfaces?resource=${resource_key}" "$output_file"
  jq '
    [
      (.surfaces // [])[].id
      | select(. != "create" and . != "list" and . != "detail" and . != "edit" and . != "delete")
    ]
    | length
  ' "$output_file"
}

count_actions_for_resource() {
  local resource_key="$1"
  local output_file="$TMPDIR_RUN/${resource_key}.actions.json"

  get_optional_json "/schemas/actions?resource=${resource_key}" "$output_file"
  jq '[(.actions // [])[].id] | length' "$output_file"
}

failures=0
summary_file="$TMPDIR_RUN/summary.jsonl"
: > "$summary_file"

for group in "${groups[@]}"; do
  doc_values="$(doc_inventory_for_group "$group")"
  if [[ -z "$doc_values" ]]; then
    echo "No inventory row found in ${DOC_PATH} for OpenAPI group ${group}." >&2
    failures=$((failures + 1))
    continue
  fi

  IFS=$'\t' read -r expected_resources expected_surfaces expected_actions <<< "$doc_values"

  actual_resources="$(count_group_resources "$group")"
  actual_surfaces=0
  actual_actions=0

  while IFS= read -r slug; do
    resource_key="${group}.${slug}"
    resource_surfaces="$(count_semantic_surfaces_for_resource "$resource_key")"
    resource_actions="$(count_actions_for_resource "$resource_key")"
    actual_surfaces=$((actual_surfaces + resource_surfaces))
    actual_actions=$((actual_actions + resource_actions))
  done < <(resource_slugs_for_group "$group")

  jq -n \
    --arg group "$group" \
    --argjson expectedResources "$expected_resources" \
    --argjson actualResources "$actual_resources" \
    --argjson expectedSurfaces "$expected_surfaces" \
    --argjson actualSurfaces "$actual_surfaces" \
    --argjson expectedActions "$expected_actions" \
    --argjson actualActions "$actual_actions" \
    '{
      group: $group,
      resources: {expected: $expectedResources, actual: $actualResources},
      semanticSurfaces: {expected: $expectedSurfaces, actual: $actualSurfaces},
      workflowActions: {expected: $expectedActions, actual: $actualActions}
    }' >> "$summary_file"

  if [[ "$expected_resources" != "$actual_resources" || "$expected_surfaces" != "$actual_surfaces" || "$expected_actions" != "$actual_actions" ]]; then
    echo "Cockpit inventory drift detected for ${group}: expected ${expected_resources}/${expected_surfaces}/${expected_actions}, got ${actual_resources}/${actual_surfaces}/${actual_actions}." >&2
    failures=$((failures + 1))
  fi
done

jq -s '{
  status: (if ([.[] | select(.resources.expected != .resources.actual or .semanticSurfaces.expected != .semanticSurfaces.actual or .workflowActions.expected != .workflowActions.actual)] | length) == 0 then "cockpit-inventory-doc-ready" else "cockpit-inventory-doc-drift" end),
  backendUrl: $backendUrl,
  docPath: $docPath,
  groups: .
}' \
  --arg backendUrl "$BACKEND_URL" \
  --arg docPath "$DOC_PATH" \
  "$summary_file"

if [[ "$failures" -gt 0 ]]; then
  exit 1
fi
