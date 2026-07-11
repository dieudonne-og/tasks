# Task Management System with AI-Based Deadline Prediction

A collaborative, web-based task management system for the **Human Resource
Department of the University of Kigali**. Staff log tasks, record the actual
effort spent, and capture deadlines on a shared board. A supervised
machine-learning model, trained on the department's own task history, predicts
how long each new task will take (with a confidence interval), warns when a task
is likely to miss its deadline, and suggests redistributing work when a staff
member is predicted to be overloaded.

This is the implementation of the research proposal *"Task Management System
with AI-Based Deadline Prediction"* by NIBAGWIRE SHALLON (Reg. 2301000336).

---

## Technology stack

| Layer      | Technology |
|------------|------------|
| Backend    | Java 17, Spring Boot 3.3 (Spring Web, Data JPA, Security, Validation) |
| Database   | MySQL (production) · H2 in-memory (default, for local dev/demo) |
| Machine learning | Weka — Linear Regression and Random Forest |
| Auth       | JWT (HS256) bearer tokens, BCrypt password hashing |
| Frontend   | HTML, CSS and vanilla JavaScript calling the JSON REST API |
| Build      | Maven |

The architecture follows Model-View-Controller: a REST API under `/api/**`
backed by services and JPA repositories, with a static single-page frontend
served from the same application.

---

## Running the application

### Quick start (no database to install)

The default profile uses an in-memory H2 database and seeds demo data on
startup, so the system runs and can be demonstrated immediately:

```bash
./mvnw spring-boot:run
# then open http://localhost:8080
```

> On Windows use `mvnw.cmd`. If you have Maven installed you can use `mvn`.

### With MySQL (production profile)

Create the database, then activate the `mysql` profile:

```sql
CREATE DATABASE taskms;
```

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=mysql
```

Connection details default to `localhost:3306`, user `root`/`root`, and can be
overridden with the environment variables `TASKMS_DB_URL`, `TASKMS_DB_USER` and
`TASKMS_DB_PASSWORD`. See `src/main/resources/application-mysql.yml`.

### Build a runnable jar

```bash
./mvnw clean package
java -jar target/taskms-1.0.0.jar --spring.profiles.active=mysql
```

---

## Demo accounts

Seeded automatically on first run (password **`password123`** for all):

| Email | Role | Can do |
|-------|------|--------|
| `admin@uok.ac.rw`    | Administrator | Manage users & task types, retrain the model, everything below |
| `manager@uok.ac.rw`  | HR Manager    | Full board, dashboard, workload suggestions |
| `officer1@uok.ac.rw` (also `officer2`, `officer3`) | HR Officer | Log/update/complete their own tasks |

The seeder also creates the department's task types (recruitment steps, leave
processing, performance appraisal) and ~120 synthetic completed tasks so the
model can train and the dashboard is populated from the first launch.

---

## How the four research objectives are met

1. **Analyse the limitations of the existing practice** — the dashboard
   contrasts the mean absolute error of users' *manual* estimates against the
   *model's* predictions and reports the on-time completion rate, quantifying the
   estimation gap the study describes.
2. **Secure, collaborative, database-backed task system** — JWT + role-based
   Spring Security, JPA/MySQL persistence, and a shared task board where users
   log tasks, record actual effort, and capture deadlines
   (`task/`, `user/`, `security/`).
3. **Integrate a supervised ML model** — `prediction/` trains **linear
   regression** and **random forest** on task type, assignee and complexity,
   predicts duration with a **95% confidence interval**, issues **delay
   warnings** (`TaskDto.atRisk`), and suggests **workload redistribution**
   (`workload/`). It **falls back to category averages** until enough history
   accumulates, then the trained model takes over (Limitation 1.7.1).
4. **Evaluate effectiveness** — every training run records **MAE, RMSE and R²**
   for both algorithms, automatically deploys the more accurate one (lower RMSE),
   and surfaces the comparison on the dashboard and admin pages (Objective 4).

---

## Key REST endpoints

| Method & path | Purpose | Access |
|---------------|---------|--------|
| `POST /api/auth/login` · `GET /api/auth/me` | Authenticate / current user | public / any |
| `GET /api/tasks` | Board (officers see own; managers see all) | any |
| `POST /api/tasks` · `PUT /api/tasks/{id}` | Create / edit (auto-predicts) | any |
| `PATCH /api/tasks/{id}/status` | Move across board columns | any |
| `PATCH /api/tasks/{id}/complete` | Record actual effort | any |
| `GET /api/task-types` · `POST/PUT/DELETE` | Task categories | read: any · write: admin |
| `GET /api/staff` | Assignable staff for the task form | any |
| `GET /api/workload` | Load per staff + redistribution suggestions | manager/admin |
| `GET /api/dashboard` | Reporting metrics | manager/admin |
| `GET /api/models/metrics` · `POST /api/models/retrain` | Model metrics / retrain | read: manager/admin · retrain: admin |
| `GET/POST/PUT/DELETE /api/users` | User management | admin |

---

## Project structure

```
src/main/java/rw/ac/uok/taskms/
  config/       Security, CORS, data seeding, startup model training
  security/     JWT service, auth filter, user principal
  user/         User & role model, auth + admin controllers
  task/         Task & task-type domain, board API, delay-warning DTO
  prediction/   Weka LR/RF training, fallback, metrics, retrain API
  workload/     Predicted load per staff + redistribution suggestions
  dashboard/    Reporting aggregation
  common/       Error handling
src/main/resources/
  application.yml, application-mysql.yml
  static/       index/board/dashboard/admin pages, css, js
src/test/java/  fallback + ML unit tests, auth/task/role integration tests
```

## Testing

```bash
./mvnw test
```

Covers the category-average fallback, the ML trainer (both models trained,
metrics computed, better model deployed, predictions carry a confidence
interval), and an end-to-end slice for authentication, the task API, role
enforcement, and that seeded tasks receive predictions.
