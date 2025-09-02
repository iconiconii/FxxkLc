#!/usr/bin/env bash
set -euo pipefail

# Dev-only helper to validate AFTER_COMMIT + delayed double-delete
# Requires: backend running with dev profile, curl, jq (optional)

API_URL="${API_URL:-http://localhost:8080/api/v1}"
EMAIL="${EMAIL:-2441933762@qq.com}"
PASSWORD="${PASSWORD:-password123_}"
PROBLEM_ID="${PROBLEM_ID:-1}"
RATING="${RATING:-3}"
DELAY_WAIT_SECS="${DELAY_WAIT_SECS:-1.2}"

red() { printf "\033[31m%s\033[0m\n" "$*"; }
green() { printf "\033[32m%s\033[0m\n" "$*"; }
yellow() { printf "\033[33m%s\033[0m\n" "$*"; }

need() { command -v "$1" >/dev/null 2>&1 || { red "Missing dependency: $1"; exit 1; }; }
need curl

login() {
  yellow "Logging in as $EMAIL ..."
  local http
  http=$(curl -s -o /tmp/login_body.json -w '%{http_code}' \
    -H 'Content-Type: application/json' \
    --data "{\"identifier\":\"$EMAIL\",\"password\":\"$PASSWORD\"}" \
    "$API_URL/auth/login") || true
  if [[ "$http" != "200" ]]; then red "Login failed (HTTP $http)"; cat /tmp/login_body.json; exit 1; fi
  if command -v jq >/dev/null 2>&1; then
    ACCESS_TOKEN=$(jq -r '.accessToken' /tmp/login_body.json)
  else
    ACCESS_TOKEN=$(sed -n 's/.*"accessToken":"\([^"]*\)".*/\1/p' /tmp/login_body.json | head -n1)
  fi
  [[ -n "${ACCESS_TOKEN:-}" ]] || { red "Failed to parse access token"; exit 1; }
  green "Login OK"
}

issue_admin_token() {
  yellow "Requesting dev admin token ..."
  local http
  http=$(curl -s -o /tmp/admin_token.json -w '%{http_code}' \
    -H "Authorization: Bearer $ACCESS_TOKEN" \
    -X POST "$API_URL/dev/admin-token") || true
  if [[ "$http" != "200" ]]; then red "Dev admin-token failed (HTTP $http)"; cat /tmp/admin_token.json; exit 1; fi
  if command -v jq >/dev/null 2>&1; then
    ADMIN_TOKEN=$(jq -r '.accessToken' /tmp/admin_token.json)
  else
    ADMIN_TOKEN=$(sed -n 's/.*"accessToken":"\([^"]*\)".*/\1/p' /tmp/admin_token.json | head -n1)
  fi
  [[ -n "${ADMIN_TOKEN:-}" ]] || { red "Failed to parse admin token"; exit 1; }
  green "Admin token OK"
}

publish_events() {
  yellow "Publishing ProblemEvent(UPDATED) ..."
  curl -s -D /tmp/h_evt_problem.txt -o /tmp/b_evt_problem.txt \
    -H "Authorization: Bearer $ADMIN_TOKEN" \
    -X POST "$API_URL/admin/test/events/problem?type=UPDATED&problemId=$PROBLEM_ID&title=dev-test" >/dev/null || true
  head -n1 /tmp/h_evt_problem.txt || true; cat /tmp/b_evt_problem.txt || true

  yellow "Publishing CompanyEvent(FREQUENCY_UPDATED) ..."
  curl -s -D /tmp/h_evt_company.txt -o /tmp/b_evt_company.txt \
    -H "Authorization: Bearer $ADMIN_TOKEN" \
    -X POST "$API_URL/admin/test/events/company?type=FREQUENCY_UPDATED&companyId=1&companyName=ACME" >/dev/null || true
  head -n1 /tmp/h_evt_company.txt || true; cat /tmp/b_evt_company.txt || true

  yellow "Publishing ReviewEvent(REVIEW_COMPLETED) ..."
  curl -s -D /tmp/h_evt_review.txt -o /tmp/b_evt_review.txt \
    -H "Authorization: Bearer $ADMIN_TOKEN" \
    -X POST "$API_URL/admin/test/events/review?type=REVIEW_COMPLETED&problemId=$PROBLEM_ID&rating=$RATING" >/dev/null || true
  head -n1 /tmp/h_evt_review.txt || true; cat /tmp/b_evt_review.txt || true
}

submit_review_api() {
  yellow "Submitting review via /review/submit (business API) ..."
  local http
  http=$(curl -s -o /tmp/rev_submit.json -w '%{http_code}' \
    -H "Authorization: Bearer $ACCESS_TOKEN" -H 'Content-Type: application/json' \
    -X POST --data "{\"problemId\":$PROBLEM_ID,\"rating\":$RATING,\"reviewType\":\"SCHEDULED\"}" \
    "$API_URL/review/submit") || true
  echo "HTTP $http"; head -c 200 /tmp/rev_submit.json || true; echo
}

show_logs_hint() {
  yellow "If running locally with backend.out available, search logs for:" \
    "\n  - 'handling .* event'\n  - 'cache invalidation completed synchronously'\n  - 'double delete'"
  if [[ -f backend.out ]]; then
    echo; green "Recent matching logs:"; rg -n "handling .* event|double delete|cache invalidation completed synchronously" backend.out | tail -n 40 || true
  fi
}

main() {
  login
  issue_admin_token
  publish_events
  submit_review_api
  sleep "$DELAY_WAIT_SECS"
  show_logs_hint
  green "Done."
}

main "$@"

