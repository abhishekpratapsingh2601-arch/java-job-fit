# JavaJobFit

JavaJobFit is a zero-cost MVP for Java developers who want to tailor their
resume and interview prep to a specific job description.

## What The MVP Does

- Accepts resume text, target Java job description, and experience level.
- Calculates a lightweight ATS-style fit score.
- Finds matched Java/backend skills and missing keywords.
- Suggests stronger resume bullet directions.
- Generates Java interview questions and a 7-day prep plan.
- Runs fully in the browser, so there is no API cost for version 1.

## Free Launch Path

1. Ship this static version on GitHub Pages, Netlify, or Vercel.
2. Share it in Java, Spring Boot, and job-search communities.
3. Add feedback capture with a free Google Form or Tally form.
4. Add a paid AI upgrade later only after users show demand.

## Next Features

- Export report as PDF.
- Add copy buttons for resume bullets.
- Add sample resume and sample job description.
- Add backend persistence for saved reports.
- Add optional AI-powered rewrite mode with user-provided API key or paid plan.

## Local Use

Open `index.html` directly in a browser, or serve the folder with:

```bash
python3 -m http.server 4173
```

Then visit:

```text
http://localhost:4173
```

## Backend

Start without a backend to launch fast and stay at zero cost. See
`BACKEND_ROADMAP.md` for the Spring Boot backend plan once saved reports,
feedback, login, or paid AI features are needed.

The repository now includes a deploy-ready Spring Boot backend in `backend/`.
The frontend uses browser-only analysis until `config.js` is updated with a
live backend URL.

See `DEPLOYMENT.md` for the Render + Supabase deployment steps.
