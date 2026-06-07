# JavaJobFit Backend

Spring Boot API for JavaJobFit reports, lead capture, feedback, and safe events.

## Endpoints

```text
GET  /api/health
POST /api/reports
GET  /api/reports/{id}
POST /api/leads
POST /api/feedback
POST /api/events
```

`POST /api/reports` processes raw resume text and raw job descriptions, but the
service stores only generated report output. The free API response returns a
limited preview until a real paid unlock flow exists.

## Local Run

This backend is configured to keep Maven dependencies inside this project:

```text
backend/.m2
```

It also uses `backend/.mvn/settings.xml` so it does not depend on machine-level
Maven mirrors from other projects on this laptop.

```bash
cd backend
mvn spring-boot:run
```

Health check:

```bash
curl http://localhost:8080/api/health
```

## Local Database

By default, the backend uses H2 file storage:

```text
backend/data/javajobfit
```

Schema changes are managed with Flyway migrations in:

```text
backend/src/main/resources/db/migration
backend/src/main/resources/db/vendor
```

Flyway runs automatically when the Spring Boot app starts. Hibernate is set to
`ddl-auto=validate`, so it checks the schema but does not create or alter
tables. To apply migrations locally, start the backend:

```bash
cd backend
mvn -Dmaven.repo.local=./.m2 spring-boot:run
```

You can also run with the explicit development profile:

```bash
SPRING_PROFILES_ACTIVE=dev mvn -Dmaven.repo.local=./.m2 spring-boot:run
```

Tests use the same migrations against an in-memory H2 database.

## Production Environment Variables

Use these when deploying with PostgreSQL:

```text
PORT=8080
DATABASE_URL=jdbc:postgresql://HOST:PORT/DATABASE
DATABASE_USERNAME=postgres_user
DATABASE_PASSWORD=postgres_password
DATABASE_DRIVER=org.postgresql.Driver
HIBERNATE_DIALECT=org.hibernate.dialect.PostgreSQLDialect
SPRING_PROFILES_ACTIVE=prod
SPRING_FLYWAY_BASELINE_ON_MIGRATE=false
ALLOWED_ORIGINS=https://abhishekpratapsingh2601-arch.github.io
PAYMENT_PROVIDER_ENABLED=false
STRIPE_SECRET_KEY=
RAZORPAY_KEY_ID=
RAZORPAY_KEY_SECRET=
```

If the frontend is served at a custom domain later, add it to `ALLOWED_ORIGINS`
as a comma-separated value.

`SPRING_FLYWAY_BASELINE_ON_MIGRATE` defaults to `false`. Set it to `true` only
for the first production migration if Flyway must adopt an existing Supabase
schema that was originally created by Hibernate. After that first successful
deploy, remove the variable or set it back to `false`.

Payment variables are scaffolding only. Leave `PAYMENT_PROVIDER_ENABLED=false`
until Stripe or Razorpay checkout is implemented for real.

## Migration Privacy Rules

Migrations must never add columns for raw resume text, raw job description text,
or uploaded files. The `reports` table stores generated report output only. The
`leads` table stores email lead metadata only. The `feedback` table stores
feedback messages only. The `events` table stores safe event metadata only,
never raw resume text, raw job description text, or generated report text.

## Free Deployment Recommendation

Use Railway or Render from GitHub.

Railway:

- Create project from GitHub repo.
- Set the service root directory to `backend`.
- Generate a public domain.
- Add PostgreSQL if available on your plan.
- Add the environment variables above.

Render:

- Create a Web Service from GitHub repo.
- Set root directory to `backend`.
- Runtime can use Docker because this folder includes `Dockerfile`.
- Add environment variables above.

After backend deploy, update root `config.js`:

```js
window.JAVAJOBFIT_API_BASE = "https://YOUR_BACKEND_URL";
```

Then commit and push. The GitHub Pages frontend will start using the backend.

## Docker Isolation

The root `docker-compose.yml` uses only JavaJobFit-prefixed resources:

```text
javajobfit-backend
javajobfit-postgres
javajobfit-postgres-data
```

Do not run Docker prune commands for this project on a shared laptop.
