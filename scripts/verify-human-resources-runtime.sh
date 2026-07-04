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
  local granularity="$3"
  local output_file="$4"

  jq -n --arg field "$field" --arg granularity "$granularity" '{
    filter: {},
    field: $field,
    granularity: $granularity,
    metrics: [{operation: "COUNT"}],
    fillGaps: false
  }' | post_json "${resource_path}/stats/timeseries" @- "$output_file"
}

histogram_count() {
  local resource_path="$1"
  local field="$2"
  local bucket_size="$3"
  local output_file="$4"

  jq -n --arg field "$field" --argjson bucket_size "$bucket_size" '{
    filter: {},
    field: $field,
    mode: "HISTOGRAM",
    metric: {operation: "COUNT"},
    bucketSize: $bucket_size,
    orderBy: "KEY_ASC"
  }' | post_json "${resource_path}/stats/distribution" @- "$output_file"
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

assert_openapi_action_contract() {
  local output_file="$1"
  local path="$2"
  local request_schema="$3"
  local response_schema="$4"

  if ! jq -e \
    --arg path "$path" \
    --arg request_schema "#/components/schemas/${request_schema}" \
    --arg response_schema "#/components/schemas/${response_schema}" \
    '
      .paths[$path].post.requestBody.content["application/json"].schema["$ref"] == $request_schema
      and .paths[$path].post.responses["200"].content["application/json"].schema["$ref"] == $response_schema
    ' "$output_file" >/dev/null; then
    echo "Expected OpenAPI action contract for '${path}' to expose request '${request_schema}' and response '${response_schema}'." >&2
    jq --arg path "$path" '.paths[$path].post | {summary, requestBody, responses}' "$output_file" >&2
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

assert_histogram_diversity_at_least() {
  local output_file="$1"
  local minimum="$2"
  local actual

  actual="$(jq '.data.buckets | length' "$output_file")"
  if [[ "$actual" -lt "$minimum" ]]; then
    echo "Expected at least ${minimum} histogram buckets, got ${actual}." >&2
    jq '{field: .data.field, mode: .data.mode, buckets: [.data.buckets[] | {from, to, count}]}' "$output_file" >&2
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

employee_surfaces="$TMPDIR_RUN/employee-surfaces.json"
get_json "/schemas/surfaces?resource=human-resources.funcionarios" "$employee_surfaces"
assert_surface_exists "$employee_surfaces" "profile"
assert_surface_exists "$employee_surfaces" "hero-profile"
assert_surface_exists "$employee_surfaces" "payroll-history"
assert_surface_exists "$employee_surfaces" "mission-participations"

payroll_surfaces="$TMPDIR_RUN/payroll-surfaces.json"
get_json "/schemas/surfaces?resource=human-resources.folhas-pagamento" "$payroll_surfaces"
assert_surface_exists "$payroll_surfaces" "payment-schedule"

payroll_actions="$TMPDIR_RUN/payroll-actions.json"
get_json "/schemas/actions?resource=human-resources.folhas-pagamento" "$payroll_actions"
assert_action_exists "$payroll_actions" "approve-events"
assert_action_exists "$payroll_actions" "mark-paid"

payroll_event_actions="$TMPDIR_RUN/payroll-event-actions.json"
get_json "/schemas/actions?resource=human-resources.eventos-folha" "$payroll_event_actions"
assert_action_exists "$payroll_event_actions" "bulk-approve"

absence_surfaces="$TMPDIR_RUN/absence-surfaces.json"
get_json "/schemas/surfaces?resource=human-resources.ferias-afastamentos" "$absence_surfaces"
assert_surface_exists "$absence_surfaces" "absence-calendar-board"

absence_actions="$TMPDIR_RUN/absence-actions.json"
get_json "/schemas/actions?resource=human-resources.ferias-afastamentos" "$absence_actions"
assert_action_exists "$absence_actions" "plan-coverage"

hr_openapi="$TMPDIR_RUN/human-resources-openapi.json"
get_json "/v3/api-docs/human-resources" "$hr_openapi"
assert_openapi_action_contract \
  "$hr_openapi" \
  "/api/human-resources/ferias-afastamentos/{id}/actions/plan-coverage" \
  "AbsenceCoverageWorkflowRequestDTO" \
  "RestApiResponseAbsenceCoverageWorkflowResultDTO"

reputation_ranking_surfaces="$TMPDIR_RUN/reputation-ranking-surfaces.json"
get_json "/schemas/surfaces?resource=human-resources.vw-ranking-reputacao" "$reputation_ranking_surfaces"
assert_surface_exists "$reputation_ranking_surfaces" "reputation-ranking-board"

employee_active="$TMPDIR_RUN/employee-active.json"
group_by_count "/api/human-resources/funcionarios" "ativo" "$employee_active"
assert_bucket_count_at_least "$employee_active" "true" 1
assert_bucket_count_at_least "$employee_active" "false" 1

employee_roles="$TMPDIR_RUN/employee-roles.json"
group_by_count "/api/human-resources/funcionarios" "cargoNome" "$employee_roles"
assert_bucket_diversity_at_least "$employee_roles" 8

employee_salary="$TMPDIR_RUN/employee-salary.json"
histogram_count "/api/human-resources/funcionarios" "salario" 5000 "$employee_salary"
assert_histogram_diversity_at_least "$employee_salary" 8

payroll_departments="$TMPDIR_RUN/payroll-departments.json"
group_by_count "/api/human-resources/vw-analytics-folha-pagamento" "departamento" "$payroll_departments"
assert_bucket_diversity_at_least "$payroll_departments" 10

payroll_profiles="$TMPDIR_RUN/payroll-profiles.json"
group_by_count "/api/human-resources/vw-analytics-folha-pagamento" "payrollProfile" "$payroll_profiles"
assert_bucket_diversity_at_least "$payroll_profiles" 5
assert_bucket_count_at_least "$payroll_profiles" "RND" 1
assert_bucket_count_at_least "$payroll_profiles" "SECURITY" 1

payroll_timeline="$TMPDIR_RUN/payroll-timeline.json"
time_series_count "/api/human-resources/vw-analytics-folha-pagamento" "competencia" "MONTH" "$payroll_timeline"
assert_time_series_points_at_least "$payroll_timeline" 24

payroll_net_salary="$TMPDIR_RUN/payroll-net-salary.json"
histogram_count "/api/human-resources/vw-analytics-folha-pagamento" "salarioLiquido" 5000 "$payroll_net_salary"
assert_histogram_diversity_at_least "$payroll_net_salary" 10

hero_universe="$TMPDIR_RUN/hero-universe.json"
group_by_count "/api/human-resources/vw-perfil-heroi" "universo" "$hero_universe"
assert_bucket_diversity_at_least "$hero_universe" 4

absence_type="$TMPDIR_RUN/absence-type.json"
group_by_count "/api/human-resources/ferias-afastamentos" "tipo" "$absence_type"
assert_bucket_diversity_at_least "$absence_type" 2
assert_bucket_count_at_least "$absence_type" "FERIAS" 1
assert_bucket_count_at_least "$absence_type" "AFASTAMENTO" 1

absence_timeline="$TMPDIR_RUN/absence-timeline.json"
time_series_count "/api/human-resources/ferias-afastamentos" "dataInicio" "MONTH" "$absence_timeline"
assert_time_series_points_at_least "$absence_timeline" 12

reputation_team="$TMPDIR_RUN/reputation-team.json"
group_by_count "/api/human-resources/vw-ranking-reputacao" "equipe" "$reputation_team"
assert_bucket_diversity_at_least "$reputation_team" 5
assert_bucket_count_at_least "$reputation_team" "S.H.I.E.L.D." 1
assert_bucket_count_at_least "$reputation_team" "Liga da Justiça" 1

reputation_media="$TMPDIR_RUN/reputation-media.json"
histogram_count "/api/human-resources/vw-ranking-reputacao" "media" 10 "$reputation_media"
assert_histogram_diversity_at_least "$reputation_media" 4

jq -n \
  --slurpfile employeeActive "$employee_active" \
  --slurpfile employeeRoles "$employee_roles" \
  --slurpfile employeeSalary "$employee_salary" \
  --slurpfile payrollDepartments "$payroll_departments" \
  --slurpfile payrollProfiles "$payroll_profiles" \
  --slurpfile payrollTimeline "$payroll_timeline" \
  --slurpfile payrollNetSalary "$payroll_net_salary" \
  --slurpfile heroUniverse "$hero_universe" \
  --slurpfile absenceType "$absence_type" \
  --slurpfile absenceTimeline "$absence_timeline" \
  --slurpfile reputationTeam "$reputation_team" \
  --slurpfile reputationMedia "$reputation_media" \
  '{
    status: "human-resources-runtime-ready",
    backendUrl: env.BACKEND_URL,
    employeeActive: $employeeActive[0].data.buckets | map({key, count}),
    employeeRoleBuckets: ($employeeRoles[0].data.buckets | length),
    employeeSalaryBuckets: ($employeeSalary[0].data.buckets | length),
    payrollDepartmentBuckets: ($payrollDepartments[0].data.buckets | length),
    payrollProfiles: $payrollProfiles[0].data.buckets | map({key, count}),
    payrollTimelinePoints: ($payrollTimeline[0].data.points | length),
    payrollNetSalaryBuckets: ($payrollNetSalary[0].data.buckets | length),
    heroUniverse: $heroUniverse[0].data.buckets | map({key, count}),
    absenceType: $absenceType[0].data.buckets | map({key, count}),
    absenceTimelinePoints: ($absenceTimeline[0].data.points | length),
    reputationTeams: $reputationTeam[0].data.buckets | map({key, count}),
    reputationMediaBuckets: ($reputationMedia[0].data.buckets | length)
  }'
