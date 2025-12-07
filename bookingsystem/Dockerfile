FROM maven:3.8.5-openjdk-17 AS builder

WORKDIR /project

COPY .. /project

RUN mvn -pl bookingsystem -am clean package -DskipTests


FROM openjdk:17-jdk-slim

WORKDIR /app

COPY target/*.jar app.jar

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
