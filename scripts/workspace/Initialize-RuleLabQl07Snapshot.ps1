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
    [string] $AllowedOrigin = "http://localhost:4200"
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
    if ($name -match '^[A-Za-z_][A-Za-z0-9_]*$') { Set-Item "Env:$name" $value }
}
if ([string]::IsNullOrWhiteSpace($env:PRACTICE_TEMP_PASSWORD)) { throw 'PRACTICE_TEMP_PASSWORD is required.' }

$null = Invoke-WebRequest -UseBasicParsing -Uri "$BaseUrl/auth/login" -Method POST -ContentType 'application/json' -Body (@{
    username = 'admin'; password = $env:PRACTICE_TEMP_PASSWORD
} | ConvertTo-Json -Compress) -SessionVariable session
$headers = @{ Accept = 'application/json'; Origin = $AllowedOrigin; 'X-Tenant-ID' = 'desenv'; 'X-Env' = 'local' }

function Invoke-ConfigJson {
    param([string] $Method, [string] $Path, $Body = $null, [hashtable] $AdditionalHeaders = @{})
    $requestHeaders = @{} + $headers
    foreach ($entry in $AdditionalHeaders.GetEnumerator()) { $requestHeaders[$entry.Key] = $entry.Value }
    $parameters = @{ Uri = "$BaseUrl$Path"; Method = $Method; Headers = $requestHeaders; WebSession = $session; UseBasicParsing = $true }
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

function Ensure-ApprovedDefinition {
    param([string] $RuleKey, [string] $RuleType, [string] $Reviewer, [string] $Summary)
    $encoded = [Uri]::EscapeDataString($RuleKey)
    $existing = (Invoke-ConfigJson GET "/api/praxis/config/domain-rules/definitions?ruleKey=$encoded").Json |
        Where-Object { $_.version -eq 1 } | Select-Object -First 1
    if (-not $existing) {
        $create = [ordered]@{
            ruleKey = $RuleKey
            version = 1
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
            createdByType = 'system'
            createdBy = 'ql07-foundation-provisioner'
        }
        $existing = (Invoke-ConfigJson POST '/api/praxis/config/domain-rules/definitions' $create).Json
    }
    if ($existing.status -notin @('approved', 'active')) {
        $approval = @{
            status = 'approved'
            decidedByType = 'human'
            decidedBy = $Reviewer
            validationResult = @{ valid = $true; approvalReason = 'Reviewed for the isolated QL-07 foundation snapshot.' }
        }
        $existing = (Invoke-ConfigJson PATCH "/api/praxis/config/domain-rules/definitions/$($existing.id)/status" $approval).Json
    }
    if ($existing.status -notin @('approved', 'active') -or -not $existing.approvedAt) {
        throw "Source definition is not approved: $RuleKey"
    }
    return $existing
}

try {
    $current = Invoke-ConfigJson GET '/api/praxis/config/domain-rules/snapshots/head?ruleSetKey=extraordinary-grant-eligibility'
    [ordered]@{
        status = 'ALREADY_PROVISIONED'
        snapshotKey = $current.Json.snapshot.snapshotKey
        snapshotContentHash = $current.Json.snapshotContentHash
        activationRevision = $current.Json.activationRevision
    } | ConvertTo-Json
    exit 0
} catch {
    if ($null -eq $_.Exception.Response -or [int] $_.Exception.Response.StatusCode -ne 404) { throw }
}

$eligibility = Ensure-ApprovedDefinition 'grant:eligibility' 'validation' 'ql07-reviewer-a' 'Approved eligibility provenance for the extraordinary benefit RuleSet.'
$amount = Ensure-ApprovedDefinition 'grant:amount' 'calculation' 'ql07-reviewer-b' 'Approved amount-calculation provenance for the extraordinary benefit RuleSet.'

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
) | Set-Content -LiteralPath $javaArguments -Encoding ASCII
$payloadJson = & java "@$javaArguments"
if ($LASTEXITCODE -ne 0) { throw 'Could not generate the canonical snapshot publication payload.' }
$payload = $payloadJson | Select-Object -Last 1 | ConvertFrom-Json
$published = Invoke-ConfigJson POST '/api/praxis/config/domain-rules/snapshots' $payload @{ 'If-None-Match' = '*' }

[ordered]@{
    status = 'PROVISIONED'
    sourceDefinitions = 2
    distinctApprovers = 2
    snapshotKey = $published.Json.snapshot.snapshotKey
    snapshotContentHash = $published.Json.snapshotContentHash
    activationRevision = $published.Json.activationRevision
} | ConvertTo-Json
