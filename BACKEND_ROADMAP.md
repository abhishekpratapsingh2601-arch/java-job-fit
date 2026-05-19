# Backend Roadmap

The fastest public launch should stay static first. It costs nothing, loads
fast, and can be hosted on GitHub Pages, Netlify, or Vercel.

Add a backend after the first users confirm they want saved reports, stronger AI
rewrites, account history, or paid plans.

## Phase 1: No Backend

Current MVP:

- Browser-only resume and job analysis.
- No database.
- No login.
- No API keys.
- No hosting cost.

Best for:

- Getting first visitors.
- Sharing on LinkedIn and Java communities.
- Validating whether people actually use the tool.

## Phase 2: Lightweight Backend

Add a Java Spring Boot API when we need persistence.

Recommended stack:

- Java 21
- Spring Boot
- Spring Web
- Spring Validation
- Spring Data JPA
- PostgreSQL on Neon or Supabase free tier
- Render, Fly.io, or Railway free/low-cost deploy

Suggested endpoints:

```text
POST /api/reports
GET /api/reports/{id}
POST /api/feedback
GET /api/health
```

Suggested tables:

```text
reports
- id
- resume_text
- job_description
- experience_level
- score
- matched_skills
- missing_keywords
- generated_report
- created_at

feedback
- id
- report_id
- email
- message
- created_at
```

## Phase 3: AI Upgrade

Add AI only after free users show demand.

Paid features:

- Full resume rewrite.
- Cover letter generation.
- Role-specific mock interview.
- Saved prep history.
- PDF report with branding.

Low-risk AI approach:

- Keep the current browser algorithm as the free tier.
- Put AI rewrite behind login and payment.
- Store only what users intentionally save.
- Show a privacy note before processing resume text.

## GitHub Repo Plan

When GitHub account details are ready:

1. Initialize git in this folder.
2. Create a repo named `java-job-fit` or `javajobfit`.
3. Commit the static MVP.
4. Push to GitHub.
5. Enable GitHub Pages from the main branch.

Recommended first commit message:

```text
feat: launch browser-only Java resume fit MVP
```
