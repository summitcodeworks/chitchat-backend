# ChitChat Authentication Guide

## Overview

ChitChat uses a comprehensive authentication system that supports both Firebase ID tokens and internal JWT tokens. The API Gateway handles authentication for all microservices and WebSocket connections.

## Authentication Flow

### 1. Firebase Authentication (Primary)

**For Mobile Apps (Android/iOS) and Web:**

1. User authenticates with Firebase (phone number, email, etc.)
2. Firebase returns an ID token
3. Client sends requests with `Authorization: Bearer <firebase-id-token>`
4. API Gateway validates the Firebase token
5. User information is extracted and forwarded to microservices

### 2. Internal JWT Authentication (Secondary)

**For Service-to-Service Communication:**

1. User authenticates with Firebase
2. User service generates internal JWT token
3. Client can use either Firebase token or internal JWT
4. API Gateway validates the token type and forwards accordingly

## API Endpoints

### Base URL
```
Production: http://65.1.185.194:9101
Development: http://localhost:9101
```

### Public Endpoints (No Authentication Required)

```http
POST /api/users/register
POST /api/users/login
POST /api/users/verify-otp
POST /api/users/refresh-token
GET /actuator/health
GET /actuator/info
GET /actuator/metrics
```

### Protected Endpoints (Authentication Required)

All other `/api/**` endpoints require authentication.

## Authentication Headers

### Required Header
```http
Authorization: Bearer <token>
```

### User Information Headers (Added by API Gateway)

When a request is authenticated, the API Gateway adds these headers for downstream services:

**For Firebase Tokens:**
```http
X-User-UID: <firebase-uid>
X-User-Phone: <phone-number>
X-User-Email: <email>
X-User-Name: <display-name>
X-Token-Type: firebase
```

**For JWT Tokens:**
```http
X-User-ID: <user-id>
X-User-Username: <username>
X-User-Phone: <phone-number>
X-Token-Type: jwt
```

## WebSocket Authentication

### Connection URL Format
```
ws://65.1.185.194:9101/ws/messages?token=<firebase-id-token>
ws://65.1.185.194:9101/ws/notifications?token=<firebase-id-token>
ws://65.1.185.194:9101/ws/calls?token=<firebase-id-token>
```

### Alternative: Authorization Header
```javascript
const ws = new WebSocket('ws://65.1.185.194:9101/ws/messages', [], {
    headers: {
        'Authorization': 'Bearer <firebase-id-token>'
    }
});
```

## Client Integration Examples

### 1. Android (Kotlin)

```kotlin
// Get Firebase ID token
FirebaseAuth.getInstance().currentUser?.getIdToken(true)
    ?.addOnCompleteListener { task ->
        if (task.isSuccessful) {
            val token = task.result?.token
            // Use token in API calls
            makeApiCall(token)
        }
    }

// Make API call
private fun makeApiCall(token: String) {
    val client = OkHttpClient()
    val request = Request.Builder()
        .url("http://65.1.185.194:9101/api/messages/send")
        .addHeader("Authorization", "Bearer $token")
        .addHeader("Content-Type", "application/json")
        .post(requestBody)
        .build()
    
    client.newCall(request).enqueue(callback)
}

// WebSocket connection
private fun connectWebSocket(token: String) {
    val wsUrl = "ws://65.1.185.194:9101/ws/messages?token=$token"
    val webSocket = client.newWebSocket(Request.Builder().url(wsUrl).build(), webSocketListener)
}
```

### 2. iOS (Swift)

```swift
// Get Firebase ID token
Auth.auth().currentUser?.getIDToken(completion: { token, error in
    if let token = token {
        // Use token in API calls
        makeApiCall(token: token)
    }
})

// Make API call
func makeApiCall(token: String) {
    var request = URLRequest(url: URL(string: "http://65.1.185.194:9101/api/messages/send")!)
    request.httpMethod = "POST"
    request.setValue("Bearer \(token)", forHTTPHeaderField: "Authorization")
    request.setValue("application/json", forHTTPHeaderField: "Content-Type")
    
    URLSession.shared.dataTask(with: request).resume()
}

// WebSocket connection
func connectWebSocket(token: String) {
    let wsUrl = "ws://65.1.185.194:9101/ws/messages?token=\(token)"
    let webSocket = URLSessionWebSocketTask(url: URL(string: wsUrl)!)
    webSocket.resume()
}
```

### 3. Angular (TypeScript)

```typescript
// Get Firebase ID token
import { AngularFireAuth } from '@angular/fire/auth';

constructor(private afAuth: AngularFireAuth) {}

async getToken(): Promise<string> {
  const user = await this.afAuth.currentUser;
  return await user.getIdToken();
}

// Make API call
async sendMessage(message: any) {
  const token = await this.getToken();
  
  const response = await fetch('http://65.1.185.194:9101/api/messages/send', {
    method: 'POST',
    headers: {
      'Authorization': `Bearer ${token}`,
      'Content-Type': 'application/json'
    },
    body: JSON.stringify(message)
  });
  
  return response.json();
}

// WebSocket connection
connectWebSocket() {
  this.getToken().then(token => {
    const wsUrl = `ws://65.1.185.194:9101/ws/messages?token=${token}`;
    this.webSocket = new WebSocket(wsUrl);
    
    this.webSocket.onopen = () => {
      console.log('WebSocket connected');
    };
    
    this.webSocket.onmessage = (event) => {
      const data = JSON.parse(event.data);
      console.log('Received:', data);
    };
  });
}
```

### 4. React Native

```javascript
// Get Firebase ID token
import auth from '@react-native-firebase/auth';

const getToken = async () => {
  const user = auth().currentUser;
  return await user.getIdToken();
};

// Make API call
const sendMessage = async (message) => {
  const token = await getToken();
  
  const response = await fetch('http://65.1.185.194:9101/api/messages/send', {
    method: 'POST',
    headers: {
      'Authorization': `Bearer ${token}`,
      'Content-Type': 'application/json'
    },
    body: JSON.stringify(message)
  });
  
  return response.json();
};

// WebSocket connection
const connectWebSocket = async () => {
  const token = await getToken();
  const wsUrl = `ws://65.1.185.194:9101/ws/messages?token=${token}`;
  
  const ws = new WebSocket(wsUrl);
  
  ws.onopen = () => {
    console.log('WebSocket connected');
  };
  
  ws.onmessage = (event) => {
    const data = JSON.parse(event.data);
    console.log('Received:', data);
  };
};
```

## Error Responses

### 401 Unauthorized
```json
{
  "error": "Unauthorized",
  "message": "No authentication token provided",
  "timestamp": "2024-01-15T10:30:00Z"
}
```

### 401 Invalid Token
```json
{
  "error": "Unauthorized",
  "message": "Invalid token",
  "timestamp": "2024-01-15T10:30:00Z"
}
```

### 401 Token Expired
```json
{
  "error": "Unauthorized",
  "message": "Token has expired",
  "timestamp": "2024-01-15T10:30:00Z"
}
```

## Token Refresh

### Firebase Tokens
Firebase ID tokens expire after 1 hour. Clients should refresh tokens automatically:

```javascript
// Auto-refresh token every 50 minutes
setInterval(async () => {
  const user = firebase.auth().currentUser;
  if (user) {
    await user.getIdToken(true); // Force refresh
  }
}, 50 * 60 * 1000);
```

### Internal JWT Tokens
Internal JWT tokens expire after 24 hours. Use the refresh token endpoint:

```http
POST /api/users/refresh-token
Authorization: Bearer <current-token>
```

## Security Best Practices

1. **Always use HTTPS in production**
2. **Store tokens securely** (Keychain on iOS, Keystore on Android)
3. **Implement token refresh logic**
4. **Validate tokens on the client side** before making requests
5. **Handle authentication errors gracefully**
6. **Use WebSocket authentication** for real-time features

## Testing Authentication

### Using cURL

```bash
# Test with Firebase token
curl -X POST "http://65.1.185.194:9101/api/messages/send" \
  -H "Authorization: Bearer <firebase-id-token>" \
  -H "Content-Type: application/json" \
  -d '{"content":"Hello","messageType":"TEXT","receiverId":1}'

# Test WebSocket connection
wscat -c "ws://65.1.185.194:9101/ws/messages?token=<firebase-id-token>"
```

### Using Postman

1. Set Authorization header: `Bearer <firebase-id-token>`
2. Make requests to protected endpoints
3. Check response headers for user information

## Troubleshooting

### Common Issues

1. **401 Unauthorized**: Check if token is valid and not expired
2. **WebSocket connection fails**: Ensure token is passed correctly
3. **Token validation fails**: Verify Firebase project configuration
4. **CORS errors**: Check if origin is allowed in CORS configuration

### Debug Mode

Enable debug logging in API Gateway:

```yaml
logging:
  level:
    com.chitchat.gateway: DEBUG
    org.springframework.cloud.gateway: DEBUG
```

## Production Deployment

### Environment Variables

```bash
# JWT Configuration
JWT_SECRET=your-super-secret-jwt-key
JWT_EXPIRATION=86400000

# Firebase Configuration
FIREBASE_PROJECT_ID=chitchat-9c074
FIREBASE_WEB_API_KEY=your-firebase-web-api-key
```

### Security Headers

Ensure these headers are set in production:

```http
Strict-Transport-Security: max-age=31536000; includeSubDomains
X-Content-Type-Options: nosniff
X-Frame-Options: DENY
X-XSS-Protection: 1; mode=block
```

## Support

For authentication issues:

1. Check the API Gateway logs
2. Verify Firebase configuration
3. Test with a valid Firebase ID token
4. Ensure all required headers are present

## Changelog

- **v1.0.0**: Initial authentication system with Firebase and JWT support
- **v1.1.0**: Added WebSocket authentication
- **v1.2.0**: Enhanced error handling and logging
