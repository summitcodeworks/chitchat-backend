# ChitChat Backend - Log Viewing Guide

This guide explains how to view logs for the ChitChat Backend microservices system in different deployment modes.

## Table of Contents
- [Overview](#overview)
- [Service Architecture](#service-architecture)
- [Development Mode Logs](#development-mode-logs)
- [Production Mode Logs](#production-mode-logs)
- [Docker Container Logs](#docker-container-logs)
- [Infrastructure Service Logs](#infrastructure-service-logs)
- [Log Levels and Configuration](#log-levels-and-configuration)
- [Troubleshooting Common Issues](#troubleshooting-common-issues)
- [Log Monitoring Tools](#log-monitoring-tools)

## Overview

The ChitChat Backend consists of 9 microservices, each running on different ports:
- **Eureka Server** (9100): Service discovery
- **API Gateway** (9101): Request routing
- **User Service** (9102): User management
- **Messaging Service** (9103): Chat functionality
- **Media Service** (9104): File uploads/downloads
- **Calls Service** (9105): Voice/video calls
- **Notification Service** (9106): Push notifications
- **Status Service** (9107): User status management
- **Admin Service** (9108): Administrative functions

## Service Architecture

### Running Services Check
```bash
# Check which services are running
./start-services.sh --help  # View available modes
ps aux | grep java          # List Java processes
lsof -i :9100-9108         # Check ports 9100-9108
```

### Service URLs
- Eureka Dashboard: http://localhost:9100
- API Gateway: http://localhost:9101
- Individual services: http://localhost:910[2-8]

## Development Mode Logs

When running with `./start-services.sh --dev`, services run with enhanced logging.

### Real-time Log Monitoring

#### View All Service Logs
```bash
# Monitor all Spring Boot processes
ps aux | grep "spring-boot:run" | awk '{print $2}' | xargs -I {} tail -f /proc/{}/fd/1 2>/dev/null
```

#### Individual Service Logs
```bash
# User Service logs
cd chitchat-user-service
mvn spring-boot:run -Dspring-boot.run.profiles=dev

# API Gateway logs
cd chitchat-api-gateway
mvn spring-boot:run -Dspring-boot.run.profiles=dev

# Any specific service
cd chitchat-[service-name]
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

#### Finding Process IDs
```bash
# Find PID for a specific service
lsof -ti :9102  # User service port
lsof -ti :9101  # API Gateway port

# View logs by PID
tail -f /proc/[PID]/fd/1  # stdout
tail -f /proc/[PID]/fd/2  # stderr
```

### Development Logging Configuration

Development mode enables:
- **DEBUG** level logging for `com.chitchat.*` packages
- **DEBUG** level for Spring Web components
- **TRACE** level for Hibernate SQL binding
- SQL query formatting and display
- Auto-reload on code changes

#### Key Log Categories in Dev Mode:
```yaml
logging:
  level:
    com.chitchat: DEBUG
    org.springframework.web: DEBUG
    org.hibernate.SQL: DEBUG
    org.hibernate.type.descriptor.sql.BasicBinder: TRACE
```

## Production Mode Logs

When running with `./start-services.sh` (default production mode):

### Service Log Locations
```bash
# Production logs are typically in:
# Each service directory when run via Maven
cd chitchat-user-service && mvn spring-boot:run  # Logs to console

# Or check Maven output files
find . -name "*.log" -type f  # Look for any log files
```

### Production Logging Configuration
```yaml
logging:
  level:
    com.chitchat.user: DEBUG      # Service-specific debug
    org.springframework.web: DEBUG
```

## Docker Container Logs

If using Docker deployment (via docker-compose.yml):

### Container Log Commands
```bash
# View all container logs
docker-compose logs

# Follow logs in real-time
docker-compose logs -f

# Specific service logs
docker logs chitchat-user-service
docker logs chitchat-api-gateway
docker logs chitchat-messaging-service

# Follow specific service logs
docker logs -f chitchat-user-service

# View last N lines
docker logs --tail 100 chitchat-user-service

# Logs with timestamps
docker logs -t chitchat-user-service
```

### Docker Service Names
- `chitchat-eureka-server`
- `chitchat-api-gateway`
- `chitchat-user-service`
- `chitchat-messaging-service`
- `chitchat-media-service`
- `chitchat-calls-service`
- `chitchat-notification-service`
- `chitchat-status-service`
- `chitchat-admin-service`

## Infrastructure Service Logs

### Redis Logs
```bash
docker logs chitchat-redis
docker logs -f chitchat-redis
```

### Kafka Logs
```bash
docker logs chitchat-kafka
docker logs chitchat-zookeeper

# Kafka topics and consumer groups
docker exec chitchat-kafka kafka-topics --list --bootstrap-server localhost:9092
docker exec chitchat-kafka kafka-consumer-groups --list --bootstrap-server localhost:9092
```

### Database Connection Logs
```bash
# PostgreSQL connection test
psql -h ec2-65-1-185-194.ap-south-1.compute.amazonaws.com -p 5432 -U summitcodeworks -d chitchat -c "SELECT 1;"

# MongoDB connection test
mongosh "mongodb://summitcodeworks:8ivhaah8@ec2-65-1-185-194.ap-south-1.compute.amazonaws.com:27017/chitchat" --eval "db.runCommand('ping')"
```

## Log Levels and Configuration

### Current Log Levels by Service

#### API Gateway
```yaml
logging:
  level:
    org.springframework.cloud.gateway: DEBUG
    org.springframework.web.reactive: DEBUG
```

#### User Service
```yaml
logging:
  level:
    com.chitchat.user: DEBUG
    org.springframework.web: DEBUG
```

#### Development Profile Additional Logging
```yaml
logging:
  level:
    com.chitchat: DEBUG
    org.springframework.web: DEBUG
    org.hibernate.SQL: DEBUG
    org.hibernate.type.descriptor.sql.BasicBinder: TRACE
```

### Modifying Log Levels
Edit the `application.yml` or `application-dev.yml` files in each service's `src/main/resources/` directory.

## Troubleshooting Common Issues

### Service Won't Start
```bash
# Check if port is in use
lsof -i :9102  # Replace with service port

# Kill processes on port
kill -9 $(lsof -ti :9102)

# Check Eureka registration
curl http://localhost:9100/eureka/apps
```

### Database Connection Issues
```bash
# Test PostgreSQL connection
./verify-database.sh

# Manual connection test
psql -h ec2-65-1-185-194.ap-south-1.compute.amazonaws.com -p 5432 -U summitcodeworks -d chitchat

# Check MongoDB collections
mongosh "mongodb://summitcodeworks:8ivhaah8@ec2-65-1-185-194.ap-south-1.compute.amazonaws.com:27017/chitchat" --eval "db.getCollectionNames()"
```

### Memory and Performance Issues
```bash
# Check Java processes and memory usage
ps aux | grep java | awk '{print $2, $3, $4, $11}' | column -t

# Check system resources
top -p $(pgrep -f "spring-boot:run" | tr '\n' ',' | sed 's/,$//')
```

### Network Issues
```bash
# Test service connectivity
curl http://localhost:9101/actuator/health  # API Gateway health
curl http://localhost:9102/actuator/health  # User Service health

# Check all service health endpoints
for port in {9100..9108}; do
  echo "Port $port:"
  curl -s http://localhost:$port/actuator/health | jq .status 2>/dev/null || echo "Not accessible"
done
```

## Log Monitoring Tools

### Built-in Monitoring
- **Eureka Dashboard**: http://localhost:9100
- **Actuator Endpoints**: `/actuator/health`, `/actuator/metrics`, `/actuator/info`

### Manual Log Monitoring Scripts

#### Monitor All Services
```bash
#!/bin/bash
# monitor-services.sh
for port in {9100..9108}; do
  echo "=== Port $port ==="
  curl -s http://localhost:$port/actuator/health 2>/dev/null | jq . || echo "Service not running"
  echo ""
done
```

#### Log Aggregation
```bash
#!/bin/bash
# aggregate-logs.sh
mkdir -p logs/$(date +%Y-%m-%d)
for service in eureka api-gateway user messaging media calls notification status admin; do
  if pgrep -f "chitchat-$service" > /dev/null; then
    echo "Collecting logs for $service..."
    # Add log collection logic here
  fi
done
```

### Development vs Production Modes

| Feature | Development Mode | Production Mode |
|---------|------------------|-----------------|
| Log Level | DEBUG/TRACE | INFO/WARN |
| SQL Logging | Enabled | Disabled |
| Auto-reload | Enabled | Disabled |
| LiveReload | Port 35729+ | Disabled |
| JVM Debug | Port 8000+ | Disabled |

## Quick Reference Commands

```bash
# Start services in development mode with detailed logs
./start-services.sh --dev

# Start services in production mode
./start-services.sh --prod

# Stop all services
./stop-services.sh

# Check service status
ps aux | grep "spring-boot:run"

# View real-time logs for all services
tail -f /var/log/chitchat/*.log 2>/dev/null || echo "No log files found"

# Monitor specific service port
lsof -i :9102 && tail -f /proc/$(lsof -ti :9102)/fd/1

# Health check all services
for port in {9100..9108}; do curl -s http://localhost:$port/actuator/health | jq .status; done
```

## Notes

1. **Remote Databases**: This setup uses remote PostgreSQL and MongoDB instances
2. **Local Infrastructure**: Redis, Kafka, and Zookeeper run in Docker containers
3. **Log Files**: By default, Spring Boot applications log to console; configure file logging if needed
4. **Development Features**: Auto-reload, enhanced logging, and debug ports are available in dev mode
5. **Port Management**: Services run on ports 9100-9108; ensure these are available

For additional help, check the service-specific documentation or run `./start-services.sh --help`.