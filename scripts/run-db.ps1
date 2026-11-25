param([switch]$NoBrowser)
$ErrorActionPreference = 'Stop'
try {
    $projectRoot = Split-Path -Parent $PSScriptRoot
    Set-Location -LiteralPath $projectRoot
    $config = Join-Path $projectRoot '.env.mysql.local'
    if (-not (Test-Path -LiteralPath $config)) { throw 'MySQL settings are missing. See README.md.' }
    foreach ($line in (Get-Content -LiteralPath $config)) {
        if ($line.Trim() -eq '' -or $line.TrimStart().StartsWith('#')) { continue }
        $pair = $line.Split('=', 2)
        if ($pair.Count -ne 2 -or $pair[0].Trim() -notmatch '^[A-Z][A-Z0-9_]*$') { throw 'Invalid line in .env.mysql.local.' }
        [Environment]::SetEnvironmentVariable($pair[0].Trim(), $pair[1].Trim(), 'Process')
    }
    if ($env:MYSQL_USE_DOCKER -ne 'true') { throw 'This phpMyAdmin launcher connects to Docker MySQL. For native MySQL use your configured Workbench connection.' }
    if (-not $env:DB_NAME) { $env:DB_NAME = 'mirpur_advising_db' }
    if ($env:DB_NAME -notmatch '^[a-zA-Z0-9_]+$') { throw 'Use only letters, digits and underscores in DB_NAME.' }
    if (-not (Get-Command docker.exe -ErrorAction SilentlyContinue)) { throw 'Install Docker Desktop first. See README.md.' }
    . (Join-Path $PSScriptRoot 'start-docker.ps1')
    Start-MirpurDocker
    & docker compose --env-file $config -f (Join-Path $projectRoot 'docker-compose.mysql.yml') --profile viewer up -d --wait --wait-timeout 120 mysql phpmyadmin
    if ($LASTEXITCODE -ne 0) { throw 'Open Docker Desktop and wait until its engine is running, then try again.' }
    $url = 'http://localhost:8082/index.php?route=/database/structure&db=' + $env:DB_NAME
    Write-Host ('Database viewer: ' + $url)
    if (-not $NoBrowser) { Start-Process -FilePath $url }
} catch { Write-Host ('[ERROR] ' + $_.Exception.Message) -ForegroundColor Red; exit 1 }
