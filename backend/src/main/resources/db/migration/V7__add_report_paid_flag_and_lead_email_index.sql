-- Money path: a report is either a free teaser or a paid full report. The full content is
-- already generated and stored; this flag is what decides how much of it leaves the server.
-- Only a verified payment webhook may ever set it (see PAYMENT_TODO.md).
ALTER TABLE reports ADD COLUMN IF NOT EXISTS paid BOOLEAN DEFAULT FALSE NOT NULL;
ALTER TABLE reports ADD COLUMN IF NOT EXISTS paid_at TIMESTAMP;

-- Free-scan caps will look leads up by email on every scan; without this the check is a
-- full table scan.
CREATE INDEX IF NOT EXISTS idx_leads_email ON leads (email);
