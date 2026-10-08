$ErrorActionPreference = 'Stop'
Set-Location $PSScriptRoot
$jar = Join-Path $PSScriptRoot 'target\city-tour-monopoly-0.1.0.jar'
if (-not (Test-Path $jar)) { throw '请先运行 .\build.ps1' }
java -jar $jar @args