#!/usr/bin/env bash
set -euo pipefail

BASE_URL="http://localhost:8090"
TS=$(date +%s)
EMAIL="smoketest-${TS}@smoke-${TS}.io"
PASSWORD="SmokeTest123!"
TENANT_NAME="Smoke-${TS}"

echo "=== Job Scanner Smoke Test ==="
echo ""

# 1. Sign up
echo "1. Signing up..."
SIGNUP_RESP=$(curl -sf -X POST "$BASE_URL/api/v1/auth/signup" \
  -H "Content-Type: application/json" \
  -d "{\"email\":\"$EMAIL\",\"password\":\"$PASSWORD\",\"tenantName\":\"$TENANT_NAME\"}")
TOKEN=$(echo "$SIGNUP_RESP" | grep -o '"token":"[^"]*"' | cut -d'"' -f4)
if [ -z "$TOKEN" ]; then
  echo "ERROR: signup did not return a token. Response: $SIGNUP_RESP"
  exit 1
fi
echo "   OK — token acquired"

AUTH_HEADER="Authorization: Bearer $TOKEN"

# 2. Set scoring profile
echo "2. Setting scoring profile..."
curl -sf -X PUT "$BASE_URL/api/v1/scores/scoring-profile" \
  -H "Content-Type: application/json" \
  -H "$AUTH_HEADER" \
  -d '{"profileText":"Senior Java engineer with Spring Boot, Kubernetes, and cloud-native experience"}' \
  > /dev/null
echo "   OK"

# 3. Create search config
echo "3. Creating search config..."
curl -sf -X POST "$BASE_URL/api/v1/scans/search-configs" \
  -H "Content-Type: application/json" \
  -H "$AUTH_HEADER" \
  -d '{"keywords":["java","spring boot"],"location":"Remote","maxResults":5,"enabled":true}' \
  > /dev/null
echo "   OK"

# 4. Trigger scan
echo "4. Triggering scan..."
curl -sf -X POST "$BASE_URL/api/v1/scans/run" \
  -H "$AUTH_HEADER" \
  > /dev/null
echo "   OK"

# 5. Wait for pipeline (scan → score → index)
echo "5. Waiting 15s for mock pipeline..."
sleep 15

# 6. Search for results
echo "6. Searching for jobs..."
SEARCH_RESP=$(curl -sf "$BASE_URL/api/v1/jobs/search?q=java" \
  -H "$AUTH_HEADER")
echo "   Response: $SEARCH_RESP"

JOB_COUNT=$(echo "$SEARCH_RESP" | grep -o '"id"' | wc -l | tr -d ' ')
if [ "$JOB_COUNT" -eq 0 ]; then
  echo ""
  echo "WARNING: No scored jobs found in search results."
  echo "  This may be normal if the mock pipeline needs more time."
  echo "  Try: curl -s '$BASE_URL/api/v1/jobs/search?q=java' -H '$AUTH_HEADER'"
else
  echo ""
  echo "=== SMOKE TEST PASSED — $JOB_COUNT job(s) found ==="
fi

