@echo off
TITLE Royal Indian Stays - Local Launcher
cd /d "%~dp0"
echo =================================================================
echo           ROYAL INDIAN STAYS - ONE-CLICK LAUNCHER
echo =================================================================
echo Starting local application controller...
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0start-royal-indian-stays.ps1"
if %ERRORLEVEL% NEQ 0 (
    echo.
    echo Launcher exited with status code %ERRORLEVEL%.
    pause
)
