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

equipment_surfaces="$TMPDIR_RUN/equipment-surfaces.json"
get_json "/schemas/surfaces?resource=assets.equipamentos" "$equipment_surfaces"
assert_surface_exists "$equipment_surfaces" "equipment-inventory-board"

custody_surfaces="$TMPDIR_RUN/custody-surfaces.json"
get_json "/schemas/surfaces?resource=assets.equipamento-alocacoes" "$custody_surfaces"
assert_surface_exists "$custody_surfaces" "equipment-custody-board"

vehicle_surfaces="$TMPDIR_RUN/vehicle-surfaces.json"
get_json "/schemas/surfaces?resource=assets.veiculos" "$vehicle_surfaces"
assert_surface_exists "$vehicle_surfaces" "fleet-readiness-board"

usage_surfaces="$TMPDIR_RUN/usage-surfaces.json"
get_json "/schemas/surfaces?resource=assets.veiculo-missao-usos" "$usage_surfaces"
assert_surface_exists "$usage_surfaces" "mission-fleet-usage-board"

equipment_status="$TMPDIR_RUN/equipment-status.json"
group_by_count "/api/assets/equipamentos" "status" "$equipment_status"
assert_bucket_diversity_at_least "$equipment_status" 5
assert_bucket_count_at_least "$equipment_status" "EM_USO" 1
assert_bucket_count_at_least "$equipment_status" "ESTOQUE" 1
assert_bucket_count_at_least "$equipment_status" "QUEBRADO" 1

equipment_resistance="$TMPDIR_RUN/equipment-resistance.json"
histogram_count "/api/assets/equipamentos" "resistencia" 2 "$equipment_resistance"
assert_histogram_diversity_at_least "$equipment_resistance" 3

custody_status="$TMPDIR_RUN/custody-status.json"
group_by_count "/api/assets/equipamento-alocacoes" "status" "$custody_status"
assert_bucket_diversity_at_least "$custody_status" 4
assert_bucket_count_at_least "$custody_status" "ATIVO" 1
assert_bucket_count_at_least "$custody_status" "DEVOLVIDO" 1
assert_bucket_count_at_least "$custody_status" "DANIFICADO" 1

custody_timeline="$TMPDIR_RUN/custody-timeline.json"
time_series_count "/api/assets/equipamento-alocacoes" "inicio" "$custody_timeline"
assert_time_series_count_at_least "$custody_timeline" 12

vehicle_status="$TMPDIR_RUN/vehicle-status.json"
group_by_count "/api/assets/veiculos" "status" "$vehicle_status"
assert_bucket_diversity_at_least "$vehicle_status" 3
assert_bucket_count_at_least "$vehicle_status" "OPERACIONAL" 1
assert_bucket_count_at_least "$vehicle_status" "MANUTENCAO" 1
assert_bucket_count_at_least "$vehicle_status" "INOPERANTE" 1

mission_vehicle_usage="$TMPDIR_RUN/mission-vehicle-usage.json"
group_by_count "/api/assets/veiculo-missao-usos" "veiculoId" "$mission_vehicle_usage"
assert_bucket_diversity_at_least "$mission_vehicle_usage" 5

jq -n \
  --slurpfile equipmentStatus "$equipment_status" \
  --slurpfile equipmentResistance "$equipment_resistance" \
  --slurpfile custodyStatus "$custody_status" \
  --slurpfile custodyTimeline "$custody_timeline" \
  --slurpfile vehicleStatus "$vehicle_status" \
  --slurpfile missionVehicleUsage "$mission_vehicle_usage" \
  '{
    status: "assets-runtime-ready",
    backendUrl: env.BACKEND_URL,
    equipmentStatus: $equipmentStatus[0].data.buckets | map({key, count}),
    equipmentResistanceBuckets: ($equipmentResistance[0].data.buckets | length),
    custodyStatus: $custodyStatus[0].data.buckets | map({key, count}),
    custodyTimelinePoints: ($custodyTimeline[0].data.points | length),
    vehicleStatus: $vehicleStatus[0].data.buckets | map({key, count}),
    missionVehicleUsage: $missionVehicleUsage[0].data.buckets | map({key, count})
  }'
