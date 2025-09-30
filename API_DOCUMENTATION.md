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

### SMS-Based Authentication (Primary Method)

The SMS-based authentication system uses Twilio for reliable SMS delivery and Redis for secure OTP storage. This provides a robust, scalable authentication solution without requiring frontend Firebase SDK integration.

#### Send OTP
```bash
curl -X POST http://localhost:9101/api/users/send-otp \
  -H "Content-Type: application/json" \
  -d '{
    "phoneNumber": "+1234567890"
  }'
```

**Note:** This endpoint does NOT require authentication. If you get a 401 error, check that the API Gateway security configuration includes `/api/users/send-otp` in the public endpoints list.

**Required Fields:**
- `phoneNumber`: String (Phone number in international format, e.g., +1234567890)

**How it works:**
1. **OTP Generation**: System generates a secure 6-digit OTP using `SecureRandom`
2. **Redis Storage**: OTP is stored in Redis with 5-minute expiration for security
3. **Twilio SMS**: OTP is sent via Twilio SMS service with custom message template
4. **WhatsApp**: OTP is also sent via WhatsApp (if user is available on WhatsApp)
5. **Push Notification**: Automatically sends a push notification to the user's registered devices (if any)
6. **Rate Limiting**: Endpoint is rate-limited to prevent abuse (5 requests per minute)

**Multi-Channel Delivery System:**

**1. SMS (Primary - Always Sent)**
- Always sent to all users
- Main delivery method
- Uses Twilio SMS service

**2. WhatsApp (Conditional - Automatic)**
- Sent automatically if SMS succeeds
- Only works if user is on WhatsApp
- Uses Twilio WhatsApp Business API
- Sender: `+918929607491` (default) or configured WhatsApp number
- Non-blocking: If WhatsApp fails, OTP request still succeeds

**3. Push Notification (Conditional - Smart)**
- Conditionally sent based on:
  - ✅ User exists in database
  - ✅ User has registered device token
  - ✅ SMS was sent successfully
- **For new users**: Only SMS (+ WhatsApp if available)
- **For existing users with device token**: SMS + WhatsApp + Push Notification
- **For existing users without device token**: SMS + WhatsApp
- Non-blocking: If push fails, OTP request still succeeds

**Delivery Priority:**
```
SMS (Required)
  ↓
If SMS Success:
  ├─→ Try WhatsApp (if available)
  └─→ Try Push Notification (if user exists & has device token)
```

**Example Scenarios:**
| User Type | SMS | WhatsApp | Push | Total Channels |
|-----------|-----|----------|------|----------------|
| New User (WhatsApp) | ✅ | ✅ | ❌ | 2 |
| New User (No WhatsApp) | ✅ | ❌ | ❌ | 1 |
| Existing (WhatsApp + Token) | ✅ | ✅ | ✅ | 3 |
| Existing (WhatsApp, No Token) | ✅ | ✅ | ❌ | 2 |
| Existing (No WhatsApp + Token) | ✅ | ❌ | ✅ | 2 |

**Response:**
```json
{
  "success": true,
  "message": "OTP sent successfully",
  "data": null,
  "timestamp": "2025-09-24T06:13:54.159093",
  "traceId": null
}
```

**Error Responses:**

*SMS Delivery Failed:*
```json
{
  "success": false,
  "message": "Failed to send OTP. Please try again.",
  "timestamp": "2025-09-24T06:13:54.159093"
}
```

*Rate Limit Exceeded:*
```json
{
  "success": false,
  "message": "Too many OTP requests. Please wait before trying again.",
  "timestamp": "2025-09-24T06:13:54.159093"
}
```

*Invalid Phone Number:*
```json
{
  "success": false,
  "message": "Phone number must be in international format (e.g., +1234567890)",
  "timestamp": "2025-09-24T06:13:54.159093"
}
```

#### Verify OTP and Authenticate
```bash
curl -X POST http://localhost:9101/api/users/verify-otp \
  -H "Content-Type: application/json" \
  -d '{
    "phoneNumber": "+1234567890",
    "otp": "123456"
  }'
```

**Required Fields:**
- `phoneNumber`: String (Same phone number used in send-otp)
- `otp`: String (6-digit OTP code received via SMS)

**How it works:**
1. **OTP Verification**: System retrieves stored OTP from Redis and compares with provided OTP
2. **Security Check**: OTP is automatically cleared after successful verification to prevent reuse
3. **User Authentication**: If OTP is valid, system authenticates user (login) or creates new user (registration)
4. **JWT Generation**: System generates JWT token with 1-hour expiration
5. **Welcome SMS**: New users receive welcome SMS via Twilio

**Response for Existing User (Login):**
```json
{
  "success": true,
  "message": "Login successful",
  "data": {
    "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
    "tokenType": "Bearer",
    "expiresIn": 3600,
    "user": {
      "id": 1,
      "phoneNumber": "+1234567890",
      "name": "John Doe",
      "avatarUrl": null,
      "about": null,
      "lastSeen": "2025-09-24T06:13:54.159093",
      "isOnline": true
    },
    "message": "Login successful"
  },
  "timestamp": "2025-09-24T06:13:54.159093"
}
```

**Response for New User (Registration):**
```json
{
  "success": true,
  "message": "User registered and authenticated successfully",
  "data": {
    "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
    "tokenType": "Bearer",
    "expiresIn": 3600,
    "user": {
      "id": 2,
      "phoneNumber": "+1234567891",
      "name": "User",
      "avatarUrl": null,
      "about": null,
      "lastSeen": "2025-09-24T06:13:54.159093",
      "isOnline": true
    },
    "message": "User registered and authenticated successfully"
  },
  "timestamp": "2025-09-24T06:13:54.159093"
}
```

**Error Responses:**

*Invalid or Expired OTP:*
```json
{
  "success": false,
  "message": "Invalid or expired OTP",
  "timestamp": "2025-09-24T06:13:54.159093"
}
```

*OTP Not Found:*
```json
{
  "success": false,
  "message": "No OTP found for this phone number. Please request a new OTP.",
  "timestamp": "2025-09-24T06:13:54.159093"
}
```

*Rate Limit Exceeded:*
```json
{
  "success": false,
  "message": "Too many verification attempts. Please wait before trying again.",
  "timestamp": "2025-09-24T06:13:54.159093"
}
```

#### Complete SMS Authentication Flow

**Step 1: Request OTP**
```bash
curl -X POST http://localhost:9101/api/users/send-otp \
  -H "Content-Type: application/json" \
  -d '{
    "phoneNumber": "+18777804236"
  }'
```

**Step 2: Verify OTP (Replace 123456 with actual OTP received)**
```bash
curl -X POST http://localhost:9101/api/users/verify-otp \
  -H "Content-Type: application/json" \
  -d '{
    "phoneNumber": "+18777804236",
    "otp": "123456"
  }'
```

**Step 3: Use JWT Token for Authenticated Requests**
```bash
curl -X POST http://localhost:9101/api/messages/send \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "content": "Hello from SMS authentication!",
    "messageType": "TEXT",
    "receiverId": 4
  }'
```

### Frontend Integration Examples

#### JavaScript/TypeScript
```javascript
class AuthService {
  async sendOtp(phoneNumber) {
    const response = await fetch('/api/users/send-otp', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ phoneNumber })
    });
    return await response.json();
  }

  async verifyOtp(phoneNumber, otp) {
    const response = await fetch('/api/users/verify-otp', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ phoneNumber, otp })
    });
    return await response.json();
  }
}

// Usage
const authService = new AuthService();

// Step 1: Send OTP
await authService.sendOtp('+18777804236');

// Step 2: Verify OTP (after user enters the code)
const authResponse = await authService.verifyOtp('+18777804236', '123456');
const token = authResponse.data.accessToken;

// Step 3: Use token for authenticated requests
localStorage.setItem('authToken', token);
```

#### Android (Kotlin)
```kotlin
class AuthManager {
    private val apiService = RetrofitClient.getApiService()
    
    suspend fun sendOtp(phoneNumber: String): ApiResponse<Void> {
        return apiService.sendOtp(SendOtpRequest(phoneNumber))
    }
    
    suspend fun verifyOtp(phoneNumber: String, otp: String): ApiResponse<AuthResponse> {
        return apiService.verifyOtp(VerifyOtpRequest(phoneNumber, otp))
    }
    
    fun saveToken(token: String) {
        // Store token securely
        val sharedPref = context.getSharedPreferences("auth", Context.MODE_PRIVATE)
        sharedPref.edit().putString("token", token).apply()
    }
}

// Usage
val authManager = AuthManager()

// Step 1: Send OTP
val otpResponse = authManager.sendOtp("+18777804236")

// Step 2: Verify OTP
val authResponse = authManager.verifyOtp("+18777804236", "123456")
val token = authResponse.data?.accessToken
authManager.saveToken(token)
```

#### iOS (Swift)
```swift
class AuthService {
    private let baseURL = "http://localhost:9101"
    
    func sendOtp(phoneNumber: String) async throws -> ApiResponse<Void> {
        let url = URL(string: "\(baseURL)/api/users/send-otp")!
        var request = URLRequest(url: url)
        request.httpMethod = "POST"
        request.setValue("application/json", forHTTPHeaderField: "Content-Type")
        
        let body = ["phoneNumber": phoneNumber]
        request.httpBody = try JSONSerialization.data(withJSONObject: body)
        
        let (data, _) = try await URLSession.shared.data(for: request)
        return try JSONDecoder().decode(ApiResponse<Void>.self, from: data)
    }
    
    func verifyOtp(phoneNumber: String, otp: String) async throws -> ApiResponse<AuthResponse> {
        let url = URL(string: "\(baseURL)/api/users/verify-otp")!
        var request = URLRequest(url: url)
        request.httpMethod = "POST"
        request.setValue("application/json", forHTTPHeaderField: "Content-Type")
        
        let body = ["phoneNumber": phoneNumber, "otp": otp]
        request.httpBody = try JSONSerialization.data(withJSONObject: body)
        
        let (data, _) = try await URLSession.shared.data(for: request)
        return try JSONDecoder().decode(ApiResponse<AuthResponse>.self, from: data)
    }
}

// Usage
let authService = AuthService()

// Step 1: Send OTP
try await authService.sendOtp(phoneNumber: "+18777804236")

// Step 2: Verify OTP
let response = try await authService.verifyOtp(phoneNumber: "+18777804236", otp: "123456")
let token = response.data.accessToken

// Step 3: Store token securely
UserDefaults.standard.set(token, forKey: "authToken")
```

---

### Firebase Token Authentication (Legacy)
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

### SMS-Based Authentication (Recommended)

#### Step-by-Step Example

#### 1. Request OTP
```bash
curl -X POST http://localhost:9101/api/users/send-otp \
  -H "Content-Type: application/json" \
  -d '{
    "phoneNumber": "+18777804236"
  }'
```

**Response:**
```json
{
  "success": true,
  "message": "OTP sent successfully",
  "data": null,
  "timestamp": "2025-09-24T06:13:54.159093"
}
```

#### 2. Verify OTP and Get JWT Token
```bash
curl -X POST http://localhost:9101/api/users/verify-otp \
  -H "Content-Type: application/json" \
  -d '{
    "phoneNumber": "+18777804236",
    "otp": "123456"
  }'
```

**Response (Existing User):**
```json
{
  "success": true,
  "message": "Login successful",
  "data": {
    "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
    "tokenType": "Bearer",
    "expiresIn": 3600,
    "user": { /* user details */ },
    "message": "Login successful"
  }
}
```

**Response (New User):**
```json
{
  "success": true,
  "message": "User registered and authenticated successfully",
  "data": {
    "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
    "tokenType": "Bearer",
    "expiresIn": 3600,
    "user": { /* user details */ },
    "message": "User registered and authenticated successfully"
  }
}
```

#### 3. Use JWT Token for Authenticated Requests
```bash
curl -X POST http://localhost:9101/api/messages/send \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiJ9..." \
  -H "Content-Type: application/json" \
  -d '{
    "content": "Hello from SMS authentication!",
    "messageType": "TEXT",
    "receiverId": 4
  }'
```

### Firebase Token Authentication (Legacy)

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

### Update Device Token
```bash
curl -X PUT http://localhost:9101/api/users/device-token \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "deviceToken": "fcm_device_token_here"
  }'
```

**Required Fields:**
- `deviceToken`: String (FCM device token for push notifications)

**Response:**
```json
{
  "success": true,
  "message": "Device token updated successfully",
  "data": {
    "id": 1,
    "phoneNumber": "+1234567890",
    "name": "John Doe",
    "avatarUrl": null,
    "about": null,
    "lastSeen": "2025-09-24T06:13:54.159093",
    "isOnline": true
  },
  "timestamp": "2025-09-24T06:13:54.159093"
}
```

**Error Responses:**

#### 400 Bad Request - Missing Device Token
```json
{
  "success": false,
  "message": "Validation failed",
  "errors": [
    {
      "field": "deviceToken",
      "message": "Device token is required"
    }
  ],
  "timestamp": "2025-09-24T06:13:54.159093"
}
```

#### 401 Unauthorized - Invalid Token
```json
{
  "success": false,
  "message": "Invalid or expired token",
  "timestamp": "2025-09-24T06:13:54.159093"
}
```

#### 404 Not Found - User Not Found
```json
{
  "success": false,
  "message": "User not found",
  "timestamp": "2025-09-24T06:13:54.159093"
}
```

#### 500 Internal Server Error
```json
{
  "success": false,
  "message": "Internal server error",
  "timestamp": "2025-09-24T06:13:54.159093"
}
```

**Frontend Integration Examples:**

#### JavaScript/TypeScript
```javascript
class DeviceTokenService {
  async updateDeviceToken(deviceToken) {
    const response = await fetch('/api/users/device-token', {
      method: 'PUT',
      headers: { 
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${localStorage.getItem('authToken')}`
      },
      body: JSON.stringify({ deviceToken })
    });
    return await response.json();
  }
}

// Usage
const deviceTokenService = new DeviceTokenService();
await deviceTokenService.updateDeviceToken('fcm_device_token_here');
```

#### Android (Kotlin)
```kotlin
class DeviceTokenManager {
    private val apiService = RetrofitClient.getApiService()
    
    suspend fun updateDeviceToken(deviceToken: String): ApiResponse<UserResponse> {
        val request = DeviceTokenUpdateRequest(deviceToken)
        return apiService.updateDeviceToken(request)
    }
}

// Usage
val deviceTokenManager = DeviceTokenManager()
deviceTokenManager.updateDeviceToken("fcm_device_token_here")
```

#### iOS (Swift)
```swift
class DeviceTokenService {
    private let baseURL = "http://localhost:9101"
    
    func updateDeviceToken(_ deviceToken: String) async throws -> ApiResponse<UserResponse> {
        let url = URL(string: "\(baseURL)/api/users/device-token")!
        var request = URLRequest(url: url)
        request.httpMethod = "PUT"
        request.setValue("application/json", forHTTPHeaderField: "Content-Type")
        request.setValue("Bearer \(authToken)", forHTTPHeaderField: "Authorization")
        
        let body = ["deviceToken": deviceToken]
        request.httpBody = try JSONSerialization.data(withJSONObject: body)
        
        let (data, _) = try await URLSession.shared.data(for: request)
        return try JSONDecoder().decode(ApiResponse<UserResponse>.self, from: data)
    }
}

// Usage
let deviceTokenService = DeviceTokenService()
try await deviceTokenService.updateDeviceToken("fcm_device_token_here")
```

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

### Check Single Phone Number Exists
```bash
curl -X GET "http://localhost:9101/api/users/check-phone/+1234567890" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

**Response (User Found):**
```json
{
  "success": true,
  "message": "User found with this phone number",
  "data": {
    "phoneNumber": "+1234567890",
    "exists": true,
    "user": {
      "id": 123,
      "phoneNumber": "+1234567890",
      "name": "John Doe",
      "avatarUrl": "https://example.com/avatar.jpg",
      "about": "Hey there! I'm using ChitChat.",
      "lastSeen": "2025-09-30T10:30:00",
      "isOnline": true,
      "createdAt": "2025-09-20T08:00:00"
    },
    "message": "User found with this phone number"
  }
}
```

**Response (User Not Found):**
```json
{
  "success": true,
  "message": "No user found with this phone number",
  "data": {
    "phoneNumber": "+1234567890",
    "exists": false,
    "user": null,
    "message": "No user found with this phone number"
  }
}
```

### Check Multiple Phone Numbers (Batch)
```bash
curl -X POST http://localhost:9101/api/users/check-phones \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "phoneNumbers": [
      "+1234567890",
      "+1987654321",
      "+1555555555"
    ]
  }'
```

**Request Body:**
```json
{
  "phoneNumbers": [
    "+1234567890",
    "+1987654321",
    "+1555555555"
  ]
}
```

**Response:**
```json
{
  "success": true,
  "message": "Checked 3 phone numbers: 2 found, 1 not found",
  "data": {
    "results": [
      {
        "phoneNumber": "+1234567890",
        "exists": true,
        "user": {
          "id": 123,
          "phoneNumber": "+1234567890",
          "name": "John Doe",
          "avatarUrl": "https://example.com/avatar.jpg",
          "about": "Hey there! I'm using ChitChat.",
          "lastSeen": "2025-09-30T10:30:00",
          "isOnline": true,
          "createdAt": "2025-09-20T08:00:00"
        },
        "message": "User found with this phone number"
      },
      {
        "phoneNumber": "+1987654321",
        "exists": true,
        "user": {
          "id": 456,
          "phoneNumber": "+1987654321",
          "name": "Jane Smith",
          "avatarUrl": "https://example.com/avatar2.jpg",
          "about": "Available",
          "lastSeen": "2025-09-30T09:15:00",
          "isOnline": false,
          "createdAt": "2025-09-15T12:00:00"
        },
        "message": "User found with this phone number"
      },
      {
        "phoneNumber": "+1555555555",
        "exists": false,
        "user": null,
        "message": "No user found with this phone number"
      }
    ],
    "totalChecked": 3,
    "foundCount": 2,
    "notFoundCount": 1,
    "message": "Checked 3 phone numbers: 2 found, 1 not found"
  }
}
```

**Features:**
- Efficient batch processing with a single database query
- Automatically removes duplicates from the input list
- Cleans and validates phone numbers
- Returns detailed status for each phone number
- Includes summary statistics (total checked, found count, not found count)

**Use Cases:**
- Contact sync - check which contacts are registered users
- Group creation - validate multiple phone numbers at once
- Bulk user lookup for messaging features

---

## 2. Messaging Service APIs (`/api/messages`)

### Understanding REST API vs WebSocket for Messaging

**Important:** The messaging system uses BOTH REST API and WebSocket - they work together, not as alternatives.

#### When to Use REST API (HTTP Endpoints)
Use REST API endpoints for:
- ✅ **Fetching conversation history** - Load past messages when opening a chat
- ✅ **Sending messages** - Send messages (fallback when WebSocket is unavailable)
- ✅ **Searching messages** - Search through message content
- ✅ **Managing messages** - Delete, mark as read, edit messages
- ✅ **Group management** - Create groups, add/remove members
- ✅ **Initial data loading** - Pagination, filtering, sorting

**Example Flow:**
1. User opens chat with John → Use REST API to fetch last 50 messages
2. User scrolls up → Use REST API to load older messages (pagination)
3. User searches "hello" → Use REST API to search messages

#### When to Use WebSocket (Real-time)
Use WebSocket for:
- ✅ **Receiving new messages instantly** - Get real-time message delivery
- ✅ **Typing indicators** - Show when someone is typing
- ✅ **Online/offline status** - Real-time presence updates
- ✅ **Read receipts** - Instant message read confirmations
- ✅ **Live updates** - Message edits, deletions in real-time

**Example Flow:**
1. User is in chat → WebSocket sends typing indicator to other person
2. John sends message → WebSocket delivers message instantly to user
3. User reads message → WebSocket sends read receipt to John

#### Recommended Implementation Pattern

```javascript
// STEP 1: Connect WebSocket first (for real-time updates)
const ws = new WebSocket('ws://localhost:9101/ws/messages');

ws.onopen = () => {
    // Authenticate WebSocket
    ws.send(JSON.stringify({ type: 'AUTH', token: jwtToken }));
};

// STEP 2: Load conversation history via REST API
async function openChat(receiverId) {
    // Fetch last 50 messages - includes BOTH sent and received messages
    // receiverId = the other user in the conversation
    const response = await fetch(`/api/messages/conversation/${receiverId}?size=50`, {
        headers: { 'Authorization': `Bearer ${jwtToken}` }
    });
    const data = await response.json();
    
    // data.data.content contains bidirectional messages
    const messages = data.data.content.map(msg => ({
        ...msg,
        isSentByMe: msg.senderId === currentUserId,    // You are the sender
        isSentByOther: msg.senderId === receiverId     // Other user is the sender
    }));
    
    displayMessages(messages);
}

// STEP 3: Listen for new messages via WebSocket
ws.onmessage = (event) => {
    const data = JSON.parse(event.data);
    if (data.type === 'NEW_MESSAGE') {
        addMessageToChat(data.message); // Real-time update
    }
};

// STEP 4: Send message via REST API (or WebSocket)
async function sendMessage(content, receiverId) {
    // Option A: Via REST API (more reliable, has retry logic)
    await fetch('/api/messages/send', {
        method: 'POST',
        headers: { 
            'Authorization': `Bearer ${jwtToken}`,
            'Content-Type': 'application/json' 
        },
        body: JSON.stringify({ content, receiverId, messageType: 'TEXT' })
    });
    
    // Option B: Via WebSocket (faster, but needs fallback)
    // ws.send(JSON.stringify({ type: 'SEND_MESSAGE', data: { content, receiverId } }));
}
```

#### Summary Table

| Task | Use REST API | Use WebSocket | Why |
|------|-------------|---------------|-----|
| Load chat history | ✅ | ❌ | Pagination, filtering needed |
| Send message | ✅ | ✅ | Both work, REST is more reliable |
| Receive new message | ❌ | ✅ | Real-time delivery required |
| Search messages | ✅ | ❌ | Complex query operations |
| Typing indicator | ❌ | ✅ | Real-time status update |
| Mark as read | ✅ | ✅ | Both work, REST for bulk operations |
| Delete message | ✅ | ❌ | Action requires confirmation |
| Online status | ❌ | ✅ | Real-time presence updates |

**Best Practice:** Use REST API for all data operations and management. Use WebSocket only for real-time updates and notifications.

---

### Complete Chat Implementation Example

Here's a complete example showing how to handle bidirectional conversations correctly:

```javascript
class ChatManager {
    constructor(currentUserId, jwtToken) {
        this.currentUserId = currentUserId;
        this.jwtToken = jwtToken;
        this.ws = null;
    }

    // Step 1: Initialize WebSocket connection
    connectWebSocket() {
        this.ws = new WebSocket('ws://localhost:9101/ws/messages');
        
        this.ws.onopen = () => {
            this.ws.send(JSON.stringify({ 
                type: 'AUTH', 
                token: this.jwtToken 
            }));
        };
        
        this.ws.onmessage = (event) => {
            const data = JSON.parse(event.data);
            if (data.type === 'NEW_MESSAGE') {
                this.handleNewMessage(data.message);
            }
        };
    }

    // Step 2: Load conversation history (bidirectional)
    async loadConversation(receiverId) {
        const response = await fetch(
            `/api/messages/conversation/${receiverId}?size=50&sort=timestamp,asc`, 
            {
                headers: { 'Authorization': `Bearer ${this.jwtToken}` }
            }
        );
        
        const data = await response.json();
        const messages = data.data.content;
        
        // Render messages with proper alignment
        messages.forEach(msg => {
            const isSentByMe = msg.senderId === this.currentUserId;  // You sent it
            this.displayMessage(msg, isSentByMe);
        });
    }

    // Step 3: Display message with correct alignment
    displayMessage(message, isSentByMe) {
        const messageDiv = document.createElement('div');
        messageDiv.className = isSentByMe ? 'message-sent' : 'message-received';
        messageDiv.innerHTML = `
            <div class="message-content">${message.content}</div>
            <div class="message-time">${message.timestamp}</div>
            ${isSentByMe ? `<div class="message-status">${message.status}</div>` : ''}
        `;
        document.getElementById('chat-container').appendChild(messageDiv);
    }

    // Step 4: Send message
    async sendMessage(receiverId, content) {
        const response = await fetch('/api/messages/send', {
            method: 'POST',
            headers: { 
                'Authorization': `Bearer ${this.jwtToken}`,
                'Content-Type': 'application/json' 
            },
            body: JSON.stringify({
                receiverId: receiverId,
                content: content,
                messageType: 'TEXT'
            })
        });
        
        const data = await response.json();
        // Message will also arrive via WebSocket for real-time update
        return data;
    }

    // Step 5: Handle new incoming message from WebSocket
    handleNewMessage(message) {
        const isSentByMe = message.senderId === this.currentUserId;
        this.displayMessage(message, isSentByMe);
    }
}

// Usage:
const chat = new ChatManager(currentUserId, jwtToken);
chat.connectWebSocket();
chat.loadConversation(receiverId);  // receiverId = the other user in the conversation
```

**Key Points:**
1. **Conversation endpoint returns BOTH directions** - sent and received messages
2. **Use `senderId` to determine message alignment** - compare with current user ID
3. **WebSocket for real-time** - new messages appear instantly
4. **REST API for history** - load past messages with pagination
5. **Proper UI rendering** - align sent messages to right, received messages to left

---

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

**How it works:**
- Returns **bidirectional conversation** between the authenticated user (sender) and receiver with ID `123`
- Includes messages sent BY you (senderId) TO receiver (receiverId = 123)
- Includes messages sent BY receiver (senderId = 123) TO you (receiverId)
- Sorted by timestamp (newest first by default)
- Supports pagination (default: 50 messages per page)

**Request Parameters:**
- `receiverId` (path): The ID of the other user in the conversation
- `page` (query, optional): Page number (default: 0)
- `size` (query, optional): Page size (default: 50)
- `sort` (query, optional): Sort order (default: timestamp,desc)

**Response Example:**
```json
{
  "success": true,
  "message": "Conversation messages retrieved successfully",
  "data": {
    "content": [
      {
        "id": "msg123",
        "senderId": 456,
        "recipientId": 123,
        "content": "Hello!",
        "messageType": "TEXT",
        "status": "READ",
        "timestamp": "2025-09-30T10:30:00",
        "readAt": "2025-09-30T10:31:00"
      },
      {
        "id": "msg124",
        "senderId": 123,
        "recipientId": 456,
        "content": "Hi, how are you?",
        "messageType": "TEXT",
        "status": "DELIVERED",
        "timestamp": "2025-09-30T10:29:00",
        "readAt": null
      }
    ],
    "totalElements": 45,
    "totalPages": 3,
    "currentPage": 0,
    "pageSize": 20
  }
}
```

**Important Notes:**
- This endpoint returns the **complete conversation** (both sides)
- Use `senderId` field to distinguish who sent each message
- If `senderId` matches your user ID → You sent this message
- If `senderId` matches the other user's ID → They sent this message
- Messages are typically sorted by timestamp in descending order (newest first)

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

### Send Notification by Phone Number
```bash
curl -X POST http://localhost:9101/api/notifications/send-by-phone \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "phoneNumber": "+1234567890",
    "title": "New Message",
    "body": "You have a new message",
    "type": "MESSAGE",
    "imageUrl": "https://example.com/image.jpg",
    "actionUrl": "/messages/123",
    "data": {
      "messageId": "789",
      "senderId": "456"
    }
  }'
```

**Features:**
- Send notifications using phone number instead of user ID
- Automatically looks up user by phone number
- Uses the registered device tokens for that user
- Supports all notification types: `MESSAGE`, `CALL`, `STATUS_UPDATE`, `FRIEND_REQUEST`, `GROUP_INVITE`, `SYSTEM`

**Use Cases:**
- Send notification when you only know the phone number
- Welcome notifications for new users
- OTP-related notifications
- Emergency or security alerts

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

API endpoints are rate-limited to prevent abuse and ensure fair usage:

| Endpoint Type | Limit | Window | Reason |
|---------------|-------|--------|--------|
| SMS OTP Sending | 5 requests | 1 minute | Prevent SMS spam and reduce costs |
| SMS OTP Verification | 10 requests | 1 minute | Prevent brute force attacks |
| Firebase Authentication | 10 requests | 1 minute | Prevent abuse of Firebase tokens |
| Messaging | 100 requests | 1 minute | Allow normal chat usage |
| Media Upload | 10 requests | 1 minute | Prevent storage abuse |
| Other APIs | 60 requests | 1 minute | General API protection |

### SMS Rate Limiting Details

**OTP Sending (`/api/users/send-otp`):**
- **Limit**: 5 requests per minute per IP address
- **Purpose**: Prevent SMS spam and reduce Twilio costs
- **Implementation**: Redis-based rate limiting with sliding window
- **Error Response**: `429 Too Many Requests` with retry-after header

**OTP Verification (`/api/users/verify-otp`):**
- **Limit**: 10 requests per minute per phone number
- **Purpose**: Prevent brute force OTP attacks
- **Implementation**: Phone number-based rate limiting
- **Security**: OTP is cleared after successful verification

**Redis OTP Storage:**
- **Expiration**: 5 minutes (300 seconds)
- **Key Format**: `otp:{phoneNumber}`
- **Security**: OTP is automatically cleared after verification
- **Storage**: Redis with TTL for automatic cleanup

**Notes:** 
- SMS OTP endpoints have stricter rate limits to prevent abuse and reduce SMS costs
- OTP codes expire after 5 minutes for security
- Rate limiting is implemented using Redis for distributed systems
- Firebase handles OTP rate limiting on their end for legacy authentication

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

**Important Update:** The authentication flow has been updated to support both SMS-based authentication (primary) and Firebase token-based authentication (legacy). 

### What's New:
- ✅ **Added:** `/api/users/send-otp` endpoint for SMS OTP sending
- ✅ **Added:** `/api/users/verify-otp` endpoint for SMS OTP verification
- ✅ **Integrated:** Twilio SMS service for OTP delivery
- ✅ **Enhanced:** Redis-based OTP storage with 5-minute expiration
- ✅ **Maintained:** Firebase token authentication for backward compatibility

### SMS Authentication Benefits:
1. **No Frontend Dependencies:** No need for Firebase SDK integration
2. **Simpler Implementation:** Direct API calls for OTP send/verify
3. **Better Control:** Custom OTP generation and validation
4. **Cost Effective:** Twilio provides competitive SMS rates
5. **Reliable Delivery:** Twilio's global SMS infrastructure

### Migration Guide:
1. **New Projects:** Use SMS-based authentication (`/api/users/send-otp` → `/api/users/verify-otp`)
2. **Existing Projects:** Continue using Firebase authentication or migrate to SMS
3. **Frontend:** Implement simple HTTP calls for OTP flow
4. **Security:** OTP codes expire in 5 minutes, rate-limited to prevent abuse

### Configuration Required:

The Twilio SMS service is configured through the database-driven configuration system. All settings are stored securely in the `application_config` table.


#### Environment Variables (Fallback)
```yaml
# application.yml fallback configuration
twilio:
  account:
    sid: ${TWILIO_ACCOUNT_SID:}  # Fallback only
  auth:
    token: ${TWILIO_AUTH_TOKEN:}  # Fallback only
  phone:
    number: ${TWILIO_PHONE_NUMBER:}  # Fallback only
```

#### Twilio Service Features

**SMS Services Available:**
1. **OTP SMS**: Sends verification codes with custom message template
2. **Welcome SMS**: Sends welcome message to new users
3. **Notification SMS**: Sends general notifications

**Message Templates:**
- **OTP Message**: "Your ChitChat verification code is: {OTP}. This code will expire in 5 minutes."
- **Welcome Message**: "Welcome to ChitChat, {USER_NAME}! Your account has been created successfully. Start chatting with your friends!"

**Security Features:**
- All credentials are encrypted in database
- SMS delivery tracking with Twilio Message SID
- Automatic error handling and logging
- Rate limiting to prevent abuse

---

## OTP and Twilio Implementation Details

### OTP Service Architecture

The OTP system is built using a secure, scalable architecture:

**Components:**
- **OtpService**: Interface for OTP operations
- **OtpServiceImpl**: Redis-based implementation with SecureRandom
- **TwilioService**: SMS delivery service
- **TwilioServiceImpl**: Twilio API integration

**OTP Generation Process:**
```java
// Secure 6-digit OTP generation
String otp = String.format("%06d", secureRandom.nextInt(1000000));

// Redis storage with 5-minute TTL
String key = "otp:" + phoneNumber;
redisTemplate.opsForValue().set(key, otp, Duration.ofMinutes(5));
```

**OTP Verification Process:**
```java
// Retrieve and verify OTP
String storedOtp = redisTemplate.opsForValue().get("otp:" + phoneNumber);
boolean isValid = storedOtp != null && storedOtp.equals(providedOtp);

// Clear OTP after successful verification
if (isValid) {
    redisTemplate.delete("otp:" + phoneNumber);
}
```

### Twilio SMS Service

**Service Implementation:**
- **TwilioService**: Interface for SMS operations
- **TwilioServiceImpl**: Twilio REST API integration
- **ConfigurationService**: Database-driven configuration management

**SMS Message Templates:**

1. **OTP SMS Template:**
```
Your ChitChat verification code is: {OTP}. This code will expire in 5 minutes.
```

2. **Welcome SMS Template:**
```
Welcome to ChitChat, {USER_NAME}! Your account has been created successfully. Start chatting with your friends!
```

3. **Notification SMS Template:**
```
{NOTIFICATION_MESSAGE}
```

**Twilio Integration Features:**
- **Message Tracking**: Each SMS gets a unique Twilio Message SID
- **Error Handling**: Comprehensive error handling with logging
- **Delivery Confirmation**: Twilio provides delivery status
- **Cost Optimization**: Rate limiting to prevent unnecessary SMS costs

### Redis Configuration

**OTP Storage Configuration:**
```yaml
spring:
  redis:
    host: localhost
    port: 6379
    timeout: 2000ms
```

**Key Naming Convention:**
- **OTP Keys**: `otp:{phoneNumber}` (e.g., `otp:+1234567890`)
- **Rate Limiting Keys**: `rate_limit:{endpoint}:{identifier}`
- **TTL**: 5 minutes for OTP, 1 minute for rate limiting

### Security Features

**OTP Security:**
- **SecureRandom**: Cryptographically secure random number generation
- **Time-based Expiration**: 5-minute automatic expiration
- **Single Use**: OTP is cleared after successful verification
- **Rate Limiting**: Prevents brute force attacks

**Twilio Security:**
- **Encrypted Credentials**: All Twilio credentials encrypted in database
- **Message Validation**: Phone number format validation
- **Error Logging**: Comprehensive logging without exposing sensitive data
- **Delivery Tracking**: Message SID tracking for audit trails

### Error Handling

**Common Error Scenarios:**

1. **Twilio Service Unavailable:**
```json
{
  "success": false,
  "message": "SMS service temporarily unavailable. Please try again later.",
  "timestamp": "2025-09-24T06:13:54.159093"
}
```

2. **Invalid Phone Number:**
```json
{
  "success": false,
  "message": "Invalid phone number format. Please use international format (+1234567890).",
  "timestamp": "2025-09-24T06:13:54.159093"
}
```

3. **Redis Connection Failed:**
```json
{
  "success": false,
  "message": "OTP service temporarily unavailable. Please try again later.",
  "timestamp": "2025-09-24T06:13:54.159093"
}
```

### Monitoring and Logging

**Key Metrics to Monitor:**
- **OTP Generation Rate**: Track OTP requests per minute
- **SMS Delivery Success Rate**: Monitor Twilio delivery success
- **OTP Verification Success Rate**: Track successful verifications
- **Rate Limit Hits**: Monitor rate limiting effectiveness

**Log Messages:**
```
INFO  - OTP generated and stored for phone number: +1234567890
INFO  - OTP SMS sent successfully. Message SID: SM1234567890abcdef
INFO  - OTP verification successful for phone number: +1234567890
WARN  - OTP verification failed for phone number: +1234567890
ERROR - Failed to send OTP SMS to phone number: +1234567890
```

---

*Last updated: September 24, 2025 - Added SMS-Based Authentication with Twilio Integration*