<#
.SYNOPSIS
  Proves that the Quickstart resolves and verifies with public Praxis artifacts only.

.DESCRIPTION
  Uses an isolated local repository plus a Maven Central-only mirror. This prevents
  artifacts installed by another workspace or supplied by a corporate mirror from
  satisfying the QL-07 downstream gate silently. Generated evidence stays in target/.
#>
[CmdletBinding()]
param(
    [string] $ProjectRoot = "",
    [string] $MavenRepository = "",
    [string] $AngularRegistryPath = "",
    [switch] $ResetRepository
)

$ErrorActionPreference = "Stop"

if (-not $ProjectRoot) {
    $ProjectRoot = (Resolve-Path (Join-Path $PSScriptRoot "..\..")).Path
}
if (-not $MavenRepository) {
    $MavenRepository = Join-Path $ProjectRoot "target\ql07-public-m2"
}

$settings = Join-Path $ProjectRoot ".mvn\ql07-central-only-settings.xml"
$target = Join-Path $ProjectRoot "target"
$treeFile = Join-Path $target "ql07-public-dependency-tree.txt"
$evidenceFile = Join-Path $target "ql07-public-maven-evidence.json"
$verifyArguments = @("verify")
$angularRegistrySha256 = $null

if ($AngularRegistryPath) {
    $resolvedAngularRegistry = [IO.Path]::GetFullPath($AngularRegistryPath)
    if (-not (Test-Path -LiteralPath $resolvedAngularRegistry -PathType Leaf)) {
        throw "Angular ingestion registry was not found: $resolvedAngularRegistry"
    }
    $verifyArguments = @(
        "-Dpraxis.angular.registry.path=$resolvedAngularRegistry",
        "verify"
    )
    $angularRegistrySha256 = (Get-FileHash -Algorithm SHA256 -LiteralPath $resolvedAngularRegistry).Hash.ToLowerInvariant()
}

if ($ResetRepository -and (Test-Path -LiteralPath $MavenRepository)) {
    $resolvedRoot = [IO.Path]::GetFullPath($ProjectRoot).TrimEnd('\')
    $resolvedRepo = [IO.Path]::GetFullPath($MavenRepository)
    $expectedParent = [IO.Path]::GetFullPath($target).TrimEnd('\') + '\'
    if (-not $resolvedRepo.StartsWith($expectedParent, [StringComparison]::OrdinalIgnoreCase)) {
        throw "Refusing to reset Maven repository outside target/: $resolvedRepo"
    }
    Remove-Item -LiteralPath $resolvedRepo -Recurse -Force
}

New-Item -ItemType Directory -Path $target -Force | Out-Null

[xml] $pom = Get-Content -Raw (Join-Path $ProjectRoot "pom.xml")
$pomText = Get-Content -Raw (Join-Path $ProjectRoot "pom.xml")
if ($pomText -match '<repositories>|<pluginRepositories>|<systemPath>|file:') {
    throw "The Quickstart POM contains a repository or local-file override; QL-07 must use canonical public coordinates."
}

function Invoke-Maven {
    param([string[]] $Arguments)
    & mvn -B -s $settings "-Dmaven.repo.local=$MavenRepository" @Arguments
    if ($LASTEXITCODE -ne 0) {
        throw "Maven failed with exit code $LASTEXITCODE."
    }
}

Push-Location $ProjectRoot
try {
    Invoke-Maven @(
        "-U",
        "-Dincludes=io.github.codexrodrigues",
        "-DoutputFile=$treeFile",
        "dependency:tree"
    )

    $expected = @(
        "io.github.codexrodrigues:praxis-metadata-starter:jar:$($pom.project.properties.'praxis.core.version'):compile",
        "io.github.codexrodrigues:praxis-config-starter:jar:$($pom.project.properties.'praxis.config.version'):compile",
        "io.github.codexrodrigues:praxis-rules-engine:jar:$($pom.project.properties.'praxis.rules.version'):compile"
    )
    $tree = Get-Content -Raw $treeFile
    foreach ($coordinate in $expected) {
        if (-not $tree.Contains($coordinate)) {
            throw "Expected public dependency was not resolved: $coordinate"
        }
    }

    Invoke-Maven $verifyArguments

    $artifacts = foreach ($coordinate in $expected) {
        $parts = $coordinate.Split(':')
        $relativeGroup = $parts[0].Replace('.', [IO.Path]::DirectorySeparatorChar)
        $jar = Join-Path $MavenRepository "$relativeGroup\$($parts[1])\$($parts[3])\$($parts[1])-$($parts[3]).jar"
        if (-not (Test-Path -LiteralPath $jar)) {
            throw "Resolved artifact JAR was not found in the isolated repository: $coordinate"
        }
        [ordered]@{
            coordinate = ($parts[0..3] -join ':')
            sha256 = (Get-FileHash -Algorithm SHA256 -LiteralPath $jar).Hash.ToLowerInvariant()
        }
    }

    $evidence = [ordered]@{
        status = "PUBLIC_MAVEN_VERIFIED"
        verifiedAtUtc = [DateTimeOffset]::UtcNow.ToString("O")
        registry = "https://repo.maven.apache.org/maven2"
        isolatedRepository = $true
        angularRegistryOverride = [bool] $AngularRegistryPath
        angularRegistrySha256 = $angularRegistrySha256
        projectVersion = [string] $pom.project.version
        javaVersion = (& javac -version | Select-Object -First 1).ToString()
        artifacts = @($artifacts)
    }
    $evidence | ConvertTo-Json -Depth 5 | Set-Content -LiteralPath $evidenceFile -Encoding UTF8
    $evidence | ConvertTo-Json -Depth 5
} finally {
    Pop-Location
}
