#!/bin/bash

# ChitChat Backend Memory Monitor (Simple Version)
# Monitors memory usage of all services

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

print_header() {
    echo -e "${BLUE}"
    echo "=================================================="
    echo "📊 ChitChat Backend Memory Monitor"
    echo "=================================================="
    echo -e "${NC}"
}

print_error() {
    echo -e "${RED}❌ $1${NC}"
}

print_success() {
    echo -e "${GREEN}✅ $1${NC}"
}

print_warning() {
    echo -e "${YELLOW}⚠️  $1${NC}"
}

print_info() {
    echo -e "${BLUE}ℹ️  $1${NC}"
}

# Function to get memory usage for a process
get_memory_usage() {
    local pid=$1
    if [ -z "$pid" ] || ! kill -0 "$pid" 2>/dev/null; then
        echo "0"
        return
    fi
    
    # Get RSS (Resident Set Size) in KB, convert to MB
    local rss_kb=$(ps -o rss= -p "$pid" 2>/dev/null | tr -d ' ')
    if [ -z "$rss_kb" ]; then
        echo "0"
        return
    fi
    
    # Convert KB to MB
    local rss_mb=$((rss_kb / 1024))
    echo "$rss_mb"
}

# Function to get total system memory
get_total_memory() {
    # Get total memory in MB
    local total_kb=$(sysctl -n hw.memsize 2>/dev/null || echo "0")
    local total_mb=$((total_kb / 1024 / 1024))
    echo "$total_mb"
}

# Function to check if service is running
is_service_running() {
    local port=$1
    lsof -ti:$port >/dev/null 2>&1
}

# Function to get service PID
get_service_pid() {
    local port=$1
    lsof -ti:$port 2>/dev/null || echo ""
}

print_header

# Get total system memory
TOTAL_MEMORY=$(get_total_memory)
echo -e "${BLUE}💻 Total System Memory: ${TOTAL_MEMORY}MB${NC}"
echo ""

echo -e "${BLUE}📊 Service Memory Usage:${NC}"
echo ""

TOTAL_USAGE=0
RUNNING_SERVICES=0

# Check each service
check_service() {
    local port=$1
    local service_name=$2
    
    if is_service_running "$port"; then
        pid=$(get_service_pid "$port")
        if [ -n "$pid" ]; then
            memory_mb=$(get_memory_usage "$pid")
            TOTAL_USAGE=$((TOTAL_USAGE + memory_mb))
            RUNNING_SERVICES=$((RUNNING_SERVICES + 1))
            
            # Color code based on memory usage
            if [ "$memory_mb" -gt 500 ]; then
                color="$RED"
                status="HIGH"
            elif [ "$memory_mb" -gt 300 ]; then
                color="$YELLOW"
                status="MEDIUM"
            else
                color="$GREEN"
                status="GOOD"
            fi
            
            printf "%-20s | Port %-4s | PID %-6s | %s%6d MB%s | %s%s%s\n" \
                "$service_name" "$port" "$pid" "$color" "$memory_mb" "$NC" "$color" "$status" "$NC"
        else
            printf "%-20s | Port %-4s | %sRUNNING (no PID)%s\n" \
                "$service_name" "$port" "$YELLOW" "$NC"
        fi
    else
        printf "%-20s | Port %-4s | %sNOT RUNNING%s\n" \
            "$service_name" "$port" "$RED" "$NC"
    fi
}

# Check all services
check_service "8761" "Eureka Server"
check_service "9101" "API Gateway"
check_service "9102" "User Service"
check_service "9103" "Messaging Service"
check_service "9104" "Media Service"
check_service "9105" "Calls Service"
check_service "9106" "Notification Service"
check_service "9107" "Status Service"
check_service "9108" "Admin Service"

echo ""
echo -e "${BLUE}📈 Summary:${NC}"
echo -e "Running Services: ${RUNNING_SERVICES}/9"
echo -e "Total Memory Usage: ${TOTAL_USAGE}MB"

# Calculate percentage
if [ "$TOTAL_MEMORY" -gt 0 ]; then
    PERCENTAGE=$((TOTAL_USAGE * 100 / TOTAL_MEMORY))
    echo -e "Memory Usage: ${PERCENTAGE}% of total system memory"
    
    if [ "$PERCENTAGE" -gt 80 ]; then
        print_warning "High memory usage detected!"
    elif [ "$PERCENTAGE" -gt 60 ]; then
        print_info "Moderate memory usage"
    else
        print_success "Memory usage is within normal range"
    fi
fi

echo ""
echo -e "${BLUE}🎯 Memory Optimization Status:${NC}"

# Check if optimized profiles are being used
if [ "$RUNNING_SERVICES" -gt 0 ]; then
    # Check JVM arguments of first running service
    first_pid=$(lsof -ti:8761 2>/dev/null || lsof -ti:9101 2>/dev/null || lsof -ti:9102 2>/dev/null || echo "")
    if [ -n "$first_pid" ]; then
        jvm_args=$(ps -p "$first_pid" -o args= 2>/dev/null | grep -o '\-Xmx[0-9]*[mMgG]' || echo "")
        if [[ "$jvm_args" == *"512m"* ]] || [[ "$jvm_args" == *"512M"* ]]; then
            print_success "Optimized JVM settings detected (-Xmx512m)"
        else
            print_warning "Standard JVM settings detected. Consider using optimized startup script."
        fi
    fi
    
    # Check for memory leaks
    if [ "$TOTAL_USAGE" -gt 4000 ]; then
        print_warning "High total memory usage. Check for memory leaks."
    fi
else
    print_error "No services are running!"
fi

echo ""
echo -e "${BLUE}🔧 Memory Optimization Commands:${NC}"
echo -e "${YELLOW}Start optimized: ./start-services-optimized.sh${NC}"
echo -e "${YELLOW}Stop services: ./stop-services.sh${NC}"
echo -e "${YELLOW}View logs: ./view-logs.sh${NC}"
echo ""
echo -e "${BLUE}📊 Detailed Process Information:${NC}"
echo -e "${YELLOW}ps aux | grep java${NC}"
echo -e "${YELLOW}top -o MEM${NC}"
