@echo off
setlocal
title JavadAI Source Export

echo ==========================================
echo          JAVADAI SOURCE EXPORT
echo ==========================================
echo.

set "SCRIPT=%~dp0ExportProject.ps1"

if not exist "%SCRIPT%" (
    echo [ERROR] ExportProject.ps1 not found!
    pause
    exit /b 1
)

powershell.exe -NoLogo -NoProfile -ExecutionPolicy Bypass -File "%SCRIPT%"

if errorlevel 1 (
    echo.
    echo [ERROR] Export failed!
) else (
    echo.
    echo [SUCCESS] Export completed!
)

echo.
pause
endlocal