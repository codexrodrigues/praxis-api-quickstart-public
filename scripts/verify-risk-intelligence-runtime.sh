#!/usr/bin/env bash
set -euo pipefail

BACKEND_URL="${BACKEND_URL:-https://praxis-api-quickstart.onrender.com}"
export BACKEND_URL

TMPDIR_RUN="$(mktemp -d)"
cleanup() {
  rm -rf "$TMPDIR_RUN"
}
trap cleanup EXIT

get_json() {
  local path="$1"
  local output_file="$2"

  curl -fsS "${BACKEND_URL%/}${path}" \
    -H "Accept: application/json" \
    -o "$output_file"
}

post_json() {
  local path="$1"
  local payload="$2"
  local output_file="$3"

  curl -fsS "${BACKEND_URL%/}${path}" \
    -H "Content-Type: application/json" \
    --data-binary "$payload" \
    -o "$output_file"
}

group_by_count() {
  local resource_path="$1"
  local field="$2"
  local output_file="$3"

  jq -n --arg field "$field" '{
    filter: {},
    field: $field,
    metrics: [{operation: "COUNT"}],
    limit: 20
  }' | post_json "${resource_path}/stats/group-by" @- "$output_file"
}

time_series_count() {
  local resource_path="$1"
  local field="$2"
  local output_file="$3"

  jq -n --arg field "$field" '{
    filter: {},
    field: $field,
    granularity: "MONTH",
    metrics: [{operation: "COUNT"}],
    fillGaps: false
  }' | post_json "${resource_path}/stats/timeseries" @- "$output_file"
}

assert_surface_exists() {
  local output_file="$1"
  local surface_id="$2"

  if ! jq -e --arg surface_id "$surface_id" '.surfaces[] | select(.id == $surface_id)' "$output_file" >/dev/null; then
    echo "Expected surface '${surface_id}' to be published." >&2
    jq '{resourceKey, surfaces: [.surfaces[] | {id, kind, title}]}' "$output_file" >&2
    return 1
  fi
}

assert_action_exists() {
  local output_file="$1"
  local action_id="$2"

  if ! jq -e --arg action_id "$action_id" '.actions[] | select(.id == $action_id)' "$output_file" >/dev/null; then
    echo "Expected action '${action_id}' to be published." >&2
    jq '{resourceKey, actions: [.actions[] | {id, scope, title, allowedStates}]}' "$output_file" >&2
    return 1
  fi
}

assert_stats_field_exists() {
  local output_file="$1"
  local field="$2"

  if ! jq -e --arg field "$field" '.stats.fields[] | select(.field == $field)' "$output_file" >/dev/null; then
    echo "Expected stats field '${field}' to be published." >&2
    jq '{resourceKey, statsFields: [.stats.fields[] | .field]}' "$output_file" >&2
    return 1
  fi
}

assert_bucket_count_at_least() {
  local output_file="$1"
  local key="$2"
  local minimum="$3"
  local actual

  actual="$(jq --arg key "$key" '[.data.buckets[] | select((.key | tostring) == $key) | .count] | add // 0' "$output_file")"
  if [[ "$actual" -lt "$minimum" ]]; then
    echo "Expected bucket '${key}' to have count >= ${minimum}, got ${actual}." >&2
    jq '{field: .data.field, buckets: [.data.buckets[] | {key, count}]}' "$output_file" >&2
    return 1
  fi
}

assert_bucket_diversity_at_least() {
  local output_file="$1"
  local minimum="$2"
  local actual

  actual="$(jq '.data.buckets | length' "$output_file")"
  if [[ "$actual" -lt "$minimum" ]]; then
    echo "Expected at least ${minimum} buckets, got ${actual}." >&2
    jq '{field: .data.field, buckets: [.data.buckets[] | {key, count}]}' "$output_file" >&2
    return 1
  fi
}

assert_time_series_count_at_least() {
  local output_file="$1"
  local minimum="$2"
  local actual

  actual="$(jq '[.data.points[] | .count] | add // 0' "$output_file")"
  if [[ "$actual" -lt "$minimum" ]]; then
    echo "Expected time-series count >= ${minimum}, got ${actual}." >&2
    jq '{field: .data.field, points: [.data.points[] | {label, count}]}' "$output_file" >&2
    return 1
  fi
}

incident_surfaces="$TMPDIR_RUN/incident-surfaces.json"
get_json "/schemas/surfaces?resource=operations.incidentes" "$incident_surfaces"
assert_surface_exists "$incident_surfaces" "incident-investigation-board"

threat_surfaces="$TMPDIR_RUN/threat-surfaces.json"
get_json "/schemas/surfaces?resource=risk-intelligence.ameacas" "$threat_surfaces"
assert_surface_exists "$threat_surfaces" "threat-monitoring-board"

threat_actions="$TMPDIR_RUN/threat-actions.json"
get_json "/schemas/actions?resource=risk-intelligence.ameacas" "$threat_actions"
assert_action_exists "$threat_actions" "mark-under-observation"
assert_action_exists "$threat_actions" "mark-captured"

incident_capabilities="$TMPDIR_RUN/incident-capabilities.json"
get_json "/api/operations/incidentes/capabilities" "$incident_capabilities"
for field in severidade local missaoId ocorridoEm danosCivis feridos mortos; do
  assert_stats_field_exists "$incident_capabilities" "$field"
done

threat_capabilities="$TMPDIR_RUN/threat-capabilities.json"
get_json "/api/risk-intelligence/ameacas/capabilities" "$threat_capabilities"
for field in classe status planeta nivel recompensa; do
  assert_stats_field_exists "$threat_capabilities" "$field"
done

incident_severity="$TMPDIR_RUN/incident-severity.json"
group_by_count "/api/operations/incidentes" "severidade" "$incident_severity"
assert_bucket_diversity_at_least "$incident_severity" 4
assert_bucket_count_at_least "$incident_severity" "CRITICA" 1
assert_bucket_count_at_least "$incident_severity" "ALTA" 1

incident_timeline="$TMPDIR_RUN/incident-timeline.json"
time_series_count "/api/operations/incidentes" "ocorridoEm" "$incident_timeline"
assert_time_series_count_at_least "$incident_timeline" 12

threat_status="$TMPDIR_RUN/threat-status.json"
group_by_count "/api/risk-intelligence/ameacas" "status" "$threat_status"
assert_bucket_diversity_at_least "$threat_status" 4
assert_bucket_count_at_least "$threat_status" "LIVRE" 1
assert_bucket_count_at_least "$threat_status" "CAPTURADO" 1

indicator_severity="$TMPDIR_RUN/indicator-severity.json"
group_by_count "/api/risk-intelligence/vw-indicadores-incidentes" "severidade" "$indicator_severity"
assert_bucket_diversity_at_least "$indicator_severity" 4

jq -n \
  --slurpfile incidentSeverity "$incident_severity" \
  --slurpfile incidentTimeline "$incident_timeline" \
  --slurpfile threatStatus "$threat_status" \
  --slurpfile indicatorSeverity "$indicator_severity" \
  '{
    status: "risk-intelligence-ready",
    backendUrl: env.BACKEND_URL,
    incidentSeverity: $incidentSeverity[0].data.buckets | map({key, count}),
    incidentTimelinePoints: ($incidentTimeline[0].data.points | length),
    threatStatus: $threatStatus[0].data.buckets | map({key, count}),
    indicatorSeverity: $indicatorSeverity[0].data.buckets | map({key, count})
  }'
