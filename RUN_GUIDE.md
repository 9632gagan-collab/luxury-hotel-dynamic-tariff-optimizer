# ROYAL INDIAN STAYS — LOCAL RUN & OPERATIONS GUIDE

Welcome to **Royal Indian Stays** (Luxury Hotel Dynamic Tariff Optimizer & Guest Preference Matcher).  
This guide explains how to start, stop, rebuild, test on mobile/LAN, and manage the system locally.

---

## 🚀 Quick Start (Local Development)

### Option 1: Double-Click Launcher (Easiest)
1. Open the project folder in Windows File Explorer:
   `c:\Users\gagan\Downloads\Java project`
2. **Double-click `START-ROYAL-INDIAN-STAYS.bat`**
3. The launcher will automatically:
   - Check Java 17+
   - Verify PostgreSQL / embedded H2 database
   - Clear any stale processes on port 8080
   - Detect your current Wi-Fi/LAN IP
   - Launch the Spring Boot server
   - Display active URLs in the terminal

---

### Option 2: PowerShell Command Line

Open Windows PowerShell in the project directory and run:

```powershell
.\start-royal-indian-stays.ps1
```

To force a fresh Maven rebuild before launching:
```powershell
.\start-royal-indian-stays.ps1 -Rebuild
```

To start on a custom port (e.g. 8081):
```powershell
.\start-royal-indian-stays.ps1 -Port 8081
```

---

## 🛑 How to Stop the Application

### Option A: Double-Click Stopper
- **Double-click `STOP-ROYAL-INDIAN-STAYS.bat`**

### Option B: PowerShell Command
```powershell
.\stop-royal-indian-stays.ps1
```

### Option C: Console Keypress
- In the active terminal window running the server, press `Ctrl + C`.

---

## 🌐 URLs & Access Endpoints

| Environment | Access URL | Description |
| :--- | :--- | :--- |
| **Local PC** | `http://localhost:8080` | Access directly from your desktop browser. |
| **LAN / Mobile Testing** | `http://<YOUR-LAN-IP>:8080` *(e.g. `http://192.168.1.36:8080`)* | Access from any smartphone or tablet connected to the same Wi-Fi network. |
| **Health Check** | `http://localhost:8080/health` | Application status & uptime heartbeat. |

> **Tip:** The `start-royal-indian-stays.ps1` script dynamically detects and prints your exact Wi-Fi/Ethernet IPv4 address every time it starts.

---

## 📱 Mobile & PWA Access on Phone

1. Ensure your smartphone is connected to the **same Wi-Fi network** as this computer.
2. Open Safari (iOS) or Chrome (Android) on your phone.
3. Navigate to the LAN URL displayed by the startup script:
   `http://<YOUR-LAN-IP>:8080`
4. **Install as App (PWA):**
   - **Android Chrome:** Tap the browser menu `⋮` → **Install App** or **Add to Home screen**.
   - **iPhone Safari:** Tap the **Share button** (square with arrow) → **Add to Home Screen**.
   - The Royal Indian Stays luxury icon and title will appear on your phone home screen like a native mobile app.

---

## 🔍 How to Check Application Logs

- **Console Output:** The active PowerShell window streams all real-time application logs, including HTTP requests, database transactions, and tariff optimizer operations.
- **Log Level Adjustment:** Production and development logging levels can be configured in `src/main/resources/application.properties`.

---

## ⚠️ What to Do if Port 8080 is in Use

The `start-royal-indian-stays.ps1` script **automatically resolves port collisions** by stopping prior Java processes bound to port 8080.

If you ever need to manually free port 8080:
```powershell
.\stop-royal-indian-stays.ps1
```
Or specify an alternative port:
```powershell
.\start-royal-indian-stays.ps1 -Port 8090
```

---

## 🔨 How to Rebuild the Project

To compile and package a fresh production JAR package:

Using Maven Wrapper:
```cmd
.\mvnw.cmd clean package -DskipTests
```

Or using the PowerShell launcher:
```powershell
.\start-royal-indian-stays.ps1 -Rebuild
```

---

## 🔄 Running After Windows Restart

1. Power on your computer and log in to Windows.
2. Double-click `START-ROYAL-INDIAN-STAYS.bat`.
3. The database and application state will automatically initialize and become accessible.
