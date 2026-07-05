#!/usr/bin/env bash
set -euo pipefail

ENV_FILE="${ENV_FILE:-.env}"
if [[ -f "$ENV_FILE" ]]; then
  set -a
  # shellcheck disable=SC1090
  source "$ENV_FILE"
  set +a
fi

: "${DB_URL:?DB_URL is required}"
: "${DB_USERNAME:?DB_USERNAME is required}"
: "${DB_PASSWORD:?DB_PASSWORD is required}"

PSQL_URL="${DB_URL#jdbc:}"
PSQL_URL="${PSQL_URL%%&channelBinding=*}"
OUTPUT="${1:-backups/qros-$(date +%Y%m%d-%H%M%S).dump}"

mkdir -p "$(dirname "$OUTPUT")"

PGPASSWORD="$DB_PASSWORD" pg_dump \
  "$PSQL_URL" \
  -U "$DB_USERNAME" \
  --format=custom \
  --no-owner \
  --no-privileges \
  --file "$OUTPUT"

echo "Backup written to $OUTPUT"
