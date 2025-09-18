FROM openjdk:21-jdk-slim

WORKDIR /app

COPY chitchat-notification-service/target/chitchat-notification-service-1.0.0.jar app.jar

EXPOSE 8085

ENTRYPOINT ["java", "-jar", "app.jar"]
