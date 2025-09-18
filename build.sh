#!/bin/bash

echo "Building ChitChat Backend Services..."

# Build shared config first
echo "Building shared config..."
cd chitchat-shared-config
mvn clean install -DskipTests
cd ..

# Build all services
echo "Building all services..."
mvn clean install -DskipTests

echo "Build completed!"
echo ""
echo "To start the services:"
echo "1. Start infrastructure: docker-compose up -d postgres mongodb redis kafka zookeeper"
echo "2. Start Eureka Server: cd chitchat-eureka-server && mvn spring-boot:run"
echo "3. Start other services in separate terminals"
echo ""
echo "Or use Docker Compose: docker-compose up -d"
