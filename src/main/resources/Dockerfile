# syntax=docker/dockerfile:1

ARG VUEDISK_PLATFORM=linux/arm64
FROM --platform=$VUEDISK_PLATFORM eclipse-temurin:25-jre

ARG VUEDISK_URL=https://github.com/ashishdoneriya/vuedisk-java/releases/latest/download/vuedisk.jar

WORKDIR /app

ADD ${VUEDISK_URL} /app/vuedisk.jar

EXPOSE 8989

ENTRYPOINT ["java", "-Djava.awt.headless=true", "-jar", "/app/vuedisk.jar"]
CMD ["-p", "8989"]
