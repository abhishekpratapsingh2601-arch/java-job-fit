# JavaJobFit Beta Deployment

JavaJobFit beta deploys to:

- Frontend: GitHub Pages
- Backend: Render
- Database: Supabase PostgreSQL

Supabase public schema currently has no JavaJobFit app tables. Keep `SPRING_FLYWAY_BASELINE_ON_MIGRATE=false` and let Flyway run the initial migrations normally.

## Supabase Free Plan Pause Warning

Supabase Free projects may be paused after low activity. If the JavaJobFit project is paused, backend database calls will fail until the project is restored from the Supabase dashboard.

For beta, use the app normally or run deployment checks after deploy. Do not rely on fake keep-alive traffic or cron jobs just to bypass Supabase inactivity rules. For serious production usage, upgrade to Supabase Pro to avoid inactivity pausing and get proper backup options. The Free plan may not include dashboard backups, so keep export and check docs ready before real users or revenue.

Manual restore steps:

1. Login to the Supabase dashboard manually.
2. Open project `javajobfit`.
3. If paused, click Restore/Unpause.
4. Wait until the project is active.
5. Redeploy or restart the Render backend if needed.
6. Check `GET https://java-job-fit.onrender.com/api/health`.
7. Check `GET https://java-job-fit.onrender.com/api/health/db`.
8. Run `supabase-checks.sql`.
9. Run the privacy canary test.

## Supabase Production Database

Use the Supabase direct PostgreSQL connection for the Render backend and Flyway migrations. Do not use the transaction pooler for migrations.

Use:

- Direct host: `db.YOUR_PROJECT_REF.supabase.co`
- Direct port: `5432`
- SSL required in the JDBC URL

Example JDBC URL format:

```text
jdbc:postgresql://db.YOUR_PROJECT_REF.supabase.co:5432/postgres?sslmode=require
```

Do not use the Supabase transaction pooler port `6543` for Flyway migrations. Flyway needs a direct database connection so it can create and validate `flyway_schema_history` safely.

Because the JavaJobFit schema is empty for the first production deploy, keep:

```text
SPRING_FLYWAY_BASELINE_ON_MIGRATE=false
SPRING_JPA_HIBERNATE_DDL_AUTO=validate
```

Production config pins the PostgreSQL JDBC driver and Hibernate dialect in `application-prod.yml`, so do not hard-code database credentials in the repo.

## Deployment Order

1. Push latest code to GitHub.
2. Set Render environment variables.
3. Deploy backend first.
4. Check `/api/health`.
5. Check `/api/health/db`.
6. Check Supabase tables.
7. Check Flyway history.
8. Run canary privacy test.
9. Deploy frontend.
10. Test live site.
11. Start beta testing with 50 users.

## Render Environment Variables

Use `render-env.example.txt` as the safe template:

```text
SPRING_PROFILES_ACTIVE=prod
SPRING_FLYWAY_ENABLED=true
SPRING_FLYWAY_BASELINE_ON_MIGRATE=false
SPRING_JPA_HIBERNATE_DDL_AUTO=validate
PAYMENT_PROVIDER_ENABLED=false
SPRING_DATASOURCE_URL=jdbc:postgresql://db.YOUR_PROJECT_REF.supabase.co:5432/postgres?sslmode=require
SPRING_DATASOURCE_USERNAME=your_supabase_db_username
SPRING_DATASOURCE_PASSWORD=your_supabase_db_password
ALLOWED_ORIGINS=https://abhishekpratapsingh2601-arch.github.io
```

Do not commit real database passwords, Supabase credentials, Stripe keys, Razorpay keys, or any other secrets.

Do not set `SPRING_FLYWAY_BASELINE_ON_MIGRATE=true` for this first deploy because the JavaJobFit app schema is empty. Use `false` so Flyway creates the initial schema from versioned migrations.

Render should use the Supabase direct PostgreSQL URL on port `5432`, not the transaction pooler URL on port `6543`.

## Backend Deploy

Render settings:

```text
Runtime: Docker
Root Directory: backend
Dockerfile Path: Dockerfile
Instance Type: Free
```

Flyway runs automatically on backend startup before JPA validation. Production uses `ddl-auto: validate`, so Hibernate validates the Flyway-created schema instead of creating or changing tables.

## Render 503 Troubleshooting

If `https://java-job-fit.onrender.com/api/health` returns `503`, treat it as a backend startup/configuration issue first, not a frontend issue.

Check Render in this order:

1. Open the `java-job-fit` Render service.
2. Confirm the latest GitHub commit is deployed.
3. Confirm the service is not paused and allow a sleeping Free instance time to wake up.
4. Confirm Render settings are still Docker, root directory `backend`, and Dockerfile path `Dockerfile`.
5. Confirm the Java runtime is Java 11 in the Dockerfile.
6. Open **Environment** and confirm the required variables from `render-env.example.txt` exist.
7. Confirm `SPRING_PROFILES_ACTIVE=prod`.
8. Confirm `SPRING_DATASOURCE_URL` uses the Supabase direct PostgreSQL host on port `5432` with `sslmode=require`.
9. Do not use the Supabase transaction pooler port `6543` for Flyway migrations.
10. Confirm `SPRING_DATASOURCE_USERNAME` and `SPRING_DATASOURCE_PASSWORD` are set in Render, not committed to the repo.
11. Confirm `SPRING_FLYWAY_ENABLED=true`.
12. Confirm `SPRING_FLYWAY_BASELINE_ON_MIGRATE=false` for the empty JavaJobFit production schema.
13. Confirm `SPRING_JPA_HIBERNATE_DDL_AUTO=validate`.
14. Check whether the Supabase Free project is paused. If it is paused, unpause it before restarting Render.
15. Open Render logs and look for Flyway migration failures, database connection failures, missing environment variables, or Spring startup failures.
16. Confirm `/api/health` returns HTTP 200.
17. Confirm `/api/health/db` returns HTTP 200.

A common 503 cause is missing Render environment variables. With the `prod` profile active, JavaJobFit expects a PostgreSQL datasource. If Render has no datasource URL, username, or password, startup can fail before health endpoints are available.

After deployment, open:

```text
https://java-job-fit.onrender.com/api/health
https://java-job-fit.onrender.com/api/health/db
```

Expected safe `/api/health` shape:

```json
{
  "status": "ok",
  "service": "JavaJobFit API",
  "timestamp": "...",
  "version": "..."
}
```

Expected safe `/api/health/db` shape when Supabase is reachable:

```json
{
  "status": "ok",
  "service": "JavaJobFit API",
  "database": "reachable",
  "timestamp": "...",
  "version": "..."
}
```

The health endpoints must not expose secrets, database URLs, database usernames, environment variables, stack traces, or Supabase credentials.

## Local Production-Like Smoke Boot

Use this only when you want to reproduce production startup locally. Keep secrets in your shell session and never commit them.

```bash
cd backend
SPRING_PROFILES_ACTIVE=prod \
SPRING_FLYWAY_ENABLED=true \
SPRING_FLYWAY_BASELINE_ON_MIGRATE=false \
SPRING_JPA_HIBERNATE_DDL_AUTO=validate \
SPRING_DATASOURCE_URL="jdbc:postgresql://db.YOUR_PROJECT_REF.supabase.co:5432/postgres?sslmode=require" \
SPRING_DATASOURCE_USERNAME="postgres" \
SPRING_DATASOURCE_PASSWORD="YOUR_SUPABASE_DB_PASSWORD" \
mvn -Dmaven.repo.local=./.m2 spring-boot:run
```

For live post-deploy verification, run the smoke script from the repo root:

```bash
PROD_API_BASE_URL="https://java-job-fit.onrender.com" bash scripts/live-smoke-test.sh
```

The script uses fake canary text, checks health endpoints, creates one public report, verifies numeric report IDs are not exposed, and confirms lead, feedback, and event endpoints accept safe payloads.

## Supabase Checks

Run the read-only checks in `supabase-checks.sql` from the Supabase SQL Editor after backend startup.

Confirm:

- `reports`, `leads`, `feedback`, `events`, and `flyway_schema_history` exist.
- Flyway migrations are marked successful.
- `reports.public_id` exists and is unique.
- No raw resume/JD columns exist.
- Canary strings such as `DO_NOT_STORE` are not found after a test scan.

## Canary Privacy Test

Use the live frontend with a test resume and job description containing a unique marker, for example:

```text
DO_NOT_STORE_BETA_CANARY_20260601
```

After the scan, run the canary query in `supabase-checks.sql`. It should return zero rows. Also check Render logs and confirm the marker was not logged.

## Frontend Deploy

GitHub Pages serves the frontend from the repository. `config.js` must point to:

```js
window.JAVAJOBFIT_API_BASE = "https://java-job-fit.onrender.com";
```

After GitHub Pages updates, test:

- Page loads.
- `Try sample resume` works.
- `Analyze my Java resume` works.
- Score and top fixes appear.
- Free preview stays limited.
- Premium sections stay locked.
- Email capture works.
- Feedback works.
- Copy buttons work.
- Export report works.
- Privacy, Terms, and Contact links work.
- Mobile view has no horizontal scroll.

## Keep Warm (free tier)

The Render free instance sleeps after ~15 min idle and cold-starts in ~100s, which makes
the first scan slow and the first upload look like it failed. The app handles cold starts
gracefully (browser-fallback scan + upload warm/retry), and scheduled keep-warm pinging runs
from cron-job.org on a ~20h/day window to stay inside the free instance-hour budget.

See `KEEP_WARM_SETUP.md` for the exact cron-job.org settings. The GitHub Actions workflow
`.github/workflows/keepalive.yml` is manual-only (`workflow_dispatch`) and is not the
scheduled pinger.

## Privacy Rules

Do not store raw resume text, raw job description text, uploaded files, analytics payloads containing raw input, or stack traces containing user input.

Only generated report output, optional lead email data, optional feedback, experience level, country, report ID/public ID, consent, source, timestamps, and safe metadata may be stored.
