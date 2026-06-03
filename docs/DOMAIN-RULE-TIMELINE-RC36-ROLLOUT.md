# Domain Rule Timeline rc.36 Rollout

Status: `praxis-config-starter:0.1.0-rc.36` was published and resolved from Maven Central on 2026-04-29; this quickstart now consumes it as the operational reference host.

This quickstart is the operational reference host for Praxis semantic decisions authored by AI. It must prove the canonical `/api/praxis/config/domain-rules/**` behavior published by `praxis-config-starter`, but it must not redefine that behavior locally.

## Current State

- `pom.xml` now consumes `praxis.config.version=0.1.0-rc.36`.
- Timeline v1 and v2 are included in the published `praxis-config-starter:0.1.0-rc.36` artifact.
- Local quickstart + Neon proof has already passed against the unreleased starter with:
  - `BACKEND_URL=http://localhost:8088 REQUIRE_TIMELINE=true scripts/verify-domain-rules-runtime.sh`
- That proof returned:
  - `eventCount=10` for the governed `form_config` path, including `intake.received`, `simulation.requested`, `simulation.completed`, approval and materialization events.
  - `eventCount=9` for the published `option_source` path, including publication and materialization events.
- On 2026-04-28, the local-first RC proof was re-run against Neon with strict gates:
  - `REQUIRE_SIMULATION=true`
  - `REQUIRE_PUBLICATION=true`
  - `REQUIRE_BACKEND_VALIDATION=true`
  - `REQUIRE_WORKFLOW_ACTION=true`
  - `REQUIRE_APPROVAL_POLICY=true`
  - `REQUIRE_TIMELINE=true`
- That strict proof passed with authenticated runtime probes:
  - `backend_validation` rejected `procurement.purchase-orders` with `409 Conflict`.
  - `workflow_action` rejected `human-resources.folhas-pagamento:mark-paid` with `409 Conflict`.
  - `approval_policy` rejected `human-resources.eventos-folha:bulk-approve` with `409 Conflict`.
  - governed timelines returned events for `form_config`, `option_source`, `backend_validation`, `workflow_action` and `approval_policy`.
- The same local validation pass also proved the LLM authoring context path with OpenAI and `REQUIRE_GOVERNED_CONTEXT=true`, returning `resolutionStatus=resolved` and `policyProfile=compliance_review`.
- `scripts/verify-domain-federation-runtime.sh` was corrected to verify active persisted release artifacts by canonical `contextKey` and `relationshipType`, without text-query filters that hid valid relationships/contracts.
- The corrected federation smoke passed locally against Neon, including candidate release, audit read, persisted validation report, activation and active context reads for operations and HR.
- On 2026-04-28, a local browser cockpit E2E against quickstart `rc.35` proved create, simulate, approve and activate through the governed buttons, but also exposed two post-`rc.35` release blockers that must be included in the same `rc.36` starter cut:
  - the timeline endpoint is unavailable in the published `rc.35` dependency, so the cockpit must treat timeline as derived observability until the quickstart consumes `rc.36`;
  - shared-rule authoring prompts with an explicit `/api/{context}/{resource}` path must preserve that resource path during intent resolution, instead of being re-grounded by looser domain words in the prompt.
- After `praxis-config-starter` PR #132, `praxis-ui-angular` PR #87 and this quickstart PR #42 were merged on 2026-04-29, integrated `main` passed the local-first rc.36 gates without publication or Actions:
  - `praxis-config-starter` focal authoring/contract and timeline tests passed.
  - `DomainAuthoringContextHintsContractTest` passed in this quickstart.
  - `praxis-ui-angular` page-builder focal spec passed with `59 SUCCESS`, and `ng build praxis-page-builder` passed.
  - `scripts/workspace/check-v0-readiness.sh` passed with all tracked repos on clean `main`, no open PRs and ports `4003/8088` free.
  - `scripts/workspace/run-local-readiness-lane.sh domain-rules-timeline-runtime` passed against quickstart packaged with local `praxis-config-starter:0.1.0-rc.5`, proving timeline `eventCount=10` for `form_config`, `eventCount=9` for `option_source`, publication runtime lookup enforcement and Neon-backed persistence.
  - `scripts/workspace/run-local-readiness-lane.sh shared-rule-timeline-cockpit` passed, proving the governed timeline rendered in the Angular cockpit with managed local services.
- No Maven/npm publication and no GitHub Actions were used during implementation. The single phase-close remote gate later passed, then the single Maven Central release published `0.1.0-rc.36`.

## Published Artifact

Maven Central resolution has succeeded for:

```text
io.github.codexrodrigues:praxis-config-starter:0.1.0-rc.36
```

Keep rollout-compatible `auto` gates until the deployed quickstart host proves the endpoint after redeploy.

Do not promote the HTTP corpus timeline example before the deployed quickstart returns `200` for:

```text
GET /api/praxis/config/domain-rules/definitions/{definitionId}/timeline
```

## Completed Publication Path

Completed for the starter release:

1. `praxis-config-starter` confirmed no existing `v0.1.0-rc.36` tag.
2. The manual Maven release workflow created tag `v0.1.0-rc.36`.
3. The tag workflow published `praxis-config-starter:0.1.0-rc.36` to Maven Central.
4. Maven Central dependency resolution succeeded locally for `0.1.0-rc.36`.
5. This quickstart now updates:
   - `pom.xml`: `praxis.config.version=0.1.0-rc.36`
   - README references from `0.1.0-rc.35` to `0.1.0-rc.36`
6. Validate locally before opening the quickstart PR:
   - `mvn -B verify`
   - local or hosted smoke with `REQUIRE_TIMELINE=true`
   - browser cockpit handoff with an explicit resource-path prompt, for example `/api/helpdesk/chamados`, proving the resolved context remains `helpdesk.chamados`
7. Merge one quickstart PR with the validation evidence.
8. Redeploy the published quickstart runtime.
9. Run the manual `Domain Rules Runtime Smoke` once with `require_timeline=true`.
10. Promote the HTTP corpus timeline example only after the published smoke passes.

## Quickstart Validation After Version Bump

Use the smallest reliable validation first:

```bash
mvn -B verify
```

Then prove runtime behavior against the target host:

```bash
BACKEND_URL=http://localhost:8088 REQUIRE_TIMELINE=true scripts/verify-domain-rules-runtime.sh
```

For the published host, use the manual workflow only as a phase gate, not as an iteration loop.

Expected governed timeline evidence:

- `form_config` path returns `eventCount=10`.
- `option_source` path returns `eventCount=9`.
- All timeline events have `visibility=safe`.
- The response does not expose prompt, assistant message, condition, parameters or materialized payload.

Expected explicit resource-path evidence:

- A shared-rule authoring prompt that names `/api/helpdesk/chamados` resolves to `contextKey=helpdesk` and `resourceKey=chamados`.
- The same prompt must not be redirected to `human-resources.funcionarios` only because it mentions employees, HR, people or other domain vocabulary as business context.
- The browser cockpit can continue from the shared rules handoff without requiring timeline availability as a blocking source of truth.

## Post-Bump Evidence

On 2026-04-29, after publishing and resolving `praxis-config-starter:0.1.0-rc.36`, this quickstart was validated locally against the published artifact:

- `./mvnw -B verify` passed with `111` tests, `0` failures, `0` errors and `9` skipped.
- `BACKEND_URL=http://localhost:8088 TENANT_ID=desenv ENVIRONMENT=local scripts/workspace/run-local-readiness-lane.sh domain-rules-timeline-runtime` passed against the canonical local quickstart starter, Neon-backed persistence and `REQUIRE_TIMELINE=true`.
- The runtime lane returned governed timeline evidence with `eventCount=10` for the `form_config` path and `eventCount=9` for the `option_source` path.
- The same runtime lane proved the publication path, option-source lookup enforcement and safe timeline publication behavior without redefining the canonical rule semantics in the quickstart.

## Corpus Handoff

After the published quickstart smoke passes, update `praxisui-http-examples`:

- remove the timeline example's `knownPublishedFailure=true`;
- set `publishedBackendConfirmed=true` only with the published proof evidence;
- keep write examples out of `llmOperational`;
- regenerate `LLM_SURFACE.md` only if an example becomes `llmOperational`;
- run the corpus focal validations documented in `DOMAIN_RULES_PUBLICATION_RUNBOOK.md`.
