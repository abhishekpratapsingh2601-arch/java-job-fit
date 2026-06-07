# JavaJobFit Beta Test Checklist

## Production backend

- /api/health works
- Render logs show successful startup
- Flyway migrations applied
- Supabase tables exist
- No raw resume/JD columns exist
- Canary strings not stored
- Canary strings not logged

## Production frontend

- Page loads
- No console errors
- Try sample resume works
- Analyze my Java resume works
- Score appears
- Top fixes appear
- Free preview is limited
- Premium cards are locked
- Email capture works
- Feedback works
- Copy buttons work
- Export report works
- Privacy/Terms/Contact links work
- Mobile view has no horizontal scroll

## Beta target

- 50 visitors
- 20+ completed scans
- 5+ email captures
- 3+ feedback messages
- 3+ premium CTA clicks

## Profitability target

- 100 visitors
- 30+ completed scans
- 10+ email captures
- 5+ premium CTA clicks

## Payment trigger

Only add real Razorpay/Stripe when:

- 100+ visitors
- 30+ completed scans
- 5-10 premium CTA clicks

## Pricing

- India pricing: ₹89 one-time Pro Report
- International pricing: $9.99 one-time Pro Report

## Paid ads rule

Do not run paid ads until:

- production app is stable
- analytics/events work
- canary privacy test passes
- at least 50 real users tested it

## Beta sharing message

I built JavaJobFit, a free ATS-style resume checker for Java/Spring Boot developers.

Paste your resume and a Java job description, and it gives:
- ATS-style match score
- missing keywords
- stronger resume bullets
- Java interview questions
- 7-day prep plan

Raw resume text and job descriptions are not stored.

I'm looking for feedback before turning it into a paid product:
[YOUR LINK]
