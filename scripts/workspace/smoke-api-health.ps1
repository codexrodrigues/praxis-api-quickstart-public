<#
.SYNOPSIS
  GET /actuator/health no quickstart (validacao rapida depois de subir a API).

.EXAMPLE
  pwsh -File scripts/workspace/smoke-api-health.ps1
  pwsh -File scripts/workspace/smoke-api-health.ps1 -BaseUrl "http://localhost:8088"
#>
[CmdletBinding()]
param(
    [string] $BaseUrl = "http://localhost:8088"
)

$ErrorActionPreference = "Stop"
$uri = ($BaseUrl.TrimEnd("/")) + "/actuator/health"

try {
    $r = Invoke-WebRequest -Uri $uri -Method GET -UseBasicParsing -TimeoutSec 15
    Write-Host "OK $($r.StatusCode) $uri" -ForegroundColor Green
    Write-Output $r.Content
    exit 0
} catch {
    Write-Host "Falhou: $uri" -ForegroundColor Red
    Write-Host $_.Exception.Message
    exit 1
}
