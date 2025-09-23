#!/bin/bash

echo "Stopping ChitChat Backend Services..."

# Kill processes on specific ports
echo "Killing processes on ports 8761, 9101-9108..."
for port in 8761 9101 9102 9103 9104 9105 9106 9107 9108; do
    PID=$(lsof -ti:$port)
    if [ ! -z "$PID" ]; then
        echo "Killing process on port $port (PID: $PID)"
        kill -9 $PID
    fi
done

# Stop all Java processes (Spring Boot applications)
echo "Stopping Spring Boot applications..."
pkill -f "spring-boot:run"

# Stop Docker containers
echo "Stopping Docker containers..."
docker-compose down

echo "All services stopped!"
