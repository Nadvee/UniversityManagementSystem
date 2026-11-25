$ErrorActionPreference = 'Stop'
try {
    $projectRoot = Split-Path -Parent $PSScriptRoot
    Set-Location -LiteralPath $projectRoot
    foreach ($line in Get-Content -LiteralPath (Join-Path $projectRoot '.env.mysql.local')) {
        if ($line.Trim() -eq '' -or $line.TrimStart().StartsWith('#')) { continue }
        $pair = $line.Split('=', 2)
        if ($pair.Count -ne 2 -or $pair[0].Trim() -notmatch '^[A-Z][A-Z0-9_]*$') { throw 'Invalid MySQL settings line.' }
        [Environment]::SetEnvironmentVariable($pair[0].Trim(), $pair[1].Trim(), 'Process')
    }
    if ($env:MYSQL_USE_DOCKER -ne 'true' -or $env:DB_NAME -notmatch '^[a-zA-Z0-9_]+$') { throw 'This migration requires the configured Docker MySQL database.' }
    $runningWebsite = Get-CimInstance Win32_Process -Filter "Name = 'java.exe'" | Where-Object { $_.CommandLine -like ('*' + (Join-Path $projectRoot 'target\mirpur-advising-1.0.0.jar') + '*') }
    if ($runningWebsite) { throw 'Stop the website with STOP-LOCAL.cmd before migrating.' }
    & docker exec -e ('MIRPUR_DATABASE=' + $env:DB_NAME) mirpur-mysql-demo sh -c 'test "$MYSQL_DATABASE" = "$MIRPUR_DATABASE"'
    if ($LASTEXITCODE -ne 0) { throw 'The configured database does not match this project container.' }
    New-Item -ItemType Directory -Path (Join-Path $projectRoot 'data\backups') -Force | Out-Null
    $backup = Join-Path $projectRoot ('data\backups\mysql-core-migration-' + (Get-Date -Format yyyyMMdd-HHmmss) + '.sql')
    & docker exec mirpur-mysql-demo sh -c 'MYSQL_PWD="$MYSQL_ROOT_PASSWORD" mysqldump --user=root --single-transaction --routines --triggers "$MYSQL_DATABASE" > /tmp/mirpur-core-backup.sql'
    if ($LASTEXITCODE -ne 0) { throw 'Backup failed. No cleanup was applied.' }
    & docker cp mirpur-mysql-demo:/tmp/mirpur-core-backup.sql $backup
    if ($LASTEXITCODE -ne 0 -or (Get-Item -LiteralPath $backup).Length -lt 1000) { throw 'Backup copy verification failed.' }
    & docker cp (Join-Path $projectRoot 'src\main\resources\schema.sql') mirpur-mysql-demo:/tmp/mirpur-core-schema.sql
    if ($LASTEXITCODE -ne 0) { throw 'Schema copy failed.' }
    & docker cp (Join-Path $PSScriptRoot 'migrations\core-schema.sql') mirpur-mysql-demo:/tmp/mirpur-core-migration.sql
    if ($LASTEXITCODE -ne 0) { throw 'Migration copy failed.' }
    & docker exec mirpur-mysql-demo sh -c 'MYSQL_PWD="$MYSQL_ROOT_PASSWORD" mysql --user=root "$MYSQL_DATABASE" < /tmp/mirpur-core-migration.sql'
    if ($LASTEXITCODE -ne 0) { throw ('Migration failed. The backup is at ' + $backup) }
    Write-Host ('Core schema migration complete. Backup: ' + $backup)
} catch { Write-Host ('[ERROR] ' + $_.Exception.Message) -ForegroundColor Red; exit 1 }
