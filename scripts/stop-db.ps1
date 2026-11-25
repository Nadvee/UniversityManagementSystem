$ErrorActionPreference = 'Stop'
try {
    if (-not (Get-Command docker.exe -ErrorAction SilentlyContinue)) { throw 'Docker Desktop was not found.' }
    $previousPreference = $ErrorActionPreference
    $ErrorActionPreference = 'Continue'
    & docker info --format '{{.ServerVersion}}' 2>$null | Out-Null
    $engineExit = $LASTEXITCODE
    $ErrorActionPreference = $previousPreference
    if ($engineExit -ne 0) { Write-Host 'phpMyAdmin is not running; the Docker engine is stopped.'; exit 0 }
    $container = & docker ps -a --filter 'name=^/mirpur-database-viewer$' --format '{{.Names}}'
    if ($LASTEXITCODE -ne 0) { throw 'Could not check phpMyAdmin.' }
    if (-not $container) { Write-Host 'phpMyAdmin is not running.'; exit 0 }
    & docker stop mirpur-database-viewer | Out-Null
    if ($LASTEXITCODE -ne 0) { throw 'Could not stop phpMyAdmin.' }
    Write-Host 'phpMyAdmin stopped.'
} catch { Write-Host ('[ERROR] ' + $_.Exception.Message) -ForegroundColor Red; exit 1 }
