#!/bin/bash

# ChitChat Backend - Memory Optimized Startup
# Uses existing start-services.sh but with JVM memory optimizations

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
    echo "🚀 ChitChat Backend - Memory Optimized Startup"
    echo "=================================================="
    echo -e "${NC}"
}

print_success() {
    echo -e "${GREEN}✅ $1${NC}"
}

print_info() {
    echo -e "${YELLOW}ℹ️  $1${NC}"
}

print_header

# Set optimized JVM settings
export MAVEN_OPTS="-Xmx512m -Xms256m -XX:MetaspaceSize=128m -XX:MaxMetaspaceSize=256m -XX:+UseG1GC -XX:MaxGCPauseMillis=200"

print_info "Starting services with memory optimization..."
print_info "JVM Settings: -Xmx512m -Xms256m per service"
print_info "Expected RAM reduction: ~70%"

# Use the existing start script but with optimized environment
./start-services.sh

print_success "Services started with memory optimization!"
print_info "Monitor memory usage with: ./monitor-memory-simple.sh"
