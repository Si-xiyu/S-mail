param(
    [string]$Python = "python",
    [int]$Port = 8000,
    [switch]$NoReload
)

$ErrorActionPreference = "Stop"

$repoRoot = Split-Path -Parent $PSScriptRoot
$agentDir = Join-Path $repoRoot "agent"
$envFile = Join-Path $agentDir ".env"
$envExample = Join-Path $agentDir ".env.example"
$configFile = Join-Path $agentDir "config\providers.toml"
$configExample = Join-Path $agentDir "config\providers.example.toml"

if (-not (Test-Path $agentDir)) {
    throw "Agent directory not found: $agentDir"
}

if (-not (Test-Path $configFile)) {
    if (-not (Test-Path $configExample)) {
        throw "Provider config example not found: $configExample"
    }
    Copy-Item $configExample $configFile
    Write-Host "Created agent/config/providers.toml from providers.example.toml" -ForegroundColor Yellow
}

if (-not (Test-Path $envFile)) {
    if (-not (Test-Path $envExample)) {
        throw ".env.example not found: $envExample"
    }
    Copy-Item $envExample $envFile
    Write-Host "Created agent/.env from .env.example" -ForegroundColor Yellow
    Write-Host "Edit agent/.env and fill DEEPSEEK_API_KEY before testing real DeepSeek calls." -ForegroundColor Yellow
}

$envContent = Get-Content -Raw -Encoding UTF8 $envFile
if ($envContent -match "DEEPSEEK_API_KEY\s*=\s*($|sk-your|sk-你的|your)") {
    Write-Host "DEEPSEEK_API_KEY in agent/.env looks empty or placeholder. Agent can still start, but LLM calls may fall back." -ForegroundColor Yellow
}

$uvicornArgs = @(
    "-m", "uvicorn",
    "app.main:app",
    "--host", "127.0.0.1",
    "--port", "$Port",
    "--env-file", ".env"
)

if (-not $NoReload) {
    $uvicornArgs += "--reload"
}

Write-Host "Starting SmartMail Agent on http://127.0.0.1:$Port" -ForegroundColor Green
Write-Host "Using env file: $envFile" -ForegroundColor Green
Write-Host "Using provider config: $configFile" -ForegroundColor Green

Push-Location $agentDir
try {
    & $Python @uvicornArgs
} finally {
    Pop-Location
}
