[CmdletBinding()]
param(
    [string]$EnvironmentFile,
    [Parameter(Mandatory)]
    [string]$ApiBranchHost,
    [Parameter(Mandatory)]
    [string]$ConsumerBranchHost,
    [ValidatePattern('^[a-z][a-z0-9_]{2,62}$')]
    [string]$ApiSchema = 'public',
    [ValidatePattern('^[a-z][a-z0-9_]{2,62}$')]
    [string]$ConsumerSchema = 'ql08_drill_consumer_20260715',
    [Parameter(Mandatory)]
    [switch]$EphemeralBranches
)

$ErrorActionPreference = 'Stop'
Set-StrictMode -Version Latest

$projectRoot = (Resolve-Path (Join-Path $PSScriptRoot '..\..')).Path
$workDirectory = Join-Path $projectRoot 'target\rule-lab-ql08-distributed-drill'
$classpathFile = Join-Path $workDirectory 'test-classpath.txt'
$keyStore = Join-Path $workDirectory 'consumer-server.p12'
$certificate = Join-Path $workDirectory 'consumer-server.cer'
$trustStore = Join-Path $workDirectory 'consumer-trust.p12'
$classpath = $null

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

function Assert-NeonJdbcUrl([string]$Name, [string]$Value) {
    if ($Value -notmatch '^jdbc:postgresql://[^/?]+\.neon\.tech(?::\d+)?/[^?]+\?.*sslmode=require') {
        throw "$Name must be a Neon JDBC URL with sslmode=require."
    }
}

function Get-JdbcHost([string]$Value) {
    if ($Value -notmatch '^jdbc:postgresql://([^/:?]+)') { throw 'Invalid PostgreSQL JDBC URL.' }
    return $Matches[1]
}

function Use-JdbcHost([string]$Value, [string]$HostName) {
    return $Value -replace '^(jdbc:postgresql://)[^/:?]+', "`$1$HostName"
}

function Add-JdbcParameter([string]$Value, [string]$Name, [string]$ParameterValue) {
    $separator = if ($Value.Contains('?')) { '&' } else { '?' }
    return "$Value$separator$Name=$([Uri]::EscapeDataString($ParameterValue))"
}

function Assert-NeonBranchHost([string]$Name, [string]$Value, [string]$CanonicalHost) {
    if ($Value -notmatch '^[a-z0-9-]+(?:-pooler)?(?:\.[a-z0-9-]+)+\.neon\.tech$') {
        throw "$Name must be a Neon endpoint hostname, without protocol, port, path, or credentials."
    }
    if ($Value.Equals($CanonicalHost, [StringComparison]::OrdinalIgnoreCase)) {
        throw "$Name must differ from the canonical endpoint; the drill cannot run on the source branch."
    }
}

if (-not $env:JAVA_HOME) { throw 'JAVA_HOME must point to JDK 21.' }
$java = Join-Path $env:JAVA_HOME 'bin\java.exe'
$keytool = Join-Path $env:JAVA_HOME 'bin\keytool.exe'
if (-not (Test-Path -LiteralPath $java) -or -not (Test-Path -LiteralPath $keytool)) {
    throw 'JAVA_HOME does not contain java.exe and keytool.exe.'
}
$javaVersion = (& $java --version | Select-Object -First 1)
if ($javaVersion -notmatch '\b21\.') { throw "JDK 21 is required; detected: $javaVersion" }

$apiUrl = Get-DotEnvValue 'SPRING_DATASOURCE_URL'
$apiUser = Get-DotEnvValue 'SPRING_DATASOURCE_USERNAME'
$apiPassword = Get-DotEnvValue 'SPRING_DATASOURCE_PASSWORD'
$consumerUrl = Get-DotEnvValue 'CONFIG_DATASOURCE_URL'
$consumerUser = Get-DotEnvValue 'CONFIG_DATASOURCE_USERNAME'
$consumerPassword = Get-DotEnvValue 'CONFIG_DATASOURCE_PASSWORD'
Assert-NeonJdbcUrl 'SPRING_DATASOURCE_URL' $apiUrl
Assert-NeonJdbcUrl 'CONFIG_DATASOURCE_URL' $consumerUrl
$canonicalApiHost = Get-JdbcHost $apiUrl
$canonicalConsumerHost = Get-JdbcHost $consumerUrl
Assert-NeonBranchHost 'ApiBranchHost' $ApiBranchHost $canonicalApiHost
Assert-NeonBranchHost 'ConsumerBranchHost' $ConsumerBranchHost $canonicalConsumerHost
$apiUrl = Use-JdbcHost $apiUrl $ApiBranchHost
$consumerUrl = Use-JdbcHost $consumerUrl $ConsumerBranchHost
$apiScopedUrl = Add-JdbcParameter $apiUrl 'currentSchema' $ApiSchema
$consumerScopedUrl = Add-JdbcParameter $consumerUrl 'currentSchema' $ConsumerSchema

New-Item -ItemType Directory -Force -Path $workDirectory | Out-Null
$keyStorePassword = ([guid]::NewGuid().ToString('N') + [guid]::NewGuid().ToString('N'))

try {
    Remove-Item -LiteralPath $keyStore,$certificate,$trustStore -Force -ErrorAction SilentlyContinue
    & $keytool -genkeypair -alias ql08-consumer -keyalg RSA -keysize 2048 -validity 2 `
        -dname 'CN=localhost, OU=QL08 Drill, O=Praxis, C=BR' `
        -ext 'SAN=dns:localhost,ip:127.0.0.1' -storetype PKCS12 `
        -keystore $keyStore -storepass $keyStorePassword -keypass $keyStorePassword -noprompt | Out-Null
    if ($LASTEXITCODE -ne 0) { throw 'Could not generate the temporary HTTPS key pair.' }
    & $keytool -exportcert -alias ql08-consumer -keystore $keyStore -storepass $keyStorePassword `
        -file $certificate -noprompt | Out-Null
    if ($LASTEXITCODE -ne 0) { throw 'Could not export the temporary HTTPS certificate.' }
    & $keytool -importcert -alias ql08-consumer -file $certificate -keystore $trustStore `
        -storetype PKCS12 -storepass $keyStorePassword -noprompt | Out-Null
    if ($LASTEXITCODE -ne 0) { throw 'Could not create the temporary HTTPS truststore.' }

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

        $env:QL08_DRILL_WORK_DIRECTORY = $workDirectory
        $env:QL08_API_JDBC_URL = $apiScopedUrl
        $env:QL08_CONSUMER_JDBC_URL = $consumerScopedUrl
        $env:QL08_CONFIG_JDBC_URL = $consumerUrl
        $env:QL08_API_SCHEMA = $ApiSchema
        $env:QL08_CONSUMER_SCHEMA = $ConsumerSchema
        $env:QL08_API_DATABASE_USER = $apiUser
        $env:QL08_API_DATABASE_PASSWORD = $apiPassword
        $env:QL08_CONSUMER_DATABASE_USER = $consumerUser
        $env:QL08_CONSUMER_DATABASE_PASSWORD = $consumerPassword
        $env:QL08_EPHEMERAL_BRANCHES = 'true'
        $env:QL08_TLS_KEYSTORE = $keyStore
        $env:QL08_TLS_KEYSTORE_PASSWORD = $keyStorePassword
        # DataSourceConfig resolves these placeholders directly from the process environment.
        $env:SPRING_DATASOURCE_URL = $apiScopedUrl
        $env:SPRING_DATASOURCE_USERNAME = $apiUser
        $env:SPRING_DATASOURCE_PASSWORD = $apiPassword
        $env:SPRING_DATASOURCE_HIKARI_SCHEMA = $ApiSchema
        $env:CONFIG_DATASOURCE_URL = $consumerUrl
        $env:CONFIG_DATASOURCE_USERNAME = $consumerUser
        $env:CONFIG_DATASOURCE_PASSWORD = $consumerPassword

        & $java "-Djavax.net.ssl.trustStore=$trustStore" `
            "-Djavax.net.ssl.trustStorePassword=$keyStorePassword" `
            -cp $classpath com.example.praxis.apiquickstart.rulelab.RuleLabDistributedOutboxDrill
        if ($LASTEXITCODE -ne 0) { throw 'QL-08 distributed outbox drill failed.' }
        $evidence = Join-Path $workDirectory 'distributed-outbox-drill-evidence.json'
        if (-not (Test-Path -LiteralPath $evidence)) {
            throw 'The drill finished without producing its evidence file.'
        }
        Write-Host "QL08_DISTRIBUTED_OUTBOX_DRILL_PASS evidence=$evidence"
    } finally {
        Pop-Location
    }
} finally {
    Remove-Item Env:QL08_DRILL_WORK_DIRECTORY,Env:QL08_API_JDBC_URL,Env:QL08_CONSUMER_JDBC_URL,Env:QL08_CONFIG_JDBC_URL,Env:QL08_API_SCHEMA,Env:QL08_CONSUMER_SCHEMA,`
        Env:QL08_API_DATABASE_USER,Env:QL08_API_DATABASE_PASSWORD,Env:QL08_CONSUMER_DATABASE_USER,`
        Env:QL08_CONSUMER_DATABASE_PASSWORD,Env:QL08_EPHEMERAL_BRANCHES,`
        Env:QL08_TLS_KEYSTORE,Env:QL08_TLS_KEYSTORE_PASSWORD,Env:SPRING_DATASOURCE_URL,Env:SPRING_DATASOURCE_HIKARI_SCHEMA,`
        Env:SPRING_DATASOURCE_USERNAME,Env:SPRING_DATASOURCE_PASSWORD,Env:CONFIG_DATASOURCE_URL,`
        Env:CONFIG_DATASOURCE_USERNAME,Env:CONFIG_DATASOURCE_PASSWORD -ErrorAction SilentlyContinue
    Remove-Item -LiteralPath $keyStore,$certificate,$trustStore -Force -ErrorAction SilentlyContinue
}
