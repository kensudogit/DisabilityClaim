# 単一 Railway サービスで backend (Spring Boot) と frontend (Next.js) を
# 同一コンテナ内の別プロセスとして起動する統合 Dockerfile。
# Next.js の rewrites() が /api・/actuator を localhost:8080 (backend) へプロキシする。

FROM eclipse-temurin:21-jdk-alpine AS backend-build
WORKDIR /workspace/backend
COPY backend/gradlew backend/gradlew.bat ./
COPY backend/gradle ./gradle
COPY backend/build.gradle backend/settings.gradle backend/gradle.properties* ./
COPY backend/src ./src
RUN chmod +x ./gradlew \
  && ./gradlew bootJar -x test --no-daemon \
  && JAR=$(ls build/libs/*.jar | grep -v plain | head -n 1) \
  && cp "$JAR" build/libs/app.jar

FROM node:22-alpine AS frontend-build
WORKDIR /workspace/frontend
COPY frontend/package.json frontend/package-lock.json ./
RUN npm ci
COPY frontend/ ./
ENV NEXT_TELEMETRY_DISABLED=1
ENV BACKEND_INTERNAL_URL=http://localhost:8080
RUN npm run build

FROM node:22-alpine AS runtime
RUN apk add --no-cache openjdk21-jre-headless bash curl
WORKDIR /app

COPY --from=backend-build /workspace/backend/build/libs/app.jar backend/app.jar

COPY --from=frontend-build /workspace/frontend/public frontend/public
COPY --from=frontend-build /workspace/frontend/.next frontend/.next
COPY --from=frontend-build /workspace/frontend/node_modules frontend/node_modules
COPY --from=frontend-build /workspace/frontend/package.json frontend/package.json
COPY --from=frontend-build /workspace/frontend/next.config.ts frontend/next.config.ts

COPY start.sh /app/start.sh
RUN chmod +x /app/start.sh

ENV SERVER_PORT=8080
ENV BACKEND_INTERNAL_URL=http://localhost:8080
ENV NEXT_TELEMETRY_DISABLED=1

EXPOSE 3000
CMD ["/app/start.sh"]
