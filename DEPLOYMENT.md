# ROYAL INDIAN STAYS — PRODUCTION DEPLOYMENT GUIDE

This document details the public HTTPS deployment architecture, portal routes, and step-by-step production deployment instructions for **Royal Indian Stays**.

---

## 🌐 Public URLs & Portal Routes

> **Deployment Status:** Pre-configured and packaged for deployment to **Railway** (or any container platform).  
> A public HTTPS domain is dynamically provisioned by Railway upon connecting your GitHub repository.

### Routes Directory (Pathnames Only — No Credentials)

- **Public Website:** `https://<your-railway-app>.up.railway.app/`
- **Admin Portal:** `/admin/dashboard`
- **Staff Portal:** `/staff/dashboard`
- **Guest Portal:** `/guest/dashboard`
- **Demo Mode Sandbox:** `/demo`
- **Public Hotel Search:** `/hotels`
- **Health Check Endpoint:** `/health`

*(Note: Real passwords are never committed to documentation or source code. Admin and staff credentials are configured through private environment variables in the Railway dashboard).*

---

## 🏗️ Production Architecture

```
                 PUBLIC INTERNET
                       ↓
              HTTPS (SSL / TLS 1.3)
                       ↓
               Railway Edge Router
           (Automatic SSL Termination)
                       ↓
     ROYAL INDIAN STAYS (Spring Boot 3.3.4)
     - Java 17 Container (Alpine Linux)
     - PWA Enabled (manifest.json + sw.js)
     - GZIP Compressed HTTP Delivery
     - Stateless / Secure Session Cookies
                       ↓
         Railway PostgreSQL Database
         (Private Internal Network)
```

---

## 📦 Deployment Files Provided

The codebase includes all configuration required for 1-click cloud deployment:

1. **`Dockerfile`**: Hardened, multi-stage Docker build utilizing Eclipse Temurin 17 with unprivileged user execution (`appuser`) and container health checks.
2. **`railway.toml`**: Railway deployment manifest with automated health check configuration and failure recovery.
3. **`application-prod.properties`**: Production configuration profile enabling reverse proxy headers, cookie flags (`secure=true`, `httpOnly=true`, `sameSite=lax`), database pooling, and template caching.
4. **`.env.example`**: Environment variable template containing parameter names.
5. **`HealthCheckController.java`**: Dedicated `/health` and `/actuator/health` endpoint for uptime monitoring.

---

## 🚀 Step-by-Step Railway Deployment Guide

Follow these simple steps to deploy Royal Indian Stays to Railway:

### Step 1: Push Codebase to GitHub
1. Create a repository on GitHub (e.g. `royal-indian-stays`).
2. Push your project code to GitHub:
   ```bash
   git init
   git add .
   git commit -m "Royal Indian Stays production release"
   git remote add origin https://github.com/<your-username>/royal-indian-stays.git
   git push -u origin main
   ```

### Step 2: Create Railway Project
1. Go to [https://railway.app/](https://railway.app/) and log in.
2. Click **"New Project"**.
3. Select **"Provision PostgreSQL"** to add a managed PostgreSQL instance.

### Step 3: Deploy the Spring Boot Application
1. In the same Railway project canvas, click **"New"** → **"GitHub Repo"**.
2. Select your `royal-indian-stays` repository.
3. Railway will detect the `Dockerfile` and `railway.toml` automatically.

### Step 4: Configure Environment Variables
In the Railway web dashboard for your Spring Boot service, go to the **Variables** tab and add:

| Variable Name | Value | Purpose |
| :--- | :--- | :--- |
| `SPRING_PROFILES_ACTIVE` | `prod` | Activates production profile |
| `DATABASE_URL` | `${{Postgres.DATABASE_URL}}` | Auto-references the Railway PostgreSQL database |
| `ADMIN_GAGAN_PASSWORD` | `<your-chosen-strong-password>` | Admin 1 credentials |
| `ADMIN_DARSHAN_PASSWORD` | `<your-chosen-strong-password>` | Admin 2 credentials |

*(Railway's internal reference `${{Postgres.DATABASE_URL}}` links your app to the database over Railway's private, encrypted network).*

### Step 5: Generate Public Domain
1. In your Spring Boot service settings on Railway, navigate to **Networking** → **Public Networking**.
2. Click **"Generate Domain"** (or add your custom domain with SSL).
3. Railway provides your public HTTPS URL:
   `https://<your-service-name>.up.railway.app`

---

## 📱 Mobile Phone Access & Testing

Once deployed:
1. Open any browser (Safari, Chrome, Firefox) on your smartphone.
2. Navigate to your public URL:
   `https://<your-service-name>.up.railway.app`
3. Tap **Add to Home Screen** to install Royal Indian Stays as a full-screen Progressive Web App (PWA).
