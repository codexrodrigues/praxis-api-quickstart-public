# Project Knowledge Vector RAG Rollout

Status: locally proven against `praxis-config-starter/main`, not yet published
as a Maven Central release.

This quickstart is the operational reference host for Praxis semantic decisions
authored by AI. It proves the canonical Project Knowledge Vector RAG behavior
published by `praxis-config-starter`, but it must not redefine that behavior
locally.

## Current State

- `praxis-config-starter/main` contains opt-in Project Knowledge vector index
  publication and opt-in vector-ranked candidate retrieval.
- `praxis-api-quickstart/main` contains the strict runtime smoke gate
  `REQUIRE_PROJECT_KNOWLEDGE_VECTOR_RETRIEVAL=true`.
- The vector path has been proved locally with managed PostgreSQL-backed persistence and
  `PgVectorStore`.
- No Maven Central publication, npm publication or GitHub Actions run has been
  used for this checkpoint.

## Feature Flags Under Rollout

The vector path is disabled by default and must remain opt-in during beta:

```bash
PRAXIS_AI_RAG_VECTOR_STORE_ENABLED=true
PRAXIS_PROJECT_KNOWLEDGE_RAG_PUBLICATION_ENABLED=true
PRAXIS_PROJECT_KNOWLEDGE_RAG_RETRIEVAL_ENABLED=true
```

The rollout proof intentionally disables Domain Catalog RAG publication so the
smoke isolates Project Knowledge behavior:

```bash
PRAXIS_DOMAIN_CATALOG_RAG_PUBLICATION_ENABLED=false
```

## Local Proof Commands

Install the unreleased starter locally:

```bash
cd /Users/rodrigo/Dev/pessoal/praxis-plataform/praxis-config-starter
mvn -q -DskipTests install
```

Package this quickstart against the local starter:

```bash
cd /Users/rodrigo/Dev/pessoal/praxis-plataform/praxis-api-quickstart
mvn -q clean package -DskipTests -Dpraxis.config.version=0.1.0-rc.5
```

Start the local quickstart through the canonical starter wrapper:

```bash
cd /Users/rodrigo/Dev/pessoal/praxis-plataform/praxis-config-starter

PORT=8099 \
PROVIDER=openai \
STREAM_TIMEOUT_SECONDS=180 \
PRAXIS_AI_RAG_VECTOR_STORE_ENABLED=true \
PRAXIS_DOMAIN_CATALOG_RAG_PUBLICATION_ENABLED=false \
PRAXIS_PROJECT_KNOWLEDGE_RAG_PUBLICATION_ENABLED=true \
PRAXIS_PROJECT_KNOWLEDGE_RAG_RETRIEVAL_ENABLED=true \
tools/local-e2e/start-quickstart-local-e2e.sh
```

Run the strict vector revert proof:

```bash
cd /Users/rodrigo/Dev/pessoal/praxis-plataform/praxis-api-quickstart

BACKEND_URL=http://localhost:8099 \
TENANT_ID=desenv \
ENVIRONMENT=local \
REQUIRE_CHANGE_SET_TIMELINE=true \
REQUIRE_PROJECT_KNOWLEDGE_VECTOR_RETRIEVAL=true \
AUTHORING_STREAM_MAX_TIME=180 \
scripts/verify-domain-knowledge-change-set-runtime.sh
```

Run the strict vector supersession proof:

```bash
cd /Users/rodrigo/Dev/pessoal/praxis-plataform/praxis-api-quickstart

BACKEND_URL=http://localhost:8099 \
TENANT_ID=desenv \
ENVIRONMENT=local \
REQUIRE_CHANGE_SET_TIMELINE=true \
REQUIRE_PROJECT_KNOWLEDGE_VECTOR_RETRIEVAL=true \
REQUIRE_EVIDENCE_SUPERSESSION=true \
AUTHORING_STREAM_MAX_TIME=180 \
scripts/verify-domain-knowledge-change-set-runtime.sh
```

## Evidence Already Collected

On 2026-05-02, the local vector-enabled proof passed:

- `praxis-config-starter` focal tests for vector retrieval, Project Knowledge
  authoring retrieval, derived index publication and metadata passed locally.
- `praxis-config-starter` installed into the local Maven repository.
- this quickstart packaged against the local starter without Maven Central.
- a local quickstart started on `http://localhost:8099` with managed PostgreSQL-backed
  persistence and `PgVectorStore` initialized.
- revert proof confirmed vector document count `1` after `add_evidence` and
  `0` after revert.
- revert proof confirmed authoring retrieval present after add and absent after
  revert.
- supersession proof confirmed the original evidence vector document is removed
  and the replacement evidence vector document remains active.
- supersession proof confirmed authoring retrieval remains present because the
  replacement evidence is still active.

## Expected Strict Smoke Evidence

The strict vector runtime smoke must prove:

- Domain Catalog context is ready for `human-resources.funcionarios` and `cpf`;
- a governed Domain Knowledge change set applies `add_evidence`;
- Project Knowledge vector publication writes exactly one active vector document
  for the target evidence;
- agentic authoring can retrieve the active Project Knowledge candidate through
  the vector-ranked path;
- revert removes active authoring influence and active vector document count;
- supersession removes the original vector document while retaining the
  replacement vector document;
- lifecycle and timeline projections remain safe and do not leak raw evidence
  payloads, source pointers, source URIs, patch hashes, prompts, transcripts or
  chat history.

## Release Gate Decision

Do not publish a new `praxis-config-starter` Maven Central version just because
this local checkpoint passed.

Publication becomes appropriate only if all are true:

- the owner explicitly authorizes a named release cut;
- a downstream consumer needs the vector path from a public Maven coordinate;
- local focal starter tests pass freshly;
- this quickstart passes both strict vector runtime smokes freshly;
- the release uses one intentional remote gate instead of repeated Actions.

Until then, keep this as source-level beta evidence.

## Published Host Decision

Do not run a hosted smoke or redeploy solely for this checkpoint.

Hosted validation is appropriate only after a published Maven coordinate is
consumed by this quickstart and the team intentionally closes the phase.

## Known Runtime Lesson

The first vector-enabled smoke exposed a canonical reload issue in the starter:
vector candidates were reloaded without eagerly fetching `sourceRelease`, which
could fail projection building outside an active persistence session.

The fix belongs in `praxis-config-starter`, not in this quickstart. This
quickstart only records and proves the downstream behavior.

## Next Step

Keep the vector path opt-in and local-first until a named release decision.

When the owner authorizes the cut, bump the starter version intentionally,
publish once, update this quickstart to consume that Maven coordinate, then run
one local strict vector proof and one phase-closing remote or hosted gate.
