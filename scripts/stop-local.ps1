[CmdletBinding(SupportsShouldProcess = $true)]
param()
$ErrorActionPreference = 'Stop'
try {
    $projectRoot = [System.IO.Path]::GetFullPath((Split-Path -Parent $PSScriptRoot)).TrimEnd('\')
    $expectedJar = Join-Path $projectRoot 'target\mirpur-advising-1.0.0.jar'
    $found = 0
    foreach ($process in (Get-CimInstance Win32_Process -Filter "Name = 'java.exe'")) {
        $match = [regex]::Match([string]$process.CommandLine, '-jar\s+(?:"([^"]+)"|(\S+))')
        if (-not $match.Success) { continue }
        $jar = if ($match.Groups[1].Success) { $match.Groups[1].Value } else { $match.Groups[2].Value }
        if ([System.IO.Path]::GetFileName($jar) -ne 'mirpur-advising-1.0.0.jar') { continue }
        if (-not [System.IO.Path]::IsPathRooted($jar)) {
            $jcmd = Join-Path (Split-Path -Parent $process.ExecutablePath) 'jcmd.exe'
            if (-not (Test-Path -LiteralPath $jcmd)) { continue }
            $properties = & $jcmd $process.ProcessId VM.system_properties
            $directoryLine = [string]($properties | Where-Object { $_.StartsWith('user.dir=') } | Select-Object -First 1)
            if (-not $directoryLine) { continue }
            $directory = $directoryLine.Substring(9).Replace('\:', ':').Replace('\\', '\')
            $jar = Join-Path $directory $jar
        }
        if ([System.IO.Path]::GetFullPath($jar) -ne $expectedJar) { continue }
        $runningProcess = Get-Process -Id $process.ProcessId -ErrorAction SilentlyContinue
        if (-not $runningProcess -or $runningProcess.HasExited) { continue }
        $found++
        if ($PSCmdlet.ShouldProcess(('University of Mirpur local server, PID ' + $process.ProcessId), 'Stop')) {
            $previousPreference = $ErrorActionPreference
            $ErrorActionPreference = 'Continue'
            & taskkill.exe /PID $process.ProcessId /T /F 2>$null | Out-Null
            $stopExit = $LASTEXITCODE
            $ErrorActionPreference = $previousPreference
            if ($stopExit -ne 0) {
                $remainingProcess = Get-Process -Id $process.ProcessId -ErrorAction SilentlyContinue
                if ($remainingProcess -and -not $remainingProcess.HasExited) { throw 'Could not stop the local server. Run STOP-LOCAL.cmd as the user who started the app.' }
                continue
            }
            Write-Host 'University of Mirpur local server stopped.'
        }
    }
    if (-not $found) { Write-Host 'No local server is running from this project folder.' }
} catch {
    Write-Host ('[ERROR] ' + $_.Exception.Message) -ForegroundColor Red
    exit 1
}
