-- Show public tables
select table_name
from information_schema.tables
where table_schema = 'public'
order by table_name;

-- Show Flyway migration history
select *
from flyway_schema_history
order by installed_rank;

-- Check for suspicious raw/private columns
select table_name, column_name
from information_schema.columns
where table_schema = 'public'
and (
  lower(column_name) like '%raw%'
  or lower(column_name) like '%resume%'
  or lower(column_name) like '%job%'
  or lower(column_name) like '%description%'
)
order by table_name, column_name;

-- Canary privacy test search
select 'reports' as table_name, id::text
from reports r
where row_to_json(r)::text ilike '%DO_NOT_STORE%'

union all

select 'leads' as table_name, id::text
from leads l
where row_to_json(l)::text ilike '%DO_NOT_STORE%'

union all

select 'feedback' as table_name, id::text
from feedback f
where row_to_json(f)::text ilike '%DO_NOT_STORE%'

union all

select 'events' as table_name, id::text
from events e
where row_to_json(e)::text ilike '%DO_NOT_STORE%';
