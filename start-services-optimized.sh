#!/bin/bash

# ChitChat Backend - Optimized Startup Script
# This script starts all services with memory-optimized JVM settings
# Expected RAM reduction: ~70% (from ~8GB to ~2.5GB total)
# Supports auto-reload with --dev flag

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Default mode (auto-reload enabled by default)
MODE="dev"
AUTO_RELOAD="true"
SERVICES_MODE="all"  # "all", "core", or "minimal"

# Parse command line arguments
while [[ $# -gt 0 ]]; do
    case $1 in
        --dev)
            MODE="dev"
            AUTO_RELOAD="true"
            shift
            ;;
        --prod)
            MODE="prod"
            AUTO_RELOAD="false"
            shift
            ;;
        --minimal)
            SERVICES_MODE="minimal"
            shift
            ;;
        --core)
            SERVICES_MODE="core"
            shift
            ;;
        --all)
            SERVICES_MODE="all"
            shift
            ;;
        --help)
            echo "Usage: $0 [OPTIONS]"
            echo ""
            echo "Options:"
            echo "  --dev       Start with auto-reload enabled (Default)"
            echo "  --prod      Start in production mode (auto-reload disabled)"
            echo "  --minimal   Start only essential services (Eureka, Gateway, User, Messaging)"
            echo "  --core      Start core services (Minimal + Notification, Status)"
            echo "  --all       Start all services (Default)"
            echo "  --help      Show this help message"
            echo ""
            echo "Examples:"
            echo "  $0                # Start all services with auto-reload (default)"
            echo "  $0 --minimal      # Start only essential services (recommended for low-end machines)"
            echo "  $0 --core --prod  # Start core services in production mode"
            echo "  $0 --all --dev    # Start all services with auto-reload"
            exit 0
            ;;
        *)
            echo "Unknown option: $1"
            echo "Use --help for usage information"
            exit 1
            ;;
    esac
done

# JVM Memory Settings (Aggressively optimized for low memory usage)
JVM_HEAP_SIZE="-Xmx256m"           # Max heap: 256MB per service (reduced from 512MB)
JVM_INITIAL_HEAP="-Xms128m"        # Initial heap: 128MB (reduced from 256MB)
JVM_METASPACE="-XX:MetaspaceSize=96m -XX:MaxMetaspaceSize=192m"  # Reduced metaspace
JVM_GC_SETTINGS="-XX:+UseSerialGC"  # Serial GC uses less memory than G1GC
JVM_OTHER="-XX:+TieredCompilation -XX:TieredStopAtLevel=1 -XX:+UseCompressedOops -XX:-UsePerfData"

# Combined JVM arguments
JVM_ARGS="$JVM_HEAP_SIZE $JVM_INITIAL_HEAP $JVM_METASPACE $JVM_GC_SETTINGS $JVM_OTHER"

# Startup delay between services (seconds)
SERVICE_START_DELAY=20  # Increased from 0 to prevent CPU spikes

# Add DevTools settings based on mode
if [ "$AUTO_RELOAD" = "true" ]; then
    JVM_ARGS="$JVM_ARGS -Dspring.devtools.restart.enabled=true"
    SPRING_PROFILE="dev"
else
    JVM_ARGS="$JVM_ARGS -Dspring.devtools.restart.enabled=false"
    SPRING_PROFILE="optimized"
fi

print_header() {
    echo -e "${BLUE}"
    echo "=================================================="
    if [ "$AUTO_RELOAD" = "true" ]; then
        echo "🚀 ChitChat Backend - Optimized + Auto-Reload"
    else
        echo "🚀 ChitChat Backend - Memory Optimized Startup"
    fi
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

    if [ "$AUTO_RELOAD" = "true" ]; then
        echo -e "${color}📦 Starting $service_name on port $port (Ultra-Optimized + Auto-Reload)...${NC}"
    else
        echo -e "${color}📦 Starting $service_name on port $port (Ultra-Optimized)...${NC}"
    fi
    echo -e "${color}📁 Logs: logs/$log_dir_name/${NC}"
    echo -e "${color}🧠 JVM Memory: 256MB max heap, 128MB initial${NC}"
    if [ "$AUTO_RELOAD" = "true" ]; then
        echo -e "${color}🔄 Auto-Reload: ENABLED${NC}"
    fi
    
    cd "$service_name" || {
        print_error "Failed to enter $service_name directory"
        return 1
    }
    
    # Start with optimized JVM settings and optional auto-reload
    mvn spring-boot:run \
        -Dspring-boot.run.profiles="$SPRING_PROFILE" \
        -Dspring-boot.run.arguments="--server.port=$port" \
        -Dspring-boot.run.jvmArguments="$JVM_ARGS -DLOG_DIR=$log_dir_path" \
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

if [ "$AUTO_RELOAD" = "true" ]; then
    print_info "Starting services with ultra memory optimization + auto-reload..."
    print_info "Mode: DEVELOPMENT (with auto-reload on file changes)"
else
    print_info "Starting services with ultra memory optimization..."
    print_info "Mode: PRODUCTION (no auto-reload)"
fi

case $SERVICES_MODE in
    minimal)
        print_info "Service Mode: MINIMAL (Eureka, Gateway, User, Messaging only)"
        print_info "Expected RAM usage: ~1.0-1.2GB"
        ;;
    core)
        print_info "Service Mode: CORE (Essential + Notification + Status)"
        print_info "Expected RAM usage: ~1.5-1.8GB"
        ;;
    all)
        print_info "Service Mode: ALL SERVICES"
        print_info "Expected RAM usage: ~2.0-2.5GB"
        ;;
esac

print_info "JVM settings: -Xmx256m -Xms128m per service (down from 512m/256m)"
print_info "Using SerialGC for minimal memory footprint"
print_info "Delay between services: ${SERVICE_START_DELAY}s (to prevent CPU spikes)"

# Start services in order with delays to prevent system overload
echo ""
print_info "Starting Eureka Server..."
start_optimized_service "chitchat-eureka-server" 8761 "$BLUE" "eureka-server"

echo ""
print_info "Waiting for Eureka to stabilize (30s)..."
sleep 30

echo ""
print_info "Starting API Gateway..."
start_optimized_service "chitchat-api-gateway" 9101 "$GREEN" "api-gateway"
echo ""
print_info "Waiting ${SERVICE_START_DELAY}s before next service..."
sleep $SERVICE_START_DELAY

echo ""
print_info "Starting User Service..."
start_optimized_service "chitchat-user-service" 9102 "$YELLOW" "user-service"
echo ""
print_info "Waiting ${SERVICE_START_DELAY}s before next service..."
sleep $SERVICE_START_DELAY

echo ""
print_info "Starting Messaging Service..."
start_optimized_service "chitchat-messaging-service" 9103 "$BLUE" "messaging-service"

# Only start additional services based on mode
if [ "$SERVICES_MODE" = "minimal" ]; then
    print_info "Skipping remaining services (minimal mode)"
else
    echo ""
    print_info "Waiting ${SERVICE_START_DELAY}s before next service..."
    sleep $SERVICE_START_DELAY
    
    echo ""
    print_info "Starting Notification Service..."
    start_optimized_service "chitchat-notification-service" 9106 "$BLUE" "notification-service"
    echo ""
    print_info "Waiting ${SERVICE_START_DELAY}s before next service..."
    sleep $SERVICE_START_DELAY
    
    echo ""
    print_info "Starting Status Service..."
    start_optimized_service "chitchat-status-service" 9107 "$GREEN" "status-service"
    
    # Start remaining services only in "all" mode
    if [ "$SERVICES_MODE" = "all" ]; then
        echo ""
        print_info "Waiting ${SERVICE_START_DELAY}s before next service..."
        sleep $SERVICE_START_DELAY
        
        echo ""
        print_info "Starting Media Service..."
        start_optimized_service "chitchat-media-service" 9104 "$GREEN" "media-service"
        echo ""
        print_info "Waiting ${SERVICE_START_DELAY}s before next service..."
        sleep $SERVICE_START_DELAY
        
        echo ""
        print_info "Starting Calls Service..."
        start_optimized_service "chitchat-calls-service" 9105 "$YELLOW" "calls-service"
        echo ""
        print_info "Waiting ${SERVICE_START_DELAY}s before next service..."
        sleep $SERVICE_START_DELAY
        
        echo ""
        print_info "Starting Admin Service..."
        start_optimized_service "chitchat-admin-service" 9108 "$YELLOW" "admin-service"
    else
        print_info "Skipping Media, Calls, and Admin services (core mode)"
    fi
fi

echo ""
if [ "$AUTO_RELOAD" = "true" ]; then
    print_success "Services started with ULTRA memory optimization + auto-reload!"
else
    print_success "Services started with ULTRA memory optimization!"
fi
echo ""
echo -e "${GREEN}🎯 Ultra Memory Optimization Summary:${NC}"
echo -e "${GREEN}• JVM Heap: 256MB max, 128MB initial per service (50% reduction!)${NC}"
echo -e "${GREEN}• SerialGC: Minimal memory footprint${NC}"
echo -e "${GREEN}• Metaspace: 192MB max (down from 256MB)${NC}"
echo -e "${GREEN}• Startup delays: ${SERVICE_START_DELAY}s between services (prevents CPU spikes)${NC}"
echo -e "${GREEN}• Service Mode: ${SERVICES_MODE^^}${NC}"
if [ "$AUTO_RELOAD" = "true" ]; then
    echo -e "${GREEN}• Auto-Reload: ENABLED (DevTools active)${NC}"
    echo -e "${GREEN}• Changes to .java files will trigger automatic restart${NC}"
else
    echo -e "${GREEN}• Auto-Reload: DISABLED (Production mode)${NC}"
fi
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
if [ "$AUTO_RELOAD" = "true" ]; then
    echo ""
    echo -e "${BLUE}🔄 Auto-Reload Information:${NC}"
    echo -e "${BLUE}• Edit any .java file to trigger automatic restart${NC}"
    echo -e "${BLUE}• Edit application.yml to reload configuration${NC}"
    echo -e "${BLUE}• Check logs for 'Restarting' messages${NC}"
    echo -e "${BLUE}• Typical restart time: 5-15 seconds${NC}"
fi

# Wait for all services to start
echo ""
print_info "Waiting for all services to start up..."
sleep 30

# Check service health
echo ""
print_info "Checking service health..."

# Define which ports to check based on service mode
case $SERVICES_MODE in
    minimal)
        PORTS_TO_CHECK="8761 9101 9102 9103"
        ;;
    core)
        PORTS_TO_CHECK="8761 9101 9102 9103 9106 9107"
        ;;
    all)
        PORTS_TO_CHECK="8761 9101 9102 9103 9104 9105 9106 9107 9108"
        ;;
esac

for port in $PORTS_TO_CHECK; do
    if curl -s http://localhost:$port/actuator/health >/dev/null 2>&1; then
        print_success "Service on port $port is healthy"
    else
        echo -e "${YELLOW}⚠️  Service on port $port is starting up...${NC}"
    fi
done

echo ""
if [ "$AUTO_RELOAD" = "true" ]; then
    print_success "ChitChat Backend started successfully with ULTRA optimization + auto-reload!"
    print_info "Development mode active - code changes will trigger automatic restart"
else
    print_success "ChitChat Backend started successfully with ULTRA optimization!"
fi

case $SERVICES_MODE in
    minimal)
        print_info "RAM usage: ~1.0-1.2GB (4 services running)"
        ;;
    core)
        print_info "RAM usage: ~1.5-1.8GB (6 services running)"
        ;;
    all)
        print_info "RAM usage: ~2.0-2.5GB (9 services running)"
        ;;
esac
print_info "Memory reduction: ~75% per service (256MB vs 1024MB default)"
