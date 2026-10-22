# JavaJobFit Production Audit — 22 Oct 2026

Scope: whole repository plus the live site (javajobfit.com) and API (java-job-fit.onrender.com).
Method: code review across four areas (security, backend, DevOps, frontend). Live checks with curl
and the browser at 320/375px and desktop. Full backend test suite run. The Docker image was built
and run under a 512MB limit. All checks were defensive and read-only against production: nothing
destructive, no credentials touched.

Scores and severities are engineering judgement, not a certification.

## 1. Executive summary

JavaJobFit is a small, well-structured product. Its core rules hold up under review:

- Raw resume text is never stored.
- The paywall is enforced on the server.
- Webhooks are signature-checked.
- No secrets were found in the code or in git history.

The audit found **two serious security risks**, both suspected from version or config evidence
rather than exploited:

1. **The resume upload library (Apache Tika 2.9.2) had a published file-read flaw** (XFA forms in
   a PDF). In theory, a crafted PDF could read the server's environment, which holds the database
   password and Razorpay keys. **Fixed:** upgraded to Tika 3.3.2, with PDF form parsing switched off.
2. **The Supabase tables may be reachable through Supabase's own data API**, because row-level
   security is not enabled. **Not fixed.** It needs a one-minute check in the Supabase dashboard
   before any change (see §9).

It also found **one payment bug and one payment-reliability bug**, both now fixed:

- After a price change, someone paying an old ₹89 link would be charged and get nothing.
- One slow Razorpay response could make a report impossible to buy for good.

Production readiness for the current beta and early revenue: **good once this batch is deployed
and the Supabase check is done.**

## 2. Security findings

| Severity | Finding | Location | Impact | Status |
|---|---|---|---|---|
| Critical (suspected) | Tika 2.9.2 XFA/XXE file read (CVE-2025-54988, CVE-2025-66516) | `backend/pom.xml`, `ResumeExtractController` | A crafted PDF could leak env vars (DB password, Razorpay keys) | **Fixed**: Tika 3.3.2, AcroForm/XFA extraction off, container no longer runs as root |
| High (suspected) | No row-level security on Supabase `public` tables | Flyway V1–V9 | Anyone with the project's anon key could read leads or set `paid=true` | **Open, needs approval** (§9) |
| Medium | Unlimited JSON body size; a 100MB POST could crash the 192MB heap | all JSON endpoints | Denial of service | **Fixed**: 128KB cap, 413 response, chunked bodies counted |
| Medium | Rate limiter skipped by decorated paths (`/api;x=1/reports`, `//api/...`) | `RateLimitFilter` | Abuse limits bypassed | **Fixed**: every POST is limited, paths normalised |
| Medium | No retention on anonymous reports/events; could fill the 500MB free DB | `ReportService`, `EventController` | DB goes read-only | **Open, needs approval** (deletes data) |
| Medium | IPv6 clients get a fresh limit per address; >10k keys resets all counters | `RateLimitFilter` | Weaker abuse limits | Open (low urgency) |
| Low | Webhook did not check currency | `PaymentService` | A non-INR payment could unlock | **Fixed** |
| Low | H2 console could be enabled in prod by an env var | `application.yml` | Remote code execution if switched on | **Fixed**: forced off in the prod profile |
| Low | No security headers on API responses | backend | Defence in depth | **Fixed**: `nosniff`, `DENY` framing, `no-referrer`, `no-store` |
| Low | Unhandled errors were not logged at all | `ApiExceptionHandler` | Payment failures invisible | **Fixed**: class names and failing line only (no messages, so no resume text can leak) |
| Low | Checkout redirect followed any URL | `app.js startCheckout` | Defence in depth | **Fixed**: https only |
| Low | Workflow had default token permissions | `keepalive.yml` | Least privilege | **Fixed**: `permissions: {}` |
| Info | No CSP on the static site | all HTML | XSS defence in depth (no XSS sink found) | Open |
| Info | Lead emails have no double opt-in | `LeadController` | List quality / consent | Open |

Checked and fine:

- SQL injection: parameterised queries only.
- XSS: everything is rendered with `textContent`.
- Report IDs: random UUIDv4 values, so there is no IDOR.
- Mass assignment: none.
- Open redirects: none.
- Errors: stack traces are never returned.
- CORS: there is an allow-list, and a foreign origin gets 403 (checked live).
- Webhook: HMAC compared in constant time, and unsigned requests get 401 (checked live).
- Admin endpoints: no actuator and no `/h2-console` exposed (checked live).

## 3. Architecture

**Current setup.** A vanilla-JS static site on GitHub Pages calls a single Spring Boot 2.7 / Java 11
service on Render (Docker, 512MB, sleeps when idle). That service talks to Supabase Postgres
through the session pooler, with Flyway migrations. Razorpay Payment Links handle payment, and a
signed webhook is the only path to `paid=true`.

There are three trust boundaries:

- browser → API
- Razorpay → webhook
- API → Supabase

**Weaknesses:**

- **Single instance on a free tier.** There is no redundancy and it has cold starts; keep-warm jobs
  mitigate this.
- **Spring Boot 2.7 is out of support**, so no more security patches.
- **`AnalysisService` is a 1,400-line god class.**
- **Tests run on H2 only**, never on Postgres.

**Improved in this batch:**

- The Razorpay call no longer holds a DB connection inside a transaction.
- The Hikari pool is sized for the Supabase pooler (4 connections, was 10).
- Graceful shutdown on deploy.
- The JVM exits on out-of-memory instead of limping on.
- The Tika parser is built once instead of on every upload.
- Upload load-shedding happens before the file is read into memory.

**Remaining technical debt:**

- Move to Spring Boot 3 / Java 17 (large job).
- Split `SkillCatalog` and `ResumeProfile` out of `AnalysisService`.
- Add foreign keys from `payments`, `leads` and `feedback` to `reports`.
- Make `markPaid` an atomic update.

## 4. UI/UX findings

The pages were checked live at 320px, 375px and desktop. There is no horizontal scroll, and the
scan-to-results flow works on a phone.

| Issue | Fix status |
|---|---|
| Payment popup: focus not moved in, Escape did nothing, no focus trap, focus not returned (verified live) | **Fixed** |
| Upload button unreachable by keyboard (`hidden` file input) | **Fixed**: visually hidden input instead |
| Focus ring ~1.5:1 contrast (WCAG needs 3:1) | **Fixed**: solid accent ring, now also on links |
| Whole results panel was `aria-live`, flooding screen readers with progress ticks | **Fixed**: one `role=status` on the progress text |
| Resume or job description over 12,000 characters showed "paste both your resume…" | **Fixed**: exact length and limit shown, client and server |
| Returning from checkout with a cold backend could show "Payments are not live yet" to someone who just paid | **Fixed**: shows only "Confirming your payment…"; confirmation starts without waiting for the config call |
| `paid=1` stayed in the URL, so every reload reopened the popup | **Fixed** |
| Late scan result could overwrite the screen after Clear or Sample | **Fixed**: stale results dropped |
| Copy button could stick on "Copied" | **Fixed** |
| Feedback over 2,000 characters failed with a generic error | **Fixed**: `maxlength` |
| Link previews (LinkedIn, WhatsApp, X) showed no image (SVG og:image) | **Fixed**: 1200×630 PNG on all 26 pages |
| Static pages had stale CSS cache-bust versions | **Fixed**: one version across all pages |
| On phones the scan form sits a full screen below the hero | Open (P2) |

## 5. Product recommendations

| Priority | Recommendation | Impact | Effort |
|---|---|---|---|
| P0 | Get traffic: Reddit and LinkedIn launch posts. Payments work; visitors are the bottleneck. | Very high | Low |
| P1 | Show a sample paid report (screenshot or "see a full example") next to the ₹89 button. Buyers cannot see what they get. | High | Low |
| P1 | Supabase check and RLS (§9). Protects the paywall and lead emails. | High | Low |
| P2 | Bring the scan form above the fold on mobile (shorter hero or a "Start scan" button) | Medium | Low |
| P2 | Data retention for old events (and free reports, if needed) before the DB nears 500MB | Medium | Low |
| P3 | Razorpay Payment Page for the ₹999 review (receipt, hides personal UPI) | Low–Med | Low |

## 6. Code changes (uncommitted, awaiting approval)

- **Payments** (`PaymentService`, `RazorpayClient`):
  - The webhook checks the paid link's stored amount, not today's price.
  - Old-price links are not reused after a price change.
  - Each link gets a unique `reference_id` (UUID plus 3 random characters), so a lost Razorpay
    response can no longer make a report unbuyable. Old plain-UUID references still work.
  - The webhook refuses non-INR payments.
  - The webhook ignores events with no link id.
  - Both `markPaid` methods are idempotent. Replays and late events keep the first payment time and id.
  - A second paid link for an already-unlocked report is recorded (so it can be refunded) without changing the unlock.
  - A signed but unreadable webhook is acknowledged instead of being retried forever.
  - Razorpay errors are logged.
  - No DB transaction is held during the HTTP call.
- **Upload** (`pom.xml`, `ResumeExtractController`):
  - Tika 3.3.2, with commons-lang3 3.20.0 because Boot 2.7's 3.12 crashes Tika 3.
  - AcroForm/XFA extraction is off.
  - Load is shed before reading the file.
  - Line breaks are kept, so uploaded resumes score like pasted ones.
  - One shared parser.
- **New filters:**
  - `RequestBodyLimitFilter`: a 128KB cap on declared length, and on chunked bodies through both the stream and the reader.
  - `SecurityHeadersFilter`.
  - The filters run in an explicit order: headers, then body cap, then rate limit.
  - Tomcat's form-post limit is also set to 128KB.
- **`RateLimitFilter`:** every POST is limited, and paths are normalised.
- **`ApiExceptionHandler`:** safe error logging.
- **`ReportRequest`:** separate too-short and too-long messages.
- **Config** (`application-prod.yml`): Hikari pool, H2 console off, graceful shutdown.
- **`Dockerfile`:** non-root user, `-XX:+ExitOnOutOfMemoryError`.
- **Frontend** (`app.js`, `index.html`, `styles.css`, all pages): the §4 fixes, the OG PNG, cache
  versions, and the sitemap using `/` instead of `/index.html`.
- **DevOps:**
  - New `.github/workflows/ci.yml` runs the tests on every backend push.
  - `keepalive.yml` gets `permissions: {}`.
  - `DEPLOYMENT.md` and `render-env.example.txt` now give the session-pooler host. The old
    direct-host guidance would have broken the Render connection.
  - Backup and rollback sections added.
  - `KEEP_WARM_SETUP.md` heap note corrected.

Side effects to know about:

- **Bigger jar.** Tika 3 makes it 84MB. Boot is still 8s and memory about 160–200MB under a 512MB
  limit (measured).
- **Text format change.** Uploaded text now keeps its line breaks.
- **Razorpay dashboard.** The `reference_id` shown there now ends in `-xxx`.

## 7. Test results

- **Backend:** 109 tests, 109 passed, 0 failed, 0 skipped (`mvn ... test`). The suite had 93
  before; 16 regression tests were added for the fixes, including:
  - price change
  - stuck link
  - currency
  - unreadable webhook
  - path bypass
  - 413 cap
  - headers
  - premium-field leakage
  - newline keeping
  - duplicate, late and out-of-order webhooks
  - chunked bodies over the stream and the reader
- **A real regression was caught and fixed during the work:** Tika 3 failed PDF extraction until
  commons-lang3 was pinned.
- **Docker image, built and run with `-m 512m`:**
  - Boots in about 8s.
  - Runs as uid 10001.
  - Real PDF and DOCX extraction works, with line structure kept.
  - A 200KB body gets 413; a chunked oversized body gets 400.
  - Security headers are present.
  - `;x=1` path requests are rate-limited (429 after 30).
  - A scan returns 201.
  - Peak memory was 196MiB.
- **Frontend, checked in the browser:**
  - The too-long message appears.
  - The file input is focusable.
  - The popup takes focus, traps Tab, closes on Escape and returns focus.
  - There are no JS errors (CORS errors on localhost are expected).
- **Not run:**
  - OWASP dependency-check, because it needs a large NVD download. Dependabot is the free
    alternative.
  - Tests against Postgres.
  - Load tests.

## 8. Production readiness scorecard (engineering judgement)

| Category | Score | Key findings |
|---|---|---|
| Architecture | 7/10 | Simple and right-sized; single free-tier instance; one god class |
| Backend quality | 7/10 | Clean layering and DTOs; old Spring Boot |
| Frontend quality | 7/10 | Safe DOM use, no framework bloat; a 1,500-line `app.js` |
| UI/UX | 7/10 | Clear and consistent; accessibility gaps now fixed; mobile form below the fold |
| Cybersecurity | 7/10 after this batch (5 before) | Tika fixed; Supabase RLS still to confirm |
| API security | 8/10 | Server-side paywall, signed webhook, caps and limits |
| Database design | 7/10 | Good migration discipline; no FKs or retention; free-tier size limit |
| Performance | 6/10 | Fine when warm; free-tier cold starts are the real limit |
| Scalability | 5/10 | Fine for beta; one 512MB instance by choice |
| Testing | 7/10 | 104 tests on the money and privacy paths; no Postgres or e2e tests |
| DevOps | 6/10 after this batch | CI added; backups manual; no alerting beyond cron-job.org emails |
| Product experience | 7/10 | Fast free value; needs a visible sample of the paid report |

## 9. Remaining risks and what is needed from you

1. **Supabase RLS (High).**
   - In Supabase → SQL Editor, run:
     `select tablename, tableowner, rowsecurity from pg_tables where schemaname = 'public';`
   - Then open Advisors → Security Advisor.
   - If RLS shows as off and the owner is `postgres` (the role the backend uses), a small migration
     can enable it with no effect on the app. Or, simpler, turn off the Data API in
     Settings → API, since the app never uses it.
2. **Data retention.** A scheduled delete of old `events` (for example, older than 90 days). This
   deletes production data, so it needs your yes.
3. **Render: Auto-Deploy → "After CI Checks Pass"**, once `ci.yml` is pushed.
4. **Weekly `pg_dump`.** The command is in `DEPLOYMENT.md`.
5. **Spring Boot 3 / Java 17.** A planned project after launch, not now.

## 10. Action plan

- **Immediate:**
  - Approve and push this batch.
  - Run the Supabase check.
  - Set Render to deploy after CI.
- **Short term:**
  - Launch posts.
  - A sample Pro report next to the pay button.
  - Events retention.
  - First `pg_dump`.
- **Medium term:**
  - Postgres-backed tests.
  - IPv6 /64 rate-limit grouping and global write caps.
  - FKs and an atomic `markPaid`.
  - Split `AnalysisService`.
- **Long term:**
  - Spring Boot 3 / Java 17.
  - CSP.
  - Lead double opt-in.
  - Render Starter once revenue pays for it (your rule).
