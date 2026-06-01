UPDATE reports SET public_id = RANDOM_UUID() WHERE public_id IS NULL;
ALTER TABLE reports ALTER COLUMN public_id SET NOT NULL;
CREATE UNIQUE INDEX idx_reports_public_id ON reports (public_id);
