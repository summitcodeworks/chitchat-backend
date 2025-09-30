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

# Function to create logback configuration for a service
create_logback_config() {
    local service_dir=$1
    local log_dir=$2
    local service_name=$3

    # Create resources directory if it doesn't exist
    mkdir -p "$service_dir/src/main/resources"

    # Check if logback-spring.xml already exists
    if [ -f "$service_dir/src/main/resources/logback-spring.xml" ]; then
        return 0
    fi

    # Create logback-spring.xml
    cat > "$service_dir/src/main/resources/logback-spring.xml" << 'LOGBACK_EOF'
<?xml version="1.0" encoding="UTF-8"?>
<configuration>
    <include resource="org/springframework/boot/logging/logback/defaults.xml"/>

    <!-- Define log directory -->
    <property name="LOG_DIR" value="${LOG_DIR:-./logs/SERVICE_LOG_DIR}"/>
    <property name="SERVICE_NAME" value="SERVICE_NAME_PLACEHOLDER"/>

    <!-- Console appender -->
    <appender name="CONSOLE" class="ch.qos.logback.core.ConsoleAppender">
        <encoder>
            <pattern>%clr(%d{ISO8601}){faint} %clr(${LOG_LEVEL_PATTERN:-%5p}) %clr(${PID:- }){magenta} %clr(---){faint} %clr([%15.15t]){faint} %clr(%-40.40logger{39}){cyan} %clr(:){faint} %m%n${LOG_EXCEPTION_CONVERSION_WORD:-%wEx}</pattern>
        </encoder>
    </appender>

    <!-- File appender for general logs -->
    <appender name="FILE" class="ch.qos.logback.core.rolling.RollingFileAppender">
        <file>${LOG_DIR}/${SERVICE_NAME}.log</file>
        <encoder>
            <pattern>%d{ISO8601} [%thread] %-5level %logger{36} - %msg%n</pattern>
        </encoder>
        <rollingPolicy class="ch.qos.logback.core.rolling.SizeAndTimeBasedRollingPolicy">
            <fileNamePattern>${LOG_DIR}/${SERVICE_NAME}.%d{yyyy-MM-dd}.%i.log</fileNamePattern>
            <maxFileSize>100MB</maxFileSize>
            <maxHistory>30</maxHistory>
            <totalSizeCap>3GB</totalSizeCap>
        </rollingPolicy>
    </appender>

    <!-- File appender for error logs -->
    <appender name="ERROR_FILE" class="ch.qos.logback.core.rolling.RollingFileAppender">
        <file>${LOG_DIR}/${SERVICE_NAME}-error.log</file>
        <filter class="ch.qos.logback.classic.filter.ThresholdFilter">
            <level>ERROR</level>
        </filter>
        <encoder>
            <pattern>%d{ISO8601} [%thread] %-5level %logger{36} - %msg%n</pattern>
        </encoder>
        <rollingPolicy class="ch.qos.logback.core.rolling.SizeAndTimeBasedRollingPolicy">
            <fileNamePattern>${LOG_DIR}/${SERVICE_NAME}-error.%d{yyyy-MM-dd}.%i.log</fileNamePattern>
            <maxFileSize>100MB</maxFileSize>
            <maxHistory>30</maxHistory>
            <totalSizeCap>1GB</totalSizeCap>
        </rollingPolicy>
    </appender>

    <!-- Root logger -->
    <springProfile name="!dev">
        <root level="INFO">
            <appender-ref ref="CONSOLE"/>
            <appender-ref ref="FILE"/>
            <appender-ref ref="ERROR_FILE"/>
        </root>
    </springProfile>

    <!-- Development profile with more verbose logging -->
    <springProfile name="dev">
        <root level="DEBUG">
            <appender-ref ref="CONSOLE"/>
            <appender-ref ref="FILE"/>
            <appender-ref ref="ERROR_FILE"/>
        </root>
    </springProfile>
</configuration>
LOGBACK_EOF

    # Replace placeholders with actual values
    sed -i.bak "s/SERVICE_LOG_DIR/$log_dir/g" "$service_dir/src/main/resources/logback-spring.xml"
    sed -i.bak "s/SERVICE_NAME_PLACEHOLDER/$service_name/g" "$service_dir/src/main/resources/logback-spring.xml"
    rm -f "$service_dir/src/main/resources/logback-spring.xml.bak"

    echo "Created logback configuration for $service_dir"
}

# Function to ensure logback configurations exist for all services
ensure_logback_configs() {
    print_header "Ensuring logback configurations exist..."

    # Services and their log directories
    declare -A services=(
        ["chitchat-notification-service"]="notification-service"
        ["chitchat-media-service"]="media-service"
        ["chitchat-calls-service"]="calls-service"
        ["chitchat-status-service"]="status-service"
        ["chitchat-admin-service"]="admin-service"
        ["chitchat-eureka-server"]="eureka-server"
    )

    for service_dir in "${!services[@]}"; do
        log_dir=${services[$service_dir]}
        service_name=${services[$service_dir]}
        create_logback_config "$service_dir" "$log_dir" "$service_name"
    done

    print_status "Logback configuration check completed!"
}

# Function to start service with appropriate mode
start_service() {
    local service_name=$1
    local port=$2
    local color=$3
    local log_dir_name=$4

    # Ensure logs directory exists
    mkdir -p "logs/$log_dir_name"

    # Set log directory for the service
    local log_dir_path="$(pwd)/logs/$log_dir_name"

    if [ "$DEV_MODE" = true ]; then
        echo -e "${color}📦 Starting $service_name on port $port with auto-reload...${NC}"
        echo -e "${color}📁 Logs will be written to: logs/$log_dir_name/${NC}"
        cd "$service_name" || {
            print_error "Failed to enter $service_name directory"
            return 1
        }
        # Development mode with DevTools and enhanced logging
        mvn spring-boot:run \
            -Dspring-boot.run.profiles=dev \
            -Dspring-boot.run.arguments="--server.port=$port" \
            -Dspring-boot.run.jvmArguments="-Dspring.devtools.restart.enabled=true -DLOG_DIR=$log_dir_path" \
            2>&1 | tee "../logs/$log_dir_name/console.log" &
    else
        echo -e "${color}📦 Starting $service_name on port $port...${NC}"
        echo -e "${color}📁 Logs will be written to: logs/$log_dir_name/${NC}"
        cd "$service_name" || {
            print_error "Failed to enter $service_name directory"
            return 1
        }
        # Production mode
        mvn spring-boot:run \
            -Dspring-boot.run.arguments="--server.port=$port" \
            -Dspring-boot.run.jvmArguments="-DLOG_DIR=$log_dir_path" \
            2>&1 | tee "../logs/$log_dir_name/console.log" &
    fi

    local pid=$!
    echo -e "${GREEN}✅ $service_name started with PID: $pid${NC}"
    echo -e "${GREEN}📋 Console logs: logs/$log_dir_name/console.log${NC}"
    echo -e "${GREEN}📋 Application logs: logs/$log_dir_name/[service-name].log${NC}"
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

# Ensure all services have logback configurations
ensure_logback_configs

# Start Eureka Server (Port 8761)
print_header "Starting microservices..."
start_service "chitchat-eureka-server" 8761 "$BLUE" "eureka-server"

# Wait for Eureka to start
print_status "Waiting for Eureka Server to start..."
sleep 30

# Start API Gateway (Port 9101)
start_service "chitchat-api-gateway" 9101 "$GREEN" "api-gateway"
sleep 5

# Start User Service (Port 9102)
start_service "chitchat-user-service" 9102 "$YELLOW" "user-service"
sleep 3

# Start Messaging Service (Port 9103)
start_service "chitchat-messaging-service" 9103 "$RED" "messaging-service"
sleep 3

# Start Media Service (Port 9104)
start_service "chitchat-media-service" 9104 "$PURPLE" "media-service"
sleep 3

# Start Calls Service (Port 9105)
start_service "chitchat-calls-service" 9105 "$BLUE" "calls-service"
sleep 3

# Start Notification Service (Port 9106)
start_service "chitchat-notification-service" 9106 "$GREEN" "notification-service"
sleep 3

# Start Status Service (Port 9107)
start_service "chitchat-status-service" 9107 "$YELLOW" "status-service"
sleep 3

# Start Admin Service (Port 9108)
start_service "chitchat-admin-service" 9108 "$RED" "admin-service"

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

print_header "📋 Log Monitoring Commands"
echo "=========================="
echo "• View all service logs:     tail -f logs/*/console.log"
echo "• View specific service:     tail -f logs/[service-name]/console.log"
echo "• View application logs:     tail -f logs/[service-name]/[service-name].log"
echo "• View error logs only:      tail -f logs/*/[service-name]-error.log"
echo "• View authentication logs:  tail -f logs/{api-gateway,user-service}/*-auth.log"
echo ""
echo "Example commands:"
echo "• tail -f logs/api-gateway/console.log"
echo "• tail -f logs/user-service/user-service.log"
echo "• tail -f logs/api-gateway/api-gateway-auth.log"
echo ""
print_status "To stop all services, run: ./stop-services.sh"

if [ "$DEV_MODE" = true ]; then
    echo -e "${GREEN}🎉 Development server ready! Start coding and enjoy auto-reload! 🎉${NC}"
else
    print_status "Process IDs saved for cleanup"
fi
