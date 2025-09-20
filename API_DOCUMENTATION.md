# ChitChat Backend API Documentation

## Overview

ChitChat is a comprehensive chat application backend built with Spring Boot microservices architecture. This documentation provides detailed REST API endpoints and WebSocket integration examples for mobile app development (Android, iOS, Angular).

### Base URLs

- **Local Development**: `http://localhost:9101`
- **Remote Server**: `http://65.1.185.194:9101`
- **Eureka Server**: `http://localhost:9100`

**Note:** Replace `localhost:9101` with `65.1.185.194:9101` when testing against the remote server.

### Authentication

All protected endpoints require an `Authorization` header with Bearer token:
```
Authorization: Bearer YOUR_JWT_TOKEN
```

### Response Format

All API responses follow this structure:
```json
{
  "success": true,
  "message": "Operation successful",
  "data": { /* Response data */ },
  "timestamp": "2024-09-20T10:30:00Z"
}
```

## Service Ports

| Service | Port | Description |
|---------|------|-------------|
| API Gateway | 9101 | Main entry point |
| User Service | 9102 | User management |
| Messaging Service | 9103 | Chat functionality |
| Media Service | 9104 | File handling |
| Calls Service | 9105 | Voice/Video calls |
| Notification Service | 9106 | Push notifications |
| Status Service | 9107 | User status updates |
| Admin Service | 9108 | Administrative functions |
| Eureka Server | 9100 | Service discovery |

---

## 1. User Service APIs (`/api/users`)

### Firebase Token Authentication
```bash
curl -X POST http://localhost:9101/api/users/authenticate \
  -H "Content-Type: application/json" \
  -d '{
    "idToken": "firebase_id_token_here",
    "name": "John Doe",
    "deviceInfo": "Android App"
  }'
```

**How it works:** This endpoint handles both registration and login using Firebase authentication:
- Frontend authenticates with Firebase directly (handles OTP internally)
- Frontend sends Firebase ID token to backend
- Backend verifies token and extracts user information
- If the phone number exists in the database → **Login**
- If the phone number doesn't exist → **Registration** (name optional)

**Required Fields:**
- `idToken`: String (Firebase ID token from frontend authentication)

**Optional Fields:**
- `name`: String (User's display name - used if not available in Firebase)
- `deviceInfo`: String (Device information for analytics)

**Frontend Integration:**
```javascript
// Example for web/mobile
import { signInWithPhoneNumber, RecaptchaVerifier } from 'firebase/auth';

// 1. Authenticate with Firebase
const userCredential = await signInWithPhoneNumber(auth, phoneNumber, recaptchaVerifier);
const idToken = await userCredential.user.getIdToken();

// 2. Send token to backend
const response = await fetch('/api/users/authenticate', {
  method: 'POST',
  headers: { 'Content-Type': 'application/json' },
  body: JSON.stringify({
    idToken: idToken,
    name: "User Name", // optional
    deviceInfo: "device info" // optional
  })
});
```

**Response for Existing User (Login):**
```json
{
  "success": true,
  "message": "Firebase authentication successful",
  "data": {
    "token": "eyJhbGciOiJIUzI1NiJ9...",
    "user": {
      "id": 1,
      "phoneNumber": "+1234567890",
      "name": "John Doe",
      "avatarUrl": null,
      "about": null,
      "lastSeen": "2025-09-20T13:51:22.606182338",
      "isOnline": true
    },
    "isNewUser": false
  }
}
```

**Response for New User (Registration):**
```json
{
  "success": true,
  "message": "Firebase authentication successful",
  "data": {
    "token": "eyJhbGciOiJIUzI1NiJ9...",
    "user": {
      "id": 2,
      "phoneNumber": "+1234567891",
      "name": "Jane Smith",
      "avatarUrl": null,
      "about": null,
      "lastSeen": "2025-09-20T14:01:30.123456789",
      "isOnline": true,
      "createdAt": "2025-09-20T14:01:30.123456789"
    },
    "isNewUser": true
  }
}
```

**Error Response (Invalid Firebase Token):**
```json
{
  "success": false,
  "message": "Firebase authentication failed",
  "timestamp": "2025-09-20T14:01:30.123456789"
}
```

---

### Legacy Endpoints (Deprecated)

#### User Registration (Deprecated - Use Firebase `/authenticate` instead)
```bash
curl -X POST http://localhost:9101/api/users/register \
  -H "Content-Type: application/json" \
  -d '{
    "phoneNumber": "+1234567890",
    "name": "John Doe",
    "deviceInfo": "Android App"
  }'
```

#### User Login (Deprecated - Use Firebase `/authenticate` instead)
```bash
curl -X POST http://localhost:9101/api/users/login \
  -H "Content-Type: application/json" \
  -d '{
    "phoneNumber": "+1234567890",
    "otp": "123456"
  }'
```

**Note:** These legacy endpoints are deprecated. Use the new Firebase token-based `/authenticate` endpoint.

---

## Complete Authentication Flow

### Step-by-Step Example

#### 1. Frontend: Authenticate with Firebase
```javascript
// Frontend handles OTP verification with Firebase
import { signInWithPhoneNumber, RecaptchaVerifier } from 'firebase/auth';

const userCredential = await signInWithPhoneNumber(auth, phoneNumber, recaptchaVerifier);
const idToken = await userCredential.user.getIdToken();
```

#### 2. Backend: Authenticate with Firebase Token (Existing User)
```bash
curl -X POST http://localhost:9101/api/users/authenticate \
  -H "Content-Type: application/json" \
  -d '{
    "idToken": "firebase_id_token_here"
  }'
```

**Response:**
```json
{
  "success": true,
  "message": "Firebase authentication successful",
  "data": {
    "token": "eyJhbGciOiJIUzI1NiJ9...",
    "user": { /* user details */ },
    "isNewUser": false
  }
}
```

#### 3. Backend: Authenticate with Firebase Token (New User)
```bash
curl -X POST http://localhost:9101/api/users/authenticate \
  -H "Content-Type: application/json" \
  -d '{
    "idToken": "firebase_id_token_here",
    "name": "Jane Smith"
  }'
```

**Response:**
```json
{
  "success": true,
  "message": "Firebase authentication successful",
  "data": {
    "token": "eyJhbGciOiJIUzI1NiJ9...",
    "user": { /* user details */ },
    "isNewUser": true
  }
}
```

### Error Responses

#### Invalid Firebase Token
```json
{
  "success": false,
  "message": "Firebase authentication failed",
  "status": 401
}
```

#### Phone Number Not Found in Token
```json
{
  "success": false,
  "message": "Phone number not found in Firebase token",
  "status": 400
}
```

#### Token Verification Failed
```json
{
  "success": false,
  "message": "Token verification failed",
  "status": 401
}
```

### Firebase Integration Examples

#### Android (Kotlin)
```kotlin
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.PhoneAuthProvider

class AuthManager {
    private val auth = FirebaseAuth.getInstance()
    
    suspend fun authenticateWithPhone(phoneNumber: String): String {
        // Firebase handles OTP verification
        val result = auth.signInWithPhoneNumber(phoneNumber).await()
        return result.user?.getIdToken(false)?.await()?.token ?: throw Exception("Auth failed")
    }
    
    fun authenticateWithBackend(idToken: String) {
        // Send token to your backend
        val request = FirebaseAuthRequest(
            idToken = idToken,
            name = "User Name", // optional
            deviceInfo = "Android"
        )
        // Make API call to /api/users/authenticate
    }
}
```

#### iOS (Swift)
```swift
import FirebaseAuth

class AuthManager: ObservableObject {
    private let auth = Auth.auth()
    
    func authenticateWithPhone(phoneNumber: String) async throws -> String {
        // Firebase handles OTP verification
        let result = try await auth.signIn(withPhoneNumber: phoneNumber)
        return try await result.user.getIDToken()
    }
    
    func authenticateWithBackend(idToken: String) async throws {
        // Send token to your backend
        let request = FirebaseAuthRequest(
            idToken: idToken,
            name: "User Name", // optional
            deviceInfo: "iOS"
        )
        // Make API call to /api/users/authenticate
    }
}
```

#### Angular (TypeScript)
```typescript
import { Injectable } from '@angular/core';
import { signInWithPhoneNumber, RecaptchaVerifier } from 'firebase/auth';
import { auth } from './firebase-config';

@Injectable({
  providedIn: 'root'
})
export class AuthService {
  
  async authenticateWithPhone(phoneNumber: string): Promise<string> {
    const recaptchaVerifier = new RecaptchaVerifier(auth, 'recaptcha-container', {});
    const userCredential = await signInWithPhoneNumber(auth, phoneNumber, recaptchaVerifier);
    return await userCredential.user.getIdToken();
  }
  
  async authenticateWithBackend(idToken: string): Promise<any> {
    const response = await fetch('/api/users/authenticate', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        idToken: idToken,
        name: "User Name", // optional
        deviceInfo: "Web"
      })
    });
    return await response.json();
  }
}
```

### Get User Profile
```bash
curl -X GET http://localhost:9101/api/users/profile \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

### Update User Profile
```bash
curl -X PUT http://localhost:9101/api/users/profile \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "John Doe Updated",
    "avatarUrl": "https://example.com/new-profile.jpg",
    "about": "Updated bio"
  }'
```

**Required Fields:**
- `name`: String (Full name)

**Optional Fields:**
- `avatarUrl`: String (Profile picture URL)
- `about`: String (User bio/about text)

### Sync Contacts
```bash
curl -X POST http://localhost:9101/api/users/contacts/sync \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "contacts": [
      {
        "phoneNumber": "+1234567891",
        "displayName": "Contact 1"
      },
      {
        "phoneNumber": "+1234567892",
        "displayName": "Contact 2"
      }
    ]
  }'
```

### Block User
```bash
curl -X POST http://localhost:9101/api/users/block/123 \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

### Unblock User
```bash
curl -X DELETE http://localhost:9101/api/users/block/123 \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

### Get Blocked Users
```bash
curl -X GET http://localhost:9101/api/users/blocked \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

### Update Online Status
```bash
curl -X PUT "http://localhost:9101/api/users/status?isOnline=true" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

### Get User by Phone Number
```bash
curl -X GET http://localhost:9101/api/users/phone/+1234567890 \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

### Get User by ID
```bash
curl -X GET http://localhost:9101/api/users/123 \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

---

## 2. Messaging Service APIs (`/api/messages`)

### Send Message
```bash
curl -X POST http://localhost:9101/api/messages/send \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "receiverId": 123,
    "content": "Hello, how are you?",
    "messageType": "TEXT",
    "replyToMessageId": null,
    "mediaId": null
  }'
```

### Send Group Message
```bash
curl -X POST http://localhost:9101/api/messages/send \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "groupId": 456,
    "content": "Hello everyone!",
    "messageType": "TEXT",
    "replyToMessageId": null,
    "mediaId": null
  }'
```

### Get Conversation Messages
```bash
curl -X GET "http://localhost:9101/api/messages/conversation/123?page=0&size=20&sort=timestamp,desc" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

### Get Group Messages
```bash
curl -X GET "http://localhost:9101/api/messages/group/456?page=0&size=20&sort=timestamp,desc" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

### Search Messages
```bash
curl -X GET "http://localhost:9101/api/messages/search?query=hello" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

### Mark Message as Read
```bash
curl -X PUT http://localhost:9101/api/messages/789/read \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

### Delete Message
```bash
curl -X DELETE "http://localhost:9101/api/messages/789?deleteForEveryone=false" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

### Create Group
```bash
curl -X POST http://localhost:9101/api/messages/groups \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "My Group",
    "description": "A group for friends",
    "memberIds": [123, 456, 789],
    "groupPicture": "https://example.com/group.jpg"
  }'
```

### Add Member to Group
```bash
curl -X POST http://localhost:9101/api/messages/groups/456/members/123 \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

### Remove Member from Group
```bash
curl -X DELETE http://localhost:9101/api/messages/groups/456/members/123 \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

### Update Group Info
```bash
curl -X PUT "http://localhost:9101/api/messages/groups/456?name=Updated Group&description=Updated description" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

### Get User Groups
```bash
curl -X GET http://localhost:9101/api/messages/groups \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

### Leave Group
```bash
curl -X POST http://localhost:9101/api/messages/groups/456/leave \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

---

## 3. Calls Service APIs (`/api/calls`)

### Initiate Call
```bash
curl -X POST http://localhost:9101/api/calls/initiate \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "calleeId": 123,
    "callType": "VOICE",
    "groupId": null
  }'
```

### Answer Call
```bash
curl -X POST http://localhost:9101/api/calls/session123/answer \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "accepted": true,
    "sdpAnswer": "v=0\r\no=- 123456789 123456789 IN IP4 192.168.1.1\r\n..."
  }'
```

### Reject Call
```bash
curl -X POST "http://localhost:9101/api/calls/session123/reject?reason=BUSY" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

### End Call
```bash
curl -X POST "http://localhost:9101/api/calls/session123/end?reason=COMPLETED" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

### Get Call Details
```bash
curl -X GET http://localhost:9101/api/calls/session123 \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

### Get Call History
```bash
curl -X GET "http://localhost:9101/api/calls/history?page=0&size=20&sort=startTime,desc" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

### Get Missed Calls
```bash
curl -X GET http://localhost:9101/api/calls/missed \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

### Get Recent Calls
```bash
curl -X GET "http://localhost:9101/api/calls/recent?limit=10" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

---

## 4. Notification Service APIs (`/api/notifications`)

### Register Device Token
```bash
curl -X POST http://localhost:9101/api/notifications/device-token \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "deviceToken": "fcm_device_token_here",
    "deviceType": "ANDROID",
    "deviceId": "unique_device_id"
  }'
```

### Unregister Device Token
```bash
curl -X DELETE http://localhost:9101/api/notifications/device-token/unique_device_id \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

### Send Notification
```bash
curl -X POST http://localhost:9101/api/notifications/send \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "recipientId": 123,
    "title": "New Message",
    "body": "You have a new message from John",
    "type": "MESSAGE",
    "data": {
      "messageId": "789",
      "senderId": "456"
    }
  }'
```

### Send Bulk Notification
```bash
curl -X POST http://localhost:9101/api/notifications/send-bulk \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "userIds": [123, 456, 789],
    "notification": {
      "title": "Group Update",
      "body": "Group settings have been updated",
      "type": "GROUP_UPDATE",
      "data": {
        "groupId": "456"
      }
    }
  }'
```

### Get User Notifications
```bash
curl -X GET "http://localhost:9101/api/notifications/user?page=0&size=20&sort=createdAt,desc" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

### Get Unread Notifications
```bash
curl -X GET http://localhost:9101/api/notifications/unread \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

### Mark Notification as Read
```bash
curl -X PUT http://localhost:9101/api/notifications/789/read \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

### Mark All Notifications as Read
```bash
curl -X PUT http://localhost:9101/api/notifications/read-all \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

---

## 5. Status Service APIs (`/api/status`)

### Create Status
```bash
curl -X POST http://localhost:9101/api/status/create \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "content": "Having a great day!",
    "mediaId": 123,
    "statusType": "IMAGE",
    "backgroundColor": "#FF5733",
    "font": "Arial",
    "privacy": "CONTACTS"
  }'
```

### Get User Statuses
```bash
curl -X GET http://localhost:9101/api/status/user/123 \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

### Get Active Statuses
```bash
curl -X GET http://localhost:9101/api/status/active \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

### Get Contacts Statuses
```bash
curl -X GET "http://localhost:9101/api/status/contacts?contactIds=123,456,789" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

### View Status
```bash
curl -X POST http://localhost:9101/api/status/789/view \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

### React to Status
```bash
curl -X POST http://localhost:9101/api/status/789/react \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "reaction": "LIKE"
  }'
```

### Delete Status
```bash
curl -X DELETE http://localhost:9101/api/status/789 \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

### Get Status Views
```bash
curl -X GET http://localhost:9101/api/status/789/views \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

---

## 6. Media Service APIs (`/api/media`)

### Upload Media
```bash
curl -X POST http://localhost:9101/api/media/upload \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -F "file=@/path/to/your/file.jpg" \
  -F "description=Profile picture"
```

### Get Media Download URL
```bash
curl -X GET http://localhost:9101/api/media/123/download \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

### Get Media Info
```bash
curl -X GET http://localhost:9101/api/media/123 \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

### Get User Media
```bash
curl -X GET "http://localhost:9101/api/media/user?mediaType=IMAGE&page=0&size=20" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

### Delete Media
```bash
curl -X DELETE http://localhost:9101/api/media/123 \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

### Update Media Info
```bash
curl -X PUT "http://localhost:9101/api/media/123?description=Updated description" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

### Get Recent Media
```bash
curl -X GET "http://localhost:9101/api/media/recent?limit=10" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

---

## 7. Admin Service APIs (`/api/admin`)

### Admin Login
```bash
curl -X POST http://localhost:9101/api/admin/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "admin",
    "password": "adminPassword123"
  }'
```

### Get Analytics
```bash
curl -X GET "http://localhost:9101/api/admin/analytics?startDate=2024-01-01&endDate=2024-12-31" \
  -H "Authorization: Bearer ADMIN_JWT_TOKEN"
```

### Manage User
```bash
curl -X POST http://localhost:9101/api/admin/users/manage \
  -H "Authorization: Bearer ADMIN_JWT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "userId": 123,
    "action": "SUSPEND",
    "reason": "Violation of terms",
    "duration": "7_DAYS"
  }'
```

### Export User Data
```bash
curl -X POST http://localhost:9101/api/admin/users/123/export \
  -H "Authorization: Bearer ADMIN_JWT_TOKEN"
```

---

## WebSocket Integration

### Connection URLs

| Purpose | WebSocket URL | Description |
|---------|---------------|-------------|
| Real-time Messaging | `ws://localhost:9101/ws/messages` | Live chat messages |
| Call Signaling | `ws://localhost:9101/ws/calls` | Voice/Video call coordination |
| Status Updates | `ws://localhost:9101/ws/status` | Real-time user status |

### JavaScript WebSocket Example

```javascript
// Connect to messaging WebSocket
const messageSocket = new WebSocket('ws://localhost:9101/ws/messages');

messageSocket.onopen = function(event) {
    console.log('Connected to messaging WebSocket');
    // Send authentication
    messageSocket.send(JSON.stringify({
        type: 'AUTH',
        token: 'YOUR_JWT_TOKEN'
    }));
};

messageSocket.onmessage = function(event) {
    const data = JSON.parse(event.data);
    console.log('Received message:', data);

    switch(data.type) {
        case 'NEW_MESSAGE':
            handleNewMessage(data.message);
            break;
        case 'MESSAGE_READ':
            handleMessageRead(data.messageId);
            break;
        case 'USER_TYPING':
            handleUserTyping(data.userId);
            break;
    }
};

// Send a message
function sendMessage(receiverId, content) {
    messageSocket.send(JSON.stringify({
        type: 'SEND_MESSAGE',
        data: {
            receiverId: receiverId,
            content: content,
            messageType: 'TEXT'
        }
    }));
}

// Send typing indicator
function sendTypingIndicator(receiverId, isTyping) {
    messageSocket.send(JSON.stringify({
        type: 'TYPING',
        data: {
            receiverId: receiverId,
            isTyping: isTyping
        }
    }));
}
```

### Android WebSocket Integration (Kotlin)

```kotlin
class WebSocketClient {
    private var webSocket: WebSocket? = null
    private val client = OkHttpClient()

    fun connect(token: String) {
        val request = Request.Builder()
            .url("ws://localhost:9101/ws/messages")
            .build()

        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                // Authenticate
                val authMessage = JSONObject().apply {
                    put("type", "AUTH")
                    put("token", token)
                }
                webSocket.send(authMessage.toString())
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                val data = JSONObject(text)
                when (data.getString("type")) {
                    "NEW_MESSAGE" -> handleNewMessage(data.getJSONObject("message"))
                    "MESSAGE_READ" -> handleMessageRead(data.getString("messageId"))
                    "USER_TYPING" -> handleUserTyping(data.getLong("userId"))
                }
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                Log.e("WebSocket", "Connection failed", t)
            }
        })
    }

    fun sendMessage(receiverId: Long, content: String) {
        val message = JSONObject().apply {
            put("type", "SEND_MESSAGE")
            put("data", JSONObject().apply {
                put("receiverId", receiverId)
                put("content", content)
                put("messageType", "TEXT")
            })
        }
        webSocket?.send(message.toString())
    }
}
```

### iOS WebSocket Integration (Swift)

```swift
import Foundation
import Starscream

class WebSocketManager: ObservableObject, WebSocketDelegate {
    private var socket: WebSocket?

    func connect(token: String) {
        var request = URLRequest(url: URL(string: "ws://localhost:9101/ws/messages")!)
        socket = WebSocket(request: request)
        socket?.delegate = self
        socket?.connect()
    }

    func didReceive(event: WebSocketEvent, client: WebSocket) {
        switch event {
        case .connected(_):
            let authMessage = [
                "type": "AUTH",
                "token": token
            ]
            if let data = try? JSONSerialization.data(withJSONObject: authMessage),
               let string = String(data: data, encoding: .utf8) {
                socket?.write(string: string)
            }

        case .text(let text):
            if let data = text.data(using: .utf8),
               let json = try? JSONSerialization.jsonObject(with: data) as? [String: Any] {
                handleWebSocketMessage(json)
            }

        case .error(let error):
            print("WebSocket error: \(error?.localizedDescription ?? "Unknown error")")

        default:
            break
        }
    }

    func sendMessage(receiverId: Int, content: String) {
        let message = [
            "type": "SEND_MESSAGE",
            "data": [
                "receiverId": receiverId,
                "content": content,
                "messageType": "TEXT"
            ]
        ]

        if let data = try? JSONSerialization.data(withJSONObject: message),
           let string = String(data: data, encoding: .utf8) {
            socket?.write(string: string)
        }
    }
}
```

### Angular WebSocket Service

```typescript
import { Injectable } from '@angular/core';
import { Observable, Subject } from 'rxjs';

@Injectable({
  providedIn: 'root'
})
export class WebSocketService {
  private socket: WebSocket | null = null;
  private messageSubject = new Subject<any>();

  connect(token: string): Observable<any> {
    this.socket = new WebSocket('ws://localhost:9101/ws/messages');

    this.socket.onopen = () => {
      console.log('WebSocket connected');
      this.sendMessage({
        type: 'AUTH',
        token: token
      });
    };

    this.socket.onmessage = (event) => {
      const data = JSON.parse(event.data);
      this.messageSubject.next(data);
    };

    this.socket.onerror = (error) => {
      console.error('WebSocket error:', error);
    };

    return this.messageSubject.asObservable();
  }

  sendMessage(message: any): void {
    if (this.socket && this.socket.readyState === WebSocket.OPEN) {
      this.socket.send(JSON.stringify(message));
    }
  }

  disconnect(): void {
    if (this.socket) {
      this.socket.close();
    }
  }
}
```

## Error Handling

### Common HTTP Status Codes

| Code | Description | Example Response |
|------|-------------|------------------|
| 200 | Success | `{"success": true, "data": {...}}` |
| 400 | Bad Request | `{"success": false, "message": "Invalid input"}` |
| 401 | Unauthorized | `{"success": false, "message": "Invalid token"}` |
| 403 | Forbidden | `{"success": false, "message": "Access denied"}` |
| 404 | Not Found | `{"success": false, "message": "Resource not found"}` |
| 500 | Server Error | `{"success": false, "message": "Internal server error"}` |

### Error Response Example
```json
{
  "success": false,
  "message": "Validation failed",
  "errors": [
    {
      "field": "phoneNumber",
      "message": "Phone number is required"
    }
  ],
  "timestamp": "2024-09-20T10:30:00Z"
}
```

## Rate Limiting

API endpoints are rate-limited to prevent abuse:

| Endpoint Type | Limit | Window |
|---------------|-------|--------|
| Firebase Authentication | 10 requests | 1 minute |
| Messaging | 100 requests | 1 minute |
| Media Upload | 10 requests | 1 minute |
| Other APIs | 60 requests | 1 minute |

**Note:** Firebase handles OTP rate limiting on their end, so the backend only limits the token verification requests.

## Security Best Practices

1. **Always use HTTPS in production**
2. **Store JWT tokens securely** (Android: EncryptedSharedPreferences, iOS: Keychain)
3. **Implement token refresh mechanism**
4. **Validate all inputs on client side**
5. **Handle expired tokens gracefully**
6. **Use proper error handling for network failures**

## Testing with Postman

Import the following environment variables in Postman:

```json
{
  "id": "chitchat-environment",
  "name": "ChitChat Environment",
  "values": [
    {
      "key": "baseUrl",
      "value": "http://localhost:9101",
      "enabled": true
    },
    {
      "key": "authToken",
      "value": "YOUR_JWT_TOKEN_HERE",
      "enabled": true
    }
  ]
}
```

## Support

For issues and support:
- Check service health: `http://localhost:9101/actuator/health`
- View service metrics: `http://localhost:9101/actuator/metrics`
- Eureka dashboard: `http://localhost:9100`

---

## Authentication Flow Changes

**Important Update:** The authentication flow has been updated to use Firebase token-based authentication instead of OTP verification. 

### What Changed:
- ✅ **Removed:** `/api/users/send-otp` endpoint
- ✅ **Updated:** `/api/users/authenticate` now accepts Firebase ID tokens
- ✅ **Frontend:** Now handles OTP verification directly with Firebase
- ✅ **Backend:** Verifies Firebase tokens and extracts user information

### Migration Guide:
1. **Frontend:** Integrate Firebase Authentication SDK
2. **Authentication:** Use Firebase's `signInWithPhoneNumber()` method
3. **Backend:** Send Firebase ID token to `/api/users/authenticate`
4. **Security:** Firebase handles OTP rate limiting and security

---

*Last updated: September 20, 2024 - Updated for Firebase Token Authentication*