# 🎌 Zenenation — Anime E-Commerce Platform

> Premium anime figures, katanas, apparel and collectibles — built with Spring Boot + React.

![Java](https://img.shields.io/badge/Java-17-orange)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2.5-green)
![React](https://img.shields.io/badge/React-18-blue)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-16-blue)
![Vite](https://img.shields.io/badge/Vite-5-purple)

---

## 📁 Project Structure

```
ZeneNation_Fullstack/
├── backend/        → Spring Boot REST API (Java 17)
└── frontend/       → React + Vite SPA
```

---

## ⚡ Quick Start

### Prerequisites

| Tool | Version |
|------|---------|
| Java | 17+ |
| Maven | 3.9+ (or use `mvnw`) |
| Node.js | 18+ |
| PostgreSQL | 14+ |

---

## 🗄️ Database Setup

Create a PostgreSQL database:

```sql
CREATE DATABASE ecommerce_db;
```

> Flyway will automatically create all tables on first startup. No manual SQL needed.

---

## 🔧 Backend Setup

### 1. Navigate to backend folder

```bash
cd backend
```

### 2. Create your `.env` file

Create a file named `.env` in the `backend/` folder:

```env
# ── Database ──────────────────────────────────────────────────────────────────
DB_URL=jdbc:postgresql://localhost:5432/ecommerce_db
DB_USERNAME=postgres
DB_PASSWORD=your_postgres_password

# ── JWT ───────────────────────────────────────────────────────────────────────
# Generate with: openssl rand -hex 64
JWT_SECRET=your_minimum_64_char_secret_key_here

# ── Admin Account ─────────────────────────────────────────────────────────────
ADMIN_EMAIL=admin@zenenation.com
ADMIN_PASSWORD=YourStrongPassword@123

# ── Google OAuth2 ─────────────────────────────────────────────────────────────
# Get from: https://console.cloud.google.com → APIs & Services → Credentials
# Authorized redirect URI: http://localhost:8080/login/oauth2/code/google
GOOGLE_CLIENT_ID=your_google_client_id.apps.googleusercontent.com
GOOGLE_CLIENT_SECRET=your_google_client_secret

# ── Gmail SMTP ────────────────────────────────────────────────────────────────
# Use Gmail App Password — NOT your account password
# Generate at: https://myaccount.google.com/apppasswords
MAIL_USERNAME=your_gmail@gmail.com
MAIL_PASSWORD=your_16_char_app_password

# ── Cloudinary ────────────────────────────────────────────────────────────────
# Get from: https://cloudinary.com/console
CLOUDINARY_CLOUD_NAME=your_cloud_name
CLOUDINARY_API_KEY=your_api_key
CLOUDINARY_API_SECRET=your_api_secret

# ── Razorpay ──────────────────────────────────────────────────────────────────
# Get from: https://dashboard.razorpay.com/app/keys
# Use test keys (rzp_test_...) for development
RAZORPAY_KEY_ID=rzp_test_your_key_id
RAZORPAY_KEY_SECRET=your_key_secret
RAZORPAY_WEBHOOK_SECRET=your_webhook_secret
```

> **Note:** `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `JWT_SECRET`, `ADMIN_EMAIL`, and `ADMIN_PASSWORD` are **required**. Everything else is optional for basic local testing (emails and payments won't work without them).

### 3. Run the backend

```bash
# Windows
.\mvnw.cmd spring-boot:run

# Mac / Linux
./mvnw spring-boot:run
```

Backend starts at → **http://localhost:8080**

Swagger UI → **http://localhost:8080/swagger-ui.html**

---

## 🎨 Frontend Setup

### 1. Navigate to frontend folder

```bash
cd frontend
```

### 2. Install dependencies

```bash
npm install
```

### 3. Create your `.env` file

Create a file named `.env` in the `frontend/` folder:

```env
# Backend API URL
VITE_API_BASE_URL=http://localhost:8080

# Razorpay test key (get from dashboard.razorpay.com/app/keys)
VITE_RAZORPAY_KEY_ID=rzp_test_your_key_id
```

### 4. Run the frontend

```bash
npm run dev
```

Frontend starts at → **http://localhost:5173**

---

## 🔑 Default Admin Account

After first startup, an admin account is created automatically:

```
Email:    admin@zenenation.com      ← or whatever you set in ADMIN_EMAIL
Password: YourStrongPassword@123   ← or whatever you set in ADMIN_PASSWORD
```

Admin panel → **http://localhost:5173/admin**

---

## ✨ Features

| Feature | Description |
|---------|-------------|
| 🛍️ Product Catalog | Categories, search, filters, pagination |
| 🛒 Cart & Checkout | Full checkout with address management |
| 💳 Payments | Razorpay — UPI, Cards, NetBanking |
| 🚀 Preorder System | 50% now / 100% upfront payment options |
| 🎁 Welcome Coupons | Auto-generated on registration |
| ⭐ Reviews | Verified purchase reviews with star ratings |
| 🏆 Rewards | Earn points on delivery, redeem at checkout |
| 📢 Announcements | Admin broadcasts with email blast |
| 🔐 Google OAuth | Login with Google |
| 📧 Email Notifications | Order confirm, password reset, abandoned cart |
| 🎌 Landing Page | Animated anime-themed landing |
| 📱 Mobile Responsive | Full mobile support |

---

## 🏗️ Tech Stack

### Backend
- **Java 17** + **Spring Boot 3.2.5**
- **Spring Security** + JWT Authentication
- **Spring Data JPA** + Hibernate
- **PostgreSQL** + Flyway migrations
- **Cloudinary** — image storage
- **Razorpay** — payment gateway
- **JavaMail** — SMTP email

### Frontend
- **React 18** + **Vite**
- **React Router v6**
- **Axios** — HTTP client
- **React Hot Toast** — notifications
- **React Icons** — icon library
- **Three.js** — 3D landing page

---

## 📡 API Overview

| Base URL | Description |
|----------|-------------|
| `/api/v1/auth/**` | Register, login, refresh token |
| `/api/v1/products/**` | Product catalog |
| `/api/v1/categories/**` | Categories |
| `/api/v1/cart/**` | Cart management |
| `/api/v1/orders/**` | Order placement & history |
| `/api/v1/payments/**` | Razorpay verification |
| `/api/v1/coupons/**` | Coupon validation |
| `/api/v1/rewards/**` | Reward wallet |
| `/api/v1/reviews/**` | Product reviews |
| `/api/v1/admin/**` | Admin dashboard |

Full API docs → **http://localhost:8080/swagger-ui.html**

---

## 🚀 Deployment

| Service | Platform |
|---------|----------|
| Frontend | Vercel |
| Backend | Render (Docker) |
| Database | Neon PostgreSQL |
| Images | Cloudinary |

### Production environment variables

**Backend (Render):** Same as local `.env` but with production DB URL from Neon and production Cloudinary/Razorpay keys.

**Frontend (Vercel):**
```env
VITE_API_BASE_URL=https://your-backend.onrender.com
VITE_RAZORPAY_KEY_ID=rzp_live_your_live_key
```

---

## 🐛 Common Issues

**Backend won't start**
- Check PostgreSQL is running
- Verify `.env` file exists in `backend/` folder
- Make sure `JWT_SECRET` is at least 64 characters

**CORS errors**
- Add `FRONTEND_URL=https://your-frontend.vercel.app` to Render env vars

**Flyway migration error**
- Run in pgAdmin: `DELETE FROM flyway_schema_history WHERE success = false;`
- Restart backend

**Landing page keeps showing**
- Clear `localStorage` in browser: `localStorage.removeItem('zn_skip_landing')`

---

## 📂 Key Files

```
backend/
├── src/main/resources/
│   ├── application.yml          → Main config (env vars)
│   └── db/migration/            → Flyway SQL migrations (V1–V17)
└── Dockerfile                   → For Render deployment

frontend/
├── src/
│   ├── api/                     → Axios API calls
│   ├── context/                 → Auth + Cart context
│   ├── pages/                   → All page components
│   └── components/              → Shared components
└── vercel.json                  → SPA routing fix for Vercel
```

---

## 👥 Team

Built with ❤️ by the Zenenation team.
