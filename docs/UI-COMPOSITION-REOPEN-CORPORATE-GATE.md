# UiCompositionPlan reopen corporate gate

## Objective

This gate proves that the Quick Start host preserves the canonical `UiCompositionPlan` after the
page is applied, closed, read again, semantically refined and applied a second time. It targets the
failure mode where a rich composition was previously degraded to an incidental component patch
after reopen.

The canonical contract belongs to `praxis-config-starter`. Quick Start is only the operational host
that proves the contract through its public HTTP boundary. Angular owns the component authoring
manifest consumed by the refinement.

## Adherence classification and impact

Before this gate, canonical composition authoring and compilation were already supported, but
continuity after persistence was only partially supported: the materialized page survived while
the canonical authoring source needed for a governed refinement did not have a durable,
server-attested path. The gap was therefore classified as `suportado-parcialmente`, not as a reason
to create a parallel Quick Start contract.

`praxis-config-starter` remains the canonical owner and release `0.1.0-rc.148` closes the persistence
gap. This Quick Start change is a transversal downstream certification: it updates the consumed
release and proves the behavior without adding an endpoint, DTO or alternate composition model.
The directly affected consumer is the Angular authoring/reopen flow; public documentation and the
five archetype browser laboratories remain downstream validation surfaces. There is no intended
breaking change in the Quick Start API.

## Deterministic proof

`UiCompositionReopenPostgresHttpIntegrationTest` starts the Quick Start application on a random
HTTP port and a real embedded PostgreSQL instance for the Config persistence unit. The test:

1. creates a master-detail-plus-chart composition through `page-preview`;
2. records the terminal authoring result and applies it through `page-apply`;
3. reads the stored page through `/api/praxis/config/ui` and verifies its ETag and server-attested
   `authoringSource`;
4. sends a chart refinement after reopen while deliberately forging the browser-provided source;
5. proves that the server uses the persisted source and preserves state, canvas and bindings;
6. proves that another user cannot refine the persisted composition;
7. applies the winning refinement with `If-Match` and verifies version 2;
8. proves that replaying the old ETag returns `412 Precondition Failed` and cannot replace the
   winning page.

The final read also verifies that both projections agree: the runtime payload contains the refined
chart, the persisted semantic source contains the same refinement, and the original layout remains
intact. Runtime diagnostics are not persisted as canonical authoring input.

The Config database fixture creates the pre-V61 schema and then executes the canonical
`V61__add_ui_user_config_authoring_source.sql` migration from the consumed Config artifact. This
keeps the migration under test instead of reproducing its new column locally.

## Real boundaries and controlled seams

The proof uses the real Quick Start HTTP/security filters, Config controllers, repositories,
PostgreSQL JSON persistence, semantic component-edit orchestration, manifest validators/effect
compiler, UiCompositionPlan compiler, apply lineage checks and optimistic concurrency.

No external LLM is invoked. The initial rich plan and the two bounded provider responses
(operation selection and `chart.type.set` parameters) are deterministic test inputs. The registry
lookup extracts the real `praxis-chart` authoring manifest from the consumed Config release's
`ai-registry/registry-snapshot.json`, and the real Config manifest compiler validates and
materializes it. That release snapshot is the governed derived artifact generated from the Angular
component registry; the test never reads an unverified local `dist`. API Catalog indexing is outside
this persistence scenario and is disabled through a mock of its coordinator.

## Commands

Run the focused gate during development:

```bash
mvn -B -Dtest=UiCompositionReopenPostgresHttpIntegrationTest test
```

Before promoting a Quick Start cut, run it together with the existing hosted authoring contracts:

```bash
mvn -B \
  -Dtest=UiCompositionReopenPostgresHttpIntegrationTest,AgenticAuthoringStreamIsolatedIntegrationTest,AiPatchSchemaResolutionIsolatedIntegrationTest,GovernedUiCompositionTemplateReferenceQuickstartIntegrationTest,SecurityConfigAiPatchPolicyTest \
  test
```

Then run the repository's normal `mvn -B verify` gate.

## Certification boundary

This is a robust backend continuity gate for one representative rich composition refinement. It
does not certify the five Angular archetypes, browser interaction, visual quality, accessibility,
responsive behavior, real Quick Start resource data or an external LLM canary. Those remain
separate downstream gates. OpenAI is a final non-deterministic canary and must not block this proof.
