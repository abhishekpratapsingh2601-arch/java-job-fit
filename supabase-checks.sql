-- Activity/connectivity check.
select now() as database_time;

-- Show public app tables.
select
  table_schema,
  table_name,
  table_type
from information_schema.tables
where table_schema = 'public'
  and table_type = 'BASE TABLE'
order by table_schema, table_name;

-- Show Flyway migration history.
select
  installed_rank,
  version,
  description,
  script,
  success,
  installed_on
from flyway_schema_history
order by installed_rank desc
limit 20;

-- Check for suspicious raw/private storage columns.
select
  table_schema,
  table_name,
  column_name,
  data_type
from information_schema.columns
where table_schema = 'public'
  and lower(column_name) in (
    'resume_text',
    'raw_resume',
    'resume_content',
    'resume_body',
    'job_description',
    'raw_job_description',
    'jd_text',
    'raw_jd',
    'jd_content',
    'uploaded_file',
    'file_blob'
  )
order by table_schema, table_name, column_name;

-- Confirm reports.public_id exists.
select
  column_name,
  data_type,
  is_nullable
from information_schema.columns
where table_schema = 'public'
  and table_name = 'reports'
  and column_name = 'public_id';

-- Confirm report public IDs are present and unique.
select
  count(*) as total_reports,
  count(public_id) as reports_with_public_id,
  count(distinct public_id) as unique_public_ids
from reports;

-- Canary privacy test search across JavaJobFit app tables.
-- Expected result: every canary_matches value is 0.
select
  'reports' as table_name,
  count(*) as canary_matches
from reports r
where row_to_json(r)::text ilike any (array['%DO_NOT_STORE%', '%CANARY%']);

select
  'leads' as table_name,
  count(*) as canary_matches
from leads l
where row_to_json(l)::text ilike any (array['%DO_NOT_STORE%', '%CANARY%']);

select
  'feedback' as table_name,
  count(*) as canary_matches
from feedback f
where row_to_json(f)::text ilike any (array['%DO_NOT_STORE%', '%CANARY%']);

select
  'events' as table_name,
  count(*) as canary_matches
from events e
where row_to_json(e)::text ilike any (array['%DO_NOT_STORE%', '%CANARY%']);
