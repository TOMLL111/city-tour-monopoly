$ErrorActionPreference = 'Stop'
Set-Location $PSScriptRoot
Write-Host 'Building City Tour Monopoly...' -ForegroundColor Cyan
& .\mvnw.cmd clean package
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
Write-Host 'Build complete: target\city-tour-monopoly-0.1.0.jar' -ForegroundColor Green