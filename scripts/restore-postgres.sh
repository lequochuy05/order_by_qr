#!/usr/bin/env bash
set -euo pipefail

if [[ $# -lt 1 ]]; then
  echo "Usage: CONFIRM_RESTORE=YES $0 <backup.dump>" >&2
  exit 2
fi

if [[ "${CONFIRM_RESTORE:-}" != "YES" ]]; then
  echo "Refusing to restore without CONFIRM_RESTORE=YES" >&2
  exit 2
fi

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

BACKUP_FILE="$1"
PSQL_URL="${DB_URL#jdbc:}"
PSQL_URL="${PSQL_URL%%&channelBinding=*}"

PGPASSWORD="$DB_PASSWORD" pg_restore \
  "$BACKUP_FILE" \
  -U "$DB_USERNAME" \
  --dbname "$PSQL_URL" \
  --clean \
  --if-exists \
  --no-owner \
  --no-privileges

echo "Restore completed from $BACKUP_FILE"
