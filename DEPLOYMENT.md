# JavaJobFit Beta Deployment

JavaJobFit beta deploys to:

- Frontend: GitHub Pages
- Backend: Render
- Database: Supabase PostgreSQL

Supabase public schema currently has no JavaJobFit app tables. Keep `SPRING_FLYWAY_BASELINE_ON_MIGRATE=false` and let Flyway run the initial migrations normally.

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
5. Check Supabase tables.
6. Check Flyway history.
7. Run canary privacy test.
8. Deploy frontend.
9. Test live site.
10. Start beta testing with 50 users.

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

After deployment, open:

```text
https://java-job-fit.onrender.com/api/health
```

Expected safe shape:

```json
{
  "status": "ok",
  "service": "JavaJobFit API",
  "timestamp": "...",
  "version": "..."
}
```

The health endpoint must not expose secrets, database URLs, environment variables, stack traces, or Supabase credentials.

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

## Privacy Rules

Do not store raw resume text, raw job description text, uploaded files, analytics payloads containing raw input, or stack traces containing user input.

Only generated report output, optional lead email data, optional feedback, experience level, country, report ID/public ID, consent, source, timestamps, and safe metadata may be stored.
