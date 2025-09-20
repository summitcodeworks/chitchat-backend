# ChitChat Backend

An enterprise-grade chatting backend system modeled after WhatsApp, built with Spring Boot microservices architecture.

## Architecture Overview

The ChitChat backend is built using a microservices architecture with the following components:

### Core Services

1. **Eureka Server** (Port 9100) - Service discovery and registration
2. **API Gateway** (Port 9101) - Central entry point for all client requests
3. **User Service** (Port 9102) - User management, authentication, and profiles
4. **Messaging Service** (Port 9103) - Real-time messaging and group chats
5. **Media Service** (Port 9104) - File uploads, downloads, and media processing
6. **Calls Service** (Port 9105) - Voice and video call signaling
7. **Notification Service** (Port 9106) - Push notifications and alerts
8. **Status Service** (Port 9107) - Ephemeral status stories
9. **Admin Service** (Port 9108) - Administrative tools and analytics

### Infrastructure Components

- **PostgreSQL** - Relational database for structured data
- **MongoDB** - Document database for messages and statuses
- **Redis** - Caching and session management
- **Apache Kafka** - Event streaming and real-time communication
- **AWS S3** - Media file storage
- **Firebase** - Authentication and push notifications

## Technology Stack

- **Java 21**
- **Spring Boot 3.2.0**
- **Spring Cloud 2023.0.0**
- **Spring Security**
- **Spring Data JPA/MongoDB**
- **Apache Kafka**
- **Redis**
- **PostgreSQL 16**
- **MongoDB 7**
- **Docker & Docker Compose**

## Project Structure

```
chitchat-backend/
├── chitchat-shared-config/          # Shared configurations and DTOs
├── chitchat-eureka-server/          # Service discovery server
├── chitchat-api-gateway/            # API Gateway and routing
├── chitchat-user-service/           # User management service
├── chitchat-messaging-service/      # Real-time messaging service
├── chitchat-media-service/          # Media file management service
├── chitchat-calls-service/          # Voice and video calls service
├── chitchat-notification-service/   # Push notifications service
├── chitchat-status-service/         # Status updates service
├── chitchat-admin-service/          # Admin panel service
├── database/                        # Database initialization scripts
├── docker-compose.yml               # Docker orchestration
├── .gitignore                       # Git ignore rules
├── .dockerignore                    # Docker ignore rules
├── pom.xml                          # Parent POM file
└── README.md                        # This file
```

## Version Control

The project includes comprehensive `.gitignore` files to exclude unnecessary files from version control:

- **Root `.gitignore`**: Excludes build artifacts, IDE files, logs, and environment-specific files
- **Service-specific `.gitignore`**: Each microservice has its own ignore file for service-specific files
- **Database `.gitignore`**: Excludes database dumps, logs, and temporary files
- **`.dockerignore`**: Optimizes Docker builds by excluding unnecessary files

### Key Excluded Files:
- Build artifacts (`target/`, `*.jar`, `*.class`)
- IDE files (`.idea/`, `.vscode/`, `*.iml`)
- Log files (`*.log`, `logs/`)
- Environment files (`.env`, `application-local.yml`)
- Media files (`uploads/`, `downloads/`, `profiles/`)
- Firebase config files (`firebase-adminsdk-*.json`)
- Database files (`*.db`, `*.sqlite`, `*.backup`)

## Database Setup

The application uses both PostgreSQL and MongoDB databases with automatic initialization:

### Database Initialization
- **PostgreSQL**: Tables, indexes, and triggers are created automatically
- **MongoDB**: Collections, indexes, and sample data are created automatically
- **Default Admin**: Username `admin`, Password `admin123` (change in production!)

### Database Structure

#### PostgreSQL Tables
- **users** - User profiles and authentication data
- **blocks** - User blocking relationships  
- **media_files** - Media file metadata and S3 references
- **calls** - Call session logs and signaling data
- **notifications** - Push notification logs
- **device_tokens** - FCM device tokens for push notifications
- **admin_users** - Administrative user accounts
- **user_actions** - Audit logs for user actions

#### MongoDB Collections
- **messages** - Chat messages and media
- **groups** - Group chat information and members
- **statuses** - Ephemeral status updates and stories

### Database Credentials
- **PostgreSQL**: `summitcodeworks` / `8ivhaah8` / `chitchat` (Remote: ec2-13-127-179-199.ap-south-1.compute.amazonaws.com:5432)
- **MongoDB**: `chitchat` (Remote: ec2-13-127-179-199.ap-south-1.compute.amazonaws.com:27017, username: summitcodeworks, password: 8ivhaah8)

## Features

### User Management
- Phone number-based registration and authentication
- Firebase Auth integration for OTP verification
- User profiles with avatar and status
- Contact synchronization
- User blocking/unblocking
- Online/offline status tracking

### Messaging
- One-to-one messaging
- Group messaging with admin controls
- Real-time message delivery via WebSocket
- Message types: text, image, video, audio, document
- Message status tracking (sent, delivered, read)
- Message search and backup
- Typing indicators
- Read receipts

### Media Management
- File upload and download
- Image and video compression
- Thumbnail generation
- AWS S3 integration
- Media type support: images, videos, audio, documents

### Calls
- Voice and video call signaling
- WebRTC integration
- Call history and logs
- Call status tracking
- Push notifications for incoming calls

### Notifications
- Firebase Cloud Messaging integration
- Push notifications for messages, calls, and status updates
- Device token management
- Notification scheduling and retry logic

### Status Updates
- Ephemeral status stories (24-hour expiry)
- Privacy controls (public, contacts only, selected contacts)
- Status views and reactions
- Auto-cleanup of expired statuses

### Admin Tools
- User management (ban, suspend, delete)
- Analytics and reporting
- Compliance and audit logs
- Data export capabilities
- System monitoring

## Getting Started

### Prerequisites

- Java 21
- Maven 3.8+
- Docker and Docker Compose
- AWS S3 bucket (for media storage)
- Firebase project (for authentication and notifications)

### Local Development Setup

1. **Clone the repository**
   ```bash
   git clone <repository-url>
   cd chitchat-backend
   ```

2. **Start all services (includes database setup)**
   ```bash
   # This will automatically setup databases and start all services
   ./start-services.sh
   ```

3. **Configure environment variables (optional)**
   - Set up AWS S3 credentials
   - Configure Firebase project settings
   - Update database connection strings if needed

4. **Build and run services**
   ```bash
   # Build all services
   mvn clean install
   
   # Start Eureka Server first
   cd chitchat-eureka-server
   mvn spring-boot:run
   
   # Start other services in separate terminals
   cd chitchat-api-gateway
   mvn spring-boot:run
   
   # Continue with other services...
   ```

### Docker Deployment

1. **Build all services**
   ```bash
   mvn clean install
   ```

2. **Start all services with Docker Compose**
   ```bash
   docker-compose up -d
   ```

3. **Check service health**
   ```bash
   # Eureka Dashboard
   open http://localhost:9100
   
   # API Gateway
   open http://localhost:9101
   ```

## API Documentation

### Base URLs
- **API Gateway**: http://localhost:9101
- **Eureka Dashboard**: http://localhost:9100

### Key Endpoints

#### User Service
- `POST /api/users/register` - User registration
- `POST /api/users/login` - User login
- `GET /api/users/profile` - Get user profile
- `PUT /api/users/profile` - Update user profile

#### Messaging Service
- `POST /api/messages/send` - Send message
- `GET /api/messages/conversation/{userId}` - Get conversation
- `POST /api/groups` - Create group
- `GET /api/messages/search` - Search messages

#### Media Service
- `POST /api/media/upload` - Upload media
- `GET /api/media/{mediaId}/download` - Download media
- `GET /api/media/user` - Get user media

#### Calls Service
- `POST /api/calls/initiate` - Initiate call
- `POST /api/calls/{sessionId}/answer` - Answer call
- `GET /api/calls/history` - Get call history

#### Status Service
- `POST /api/status/create` - Create status
- `GET /api/status/contacts` - Get contacts' statuses
- `POST /api/status/{statusId}/view` - View status

#### Admin Service
- `POST /api/admin/login` - Admin login
- `GET /api/admin/analytics` - Get analytics
- `POST /api/admin/users/manage` - Manage users

## Monitoring and Logging

### Health Checks
All services expose health check endpoints at `/actuator/health`

### Metrics
Prometheus metrics are available at `/actuator/prometheus`

### Logging
- Centralized logging with ELK Stack
- Request/response logging for all API calls
- Distributed tracing with Spring Cloud Sleuth

## Security

- JWT-based authentication
- Role-based access control
- Input validation and sanitization
- Rate limiting
- HTTPS enforcement
- Data encryption at rest and in transit

## Performance

- Horizontal scaling support
- Connection pooling
- Caching with Redis
- Asynchronous processing
- Database indexing
- CDN integration for media files

## Contributing

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Add tests
5. Submit a pull request

## License

This project is licensed under the MIT License.

## Support

For support and questions, please contact the development team or create an issue in the repository.
