# TaskMS — Task Management System with AI-Based Deadline Prediction

A collaborative web application for the **Human Resource Department, University of Kigali**.
HR staff log tasks and record actual effort on a shared board; a supervised machine-learning
model trained on the department's own task history predicts how long each new task will take
(with a 95% confidence interval), warns when a task is likely to miss its deadline, and
suggests redistributing work when a staff member is predicted to be overloaded. A reporting
dashboard compares the accuracy of manual estimates against the model.

Implements the research proposal by **NIBAGWIRE SHALLON**.

## Stack

- Java 17, Spring Boot 3.3 (Web, Data JPA, Security, Validation)
- **H2** in-memory database for local dev/tests; **MySQL** for production (Spring profiles)
- **Smile** (`smile-core`) for the ML models: OLS **Linear Regression** and **Random Forest**
- JWT (jjwt) bearer authentication + BCrypt password hashing
- Vanilla HTML/CSS/JS frontend served as static resources (no external CDN)

## Running locally (H2, no database setup required)

```bash
./mvnw spring-boot:run
```

Open <http://localhost:8080> and sign in with a demo account.

### Demo accounts (seeded on first launch)

| Role        | Email                 | Password    |
|-------------|-----------------------|-------------|
| Admin       | admin@uok.ac.rw       | admin123    |
| HR Manager  | manager@uok.ac.rw     | manager123  |
| HR Officer  | alice@uok.ac.rw       | officer123  |
| HR Officer  | bob@uok.ac.rw         | officer123  |
| HR Officer  | claire@uok.ac.rw      | officer123  |

The database is seeded with the HR task types and **60 synthetic completed tasks**, so the
ML model is trained and the dashboard/predictions work from the first launch.

## Running with MySQL

Create (or let the app create) a `taskms` schema, then run with the `mysql` profile:

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=mysql
```

Adjust credentials in `src/main/resources/application-mysql.yml`. Tables are created via
Hibernate `ddl-auto=update`.

## Tests

```bash
./mvnw test
```

- `FallbackPredictorTest` — category-average baseline below the training threshold.
- `MlPredictionServiceTest` — both models train, metrics computed, one active model chosen,
  prediction falls within its confidence interval.
- `ApiIntegrationTest` — MockMvc slice: login/JWT, `/me`, task creation with prediction,
  and role enforcement (HR officer gets `403` on admin endpoints).

## How the four research objectives are satisfied

1. **Capture tasks, effort and deadlines on a shared board** — `task/` package: `Task`
   entity, CRUD REST API (`/api/tasks`), status and `complete` (actual-effort) flows, and
   the kanban board UI (`board.html`).
2. **Store the department's task history for learning** — completed tasks (`status = DONE`
   with recorded `actualDurationDays`) form the training set; `DataSeeder` bootstraps a
   realistic history.
3. **Predict deadlines, warn of delays, rebalance workload** — `prediction/` package:
   `MlPredictionService` trains and compares Linear Regression vs Random Forest (MAE, RMSE,
   R²), keeps the lower-RMSE model, and returns a 95% confidence interval; `FallbackPredictor`
   covers the cold-start limitation with category averages. `TaskService.isAtRisk` raises
   delay warnings; `WorkloadService` flags overloaded staff and suggests reassignments.
4. **Reporting dashboard** — `dashboard/` package: `DashboardService` aggregates estimate-vs-
   AI accuracy, on-time completion rate, task counts, per-assignee workload and model metrics,
   visualised in `dashboard.html`.

## Key endpoints

| Method | Path | Role |
|--------|------|------|
| POST | `/api/auth/login` | public |
| GET | `/api/auth/me` | any |
| GET/POST/PUT/DELETE | `/api/tasks` | officer (own) / manager / admin |
| PATCH | `/api/tasks/{id}/status`, `/api/tasks/{id}/complete` | as above |
| GET | `/api/tasks/{id}/prediction` | as above |
| GET | `/api/task-types` | any; write = admin |
| GET | `/api/workload`, `/api/workload/suggestions` | manager / admin |
| GET | `/api/dashboard` | manager / admin |
| GET | `/api/models/metrics` | manager / admin |
| POST | `/api/models/retrain` | admin |
| GET/POST/PUT/DELETE | `/api/users` | admin |

## Configuration

See `src/main/resources/application.yml`:

- `taskms.jwt.secret` / `expiration-minutes` — **override the secret in production**.
- `taskms.prediction.min-training-samples` (default 20) — below this the fallback is used.
- `taskms.prediction.retrain-after-new-completed` (default 10) — auto-retrain cadence.
- `taskms.workload.overload-factor` (default 1.3) — overload threshold vs team mean.
