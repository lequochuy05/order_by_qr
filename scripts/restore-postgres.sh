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

load_env_file() {
  local file="$1"
  local line key value
  while IFS= read -r line || [[ -n "$line" ]]; do
    line="${line%$'\r'}"
    [[ "$line" =~ ^[[:space:]]*(#|$) ]] && continue
    line="${line#export }"
    [[ "$line" =~ ^[[:space:]]*([A-Za-z_][A-Za-z0-9_]*)[[:space:]]*=(.*)$ ]] || continue
    key="${BASH_REMATCH[1]}"
    value="${BASH_REMATCH[2]}"
    value="${value#"${value%%[![:space:]]*}"}"
    value="${value%"${value##*[![:space:]]}"}"
    if [[ "$value" =~ ^\"(.*)\"$ ]]; then
      value="${BASH_REMATCH[1]}"
    elif [[ "$value" =~ ^\'(.*)\'$ ]]; then
      value="${BASH_REMATCH[1]}"
    fi
    if [[ -z "${!key+x}" ]]; then
      printf -v "$key" '%s' "$value"
      export "$key"
    fi
  done < "$file"
}

sanitize_jdbc_url() {
  local raw="${1#jdbc:}"
  if command -v python3 >/dev/null 2>&1; then
    python3 - "$raw" <<'PY'
import sys
from urllib.parse import parse_qsl, urlencode, urlsplit, urlunsplit

jdbc_only = {
    "channelbinding",
    "stringtype",
    "applicationname",
    "preparethreshold",
    "rewritebatchedinserts",
    "currentschema",
}
parts = urlsplit(sys.argv[1])
kept = [
    (k, v)
    for k, v in parse_qsl(parts.query, keep_blank_values=True)
    if k.lower() not in jdbc_only
]
print(urlunsplit((parts.scheme, parts.netloc, parts.path, urlencode(kept), parts.fragment)))
PY
  else
    local base="${raw%%\?*}"
    if [[ "$raw" == *"?"* ]]; then
      local query="${raw#*\?}"
      local out="" pair key
      IFS='&' read -ra pairs <<< "$query"
      for pair in "${pairs[@]}"; do
        key="${pair%%=*}"
        case "${key,,}" in
          channelbinding|stringtype|applicationname|preparethreshold|rewritebatchedinserts|currentschema|"") continue ;;
          *) out="${out:+${out}&}${pair}" ;;
        esac
      done
      echo "${base}${out:+?${out}}"
    else
      echo "$base"
    fi
  fi
}

ENV_FILE="${ENV_FILE:-.env}"
if [[ -f "$ENV_FILE" ]]; then
  load_env_file "$ENV_FILE"
fi

: "${DB_URL:?DB_URL is required}"
: "${DB_USERNAME:?DB_USERNAME is required}"
: "${DB_PASSWORD:?DB_PASSWORD is required}"

BACKUP_FILE="$1"
PSQL_URL="$(sanitize_jdbc_url "$DB_URL")"

PGPASSWORD="$DB_PASSWORD" pg_restore \
  "$BACKUP_FILE" \
  -U "$DB_USERNAME" \
  --dbname "$PSQL_URL" \
  --clean \
  --if-exists \
  --no-owner \
  --no-privileges

echo "Restore completed from $BACKUP_FILE"
