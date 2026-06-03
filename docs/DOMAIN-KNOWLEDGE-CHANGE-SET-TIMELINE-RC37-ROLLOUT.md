# Domain Knowledge Change-Set Timeline rc.37 Rollout

Status: `praxis-config-starter:0.1.0-rc.37` was published and resolved from
Maven Central on 2026-05-01; this quickstart now consumes it as the operational
reference host for the Domain Knowledge change-set timeline.

This quickstart is the operational reference host for Praxis semantic decisions
authored by AI. It proves the canonical
`/api/praxis/config/domain-knowledge/**` behavior published by
`praxis-config-starter`, but it must not redefine that behavior locally.

## Current State

- `pom.xml` now consumes `praxis.config.version=0.1.0-rc.37`.
- `0.1.0-rc.37` includes the Domain Knowledge change-set timeline endpoint.
- The quickstart smoke already supports the timeline gate through
  `REQUIRE_CHANGE_SET_TIMELINE=auto|true|false`.
- Default `auto` mode remains useful while a deployed host is still rolling out.
- Strict mode `REQUIRE_CHANGE_SET_TIMELINE=true` is the expected phase gate for
  this published starter contract.

## Endpoint Under Rollout

```text
GET /api/praxis/config/domain-knowledge/change-sets/{changeSetId}/timeline
```

Expected safe events:

- `change_set.created`
- `validation.completed`
- `review.approved` or `review.rejected`
- `change_set.applied` when the change set has been applied

The endpoint is safe observability over persisted governance state. It must not
return raw patch payloads, evidence payload text, `sourcePointer`, `sourceUri`,
`patchHash`, prompt content or chat history.

## Local Evidence Already Collected

Before this rollout note, the platform was proved locally without publishing a
new Maven coordinate:

- `praxis-config-starter` PR #177 added the endpoint and was merged to `main`
  as `e32d6ff`.
- `praxis-api-quickstart` PR #48 added the strict smoke support and was merged
  to `main` as `3cb74ef`.
- Quickstart PR CI passed in run `25199824379`.
- Quickstart main CI passed in run `25199903432`.
- A local quickstart packaged with the locally installed starter passed the
  managed PostgreSQL-backed strict smoke:

```bash
BASE_URL=http://localhost:8099 TENANT_ID=desenv ENVIRONMENT=local \
REQUIRE_CHANGE_SET_TIMELINE=true \
tools/local-e2e/run-domain-knowledge-change-set-local.sh
```

The smoke returned:

```text
domain-knowledge-change-set-timeline-ready
eventCount=4
```

and completed:

```text
created -> validated -> approved -> applied -> readback-confirmed -> timeline-confirmed-or-skipped
```

## Published Artifact

Maven Central resolution has succeeded for:

```text
io.github.codexrodrigues:praxis-config-starter:0.1.0-rc.37
```

Keep rollout-compatible `auto` gates only while a specific deployed quickstart
host is still being redeployed. For this source line and local validation, use
`REQUIRE_CHANGE_SET_TIMELINE=true`.

## Completed Before Quickstart Version Bump

These items are already complete:

1. Starter endpoint implemented and merged.
2. Starter focal tests passed locally.
3. Starter main CI passed.
4. Quickstart smoke support merged.
5. Quickstart PR CI and main CI passed.
6. Local managed PostgreSQL strict smoke passed against a quickstart packaged with the local
   starter.

## Completed Publication Path

Completed for the starter release and quickstart bump:

1. Published `praxis-config-starter:0.1.0-rc.37` from `praxis-config-starter/main`.
2. Confirmed Maven Central resolution:

```bash
mvn -q dependency:get \
  -Dartifact=io.github.codexrodrigues:praxis-config-starter:0.1.0-rc.37 \
  -Dtransitive=false
```

3. Updated this quickstart:
   - `pom.xml`: `praxis.config.version=0.1.0-rc.37`
   - `README.md`: dependency and rollout references
   - this rollout note: status and post-bump evidence
4. Run the smallest reliable local validation before merge:

```bash
mvn -B verify
```

5. Run the runtime proof with the strict gate:

```bash
BASE_URL=http://localhost:8088 TENANT_ID=desenv ENVIRONMENT=local \
REQUIRE_CHANGE_SET_TIMELINE=true \
tools/local-e2e/run-domain-knowledge-change-set-local.sh
```

6. Merged a single quickstart PR with the evidence.
7. Redeployed the published quickstart runtime through the repository's Render
   integration.
8. Ran the hosted/published smoke once with
   `REQUIRE_CHANGE_SET_TIMELINE=true`.

Use GitHub Actions only for the phase-closing gate or publication. Do not use
remote workflows for local iteration that can be reproduced with the commands
above.

## Post-Bump Evidence

On 2026-05-01, after publishing and resolving
`praxis-config-starter:0.1.0-rc.37`, this quickstart was validated locally
against the published artifact:

- `mvn -B verify` passed after Maven downloaded
  `praxis-config-starter:0.1.0-rc.37` from Maven Central.
- The build reported `111` tests, `0` failures, `0` errors and `9` skipped.
- A local quickstart packaged from this source line was started on
  `http://localhost:8099` with managed PostgreSQL-backed persistence and Domain Knowledge
  projection enabled.
- `BASE_URL=http://localhost:8099 TENANT_ID=desenv ENVIRONMENT=local REQUIRE_CHANGE_SET_TIMELINE=true tools/local-e2e/run-domain-knowledge-change-set-local.sh`
  passed.
- The strict runtime smoke returned
  `domain-knowledge-change-set-timeline-ready` with `eventCount=4` for change
  set `6cf53cf2-b9eb-4ddd-a8d7-e20d345f601d`.
- The same smoke completed the governed lifecycle through creation,
  validation, approval, application, readback and safe timeline confirmation.

## Active Evidence Retrieval Evidence

On 2026-05-01, after the starter added active-evidence filtering for Project
Knowledge retrieval, this quickstart was validated locally against a locally
installed starter artifact, without publishing Maven/npm and without GitHub
Actions:

- `mvn -q -DskipTests install` passed in `praxis-config-starter`.
- `mvn -q clean package -DskipTests -Dpraxis.config.version=0.1.0-rc.5`
  passed in this quickstart.
- A local quickstart was started on `http://localhost:8099` with managed PostgreSQL-backed
  persistence and Domain Knowledge projection enabled.
- `BACKEND_URL=http://localhost:8099 TENANT_ID=desenv ENVIRONMENT=local REQUIRE_CHANGE_SET_TIMELINE=true REQUIRE_EVIDENCE_REVERT=true REQUIRE_PROJECT_KNOWLEDGE_RETRIEVAL=true AUTHORING_STREAM_MAX_TIME=180 scripts/verify-domain-knowledge-change-set-runtime.sh`
  passed.
- The smoke created change set `191e9f50-d840-4f44-ac9c-30e0a40014b4`, applied
  active evidence to Project Knowledge concept
  `page-builder.e2e.project-knowledge.identity-card` and observed authoring
  retrieval `phase=after-add`, `expected=present`, `retrievalCount=2`.
- The smoke then applied revert change set
  `1db47667-ce1c-462a-8e42-ce1806782038` and observed authoring retrieval
  `phase=after-revert`, `expected=absent`, `retrievalCount=0`.
- Final status was `domain-knowledge-change-set-runtime-ready` with
  `revertChecked=true` and `projectKnowledgeRetrievalChecked=true`.

## Published Host Evidence

On 2026-05-01, after the Render-hosted quickstart picked up `main` commit
`688aea1`, the published host was validated without a GitHub Actions smoke
rerun:

- `https://praxis-api-quickstart.onrender.com/actuator/health` returned
  `status=UP`.
- `https://praxis-api-quickstart.onrender.com/actuator/info` reported build
  time `2026-05-01T03:40:15.468Z`.
- The protected timeline route was present; a nonexistent change-set id returned
  a domain `422` instead of `404`, confirming the endpoint was deployed.
- `BACKEND_URL=https://praxis-api-quickstart.onrender.com TENANT_ID=desenv ENVIRONMENT=local REQUIRE_CHANGE_SET_TIMELINE=true scripts/verify-domain-knowledge-change-set-runtime.sh`
  passed.
- The hosted strict smoke returned
  `domain-knowledge-change-set-timeline-ready` with `eventCount=4` for change
  set `01300db8-119c-4925-9d5f-1049c31cf4cc`.
- The hosted lifecycle completed through creation, validation, approval,
  application, readback and safe timeline confirmation.

## Expected Strict Smoke Evidence

The strict runtime smoke must prove:

- Domain Catalog context is ready for `human-resources.funcionarios` and `cpf`;
- a governed Domain Knowledge change set is created with `add_evidence`;
- the persisted change set is revalidated as `valid`;
- the reviewer approves it through the governed status endpoint;
- application succeeds only through the separate apply boundary;
- readback confirms `status=applied`;
- the timeline returns at least four safe events;
- all timeline events have `visibility=safe`;
- the timeline includes `operationTypes=["add_evidence"]` and the target concept
  key for `human-resources.funcionarios.field.cpf`;
- the timeline does not leak raw evidence text, source pointers, source URIs,
  patch hashes, prompt content or chat history.

## Corpus Handoff Closure

The protected HTTP corpus handoff is complete.

- `praxisui-http-examples` commit `271b13e` added
  `domain-knowledge-change-set-timeline`.
- The example points at the hosted change set
  `01300db8-119c-4925-9d5f-1049c31cf4cc`.
- It is marked `protectedContract=true`, `publishedBackendConfirmed=true`,
  `runtimeRecordConfirmed=true`, `knownPublishedFailure=false` and
  `llmOperational=false`.
- `DOMAIN_KNOWLEDGE_TIMELINE_RUNBOOK.md` records the hosted proof and safety
  boundary.
- `LLM_SURFACE.md` was regenerated without promoting the protected endpoint into
  the unauthenticated operational LLM surface.
- Local corpus validation passed with `npm run verify:manifest` and
  `npm run smoke:corpus-promises`.
- Direct hosted response inspection returned `eventCount=4`, `unsafeCount=0`
  and `leakCount=0`.

Do not promote this protected endpoint to `llmOperational`. A future operational
LLM lane must be a separate safe runtime read, not the protected config timeline.
