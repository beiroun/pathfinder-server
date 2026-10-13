# syntax=docker/dockerfile:1
# SPDX-License-Identifier: BUSL-1.1
# Copyright (c) 2026 Andrei Baranov (84softworks). Licensed under the Business Source License 1.1 - see LICENSE.
#
# Pathfinder Server image: the Gradle wrapper builds the boot jar on a JDK, the runtime stage carries only the JRE
# and the jar. Published as ghcr.io/beiroun/pathfinder-server:<version> by CI on a tagged release.

ARG APP_VERSION=0.0.0-dev

FROM eclipse-temurin:25-jdk AS build
ARG APP_VERSION
WORKDIR /workspace

# Dependency resolution is cached separately from the sources: a code change does not re-download the world
COPY gradlew settings.gradle.kts build.gradle.kts gradle.properties ./
COPY gradle ./gradle
RUN ./gradlew --no-daemon dependencies --quiet > /dev/null

COPY src ./src
RUN ./gradlew --no-daemon -PappVersion="${APP_VERSION}" bootJar

FROM eclipse-temurin:25-jre
ARG APP_VERSION
LABEL org.opencontainers.image.title="Pathfinder Server" \
      org.opencontainers.image.description="Device-aware QR redirect service and management API" \
      org.opencontainers.image.source="https://github.com/beiroun/pathfinder-server" \
      org.opencontainers.image.licenses="BUSL-1.1" \
      org.opencontainers.image.version="${APP_VERSION}"

RUN groupadd --system pathfinder && useradd --system --gid pathfinder --no-create-home --shell /usr/sbin/nologin pathfinder
COPY --from=build /workspace/build/libs/pathfinder-server.jar /app/pathfinder-server.jar

USER pathfinder
# Host-networked in the compose deployment; the port is informational
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/pathfinder-server.jar"]
