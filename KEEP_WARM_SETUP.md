# Keep the backend warm (free-tier plan)

The backend runs on Render's **free** tier, which sleeps after ~15 minutes of
inactivity and cold-starts in ~60 seconds (measured 57s and 65s on 6–7 Aug 2026, after the
cold-start tuning). Cold starts make the first scan slow and the first PDF/DOCX upload
appear to fail — pasted text still scores instantly via the in-browser fallback, but
PDF/DOCX parsing is server-side only and has no fallback.

Two layers handle this:

1. **Graceful handling (already in the app).** A cold start never shows a hard error:
   - Scans fall back to the in-browser engine instantly, then auto-upgrade to the saved
     backend score when the instance wakes.
   - Uploads warm the backend on page load and field focus, use a 120s per-request timeout,
     and retry up to 6 times with backoff (~95s total) to outlast a full cold start.
2. **Keep-warm pinging (this doc).** Ping the health endpoint on a schedule so the
   instance rarely sleeps during active hours.

## Decision: warm ~20h/day, not 24/7

Render free gives ~750 instance-hours/month. Running 24/7 uses ~720–744 hrs — it *fits*
but with almost no margin, and only if this is the only free service. Exceeding the cap
suspends the service for the rest of the month, so keep a buffer. We ping ~20h/day and let
the deadest window sleep; the app's graceful handling covers any off-window cold start.

### These schedules run in IST, not UTC

cron-job.org labels its Next-executions list "(UTC)", but on this account both jobs
demonstrably fire on **IST (Asia/Kolkata)**. Verified 7 Aug 2026 by comparing each job's
Next-executions list against the real clock: with the wall clock at 18:45 UTC / 00:15 IST,
`0 2,8,14,20` showed its next run as 02:00 the following day — the next matching hour in
IST, not the 20:00 UTC a UTC schedule would have given. Both jobs agreed.

Check a job's ADVANCED tab before assuming otherwise, and re-verify if the account timezone
ever changes.

Audience is India-first (r/developersIndia, ₹ pricing), so the sleep window belongs in the
Indian small hours: **04:00–07:59 IST**, which is exactly where `*/10 0-3,8-23` puts it.

Do **not** "correct" this to a UTC-style window without re-checking the timezone. Read as
IST, a `*/10 1-20 * * *` range would move the daily outage to 21:00–00:59 IST — prime
evening job-hunting hours.

## Setup: cron-job.org (free, reliable, supports an hour window)

1. Sign up at https://cron-job.org
2. Create a cronjob:
   - **Title:** JavaJobFit keep-warm
   - **URL:** `https://java-job-fit.onrender.com/api/health`
   - **Schedule:** every **10 minutes**, restricted to IST hours **00–03 and 08–23**
     (i.e., paused 04:00–07:59 IST).
   - Custom cron expression if needed: `*/10 0-3,8-23 * * *`
   - **Timezone:** evaluated in IST on this account — see the section above.
   - **Notifications:** turn ON *"the cronjob will be disabled because of too many
     failures"*. Leave *"execution of the cronjob fails"* OFF — it would email daily
     (see the timeout note below).
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

### Expect one "failed" ping per day — that one is harmless

cron-job.org's free plan caps a request at **30 seconds**, and a cold start takes **~60s**.
So the first ping after each sleep window is always recorded as *failed*. It still reaches
Render and still triggers the wake, so the ping 10 minutes later succeeds. One red row per
day at the start of the warm window is expected; don't chase it.

What is **not** normal is a long unbroken run of failures — that's what trips the
~26-consecutive auto-disable.

### This job has been auto-disabled twice — suspect it whenever the site feels slow

Most recently 5–6 Aug 2026: the backend OOM-crashed (exit 137, heap sized at 75% of a 512MB
container) and served 503s for hours. That burned through the consecutive-failure budget and
cron-job.org switched the keep-warm job off. It went unnoticed until 7 Aug, and in the
meantime every visitor paid a ~60s cold start — or simply couldn't upload a PDF at all.

Mitigations now in place: the *"disabled because of too many failures"* notification is ON
for both jobs, and the heap is capped at 50% so the OOM cause is fixed.

To check in one command — under a second means warm, ~60s means it slept and the keep-warm
job is not doing its job:

```
curl -s -o /dev/null -w '%{time_total}\n' https://java-job-fit.onrender.com/api/health
```

Trade-off: pinging `/api/health` does not generate Supabase DB activity, so it does not
prevent Supabase's 7-day idle pause. This actually happened (Aug 2026): three quiet weeks
paused Supabase, and the next deploy failed at boot with the pooler error
`FATAL: (ENOTFOUND) tenant/user ... not found`. If you ever see that error, the fix is:
unpause the Supabase project from its dashboard, then Manual Deploy on Render.

### Second job: keep Supabase awake (low-frequency DB ping)

To prevent the 7-day pause during zero-traffic stretches, add a SECOND cron-job.org job:

- **Title:** supabase keep-awake
- **URL:** `https://java-job-fit.onrender.com/api/health/db`
- **Schedule (recommended):** `0 2,8,14,20 * * *` — 4 pings/day, 6h apart, every hour inside
  the warm window (IST 02:00, 08:00, 14:00, 20:00)
- **Failure notifications:** OFF. *"Disabled because of too many failures"*: ON.

**As actually configured (7 Aug 2026)** this job runs at **01:30, 07:30, 13:30, 19:30 IST**
(:30 past hours 1/7/13/19) rather than the recommended expression. That still satisfies the
anti-pause requirement — 4 real DB queries a day, evenly spaced — but the **07:30 run falls
inside the 04:00–07:59 sleep window**, so once a day it wakes the instance early and logs a
timeout failure. Harmless (the request still reaches Render and the DB query still runs;
cron just stops waiting for the reply), but switching to `0 2,8,14,20 * * *` removes it.

**Update (20 Aug 2026): tiny reads were not enough.** Supabase sent a "project is going to
be paused" warning even while this job was successfully running `select 1` through the
pooler 3-4x/day — small reads evidently sit below their "sufficient activity" heuristic.
`/api/health/db` therefore now also INSERTS one `db_keepalive` row into the events table
(throttled to at most one write per 4 hours, so the unlimited GET cannot bloat the table).
Writes are unambiguous user database activity.

**If a pause-warning email ever arrives anyway:** open the Supabase dashboard SQL editor and
run any query (e.g. `select count(*) from events;`) — dashboard activity resets the idle
clock immediately and buys 7 days while you investigate.

Why 4/day is safe where 10-minute DB pinging wasn't: a multi-hour outage produces only a
couple of failures — far below cron-job.org's ~26-consecutive-failures auto-disable
threshold — and each ping hits an already-warm instance (the main job keeps Render awake),
so there are no cold-start timeouts. Each ping is a real DB query, which resets Supabase's
idle clock.

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
