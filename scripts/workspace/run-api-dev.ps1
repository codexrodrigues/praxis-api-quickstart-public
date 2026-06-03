<#
.SYNOPSIS
  Arranca o JAR do quickstart em perfil dev, com datasource via env (Neon / Postgres remoto).

.DESCRIPTION
  1) Opcional: carrega `praxis-api-quickstart/.env.dev` (gitignored) — linhas KEY=VAL, # comentario.
  2) Garante SPRING_PROFILES_ACTIVE=dev se nao estiver definido.
  3) Executa o JAR em `target/praxis-api-quickstart-*.jar` (exclui *-sources*).

  Neon: no dashboard, copia a connection string. JDBC tipico:
    jdbc:postgresql://<host>/<dbname>?sslmode=require
  Nao use `channel_binding=require` no JDBC (driver nao suporta).

  Variaveis minimas (export ou no .env.dev):
    SPRING_DATASOURCE_URL
    SPRING_DATASOURCE_USERNAME
    SPRING_DATASOURCE_PASSWORD
    CORS_ALLOWED_ORIGINS (ex.: http://localhost:4200)
    PRACTICE_TEMP_PASSWORD (login /auth)
#>
[CmdletBinding()]
param(
    [string] $EnvFile = "",
    [string] $ProjectRoot = ""
)

$ErrorActionPreference = "Stop"

if (-not $ProjectRoot) {
    $ProjectRoot = (Resolve-Path (Join-Path $PSScriptRoot "..\..")).Path
}
if (-not $EnvFile) {
    $EnvFile = Join-Path $ProjectRoot ".env.dev"
}

function Import-DotEnvFile {
    param([string] $Path)
    if (-not (Test-Path -LiteralPath $Path)) { return }
    Get-Content -LiteralPath $Path -Encoding UTF8 | ForEach-Object {
        $line = $_.Trim()
        if ($line -match '^\s*#' -or $line -eq "") { return }
        $eq = $line.IndexOf("=")
        if ($eq -lt 1) { return }
        $name = $line.Substring(0, $eq).Trim()
        $val = $line.Substring($eq + 1).Trim()
        if ($val.Length -ge 2 -and $val.StartsWith('"') -and $val.EndsWith('"')) {
            $val = $val.Substring(1, $val.Length - 2)
        }
        if ($name -match '^[A-Za-z_][A-Za-z0-9_]*$') {
            Set-Item -Path "Env:$name" -Value $val
        }
    }
    Write-Host "Carregado: $Path" -ForegroundColor DarkGreen
}

if (Test-Path -LiteralPath $EnvFile) {
    Import-DotEnvFile -Path $EnvFile
} else {
    Write-Host "Aviso: $EnvFile nao encontrado; usando apenas variaveis ja definidas no processo." -ForegroundColor Yellow
}

if (-not $env:SPRING_PROFILES_ACTIVE) {
    $env:SPRING_PROFILES_ACTIVE = "dev"
}

$required = @("SPRING_DATASOURCE_URL", "SPRING_DATASOURCE_USERNAME", "SPRING_DATASOURCE_PASSWORD")
foreach ($k in $required) {
    $v = [Environment]::GetEnvironmentVariable($k, "Process")
    if ([string]::IsNullOrWhiteSpace($v)) {
        Write-Error "Defina $k no ambiente ou em .env.dev (ver .env.dev.example e README)."
    }
}

$jars = Get-ChildItem -Path (Join-Path $ProjectRoot "target") -Filter "praxis-api-quickstart-*.jar" -File -ErrorAction SilentlyContinue |
    Where-Object { $_.Name -notmatch '-sources\.jar$' }
if (-not $jars) {
    Write-Error "JAR nao encontrado em target. Execute antes: mvn -DskipTests package"
}
$jar = $jars | Sort-Object LastWriteTime -Descending | Select-Object -First 1

Write-Host "JAR: $($jar.FullName)" -ForegroundColor Cyan
Write-Host "Profile: $($env:SPRING_PROFILES_ACTIVE)" -ForegroundColor Cyan
& java -jar $jar.FullName
