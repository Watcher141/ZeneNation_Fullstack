# ZeneNation Production Architecture & Maintenance Manual

> **Document Version:** 1.0  
> **Last Updated:** November 2026  
> **Target Audience:** Developers, DevOps Engineers, System Administrators & AI Agents  
> **Production URL:** [https://zenenation.in](https://zenenation.in) & [https://www.zenenation.in](https://www.zenenation.in)

---

## 1. System & Architecture Overview

### 1.1 VPS Specifications
- **Operating System:** Ubuntu 24.04.4 LTS (x86_64)
- **Public IPv4:** `109.122.56.213`
- **Hardware:** 2 vCPU, 2 GB RAM, 30 GB SSD
- **Virtual Memory:** 4 GB Swap file located at `/swapfile` (protects JVM and build processes from OOM kill)
- **Deployment Root Directory:** `/var/www/zenenation`

### 1.2 High-Level Architecture Diagram
```
                           Internet Traffic (HTTP:80 / HTTPS:443)
                                          │
                                          ▼
                       ┌──────────────────────────────────────┐
                       │       Host Nginx Reverse Proxy       │
                       │  (SSL Termination via Let's Encrypt) │
                       └──────┬────────────────────────┬──────┘
                              │                        │
            "/*" (Static SPA) │                        │ "/api/*", "/oauth2/*"
                              ▼                        ▼
       ┌──────────────────────────────┐    ┌──────────────────────────────┐
       │   React 19 Frontend Build    │    │ Spring Boot 3 Backend (Java) │
       │ (/var/www/zenenation/dist)   │    │  Docker Container (:8080)    │
       └──────────────────────────────┘    └──────────────┬───────────────┘
                                                          │
                                         Port 5432 (JDBC) │
                                                          ▼
                                           ┌──────────────────────────────┐
                                           │  PostgreSQL 16 DB Container  │
                                           │ Volume: zenenation_postgres  │
                                           └──────────────────────────────┘
```

### 1.3 Port Allocations & Network Topology
| Service | Internal Port | Exposed / Bound To | Management Type |
| :--- | :--- | :--- | :--- |
| **Nginx Web Server** | `80`, `443` | `0.0.0.0` (Public) | Host Systemd service (`systemctl`) |
| **Spring Boot Backend** | `8080` | `127.0.0.1:8080` (Localhost only) | Docker Compose (`zenenation-backend`) |
| **PostgreSQL 16** | `5432` | `127.0.0.1:5432` (Localhost only) | Docker Compose (`zenenation-postgres`) |

---

## 2. Directory Structure & File Mapping

```
/var/www/zenenation/                       # Root project directory on VPS
├── .env                                  # Real production secrets (NEVER commit to git)
├── .env.example                          # Sanitized template for environment variables
├── .gitignore                            # Excludes .env, build output, targets
├── docker-compose.yml                    # Multi-container orchestration (DB + Backend)
├── deploy.sh                             # Automated 1-command deployment script
├── setup-vps.sh                          # Initial VPS initialization script
├── DEPLOYMENT_MANUAL.md                  # This technical manual
│
├── backend/                              # Spring Boot 3 Java Application
│   ├── Dockerfile                        # Multi-stage Docker build (Maven compile + JRE runtime)
│   ├── pom.xml                           # Maven project descriptors & dependencies
│   ├── .mvn/ & mvnw                      # Maven wrapper binaries
│   └── src/                              # Java source code
│       └── main/
│           ├── java/com/zenenation/      # Controllers, Entities, Services, Security
│           └── resources/
│               ├── application.yml       # Spring configuration (Datasource, JWT, OAuth2)
│               └── db/migration/         # Flyway versioned SQL migrations (V1 to V20)
│
├── frontend/                             # React 19 + Vite Application
│   ├── .env                              # Production frontend env (VITE_API_BASE_URL, etc.)
│   ├── .env.example                      # Sanitized frontend env template
│   ├── package.json & package-lock.json  # NPM dependencies and build scripts
│   ├── vite.config.js                    # Vite bundler configuration
│   ├── dist/                             # Compiled production static bundle (served by Nginx)
│   └── src/                              # React components, pages, context, and API axios instances
│
└── nginx/                                # Web server reverse proxy configuration
    └── zenenation.in.conf                # Reference config (installed at /etc/nginx/sites-available/)
```

---

## 3. How the Pieces Work Together

### 3.1 Nginx Routing Logic
The configuration at `/etc/nginx/sites-available/zenenation.in` governs all web traffic:
- **`location /`**: Serves static HTML/JS/CSS from `/var/www/zenenation/frontend/dist`. Employs `try_files $uri $uri/ /index.html;` to allow client-side routing (React Router) to function on page refreshes without 404 errors.
- **`location /api`**: Proxies REST API calls to `http://127.0.0.1:8080`.
- **`location /oauth2/authorization`**: Routes initial Google OAuth2 login triggers to Spring Boot.
- **`location /login/oauth2`**: Routes the Google OAuth2 authentication callback authorization code to Spring Boot.
- **`/oauth2/callback`**: Handled by **React** (`OAuth2CallbackPage.jsx`), which grabs tokens and saves them in `localStorage`.

### 3.2 Spring Boot Backend Runtime
- Runs inside a minimal container (`eclipse-temurin:17-jre-alpine`).
- Capped with `JAVA_OPTS="-Xms256m -Xmx512m -XX:+UseG1GC"` and Docker limit `800M` to ensure server memory stability.
- Reads database credentials, JWT keys, and third-party API credentials dynamically from `/var/www/zenenation/.env`.

### 3.3 PostgreSQL Persistence
- Database files reside inside Docker volume `zenenation_postgres_data` mapped to `/var/lib/postgresql/data`.
- Data persists across container restarts, image upgrades, and system reboots.

---

## 4. Maintenance & Update Workflows

### Scenario A: Fast Re-Deploy Everything (Frontend + Backend)
Whenever new commits are pushed to the `main` branch on GitHub:

1. SSH into the VPS:
   ```bash
   ssh root@109.122.56.213
   ```
2. Run the automated deployment script:
   ```bash
   cd /var/www/zenenation
   ./deploy.sh
   ```
This script automatically pulls from Git, rebuilds Docker containers, compiles the React frontend, and reloads Nginx.

---

### Scenario B: Updating Frontend Only
If you only changed UI, styling, or frontend pages:

```bash
cd /var/www/zenenation
git pull origin main

cd frontend
npm ci
npm run build

# Nginx serves the new dist immediately (no restart strictly required, but reload is good practice)
sudo systemctl reload nginx
```

---

### Scenario C: Updating Backend Only
If you only changed Java controllers, services, or entities:

```bash
cd /var/www/zenenation
git pull origin main

# Rebuild and restart the backend container
docker compose down backend
docker compose up -d --build backend

# Verify health
docker compose ps
docker logs -f zenenation-backend
```

---

### Scenario D: Changing Environment Variables (`.env`)
If you change database passwords, API keys (Razorpay, Cloudinary, Resend), or secrets:

1. **Edit root `.env`**:
   ```bash
   nano /var/www/zenenation/.env
   ```
2. **Apply to containers**:
   ```bash
   cd /var/www/zenenation
   docker compose up -d
   ```
   *(Docker will detect the changed environment variables and recreate the container with zero data loss).*

3. **If changing Frontend `.env` (`/var/www/zenenation/frontend/.env`)**:
   Because Vite bakes `VITE_*` variables into the static javascript bundle at compile-time:
   ```bash
   cd /var/www/zenenation/frontend
   nano .env
   npm run build
   sudo systemctl reload nginx
   ```

---

## 5. Database Backup & Restore Guide

### 5.1 Creating a Database Backup (Export)
To create a complete SQL backup of PostgreSQL:

```bash
# Dump into a timestamped file
docker exec -t zenenation-postgres pg_dump -U postgres ecommerce_db > /var/www/zenenation/backup_$(date +%Y%m%d_%H%M%S).sql
```

To download the backup file to your local computer:
```powershell
# Run from local PowerShell
scp root@109.122.56.213:/var/www/zenenation/backup_*.sql C:\Users\YourName\Downloads\
```

### 5.2 Restoring a Database Backup (Import)
If you need to restore or replace data from a `.sql` file:

```bash
# Pipe the SQL file into PostgreSQL inside the container
docker exec -i zenenation-postgres psql -U postgres -d ecommerce_db < /path/to/backup.sql

# Restart backend to re-sync cache and Hibernate session
docker compose restart backend
```

---

## 6. System Monitoring & Troubleshooting Cheat Sheet

### 6.1 Checking Service Health
```bash
# Check Docker container status (Should show "Up" and "Healthy")
docker compose -f /var/www/zenenation/docker-compose.yml ps

# View live backend logs
docker logs -f --tail 50 zenenation-backend

# View database logs
docker logs -f --tail 50 zenenation-postgres

# Check Nginx service status
systemctl status nginx

# Test Nginx configuration for syntax errors
nginx -t
```

### 6.2 Resource & Memory Monitoring
```bash
# Check RAM & Swap usage (Ensure swap is active and RAM is not pinned at 100%)
free -h

# Check Docker container memory and CPU usage
docker stats --no-stream

# Check disk space usage
df -h /
```

### 6.3 SSL Certificate Management
Certbot is configured to auto-renew via systemd timer. To manually test or force renewal:

```bash
# Dry run test (checks that renew will succeed)
sudo certbot renew --dry-run

# Re-issue or expand certificates manually
sudo certbot --nginx --expand -d zenenation.in -d www.zenenation.in --agree-tos -m zenenationstore@gmail.com
```

### 6.4 Common Error Diagnoses
| Symptom | Root Cause | Solution |
| :--- | :--- | :--- |
| `502 Bad Gateway` on `/api/*` | Backend container is stopped or restarting | Run `docker logs --tail 50 zenenation-backend` to view stack trace. Check if missing columns exist in DB. |
| `ERR_CONNECTION_REFUSED` | Nginx service is stopped or firewall blocked ports | Run `systemctl restart nginx` and check `ufw status`. |
| Cross-Origin (CORS) header errors | Origin mismatch or duplicate headers | Verify Nginx config doesn't duplicate headers that Spring Boot already provides. |
| Google OAuth `redirect_uri_mismatch` | Domain not registered in Google Cloud Console | Ensure `https://zenenation.in/login/oauth2/code/google` is added to Authorized Redirect URIs in Google Cloud Console. |

