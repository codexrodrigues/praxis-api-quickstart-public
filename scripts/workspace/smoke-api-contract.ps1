<#
.SYNOPSIS
  Valida /actuator/health e GET /schemas/filtered (superficie canónica para UI e LLM).

.DESCRIPTION
  O segundo pedido exige `APP_SECURITY_SCHEMAS_AGGREGATOR_ENABLED=true` (ver .env.dev.example)
  para tornar /schemas/** público. Sem isso, pode devolver 401/403.

.EXAMPLE
  powershell -File scripts/workspace/smoke-api-contract.ps1
  powershell -File scripts/workspace/smoke-api-contract.ps1 -BaseUrl "http://localhost:8088" -ResourcePath "/api/operations/missoes"
#>
[CmdletBinding()]
param(
    [string] $BaseUrl = "http://localhost:8088",
    [string] $ResourcePath = "/api/human-resources/funcionarios"
)

$ErrorActionPreference = "Stop"
$BaseUrl = $BaseUrl.TrimEnd("/")

function Test-GetJson {
    param([string] $Name, [string] $Uri)
    try {
        $r = Invoke-WebRequest -Uri $Uri -Method GET -UseBasicParsing -TimeoutSec 30
        Write-Host "OK $($r.StatusCode) $Name" -ForegroundColor Green
        return $r.Content
    } catch {
        Write-Host "Falhou $Name : $($_.Exception.Message)" -ForegroundColor Red
        throw
    }
}

# 1) Health
$null = Test-GetJson "health" "$BaseUrl/actuator/health"

# 2) Schema filtrado (mesmo contrato que o runtime Angular / asserts LLM)
$p = [uri]::EscapeDataString($ResourcePath)
$filteredUri = "$BaseUrl/schemas/filtered?path=$p&operation=post&schemaType=request"
try {
    $body = Test-GetJson "schemas/filtered" $filteredUri
    if ($body.Length -gt 800) {
        Write-Host "Corpo (primeiros 800 chars):" -ForegroundColor Cyan
        Write-Output $body.Substring(0, 800)
        Write-Host "..." -ForegroundColor DarkGray
    } else {
        Write-Host "Corpo:" -ForegroundColor Cyan
        Write-Output $body
    }
} catch {
    Write-Host "Dica: defina APP_SECURITY_SCHEMAS_AGGREGATOR_ENABLED=true no .env.dev e reinicie a API." -ForegroundColor Yellow
    exit 1
}

Write-Host "Smoke de contrato concluido." -ForegroundColor Green
exit 0
