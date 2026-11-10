# syntax=docker/dockerfile:1
# Production image. Dockerfile.vercel is the scale-to-zero variant for Vercel.

ARG JAVA_VERSION=27

FROM maven:3.10.0-eclipse-temurin-${JAVA_VERSION} AS build
WORKDIR /workspace
COPY pom.xml .
RUN --mount=type=cache,target=/root/.m2 mvn -B -q -ntp dependency:go-offline
COPY src ./src
# Tests run in CI before the image is built; extract layers so dependencies cache separately from code
RUN --mount=type=cache,target=/root/.m2 mvn -B -q -ntp package -DskipTests \
 && java -Djarmode=tools -jar target/backend-*.jar extract --layers --launcher --destination target/extracted

FROM eclipse-temurin:${JAVA_VERSION}-jre-alpine
LABEL org.opencontainers.image.title="Opposite of Boring backend" \
      org.opencontainers.image.description="Spring Boot 4 API and SSE stream for Opposite of Boring" \
      org.opencontainers.image.source="https://github.com/soulwax/No-Boring-Backend"
# A fixed numeric UID, so orchestrators can verify runAsNonRoot without resolving names
RUN addgroup -S -g 10001 app && adduser -S -u 10001 -G app -H app
WORKDIR /app
# Least to most frequently changing, so a code change only rebuilds the last layer
COPY --from=build /workspace/target/extracted/dependencies/ ./
COPY --from=build /workspace/target/extracted/spring-boot-loader/ ./
COPY --from=build /workspace/target/extracted/snapshot-dependencies/ ./
COPY --from=build /workspace/target/extracted/application/ ./
USER 10001:10001
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75 -XX:+ExitOnOutOfMemoryError" \
    SERVER_PORT=8080
EXPOSE 8080
HEALTHCHECK --interval=10s --timeout=3s --start-period=60s --retries=3 \
  CMD ["wget", "-q", "-O", "/dev/null", "http://127.0.0.1:8080/actuator/health/liveness"]
ENTRYPOINT ["java", "org.springframework.boot.loader.launch.JarLauncher"]
