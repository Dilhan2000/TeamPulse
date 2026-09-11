# TeamPulse — Weekly Report Generator & Team Dashboard

A full-stack enterprise web application for managing weekly engineering reports, submissions, review workflows, and executive team performance dashboards.

---

## Tech Stack

| Layer          | Technology                                                 |
|----------------|------------------------------------------------------------|
| **Frontend**   | Angular 17/18, Angular Material, SCSS, Chart.js / ng2-charts |
| **Backend**    | Java 21 (LTS), Spring Boot 3.3.x, Spring Security, Gradle   |
| **Database**   | MySQL 8 (InnoDB, utf8mb4)                                  |
| **Migrations** | Flyway (automated on startup)                              |
| **Auth**       | JWT Authentication (stateless, secure HttpOnly cookies)    |

---

## Prerequisites

Ensure the following tools are installed on your system before proceeding:

- **Java 21 (LTS)**: Check with `java -version`
- **Node.js (v18 or v20 LTS)** & **npm**: Check with `node -v` and `npm -v` (see `.nvmrc` for version)
- **MySQL 8.x**: Running locally on port `3306`
- **Gradle**: Wrapper included (`./gradlew`) — no standalone install required

---

## Quick Start (Automated with `run.sh`)

Automated `run.sh` scripts are provided to automatically install all dependencies and start the servers.

### 1. Start the Backend
In a terminal window:
```bash
cd backend
./run.sh
```
*Or from the repository root:* `./run.sh backend`

> This script checks Java 21, loads environment variables from `.env`, installs/compiles Gradle dependencies, and starts the Spring Boot backend on `http://localhost:8080`.

### 2. Start the Frontend
In a second terminal window:
```bash
cd frontend
./run.sh
```
*Or from the repository root:* `./run.sh frontend`

> This script checks Node.js, runs `npm install` if dependencies are missing, and launches the Angular development server on `http://localhost:4200`.

---

## Detailed Step-by-Step Setup Instructions

### 1) Running the Database

1. Ensure your local MySQL 8 server is running (default port `3306`).
2. Log into MySQL as root or an administrative user:
   ```bash
   mysql -u root -p
   ```
3. Create the database and dedicated application user:
   ```sql
   CREATE DATABASE IF NOT EXISTS teamplan CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
   CREATE USER IF NOT EXISTS 'teamplan_admin'@'localhost' IDENTIFIED BY 'Abc@123#$';
   GRANT ALL PRIVILEGES ON teamplan.* TO 'teamplan_admin'@'localhost';
   FLUSH PRIVILEGES;
   EXIT;
   ```
4. Copy the environment configuration template in the project root and update the password if needed:
   ```bash
   cp .env.example .env
   ```
   Edit the `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USER`, or `DB_PASSWORD` values in `.env` if your local MySQL configuration differs. The `.env` file is ignored by Git and must not be committed.
5. **Database Migrations:** Flyway runs automatically on application startup. Schema tables and initial seed data (migrations `V1` through `V8`) are applied seamlessly without needing manual SQL import scripts.

---

### 2) Installing Dependencies

If you prefer to install dependencies manually without using `run.sh`:

#### Backend Dependencies
```bash
cd backend
./gradlew build -x test
```
*Downloads all required Spring Boot starters, MySQL driver, Flyway, and JWT dependencies.*

#### Frontend Dependencies
```bash
cd frontend
npm install
```
*Installs Angular core, Angular Material, CDK, Chart.js, ng2-charts, and dev dependencies.*

---

### 3) Running the Backend

Ensure the database is running, then execute:

```bash
# Option A: Using the automated script (recommended)
cd backend
./run.sh

# Option B: Using Gradle directly
cd backend
./gradlew bootRun
```

- Backend API: `http://localhost:8080/api`
- Health check: `http://localhost:8080/actuator/health`

---

### 4) Running the Frontend

Once the backend is running, launch the Angular frontend:

```bash
# Option A: Using the automated script (recommended)
cd frontend
./run.sh

# Option B: Using npm directly
cd frontend
npm start
```

- Open your browser and navigate to: **`http://localhost:4200`**

---

## Default Credentials & Roles

The system comes pre-seeded with accounts for testing role-based features:

| Role          | Email                        | Password       | Dashboard / Features |
|---------------|------------------------------|----------------|----------------------|
| **Admin**     | `admin@weeklyreport.local`   | `ChangeMe123!` | User Management, Approvals, System Config |
| **Manager**   | `manager@weeklyreport.local` | `ChangeMe123!` | Manager Dashboard, Team Reports Review Queue, Projects |
| **Team Member** | `member@weeklyreport.local`  | `ChangeMe123!` | Weekly Report Creation, Drafts, History |

---

## Running Tests

### Backend Tests
Integration and unit tests verify authentication, report lifecycles, and dashboard metric calculations.

```bash
cd backend
./gradlew test
```

### Frontend Tests
Angular Jasmine/Karma unit tests for all components, guards, and services:

```bash
cd frontend
npm test -- --watch=false
```
