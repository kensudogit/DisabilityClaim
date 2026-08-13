# Multi-stage build. Prefer generating wrapper first:
#   gradle wrapper --gradle-version 8.10.2
FROM eclipse-temurin:21-jdk AS build
WORKDIR /app
COPY . .
RUN if [ -f ./gradlew ]; then chmod +x ./gradlew && ./gradlew bootJar -x test; \
    elif command -v gradle >/dev/null 2>&1; then gradle bootJar -x test; \
    else echo "Install Gradle or provide gradlew" && exit 1; fi

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/build/libs/*-SNAPSHOT.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
