FROM ubuntu:latest

RUN apt-get update && \
    apt-get install -y curl unzip git && \
    apt-get clean
# JDK 22 From https://adoptium.net/download/, the latest one wouldn't download
RUN curl -o openjdk-22.0.9.tar.gz  'https://objects.githubusercontent.com/github-production-release-asset-2e65be/677419466/ab058890-aef4-43de-a0c9-98893d0a3397?X-Amz-Algorithm=AWS4-HMAC-SHA256&X-Amz-Credential=releaseassetproduction%2F20250224%2Fus-east-1%2Fs3%2Faws4_request&X-Amz-Date=20250224T023514Z&X-Amz-Expires=300&X-Amz-Signature=a2689fb26cfb951dbf114b58900593cb569acf2b10868f0449a96e0e5a89fa18&X-Amz-SignedHeaders=host&response-content-disposition=attachment%3B%20filename%3DOpenJDK22U-jdk_x64_linux_hotspot_22.0.2_9.tar.gz&response-content-type=application%2Foctet-stream' && \
    tar -xvzf openjdk-22.0.9.tar.gz && \
    mv jdk-22.0.2+9 /usr/local/jdk-22 && \
    ln -s /usr/local/jdk-22/bin/java /usr/bin/java

RUN curl -o /tmp/gradle-8.9-bin.zip -L https://services.gradle.org/distributions/gradle-8.9-bin.zip 
RUN unzip /tmp/gradle-8.9-bin.zip -d /opt
RUN ln -s /opt/gradle-8.9/bin/gradle /usr/bin/gradle

ENV JAVA_HOME /usr/local/jdk-22

WORKDIR /workspace

COPY . .

RUN gradle buildFatJar
