# Backend Roadmap

JavaJobFit is already past the browser-only MVP stage. The backend is live and
is now part of the product’s normal flow.

## Current Backend

- Java 11
- Spring Boot 2.7
- Render web service
- Supabase PostgreSQL in production
- H2 file storage for local development

Current endpoints:

- `POST /api/reports`
- `GET /api/reports/{id}`
- `POST /api/leads`
- `POST /api/feedback`
- `GET /api/health`
- `GET /`

Current responsibilities:

- Generate and persist saved report output
- Return a free preview response without full premium report content
- Avoid storing raw resume or job description text
- Capture optional lead emails for Pro report early access
- Accept user feedback linked to saved reports
- Provide health and root status endpoints

## Phase 1 Complete

Shipped behavior today:

- Frontend sample loading is input-only
- Report generation happens only on `Analyze fit`
- `Clear` resets fields and rendered output
- Frontend can fall back to browser-only analysis if the backend is unavailable

## Next Backend Work

- Add more API coverage around end-to-end saved report and feedback flows
- Add lightweight analytics or admin visibility for popular job-skill gaps
- Add request logging and production-safe observability
- Improve feedback moderation and follow-up workflow

## Phase 2 Options

Potential product upgrades:

- Saved report history per user
- Auth for returning users
- Resume export with branded PDF output
- Admin dashboard for reports and feedback
- Rate limiting and abuse protection

## Phase 3 AI Upgrade

Add AI only if usage justifies the cost and product complexity.

Paid features:

- Full resume rewrite
- Cover letter generation
- Role-specific mock interview
- Saved prep history
- PDF report with branding

Low-risk AI approach:

- Keep the current browser algorithm as the free tier
- Put AI rewrite behind login and payment
- Store only what users intentionally save
- Show a privacy note before processing resume text
