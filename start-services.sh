#!/bin/bash

echo "Starting ChitChat Backend Services..."

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Function to print colored output
print_status() {
    echo -e "${GREEN}[INFO]${NC} $1"
}

print_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

print_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

print_header() {
    echo -e "${BLUE}[HEADER]${NC} $1"
}

# Kill any existing processes on ports 9100-9108
print_header "Cleaning up existing processes..."
echo "Killing existing processes on ports 9100-9108..."
for port in 9100 9101 9102 9103 9104 9105 9106 9107 9108; do
    PID=$(lsof -ti:$port)
    if [ ! -z "$PID" ]; then
        echo "Killing process on port $port (PID: $PID)"
        kill -9 $PID
    fi
done

# Check if Docker is running
if ! docker info > /dev/null 2>&1; then
    print_error "Docker is not running. Please start Docker first."
    exit 1
fi

# Start infrastructure services
print_header "Starting infrastructure services..."
docker-compose up -d postgres mongodb redis kafka zookeeper

# Wait for infrastructure to be ready
print_status "Waiting for infrastructure to be ready..."
sleep 30

# Setup and verify databases
print_header "Setting up databases..."

# Check if database setup script exists
if [ -f "./setup-database.sh" ]; then
    print_status "Running database setup..."
    chmod +x ./setup-database.sh
    ./setup-database.sh
    
    if [ $? -eq 0 ]; then
        print_status "Database setup completed successfully!"
    else
        print_error "Database setup failed. Please check the logs."
        exit 1
    fi
else
    print_warning "Database setup script not found. Skipping database initialization."
fi

# Verify database setup
if [ -f "./verify-database.sh" ]; then
    print_status "Verifying database setup..."
    chmod +x ./verify-database.sh
    ./verify-database.sh
    
    if [ $? -eq 0 ]; then
        print_status "Database verification completed successfully!"
    else
        print_warning "Database verification failed. Services may not work properly."
    fi
else
    print_warning "Database verification script not found. Skipping verification."
fi

# Start Eureka Server (Port 9100)
print_header "Starting microservices..."
print_status "Starting Eureka Server on port 9100..."
cd chitchat-eureka-server
mvn spring-boot:run -Dspring-boot.run.arguments="--server.port=9100" &
EUREKA_PID=$!
cd ..

# Wait for Eureka to start
print_status "Waiting for Eureka Server to start..."
sleep 30

# Start API Gateway (Port 9101)
print_status "Starting API Gateway on port 9101..."
cd chitchat-api-gateway
mvn spring-boot:run -Dspring-boot.run.arguments="--server.port=9101" &
GATEWAY_PID=$!
cd ..

# Start User Service (Port 9102)
print_status "Starting User Service on port 9102..."
cd chitchat-user-service
mvn spring-boot:run -Dspring-boot.run.arguments="--server.port=9102" &
USER_PID=$!
cd ..

# Start Messaging Service (Port 9103)
print_status "Starting Messaging Service on port 9103..."
cd chitchat-messaging-service
mvn spring-boot:run -Dspring-boot.run.arguments="--server.port=9103" &
MESSAGING_PID=$!
cd ..

# Start Media Service (Port 9104)
print_status "Starting Media Service on port 9104..."
cd chitchat-media-service
mvn spring-boot:run -Dspring-boot.run.arguments="--server.port=9104" &
MEDIA_PID=$!
cd ..

# Start Calls Service (Port 9105)
print_status "Starting Calls Service on port 9105..."
cd chitchat-calls-service
mvn spring-boot:run -Dspring-boot.run.arguments="--server.port=9105" &
CALLS_PID=$!
cd ..

# Start Notification Service (Port 9106)
print_status "Starting Notification Service on port 9106..."
cd chitchat-notification-service
mvn spring-boot:run -Dspring-boot.run.arguments="--server.port=9106" &
NOTIFICATION_PID=$!
cd ..

# Start Status Service (Port 9107)
print_status "Starting Status Service on port 9107..."
cd chitchat-status-service
mvn spring-boot:run -Dspring-boot.run.arguments="--server.port=9107" &
STATUS_PID=$!
cd ..

# Start Admin Service (Port 9108)
print_status "Starting Admin Service on port 9108..."
cd chitchat-admin-service
mvn spring-boot:run -Dspring-boot.run.arguments="--server.port=9108" &
ADMIN_PID=$!
cd ..

print_header "All services started successfully!"
echo ""
echo "Service URLs:"
echo "============="
echo "Eureka Dashboard: http://localhost:9100"
echo "API Gateway: http://localhost:9101"
echo "User Service: http://localhost:9102"
echo "Messaging Service: http://localhost:9103"
echo "Media Service: http://localhost:9104"
echo "Calls Service: http://localhost:9105"
echo "Notification Service: http://localhost:9106"
echo "Status Service: http://localhost:9107"
echo "Admin Service: http://localhost:9108"
echo ""
print_status "To stop all services, run: ./stop-services.sh"
print_status "Process IDs saved for cleanup"
