#!/bin/bash

# ChitChat Backend Log Viewer
# Provides easy access to view logs from different services

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
PURPLE='\033[0;35m'
NC='\033[0m' # No Color

# Available services
SERVICES=("api-gateway" "user-service" "messaging-service" "media-service" "calls-service" "notification-service" "status-service" "admin-service" "eureka-server")

# Function to show usage
show_usage() {
    echo -e "${BLUE}ChitChat Backend Log Viewer${NC}"
    echo "=============================="
    echo ""
    echo "Usage: $0 [SERVICE] [LOG_TYPE]"
    echo ""
    echo "Services:"
    for service in "${SERVICES[@]}"; do
        echo "  • $service"
    done
    echo ""
    echo "Log Types:"
    echo "  • console    - Maven console output (default)"
    echo "  • app        - Application logs"
    echo "  • error      - Error logs only"
    echo "  • auth       - Authentication logs (api-gateway, user-service only)"
    echo "  • otp        - OTP logs (user-service only)"
    echo "  • messages   - Message logs (messaging-service only)"
    echo ""
    echo "Special Commands:"
    echo "  • all        - View all console logs"
    echo "  • errors     - View all error logs"
    echo "  • auth-all   - View all authentication logs"
    echo ""
    echo "Examples:"
    echo "  $0 api-gateway           # View API Gateway console logs"
    echo "  $0 user-service app      # View User Service application logs"
    echo "  $0 api-gateway auth      # View API Gateway authentication logs"
    echo "  $0 all                   # View all console logs"
    echo "  $0 errors                # View all error logs"
    echo ""
}

# Function to check if service exists
service_exists() {
    local service=$1
    for s in "${SERVICES[@]}"; do
        if [[ $s == $service ]]; then
            return 0
        fi
    done
    return 1
}

# Function to check if log file exists
log_exists() {
    local log_path=$1
    if [[ -f $log_path ]]; then
        return 0
    else
        echo -e "${RED}Log file not found: $log_path${NC}"
        echo "Make sure the service is running and has generated logs."
        return 1
    fi
}

# Main logic
SERVICE=${1:-""}
LOG_TYPE=${2:-"console"}

if [[ -z $SERVICE ]]; then
    show_usage
    exit 0
fi

case $SERVICE in
    "all")
        echo -e "${GREEN}Viewing all console logs...${NC}"
        echo "Press Ctrl+C to exit"
        tail -f logs/*/console.log
        ;;
    "errors")
        echo -e "${RED}Viewing all error logs...${NC}"
        echo "Press Ctrl+C to exit"
        tail -f logs/*/*-error.log 2>/dev/null || {
            echo "No error logs found. Services may not be running or no errors have occurred."
            exit 1
        }
        ;;
    "auth-all")
        echo -e "${YELLOW}Viewing all authentication logs...${NC}"
        echo "Press Ctrl+C to exit"
        tail -f logs/{api-gateway,user-service}/*-auth.log 2>/dev/null || {
            echo "No authentication logs found. Services may not be running."
            exit 1
        }
        ;;
    *)
        if ! service_exists "$SERVICE"; then
            echo -e "${RED}Error: Unknown service '$SERVICE'${NC}"
            echo ""
            show_usage
            exit 1
        fi

        case $LOG_TYPE in
            "console")
                LOG_PATH="logs/$SERVICE/console.log"
                if log_exists "$LOG_PATH"; then
                    echo -e "${GREEN}Viewing $SERVICE console logs...${NC}"
                    echo "Press Ctrl+C to exit"
                    tail -f "$LOG_PATH"
                fi
                ;;
            "app")
                LOG_PATH="logs/$SERVICE/$SERVICE.log"
                if log_exists "$LOG_PATH"; then
                    echo -e "${GREEN}Viewing $SERVICE application logs...${NC}"
                    echo "Press Ctrl+C to exit"
                    tail -f "$LOG_PATH"
                fi
                ;;
            "error")
                LOG_PATH="logs/$SERVICE/$SERVICE-error.log"
                if log_exists "$LOG_PATH"; then
                    echo -e "${RED}Viewing $SERVICE error logs...${NC}"
                    echo "Press Ctrl+C to exit"
                    tail -f "$LOG_PATH"
                fi
                ;;
            "auth")
                if [[ $SERVICE == "api-gateway" || $SERVICE == "user-service" ]]; then
                    LOG_PATH="logs/$SERVICE/$SERVICE-auth.log"
                    if log_exists "$LOG_PATH"; then
                        echo -e "${YELLOW}Viewing $SERVICE authentication logs...${NC}"
                        echo "Press Ctrl+C to exit"
                        tail -f "$LOG_PATH"
                    fi
                else
                    echo -e "${RED}Error: Authentication logs only available for api-gateway and user-service${NC}"
                    exit 1
                fi
                ;;
            "otp")
                if [[ $SERVICE == "user-service" ]]; then
                    LOG_PATH="logs/$SERVICE/$SERVICE-otp.log"
                    if log_exists "$LOG_PATH"; then
                        echo -e "${PURPLE}Viewing $SERVICE OTP logs...${NC}"
                        echo "Press Ctrl+C to exit"
                        tail -f "$LOG_PATH"
                    fi
                else
                    echo -e "${RED}Error: OTP logs only available for user-service${NC}"
                    exit 1
                fi
                ;;
            "messages")
                if [[ $SERVICE == "messaging-service" ]]; then
                    LOG_PATH="logs/$SERVICE/$SERVICE-messages.log"
                    if log_exists "$LOG_PATH"; then
                        echo -e "${PURPLE}Viewing $SERVICE message logs...${NC}"
                        echo "Press Ctrl+C to exit"
                        tail -f "$LOG_PATH"
                    fi
                else
                    echo -e "${RED}Error: Message logs only available for messaging-service${NC}"
                    exit 1
                fi
                ;;
            *)
                echo -e "${RED}Error: Unknown log type '$LOG_TYPE'${NC}"
                echo ""
                show_usage
                exit 1
                ;;
        esac
        ;;
esac