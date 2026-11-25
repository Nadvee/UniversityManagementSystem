@echo off
setlocal
"%SystemRoot%\System32\WindowsPowerShell\v1.0\powershell.exe" -NoProfile -ExecutionPolicy Bypass -File "%~dp0scripts\stop-db.ps1" %*
set "DATABASE_EXIT=%ERRORLEVEL%"
if not "%DATABASE_EXIT%"=="0" if not defined MIRPUR_NO_PAUSE pause
exit /b %DATABASE_EXIT%
