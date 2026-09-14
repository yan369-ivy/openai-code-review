#!/usr/bin/env bash
set -euo pipefail

DEEPSEEK_BASE_URL="${DEEPSEEK_BASE_URL:-https://api.deepseek.com}"
DEEPSEEK_MODEL="${DEEPSEEK_MODEL:-deepseek-flash}"

if [[ -n "${DEEPSEEK_API_KEY:-}" ]]; then
  DEEPSEEK_TOKEN="${DEEPSEEK_API_KEY}"
  echo "Using DEEPSEEK_API_KEY" >&2
else
  echo "Please set DEEPSEEK_API_KEY." >&2
  exit 1
fi

DEEPSEEK_TOKEN="$(printf '%s' "${DEEPSEEK_TOKEN}" | tr -d '\r\n' | sed 's/^[[:space:]]*//;s/[[:space:]]*$//')"

curl -i -X POST \
  -H "Authorization: Bearer ${DEEPSEEK_TOKEN}" \
  -H "Content-Type: application/json" \
  -d "{
    \"model\": \"${DEEPSEEK_MODEL}\",
    \"messages\": [
      {
        \"role\": \"user\",
        \"content\": \"1+1\"
      }
    ],
    \"stream\": false
  }" \
  "${DEEPSEEK_BASE_URL%/}/chat/completions"
