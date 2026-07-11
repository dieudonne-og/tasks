# TaskMS Developer Guide

Technical reference for building, running, and extending **TaskMS** — an HR task-management
system with AI-based deadline prediction.

---

## 1. TL;DR

```bash
# Prerequisites: Java 17+ and Git installed
git clone <repo-url>
cd tasks
./mvnw spring-boot:run          # Windows: mvnw.cmd spring-boot:run
# App on http://localhost:8080  (login admin@uok.ac.rw / admin123)
```

---

## 2. Tech stack

| Layer | Technology |
|-------|------------|
| Language | Java 17 |
| Framework | Spring Boot 3.3.5 (Web, Data JPA, Security, Validation) |
| Auth | JWT (jjwt) — stateless bearer tokens |
| Database | H2 in-memory (dev) · MySQL (via `mysql` profile) |
| ML | [Smile](https://haifengl.github.io/) — Random Forest + Linear Regression |
| Build | Maven (wrapper included: `mvnw` / `mvnw.cmd`) |
| Frontend | Static HTML/CSS/vanilla JS served from `src/main/resources/static` |

---

## 3. Project structure

```
src/main/java/rw/ac/uok/taskms/
├── TaskmsApplication.java        # Spring Boot entry point
├── common/                       # Shared errors + global exception handler
│   ├── ApiError, BadRequestException, NotFoundException
│   └── GlobalExceptionHandler
├── config/
│   ├── CorsConfig                # CORS rules
│   ├── DataSeeder                # Seeds users, task types, historical tasks on startup
│   └── SecurityConfig            # URL authorization + filter chain
├── security/
│   ├── AuthController            # /api/auth/login, /api/auth/me
│   ├── JwtService                # Sign/verify tokens
│   ├── JwtAuthFilter             # Reads Bearer token per request
│   └── AppUserDetailsService     # Loads user for Spring Security
├── user/                         # User, Role, CRUD (admin only)
├── task/                         # Task, TaskType, status/complexity enums, CRUD
├── prediction/                   # ML: MlPredictionService, predictors, metrics
├── dashboard/                    # Aggregated stats
└── workload/                     # Per-assignee load + assignment suggestions

src/main/resources/
├── application.yml               # Default (H2) config
├── application-mysql.yml         # MySQL profile
└── static/                       # index/board/dashboard/admin .html + js/ + css/
```

---

## 4. Roles & authorization

Three roles (`user/Role.java`): `ADMIN`, `HR_MANAGER`, `HR_OFFICER`.

Authorization rules (`config/SecurityConfig.java`):

| Path | Access |
|------|--------|
| `/api/auth/login` | public |
| `/`, static pages, `/css/**`, `/js/**` | public |
| `/h2-console/**` | public (dev only — lock down in prod) |
| `GET /api/task-types` | any authenticated user |
| `/api/task-types/**` (write) | `ADMIN` |
| `/api/users/**` | `ADMIN` |
| `/api/models/retrain` | `ADMIN` |
| `/api/dashboard/**` | `ADMIN`, `HR_MANAGER` |
| `/api/workload/**` | `ADMIN`, `HR_MANAGER` |
| everything else | authenticated |

Auth is **stateless JWT**. Login returns a token; send it as
`Authorization: Bearer <token>` on every subsequent call. Tokens expire after
`taskms.jwt.expiration-minutes` (default 480 = 8h).

---

## 5. API reference

Base URL: `http://localhost:8080`

### Auth
| Method | Path | Body / notes |
|--------|------|--------------|
| POST | `/api/auth/login` | `{ "email", "password" }` → `{ token, userId, fullName, email, role }` |
| GET | `/api/auth/me` | current user (requires token) |

### Tasks (`/api/tasks`)
| Method | Path | Purpose |
|--------|------|---------|
| GET | `/api/tasks` | list tasks (scoped by role) |
| GET | `/api/tasks/{id}` | one task |
| POST | `/api/tasks` | create (returns AI prediction inline) |
| PUT | `/api/tasks/{id}` | update |
| PATCH | `/api/tasks/{id}/status` | change status |
| PATCH | `/api/tasks/{id}/complete` | mark done + record actual duration (triggers retrain counter) |
| GET | `/api/tasks/{id}/prediction` | prediction detail for a task |
| DELETE | `/api/tasks/{id}` | delete |

### Task types (`/api/task-types`)
GET (any auth) · POST/PUT/DELETE (ADMIN).

### Users (`/api/users`) — ADMIN only
GET list, GET `/{id}`, POST, PUT `/{id}`, DELETE `/{id}`.

### Dashboard / Workload / Models
| Method | Path | Access |
|--------|------|--------|
| GET | `/api/dashboard` | ADMIN, HR_MANAGER |
| GET | `/api/workload` | ADMIN, HR_MANAGER |
| GET | `/api/workload/suggestions` | ADMIN, HR_MANAGER |
| GET | `/api/models/metrics` | ADMIN, HR_MANAGER |
| POST | `/api/models/retrain` | ADMIN |

### Quick curl example
```bash
TOKEN=$(curl -s -X POST http://localhost:8080/api/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"email":"admin@uok.ac.rw","password":"admin123"}' | grep -oP '"token":"\K[^"]+')

curl -s http://localhost:8080/api/dashboard -H "Authorization: Bearer $TOKEN"
```

---

## 6. The AI deadline predictor

Lives in `prediction/MlPredictionService.java`.

**Idea:** learn how long each task type takes and predict duration for new tasks.

- **Features** (`featureRow`): task type, assignee, complexity (encoded numerically).
- **Models:** trains both a **Linear Regression** (OLS) and a **Random Forest** (Smile).
  On each training run it evaluates both on a held-out test split and picks the one with the
  lower RMSE as the **active** model (`rfWins = rfEval.rmse <= olsEval.rmse`).
- **Metrics:** MAE, RMSE, R² per model — persisted to `ModelMetrics`, exposed via
  `/api/models/metrics`.
- **Cold start:** if fewer than `taskms.prediction.min-training-samples` (default 20)
  completed tasks exist, a `FallbackPredictor` returns a simple type-average estimate.
- **Auto-retrain:** every time a task is completed, a counter increments; once it reaches
  `taskms.prediction.retrain-after-new-completed` (default 10) the model retrains itself.
- **Output** (`PredictionResult`): predicted days + a lower/upper confidence band derived
  from the active model's residual RMSE, plus the model type used.

Tunable in `application.yml` under `taskms.prediction.*` and `taskms.workload.overload-factor`.

---

## 7. Configuration (`application.yml`)

| Key | Default | Meaning |
|-----|---------|---------|
| `server.port` | 8080 | HTTP port |
| `spring.datasource.*` | H2 in-mem | dev DB (wiped on restart) |
| `taskms.jwt.secret` | dev secret | **override in production** (≥32 chars, HS256) |
| `taskms.jwt.expiration-minutes` | 480 | token lifetime |
| `taskms.prediction.min-training-samples` | 20 | cold-start threshold |
| `taskms.prediction.retrain-after-new-completed` | 10 | auto-retrain trigger |
| `taskms.workload.overload-factor` | 1.3 | overload flag multiplier |

H2 console (dev): http://localhost:8080/h2-console — JDBC URL
`jdbc:h2:mem:taskms`, user `sa`, empty password.

> ⚠️ **Production:** override `taskms.jwt.secret`, disable/secure the H2 console, and switch
> to a persistent database (MySQL profile below).

---

## 8. Installation & run — Windows

Step-by-step for a Windows machine (no prior setup assumed).

### Step 1 — Install Java 17
1. Download a JDK 17 (e.g. [Eclipse Temurin 17](https://adoptium.net/temurin/releases/?version=17)).
2. Run the installer. Tick **"Set JAVA_HOME"** and **"Add to PATH"** if offered.
3. Verify in **Command Prompt** (press `Win`, type `cmd`, Enter):
   ```cmd
   java -version
   ```
   You should see `openjdk version "17..."`.

### Step 2 — Install Git (to get the code)
1. Download from [git-scm.com](https://git-scm.com/download/win) and install (defaults are fine).
2. Verify:
   ```cmd
   git --version
   ```

### Step 3 — Get the project
```cmd
git clone <repo-url>
cd tasks
```
*(Or unzip the project folder and `cd` into it.)*

### Step 4 — Run it (Maven wrapper — no separate Maven install needed)
```cmd
mvnw.cmd spring-boot:run
```
The first run downloads dependencies (a few minutes). When you see
`Started TaskmsApplication`, it's ready.

### Step 5 — Open the app
In your browser go to **http://localhost:8080/** and log in with
`admin@uok.ac.rw` / `admin123`.

### Step 6 — Stop the app
In the Command Prompt window, press **Ctrl + C**.

### Windows troubleshooting
| Problem | Fix |
|---------|-----|
| `'java' is not recognized` | Java not on PATH — reinstall JDK 17 and tick "Add to PATH", reopen cmd |
| `'mvnw.cmd' is not recognized` | You're not inside the project folder — `cd` into it |
| Port 8080 already in use | Close the other program, or set a new port: `mvnw.cmd spring-boot:run "-Dspring-boot.run.arguments=--server.port=9090"` |
| Firewall popup on first run | Allow access (needed to serve localhost) |
| Slow first build | Normal — it's downloading dependencies once; later runs are fast |

---

## 9. Installation & run — macOS / Linux

```bash
# Install Java 17 (example)
#   macOS:  brew install openjdk@17
#   Ubuntu: sudo apt install openjdk-17-jdk
java -version

git clone <repo-url>
cd tasks
./mvnw spring-boot:run           # first run downloads deps
# Ctrl+C to stop
```

---

## 10. Build, test, package

```bash
./mvnw test              # run tests            (Windows: mvnw.cmd test)
./mvnw clean package     # build runnable jar -> target/taskms-1.0.0.jar
java -jar target/taskms-1.0.0.jar
```

---

## 11. Using MySQL instead of H2

A `mysql` Spring profile is provided (`application-mysql.yml`).

```bash
# ensure MySQL is running and a database exists, then:
./mvnw spring-boot:run -Dspring-boot.run.profiles=mysql
# or for the jar:
java -jar target/taskms-1.0.0.jar --spring.profiles.active=mysql
```
Set the DB URL / credentials in `application-mysql.yml` (or via environment variables /
`--spring.datasource.*` args). Unlike H2, MySQL data **persists** across restarts.

---

## 12. Seed data

`config/DataSeeder.java` runs on startup and creates:

| User | Email | Password | Role |
|------|-------|----------|------|
| System Administrator | `admin@uok.ac.rw` | `admin123` | ADMIN |
| HR Manager | `manager@uok.ac.rw` | `manager123` | HR_MANAGER |
| Alice Uwase | `alice@uok.ac.rw` | `officer123` | HR_OFFICER |
| Bob Mugisha | `bob@uok.ac.rw` | `officer123` | HR_OFFICER |
| Claire Ingabire | `claire@uok.ac.rw` | `officer123` | HR_OFFICER |

Plus HR task types and ~60 historical completed tasks so the ML model has data to train on
from the first launch.

> Because dev uses **H2 in-memory**, all data (including anything you create) resets on every
> restart. Switch to MySQL for persistence.

---

## 13. Extending the system

- **New endpoint:** add a controller under the relevant package; wire authorization in
  `SecurityConfig` if it needs a role restriction.
- **New task field:** update `task/Task.java` + `task/dto/TaskDtos.java`; if it should feed
  the model, add it to `featureRow` in `MlPredictionService`.
- **New ML feature/model:** extend `featureRow`/`COLUMNS`, or add a predictor implementing
  the predictor contract, and update the model-selection logic.

Keep changes surgical and match existing package conventions.
