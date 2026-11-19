#!/usr/bin/env bash
# The measurable part of the quarterly check of the plan (section 17), against the public demo:
# the time to the first answer, a login with the public demo manager, the results of the mobility
# survey timed five times, the workbook export, the logout, and the figures that GitHub can give
# (open Dependabot pull requests, the last demo reset) when the gh command is present. It prints
# the values for the row of docs/known-issues.md; the hours of Neon and Render come from their
# dashboards. The session token lives in a variable and is never printed or written.
#
# Usage: deploy/quarterly-check.sh            (needs curl and python3; gh is optional)
# For a cold start, run it before the Worker's cron has woken the API: before 06:00 UTC.
set -euo pipefail

API="${DELFOS_API:-https://api.delfos.manuelalejandro.com/api/v1}"
USERNAME="${DELFOS_DEMO_USER:-demo-manager}"
PASSWORD="${DELFOS_DEMO_PASSWORD:-delfos-demo}"
SURVEY="${DELFOS_DEMO_SURVEY:-demo-mobility}"
CLIENT="quarterly-check"

json() { python3 -c "import json, sys; print(json.load(sys.stdin)$1)"; }
seconds() { local timeout=$1; shift; curl --silent --output /dev/null --write-out '%{time_total}' --max-time "$timeout" "$@"; }

echo "Date: $(date -u +%Y-%m-%d) at $(date -u +%H:%M) UTC, against $API"

first=$(curl --silent --output /dev/null --write-out '%{http_code} %{time_total}' --max-time 180 \
  "$API/public/surveys/$SURVEY" || echo "000 timeout")
echo "First answer: HTTP ${first% *} after ${first#* } s (a cold start takes over a minute; a warm API answers under a second, or a few seconds while only its database wakes)"

token=$(curl --silent --fail --max-time 60 -X POST "$API/auth/sessions" \
  -H 'Content-Type: application/json' -H "X-Delfos-Client: $CLIENT" \
  -d "{\"username\":\"$USERNAME\",\"password\":\"$PASSWORD\",\"transport\":\"BEARER\"}" | json '["token"]')
auth=(-H "Authorization: Bearer $token")
echo "Login as $USERNAME: ok"

id=$(curl --silent --fail --max-time 30 "$API/surveys?pageSize=50" "${auth[@]}" | python3 -c "
import json, sys
d = json.load(sys.stdin); items = d.get('items', d)
print(next(s['id'] for s in items if s.get('publicId') == '$SURVEY'))")
version=$(curl --silent --fail --max-time 30 "$API/public/surveys/$SURVEY" | json '["version"]["number"]')

times=()
for _ in 1 2 3 4 5; do
  times+=("$(seconds 30 "$API/surveys/$id/versions/$version/results" "${auth[@]}")")
done
echo "Results of $SURVEY, version $version, five calls: ${times[*]} s (the first may include the database waking)"

export_time=$(seconds 60 "$API/surveys/$id/versions/$version/export.xlsx" "${auth[@]}")
echo "Workbook export: $export_time s"

curl --silent --fail --output /dev/null --max-time 30 -X DELETE "$API/auth/sessions/current" \
  "${auth[@]}" -H "X-Delfos-Client: $CLIENT" && echo "Logout: ok"
unset token auth

if command -v gh >/dev/null 2>&1; then
  echo "Dependabot: $(gh pr list --repo majimtor/delfos --author app/dependabot --json number --jq 'if length == 0 then "nothing open" else (map("#" + (.number | tostring)) | join(", ")) + " open" end')"
  echo "Last demo reset: $(gh run list --repo majimtor/delfos --workflow demo-reset.yml --limit 1 --json createdAt,conclusion,event --jq '.[] | "\(.createdAt) \(.event) \(.conclusion)"')"
fi
echo "Still to read by hand: Neon compute hours and storage, Render instance hours; then the row and the line in docs/known-issues.md."
