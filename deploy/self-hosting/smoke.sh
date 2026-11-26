#!/bin/sh
# Smoke of a self-hosted instance: the API becomes ready, the web answers with its headers, the
# first administrator logs in and sees no survey yet. Usage: smoke.sh <base url> <username> <password>
set -eu
base=${1:?base url, such as http://localhost}
user=${2:?username of the administrator}
pass=${3:?password of the administrator}

code=000
for i in $(seq 1 60); do
  code=$(curl -s -o /dev/null -w '%{http_code}' "$base/actuator/health/readiness" || true)
  [ "$code" = 200 ] && break
  sleep 3
done
[ "$code" = 200 ] || { echo "the API is not ready after three minutes: $code"; exit 1; }

curl -sS -D - -o /dev/null "$base/surveys" | grep -qi '^content-security-policy:' \
  || { echo "the web does not answer with its headers"; exit 1; }

token=$(curl -sS -X POST "$base/api/v1/auth/sessions" \
  -H 'Content-Type: application/json' -H 'X-Delfos-Client: smoke' \
  -d "{\"username\":\"$user\",\"password\":\"$pass\",\"transport\":\"BEARER\"}" \
  | sed -n 's/.*"token":"\([^"]*\)".*/\1/p')
[ -n "$token" ] || { echo "the administrator cannot log in"; exit 1; }

surveys=$(curl -sS "$base/api/v1/surveys" -H "Authorization: Bearer $token" -H 'X-Delfos-Client: smoke')
echo "$surveys" | grep -q '"items":\[\]' || { echo "unexpected surveys: $surveys"; exit 1; }

echo "ok: the web is served, the API is ready, '$user' logged in, no survey yet"
