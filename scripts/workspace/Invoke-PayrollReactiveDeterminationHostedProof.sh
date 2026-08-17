#!/usr/bin/env bash
# Creates, proves and always removes a disposable Render A/B + PostgreSQL + Toxiproxy lane.
set -euo pipefail
umask 077

required=(SMOKE_RUN_ID SOURCE_COMMIT RENDER_WORKSPACE_ID HOSTED_PROOF_CONFIRM)
for name in "${required[@]}"; do
  [[ -n "${!name:-}" ]] || { printf 'Missing required environment variable: %s\n' "$name" >&2; exit 2; }
done
[[ "$HOSTED_PROOF_CONFIRM" == "CREATE_DISPOSABLE_RENDER_RESOURCES" ]] || {
  echo 'HOSTED_PROOF_CONFIRM does not authorize disposable resource creation.' >&2; exit 2;
}
[[ "$SMOKE_RUN_ID" =~ ^[0-9]{8}T[0-9]{6}Z$ ]] || { echo 'SMOKE_RUN_ID must be UTC YYYYMMDDTHHMMSSZ.' >&2; exit 2; }
[[ "$SOURCE_COMMIT" =~ ^[0-9a-f]{40}$ ]] || { echo 'SOURCE_COMMIT must be a full lowercase Git SHA.' >&2; exit 2; }

for command_name in render git jq curl openssl python3 java shasum mvn; do
  command -v "$command_name" >/dev/null || { printf 'Missing required command: %s\n' "$command_name" >&2; exit 2; }
done

repo_root=$(git rev-parse --show-toplevel)
[[ -f "$repo_root/pom.xml" && -f "$repo_root/scripts/workspace/Provision-PayrollReactiveDeterminationHostedFixture.py" ]] || {
  echo 'Run from the praxis-api-quickstart repository.' >&2; exit 2;
}
git cat-file -e "$SOURCE_COMMIT^{commit}"
[[ -z "$(git status --porcelain)" ]] || { echo 'Hosted proof requires a clean worktree.' >&2; exit 2; }
[[ "$(git rev-parse HEAD)" == "$SOURCE_COMMIT" ]] || {
  echo 'Hosted proof requires the checked-out HEAD to equal SOURCE_COMMIT.' >&2; exit 2;
}

ttl_seconds=${HOSTED_TTL_SECONDS:-7200}
poll_seconds=${HOSTED_POLL_SECONDS:-10}
deploy_timeout_seconds=${HOSTED_DEPLOY_TIMEOUT_SECONDS:-900}
cost_ceiling=${HOSTED_COST_CEILING_USD:-0.30}
[[ "$ttl_seconds" =~ ^[0-9]+$ && "$ttl_seconds" -le 7200 ]] || { echo 'HOSTED_TTL_SECONDS must be <= 7200.' >&2; exit 2; }
[[ "$deploy_timeout_seconds" =~ ^[0-9]+$ && "$deploy_timeout_seconds" -le 1200 ]] || { echo 'Deploy timeout must be <= 1200 seconds.' >&2; exit 2; }
python3 - "$cost_ceiling" <<'PY'
import decimal, sys
if decimal.Decimal(sys.argv[1]) > decimal.Decimal('0.30'):
    raise SystemExit('HOSTED_COST_CEILING_USD exceeds the authorized US$0.30 ceiling')
PY
# Two Standard services, one Starter proxy and one Basic 256 MB PostgreSQL for at most two hours.
estimated_upper_bound=0.17
python3 - "$estimated_upper_bound" "$cost_ceiling" <<'PY'
import decimal, sys
if decimal.Decimal(sys.argv[1]) > decimal.Decimal(sys.argv[2]):
    raise SystemExit('Estimated disposable Render cost exceeds the configured ceiling')
PY

run_slug=$(printf '%s' "$SMOKE_RUN_ID" | tr '[:upper:]' '[:lower:]')
branch="smoke/rd-lkg-$SMOKE_RUN_ID-${SOURCE_COMMIT:0:7}"
artifact_dir=${HOSTED_ARTIFACT_DIR:-"${TMPDIR:-/tmp}/praxis-rd-lkg-$SMOKE_RUN_ID-${SOURCE_COMMIT:0:7}"}
mkdir -p "$artifact_dir"
chmod 700 "$artifact_dir"
ledger="$artifact_dir/ledger.json"
secrets="$artifact_dir/identity-secrets.json"
app_jar=
origin="https://rd-lkg-proof-$run_slug.invalid"

jq -n --arg runId "$SMOKE_RUN_ID" --arg sourceCommit "$SOURCE_COMMIT" --arg branch "$branch" \
  --argjson ttl "$ttl_seconds" --argjson cost "$estimated_upper_bound" \
  '{schemaVersion:"praxis-render-hosted-proof-ledger/v2",runId:$runId,sourceCommit:$sourceCommit,branch:$branch,ttlSeconds:$ttl,estimatedCostUpperBoundUsd:$cost,resources:[]}' > "$ledger"

random_secret() { openssl rand -base64 36 | tr -d '\n'; }
jq -n \
  --arg businessA "$(random_secret)" --arg businessB "$(random_secret)" \
  --arg authorA "$(random_secret)" --arg authorB "$(random_secret)" \
  --arg approverAA "$(random_secret)" --arg approverAB "$(random_secret)" \
  --arg approverBA "$(random_secret)" --arg approverBB "$(random_secret)" \
  --arg publisherA "$(random_secret)" --arg publisherB "$(random_secret)" \
  --arg operatorA "$(random_secret)" --arg operatorB "$(random_secret)" \
  --arg auditorA "$(random_secret)" --arg auditorB "$(random_secret)" \
  --arg jwtA "$(random_secret)" --arg jwtB "$(random_secret)" \
  '{businessA:$businessA,businessB:$businessB,authorA:$authorA,authorB:$authorB,approverAA:$approverAA,approverAB:$approverAB,approverBA:$approverBA,approverBB:$approverBB,publisherA:$publisherA,publisherB:$publisherB,operatorA:$operatorA,operatorB:$operatorB,auditorA:$auditorA,auditorB:$auditorB,jwtA:$jwtA,jwtB:$jwtB}' > "$secrets"

resource_id() { jq -r '.id // .service.id // .data.id // .data.service.id // empty' "$1"; }
append_resource() {
  local id=$1 kind=$2 name=$3
  jq --arg id "$id" --arg kind "$kind" --arg name "$name" \
    '.resources += [{id:$id,kind:$kind,name:$name,status:"created"}]' "$ledger" > "$ledger.tmp"
  mv "$ledger.tmp" "$ledger"
}

delete_id() {
  local kind=$1 id=$2
  [[ -n "$id" ]] || return 0
  if [[ "$kind" == postgres ]]; then
    render postgres delete "$id" --confirm --output text >> "$artifact_dir/cleanup.log" 2>&1 || true
  else
    render services delete "$id" --confirm --output text >> "$artifact_dir/cleanup.log" 2>&1 || true
  fi
}

cleanup() {
  trap - EXIT INT TERM
  [[ -z "${deadline_pid:-}" ]] || kill "$deadline_pid" 2>/dev/null || true
  if [[ -f "$ledger" ]]; then
    while IFS=$'\t' read -r kind id; do delete_id "$kind" "$id"; done < <(
      jq -r '.resources | reverse[] | [.kind,.id] | @tsv' "$ledger")
    jq '.resources |= map(.status="deletion_requested")' "$ledger" > "$ledger.tmp" && mv "$ledger.tmp" "$ledger"
    if render services --output json > "$artifact_dir/services-after-cleanup.json" 2>> "$artifact_dir/cleanup.log" \
        && render postgres list --output json > "$artifact_dir/postgres-after-cleanup.json" 2>> "$artifact_dir/cleanup.log"; then
      while IFS=$'\t' read -r kind id; do
        inventory="$artifact_dir/services-after-cleanup.json"
        [[ "$kind" == postgres ]] && inventory="$artifact_dir/postgres-after-cleanup.json"
        status=deleted
        jq -e --arg id "$id" '[.. | objects | select(.id? == $id)] | length == 0' "$inventory" >/dev/null \
          || status=cleanup_failed
        jq --arg id "$id" --arg status "$status" \
          '.resources |= map(if .id==$id then .status=$status else . end)' \
          "$ledger" > "$ledger.tmp" && mv "$ledger.tmp" "$ledger"
      done < <(jq -r '.resources[] | [.kind,.id] | @tsv' "$ledger")
    else
      jq '.cleanupVerification="provider_query_failed"' "$ledger" > "$ledger.tmp" && mv "$ledger.tmp" "$ledger"
    fi
    jq '.cleanupCompletedAtUtc=(now|todateiso8601)' "$ledger" > "$ledger.tmp" && mv "$ledger.tmp" "$ledger"
  fi
  git push origin --delete "$branch" >> "$artifact_dir/cleanup.log" 2>&1 || true
  branch_query=
  if branch_query=$(git ls-remote origin "refs/heads/$branch" 2>> "$artifact_dir/cleanup.log") \
      && [[ -z "$branch_query" ]]; then
    jq '.branchStatus="deleted"' "$ledger" > "$ledger.tmp" && mv "$ledger.tmp" "$ledger"
  else
    jq '.branchStatus="cleanup_failed"' "$ledger" > "$ledger.tmp" && mv "$ledger.tmp" "$ledger"
  fi
  rm -f "$secrets"
  chmod 600 "$artifact_dir"/* 2>/dev/null || true
}
trap cleanup EXIT INT TERM
(sleep "$ttl_seconds"; kill -TERM $$) > "$artifact_dir/ttl-deadline.log" 2>&1 &
deadline_pid=$!

render whoami --output json > "$artifact_dir/render-whoami.json"
render workspace current --output json > "$artifact_dir/render-workspace.json"
jq -e --arg workspace "$RENDER_WORKSPACE_ID" '
  [.. | objects | .id? // empty] | index($workspace) != null
' "$artifact_dir/render-workspace.json" >/dev/null || { echo 'Active Render workspace does not match RENDER_WORKSPACE_ID.' >&2; exit 2; }

names=(
  "praxis-rd-lkg-a-$run_slug" "praxis-rd-lkg-b-$run_slug"
  "praxis-rd-lkg-proxy-$run_slug" "praxis-rd-lkg-pg-$run_slug"
)
# cleanup-before-create is exact-name only and covers incomplete attempts with the same run id.
render services --output json > "$artifact_dir/preexisting-services.json"
for name in "${names[@]:0:3}"; do
  while IFS= read -r id; do delete_id service "$id"; done < <(
    jq -r --arg name "$name" '.[] | (.service // .) | select(.name==$name) | .id' "$artifact_dir/preexisting-services.json")
done
render postgres list --output json > "$artifact_dir/preexisting-postgres.json"
while IFS= read -r id; do delete_id postgres "$id"; done < <(
  jq -r --arg name "${names[3]}" '.[] | (.postgres // .) | select(.name==$name) | .id' "$artifact_dir/preexisting-postgres.json")
render services --output json > "$artifact_dir/services-after-precleanup.json"
render postgres list --output json > "$artifact_dir/postgres-after-precleanup.json"
for name in "${names[@]:0:3}"; do
  jq -e --arg name "$name" '[.[] | (.service // .) | select(.name==$name)] | length == 0' \
    "$artifact_dir/services-after-precleanup.json" >/dev/null || {
      echo "cleanup-before-create did not remove service $name" >&2; exit 1;
    }
done
jq -e --arg name "${names[3]}" '[.[] | (.postgres // .) | select(.name==$name)] | length == 0' \
  "$artifact_dir/postgres-after-precleanup.json" >/dev/null || {
    echo "cleanup-before-create did not remove PostgreSQL ${names[3]}" >&2; exit 1;
  }
git push origin --delete "$branch" >> "$artifact_dir/cleanup-before-create.log" 2>&1 || true
git push origin "$SOURCE_COMMIT:refs/heads/$branch" >> "$artifact_dir/branch-create.log" 2>&1
[[ "$(git ls-remote origin "refs/heads/$branch" | awk '{print $1}')" == "$SOURCE_COMMIT" ]] || {
  echo 'Remote smoke branch did not resolve to SOURCE_COMMIT.' >&2; exit 1;
}

mvn -B -DskipTests package > "$artifact_dir/package.log"
jar_count=$(find "$repo_root/target" -maxdepth 1 -type f -name 'praxis-api-quickstart-*.jar' ! -name '*.original' | wc -l | tr -d ' ')
[[ "$jar_count" == 1 ]] || { echo 'Expected exactly one packaged Quickstart executable jar.' >&2; exit 1; }
app_jar=$(find "$repo_root/target" -maxdepth 1 -type f -name 'praxis-api-quickstart-*.jar' ! -name '*.original' -print)

render postgres create --confirm --output json --workspace "$RENDER_WORKSPACE_ID" \
  --name "${names[3]}" --plan basic_256mb --version 17 --region oregon \
  > "$artifact_dir/postgres-create.private.json"
pg_id=$(resource_id "$artifact_dir/postgres-create.private.json")
[[ -n "$pg_id" ]] || { echo 'Render did not return the PostgreSQL id.' >&2; exit 1; }
append_resource "$pg_id" postgres "${names[3]}"

poll_postgres() {
  local deadline=$((SECONDS + deploy_timeout_seconds)) status
  while (( SECONDS < deadline )); do
    render postgres get "$pg_id" --output json > "$artifact_dir/postgres-status.private.json"
    status=$(jq -r '.status // .data.status // empty' "$artifact_dir/postgres-status.private.json")
    [[ "$status" == available ]] && return 0
    [[ "$status" =~ ^(failed|unavailable|suspended)$ ]] && { echo "PostgreSQL reached terminal status $status" >&2; return 1; }
    sleep "$poll_seconds"
  done
  echo 'PostgreSQL availability timeout expired.' >&2; return 1
}
poll_postgres

render services create --confirm --output json --name "${names[2]}" --type web_service \
  --image ghcr.io/shopify/toxiproxy:2.12.0 --plan starter --region oregon \
  > "$artifact_dir/proxy-create.json"
proxy_id=$(resource_id "$artifact_dir/proxy-create.json")
[[ -n "$proxy_id" ]] || { echo 'Render did not return the Toxiproxy service id.' >&2; exit 1; }
append_resource "$proxy_id" service "${names[2]}"

poll_service() {
  local id=$1 label=$2 deadline=$((SECONDS + deploy_timeout_seconds)) status commit
  while (( SECONDS < deadline )); do
    render deploys list "$id" --output json > "$artifact_dir/$label-deploys.json"
    status=$(jq -r '.[0] | (.deploy // .) | .status // empty' "$artifact_dir/$label-deploys.json")
    commit=$(jq -r '.[0] | (.deploy // .) | .commit.id // .commitId // empty' "$artifact_dir/$label-deploys.json")
    if [[ "$status" == live ]]; then
      [[ -z "$commit" || "$commit" == "$SOURCE_COMMIT" ]] || { echo "$label deployed an unexpected commit." >&2; return 1; }
      return 0
    fi
    [[ "$status" =~ ^(build_failed|update_failed|canceled|deactivated)$ ]] && {
      echo "$label reached terminal deploy status $status" >&2; return 1;
    }
    sleep "$poll_seconds"
  done
  echo "$label deploy timeout expired." >&2; return 1
}
poll_service "$proxy_id" proxy

pg_json="$artifact_dir/postgres-status.private.json"
pg_user=$(jq -r '.databaseUser // .data.databaseUser // empty' "$pg_json")
pg_database=$(jq -r '.databaseName // .data.databaseName // empty' "$pg_json")
pg_password=$(jq -r '.connectionInfo.password // .data.connectionInfo.password // empty' "$pg_json")
pg_internal=$(jq -r '.connectionInfo.internalConnectionString // .data.connectionInfo.internalConnectionString // empty' "$pg_json")
[[ -n "$pg_user" && -n "$pg_database" && -n "$pg_password" && -n "$pg_internal" ]] || {
  echo 'Render PostgreSQL connection contract is incomplete.' >&2; exit 1;
}
pg_host=$(printf '%s' "$pg_internal" | sed -E 's#^postgresql://[^@]+@([^/]+)/.*#\1#')
proxy_url=$(jq -r '.serviceDetails.url // .service.serviceDetails.url // empty' "$artifact_dir/proxy-create.json")
[[ -n "$pg_host" && "$pg_host" != "$pg_internal" && -n "$proxy_url" ]] || {
  echo 'Could not derive the internal PostgreSQL host or proxy URL.' >&2; exit 1;
}
curl --fail --silent --show-error --max-time 20 -H 'Content-Type: application/json' \
  --data "$(jq -cn --arg upstream "$pg_host:5432" '{name:"config-pg",listen:"[::]:8666",upstream:$upstream,enabled:true}')" \
  "$proxy_url/proxies" > "$artifact_dir/proxy-config.json"

create_host() {
  local side=$1 name=$2 side_lower tenant business author approver_a approver_b publisher operator auditor jwt output id
  side_lower=$(printf '%s' "$side" | tr '[:upper:]' '[:lower:]')
  tenant="corporate-$side_lower-$SMOKE_RUN_ID"
  business=$(jq -r ".business$side" "$secrets")
  author=$(jq -r ".author$side" "$secrets")
  approver_a=$(jq -r ".approver${side}A" "$secrets")
  approver_b=$(jq -r ".approver${side}B" "$secrets")
  publisher=$(jq -r ".publisher$side" "$secrets")
  operator=$(jq -r ".operator$side" "$secrets")
  auditor=$(jq -r ".auditor$side" "$secrets")
  jwt=$(jq -r ".jwt$side" "$secrets")
  output="$artifact_dir/service-$side_lower-create.private.json"
  render services create --confirm --output json --name "$name" --type web_service \
    --repo https://github.com/codexrodrigues/praxis-api-quickstart --runtime docker --branch "$branch" \
    --plan standard --region oregon --health-check-path /actuator/health \
    --pre-deploy-command '/app/bin/praxis-operational-migrate' \
    --env-var "SPRING_PROFILES_ACTIVE=prod" \
    --env-var "SPRING_JPA_HIBERNATE_DDL_AUTO=validate" \
    --env-var "PRAXIS_OPERATIONAL_BOOTSTRAP_MODE=hosted-public-demo-fixture" \
    --env-var "SPRING_DATASOURCE_URL=jdbc:postgresql://$pg_host/$pg_database?sslmode=require" \
    --env-var "SPRING_DATASOURCE_USERNAME=$pg_user" --env-var "SPRING_DATASOURCE_PASSWORD=$pg_password" \
    --env-var "OPERATIONAL_MIGRATION_DATASOURCE_URL=jdbc:postgresql://$pg_host/$pg_database?sslmode=require" \
    --env-var "OPERATIONAL_MIGRATION_DATASOURCE_USERNAME=$pg_user" \
    --env-var "OPERATIONAL_MIGRATION_DATASOURCE_PASSWORD=$pg_password" \
    --env-var "OPERATIONAL_RUNTIME_ROLE=$pg_user" \
    --env-var "CONFIG_DATASOURCE_URL=jdbc:postgresql://$proxy_id:8666/$pg_database?sslmode=require" \
    --env-var "CONFIG_DATASOURCE_USERNAME=$pg_user" --env-var "CONFIG_DATASOURCE_PASSWORD=$pg_password" \
    --env-var "PRACTICE_TEMP_PASSWORD=$business" --env-var "APP_JWT_SECRET=$jwt" \
    --env-var "PRAXIS_RESOURCE_VERSION_ETAG_SECRET=$jwt" \
    --env-var "APP_SESSION_SECURE=true" --env-var "APP_SESSION_SAMESITE=None" \
    --env-var "APP_SECURITY_WRITE_DISABLED=true" \
    --env-var "APP_SECURITY_CONFIG_ORIGIN_RESTRICTION_ENABLED=true" \
    --env-var "APP_SECURITY_CONFIG_ORIGIN_RESTRICTION_ALLOWED_ORIGINS=$origin" \
    --env-var "CORS_ALLOWED_ORIGINS=$origin" --env-var "APP_AUTH_GOVERNANCE_LAB_ENABLED=true" \
    --env-var "APP_AUTH_GOVERNANCE_AUTHOR_USERNAME=author-$side_lower" \
    --env-var "APP_AUTH_GOVERNANCE_AUTHOR_PASSWORD=$author" \
    --env-var "APP_AUTH_GOVERNANCE_APPROVER_A_USERNAME=approver-a-$side_lower" \
    --env-var "APP_AUTH_GOVERNANCE_APPROVER_A_PASSWORD=$approver_a" \
    --env-var "APP_AUTH_GOVERNANCE_APPROVER_B_USERNAME=approver-b-$side_lower" \
    --env-var "APP_AUTH_GOVERNANCE_APPROVER_B_PASSWORD=$approver_b" \
    --env-var "APP_AUTH_GOVERNANCE_PUBLISHER_USERNAME=publisher-$side_lower" \
    --env-var "APP_AUTH_GOVERNANCE_PUBLISHER_PASSWORD=$publisher" \
    --env-var "APP_AUTH_GOVERNANCE_OPERATOR_USERNAME=operator-$side_lower" \
    --env-var "APP_AUTH_GOVERNANCE_OPERATOR_PASSWORD=$operator" \
    --env-var "APP_AUTH_GOVERNANCE_AUDITOR_USERNAME=auditor-$side_lower" \
    --env-var "APP_AUTH_GOVERNANCE_AUDITOR_PASSWORD=$auditor" \
    --env-var "PRAXIS_AI_SECURITY_CORPORATE_MODE=true" \
    --env-var "PRAXIS_AI_SECURITY_ALLOW_DEFAULT_TENANT_IN_CORPORATE=true" \
    --env-var "PRAXIS_AI_SECURITY_SERVER_DEFAULT_TENANT=$tenant" \
    --env-var "PRAXIS_AI_SECURITY_SERVER_DEFAULT_ENVIRONMENT=prod" \
    --env-var "PRAXIS_REACTIVE_DETERMINATIONS_LKG_MAX_STALENESS=PT5M" > "$output"
  id=$(resource_id "$output")
  [[ -n "$id" ]] || { echo "Render did not return the $side service id." >&2; return 1; }
  append_resource "$id" service "$name"; printf '%s' "$id"
}

service_a=$(create_host A "${names[0]}")
service_b=$(create_host B "${names[1]}")
poll_service "$service_a" service-a
poll_service "$service_b" service-b

base_a=$(jq -r '.serviceDetails.url // .service.serviceDetails.url' "$artifact_dir/service-a-create.private.json")
base_b=$(jq -r '.serviceDetails.url // .service.serviceDetails.url' "$artifact_dir/service-b-create.private.json")
[[ -n "$base_a" && "$base_a" != null && -n "$base_b" && "$base_b" != null ]] || {
  echo 'Render did not return both hosted service URLs.' >&2; exit 1;
}
for side in A B; do
  side_lower=$(printf '%s' "$side" | tr '[:upper:]' '[:lower:]')
  base_var="base_$side_lower"; base=${!base_var}
  HOSTED_FIXTURE_BASE_URL="$base" HOSTED_FIXTURE_ORIGIN="$origin" \
  HOSTED_FIXTURE_TENANT="corporate-$side_lower-$SMOKE_RUN_ID" HOSTED_FIXTURE_ENVIRONMENT=prod \
  HOSTED_FIXTURE_APP_JAR="$app_jar" HOSTED_FIXTURE_OUTPUT="$artifact_dir/fixture-$side_lower.json" \
  HOSTED_FIXTURE_AUTHOR_USERNAME="author-$side_lower" \
  HOSTED_FIXTURE_AUTHOR_PASSWORD="$(jq -r ".author$side" "$secrets")" \
  HOSTED_FIXTURE_APPROVER_A_USERNAME="approver-a-$side_lower" \
  HOSTED_FIXTURE_APPROVER_A_PASSWORD="$(jq -r ".approver${side}A" "$secrets")" \
  HOSTED_FIXTURE_APPROVER_B_USERNAME="approver-b-$side_lower" \
  HOSTED_FIXTURE_APPROVER_B_PASSWORD="$(jq -r ".approver${side}B" "$secrets")" \
  HOSTED_FIXTURE_PUBLISHER_USERNAME="publisher-$side_lower" \
  HOSTED_FIXTURE_PUBLISHER_PASSWORD="$(jq -r ".publisher$side" "$secrets")" \
    python3 "$repo_root/scripts/workspace/Provision-PayrollReactiveDeterminationHostedFixture.py"
done

BASE_URL_TENANT_A="$base_a" BASE_URL_TENANT_B="$base_b" ALLOWED_ORIGIN="$origin" \
BUSINESS_USERNAME_TENANT_A=admin BUSINESS_PASSWORD_TENANT_A="$(jq -r .businessA "$secrets")" \
BUSINESS_USERNAME_TENANT_B=admin BUSINESS_PASSWORD_TENANT_B="$(jq -r .businessB "$secrets")" \
PUBLISHER_USERNAME_TENANT_A=publisher-a PUBLISHER_PASSWORD_TENANT_A="$(jq -r .publisherA "$secrets")" \
PUBLISHER_USERNAME_TENANT_B=publisher-b PUBLISHER_PASSWORD_TENANT_B="$(jq -r .publisherB "$secrets")" \
TOXIPROXY_ADMIN_URL="$proxy_url" TOXIPROXY_PROXY_NAME=config-pg \
HOSTED_PROOF_OUTPUT="$artifact_dir/hosted-proof.redacted.json" \
  "$repo_root/scripts/workspace/Invoke-PayrollReactiveDeterminationHostedSmoke.sh"

jq '.result="PASS" | .proofCompletedAtUtc=(now|todateiso8601)' "$ledger" > "$ledger.tmp"
mv "$ledger.tmp" "$ledger"
echo "Hosted proof completed; cleanup will run before exit. Evidence: $artifact_dir"
