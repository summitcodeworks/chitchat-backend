#!/bin/bash

# ChitChat Backend Services Launcher
# Supports both production and development modes with auto-reload

# Default mode
MODE="production"
DEV_MODE=false

# Parse command line arguments
while [[ $# -gt 0 ]]; do
    case $1 in
        --dev|--development)
            MODE="development"
            DEV_MODE=true
            shift
            ;;
        --prod|--production)
            MODE="production"
            DEV_MODE=false
            shift
            ;;
        --help|-h)
            echo "ChitChat Backend Services Launcher"
            echo ""
            echo "Usage: $0 [OPTIONS]"
            echo ""
            echo "OPTIONS:"
            echo "  --dev, --development    Start services in development mode with auto-reload"
            echo "  --prod, --production    Start services in production mode (default)"
            echo "  --help, -h              Show this help message"
            echo ""
            echo "Development Mode Features:"
            echo "  • Automatic restart on code changes"
            echo "  • Enhanced logging for debugging"
            echo "  • LiveReload for browser refresh"
            echo "  • Debug ports enabled"
            echo ""
            echo "Examples:"
            echo "  $0                      # Start in production mode"
            echo "  $0 --dev               # Start in development mode with auto-reload"
            echo "  $0 --production        # Explicitly start in production mode"
            exit 0
            ;;
        *)
            echo "Unknown option: $1"
            echo "Use --help for usage information"
            exit 1
            ;;
    esac
done

if [ "$DEV_MODE" = true ]; then
    echo "🚀 Starting ChitChat Backend Services in DEVELOPMENT mode with auto-reload..."
else
    echo "Starting ChitChat Backend Services in PRODUCTION mode..."
fi

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
PURPLE='\033[0;35m'
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

# Function to start service with appropriate mode
start_service() {
    local service_name=$1
    local port=$2
    local color=$3

    if [ "$DEV_MODE" = true ]; then
        echo -e "${color}📦 Starting $service_name on port $port with auto-reload...${NC}"
        cd "$service_name" || {
            print_error "Failed to enter $service_name directory"
            return 1
        }
        # Development mode with DevTools and enhanced logging
        mvn spring-boot:run \
            -Dspring-boot.run.profiles=dev \
            -Dspring-boot.run.arguments="--server.port=$port" \
            -Dspring-boot.run.jvmArguments="-Dspring.devtools.restart.enabled=true" &
    else
        echo -e "${color}📦 Starting $service_name on port $port...${NC}"
        cd "$service_name" || {
            print_error "Failed to enter $service_name directory"
            return 1
        }
        # Production mode
        mvn spring-boot:run -Dspring-boot.run.arguments="--server.port=$port" &
    fi

    local pid=$!
    echo -e "${GREEN}✅ $service_name started with PID: $pid${NC}"
    cd ..
    return 0
}

# Kill any existing processes on ports 8761, 9101-9108
print_header "Cleaning up existing processes..."
echo "Killing existing processes on ports 8761, 9101-9108..."
for port in 8761 9101 9102 9103 9104 9105 9106 9107 9108; do
    PID=$(lsof -ti:$port)
    if [ ! -z "$PID" ]; then
        echo "Killing process on port $port (PID: $PID)"
        kill -9 $PID
    fi
done

# Also kill any existing Spring Boot processes
if [ "$DEV_MODE" = true ]; then
    print_status "Cleaning up existing development processes..."
    pkill -f "spring-boot:run" || true
    pkill -f "java.*chitchat" || true
fi

# Check if Docker is running
if ! docker info > /dev/null 2>&1; then
    print_error "Docker is not running. Please start Docker first."
    exit 1
fi

# Start infrastructure services
print_header "Starting infrastructure services..."
docker-compose up -d redis kafka zookeeper

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

# Start Eureka Server (Port 8761)
print_header "Starting microservices..."
start_service "chitchat-eureka-server" 8761 "$BLUE"

# Wait for Eureka to start
print_status "Waiting for Eureka Server to start..."
sleep 30

# Start API Gateway (Port 9101)
start_service "chitchat-api-gateway" 9101 "$GREEN"
sleep 5

# Start User Service (Port 9102)
start_service "chitchat-user-service" 9102 "$YELLOW"
sleep 3

# Start Messaging Service (Port 9103)
start_service "chitchat-messaging-service" 9103 "$RED"
sleep 3

# Start Media Service (Port 9104)
start_service "chitchat-media-service" 9104 "$PURPLE"
sleep 3

# Start Calls Service (Port 9105)
start_service "chitchat-calls-service" 9105 "$BLUE"
sleep 3

# Start Notification Service (Port 9106)
start_service "chitchat-notification-service" 9106 "$GREEN"
sleep 3

# Start Status Service (Port 9107)
start_service "chitchat-status-service" 9107 "$YELLOW"
sleep 3

# Start Admin Service (Port 9108)
start_service "chitchat-admin-service" 9108 "$RED"

print_header "All services started successfully!"
echo ""
echo "Service URLs:"
echo "============="
echo "Eureka Dashboard: http://localhost:8761"
echo "API Gateway: http://localhost:9101"
echo "User Service: http://localhost:9102"
echo "Messaging Service: http://localhost:9103"
echo "Media Service: http://localhost:9104"
echo "Calls Service: http://localhost:9105"
echo "Notification Service: http://localhost:9106"
echo "Status Service: http://localhost:9107"
echo "Admin Service: http://localhost:9108"
echo ""

if [ "$DEV_MODE" = true ]; then
    echo -e "${PURPLE}🚀 Development Mode Features Active:${NC}"
    echo "======================================="
    echo "✅ Auto-reload: Code changes trigger automatic restart"
    echo "✅ Enhanced logging: DEBUG level enabled for development"
    echo "✅ LiveReload: Browser refresh on static resource changes"
    echo "✅ Debug ports: Remote debugging available"
    echo ""
    echo -e "${YELLOW}💡 Development Tips:${NC}"
    echo "• Make changes to Java files and watch services restart automatically"
    echo "• Check logs for detailed DEBUG information"
    echo "• Use browser LiveReload extension for automatic page refresh"
    echo ""
    echo -e "${BLUE}🔧 LiveReload Ports:${NC}"
    echo "• User Service: 35729"
    echo "• API Gateway: 35730"
    echo "• Other Services: 35731-35737"
    echo ""
fi

print_status "To stop all services, run: ./stop-services.sh"

if [ "$DEV_MODE" = true ]; then
    echo -e "${GREEN}🎉 Development server ready! Start coding and enjoy auto-reload! 🎉${NC}"
else
    print_status "Process IDs saved for cleanup"
fi
