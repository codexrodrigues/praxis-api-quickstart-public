#!/usr/bin/env bash
# Disposable A/B hosted proof for the governed payroll aggregate and its bounded LKG.
# The control-plane publisher and business data-plane sessions are intentionally distinct.
set -euo pipefail

required=(
  BASE_URL_TENANT_A BASE_URL_TENANT_B ALLOWED_ORIGIN
  BUSINESS_USERNAME_TENANT_A BUSINESS_PASSWORD_TENANT_A
  BUSINESS_USERNAME_TENANT_B BUSINESS_PASSWORD_TENANT_B
  PUBLISHER_USERNAME_TENANT_A PUBLISHER_PASSWORD_TENANT_A
  PUBLISHER_USERNAME_TENANT_B PUBLISHER_PASSWORD_TENANT_B
  TOXIPROXY_ADMIN_URL TOXIPROXY_PROXY_NAME HOSTED_PROOF_OUTPUT
)
for name in "${required[@]}"; do
  if [[ -z "${!name:-}" ]]; then
    printf 'Missing required environment variable: %s\n' "$name" >&2
    exit 2
  fi
done
for command_name in cmp curl jq shasum; do
  command -v "$command_name" >/dev/null || {
    printf 'Missing required command: %s\n' "$command_name" >&2
    exit 2
  }
done

if [[ "$BUSINESS_USERNAME_TENANT_A" == "$PUBLISHER_USERNAME_TENANT_A" \
   || "$BUSINESS_USERNAME_TENANT_B" == "$PUBLISHER_USERNAME_TENANT_B" ]]; then
  printf 'Business and publisher usernames must be distinct inside each host scope.\n' >&2
  exit 2
fi

proof_dir=$(mktemp -d "${TMPDIR:-/tmp}/praxis-payroll-lkg.XXXXXX")
chmod 700 "$proof_dir"
proxy_restored=false

restore_proxy() {
  if [[ -f "$proof_dir/proxy-enabled.json" ]]; then
    curl --fail --silent --show-error --max-time 20 \
      -X POST -H 'Content-Type: application/json' \
      --data-binary @"$proof_dir/proxy-enabled.json" \
      "$TOXIPROXY_ADMIN_URL/proxies/$TOXIPROXY_PROXY_NAME" >/dev/null || true
    proxy_restored=true
  fi
  for side in a b; do
    if [[ -f "$proof_dir/business-$side.cookies" ]]; then
      if [[ "$side" == a ]]; then base_var=BASE_URL_TENANT_A; else base_var=BASE_URL_TENANT_B; fi
      curl --silent --show-error --max-time 10 -b "$proof_dir/business-$side.cookies" \
        -H "Origin: $ALLOWED_ORIGIN" \
        -X POST "${!base_var}/auth/logout" >/dev/null || true
    fi
    if [[ -f "$proof_dir/publisher-$side.cookies" ]]; then
      if [[ "$side" == a ]]; then base_var=BASE_URL_TENANT_A; else base_var=BASE_URL_TENANT_B; fi
      curl --silent --show-error --max-time 10 -b "$proof_dir/publisher-$side.cookies" \
        -H "Origin: $ALLOWED_ORIGIN" \
        -X POST "${!base_var}/auth/logout" >/dev/null || true
    fi
  done
  find "$proof_dir" -type f -exec chmod 600 {} \; 2>/dev/null || true
  find "$proof_dir" -type f -delete 2>/dev/null || true
  rmdir "$proof_dir" 2>/dev/null || true
}
trap restore_proxy EXIT INT TERM

login() {
  local base_url=$1 username=$2 password=$3 cookie_jar=$4
  local payload
  payload=$(jq -cn --arg username "$username" --arg password "$password" \
    '{username:$username,password:$password}')
  local status
  status=$(curl --silent --show-error --max-time 20 -o /dev/null -w '%{http_code}' \
    -c "$cookie_jar" -H 'Content-Type: application/json' -H "Origin: $ALLOWED_ORIGIN" \
    --data "$payload" \
    "$base_url/auth/login")
  chmod 600 "$cookie_jar"
  [[ "$status" == 204 ]] || {
    printf 'Login failed with HTTP %s\n' "$status" >&2
    return 1
  }
}

call_net_salary() {
  local base_url=$1 cookie_jar=$2 output=$3
  curl --silent --show-error --max-time 30 -o "$output" -w '%{http_code}' \
    -b "$cookie_jar" -H 'Content-Type: application/json' -H "Origin: $ALLOWED_ORIGIN" \
    --data '{"salarioBruto":10000.00,"totalDescontos":1250.00}' \
    "$base_url/api/human-resources/folhas-pagamento/determinations/net-salary"
}

call_payment_date() {
  local base_url=$1 cookie_jar=$2 output=$3
  curl --silent --show-error --max-time 30 -o "$output" -w '%{http_code}' \
    -b "$cookie_jar" -H 'Content-Type: application/json' -H "Origin: $ALLOWED_ORIGIN" \
    --data '{"ano":2026,"mes":4,"salarioLiquido":8750.00}' \
    "$base_url/api/human-resources/folhas-pagamento/determinations/payment-date"
}

login "$BASE_URL_TENANT_A" "$BUSINESS_USERNAME_TENANT_A" "$BUSINESS_PASSWORD_TENANT_A" "$proof_dir/business-a.cookies"
login "$BASE_URL_TENANT_B" "$BUSINESS_USERNAME_TENANT_B" "$BUSINESS_PASSWORD_TENANT_B" "$proof_dir/business-b.cookies"
login "$BASE_URL_TENANT_A" "$PUBLISHER_USERNAME_TENANT_A" "$PUBLISHER_PASSWORD_TENANT_A" "$proof_dir/publisher-a.cookies"
login "$BASE_URL_TENANT_B" "$PUBLISHER_USERNAME_TENANT_B" "$PUBLISHER_PASSWORD_TENANT_B" "$proof_dir/publisher-b.cookies"

cookie_fingerprints=$(for jar in "$proof_dir/business-a.cookies" "$proof_dir/business-b.cookies" \
  "$proof_dir/publisher-a.cookies" "$proof_dir/publisher-b.cookies"; do
  sed 's/^#HttpOnly_//' "$jar" | awk '!/^#/ && NF >= 7 {print $7}' \
    | shasum -a 256 | awk '{print $1}'
done | sort -u | wc -l | tr -d ' ')
[[ "$cookie_fingerprints" == 4 ]] || {
  printf 'Business and publisher sessions must be pairwise distinct across hosts.\n' >&2
  exit 2
}

publisher_status_a=0
publisher_status_b=0
business_status_a=0
business_status_b=0

for side in a b; do
  if [[ "$side" == a ]]; then side_upper=A; else side_upper=B; fi
  base_var="BASE_URL_TENANT_$side_upper"
  publisher_status=$(call_net_salary "${!base_var}" "$proof_dir/publisher-$side.cookies" "$proof_dir/publisher-$side.json")
  [[ "$publisher_status" == 403 ]] || {
    printf 'Governance publisher unexpectedly reached business data plane on host %s (HTTP %s).\n' "$side_upper" "$publisher_status" >&2
    exit 1
  }
  if [[ "$side" == a ]]; then publisher_status_a=$publisher_status; else publisher_status_b=$publisher_status; fi
  fresh_status=$(call_net_salary "${!base_var}" "$proof_dir/business-$side.cookies" "$proof_dir/fresh-net-$side.json")
  [[ "$fresh_status" == 200 ]] || {
    printf 'Business principal fresh evaluation failed on host %s (HTTP %s).\n' "$side_upper" "$fresh_status" >&2
    exit 1
  }
  if [[ "$side" == a ]]; then business_status_a=$fresh_status; else business_status_b=$fresh_status; fi
  jq -e '.salarioLiquido == 8750 and .decisionVersion == "payroll-net-v1"' "$proof_dir/fresh-net-$side.json" >/dev/null
  date_status=$(call_payment_date "${!base_var}" "$proof_dir/business-$side.cookies" "$proof_dir/fresh-date-$side.json")
  [[ "$date_status" == 200 ]] || exit 1
done

curl --fail --silent --show-error --max-time 20 \
  "$TOXIPROXY_ADMIN_URL/proxies/$TOXIPROXY_PROXY_NAME" > "$proof_dir/proxy-current.json"
jq '{name,listen,upstream,enabled:true}' "$proof_dir/proxy-current.json" > "$proof_dir/proxy-enabled.json"
jq '.enabled = false' "$proof_dir/proxy-enabled.json" > "$proof_dir/proxy-disabled.json"
curl --fail --silent --show-error --max-time 20 -X POST -H 'Content-Type: application/json' \
  --data-binary @"$proof_dir/proxy-disabled.json" \
  "$TOXIPROXY_ADMIN_URL/proxies/$TOXIPROXY_PROXY_NAME" >/dev/null

fault_started_at=$(date -u +%Y-%m-%dT%H:%M:%SZ)
for side in a b; do
  if [[ "$side" == a ]]; then side_upper=A; else side_upper=B; fi
  base_var="BASE_URL_TENANT_$side_upper"
  lkg_status=$(call_net_salary "${!base_var}" "$proof_dir/business-$side.cookies" "$proof_dir/lkg-net-$side.json")
  [[ "$lkg_status" == 200 ]] || {
    printf 'Bounded LKG evaluation failed on host %s (HTTP %s).\n' "$side_upper" "$lkg_status" >&2
    exit 1
  }
  cmp -s "$proof_dir/fresh-net-$side.json" "$proof_dir/lkg-net-$side.json" || {
    printf 'LKG evaluation changed deterministic response on host %s.\n' "$side_upper" >&2
    exit 1
  }
done

curl --fail --silent --show-error --max-time 20 -X POST -H 'Content-Type: application/json' \
  --data-binary @"$proof_dir/proxy-enabled.json" \
  "$TOXIPROXY_ADMIN_URL/proxies/$TOXIPROXY_PROXY_NAME" >/dev/null
proxy_restored=true
fault_ended_at=$(date -u +%Y-%m-%dT%H:%M:%SZ)

for side in a b; do
  if [[ "$side" == a ]]; then side_upper=A; else side_upper=B; fi
  base_var="BASE_URL_TENANT_$side_upper"
  recovery_status=$(call_net_salary "${!base_var}" "$proof_dir/business-$side.cookies" "$proof_dir/recovery-net-$side.json")
  [[ "$recovery_status" == 200 ]] || {
    printf 'Control-plane recovery failed on host %s (HTTP %s).\n' "$side_upper" "$recovery_status" >&2
    exit 1
  }
done

mkdir -p "$(dirname "$HOSTED_PROOF_OUTPUT")"
jq -n \
  --arg faultStartedAtUtc "$fault_started_at" \
  --arg faultEndedAtUtc "$fault_ended_at" \
  --arg hostAHash "$(printf '%s' "$BASE_URL_TENANT_A" | shasum -a 256 | awk '{print $1}')" \
  --arg hostBHash "$(printf '%s' "$BASE_URL_TENANT_B" | shasum -a 256 | awk '{print $1}')" \
  --arg freshAHash "$(shasum -a 256 "$proof_dir/fresh-net-a.json" | awk '{print $1}')" \
  --arg freshBHash "$(shasum -a 256 "$proof_dir/fresh-net-b.json" | awk '{print $1}')" \
  --arg publisherStatusA "$publisher_status_a" \
  --arg publisherStatusB "$publisher_status_b" \
  --arg businessStatusA "$business_status_a" \
  --arg businessStatusB "$business_status_b" \
  '{schemaVersion:"praxis-payroll-reactive-determination-hosted-smoke/v1",status:"PASS",identitySeparation:{sessionsDistinct:true,publisherTenantAHttpStatus:($publisherStatusA|tonumber),publisherTenantBHttpStatus:($publisherStatusB|tonumber),businessTenantAHttpStatus:($businessStatusA|tonumber),businessTenantBHttpStatus:($businessStatusB|tonumber)},hosts:{tenantA:{baseUrlHash:$hostAHash,freshResponseSha256:$freshAHash,lkgResponseMatched:true,recoveryHttpStatus:200},tenantB:{baseUrlHash:$hostBHash,freshResponseSha256:$freshBHash,lkgResponseMatched:true,recoveryHttpStatus:200}},faultWindow:{startedAtUtc:$faultStartedAtUtc,endedAtUtc:$faultEndedAtUtc,configDatasourceOnly:true},proxyRestored:true}' \
  > "$HOSTED_PROOF_OUTPUT"
chmod 600 "$HOSTED_PROOF_OUTPUT"
printf 'Hosted payroll LKG proof passed; redacted evidence: %s\n' "$HOSTED_PROOF_OUTPUT"
