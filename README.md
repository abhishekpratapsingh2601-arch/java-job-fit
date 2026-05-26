# JavaJobFit

JavaJobFit helps Java developers tailor a resume and interview prep plan to a
specific job description.

Current production setup:

- Frontend: HTML/CSS/JS on GitHub Pages
- Backend: Java 11 Spring Boot on Render
- Database: Supabase PostgreSQL

## What It Does

- Accepts resume text, target Java job description, and experience level
- Calculates an ATS-style fit score
- Finds matched Java/backend skills and missing keywords
- Suggests stronger resume bullet directions
- Generates Java interview questions and a 7-day prep plan
- Saves generated reports through the backend API while returning only the free preview to the browser
- Captures optional lead emails for Pro report early access
- Accepts product feedback tied to saved reports

## Current UX Rules

- `Try sample resume` fills the resume, job description, and experience level only
- `Analyze my Java resume` generates the report
- `Clear` resets the inputs and all rendered results
- Paid plans are labeled as coming soon / early access until payment is configured

## Local Frontend Use

Open `index.html` directly in a browser, or serve the folder with:

```bash
python3 -m http.server 4173
```

Then visit:

```text
http://localhost:4173
```

The frontend is already configured to use the live Render backend in
`config.js`.

## Local Backend Use

The backend uses a repo-local Maven cache and settings so it stays isolated on
this shared laptop:

```text
backend/.m2
backend/.mvn/settings.xml
```

Run it with:

```bash
cd backend
mvn test
mvn spring-boot:run
```

See `backend/README.md`, `PROJECT_ISOLATION.md`, and `DEPLOYMENT.md` for the
backend, isolation, and deploy details.
