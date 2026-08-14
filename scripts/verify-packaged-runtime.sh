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

echo "Packaged runtime contract is valid: commons-lang3 is present."
