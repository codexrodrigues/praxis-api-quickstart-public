<#
.SYNOPSIS
  Executes the authenticated QL-07 HTTP and no-mutation proof against a running Quickstart.

.DESCRIPTION
  Uses fictitious data, records only allowlisted evidence, compares all four operational
  ledger counts around ALLOW/DENY shadow calls, exercises the persisted lifecycle, and
  verifies the retained fictitious lifecycle fixture without weakening append-only audit retention.
#>
[CmdletBinding()]
param(
    [string] $BaseUrl = "http://127.0.0.1:18088",
    [string] $ProjectRoot = "",
    [string] $EnvFile = "",
    [string] $MavenRepository = "",
    [string] $EvidencePath = "",
    [string] $AllowedOrigin = "http://localhost:4200"
)

$ErrorActionPreference = "Stop"

if (-not $ProjectRoot) { $ProjectRoot = (Resolve-Path (Join-Path $PSScriptRoot "..\..")).Path }
if (-not $EnvFile) { $EnvFile = Join-Path $ProjectRoot ".env.dev" }
if (-not $MavenRepository) { $MavenRepository = Join-Path $ProjectRoot "target\ql07-public-m2" }
if (-not $EvidencePath) { $EvidencePath = Join-Path $ProjectRoot "target\ql07-http-evidence.json" }
$BaseUrl = $BaseUrl.TrimEnd('/')

function Import-DotEnv {
    param([string] $Path)
    if (-not (Test-Path -LiteralPath $Path)) { throw "Environment file was not found: $Path" }
    Get-Content -LiteralPath $Path -Encoding UTF8 | ForEach-Object {
        $line = $_.Trim()
        if (-not $line -or $line.StartsWith('#')) { return }
        $separator = $line.IndexOf('=')
        if ($separator -lt 1) { return }
        $name = $line.Substring(0, $separator).Trim()
        $value = $line.Substring($separator + 1).Trim()
        if ($value.Length -ge 2 -and $value.StartsWith('"') -and $value.EndsWith('"')) {
            $value = $value.Substring(1, $value.Length - 2)
        }
        if ($name -match '^[A-Za-z_][A-Za-z0-9_]*$' -and
                [string]::IsNullOrWhiteSpace([Environment]::GetEnvironmentVariable($name, 'Process'))) {
            Set-Item "Env:$name" $value
        }
    }
}

function Assert-True {
    param([bool] $Condition, [string] $Message)
    if (-not $Condition) { throw $Message }
}

function Get-Data {
    param($Json)
    if ($null -ne $Json.PSObject.Properties['data']) { return $Json.data }
    return $Json
}

function Get-HeaderValue {
    param($Headers, [string] $Name)
    if ($null -eq $Headers) { return $null }
    foreach ($key in $Headers.Keys) {
        if ([string]::Equals([string] $key, $Name, [StringComparison]::OrdinalIgnoreCase)) {
            $value = $Headers[$key]
            if ($value -is [System.Collections.IEnumerable] -and $value -isnot [string]) {
                return [string] ($value | Select-Object -First 1)
            }
            return [string] $value
        }
    }
    return $null
}

function Invoke-JsonRequest {
    param(
        [string] $Method,
        [string] $Path,
        [hashtable] $Headers = @{},
        $Body = $null,
        [Microsoft.PowerShell.Commands.WebRequestSession] $Session = $null
    )
    $parameters = @{
        Uri = "$BaseUrl$Path"
        Method = $Method
        Headers = $Headers
        UseBasicParsing = $true
        ErrorAction = 'Stop'
    }
    if ($Session) { $parameters.WebSession = $Session }
    if ($null -ne $Body) {
        $parameters.ContentType = 'application/json'
        $parameters.Body = ($Body | ConvertTo-Json -Depth 12 -Compress)
    }
    try {
        $response = Invoke-WebRequest @parameters
    } catch {
        $actualStatus = if ($null -ne $_.Exception.Response) { [int] $_.Exception.Response.StatusCode } else { $null }
        throw "HTTP request failed for $Method $Path with $(if ($null -eq $actualStatus) { 'no response' } else { "status $actualStatus" })."
    }
    $json = if ($response.Content) { $response.Content | ConvertFrom-Json } else { $null }
    [pscustomobject]@{ Status = [int] $response.StatusCode; Headers = $response.Headers; Raw = $response.Content; Json = $json }
}

function Assert-ExpectedStatus {
    param([string] $Method, [string] $Path, [int] $ExpectedStatus, $Body = $null)
    try {
        $parameters = @{ Uri = "$BaseUrl$Path"; Method = $Method; UseBasicParsing = $true; ErrorAction = 'Stop' }
        if ($null -ne $Body) {
            $parameters.ContentType = 'application/json'
            $parameters.Body = ($Body | ConvertTo-Json -Depth 12 -Compress)
        }
        $null = Invoke-WebRequest @parameters
        throw "Expected HTTP $ExpectedStatus for $Method $Path."
    } catch {
        $response = $_.Exception.Response
        if ($null -eq $response -or [int] $response.StatusCode -ne $ExpectedStatus) { throw }
    }
}

function Compile-LedgerProbe {
    $driver = Get-ChildItem -LiteralPath (Join-Path $MavenRepository 'org\postgresql\postgresql') -Filter 'postgresql-*.jar' -Recurse -File |
        Sort-Object FullName -Descending | Select-Object -First 1
    if (-not $driver) { throw "PostgreSQL driver was not found in the isolated Maven repository." }
    $classes = Join-Path $ProjectRoot 'target\ql07-ledger-probe'
    New-Item -ItemType Directory -Path $classes -Force | Out-Null
    & javac -cp $driver.FullName -d $classes (Join-Path $ProjectRoot 'scripts\RuleLabQl07LedgerProbe.java')
    if ($LASTEXITCODE -ne 0) { throw "Could not compile RuleLabQl07LedgerProbe." }
    return [pscustomobject]@{ Classes = $classes; Driver = $driver.FullName }
}

function Invoke-LedgerProbe {
    param($Probe, [string[]] $Arguments = @())
    $classPath = "$($Probe.Classes);$($Probe.Driver)"
    $output = & java -cp $classPath RuleLabQl07LedgerProbe @Arguments
    if ($LASTEXITCODE -ne 0) { throw "RuleLabQl07LedgerProbe failed." }
    return ($output | Select-Object -Last 1 | ConvertFrom-Json)
}

function Assert-CountsEqual {
    param($Expected, $Actual, [string] $Context)
    foreach ($field in @('requests', 'effects', 'executions', 'transitions')) {
        if ([long] $Expected.$field -ne [long] $Actual.$field) {
            throw "$Context changed ledger '$field': expected $($Expected.$field), got $($Actual.$field)."
        }
    }
}

Import-DotEnv $EnvFile
foreach ($required in @(
    'PRACTICE_TEMP_PASSWORD',
    'PRAXIS_RESOURCE_VERSION_ETAG_SECRET',
    'SPRING_DATASOURCE_URL',
    'SPRING_DATASOURCE_USERNAME',
    'SPRING_DATASOURCE_PASSWORD'
)) {
    if (-not (Test-Path "Env:$required") -or [string]::IsNullOrWhiteSpace((Get-Item "Env:$required").Value)) {
        throw "Required environment variable is missing: $required"
    }
}

$probe = Compile-LedgerProbe
$baseline = Invoke-LedgerProbe $probe
$runToken = "ql07-$([Guid]::NewGuid().ToString('N').Substring(0, 12))"
$fixtureReference = "QL07-$([Guid]::NewGuid().ToString('N').Substring(0, 16).ToUpperInvariant())"
$fixtureCreated = $false
$proofCompleted = $false

$payload = [ordered]@{
    requestReference = $fixtureReference
    reasonCode = 'FAMILY_HARDSHIP'
    eventDate = '2026-07-13'
    requestedAmount = 2500.00
    workerStatus = 'ACTIVE'
    duplicateGrant = $false
    programActive = $true
    programMaximumAmount = 5000.00
    customerAdditionalEligible = $true
    requestedPaymentDate = '2026-07-20'
    allowedPaymentDates = @('2026-07-20', '2026-08-05')
    availableBudgetAmount = 100000.00
    userTimeZone = 'America/Sao_Paulo'
}
$evaluatePayload = [ordered]@{
    requestReference = $fixtureReference
    reasonCode = 'FAMILY_HARDSHIP'
    eventDate = '2026-07-13'
    requestedAmount = 2500.00
    factReference = 'QL10-FICTIONAL-001'
    requestedPaymentDate = '2026-07-20'
    userTimeZone = 'America/Sao_Paulo'
}

try {
    Assert-ExpectedStatus -Method POST -Path '/api/human-resources/extraordinary-benefit-requests/actions/shadow-compare' -ExpectedStatus 403 -Body $payload

    $login = Invoke-WebRequest -Uri "$BaseUrl/auth/login" -Method POST -ContentType 'application/json' -Body (@{
        username = 'admin'; password = $env:PRACTICE_TEMP_PASSWORD
    } | ConvertTo-Json -Compress) -SessionVariable session -UseBasicParsing
    Assert-True ([int] $login.StatusCode -eq 204) 'Authenticated session was not established.'

    $commonHeaders = @{ Accept = 'application/json' }
    $configHeaders = @{ Accept = 'application/json'; Origin = $AllowedOrigin; 'X-Tenant-ID' = 'desenv'; 'X-Env' = 'local' }
    $health = Invoke-JsonRequest GET '/actuator/health' $commonHeaders $null $session
    $info = Invoke-JsonRequest GET '/actuator/info' $commonHeaders $null $session
    $actions = Invoke-JsonRequest GET '/schemas/actions?resource=human-resources.extraordinary-benefit-requests' $commonHeaders $null $session
    $requestSchema = Invoke-JsonRequest GET '/schemas/filtered?path=/api/human-resources/extraordinary-benefit-requests/actions/evaluate&operation=post&schemaType=request' $commonHeaders $null $session
    $shadowSchema = Invoke-JsonRequest GET '/schemas/filtered?path=/api/human-resources/extraordinary-benefit-requests/actions/shadow-compare&operation=post&schemaType=response' $commonHeaders $null $session
    $head = Invoke-JsonRequest GET '/api/praxis/config/domain-rules/snapshots/head?ruleSetKey=extraordinary-grant-eligibility' $configHeaders $null $session

    $actionData = Get-Data $actions.Json
    Assert-True ($null -ne ($actionData.actions | Where-Object id -eq 'evaluate')) 'Evaluate action was not discovered.'
    Assert-True ($null -ne ($actionData.actions | Where-Object id -eq 'shadow-compare')) 'Shadow action was not discovered.'
    $requestSchemaData = Get-Data $requestSchema.Json
    $shadowSchemaData = Get-Data $shadowSchema.Json
    Assert-True ($null -ne $requestSchemaData.properties.requestReference) 'Evaluate request schema is incomplete.'
    Assert-True ($null -ne $requestSchemaData.properties.factReference) 'Evaluate request schema does not expose the host fact reference.'
    Assert-True ($null -eq $requestSchemaData.properties.workerStatus) 'Evaluate request schema still accepts caller-owned worker status.'
    Assert-True ($null -ne $shadowSchemaData.properties.comparisonStatus) 'Shadow response schema is incomplete.'
    Assert-True ($null -eq $shadowSchemaData.properties.requestReference) 'Shadow response schema leaks request identity.'

    $shadowAllow = Invoke-JsonRequest POST '/api/human-resources/extraordinary-benefit-requests/actions/shadow-compare' $commonHeaders $payload $session
    $allow = Get-Data $shadowAllow.Json
    Assert-True ($allow.comparisonStatus -eq 'MATCH' -and $allow.baselineOutcome -eq 'ALLOW' -and $allow.candidateOutcome -eq 'ALLOW') 'ALLOW shadow comparison did not match.'
    Assert-True ($allow.sanitized -and -not $allow.persisted -and -not $allow.effectExecuted) 'ALLOW shadow observation is not safely sanitized.'
    Assert-True (-not $shadowAllow.Raw.Contains($fixtureReference)) 'ALLOW shadow response leaked the fixture reference.'

    $denyPayload = $payload | ConvertTo-Json -Depth 12 | ConvertFrom-Json
    $denyPayload.duplicateGrant = $true
    $shadowDeny = Invoke-JsonRequest POST '/api/human-resources/extraordinary-benefit-requests/actions/shadow-compare' $commonHeaders $denyPayload $session
    $deny = Get-Data $shadowDeny.Json
    Assert-True ($deny.comparisonStatus -eq 'MATCH' -and $deny.baselineOutcome -eq 'DENY' -and $deny.candidateOutcome -eq 'DENY') 'DENY shadow comparison did not match.'
    Assert-True (-not $shadowDeny.Raw.Contains($fixtureReference)) 'DENY shadow response leaked the fixture reference.'

    $afterShadow = Invoke-LedgerProbe $probe
    Assert-CountsEqual $baseline $afterShadow 'Shadow ALLOW/DENY proof'

    $evaluateHeaders = @{ Accept = 'application/json'; 'Idempotency-Key' = "$runToken-evaluate"; 'X-Correlation-ID' = "$runToken-evaluate" }
    $evaluation = Invoke-JsonRequest POST '/api/human-resources/extraordinary-benefit-requests/actions/evaluate' $evaluateHeaders $evaluatePayload $session
    $evaluationData = Get-Data $evaluation.Json
    Assert-True ($evaluationData.evaluation.outcome -eq 'ALLOW') 'Persisted evaluation was not ALLOW.'
    Assert-True ($evaluationData.resource.lifecycleStatus -eq 'EVALUATED') 'Persisted resource did not start in EVALUATED.'
    $resourceId = [long] $evaluationData.resource.id
    $fixtureCreated = $true

    $detail = Invoke-JsonRequest GET "/api/human-resources/extraordinary-benefit-requests/$resourceId" $commonHeaders $null $session
    $etag = Get-HeaderValue $detail.Headers 'ETag'
    Assert-True (-not [string]::IsNullOrWhiteSpace($etag)) 'Resource detail did not publish ETag.'
    $transition = @{ justification = 'QL-07 disposable corporate lifecycle proof.'; effectiveAt = '2026-07-13' }
    foreach ($action in @('submit', 'approve', 'apply')) {
        $headers = @{ Accept = 'application/json'; 'Idempotency-Key' = "$runToken-$action"; 'X-Correlation-ID' = "$runToken-$action"; 'If-Match' = $etag }
        $transitionResponse = Invoke-JsonRequest POST "/api/human-resources/extraordinary-benefit-requests/$resourceId/actions/$action" $headers $transition $session
        $etag = Get-HeaderValue $transitionResponse.Headers 'ETag'
        Assert-True (-not [string]::IsNullOrWhiteSpace($etag)) "Action $action did not publish the next ETag."
    }
    $finalDetail = Get-Data (Invoke-JsonRequest GET "/api/human-resources/extraordinary-benefit-requests/$resourceId" $commonHeaders $null $session).Json
    Assert-True ($finalDetail.lifecycleStatus -eq 'APPLIED' -and $finalDetail.effectStatus -eq 'EXECUTED') 'Lifecycle did not finish as APPLIED/EXECUTED.'

    $snapshot = Get-Data $head.Json
    Assert-True (-not [string]::IsNullOrWhiteSpace([string] $snapshot.snapshot.ruleSet.ref.ruleSetKey)) 'Active snapshot did not publish ruleSetKey.'
    $build = Get-Data $info.Json
    $evidence = [ordered]@{
        status = 'QL07_HTTP_VERIFIED'
        verifiedAtUtc = [DateTimeOffset]::UtcNow.ToString('O')
        authenticatedSession = $true
        unauthenticatedShadowStatus = 403
        discovery = [ordered]@{
            evaluate = $true
            shadowCompare = $true
            requestSchema = $true
            responseSchemaSanitized = $true
            requestSchemaEtag = Get-HeaderValue $requestSchema.Headers 'ETag'
            requestSchemaHash = Get-HeaderValue $requestSchema.Headers 'X-Schema-Hash'
        }
        host = [ordered]@{ health = $health.Json.status; build = $build.build }
        snapshot = [ordered]@{
            snapshotKey = $snapshot.snapshot.snapshotKey
            contentHash = $snapshot.snapshotContentHash
            activationRevision = $snapshot.activationRevision
            ruleSetKey = $snapshot.snapshot.ruleSet.ref.ruleSetKey
            ruleSetVersion = $snapshot.snapshot.ruleSet.ref.version
        }
        shadow = [ordered]@{
            allow = $allow.comparisonStatus
            deny = $deny.comparisonStatus
            persisted = $false
            effectExecuted = $false
            ledgerCountsInvariant = $true
        }
        lifecycle = [ordered]@{
            evaluated = $true
            submitted = $true
            approved = $true
            applied = $true
            effectExactlyOnce = $true
            retentionRequired = $true
        }
    }
    New-Item -ItemType Directory -Path (Split-Path -Parent $EvidencePath) -Force | Out-Null
    $evidence | ConvertTo-Json -Depth 8 | Set-Content -LiteralPath $EvidencePath -Encoding UTF8
    $evidence | ConvertTo-Json -Depth 8
    $proofCompleted = $true
} finally {
    if ($fixtureCreated) {
        $retainedFixture = Invoke-LedgerProbe $probe @('fixture', $fixtureReference, $runToken)
        if ($proofCompleted) {
            Assert-True ($retainedFixture.requests -eq 1) 'QL-07 retained fixture request count is invalid.'
            Assert-True ($retainedFixture.effects -eq 1) 'QL-07 retained fixture effect count is invalid.'
            Assert-True ($retainedFixture.executions -eq 4) 'QL-07 retained fixture action execution count is invalid.'
            Assert-True ($retainedFixture.transitions -eq 3) 'QL-07 retained fixture transition count is invalid.'
        }
    }
}
