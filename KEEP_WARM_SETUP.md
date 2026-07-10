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
   - **URL:** `https://java-job-fit.onrender.com/api/health/db`
   - **Schedule:** every **10 minutes**, restricted to UTC hours **00–03 and 08–23**
     (i.e., paused 04:00–07:59 UTC).
   - Custom cron expression if needed: `*/10 0-3,8-23 * * *`
   - **Timezone:** UTC
3. Save. Expected response: HTTP 200 with `"database":"reachable"`.

Result: ~20h/day × ~30 days ≈ ~600 instance-hours/month — safely under the 750 cap.

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
