#!/bin/bash
set -euo pipefail

# Spring Boot: internal only. Railway public PORT is for Next.js.
export SERVER_PORT="${SERVER_PORT:-8080}"
export BACKEND_INTERNAL_URL="${BACKEND_INTERNAL_URL:-http://127.0.0.1:${SERVER_PORT}}"

urldecode() {
  local value="${1//+/ }"
  printf '%b' "${value//%/\\x}"
}

# Railway Postgres プラグインが postgres://user:pass@host:port/db を渡す。
# PostgreSQL JDBC ドライバは URL 内の資格情報を解釈しないため、分離して渡す。
RAW_DB_URL="${DATABASE_URL:-${DATABASE_PUBLIC_URL:-}}"
if [ -n "$RAW_DB_URL" ]; then
  case "$RAW_DB_URL" in
    jdbc:*)
      export SPRING_DATASOURCE_URL="$RAW_DB_URL"
      ;;
    postgres://*|postgresql://*)
      authority_and_path="${RAW_DB_URL#*://}"
      credentials=""
      host_and_path="$authority_and_path"
      case "$authority_and_path" in
        *@*)
          credentials="${authority_and_path%@*}"
          host_and_path="${authority_and_path##*@}"
          ;;
      esac
      export SPRING_DATASOURCE_URL="jdbc:postgresql://${host_and_path}"
      if [ -n "$credentials" ]; then
        SPRING_DATASOURCE_USERNAME="${SPRING_DATASOURCE_USERNAME:-$(urldecode "${credentials%%:*}")}"
        export SPRING_DATASOURCE_USERNAME
        case "$credentials" in
          *:*)
            SPRING_DATASOURCE_PASSWORD="${SPRING_DATASOURCE_PASSWORD:-$(urldecode "${credentials#*:}")}"
            export SPRING_DATASOURCE_PASSWORD
            ;;
        esac
      fi
      ;;
  esac
fi

# Prefer explicit Spring vars when provided
if [ -n "${DATABASE_USERNAME:-}" ] && [ -z "${SPRING_DATASOURCE_USERNAME:-}" ]; then
  export SPRING_DATASOURCE_USERNAME="$DATABASE_USERNAME"
fi
if [ -n "${DATABASE_PASSWORD:-}" ] && [ -z "${SPRING_DATASOURCE_PASSWORD:-}" ]; then
  export SPRING_DATASOURCE_PASSWORD="$DATABASE_PASSWORD"
fi

echo "[start] SERVER_PORT=${SERVER_PORT} PORT=${PORT:-3000}"
echo "[start] datasource url set=$( [ -n "${SPRING_DATASOURCE_URL:-}" ] && echo yes || echo no ) host=$( echo "${SPRING_DATASOURCE_URL:-}" | sed -e 's#^jdbc:postgresql://##' -e 's#[?].*##' )"
echo "[start] datasource user set=$( [ -n "${SPRING_DATASOURCE_USERNAME:-}" ] && echo yes || echo no ) password set=$( [ -n "${SPRING_DATASOURCE_PASSWORD:-}" ] && echo yes || echo no )"
if [ -z "${SPRING_DATASOURCE_URL:-}" ]; then
  echo "[start] WARNING: DATABASE_URL が未設定です。Railway に PostgreSQL を追加し変数を設定してください。"
fi

# Backend watchdog: DB 未設定や一時障害で死んでもコンテナ全体を落とさない
(
  while true; do
    echo "[backend] starting..."
    java -jar /app/backend/app.jar || true
    echo "[backend] exited; retry in 5s"
    sleep 5
  done
) &
BACKEND_WATCHER_PID=$!

# Wait for backend health (best-effort, max ~90s)
for i in $(seq 1 90); do
  if curl -fsS "http://127.0.0.1:${SERVER_PORT}/actuator/health" >/dev/null 2>&1; then
    echo "[start] backend healthy after ${i}s"
    break
  fi
  sleep 1
done

cd /app/frontend
# Railway edge は 0.0.0.0 待ち受け必須。HOSTNAME も Next 15 で参照される。
export HOSTNAME=0.0.0.0
PUBLIC_PORT="${PORT:-3000}"
echo "[frontend] starting next on 0.0.0.0:${PUBLIC_PORT}"
node_modules/.bin/next start -H 0.0.0.0 -p "${PUBLIC_PORT}" &
FRONTEND_PID=$!

cleanup() {
  kill "$FRONTEND_PID" "$BACKEND_WATCHER_PID" 2>/dev/null || true
}
trap cleanup TERM INT

# 公開面は frontend。frontend が落ちたらコンテナ終了。
wait "$FRONTEND_PID"
EXIT_CODE=$?
cleanup
exit "$EXIT_CODE"
