#!/bin/bash
set -euo pipefail

# Spring Boot: internal only. Railway public PORT is for Next.js.
export SERVER_PORT="${SERVER_PORT:-8080}"
export BACKEND_INTERNAL_URL="${BACKEND_INTERNAL_URL:-http://127.0.0.1:${SERVER_PORT}}"

# Railway Postgres プラグインが postgres:// を渡す場合に JDBC へ変換
if [ -n "${DATABASE_URL:-}" ]; then
  case "$DATABASE_URL" in
    jdbc:*)
      export SPRING_DATASOURCE_URL="$DATABASE_URL"
      ;;
    postgres://*|postgresql://*)
      export SPRING_DATASOURCE_URL="jdbc:${DATABASE_URL/postgres:/postgresql:}"
      # jdbc:postgresql://user:pass@host:port/db — Spring は url 内の資格情報も解釈可能
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
echo "[start] datasource url set=$( [ -n "${SPRING_DATASOURCE_URL:-}${DATABASE_URL:-}" ] && echo yes || echo no )"

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
