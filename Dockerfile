# 単一 Railway サービスで backend (Spring Boot) と frontend (Next.js) を
# 同一コンテナ内の別プロセスとして起動する統合 Dockerfile。
# Next.js は Railway の PORT で公開し、Spring Boot は内部 18080 で待ち受ける。
# テストレポートはビルド時に生成し public/qa-reports として同梱する（/test-reports 画面で表示）。

FROM eclipse-temurin:21-jdk-alpine AS backend-build
WORKDIR /workspace/backend
COPY backend/gradlew backend/gradlew.bat ./
COPY backend/gradle ./gradle
COPY backend/build.gradle backend/settings.gradle backend/gradle.properties* ./
COPY backend/src ./src
RUN chmod +x ./gradlew \
  && ./gradlew bootJar test jacocoTestReport --no-daemon \
  && JAR=$(ls build/libs/*.jar | grep -v plain | head -n 1) \
  && cp "$JAR" build/libs/app.jar \
  && mkdir -p /workspace/qa-reports/backend \
  && cp -R build/reports/tests/test /workspace/qa-reports/backend/surefire \
  && cp -R build/reports/jacoco/test/html /workspace/qa-reports/backend/jacoco \
  && mkdir -p /workspace/qa-reports/backend/test-results \
  && cp -R build/test-results/test /workspace/qa-reports/backend/test-results/test

FROM node:22-alpine AS frontend-build
WORKDIR /workspace
COPY frontend/package.json frontend/package-lock.json ./frontend/
RUN cd frontend && npm ci
COPY frontend/ ./frontend/
COPY scripts/generate-report-manifest.js ./scripts/generate-report-manifest.js
COPY --from=backend-build /workspace/qa-reports/backend ./frontend/public/qa-reports/backend
# manifest 生成用に Surefire XML を backend/build 相当へ配置
RUN mkdir -p backend/build/test-results \
  && cp -R frontend/public/qa-reports/backend/test-results/test backend/build/test-results/test
WORKDIR /workspace/frontend
ENV NEXT_TELEMETRY_DISABLED=1
ENV BACKEND_INTERNAL_URL=http://127.0.0.1:18080
RUN mkdir -p public/qa-reports/frontend/vitest \
  && npm run test:coverage \
  && node ../scripts/generate-report-manifest.js \
  && rm -rf public/qa-reports/backend/test-results \
  && npm run build

FROM node:22-alpine AS runtime
RUN apk add --no-cache bash curl \
  && (apk add --no-cache openjdk21-jre || apk add --no-cache openjdk21-jre-headless)
WORKDIR /app

COPY --from=backend-build /workspace/backend/build/libs/app.jar backend/app.jar

COPY --from=frontend-build /workspace/frontend/public frontend/public
COPY --from=frontend-build /workspace/frontend/.next frontend/.next
COPY --from=frontend-build /workspace/frontend/node_modules frontend/node_modules
COPY --from=frontend-build /workspace/frontend/package.json frontend/package.json
COPY --from=frontend-build /workspace/frontend/next.config.ts frontend/next.config.ts

COPY start.sh /app/start.sh
RUN chmod +x /app/start.sh

ENV SERVER_PORT=18080
ENV INTERNAL_BACKEND_PORT=18080
ENV BACKEND_INTERNAL_URL=http://127.0.0.1:18080
ENV NEXT_TELEMETRY_DISABLED=1

EXPOSE 3000
CMD ["/app/start.sh"]
