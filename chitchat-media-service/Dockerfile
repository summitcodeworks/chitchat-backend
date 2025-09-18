FROM openjdk:21-jdk-slim

WORKDIR /app

COPY chitchat-media-service/target/chitchat-media-service-1.0.0.jar app.jar

EXPOSE 8083

ENTRYPOINT ["java", "-jar", "app.jar"]
