FROM openjdk:21-jdk-slim

WORKDIR /app

COPY chitchat-calls-service/target/chitchat-calls-service-1.0.0.jar app.jar

EXPOSE 8084

ENTRYPOINT ["java", "-jar", "app.jar"]
