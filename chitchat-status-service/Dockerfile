FROM openjdk:21-jdk-slim

WORKDIR /app

COPY chitchat-status-service/target/chitchat-status-service-1.0.0.jar app.jar

EXPOSE 8086

ENTRYPOINT ["java", "-jar", "app.jar"]
