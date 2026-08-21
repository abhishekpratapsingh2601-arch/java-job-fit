-- Premium sections generated at scan time and released only to paid reports. Generated output
-- only: these are built from skill labels and templates, never from the user's raw resume or
-- job-description text (AnalysisService.buildResumeSummary and friends).
ALTER TABLE reports ADD COLUMN IF NOT EXISTS resume_summary TEXT;
ALTER TABLE reports ADD COLUMN IF NOT EXISTS cover_letter TEXT;
ALTER TABLE reports ADD COLUMN IF NOT EXISTS keyword_placements TEXT;
ALTER TABLE reports ADD COLUMN IF NOT EXISTS linkedin_headline TEXT;
ALTER TABLE reports ADD COLUMN IF NOT EXISTS linkedin_about TEXT;
