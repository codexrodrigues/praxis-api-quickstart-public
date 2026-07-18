<#
.SYNOPSIS
  Provisions the governed QL-07 snapshot through the Config Starter HTTP lifecycle.

.DESCRIPTION
  Creates two scoped draft source definitions when absent, approves them with distinct
  allowlisted reviewers, and publishes the canonical host RuleSet with If-None-Match.
  It never writes directly to Config Starter tables and is idempotent after publication.
#>
[CmdletBinding()]
param(
    [string] $BaseUrl = "http://127.0.0.1:18088",
    [string] $ProjectRoot = "",
    [string] $EnvFile = "",
    [string] $MavenRepository = "",
    [string] $AllowedOrigin = "http://localhost:4200",
    [switch] $ForceSupersession
)

$ErrorActionPreference = "Stop"
if (-not $ProjectRoot) { $ProjectRoot = (Resolve-Path (Join-Path $PSScriptRoot "..\..")).Path }
if (-not $EnvFile) { $EnvFile = Join-Path $ProjectRoot ".env.dev" }
if (-not $MavenRepository) { $MavenRepository = Join-Path $ProjectRoot "target\ql07-public-m2" }
$BaseUrl = $BaseUrl.TrimEnd('/')

Get-Content -LiteralPath $EnvFile -Encoding UTF8 | ForEach-Object {
    $line = $_.Trim()
    if (-not $line -or $line.StartsWith('#')) { return }
    $separator = $line.IndexOf('=')
    if ($separator -lt 1) { return }
    $name = $line.Substring(0, $separator).Trim()
    $value = $line.Substring($separator + 1).Trim()
    if ($value.Length -ge 2 -and $value.StartsWith('"') -and $value.EndsWith('"')) { $value = $value.Substring(1, $value.Length - 2) }
    if ($name -match '^[A-Za-z_][A-Za-z0-9_]*$' -and
            [string]::IsNullOrWhiteSpace([Environment]::GetEnvironmentVariable($name, 'Process'))) {
        Set-Item "Env:$name" $value
    }
}
foreach ($required in @(
    'APP_AUTH_GOVERNANCE_APPROVER_A_USERNAME', 'APP_AUTH_GOVERNANCE_APPROVER_A_PASSWORD',
    'APP_AUTH_GOVERNANCE_APPROVER_B_USERNAME', 'APP_AUTH_GOVERNANCE_APPROVER_B_PASSWORD',
    'APP_AUTH_GOVERNANCE_PUBLISHER_USERNAME', 'APP_AUTH_GOVERNANCE_PUBLISHER_PASSWORD'
)) {
    if (-not (Test-Path "Env:$required") -or [string]::IsNullOrWhiteSpace((Get-Item "Env:$required").Value)) {
        throw "Required maker-checker identity variable is missing: $required"
    }
}

function New-AuthenticatedSession {
    param([string] $Username, [string] $Password)
    $null = Invoke-WebRequest -UseBasicParsing -Uri "$BaseUrl/auth/login" -Method POST -ContentType 'application/json' -Body (@{
        username = $Username; password = $Password
    } | ConvertTo-Json -Compress) -SessionVariable authenticatedSession
    return $authenticatedSession
}

$approverASession = New-AuthenticatedSession $env:APP_AUTH_GOVERNANCE_APPROVER_A_USERNAME $env:APP_AUTH_GOVERNANCE_APPROVER_A_PASSWORD
$approverBSession = New-AuthenticatedSession $env:APP_AUTH_GOVERNANCE_APPROVER_B_USERNAME $env:APP_AUTH_GOVERNANCE_APPROVER_B_PASSWORD
$publisherSession = New-AuthenticatedSession $env:APP_AUTH_GOVERNANCE_PUBLISHER_USERNAME $env:APP_AUTH_GOVERNANCE_PUBLISHER_PASSWORD
$headers = @{ Accept = 'application/json'; Origin = $AllowedOrigin; 'X-Tenant-ID' = 'desenv'; 'X-Env' = 'local' }

function Invoke-ConfigJson {
    param(
        [string] $Method,
        [string] $Path,
        $Session,
        $Body = $null,
        [hashtable] $AdditionalHeaders = @{}
    )
    $requestHeaders = @{} + $headers
    foreach ($entry in $AdditionalHeaders.GetEnumerator()) { $requestHeaders[$entry.Key] = $entry.Value }
    $parameters = @{ Uri = "$BaseUrl$Path"; Method = $Method; Headers = $requestHeaders; WebSession = $Session; UseBasicParsing = $true }
    if ($null -ne $Body) { $parameters.ContentType = 'application/json'; $parameters.Body = ($Body | ConvertTo-Json -Depth 12 -Compress) }
    try {
        $response = Invoke-WebRequest @parameters
    } catch {
        if ($null -ne $_.Exception.Response -and [int] $_.Exception.Response.StatusCode -eq 404) { throw }
        $responseBody = ''
        if ($null -ne $_.Exception.Response) {
            $reader = [IO.StreamReader]::new($_.Exception.Response.GetResponseStream())
            try { $responseBody = $reader.ReadToEnd() } finally { $reader.Dispose() }
        }
        throw "Config API failed for $Method $Path. $responseBody"
    }
    return [pscustomobject]@{ Json = ($response.Content | ConvertFrom-Json); Headers = $response.Headers; Status = [int] $response.StatusCode }
}

function Assert-ConfigForbidden {
    param(
        [string] $Path,
        $Session,
        $Body,
        [string] $Method = 'POST'
    )
    try {
        $null = Invoke-WebRequest -Uri "$BaseUrl$Path" -Method $Method -Headers $headers -WebSession $Session `
            -UseBasicParsing -ContentType 'application/json' -Body ($Body | ConvertTo-Json -Depth 12 -Compress)
        throw "Expected IAM role rejection for $Method $Path."
    } catch {
        $actualStatus = if ($null -ne $_.Exception.Response) { [int] $_.Exception.Response.StatusCode } else { $null }
        if ($actualStatus -ne 403) {
            throw "Expected IAM role rejection (403) for $Method $Path, but received $(if ($null -eq $actualStatus) { 'no HTTP response' } else { $actualStatus })."
        }
    }
}

function Ensure-ApprovedDefinition {
    param([string] $RuleKey, [string] $RuleType, [string] $Reviewer, $ReviewerSession, [string] $Summary)
    $encoded = [Uri]::EscapeDataString($RuleKey)
    $definitions = @((Invoke-ConfigJson GET "/api/praxis/config/domain-rules/definitions?ruleKey=$encoded" $publisherSession).Json)
    $existing = $definitions | Sort-Object version -Descending | Select-Object -First 1
    if (-not $existing -or $existing.createdByType -ne 'authenticated') {
        $nextVersion = if ($existing) { [int] $existing.version + 1 } else { 1 }
        $create = [ordered]@{
            ruleKey = $RuleKey
            version = $nextVersion
            ruleType = $RuleType
            status = 'draft'
            contextKey = 'workforce-benefits'
            resourceKey = 'human-resources.extraordinary-benefit-requests'
            serviceKey = 'praxis-api-quickstart'
            semanticOwner = 'workforce-benefits-owner'
            steward = 'rule-platform-steward'
            definition = @{ summary = $Summary; runtimeSurfacesAreDerived = $true; ql07FoundationFixture = $true }
            parameters = @{}
            governance = @{ requiredApprovals = @($Reviewer); authorizedApprovers = @($Reviewer); auditReason = 'QL-07 public artifact downstream proof.' }
        }
        $existing = (Invoke-ConfigJson POST '/api/praxis/config/domain-rules/definitions' $publisherSession $create).Json
    }
    if ($existing.status -notin @('approved', 'active')) {
        $approval = @{
            status = 'approved'
            validationResult = @{ valid = $true; approvalReason = 'Reviewed for the isolated QL-07 foundation snapshot.' }
        }
        Assert-ConfigForbidden "/api/praxis/config/domain-rules/definitions/$($existing.id)/status" $publisherSession $approval 'PATCH'
        $existing = (Invoke-ConfigJson PATCH "/api/praxis/config/domain-rules/definitions/$($existing.id)/status" $ReviewerSession $approval).Json
    }
    if ($existing.status -notin @('approved', 'active') -or -not $existing.approvedAt) {
        throw "Source definition is not approved: $RuleKey"
    }
    return $existing
}

try {
    $current = Invoke-ConfigJson GET '/api/praxis/config/domain-rules/snapshots/head/status?ruleSetKey=extraordinary-grant-eligibility' $publisherSession
    if (-not $current.Json.executionReady) {
        throw [InvalidOperationException]::new('Current RuleSet head requires governed supersession.')
    }
    if ($ForceSupersession) {
        throw [InvalidOperationException]::new('Governed supersession was explicitly requested.')
    }
    [ordered]@{
        status = 'ALREADY_PROVISIONED'
        snapshotKey = $current.Json.activeSnapshotKey
        ruleSetVersion = $current.Json.ruleSetVersion
        activationRevision = $current.Json.activationRevision
    } | ConvertTo-Json
    exit 0
} catch {
    $requiresSupersession = $_.Exception -is [InvalidOperationException]
    if (-not $requiresSupersession -and ($null -eq $_.Exception.Response -or [int] $_.Exception.Response.StatusCode -ne 404)) { throw }
}

$nextRuleSetVersion = if ($requiresSupersession) { [int] $current.Json.ruleSetVersion + 1 } else { 1 }
$headEtag = if ($requiresSupersession) { [string] $current.Json.headEtag } else { $null }

$eligibility = Ensure-ApprovedDefinition 'grant:eligibility' 'validation' $env:APP_AUTH_GOVERNANCE_APPROVER_A_USERNAME $approverASession 'Approved eligibility provenance for the extraordinary benefit RuleSet.'
$amount = Ensure-ApprovedDefinition 'grant:amount' 'calculation' $env:APP_AUTH_GOVERNANCE_APPROVER_B_USERNAME $approverBSession 'Approved amount-calculation provenance for the extraordinary benefit RuleSet.'

$classPathFile = Join-Path $ProjectRoot 'target\ql07-runtime-classpath.txt'
Push-Location $ProjectRoot
try {
    & mvn -B -s (Join-Path $ProjectRoot '.mvn\ql07-central-only-settings.xml') "-Dmaven.repo.local=$MavenRepository" "-Dmdep.outputFile=$classPathFile" dependency:build-classpath
    if ($LASTEXITCODE -ne 0) { throw 'Could not build the QL-07 runtime classpath.' }
} finally { Pop-Location }
$classes = Join-Path $ProjectRoot 'target\ql07-snapshot-payload'
New-Item -ItemType Directory -Path $classes -Force | Out-Null
$runtimeClassPath = (Get-Content -Raw $classPathFile).Trim()
$compileClassPath = "$(Join-Path $ProjectRoot 'target\classes');$runtimeClassPath"
$argFileCompileClassPath = $compileClassPath.Replace('\', '/')
$argFileClasses = $classes.Replace('\', '/')
$argFileSource = (Join-Path $ProjectRoot 'scripts\RuleLabQl07SnapshotPayload.java').Replace('\', '/')
$javacArguments = Join-Path $ProjectRoot 'target\ql07-snapshot-javac.args'
@(
    '-cp'
    ('"{0}"' -f $argFileCompileClassPath)
    '-d'
    ('"{0}"' -f $argFileClasses)
    ('"{0}"' -f $argFileSource)
) | Set-Content -LiteralPath $javacArguments -Encoding ASCII
& javac "@$javacArguments"
if ($LASTEXITCODE -ne 0) { throw 'Could not compile RuleLabQl07SnapshotPayload.' }
$javaArguments = Join-Path $ProjectRoot 'target\ql07-snapshot-java.args'
@(
    '-cp'
    ('"{0}"' -f "$argFileClasses;$argFileCompileClassPath")
    'com.example.praxis.apiquickstart.rulelab.RuleLabQl07SnapshotPayload'
    [string] $eligibility.id
    [string] $amount.id
    [string] $nextRuleSetVersion
) | Set-Content -LiteralPath $javaArguments -Encoding ASCII
$payloadJson = & java "@$javaArguments"
if ($LASTEXITCODE -ne 0) { throw 'Could not generate the canonical snapshot publication payload.' }
$payload = $payloadJson | Select-Object -Last 1 | ConvertFrom-Json
$manifest = Invoke-ConfigJson POST '/api/praxis/config/domain-rules/snapshots/composition-manifest' $publisherSession $payload
Assert-ConfigForbidden '/api/praxis/config/domain-rules/snapshots/composition-approvals' $publisherSession $payload
Assert-ConfigForbidden '/api/praxis/config/domain-rules/snapshots' $approverASession $payload
$approvalA = Invoke-ConfigJson POST '/api/praxis/config/domain-rules/snapshots/composition-approvals' $approverASession $payload
$approvalB = Invoke-ConfigJson POST '/api/praxis/config/domain-rules/snapshots/composition-approvals' $approverBSession $payload
if ($approvalA.Json.actorRef -eq $approvalB.Json.actorRef) {
    throw 'Maker-checker proof resolved both approvals to the same authenticated actor.'
}
if ($approvalA.Json.evidenceHash -ne $manifest.Json.compositionDigest -or
    $approvalB.Json.evidenceHash -ne $manifest.Json.compositionDigest) {
    throw 'Persisted approvals do not bind the exact canonical composition digest.'
}
$payload | Add-Member -NotePropertyName compositionDigest -NotePropertyValue $manifest.Json.compositionDigest
$quotedHeadEtag = if ($headEtag -and $headEtag.StartsWith('"')) { $headEtag } elseif ($headEtag) { '"' + $headEtag + '"' } else { $null }
$precondition = if ($quotedHeadEtag) { @{ 'If-Match' = $quotedHeadEtag } } else { @{ 'If-None-Match' = '*' } }
$published = Invoke-ConfigJson POST '/api/praxis/config/domain-rules/snapshots' $publisherSession $payload $precondition

[ordered]@{
    status = 'PROVISIONED'
    sourceDefinitions = 2
    distinctApprovers = 2
    ruleSetVersion = $nextRuleSetVersion
    compositionDigest = $manifest.Json.compositionDigest
    snapshotKey = $published.Json.snapshot.snapshotKey
    snapshotContentHash = $published.Json.snapshotContentHash
    activationRevision = $published.Json.activationRevision
} | ConvertTo-Json
