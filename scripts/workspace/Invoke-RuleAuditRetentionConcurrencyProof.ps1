[CmdletBinding()]
param(
    [string]$EnvironmentFile,
    [Parameter(Mandatory)]
    [string]$BranchHost,
    [Parameter(Mandatory)]
    [switch]$EphemeralBranch,
    [Parameter(Mandatory)]
    [switch]$ConnectionStringFromClipboard
)

$ErrorActionPreference = 'Stop'
Set-StrictMode -Version Latest

$projectRoot = (Resolve-Path (Join-Path $PSScriptRoot '..\..')).Path
$workDirectory = Join-Path $projectRoot 'target\rule-audit-retention-concurrency-proof'
$classpathFile = Join-Path $workDirectory 'test-classpath.txt'
$evidenceFile = Join-Path $workDirectory 'concurrency-proof-evidence.json'

if (-not $EnvironmentFile) {
    $commonGitDirectory = (& git -C $projectRoot rev-parse --path-format=absolute --git-common-dir).Trim()
    if ($LASTEXITCODE -ne 0) { throw 'Could not locate the canonical Quickstart worktree.' }
    $EnvironmentFile = Join-Path (Split-Path $commonGitDirectory -Parent) '.env.dev'
}
if (-not (Test-Path -LiteralPath $EnvironmentFile)) {
    throw "Quickstart environment file was not found: $EnvironmentFile"
}

function Get-DotEnvValue([string]$Name) {
    $prefix = "$Name="
    $line = Get-Content -LiteralPath $EnvironmentFile | Where-Object {
        $_.StartsWith($prefix, [StringComparison]::Ordinal)
    } | Select-Object -Last 1
    if (-not $line) { throw "$Name is missing from the Quickstart environment file." }
    $value = $line.Substring($prefix.Length).Trim()
    if (($value.StartsWith('"') -and $value.EndsWith('"')) -or
            ($value.StartsWith("'") -and $value.EndsWith("'"))) {
        $value = $value.Substring(1, $value.Length - 2)
    }
    if ([string]::IsNullOrWhiteSpace($value)) { throw "$Name is empty in the environment file." }
    return $value
}

function Assert-NeonJdbcUrl([string]$Value) {
    if ($Value -notmatch '^jdbc:postgresql://[^/?]+\.neon\.tech(?::\d+)?/[^?]+\?.*sslmode=require') {
        throw 'SPRING_DATASOURCE_URL must be a Neon JDBC URL with sslmode=require.'
    }
}

function Get-JdbcHost([string]$Value) {
    if ($Value -notmatch '^jdbc:postgresql://([^/:?]+)') { throw 'Invalid PostgreSQL JDBC URL.' }
    return $Matches[1]
}

if (-not $EphemeralBranch) { throw 'The proof requires -EphemeralBranch.' }
if ($BranchHost -notmatch '^[a-z0-9-]+(?:-pooler)?(?:\.[a-z0-9-]+)+\.neon\.tech$') {
    throw 'BranchHost must be a Neon endpoint hostname without protocol, port, path, or credentials.'
}
if (-not $env:JAVA_HOME) { throw 'JAVA_HOME must point to JDK 21.' }
$java = Join-Path $env:JAVA_HOME 'bin\java.exe'
if (-not (Test-Path -LiteralPath $java)) { throw 'JAVA_HOME does not contain java.exe.' }
$javaVersion = (& $java --version | Select-Object -First 1)
if ($javaVersion -notmatch '^openjdk 21(?:\.|\s)') {
    throw "JDK 21 is required; detected: $javaVersion"
}

$canonicalUrl = Get-DotEnvValue 'SPRING_DATASOURCE_URL'
Assert-NeonJdbcUrl $canonicalUrl
$canonicalHost = Get-JdbcHost $canonicalUrl
if ($BranchHost.Equals($canonicalHost, [StringComparison]::OrdinalIgnoreCase)) {
    throw 'BranchHost must differ from the canonical endpoint.'
}

$connectionString = Get-Clipboard -Raw
Set-Clipboard -Value ' '
if ($connectionString -notmatch '^postgresql://') {
    throw 'Clipboard must contain a PostgreSQL connection string copied from the ephemeral branch.'
}
$connectionUri = [Uri]$connectionString.Trim()
if (-not $connectionUri.Host.Equals($BranchHost, [StringComparison]::OrdinalIgnoreCase)) {
    throw 'Clipboard connection string does not target BranchHost.'
}
$credentials = $connectionUri.UserInfo.Split(':', 2)
if ($credentials.Count -ne 2) { throw 'Clipboard connection string has invalid credentials.' }
$databaseUser = [Uri]::UnescapeDataString($credentials[0])
$databasePassword = [Uri]::UnescapeDataString($credentials[1])
$jdbcUrl = "jdbc:postgresql://$BranchHost$($connectionUri.AbsolutePath)?sslmode=require"

New-Item -ItemType Directory -Force -Path $workDirectory | Out-Null
try {
    Push-Location $projectRoot
    try {
        & .\mvnw.cmd -q -DskipTests test-compile
        if ($LASTEXITCODE -ne 0) { throw 'Quickstart test compilation failed.' }
        & .\mvnw.cmd -q dependency:build-classpath "-Dmdep.includeScope=test" "-Dmdep.outputFile=$classpathFile"
        if ($LASTEXITCODE -ne 0 -or -not (Test-Path -LiteralPath $classpathFile)) {
            throw 'Could not build the test runtime classpath.'
        }
        $dependencies = (Get-Content -LiteralPath $classpathFile -Raw).Trim()
        $classpath = @(
            (Join-Path $projectRoot 'target\test-classes'),
            (Join-Path $projectRoot 'target\classes'),
            $dependencies
        ) -join [IO.Path]::PathSeparator

        $env:ADR12_JDBC_URL = $jdbcUrl
        $env:ADR12_DATABASE_USER = $databaseUser
        $env:ADR12_DATABASE_PASSWORD = $databasePassword
        $env:ADR12_PROJECT_ROOT = $projectRoot
        $env:ADR12_EVIDENCE_FILE = $evidenceFile
        $env:ADR12_EPHEMERAL_BRANCH = 'true'

        & $java -cp $classpath com.example.praxis.apiquickstart.rulelab.RuleAuditRetentionConcurrencyProof
        if ($LASTEXITCODE -ne 0) { throw 'ADR-12 concurrency proof failed.' }
        if (-not (Test-Path -LiteralPath $evidenceFile)) {
            throw 'ADR-12 concurrency proof produced no evidence file.'
        }
        Write-Host "ADR12_CONCURRENCY_PROOF_PASS evidence=$evidenceFile"
    } finally {
        Pop-Location
    }
} finally {
    Remove-Item Env:ADR12_JDBC_URL,Env:ADR12_DATABASE_USER,Env:ADR12_DATABASE_PASSWORD,`
        Env:ADR12_PROJECT_ROOT,Env:ADR12_EVIDENCE_FILE,Env:ADR12_EPHEMERAL_BRANCH -ErrorAction SilentlyContinue
}
