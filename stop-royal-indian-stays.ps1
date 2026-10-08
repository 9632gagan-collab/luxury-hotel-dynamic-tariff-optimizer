# ==============================================================================
# ROYAL INDIAN STAYS - APPLICATION SHUTDOWN CONTROLLER (PowerShell)
# Safely stops the Royal Indian Stays instance without affecting other processes
# ==============================================================================
[CmdletBinding()]
param(
    [int]$Port = 8080
)

$Host.UI.RawUI.WindowTitle = "ROYAL INDIAN STAYS - Stop Server"
Clear-Host

Write-Host "=================================================================" -ForegroundColor DarkYellow
Write-Host "               ROYAL INDIAN STAYS - STOPPING SERVER              " -ForegroundColor Yellow
Write-Host "=================================================================" -ForegroundColor DarkYellow
Write-Host ""

Write-Host "Scanning for processes listening on port $Port..." -ForegroundColor Cyan

$connections = Get-NetTCPConnection -LocalPort $Port -State Listen -ErrorAction SilentlyContinue

if (-not $connections) {
    Write-Host "  -> No active server found listening on port $Port." -ForegroundColor Green
    Write-Host "  -> Royal Indian Stays is already stopped." -ForegroundColor Green
    Start-Sleep -Seconds 2
    exit 0
}

$stoppedCount = 0
$processIds = $connections.OwningProcess | Select-Object -Unique

foreach ($pidToStop in $processIds) {
    try {
        $proc = Get-Process -Id $pidToStop -ErrorAction SilentlyContinue
        if ($proc) {
            Write-Host "  -> Found target process '$($proc.ProcessName)' (PID: $pidToStop) on port $Port." -ForegroundColor Yellow
            Write-Host "     Stopping process..." -ForegroundColor Yellow
            Stop-Process -Id $pidToStop -Force -ErrorAction Stop
            Write-Host "     Successfully terminated PID $pidToStop." -ForegroundColor Green
            $stoppedCount++
        }
    } catch {
        Write-Host "     Failed to stop PID $($pidToStop): $($_.Exception.Message)" -ForegroundColor Red
    }
}

Write-Host ""
if ($stoppedCount -gt 0) {
    Write-Host "Royal Indian Stays server on port $Port has been successfully stopped." -ForegroundColor Green
} else {
    Write-Host "Port check complete." -ForegroundColor Yellow
}
Start-Sleep -Seconds 2
