FROM eclipse-temurin:25-jdk-alpine

# Java 25, Gradle und benötigte Tools installieren
RUN apk add --no-cache \
    openjdk25 \
    gradle \
    git \
    bash

# JAVA_HOME setzen
ENV JAVA_HOME=/usr/lib/jvm/java-25-openjdk
ENV PATH="$JAVA_HOME/bin:$PATH"

# Benutzer mit UID:GID 1000:1000 erstellen
RUN addgroup -g 1000 devuser && \
    adduser -D -u 1000 -G devuser devuser

# Arbeitsverzeichnis
WORKDIR /workspace

# Besitzer des Arbeitsverzeichnisses setzen
RUN chown -R 1000:1000 /workspace