# syntax=docker/dockerfile:1.7

FROM maven:3.9.9-eclipse-temurin-17 AS build
WORKDIR /workspace

COPY pom.xml ./
RUN mvn --batch-mode --no-transfer-progress -DskipTests dependency:go-offline

COPY src ./src
RUN mvn --batch-mode --no-transfer-progress -DskipTests package

FROM eclipse-temurin:17-jre-jammy
RUN apt-get update \
    && apt-get install --yes --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/* \
    && groupadd --system sentinel \
    && useradd --system --gid sentinel --home-dir /app sentinel

WORKDIR /app
COPY --from=build /workspace/target/sentinel-0.1.0.jar ./sentinel.jar
COPY --chown=sentinel:sentinel runbooks ./runbooks

USER sentinel
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/sentinel.jar"]
