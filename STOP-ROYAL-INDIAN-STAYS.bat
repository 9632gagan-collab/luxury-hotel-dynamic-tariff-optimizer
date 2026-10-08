@echo off
TITLE Royal Indian Stays - Stop Server
cd /d "%~dp0"
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0stop-royal-indian-stays.ps1"
