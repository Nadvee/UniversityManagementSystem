param([int]$Port = 0, [switch]$NoBrowser)
$ErrorActionPreference = 'Stop'
try {
    $projectRoot = Split-Path -Parent $PSScriptRoot
    Set-Location -LiteralPath $projectRoot
    $config = Join-Path $projectRoot '.env.mysql.local'
    if (-not (Test-Path -LiteralPath $config)) { throw 'Copy .env.mysql.example to .env.mysql.local and enter your database settings first. See README.md.' }
    foreach ($line in (Get-Content -LiteralPath $config)) {
        if ($line.Trim() -eq '' -or $line.TrimStart().StartsWith('#')) { continue }
        $pair = $line.Split('=', 2)
        if ($pair.Count -ne 2 -or $pair[0].Trim() -notmatch '^[A-Z][A-Z0-9_]*$') { throw 'Invalid line in .env.mysql.local.' }
        [Environment]::SetEnvironmentVariable($pair[0].Trim(), $pair[1].Trim(), 'Process')
    }
    if (-not $env:DB_HOST) { $env:DB_HOST = '127.0.0.1' }
    if (-not $env:DB_PORT) { $env:DB_PORT = '3306' }
    if (-not $env:DB_NAME) { $env:DB_NAME = 'mirpur_advising_db' }
    if ($env:DB_NAME -notmatch '^[a-zA-Z0-9_]+$') { throw 'Use only letters, digits and underscores in DB_NAME.' }
    if (-not $env:DB_USERNAME) { throw 'Set DB_USERNAME in .env.mysql.local.' }
    if ($Port -eq 0) { $Port = if ($env:MYSQL_APP_PORT) { [int]$env:MYSQL_APP_PORT } else { 8081 } }
    if ($Port -lt 1 -or $Port -gt 65535) { throw 'App port must be between 1 and 65535.' }
    $java = $null
    if ($env:JAVA_HOME -and (Test-Path -LiteralPath (Join-Path $env:JAVA_HOME 'bin\java.exe'))) { $java = Join-Path $env:JAVA_HOME 'bin\java.exe' }
    if (-not $java) { $command = Get-Command java.exe -ErrorAction SilentlyContinue; if ($command) { $java = $command.Source } }
    if (-not $java) { throw 'Java 17 or later is required.' }
    $jar = Join-Path $projectRoot 'target\mirpur-advising-1.0.0.jar'
    if (-not (Test-Path -LiteralPath $jar)) { throw 'The app JAR is missing from target. Extract the entire project ZIP.' }
    if ($Port -eq 8082) { throw 'Port 8082 belongs to phpMyAdmin. Choose an app port such as 8081 or 8083.' }
    $url = 'http://localhost:' + $Port + '/login.html'
    $running = Get-CimInstance Win32_Process -Filter "Name = 'java.exe'" | Where-Object {
        $_.CommandLine -like ('*' + $jar + '*') -and
        $_.CommandLine -match '--spring.profiles.active=mysql(?:\s|$)' -and
        $_.CommandLine -match ('--server.port=' + $Port + '(?:\s|$)')
    }
    if ($running) {
        for ($attempt = 0; $attempt -lt 90; $attempt++) {
            try {
                $page = Invoke-WebRequest -Uri $url -UseBasicParsing -TimeoutSec 1
                if ($page.StatusCode -eq 200 -and $page.Content.Contains('id="login-form"')) {
                    Write-Host ('Website is already running: ' + $url)
                    if (-not $NoBrowser) { Start-Process -FilePath $url }
                    exit 0
                }
            } catch { }
            Start-Sleep -Milliseconds 500
        }
        throw 'The existing website did not become ready. Run STOP-LOCAL.cmd, then RUN-LOCAL.cmd again.'
    }
    $probe = [System.Net.Sockets.TcpListener]::new([System.Net.IPAddress]::Loopback, $Port)
    try { $probe.Start() } catch { throw ('Port ' + $Port + ' is busy. Stop the old app with STOP-LOCAL.cmd, or use RUN-LOCAL.cmd -Port 8083.') } finally { $probe.Stop() }
    if ($env:MYSQL_USE_DOCKER -eq 'true') {
        if (-not (Get-Command docker.exe -ErrorAction SilentlyContinue)) { throw 'Docker Desktop is required for MYSQL_USE_DOCKER=true. For installed MySQL set it to false.' }
        . (Join-Path $PSScriptRoot 'start-docker.ps1')
        Start-MirpurDocker
        $composeArguments = @('compose', '--env-file', $config, '-f', (Join-Path $projectRoot 'docker-compose.mysql.yml'))
        $composeArguments += @('up', '-d', '--wait', '--wait-timeout', '120', 'mysql')
        & docker @composeArguments
        if ($LASTEXITCODE -ne 0) { throw 'MySQL did not start. Open Docker Desktop, wait until its engine is running, then try again.' }
    }
    $env:DB_URL = 'jdbc:mysql://' + $env:DB_HOST + ':' + $env:DB_PORT + '/' + $env:DB_NAME + '?serverTimezone=Asia/Dhaka&forceConnectionTimeZoneToSession=true&useSSL=false&allowPublicKeyRetrieval=true'
    $url = 'http://localhost:' + $Port + '/login.html'
    Write-Host ('MySQL database: ' + $env:DB_HOST + ':' + $env:DB_PORT + '/' + $env:DB_NAME)
    Write-Host 'All application data reads/writes use MySQL. Redis cache is disabled.'
    Write-Host ('Starting app: ' + $url)
    $logFolder = Join-Path $projectRoot 'data\logs'
    New-Item -ItemType Directory -Path $logFolder -Force | Out-Null
    $outputLog = Join-Path $logFolder 'website.log'
    $errorLog = Join-Path $logFolder 'website-error.log'
    $arguments = @('-jar', ('"' + $jar + '"'), '--spring.profiles.active=mysql', '--spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver', '--app.cache-enabled=false', '--management.health.redis.enabled=false', ('--server.port=' + $Port))
    $server = Start-Process -FilePath $java -ArgumentList $arguments -WorkingDirectory $projectRoot -WindowStyle Hidden -RedirectStandardOutput $outputLog -RedirectStandardError $errorLog -PassThru
    for ($attempt = 0; $attempt -lt 90; $attempt++) {
        if ($server.HasExited) { throw ('Website startup failed. Check ' + $outputLog + ' and ' + $errorLog) }
        try {
            $page = Invoke-WebRequest -Uri $url -UseBasicParsing -TimeoutSec 1
            if ($page.StatusCode -eq 200 -and $page.Content.Contains('id="login-form"')) {
                Write-Host ('Website is ready: ' + $url)
                Write-Host 'Use STOP-LOCAL.cmd to stop the website.'
                if (-not $NoBrowser) { Start-Process -FilePath $url }
                exit 0
            }
        } catch { }
        Start-Sleep -Milliseconds 500
    }
    if (-not $server.HasExited) { Stop-Process -Id $server.Id -ErrorAction SilentlyContinue }
    throw ('Website startup timed out. Check ' + $outputLog)
} catch { Write-Host ('[ERROR] ' + $_.Exception.Message) -ForegroundColor Red; exit 1 }
