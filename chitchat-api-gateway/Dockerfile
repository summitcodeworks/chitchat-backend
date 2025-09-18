FROM openjdk:21-jdk-slim

WORKDIR /app

COPY chitchat-api-gateway/target/chitchat-api-gateway-1.0.0.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
