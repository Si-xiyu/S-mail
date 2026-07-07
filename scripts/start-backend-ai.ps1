param(
    [string]$AgentBaseUrl = "http://127.0.0.1:8000",
    [string]$PluginToken = "smartmail-agent-plugin-dev-token",
    [string]$InternalToken = "smartmail-internal-dev-token",
    [switch]$DisableAi
)

$ErrorActionPreference = "Stop"

$repoRoot = Split-Path -Parent $PSScriptRoot
$backendDir = Join-Path $repoRoot "backend"

if (-not (Test-Path $backendDir)) {
    throw "Backend directory not found: $backendDir"
}

$env:SMARTMAIL_AI_ENABLED = if ($DisableAi) { "false" } else { "true" }
$env:SMARTMAIL_AGENT_BASE_URL = $AgentBaseUrl
$env:SMARTMAIL_AGENT_PLUGIN_TOKEN = $PluginToken
$env:SMARTMAIL_INTERNAL_TOKEN = $InternalToken

Write-Host "Starting SmartMail Backend on http://localhost:8080" -ForegroundColor Green
Write-Host "SMARTMAIL_AI_ENABLED=$env:SMARTMAIL_AI_ENABLED" -ForegroundColor Green
Write-Host "SMARTMAIL_AGENT_BASE_URL=$env:SMARTMAIL_AGENT_BASE_URL" -ForegroundColor Green

Push-Location $backendDir
try {
    mvn spring-boot:run
} finally {
    Pop-Location
}
