# ChitChat Backend - Troubleshooting Guide

## Common Issues and Solutions

### 1. Lombok Errors
**Problem**: `lombok cannot be resolved to a type` errors
**Solution**: 
- Ensure Lombok is added to all service POM files
- Run `mvn clean install` to rebuild dependencies
- Make sure your IDE has Lombok plugin installed

### 2. Missing Dependencies
**Problem**: `Cannot resolve import` errors
**Solution**:
- Build shared-config module first: `cd chitchat-shared-config && mvn clean install`
- Then build all services: `mvn clean install`

### 3. Eureka Client Errors
**Problem**: `EnableEurekaClient cannot be resolved to a type`
**Solution**:
- Ensure Spring Cloud Netflix Eureka Client dependency is present
- Check Spring Cloud version compatibility

### 4. Database Connection Issues
**Problem**: Database connection failures
**Solution**:
- Start infrastructure services first: `docker-compose up -d postgres mongodb redis kafka zookeeper`
- Wait for services to be ready (30-60 seconds)
- Check database credentials in application.yml files

### 5. Port Conflicts
**Problem**: Port already in use errors
**Solution**:
- Check if ports are already in use: `lsof -i :8080`
- Kill existing processes or change ports in application.yml
- Use `docker-compose down` to stop all containers

### 6. Kafka Connection Issues
**Problem**: Kafka connection failures
**Solution**:
- Ensure Zookeeper is running before Kafka
- Check Kafka configuration in application.yml
- Verify Kafka topics are created

### 7. Firebase Configuration
**Problem**: Firebase authentication errors
**Solution**:
- Add Firebase credentials file to resources
- Configure Firebase project settings
- Update Firebase configuration in application.yml

### 8. AWS S3 Configuration
**Problem**: S3 upload/download failures
**Solution**:
- Configure AWS credentials
- Create S3 bucket
- Update S3 configuration in application.yml

## Build Order

1. **Infrastructure Services** (Docker Compose)
   ```bash
   docker-compose up -d postgres mongodb redis kafka zookeeper
   ```

2. **Shared Config Module**
   ```bash
   cd chitchat-shared-config
   mvn clean install
   cd ..
   ```

3. **Eureka Server**
   ```bash
   cd chitchat-eureka-server
   mvn spring-boot:run
   ```

4. **Other Services** (in any order after Eureka is running)
   ```bash
   cd chitchat-api-gateway
   mvn spring-boot:run
   ```

## Service Ports

- Eureka Server: 9100
- API Gateway: 9101
- User Service: 9102
- Messaging Service: 9103
- Media Service: 9104
- Calls Service: 9105
- Notification Service: 9106
- Status Service: 9107
- Admin Service: 9108

## Health Checks

- Eureka Dashboard: http://localhost:9100
- API Gateway Health: http://localhost:9101/actuator/health
- Service Health: http://localhost:910X/actuator/health

## Logs

Check application logs for detailed error information:
```bash
# For individual services
tail -f logs/application.log

# For Docker services
docker-compose logs -f [service-name]
```

## Database Setup

### PostgreSQL
- Default database: `chitchat`
- Host: `ec2-13-233-106-55.ap-south-1.compute.amazonaws.com:5432`
- Username: `summitcodeworks`
- Password: `8ivhaah8`

### MongoDB
- Default database: `chitchat`
- Host: `ec2-13-233-106-55.ap-south-1.compute.amazonaws.com:27017`
- Username: `summitcodeworks`
- Password: `8ivhaah8`

## Environment Variables

Set these environment variables if needed:
```bash
export SPRING_PROFILES_ACTIVE=dev
export AWS_ACCESS_KEY_ID=your_access_key
export AWS_SECRET_ACCESS_KEY=your_secret_key
export FIREBASE_PROJECT_ID=your_project_id
```

## Quick Fixes

### Reset Everything
```bash
# Stop all services
./stop-services.sh

# Clean Maven cache
mvn clean

# Remove Docker containers and volumes
docker-compose down -v

# Rebuild and start
./build.sh
./start-services.sh
```

### Check Service Status
```bash
# Check if ports are in use
netstat -tulpn | grep :910

# Check Docker containers
docker-compose ps

# Check service health
curl http://localhost:9101/actuator/health
```

## IDE Configuration

### IntelliJ IDEA
1. Install Lombok plugin
2. Enable annotation processing
3. Add Lombok to project structure

### Eclipse
1. Install Lombok jar
2. Enable annotation processing
3. Refresh project

### VS Code
1. Install Java Extension Pack
2. Install Lombok Annotations Support
3. Reload window

## Performance Issues

### Memory Issues
- Increase JVM heap size: `-Xmx2g -Xms1g`
- Monitor memory usage with JVisualVM

### Database Performance
- Add database indexes
- Optimize queries
- Use connection pooling

### Network Issues
- Check firewall settings
- Verify network connectivity
- Monitor network latency
