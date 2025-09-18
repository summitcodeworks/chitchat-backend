FROM openjdk:21-jdk-slim

WORKDIR /app

COPY chitchat-admin-service/target/chitchat-admin-service-1.0.0.jar app.jar

EXPOSE 8087

ENTRYPOINT ["java", "-jar", "app.jar"]
