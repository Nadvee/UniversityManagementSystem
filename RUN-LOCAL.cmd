@echo off
setlocal
"%SystemRoot%\System32\WindowsPowerShell\v1.0\powershell.exe" -NoProfile -ExecutionPolicy Bypass -File "%~dp0scripts\run-local.ps1" %*
set "LOCAL_EXIT=%ERRORLEVEL%"
if not "%LOCAL_EXIT%"=="0" if not defined MIRPUR_NO_PAUSE pause
exit /b %LOCAL_EXIT%
