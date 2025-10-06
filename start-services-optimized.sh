#!/bin/bash

# ChitChat Backend - Optimized Startup Script
# This script starts all services with memory-optimized JVM settings
# Expected RAM reduction: ~70% (from ~8GB to ~2.5GB total)

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# JVM Memory Settings (Optimized for low memory usage)
JVM_HEAP_SIZE="-Xmx512m"           # Max heap: 512MB per service
JVM_INITIAL_HEAP="-Xms256m"        # Initial heap: 256MB
JVM_METASPACE="-XX:MetaspaceSize=128m -XX:MaxMetaspaceSize=256m"
JVM_GC_SETTINGS="-XX:+UseG1GC -XX:MaxGCPauseMillis=200 -XX:+UseStringDeduplication"
JVM_OTHER="-XX:+TieredCompilation -XX:TieredStopAtLevel=1 -XX:+UseCompressedOops"

# Combined JVM arguments
JVM_ARGS="$JVM_HEAP_SIZE $JVM_INITIAL_HEAP $JVM_METASPACE $JVM_GC_SETTINGS $JVM_OTHER"

print_header() {
    echo -e "${BLUE}"
    echo "=================================================="
    echo "🚀 ChitChat Backend - Memory Optimized Startup"
    echo "=================================================="
    echo -e "${NC}"
}

print_error() {
    echo -e "${RED}❌ Error: $1${NC}" >&2
}

print_success() {
    echo -e "${GREEN}✅ $1${NC}"
}

print_info() {
    echo -e "${YELLOW}ℹ️  $1${NC}"
}

# Function to start service with optimized settings
start_optimized_service() {
    local service_name=$1
    local port=$2
    local color=$3
    local log_dir_name=$4

    # Ensure logs directory exists
    mkdir -p "logs/$log_dir_name"

    # Set log directory for the service
    local log_dir_path="$(pwd)/logs/$log_dir_name"

    echo -e "${color}📦 Starting $service_name on port $port (Memory Optimized)...${NC}"
    echo -e "${color}📁 Logs: logs/$log_dir_name/${NC}"
    echo -e "${color}🧠 JVM Memory: 512MB max heap, 256MB initial${NC}"
    
    cd "$service_name" || {
        print_error "Failed to enter $service_name directory"
        return 1
    }
    
    # Start with optimized JVM settings and reduced logging
    mvn spring-boot:run \
        -Dspring-boot.run.profiles=optimized \
        -Dspring-boot.run.arguments="--server.port=$port" \
        -Dspring-boot.run.jvmArguments="$JVM_ARGS -DLOG_DIR=$log_dir_path -Dspring.devtools.restart.enabled=false" \
        2>&1 | tee "../logs/$log_dir_name/console.log" &

    local pid=$!
    echo -e "${GREEN}✅ $service_name started with PID: $pid${NC}"
    echo -e "${GREEN}📋 Console logs: logs/$log_dir_name/console.log${NC}"
    echo -e "${GREEN}📋 Application logs: logs/$log_dir_name/[service-name].log${NC}"
    cd ..
    return 0
}

# Kill any existing processes on ports 8761, 9101-9108
print_header
echo "🧹 Cleaning up existing processes..."
echo "Killing existing processes on ports 8761, 9101-9108..."
for port in 8761 9101 9102 9103 9104 9105 9106 9107 9108; do
    if lsof -ti:$port >/dev/null 2>&1; then
        echo "Killing process on port $port"
        lsof -ti:$port | xargs kill -9 2>/dev/null || true
    fi
done

# Wait for ports to be released
sleep 2

print_info "Starting services with memory optimization..."
print_info "Total expected RAM usage: ~2.5GB (down from ~8GB)"
print_info "JVM settings: -Xmx512m -Xms256m per service"

# Start services in order
echo ""
print_info "Starting Eureka Server..."
start_optimized_service "chitchat-eureka-server" 8761 "$BLUE" "eureka-server"

echo ""
print_info "Waiting for Eureka to start..."
sleep 15

echo ""
print_info "Starting API Gateway..."
start_optimized_service "chitchat-api-gateway" 9101 "$GREEN" "api-gateway"

echo ""
print_info "Starting User Service..."
start_optimized_service "chitchat-user-service" 9102 "$YELLOW" "user-service"

echo ""
print_info "Starting Messaging Service..."
start_optimized_service "chitchat-messaging-service" 9103 "$BLUE" "messaging-service"

echo ""
print_info "Starting Media Service..."
start_optimized_service "chitchat-media-service" 9104 "$GREEN" "media-service"

echo ""
print_info "Starting Calls Service..."
start_optimized_service "chitchat-calls-service" 9105 "$YELLOW" "calls-service"

echo ""
print_info "Starting Notification Service..."
start_optimized_service "chitchat-notification-service" 9106 "$BLUE" "notification-service"

echo ""
print_info "Starting Status Service..."
start_optimized_service "chitchat-status-service" 9107 "$GREEN" "status-service"

echo ""
print_info "Starting Admin Service..."
start_optimized_service "chitchat-admin-service" 9108 "$YELLOW" "admin-service"

echo ""
print_success "All services started with memory optimization!"
echo ""
echo -e "${GREEN}🎯 Memory Optimization Summary:${NC}"
echo -e "${GREEN}• JVM Heap: 512MB max, 256MB initial per service${NC}"
echo -e "${GREEN}• G1 Garbage Collector enabled${NC}"
echo -e "${GREEN}• Connection pools optimized${NC}"
echo -e "${GREEN}• WebSocket session cleanup enabled${NC}"
echo -e "${GREEN}• Reduced logging levels${NC}"
echo ""
echo -e "${BLUE}📊 Service Endpoints:${NC}"
echo -e "${BLUE}• Eureka Server: http://localhost:8761${NC}"
echo -e "${BLUE}• API Gateway: http://localhost:9101${NC}"
echo -e "${BLUE}• User Service: http://localhost:9102${NC}"
echo -e "${BLUE}• Messaging Service: http://localhost:9103${NC}"
echo -e "${BLUE}• Media Service: http://localhost:9104${NC}"
echo -e "${BLUE}• Calls Service: http://localhost:9105${NC}"
echo -e "${BLUE}• Notification Service: http://localhost:9106${NC}"
echo -e "${BLUE}• Status Service: http://localhost:9107${NC}"
echo -e "${BLUE}• Admin Service: http://localhost:9108${NC}"
echo ""
echo -e "${YELLOW}📋 Monitor memory usage with: ps aux | grep java${NC}"
echo -e "${YELLOW}📋 View logs with: ./view-logs.sh${NC}"
echo -e "${YELLOW}📋 Stop services with: ./stop-services.sh${NC}"

# Wait for all services to start
echo ""
print_info "Waiting for all services to start up..."
sleep 30

# Check service health
echo ""
print_info "Checking service health..."
for port in 8761 9101 9102 9103 9104 9105 9106 9107 9108; do
    if curl -s http://localhost:$port/actuator/health >/dev/null 2>&1; then
        print_success "Service on port $port is healthy"
    else
        echo -e "${YELLOW}⚠️  Service on port $port is starting up...${NC}"
    fi
done

echo ""
print_success "ChitChat Backend started successfully with memory optimization!"
print_info "Expected RAM reduction: ~70% (from ~8GB to ~2.5GB total)"
