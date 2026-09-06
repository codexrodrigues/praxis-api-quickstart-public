#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

# Keep one governed implementation of the runtime proof. The focused entrypoint
# only selects the workflow_action slice of the canonical multi-persona smoke.
export REQUIRE_PUBLICATION="${REQUIRE_PUBLICATION:-true}"
export REQUIRE_BACKEND_VALIDATION="${REQUIRE_BACKEND_VALIDATION:-false}"
export REQUIRE_WORKFLOW_ACTION="${REQUIRE_WORKFLOW_ACTION:-true}"
export REQUIRE_APPROVAL_POLICY="${REQUIRE_APPROVAL_POLICY:-false}"

exec bash "$SCRIPT_DIR/verify-domain-rules-runtime.sh" "$@"
