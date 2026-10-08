# ==============================================================================
# ROYAL INDIAN STAYS - LOCAL APPLICATION LAUNCHER (PowerShell)
# ==============================================================================
[CmdletBinding()]
param(
    [switch]$Rebuild,
    [int]$Port = 8080
)

try { $Host.UI.RawUI.WindowTitle = "ROYAL INDIAN STAYS - Server Controller" } catch {}
try { Clear-Host } catch {}

Write-Host "=================================================================" -ForegroundColor DarkYellow
Write-Host "               ROYAL INDIAN STAYS - LUXURY HOTEL SYSTEM          " -ForegroundColor Yellow
Write-Host "=================================================================" -ForegroundColor DarkYellow
Write-Host ""

$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
Set-Location $ScriptDir

# 1. Inspect Java Runtime
Write-Host "[1/6] Verifying Java Environment..." -ForegroundColor Cyan
try {
    $javaVersion = java -version 2>&1 | Out-String
    if ($LASTEXITCODE -ne 0 -and [string]::IsNullOrWhiteSpace($javaVersion)) {
        throw "Java executable not found on PATH."
    }
    $firstJavaLine = ($javaVersion -split "`n")[0].Trim()
    Write-Host "  -> OK: $firstJavaLine" -ForegroundColor Green
} catch {
    Write-Host "  -> ERROR: Java 17+ is required to run Royal Indian Stays." -ForegroundColor Red
    Write-Host "     Please install JDK 17+ and ensure 'java' is accessible from PATH." -ForegroundColor Red
    Read-Host "Press Enter to exit..."
    exit 1
}

# 2. Inspect Project Structure
Write-Host "[2/6] Verifying Project Directory..." -ForegroundColor Cyan
if (-not (Test-Path "$ScriptDir\pom.xml")) {
    Write-Host "  -> ERROR: pom.xml not found in '$ScriptDir'." -ForegroundColor Red
    Read-Host "Press Enter to exit..."
    exit 1
}
Write-Host "  -> OK: Valid Maven project structure found." -ForegroundColor Green

# 3. Check Database Services
Write-Host "[3/6] Checking Database Services..." -ForegroundColor Cyan
$pgAvailable = $false
try {
    $tcp = New-Object System.Net.Sockets.TcpClient
    $tcp.Connect("127.0.0.1", 5432)
    $tcp.Close()
    $pgAvailable = $true
    Write-Host "  -> Local PostgreSQL detected on port 5432 (Using PostgreSQL database)." -ForegroundColor Green
} catch {
    Write-Host "  -> Local PostgreSQL not detected on port 5432." -ForegroundColor Yellow
    Write-Host "     Application will automatically use embedded high-performance H2 fallback." -ForegroundColor Green
}

# 4. Port Collision & Cleanup Check
Write-Host "[4/6] Checking Port $Port Availability..." -ForegroundColor Cyan
$occupiedConnections = Get-NetTCPConnection -LocalPort $Port -State Listen -ErrorAction SilentlyContinue
if ($occupiedConnections) {
    Write-Host "  -> Port $Port is currently in use by PID(s): $($occupiedConnections.OwningProcess -join ', ')." -ForegroundColor Yellow
    foreach ($procId in ($occupiedConnections.OwningProcess | Select-Object -Unique)) {
        try {
            $proc = Get-Process -Id $procId -ErrorAction SilentlyContinue
            if ($proc.ProcessName -match "java|cmd|powershell") {
                Write-Host "     Terminating existing process '$($proc.ProcessName)' (PID $procId)..." -ForegroundColor Yellow
                Stop-Process -Id $procId -Force -ErrorAction SilentlyContinue
                Start-Sleep -Seconds 1
            } else {
                Write-Host "     WARNING: Non-Java process '$($proc.ProcessName)' (PID $procId) is on port $Port." -ForegroundColor Red
            }
        } catch {}
    }
} else {
    Write-Host "  -> OK: Port $Port is free and ready." -ForegroundColor Green
}

# 5. Build Artifact If Required
$targetJars = Get-ChildItem -Path "$ScriptDir\target\*.jar" -Exclude "*sources.jar","*javadoc.jar" -ErrorAction SilentlyContinue
if ($Rebuild -or (-not $targetJars)) {
    Write-Host "[5/6] Building Production Artifact via Maven Wrapper..." -ForegroundColor Cyan
    $mvnCmd = if (Test-Path "$ScriptDir\mvnw.cmd") { "$ScriptDir\mvnw.cmd" } else { "mvn" }
    & $mvnCmd clean package -DskipTests
    if ($LASTEXITCODE -ne 0) {
        Write-Host "  -> ERROR: Maven build failed. Check the error log above." -ForegroundColor Red
        Read-Host "Press Enter to exit..."
        exit 1
    }
    $targetJars = Get-ChildItem -Path "$ScriptDir\target\*.jar" -Exclude "*sources.jar","*javadoc.jar" -ErrorAction SilentlyContinue
} else {
    Write-Host "[5/6] Existing build artifact detected in /target (Skip rebuild; use -Rebuild to force)." -ForegroundColor Green
}

# 6. Detect LAN IP dynamically
$lanIp = $null
try {
    $ipAddresses = Get-NetIPAddress -AddressFamily IPv4 -ErrorAction SilentlyContinue | 
        Where-Object { 
            $_.IPAddress -notlike "127.*" -and 
            $_.IPAddress -notlike "169.254.*" -and
            $_.InterfaceAlias -notmatch "vEthernet|Virtual|Loopback"
        }
    if ($ipAddresses) {
        # Prefer Wi-Fi or primary Ethernet
        $primary = $ipAddresses | Where-Object { $_.InterfaceAlias -match "Wi-Fi|Wireless|Ethernet" } | Select-Object -First 1
        if ($primary) {
            $lanIp = $primary.IPAddress
        } else {
            $lanIp = ($ipAddresses | Select-Object -First 1).IPAddress
        }
    }
} catch {}

if (-not $lanIp) {
    $lanIp = "127.0.0.1"
}

# Read optional Public URL from .env or DEPLOYMENT.md
$publicUrl = "https://<DEPLOYED-DOMAIN> (Deploy via Railway to activate public HTTPS)"
if (Test-Path "$ScriptDir\.env") {
    $envContent = Get-Content "$ScriptDir\.env" -ErrorAction SilentlyContinue
    $domainLine = $envContent | Where-Object { $_ -match "^PUBLIC_DOMAIN=" } | Select-Object -First 1
    if ($domainLine) {
        $val = ($domainLine -split "=", 2)[1].Trim()
        if ($val) { $publicUrl = "https://$val" }
    }
}

Write-Host ""
Write-Host "=================================================================" -ForegroundColor DarkGreen
Write-Host "                   SERVER READY TO LAUNCH                        " -ForegroundColor Green
Write-Host "=================================================================" -ForegroundColor DarkGreen
Write-Host ""
Write-Host "  ROYAL INDIAN STAYS" -ForegroundColor Yellow
Write-Host "  Local:" -ForegroundColor Cyan
Write-Host "  http://localhost:$Port" -ForegroundColor White
Write-Host ""
Write-Host "  LAN (Mobile on same Wi-Fi):" -ForegroundColor Cyan
Write-Host "  http://${lanIp}:$Port" -ForegroundColor White
Write-Host ""
Write-Host "  Mobile QR Code (Scan with Mobile Camera):" -ForegroundColor Yellow
$mobileUrl = "http://${lanIp}:$Port"
try {
    $qrGenCmd = "import sys, qrcode; sys.stdout.reconfigure(encoding='utf-8'); qr = qrcode.QRCode(error_correction=qrcode.constants.ERROR_CORRECT_H, border=2); qr.add_data('$mobileUrl'); qr.print_ascii(invert=True); hi_res = qrcode.QRCode(error_correction=qrcode.constants.ERROR_CORRECT_H, box_size=16, border=4); hi_res.add_data('$mobileUrl'); img = hi_res.make_image(fill_color='black', back_color='white'); img.save('mobile-qr.png')"
    python -c $qrGenCmd 2>$null
    if (Test-Path "$ScriptDir\mobile-qr.png") {
        Write-Host "  -> Ultra-Clear QR Code Image saved to: $ScriptDir\mobile-qr.png" -ForegroundColor Green
    }
} catch {}
Write-Host ""
Write-Host "  Public:" -ForegroundColor Cyan
Write-Host "  $publicUrl" -ForegroundColor White
Write-Host ""
Write-Host "=================================================================" -ForegroundColor DarkGreen
Write-Host "Starting Spring Boot application (Press Ctrl+C to stop)..." -ForegroundColor Gray
Write-Host ""

# Start the Spring Boot application
$jarFile = $targetJars | Select-Object -Last 1
if ($jarFile -and (Test-Path $jarFile.FullName)) {
    Write-Host "Running executable JAR: $($jarFile.Name)" -ForegroundColor DarkGray
    java -jar $jarFile.FullName --server.port=$Port
} else {
    $mvnCmd = if (Test-Path "$ScriptDir\mvnw.cmd") { "$ScriptDir\mvnw.cmd" } else { "mvn" }
    & $mvnCmd spring-boot:run "-Dspring-boot.run.arguments=--server.port=$Port"
}

Write-Host ""
Write-Host "Server process terminated." -ForegroundColor Yellow
Read-Host "Press Enter to exit..."
