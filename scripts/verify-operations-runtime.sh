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
  local resource_path="$1"
  local output_file="$2"

  curl -fsS "${BACKEND_URL%/}${resource_path}" \
    -H "Accept: application/json" \
    -o "$output_file"
}

post_json() {
  local resource_path="$1"
  local payload="$2"
  local output_file="$3"

  curl -fsS "${BACKEND_URL%/}${resource_path}" \
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
    jq '{resourceKey, actions: [.actions[] | {id, scope, title, path}]}' "$output_file" >&2
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

assert_time_series_points_at_least() {
  local output_file="$1"
  local minimum="$2"
  local actual

  actual="$(jq '.data.points | length' "$output_file")"
  if [[ "$actual" -lt "$minimum" ]]; then
    echo "Expected at least ${minimum} time-series points, got ${actual}." >&2
    jq '{field: .data.field, points: [.data.points[] | {label, count}]}' "$output_file" >&2
    return 1
  fi
}

mission_surfaces="$TMPDIR_RUN/mission-surfaces.json"
get_json "/schemas/surfaces?resource=operations.missoes" "$mission_surfaces"
assert_surface_exists "$mission_surfaces" "reschedule"
assert_surface_exists "$mission_surfaces" "team-plan"
assert_surface_exists "$mission_surfaces" "summary"
assert_surface_exists "$mission_surfaces" "team"
assert_surface_exists "$mission_surfaces" "timeline"

mission_actions="$TMPDIR_RUN/mission-actions.json"
get_json "/schemas/actions?resource=operations.missoes" "$mission_actions"
assert_action_exists "$mission_actions" "start"
assert_action_exists "$mission_actions" "pause"
assert_action_exists "$mission_actions" "resume"
assert_action_exists "$mission_actions" "complete"
assert_action_exists "$mission_actions" "fail"

base_access_surfaces="$TMPDIR_RUN/base-access-surfaces.json"
get_json "/schemas/surfaces?resource=operations.base-acessos" "$base_access_surfaces"
assert_surface_exists "$base_access_surfaces" "review-access"

base_access_actions="$TMPDIR_RUN/base-access-actions.json"
get_json "/schemas/actions?resource=operations.base-acessos" "$base_access_actions"
assert_action_exists "$base_access_actions" "activate"
assert_action_exists "$base_access_actions" "deactivate"

agreement_surfaces="$TMPDIR_RUN/agreement-surfaces.json"
get_json "/schemas/surfaces?resource=operations.acordos-regulatorios" "$agreement_surfaces"
assert_surface_exists "$agreement_surfaces" "review"

agreement_actions="$TMPDIR_RUN/agreement-actions.json"
get_json "/schemas/actions?resource=operations.acordos-regulatorios" "$agreement_actions"
assert_action_exists "$agreement_actions" "suspend"
assert_action_exists "$agreement_actions" "reinstate"
assert_action_exists "$agreement_actions" "revoke"

mission_status="$TMPDIR_RUN/mission-status.json"
group_by_count "/api/operations/vw-resumo-missoes" "status" "$mission_status"
assert_bucket_diversity_at_least "$mission_status" 5
assert_bucket_count_at_least "$mission_status" "PLANEJADA" 1
assert_bucket_count_at_least "$mission_status" "EM_ANDAMENTO" 1
assert_bucket_count_at_least "$mission_status" "CONCLUIDA" 1
assert_bucket_count_at_least "$mission_status" "FALHOU" 1

mission_priority="$TMPDIR_RUN/mission-priority.json"
group_by_count "/api/operations/vw-resumo-missoes" "prioridade" "$mission_priority"
assert_bucket_diversity_at_least "$mission_priority" 3
assert_bucket_count_at_least "$mission_priority" "CRITICA" 1
assert_bucket_count_at_least "$mission_priority" "ALTA" 1
assert_bucket_count_at_least "$mission_priority" "MEDIA" 1

mission_timeline="$TMPDIR_RUN/mission-timeline.json"
time_series_count "/api/operations/vw-resumo-missoes" "primeiraAcao" "$mission_timeline"
assert_time_series_points_at_least "$mission_timeline" 6

event_type="$TMPDIR_RUN/event-type.json"
group_by_count "/api/operations/missao-eventos" "tipo" "$event_type"
assert_bucket_diversity_at_least "$event_type" 5
assert_bucket_count_at_least "$event_type" "CONTATO" 1
assert_bucket_count_at_least "$event_type" "INTEL" 1
assert_bucket_count_at_least "$event_type" "COMBATE" 1
assert_bucket_count_at_least "$event_type" "SOS" 1

event_timeline="$TMPDIR_RUN/event-timeline.json"
time_series_count "/api/operations/missao-eventos" "ocorridoEm" "$event_timeline"
assert_time_series_points_at_least "$event_timeline" 6

participant_role="$TMPDIR_RUN/participant-role.json"
group_by_count "/api/operations/missao-participantes" "papel" "$participant_role"
assert_bucket_diversity_at_least "$participant_role" 4
assert_bucket_count_at_least "$participant_role" "LIDER" 1
assert_bucket_count_at_least "$participant_role" "COMBATE" 1
assert_bucket_count_at_least "$participant_role" "SUPORTE" 1

jq -n \
  --slurpfile missionStatus "$mission_status" \
  --slurpfile missionPriority "$mission_priority" \
  --slurpfile missionTimeline "$mission_timeline" \
  --slurpfile eventType "$event_type" \
  --slurpfile eventTimeline "$event_timeline" \
  --slurpfile participantRole "$participant_role" \
  '{
    status: "operations-runtime-ready",
    backendUrl: env.BACKEND_URL,
    missionStatus: $missionStatus[0].data.buckets | map({key, count}),
    missionPriority: $missionPriority[0].data.buckets | map({key, count}),
    missionTimelinePoints: ($missionTimeline[0].data.points | length),
    eventTypes: $eventType[0].data.buckets | map({key, count}),
    eventTimelinePoints: ($eventTimeline[0].data.points | length),
    participantRoles: $participantRole[0].data.buckets | map({key, count})
  }'
