function Start-MirpurDocker {
    $previousPreference = $ErrorActionPreference
    $ErrorActionPreference = 'Continue'
    try {
        & docker info --format '{{.ServerVersion}}' 2>$null | Out-Null
        if ($LASTEXITCODE -eq 0) { return }
        $desktop = Join-Path $env:ProgramFiles 'Docker\Docker\Docker Desktop.exe'
        if (-not (Test-Path -LiteralPath $desktop)) { throw 'Start your Docker engine, then run the launcher again.' }
        Write-Host 'Starting Docker Desktop. Waiting for the database engine...'
        Start-Process -FilePath $desktop -WindowStyle Hidden
        for ($attempt = 0; $attempt -lt 60; $attempt++) {
            Start-Sleep -Seconds 2
            & docker info --format '{{.ServerVersion}}' 2>$null | Out-Null
            if ($LASTEXITCODE -eq 0) { return }
        }
        throw 'Docker is not ready. Open Docker Desktop, finish any setup it requires, then try again.'
    } finally { $ErrorActionPreference = $previousPreference }
}
