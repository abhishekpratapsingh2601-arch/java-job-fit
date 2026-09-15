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

## Decision (revised 11 Sep 2026): run keep-warm 24/7 — never let Render sleep

Original plan was ~20h/day with a 04:00–08:00 IST sleep to keep a buffer under Render's 750
free instance-hours. That sleep turned out to be the root of every keep-alive incident since
August: once Render hibernates, **nothing free wakes it dependably.** cron-job.org's 30s cap
gets a fast 503 instead of a wake; GitHub Actions has a 90s timeout but drops scheduled ticks
for hours at a time (four consecutive misses on the morning of 11 Sep, server down 7+ hours,
then a 9-minute wake). The only robust free option is to remove the sleep entirely:

- keep-warm expression: `*/10 * * * *` (every 10 minutes, all day). This also makes the
  UTC-vs-IST timezone question moot.
- Hours: 30-day month = 720h, 31-day month = 744h, cap 750h. Fits, with a thin margin in
  31-day months. Check Render's usage meter mid-month; if it ever trends over, re-add a short
  gap for the last few days rather than risk suspension (which lasts until month end).
- Only valid while this is the ONLY free web service on the Render account.

The 4–8 AM sleep was never worth it: it saved ~120h/month of a budget we were not close to,
at the cost of a daily outage that free tooling could not reliably end. The permanent fix
remains Render's paid tier (no sleep at all); first revenue pays for it.

### These schedules run in UTC (corrected 11 Sep 2026 — the doc said IST for a month)

On 7 Aug this doc concluded the jobs evaluate in IST. **That was wrong.** Conclusive evidence:
the supabase job on `0 2,8,14,20` shows last/next executions of 1:30 PM and 7:30 PM IST — which
is 08:00 and 14:00 UTC. Warmth samples agree: the server held warm at 14:03–14:33 IST
(08:33–09:03 UTC, inside the pinging hours) and was cold at 11:43 IST (06:13 UTC, inside the
`*/10 0-3,8-23` gap of 04:00–07:59 UTC).

So the intended "04:00–08:00 IST" sleep window was actually **09:30–13:30 IST — peak Indian
job-hunting hours — every day from 7 Aug to 11 Sep.** That, not any single crash, is why the
server kept being found asleep at midday.

With keep-warm on `*/10 * * * *` (24/7) the timezone no longer matters. If a window is ever
reintroduced, set the job's timezone explicitly in the ADVANCED tab and verify against a
known-time execution in History rather than the edit page's "Next executions" list, which
was what misled the 7 Aug reading.

## Setup: cron-job.org (free, reliable, supports an hour window)

1. Sign up at https://cron-job.org
2. Create a cronjob:
   - **Title:** JavaJobFit keep-warm
   - **URL:** `https://java-job-fit.onrender.com/api/health`
   - **Schedule:** every **10 minutes, 24/7** — custom expression `*/10 * * * *`
     (see the revised decision above; the former windowed expression `*/10 0-3,8-23` ran in
     UTC and slept the server at Indian midday).
   - **Timezone:** UTC on this account — see the section above.
   - **Notifications:** turn ON *"the cronjob will be disabled because of too many
     failures"*. Leave *"execution of the cronjob fails"* OFF — it would email daily
     (see the timeout note below).
3. Save. Expected response: HTTP 200 with `"status":"ok"`.

Result: 720h (30-day month) or 744h (31-day month) — under the 750 cap with a thin margin.

### Important: ping `/api/health`, NOT `/api/health/db`

Keep-warm only needs to stop Render's 15-min sleep, so it must hit a **liveness** endpoint
(`/api/health`) that returns 200 whenever the app is up. Do **not** point it at
`/api/health/db` (a readiness/DB check): if Supabase has a transient blip, that endpoint
returns 503, cron-job.org counts repeated failures, and after ~26 it **auto-disables the
keep-warm job entirely** — so a brief DB hiccup silently turns off your keep-warm and the
instance starts sleeping again. Liveness pinging avoids that failure mode.

`/api/health/db` is for manual monitoring/alerting, not for keep-warm.

### Expect failed pings right after the sleep window — and do not rely on them to wake Render

cron-job.org's free plan caps a request at **30 seconds**, and a cold start takes **~60s**.
So the first ping after each sleep window is always recorded as *failed*. Earlier versions of
this doc claimed that ping "still triggers the wake" — **that turned out to be unreliable**
(10 Sep 2026: five hours of post-sleep pings never woke the server). Render tends to answer
the abandoned 30s request with a fast 503 rather than finish booting, and each following ping
repeats it. The hourly GitHub Actions run (90s timeout) is what actually wakes Render; expect
red cron rows between ~08:00 IST and that first GitHub run, then green.

What is **not** normal is red rows continuing all day — that means the GitHub waker is not
running (check the Actions tab), and a long unbroken run of failures is also what trips
cron-job.org's ~26-consecutive auto-disable.

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

**As configured since 9 Sep 2026** this job runs on the recommended `0 2,8,14,20 * * *`
(02:00, 08:00, 14:00, 20:00 IST), all inside the warm window. From 7 Aug to 9 Sep it ran at
:30 past 1/7/13/19, whose 07:30 run fell inside the 04:00–07:59 sleep window and logged one
harmless timeout a day.

**Update (20 Aug 2026): tiny reads were not enough.** Supabase sent a "project is going to
be paused" warning even while this job was successfully running `select 1` through the
pooler 3-4x/day — small reads evidently sit below their "sufficient activity" heuristic.
`/api/health/db` therefore now also INSERTS one `db_keepalive` row into the events table
(throttled to at most one write per 4 hours, so the unlimited GET cannot bloat the table).
Writes are unambiguous user database activity.

**If a pause-warning email ever arrives anyway:** open the Supabase dashboard SQL editor and
run any query (e.g. `select count(*) from events;`) — dashboard activity resets the idle
clock immediately and buys 7 days while you investigate.

**Correction (9 Sep 2026): this job is NOT independent of keep-warm, and both were found
dead.** keep-warm auto-disabled on 22 Aug (deploy-restart 503s), after which Render slept; every
DB ping then hit a cold server, and a ~60s cold start exceeds cron-job.org's 30s cap, so this job
failed 4x/day until it auto-disabled too on 28 Aug. Supabase survived the following 12 days only
because a manual health check on 30 Aug and another on 9 Sep happened to write rows — not
because of any job. Check the Supabase inbox for a pause warning whenever both jobs show
Inactive.

**Fix:** `.github/workflows/keepalive.yml` is scheduled **hourly through the warm window**
(`5 2-21 * * *` UTC ≈ 07:35–03:05 IST). It has a 90s timeout and 5 retries, so it wakes a
sleeping Render and still completes, and GitHub does not auto-disable a workflow for failing.
It is the real Supabase keeper *and* the "patient waker" for Render; the cron-job.org jobs
keep an awake server awake but cannot wake a sleeping one (see below). GitHub does pause
schedules in repos with no commits for 60 days — any push re-arms it.

**Found 10 Sep 2026: cron-job.org cannot wake Render after the nightly sleep.** With keep-warm
enabled and pinging, the server was still asleep at 13:46 IST — five hours of 30-second pings
had failed to wake it. Render answers an abandoned 30s request with a fast 503 instead of
finishing the ~60s boot, so every ping fails the same way until a client waits the full minute.
Once woken (by a manual curl), the same cron kept it warm for hours. Conclusion: the 10-minute
cron is a *keeper*, not a *waker*; the hourly GitHub run is the waker.

Why 4/day is safe where 10-minute DB pinging wasn't: a multi-hour outage produces only a
couple of failures — far below cron-job.org's ~26-consecutive-failures auto-disable
threshold — and each ping hits an already-warm instance (the main job keeps Render awake),
so there are no cold-start timeouts. Each ping is a real DB query, which resets Supabase's
idle clock.

## Fourth auto-disable (15 Sep 2026) — and why GitHub Actions is not a waker

Sequence: GitHub run woke Render at 13:07 IST; nothing pinged for 15 min so it slept at
~13:22; cron-job.org pings resumed 13:30 (still on the windowed UTC schedule) and hit a
sleeping server; every ping got a fast 503 (~700 ms); GitHub then **skipped five consecutive
hourly runs** (13:35–17:35 IST); 26 failures later the job auto-disabled at 17:40. The
"disabled" email arrived within one minute — that alert is reliable.

Lessons, in order of importance:

1. **GitHub's schedule is dropped under load for hours at a time.** Fine for Supabase
   (7-day tolerance). Useless as a Render waker (15-minute tolerance). Do not rely on it
   for that; the 10 Sep note above calling it "the waker" was too optimistic.
2. **cron-job.org can hold an awake server but never wake a sleeping one.** Any sleep —
   the daily window, a blip, a deploy gap — becomes a 4-hour failure run and an auto-disable.
3. **Recovery procedure (manual):** open the site or curl `/api/health` and wait the full
   ~60 s for it to boot, THEN re-enable the job so its first ping lands on a warm server.
   Re-enabling first just restarts the 26-failure countdown.
4. **The only fix that removes the failure mode is a server that does not sleep** — Render
   Starter (~$7/mo). Everything on the free tier is mitigation: the 24/7 `*/10 * * * *`
   schedule removes the *daily* trigger, and this email alert plus the recovery procedure
   handles the rest.

## GitHub Actions workflow

`.github/workflows/keepalive.yml` runs **twice daily** (03:00 and 15:00 UTC) as the Supabase
keeper — see the correction above for why cron-job.org alone cannot do that job. It is still
NOT a keep-warm: GitHub cron is delayed by minutes to an hour, useless against Render's
15-minute sleep, so the old 10-minute schedule stays removed. It can also be run manually from
the Actions tab for an ad-hoc health check.

## When to upgrade to paid

Move to Render's paid tier (~$7/mo) for true always-on with zero cold starts **only once
analytics show real, sustained US/Europe traffic**. Until then, free + this plan is
enough for beta.

## Verify

```
curl -s https://java-job-fit.onrender.com/api/health/db
# {"service":"JavaJobFit API",...,"status":"ok","database":"reachable"}
```
