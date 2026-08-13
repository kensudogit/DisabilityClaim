#!/bin/bash
set -e

# Spring Boot listens on fixed internal port; Railway public PORT is for Next.js.
export SERVER_PORT="${SERVER_PORT:-8080}"
export BACKEND_INTERNAL_URL="${BACKEND_INTERNAL_URL:-http://localhost:${SERVER_PORT}}"

java -jar /app/backend/app.jar &
BACKEND_PID=$!

# Wait briefly for backend readiness (best-effort).
for i in $(seq 1 60); do
  if curl -fsS "http://127.0.0.1:${SERVER_PORT}/actuator/health" >/dev/null 2>&1; then
    break
  fi
  sleep 1
done

cd /app/frontend
PORT="${PORT:-3000}" node_modules/.bin/next start -p "${PORT:-3000}" &
FRONTEND_PID=$!

trap 'kill $BACKEND_PID $FRONTEND_PID 2>/dev/null || true' TERM INT

wait -n "$BACKEND_PID" "$FRONTEND_PID"
EXIT_CODE=$?
kill $BACKEND_PID $FRONTEND_PID 2>/dev/null || true
exit $EXIT_CODE
