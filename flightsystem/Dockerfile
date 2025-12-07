FROM maven:3.8.5-openjdk-17
WORKDIR /project
COPY . /project
RUN mvn clean package -DskipTests
ENTRYPOINT ["java", "-jar", "target/flightsystem-0.0.1-SNAPSHOT.jar"]
