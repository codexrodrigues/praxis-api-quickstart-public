# HR absence analytics pilot

This pilot materializes `human-resources.vw-analytics-afastamentos` as a read-only operational proof for issue #98.

The projection is intentionally aggregate-safe:

- It uses `funcionario_lotacoes_departamento` as the only department source for analytics attribution.
- It does not fallback to `funcionarios.departamento_id`.
- It does not expose absence type, notes, employee name, CPF, email or phone.
- It keeps `funcionarioId` only for authorized employee-360 drill-down.
- It marks `funcionarioId` and the nominal employee filter with privacy governance because pseudonymous identifiers still require nominal access.
- It publishes `criticalityPolicyId` and `criticalityPolicyVersion` with every row.
- Its exact comparison schema publishes `x-ui.analytics.projections[].governance.policyRefs[]` with the canonical policy identity, version, result field and row-attestation fields.
- Its primary dimension publishes `keyFilterField=departamentoIdsIn`, so consumers apply
  `bucket.key` without deriving a field name or filtering by the department label.
- Its `recordOpen` publishes `sourceIdentityField=funcionarioId` and the target
  `human-resources.funcionarios/hero-profile`; the surface catalog remains the owner of path,
  schema, scope and availability.

The operational view materializes one row for each `funcionarioId × departamentoId × competencia`.
It expands source events to dates, attributes each date by the half-open effective assignment and
deduplicates overlapping dates before aggregation. A department change therefore creates distinct
monthly rows, while two overlapping events on the same date count once.

`periodoInicio` and `periodoFim` are the first and last covered dates of the union; they do not
claim that every day in the envelope was absent. The comparison default is the current calendar
month against the preceding calendar month in `America/Sao_Paulo`.

This lets `POST /api/human-resources/vw-analytics-afastamentos/stats/comparison` compare departments with:

- `DISTINCT_COUNT(funcionarioId)`
- `SUM(diasAfastado)`

Criticality policy `hr-absence-criticality-v1` / `2026-07-15`:

- `STANDARD`: fewer than 7 days in the analytic slice.
- `ATTENTION`: 7 to 14 days.
- `CRITICAL`: 15 or more days.

The PostgreSQL function `hr_absence_criticality_level(bigint)` is the sole executable threshold
owner. The Java constants attest identity in discovery metadata, while the golden lab and the
PostgreSQL proof detect divergence from the materialized result.

Rows without an effective department assignment are intentionally absent from the view. The golden
lab records this as `DEPARTMENT_ASSIGNMENT_MISSING`; it is a data quality failure, not permission
to infer the current department from the employee record.

## Corporate access boundary

`POST /stats/comparison` requires `HR_ANALYTICS_AGGREGATE_READ`. The nominal analytics resource
requires `HR_ANALYTICS_NOMINAL_READ`, and `GET /funcionarios/{id}/hero-profile` requires
`HR_EMPLOYEE_360_READ`. The demo login grants all three to `ROLE_ADMIN`.

An aggregate-only comparison may filter by period, department and criticality, but it cannot send
`funcionarioIdsIn`: the host rejects that nominal filter unless the principal also holds
`HR_ANALYTICS_NOMINAL_READ`. `HEAD` follows the same authority policy as `GET` for nominal rows
and employee-360 profiles. Group-by, time-series and distribution statistics receive the same
server-side department intersection as filter, cursor, locate, options and export requests. A
department-restricted principal that asks exclusively for an outside department receives `403`;
the host never turns that unauthorized request into an unscoped aggregate.

Department scope is resolved server-side by `HrDepartmentScopeProvider`, never from a request
header or client claim. The default `demo-manager=1` adapter exists only to make the quickstart
deterministic; a corporate deployment replaces the bean with an IAM entitlement source. A scoped
principal receives the intersection of requested and allowed departments. Exports and options use
the same rule, and direct profile-view access is denied to scoped principals.

For nominal `POST /filter`, the client-owned functional filter is not the authorization boundary.
The host resolves a canonical `ResourceFilterAccessScope` from the authenticated principal, and the
Metadata Starter applies it both to the regular page and to `includeIds` rehydration. An authorized
selection outside the current functional filter can therefore remain visible without allowing a
client-supplied ID from another department to cross the row-access boundary.

`HrAnalyticsResourceOperationAvailabilityProvider` exposes the same split through
`capabilities.operations`: aggregate-only principals can discover `statsComparison` as allowed and
nominal operations as `missing-authority`; nominal-only principals receive the inverse decision.
This metadata does not replace endpoint enforcement.

An aggregate comparison bucket intentionally has no `funcionarioId`, so it cannot open an ITEM
surface directly. The executable sequence is comparison bucket -> `departamentoIdsIn` cross-filter
-> nominal row -> `funcionarioId` -> catalog-resolved `hero-profile` surface.
