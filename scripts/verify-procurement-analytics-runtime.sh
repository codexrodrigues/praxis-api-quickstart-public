#!/usr/bin/env bash
set -euo pipefail

BACKEND_URL="${BACKEND_URL:-https://praxis-api-quickstart.onrender.com}"
export BACKEND_URL

TMPDIR_RUN="$(mktemp -d)"
cleanup() {
  rm -rf "$TMPDIR_RUN"
}
trap cleanup EXIT

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

purchase_order_status="$TMPDIR_RUN/purchase-order-status.json"
group_by_count "/api/procurement/purchase-orders" "status" "$purchase_order_status"
assert_bucket_diversity_at_least "$purchase_order_status" 4
assert_bucket_count_at_least "$purchase_order_status" "DRAFT" 1
assert_bucket_count_at_least "$purchase_order_status" "APPROVED" 1
assert_bucket_count_at_least "$purchase_order_status" "CANCELLED" 1
assert_bucket_count_at_least "$purchase_order_status" "RECEIVED" 1

for field in approvedAt cancelledAt receivedAt; do
  output_file="$TMPDIR_RUN/purchase-order-${field}.json"
  time_series_count "/api/procurement/purchase-orders" "$field" "$output_file"
  assert_time_series_count_at_least "$output_file" 1
done

supplier_status="$TMPDIR_RUN/supplier-status.json"
group_by_count "/api/procurement/suppliers" "status" "$supplier_status"
assert_bucket_count_at_least "$supplier_status" "BLOCKED" 1

supplier_risk="$TMPDIR_RUN/supplier-risk.json"
group_by_count "/api/procurement/suppliers" "riskLevel" "$supplier_risk"
assert_bucket_count_at_least "$supplier_risk" "HIGH" 1
assert_bucket_diversity_at_least "$supplier_risk" 3

contract_status="$TMPDIR_RUN/contract-status.json"
group_by_count "/api/procurement/contracts" "status" "$contract_status"
assert_bucket_count_at_least "$contract_status" "EXPIRED" 1
assert_bucket_count_at_least "$contract_status" "SIGNED" 1

product_status="$TMPDIR_RUN/product-status.json"
group_by_count "/api/procurement/products" "status" "$product_status"
assert_bucket_count_at_least "$product_status" "BLOCKED" 1

jq -n \
  --slurpfile purchaseOrderStatus "$purchase_order_status" \
  --slurpfile supplierStatus "$supplier_status" \
  --slurpfile supplierRisk "$supplier_risk" \
  --slurpfile contractStatus "$contract_status" \
  --slurpfile productStatus "$product_status" \
  '{
    status: "procurement-analytics-ready",
    backendUrl: env.BACKEND_URL,
    purchaseOrders: $purchaseOrderStatus[0].data.buckets | map({key, count}),
    suppliersByStatus: $supplierStatus[0].data.buckets | map({key, count}),
    suppliersByRisk: $supplierRisk[0].data.buckets | map({key, count}),
    contractsByStatus: $contractStatus[0].data.buckets | map({key, count}),
    productsByStatus: $productStatus[0].data.buckets | map({key, count})
  }'
