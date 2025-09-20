# Auto-Reload Setup for ChitChat Backend

This document describes the auto-reload functionality integrated into the ChitChat backend, similar to nodemon in Node.js.

## 🚀 Features

### Spring Boot DevTools Integration
- **Automatic restart** when classes change
- **LiveReload** for browser refresh
- **Enhanced development logging**
- **Remote debugging support**
- **Faster restart times** compared to full application restart

## 📦 What's Been Added

### 1. Dependencies Added
- `spring-boot-devtools` dependency added to all service modules
- Configured as `runtime` scope with `optional=true`

### 2. Development Configuration
- `application-dev.yml` files created for optimized development settings
- Custom restart triggers and exclusions
- Enhanced logging levels for debugging

### 3. Development Integration
- `start-services.sh` enhanced with `--dev` mode for auto-reload
- VS Code launch configurations for debugging

## 🛠 Usage

### Option 1: Using the Integrated Start Script (Recommended)

```bash
# Start all services with auto-reload (Development Mode)
./start-services.sh --dev

# Start all services in production mode (Default)
./start-services.sh
./start-services.sh --prod

# Show help and usage information
./start-services.sh --help
```

### Option 2: Using Maven Directly

```bash
# Run individual service with development profile
cd chitchat-user-service
mvn spring-boot:run -Dspring-boot.run.profiles=dev

# Run with JVM arguments for DevTools
mvn spring-boot:run \
    -Dspring-boot.run.profiles=dev \
    -Dspring-boot.run.jvmArguments="-Dspring.devtools.restart.enabled=true"
```

### Option 3: Using IDE (VS Code/IntelliJ)

#### VS Code
1. Use the provided launch configurations in `.vscode/launch.json`
2. Select "Spring Boot-[ServiceName]" from the debug panel
3. Press F5 to start with debugging and auto-reload

#### IntelliJ IDEA
1. Right-click on main application class
2. Select "Run [ApplicationName]"
3. Add VM options: `-Dspring.devtools.restart.enabled=true`
4. Add program arguments: `--spring.profiles.active=dev`

## ⚡ How Auto-Reload Works

### File Change Detection
- **Java classes**: Automatic restart when compiled
- **Configuration files**: Restart when application.yml/properties change
- **Static resources**: LiveReload without restart (HTML, CSS, JS)

### Restart Triggers
```
src/main/java/**/*.java     → Full restart
src/main/resources/**/*.yml → Full restart
src/main/resources/static/** → LiveReload only
src/main/resources/templates/** → LiveReload only
```

### Exclusions (No Restart)
- Static resources (`static/**`, `public/**`)
- Templates (`templates/**`)
- Log files
- Test files

## 🔧 Configuration Details

### DevTools Settings (application-dev.yml)
```yaml
spring:
  devtools:
    restart:
      enabled: true
      poll-interval: 1000      # Check for changes every 1 second
      quiet-period: 400        # Wait 400ms after last change
      additional-paths: src/main/java
    livereload:
      enabled: true
      port: 35729              # LiveReload port
```

### Service-Specific Ports
- **User Service LiveReload**: 35729
- **API Gateway LiveReload**: 35730
- **Other Services**: 35731-35737

## 🌐 LiveReload Browser Integration

### Chrome/Firefox Extension
1. Install "LiveReload" browser extension
2. Enable the extension on your development pages
3. Changes to static resources will auto-refresh the browser

### Manual Browser Refresh
If not using the extension, you'll need to manually refresh the browser for static resource changes.

## 📊 Development URLs

| Service | Application URL | LiveReload Port |
|---------|-----------------|-----------------|
| Eureka Server | http://localhost:9100 | 35729 |
| API Gateway | http://localhost:9101 | 35730 |
| User Service | http://localhost:9102 | 35729 |
| Messaging Service | http://localhost:9103 | 35731 |
| Media Service | http://localhost:9104 | 35732 |
| Calls Service | http://localhost:9105 | 35733 |
| Notification Service | http://localhost:9106 | 35734 |
| Status Service | http://localhost:9107 | 35735 |
| Admin Service | http://localhost:9108 | 35736 |

## 🐛 Debugging Features

### Enhanced Logging
- **Application logs**: DEBUG level for your packages
- **Spring logs**: DEBUG level for web and security
- **SQL logs**: Formatted SQL queries and parameters
- **Gateway logs**: Request/response tracing

### Remote Debugging
- **User Service**: Debug port 8000
- **API Gateway**: Debug port 8001
- **Other services**: Ports 8002-8008

### JVM Debug Connection
```bash
# Connect debugger to User Service
java -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=8000
```

## 🔍 Troubleshooting

### Common Issues

#### 1. Services Not Restarting
```bash
# Check if DevTools is enabled
grep -r "spring-boot-devtools" */pom.xml

# Verify development profile is active
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

#### 2. LiveReload Not Working
- Ensure LiveReload port is not blocked by firewall
- Check browser extension is enabled
- Verify different ports for each service

#### 3. Slow Restart Times
- Increase JVM heap size: `-Xmx2g`
- Exclude unnecessary directories from classpath scanning
- Use SSD for faster file I/O

#### 4. Out of Memory Errors
```bash
# Increase memory for Maven
export MAVEN_OPTS="-Xmx2g -XX:MaxPermSize=512m"
```

### Performance Tips

#### 1. Faster Compilation
```bash
# Use parallel compilation
mvn spring-boot:run -T 4  # Use 4 threads
```

#### 2. Selective Service Restart
- Start only services you're working on
- Use service dependencies wisely
- Consider using profiles for different development scenarios

## 📈 Monitoring Auto-Reload

### DevTools Actuator Endpoints
```bash
# Check restart statistics
curl http://localhost:9102/actuator/info

# View application properties
curl http://localhost:9102/actuator/env
```

### Log Monitoring
```bash
# Watch logs for restart events
tail -f logs/chitchat-user-service.log | grep "Restarting"
```

## 🚀 Production Considerations

### Important Notes
- **DevTools is automatically disabled** in production
- No performance impact on production builds
- All DevTools dependencies are marked as `optional=true`
- Development configurations only active with `dev` profile

### Production Build
```bash
# DevTools will be excluded automatically
mvn clean package -Pprod
```

## 🎯 Next Steps

1. **IDE Integration**: Configure your IDE for optimal development experience
2. **Custom Triggers**: Add custom file watchers for specific use cases
3. **Docker Development**: Extend auto-reload to containerized development
4. **Test Auto-Reload**: Set up auto-reload for test execution

---

**Note**: This auto-reload setup provides a development experience similar to nodemon in Node.js, with automatic restarts on code changes and enhanced debugging capabilities.