# JavaJobFit LLM Pro Plan

This is a planning document only. JavaJobFit does not call an LLM provider in the current beta build.

## Goal

Use an LLM later to improve premium report quality without weakening the current privacy posture.

## Candidate Pro Features

- Deeper resume bullet rewrites with role-specific language.
- Keyword placement suggestions by resume section.
- Tailored Java/Spring Boot professional summary.
- Cover letter draft based on generated report output.
- LinkedIn headline and About rewrite.

## Privacy Guardrails

- Do not store raw resume text or raw job description text.
- Do not send raw resume or raw job description to an LLM until the product has explicit user consent and a clear privacy policy update.
- Prefer sending structured, minimized signals derived from the scan where possible.
- Do not log prompts, completions, raw resumes, raw job descriptions, or uploaded files.
- Keep provider API keys only in deployment environment variables.

## Implementation Notes For Later

- Add a feature flag such as `LLM_PROVIDER_ENABLED=false`.
- Add provider-specific settings only after payment and consent flows exist.
- Keep free responses limited so locked premium content is not sent to the browser.
- Add tests proving private canary markers are not persisted or logged.

## Current Status

No LLM dependencies, API calls, provider keys, or runtime configuration have been added.
