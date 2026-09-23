# syntax=docker/dockerfile:1

FROM eclipse-temurin:21-jdk-jammy AS builder

WORKDIR /workspace

COPY gradlew build.gradle settings.gradle ./
COPY gradle ./gradle

COPY src ./src

RUN --mount=type=cache,target=/root/.gradle \
    chmod +x gradlew \
    && ./gradlew clean bootJar --no-daemon

FROM eclipse-temurin:21-jre-jammy

RUN groupadd --system spring \
    && useradd --system --gid spring spring

WORKDIR /app

COPY --from=builder --chown=spring:spring /workspace/build/libs/*.jar app.jar

COPY --chown=spring:spring deploy/start.sh /app/start.sh

USER spring:spring

EXPOSE 8080

ENTRYPOINT ["sh", "/app/start.sh"]
