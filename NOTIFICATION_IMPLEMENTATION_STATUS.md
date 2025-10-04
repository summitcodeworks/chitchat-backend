# ChitChat Notification Implementation Status

## 📊 Current State

### ✅ What's Already Built

1. **Notification Service** (`chitchat-notification-service`)
   - ✅ Firebase Cloud Messaging integration
   - ✅ Device token registration/unregistration
   - ✅ Push notification sending via FCM
   - ✅ Notification history tracking
   - ✅ Read/unread status management
   - ✅ Bulk notifications support

2. **Messaging Service** (`chitchat-messaging-service`)
   - ✅ Message sending/receiving
   - ✅ MongoDB storage
   - ✅ Kafka event publishing
   - ✅ WebSocket real-time delivery
   - ❌ **NOT integrated with notifications yet**

3. **API Endpoints Available**
   - `POST /api/notifications/register` - Register device token
   - `DELETE /api/notifications/unregister` - Unregister device token
   - `POST /api/notifications/send` - Send notification manually
   - `POST /api/notifications/send-by-phone` - Send to phone number
   - `GET /api/notifications/user` - Get user notifications
   - `PUT /api/notifications/{id}/read` - Mark as read

---

## ❌ What's Missing

### 1. Automatic Message Notifications

**Current:** Messages are sent but NO automatic push notifications

**Needed:** When User A sends a message to User B:
1. Save message to MongoDB ✅
2. Publish to Kafka ✅
3. Send via WebSocket ✅
4. **❌ Send push notification to User B** ← MISSING

### 2. Integration Code

Need to add notification call in `MessagingServiceImpl.java`:

```java
@Override
@Transactional
public MessageResponse sendMessage(Long senderId, SendMessageRequest request) {
    // ... existing code ...
    
    message = messageRepository.save(message);
    
    // Publish message event to Kafka
    publishMessageEvent(message);
    
    // ❌ ADD THIS: Send push notification to recipient
    notificationClient.sendMessageNotification(
        request.getRecipientId(),
        getSenderName(senderId),
        request.getContent(),
        senderId,
        message.getId()
    );
    
    return mapToMessageResponse(message);
}
```

---

## 🔧 To Complete Integration

### Backend Changes Needed

**File: `chitchat-messaging-service/src/main/java/com/chitchat/messaging/service/impl/MessagingServiceImpl.java`**

1. Add dependency injection:
```java
private final NotificationServiceClient notificationClient;
private final UserServiceClient userServiceClient; // To get sender name
```

2. Update `sendMessage()` method to call notification service

3. Add `RestTemplate` bean in config

**File: `chitchat-messaging-service/src/main/java/com/chitchat/messaging/config/RestTemplateConfig.java`**

```java
@Configuration
public class RestTemplateConfig {
    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}
```

### Client App Changes Needed

See `REACT_NATIVE_PUSH_NOTIFICATIONS_GUIDE.md` for complete implementation:

1. Install dependencies:
   ```bash
   npm install @react-native-firebase/app
   npm install @react-native-firebase/messaging
   npm install @notifee/react-native
   ```

2. Setup Firebase project (iOS & Android)

3. Implement `NotificationService.js`

4. Register device token after login

5. Handle notifications in foreground/background

---

## 📱 React Native Implementation Summary

### Key Files to Create

1. **`src/services/NotificationService.js`**
   - Request notification permission
   - Get FCM token
   - Register with backend
   - Handle incoming notifications
   - Navigate to screens on tap

2. **Update `App.js`**
   ```javascript
   useEffect(() => {
     NotificationService.initialize(navigationRef.current);
   }, []);
   ```

3. **Update `LoginScreen.js`**
   ```javascript
   // After successful login
   await NotificationService.registerDeviceToken(user.id);
   ```

4. **Update `LogoutScreen.js`**
   ```javascript
   // Before logout
   await NotificationService.unregisterDeviceToken();
   ```

---

## 🎯 Implementation Steps

### Phase 1: Backend Integration (30 minutes)

1. ✅ Create `NotificationServiceClient.java` (DONE)
2. ⏳ Add `RestTemplate` configuration
3. ⏳ Inject client into `MessagingServiceImpl`
4. ⏳ Call notification on message send
5. ⏳ Test with Postman

### Phase 2: React Native Setup (1-2 hours)

1. ⏳ Install Firebase dependencies
2. ⏳ Setup Firebase project (iOS & Android)
3. ⏳ Add `GoogleService-Info.plist` (iOS)
4. ⏳ Add `google-services.json` (Android)
5. ⏳ Build and run on device

### Phase 3: React Native Implementation (2-3 hours)

1. ⏳ Create `NotificationService.js`
2. ⏳ Request permissions on first launch
3. ⏳ Get FCM token
4. ⏳ Register device token with backend
5. ⏳ Setup notification listeners
6. ⏳ Handle foreground notifications
7. ⏳ Handle background/quit notifications
8. ⏳ Implement navigation on tap

### Phase 4: Testing (1 hour)

1. ⏳ Test foreground notifications
2. ⏳ Test background notifications
3. ⏳ Test killed app notifications
4. ⏳ Test notification tap navigation
5. ⏳ Test token refresh
6. ⏳ Test unregister on logout

---

## 📋 API Usage Examples

### 1. Register Device Token (After Login)

```javascript
// React Native
const response = await api.post('/api/notifications/register', {
  deviceToken: 'FCM_TOKEN_HERE',
  platform: 'ANDROID', // or 'IOS'
  deviceId: 'unique-device-id',
  deviceName: 'Samsung Galaxy S21'
}, {
  headers: {
    Authorization: `Bearer ${accessToken}`
  }
});
```

```bash
# cURL
curl -X POST http://65.1.185.194:9101/api/notifications/register \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "deviceToken": "FCM_TOKEN",
    "platform": "ANDROID",
    "deviceId": "device-123",
    "deviceName": "Android Device"
  }'
```

### 2. Send Message (Automatic Notification)

```bash
# After integration, this will automatically send notification
curl -X POST http://65.1.185.194:9101/api/messages/send \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "recipientId": 9,
    "content": "Hey, how are you?",
    "type": "TEXT"
  }'
```

**What happens:**
1. ✅ Message saved to MongoDB
2. ✅ Published to Kafka
3. ✅ Sent via WebSocket (if online)
4. ✅ Push notification sent (if offline or app in background)

### 3. Unregister Device Token (On Logout)

```javascript
// React Native
await api.delete('/api/notifications/unregister', {
  data: {deviceId: 'unique-device-id'},
  headers: {
    Authorization: `Bearer ${accessToken}`
  }
});
```

---

## 🔔 Notification Flow

### When User A Sends Message to User B

```
User A (Mobile App)
    |
    | POST /api/messages/send
    |
    v
API Gateway (9101)
    |
    | Route to Messaging Service
    |
    v
Messaging Service (9103)
    |
    ├─> Save to MongoDB
    ├─> Publish to Kafka
    ├─> Send via WebSocket (if User B online)
    └─> Call Notification Service ← ADD THIS
        |
        v
    Notification Service (9106)
        |
        ├─> Get User B's device tokens
        ├─> Save notification to DB
        └─> Send via Firebase FCM
            |
            v
        User B's Device
            |
            ├─> Foreground: Show local notification
            ├─> Background: Show system notification
            └─> Killed: Wake up and show notification
```

---

## 🎉 Expected Behavior After Implementation

### Scenario 1: User B App is in Foreground

1. Message arrives
2. Local notification shown (using Notifee)
3. Notification sound plays
4. User can tap to view message

### Scenario 2: User B App is in Background

1. System notification appears
2. Notification sound plays
3. Tap opens app and navigates to chat

### Scenario 3: User B App is Killed

1. System notification appears
2. Notification sound plays
3. Tap launches app and navigates to chat

### Scenario 4: User B has Multiple Devices

1. Notification sent to ALL registered devices
2. Each device receives push notification
3. First device to open marks as read
4. Other devices see "read" status

---

## 📊 Monitoring & Debugging

### Check if Device Token is Registered

```sql
SELECT * FROM device_tokens 
WHERE user_id = 8 AND is_active = true;
```

### Check Notification History

```sql
SELECT * FROM notifications 
WHERE user_id = 8 
ORDER BY created_at DESC 
LIMIT 10;
```

### Test Manual Notification

```bash
curl -X POST http://65.1.185.194:9101/api/notifications/send \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "userId": 8,
    "title": "Test",
    "body": "This is a test",
    "type": "SYSTEM"
  }'
```

---

## 🚨 Important Notes

1. **Firebase Setup Required**
   - Must have Firebase project created
   - Must download `google-services.json` (Android)
   - Must download `GoogleService-Info.plist` (iOS)
   - Must upload APNs certificate (iOS)

2. **Test on Real Devices**
   - Push notifications don't work on simulators/emulators
   - Need physical iOS/Android device

3. **Permissions**
   - Must request notification permission
   - Android 13+ requires runtime permission
   - iOS always requires permission

4. **Background Limitations**
   - iOS: Limited background processing
   - Android: May be killed by battery optimization
   - Use high-priority notifications

---

## 📚 Documentation Files

1. **`REACT_NATIVE_PUSH_NOTIFICATIONS_GUIDE.md`**
   - Complete React Native implementation
   - Code examples
   - Step-by-step guide

2. **`API_DOCUMENTATION.md`**
   - All API endpoints
   - Request/response examples

3. **`NOTIFICATION_IMPLEMENTATION_STATUS.md`** (this file)
   - Current state
   - What's missing
   - Implementation roadmap

---

## ✅ Quick Start Checklist

### Backend (15 minutes)
- [ ] Review `NotificationServiceClient.java` (already created)
- [ ] Add `RestTemplate` config
- [ ] Integrate with `MessagingServiceImpl`
- [ ] Test notification sending

### React Native (2-3 hours)
- [ ] Install Firebase dependencies
- [ ] Setup Firebase project
- [ ] Add Firebase config files
- [ ] Implement `NotificationService.js`
- [ ] Update App.js, Login, Logout
- [ ] Test on real device

---

**Status:** Ready for Implementation
**Priority:** High
**Estimated Time:** 3-4 hours total

**Created:** October 3, 2025

