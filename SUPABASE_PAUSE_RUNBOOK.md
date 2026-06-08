# Supabase Pause / Restore Runbook

## A. Why this happens

JavaJobFit is currently using the Supabase Free plan.

Free projects can be paused after low activity. This is normal for beta/free-stage projects.

## B. What breaks when paused

- Backend may fail to create reports.
- Lead capture may fail.
- Feedback may fail.
- Events may fail.
- `/api/health` may still work if the backend is alive.
- `/api/health/db` will show database unreachable.

## C. How to restore

1. Go to Supabase dashboard manually.
2. Open project `javajobfit`.
3. Click Restore/Unpause if paused.
4. Wait for project to become active.
5. Restart/redeploy Render backend if needed.
6. Open `/api/health`.
7. Open `/api/health/db`.
8. Run `supabase-checks.sql`.
9. Run canary privacy test.

## D. When to upgrade

Upgrade to Pro when:

- beta users depend on the app
- you start paid reports
- downtime hurts trust
- you need proper backups
- you begin marketing/ads

## E. What not to do

- Do not commit DB password.
- Do not commit Supabase service role key.
- Do not add fake traffic cron just to bypass inactivity.
- Do not store raw resume/JD.
- Do not use transaction pooler `6543` for Flyway migrations.
