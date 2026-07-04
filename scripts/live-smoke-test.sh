#!/usr/bin/env bash
set -euo pipefail

if [ -z "${PROD_API_BASE_URL:-}" ]; then
  echo "PROD_API_BASE_URL is required, for example:"
  echo "PROD_API_BASE_URL=\"https://java-job-fit.onrender.com\" bash scripts/live-smoke-test.sh"
  exit 1
fi

if ! command -v python3 >/dev/null 2>&1; then
  echo "python3 is required for JSON parsing."
  exit 1
fi

BASE_URL="${PROD_API_BASE_URL%/}"
TMP_DIR="$(mktemp -d)"
trap 'rm -rf "$TMP_DIR"' EXIT

fail() {
  echo "FAIL: $1"
  exit 1
}

request() {
  local method="$1"
  local path="$2"
  local body="$3"
  local expected_status="$4"
  local output_file="$5"
  local status

  if [ -n "$body" ]; then
    status="$(curl -sS -o "$output_file" -w "%{http_code}" \
      -H "Content-Type: application/json" \
      -X "$method" \
      --data "$body" \
      "$BASE_URL$path")"
  else
    status="$(curl -sS -o "$output_file" -w "%{http_code}" \
      -X "$method" \
      "$BASE_URL$path")"
  fi

  if [ "$status" != "$expected_status" ]; then
    echo "Unexpected status for $method $path: got $status, expected $expected_status"
    echo "Response saved at $output_file"
    exit 1
  fi
}

assert_no_sensitive_output() {
  local file="$1"

  if grep -Eqi 'jdbc:|postgresql://|DATABASE_URL|SPRING_DATASOURCE|password|stacktrace|stack trace|java\.lang\.|org\.springframework|org\.postgresql|SQLException|HikariPool|at com\.javajobfit' "$file"; then
    fail "response exposed infrastructure or error details"
  fi
}

assert_no_canary_output() {
  local file="$1"

  if grep -Eq 'DO_NOT_STORE|BETA_CANARY|CANARY' "$file"; then
    fail "response exposed canary resume/JD text"
  fi
}

json_field() {
  local file="$1"
  local field="$2"

  python3 -c 'import json,sys; print(json.load(open(sys.argv[1])).get(sys.argv[2], ""))' "$file" "$field"
}

echo "Checking live API: $BASE_URL"

request "GET" "/api/health" "" "200" "$TMP_DIR/health.json"
assert_no_sensitive_output "$TMP_DIR/health.json"

request "GET" "/api/health/db" "" "200" "$TMP_DIR/health-db.json"
assert_no_sensitive_output "$TMP_DIR/health-db.json"

CANARY="DO_NOT_STORE_BETA_CANARY_$(date +%s)"
REPORT_BODY="$(python3 -c 'import json,sys
marker=sys.argv[1]
print(json.dumps({
  "resumeText": "Java Spring Boot REST SQL JUnit backend developer. Privacy marker " + marker + " should never be persisted as raw input.",
  "jobDescription": "Hiring Java Spring Boot backend engineer with REST APIs, SQL, testing, CI/CD, and cloud deployment experience. Marker " + marker + " must not be stored.",
  "experienceLevel": "oneToThree"
}))' "$CANARY")"

request "POST" "/api/reports" "$REPORT_BODY" "201" "$TMP_DIR/report.json"
assert_no_sensitive_output "$TMP_DIR/report.json"
assert_no_canary_output "$TMP_DIR/report.json"

PUBLIC_ID="$(json_field "$TMP_DIR/report.json" "publicId")"
if ! printf '%s' "$PUBLIC_ID" | grep -Eq '^[0-9a-fA-F-]{36}$'; then
  fail "report response did not include a public UUID"
fi

echo "Created public report ID: $PUBLIC_ID"

request "GET" "/api/reports/$PUBLIC_ID" "" "200" "$TMP_DIR/report-get.json"
assert_no_sensitive_output "$TMP_DIR/report-get.json"
assert_no_canary_output "$TMP_DIR/report-get.json"

request "GET" "/api/reports/1" "" "404" "$TMP_DIR/report-internal-id.json"
assert_no_sensitive_output "$TMP_DIR/report-internal-id.json"

LEAD_FALSE_BODY="$(python3 -c 'import json,sys
print(json.dumps({
  "email": "beta-smoke@example.com",
  "experienceLevel": "oneToThree",
  "country": "India",
  "reportId": sys.argv[1],
  "consent": False,
  "source": "scan_result"
}))' "$PUBLIC_ID")"

request "POST" "/api/leads" "$LEAD_FALSE_BODY" "400" "$TMP_DIR/lead-false.json"
assert_no_sensitive_output "$TMP_DIR/lead-false.json"

LEAD_TRUE_BODY="$(python3 -c 'import json,sys
print(json.dumps({
  "email": "beta-smoke@example.com",
  "experienceLevel": "oneToThree",
  "country": "India",
  "reportId": sys.argv[1],
  "consent": True,
  "source": "scan_result"
}))' "$PUBLIC_ID")"

request "POST" "/api/leads" "$LEAD_TRUE_BODY" "201" "$TMP_DIR/lead-true.json"
assert_no_sensitive_output "$TMP_DIR/lead-true.json"

FEEDBACK_BODY="$(python3 -c 'import json,sys
print(json.dumps({
  "reportId": sys.argv[1],
  "email": "beta-smoke@example.com",
  "message": "Smoke test feedback: report generated and beta flow is reachable."
}))' "$PUBLIC_ID")"

request "POST" "/api/feedback" "$FEEDBACK_BODY" "201" "$TMP_DIR/feedback.json"
assert_no_sensitive_output "$TMP_DIR/feedback.json"

EVENT_BODY="$(python3 -c 'import json,sys
print(json.dumps({
  "eventName": "beta_smoke_test",
  "pagePath": "/java-job-fit/index.html?private=ignored",
  "reportPublicId": sys.argv[1],
  "experienceLevel": "oneToThree",
  "country": "India",
  "source": "live_smoke_test"
}))' "$PUBLIC_ID")"

request "POST" "/api/events" "$EVENT_BODY" "201" "$TMP_DIR/event.json"
assert_no_sensitive_output "$TMP_DIR/event.json"

echo "PASS: live smoke test completed without exposing canary text or infrastructure details."
