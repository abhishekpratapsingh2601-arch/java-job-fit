# Live Deployment Plan

JavaJobFit uses two deployed pieces:

- Frontend: GitHub Pages
- Backend: Spring Boot API on Render
- Database: Supabase Postgres free project

This keeps hosting free for the MVP while still giving us a real running
backend and persistent database.

## Why This Setup

Render Free Web Service:

- Can run the Spring Boot backend from `backend/Dockerfile`.
- Supports GitHub deploys.
- Free web services spin down after idle, so the first request after a quiet
  period can be slow.

Supabase Free Postgres:

- Includes 500 MB database storage.
- Better for MVP persistence than Render Free Postgres because Render Free
  Postgres expires after 30 days.
- Free Supabase projects can pause after inactivity.

## Step 1: Create Supabase Database

1. Go to `https://supabase.com`.
2. Create a free project named `javajobfit`.
3. Open `Project Settings`.
4. Open `Database`.
5. Copy the JDBC connection string.

Use environment values like:

```text
DATABASE_URL=jdbc:postgresql://HOST:PORT/postgres?sslmode=require
DATABASE_USERNAME=postgres
DATABASE_PASSWORD=YOUR_SUPABASE_DATABASE_PASSWORD
DATABASE_DRIVER=org.postgresql.Driver
HIBERNATE_DIALECT=org.hibernate.dialect.PostgreSQLDialect
SPRING_PROFILES_ACTIVE=prod
FLYWAY_BASELINE_ON_MIGRATE=true
PAYMENT_PROVIDER_ENABLED=false
```

Do not commit the real password to GitHub.

## Step 2: Deploy Backend On Render

1. Go to `https://render.com`.
2. Create a free account or sign in.
3. Click `New`.
4. Choose `Web Service`.
5. Connect the GitHub repository:

```text
abhishekpratapsingh2601-arch/java-job-fit
```

6. Use these settings:

```text
Name: javajobfit-backend
Runtime: Docker
Root Directory: backend
Dockerfile Path: Dockerfile
Instance Type: Free
```

7. Add environment variables:

```text
PORT=8080
DATABASE_URL=jdbc:postgresql://HOST:PORT/postgres?sslmode=require
DATABASE_USERNAME=postgres
DATABASE_PASSWORD=YOUR_SUPABASE_DATABASE_PASSWORD
DATABASE_DRIVER=org.postgresql.Driver
HIBERNATE_DIALECT=org.hibernate.dialect.PostgreSQLDialect
SPRING_PROFILES_ACTIVE=prod
FLYWAY_BASELINE_ON_MIGRATE=true
ALLOWED_ORIGINS=https://abhishekpratapsingh2601-arch.github.io
PAYMENT_PROVIDER_ENABLED=false
```

8. Deploy.

Flyway runs automatically on Render startup. `SPRING_PROFILES_ACTIVE=prod`
keeps Hibernate in schema-validation mode instead of auto-updating tables.
`FLYWAY_BASELINE_ON_MIGRATE=true` is important for the current Supabase database
if it already has JavaJobFit tables from the older Hibernate-managed setup.

## Step 3: Test Backend

After Render gives a public URL, test:

```text
https://YOUR_RENDER_BACKEND_URL/api/health
```

Expected response:

```json
{"status":"ok"}
```

## Step 4: Connect Frontend To Backend

Edit `config.js`:

```js
window.JAVAJOBFIT_API_BASE = "https://YOUR_RENDER_BACKEND_URL";
```

Commit and push:

```bash
git add config.js
git commit -m "chore: connect frontend to live backend"
git push
```

After GitHub Pages updates, the live site will use the backend API.

## Live Test Checklist

- Open the GitHub Pages frontend.
- Click `Try sample resume`.
- Click `Analyze my Java resume`.
- Confirm report appears.
- Submit the lead email form with a test email.
- Submit feedback.
- Check Supabase table data for `reports`, `leads`, and `feedback`.

## Database Migrations

Migration files live in:

```text
backend/src/main/resources/db/migration
```

Render does not need a separate migration job for this MVP. The backend runs
Flyway migrations before JPA starts. If migration fails, the app should fail to
start instead of silently changing schema through Hibernate.

Privacy rule: migrations must not add raw resume, raw job description, analytics
payload, or uploaded file storage columns.
