#!/usr/bin/env bash
set -euo pipefail

shopt -s nullglob
artifacts=(target/praxis-api-quickstart-*.jar)

if (( ${#artifacts[@]} != 1 )); then
  echo "Expected exactly one packaged Quickstart JAR, found ${#artifacts[@]}." >&2
  exit 1
fi

if ! jar tf "${artifacts[0]}" | grep -Eq '^BOOT-INF/lib/commons-lang3-[^/]+\.jar$'; then
  echo "Packaged Quickstart JAR is missing the commons-lang3 runtime dependency." >&2
  exit 1
fi

operational_migration='BOOT-INF/classes/db/operational-runtime-migrations/V20260905_001__acordos_regulatorios_resource_version.sql'
if ! jar tf "${artifacts[0]}" | grep -Fxq "$operational_migration"; then
  echo "Packaged Quickstart JAR is missing the Acordos Regulatorios operational migration." >&2
  exit 1
fi

operational_entrypoint='scripts/workspace/Invoke-OperationalDatasourceMigrations.sh'
if ! sh -n "$operational_entrypoint"; then
  echo "Operational datasource migration entrypoint has invalid POSIX shell syntax." >&2
  exit 1
fi

echo "Packaged runtime contract is valid: runtime dependency and operational migration are present."
