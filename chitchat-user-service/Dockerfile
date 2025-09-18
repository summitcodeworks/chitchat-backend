FROM openjdk:21-jdk-slim

WORKDIR /app

COPY chitchat-user-service/target/chitchat-user-service-1.0.0.jar app.jar

EXPOSE 8081

ENTRYPOINT ["java", "-jar", "app.jar"]
