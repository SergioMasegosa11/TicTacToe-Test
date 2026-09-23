# DevContainer fuer TicTacTest
# Die Version dieses Images steht in .devcontainer/VERSION (siehe docs/devcontainer.md).
# Base-Image mit fester Version, damit Builds reproduzierbar sind.
FROM alpine:3.24

# Verknuepft das Image in der GitHub Container Registry mit dem Repository
LABEL org.opencontainers.image.source="https://github.com/SergioMasegosa11/TicTacToe-Test"
LABEL org.opencontainers.image.description="DevContainer fuer TicTacTest (Java 25, Gradle, JUnit)"

# Java 25, Gradle und benötigte Tools installieren
# - JUnit/AssertJ kommen als Gradle-Abhängigkeiten (build.gradle) dazu
# - libstdc++/procps: nötig für den VS Code Server im Container
# - rsync: nötig für die GitHub-Pages-Deploy-Action in der Pipeline
RUN apk add --no-cache \
    openjdk25 \
    gradle \
    git \
    bash \
    libstdc++ \
    procps \
    rsync

# JAVA_HOME setzen
ENV JAVA_HOME=/usr/lib/jvm/java-25-openjdk
ENV PATH="$JAVA_HOME/bin:$PATH"

# Benutzer mit UID:GID 1000:1000 erstellen
# (lokal wird er über remoteUser in devcontainer.json verwendet, die CI läuft als root)
RUN addgroup -g 1000 devuser && \
    adduser -D -u 1000 -G devuser devuser

# Arbeitsverzeichnis
WORKDIR /workspace

# Besitzer des Arbeitsverzeichnisses setzen
RUN chown -R 1000:1000 /workspace
