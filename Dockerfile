FROM ubuntu:latest

RUN apt-get update && \
    apt-get install -y wget unzip git && \
    apt-get clean

RUN wget https://download.java.net/java/early_access/jdk22/7/GPL/openjdk-22-ea+7_linux-x64_bin.tar.gz && \
    tar -xzf openjdk-22-ea+7_linux-x64_bin.tar.gz && \
    mv jdk-22 /usr/local/ && \
    ln -s /usr/local/jdk-22/bin/java /usr/bin/java

RUN wget https://services.gradle.org/distributions/gradle-8.9-bin.zip -P /tmp && \
    unzip /tmp/gradle-8.9-bin.zip -d /opt && \
    ln -s /opt/gradle-8.9/bin/gradle /usr/bin/gradle

ENV JAVA_HOME /usr/local/jdk-22

WORKDIR /workspace

COPY . .

RUN gradle build
