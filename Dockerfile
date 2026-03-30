FROM ubuntu:24.04

RUN apt-get update && \
    apt-get install -y curl unzip git && \
    apt-get clean && \
    rm -rf /var/lib/apt/lists/*

# JDK 22 from Adoptium
RUN curl -L -o /tmp/openjdk-22.tar.gz https://api.adoptium.net/v3/binary/latest/22/ga/linux/x64/jdk/hotspot/normal/eclipse && \
    mkdir -p /usr/local/jdk-22 && \
    tar -xvzf /tmp/openjdk-22.tar.gz --strip-components=1 -C /usr/local/jdk-22 && \
    ln -s /usr/local/jdk-22/bin/java /usr/bin/java && \
    rm /tmp/openjdk-22.tar.gz

# Gradle 8.9
RUN curl -L -o /tmp/gradle-8.9-bin.zip https://services.gradle.org/distributions/gradle-8.9-bin.zip && \
    unzip /tmp/gradle-8.9-bin.zip -d /opt && \
    ln -s /opt/gradle-8.9/bin/gradle /usr/bin/gradle && \
    rm /tmp/gradle-8.9-bin.zip

ENV JAVA_HOME=/usr/local/jdk-22

WORKDIR /workspace

COPY . .

RUN gradle shadowJar
