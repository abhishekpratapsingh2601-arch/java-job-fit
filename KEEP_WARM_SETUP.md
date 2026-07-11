# Keep the backend warm (free-tier plan)

The backend runs on Render's **free** tier, which sleeps after ~15 minutes of
inactivity and cold-starts in ~100 seconds. Cold starts make the first scan slow and
the first PDF/DOCX upload appear to fail.

Two layers handle this:

1. **Graceful handling (already in the app).** A cold start never shows a hard error:
   - Scans fall back to the in-browser engine instantly, then auto-upgrade to the saved
     backend score when the instance wakes.
   - Uploads warm the backend on page load, use a 120s timeout, and retry once.
2. **Keep-warm pinging (this doc).** Ping the health endpoint on a schedule so the
   instance rarely sleeps during active hours.

## Decision: warm ~20h/day, not 24/7

Render free gives ~750 instance-hours/month. Running 24/7 uses ~720–744 hrs — it *fits*
but with almost no margin, and only if this is the only free service. A beta with low
traffic does not need 24/7. We ping ~20h/day and let the deadest UTC window sleep; the
app's graceful handling covers any off-window cold start.

US + Europe + India together span nearly the whole clock, so **04:00–08:00 UTC** is the
least-active slice (US night + Europe pre-dawn) — that's the window we let it sleep.

## Setup: cron-job.org (free, reliable, supports an hour window)

1. Sign up at https://cron-job.org
2. Create a cronjob:
   - **Title:** JavaJobFit keep-warm
   - **URL:** `https://java-job-fit.onrender.com/api/health`
   - **Schedule:** every **10 minutes**, restricted to UTC hours **00–03 and 08–23**
     (i.e., paused 04:00–07:59 UTC).
   - Custom cron expression if needed: `*/10 0-3,8-23 * * *`
   - **Timezone:** UTC
3. Save. Expected response: HTTP 200 with `"status":"ok"`.

Result: ~20h/day × ~30 days ≈ ~600 instance-hours/month — safely under the 750 cap.

### Important: ping `/api/health`, NOT `/api/health/db`

Keep-warm only needs to stop Render's 15-min sleep, so it must hit a **liveness** endpoint
(`/api/health`) that returns 200 whenever the app is up. Do **not** point it at
`/api/health/db` (a readiness/DB check): if Supabase has a transient blip, that endpoint
returns 503, cron-job.org counts repeated failures, and after ~26 it **auto-disables the
keep-warm job entirely** — so a brief DB hiccup silently turns off your keep-warm and the
instance starts sleeping again. Liveness pinging avoids that failure mode.

`/api/health/db` is for manual monitoring/alerting, not for keep-warm.

Trade-off: pinging `/api/health` does not generate Supabase DB activity, so it does not
prevent Supabase's 7-day idle pause. During an active beta, real scans keep Supabase
active. If the app goes ~7 days with zero traffic, unpause Supabase manually from its
dashboard (30 seconds). This is a much rarer/gentler event than Render's 15-min sleep.

## GitHub Actions workflow

`.github/workflows/keepalive.yml` is now **manual-only** (`workflow_dispatch`). Its old
10-minute schedule was removed: GitHub cron is frequently delayed past the 15-minute
sleep window (so it missed cold starts) and emailed a failure on every miss. Trigger it
manually from the Actions tab only for an ad-hoc health check.

## When to upgrade to paid

Move to Render's paid tier (~$7/mo) for true always-on with zero cold starts **only once
analytics show real, sustained US/Europe traffic**. Until then, free + this plan is
enough for beta.

## Verify

```
curl -s https://java-job-fit.onrender.com/api/health/db
# {"service":"JavaJobFit API",...,"status":"ok","database":"reachable"}
```
