# React Native Push Notifications Implementation Guide

## Overview

This guide shows you how to integrate Firebase Cloud Messaging (FCM) for push notifications in your React Native ChitChat app.

---

## 📦 Step 1: Install Dependencies

```bash
npm install @react-native-firebase/app
npm install @react-native-firebase/messaging
npm install @notifee/react-native  # For local notifications (Android)
```

---

## 🔥 Step 2: Firebase Setup

### iOS
1. Download `GoogleService-Info.plist` from Firebase Console
2. Add to your iOS project in Xcode
3. Run `cd ios && pod install`

### Android
1. Download `google-services.json` from Firebase Console
2. Place in `android/app/`
3. Update `android/build.gradle`:

```gradle
buildscript {
    dependencies {
        classpath 'com.google.gms:google-services:4.3.15'
    }
}
```

4. Update `android/app/build.gradle`:

```gradle
apply plugin: 'com.google.gms.google-services'
```

---

## 📱 Step 3: Request Permission & Get Device Token

Create `src/services/NotificationService.js`:

```javascript
import messaging from '@react-native-firebase/messaging';
import {Platform, PermissionsAndroid} from 'react';
import AsyncStorage from '@react-native-async-storage/async-storage';
import notifee, {AndroidImportance} from '@notifee/react-native';
import api from './api'; // Your axios instance

class NotificationService {
  constructor() {
    this.fcmToken = null;
  }

  /**
   * Request permission for notifications
   */
  async requestPermission() {
    try {
      if (Platform.OS === 'ios') {
        const authStatus = await messaging().requestPermission();
        const enabled =
          authStatus === messaging.AuthorizationStatus.AUTHORIZED ||
          authStatus === messaging.AuthorizationStatus.PROVISIONAL;

        if (enabled) {
          console.log('iOS Notification permission granted');
          return true;
        }
      } else {
        // Android 13+ requires permission
        if (Platform.Version >= 33) {
          const granted = await PermissionsAndroid.request(
            PermissionsAndroid.PERMISSIONS.POST_NOTIFICATIONS,
          );
          if (granted === PermissionsAndroid.RESULTS.GRANTED) {
            console.log('Android Notification permission granted');
            return true;
          }
        } else {
          return true; // Auto-granted on Android < 13
        }
      }
      return false;
    } catch (error) {
      console.error('Permission request error:', error);
      return false;
    }
  }

  /**
   * Get FCM device token
   */
  async getToken() {
    try {
      const fcmToken = await messaging().getToken();
      console.log('FCM Token:', fcmToken);
      this.fcmToken = fcmToken;
      await AsyncStorage.setItem('fcmToken', fcmToken);
      return fcmToken;
    } catch (error) {
      console.error('Get FCM Token error:', error);
      return null;
    }
  }

  /**
   * Register device token with backend
   */
  async registerDeviceToken(userId) {
    try {
      const token = this.fcmToken || (await this.getToken());
      if (!token) {
        console.warn('No FCM token available');
        return false;
      }

      // Call your backend API
      const response = await api.post('/api/notifications/register', {
        deviceToken: token,
        platform: Platform.OS === 'ios' ? 'IOS' : 'ANDROID',
        deviceId: token, // Use FCM token as device ID
        deviceName: Platform.OS === 'ios' ? 'iPhone' : 'Android Device',
      });

      console.log('Device token registered successfully');
      return true;
    } catch (error) {
      console.error('Register device token error:', error);
      return false;
    }
  }

  /**
   * Setup notification listeners
   */
  setupListeners(navigation) {
    // Handle notification when app is in FOREGROUND
    messaging().onMessage(async remoteMessage => {
      console.log('Foreground notification:', remoteMessage);
      await this.displayNotification(remoteMessage);
    });

    // Handle notification when app is in BACKGROUND/QUIT and user taps it
    messaging().onNotificationOpenedApp(remoteMessage => {
      console.log('Notification opened from background:', remoteMessage);
      this.handleNotificationNavigation(remoteMessage, navigation);
    });

    // Handle notification when app is QUIT and opened by tapping notification
    messaging()
      .getInitialNotification()
      .then(remoteMessage => {
        if (remoteMessage) {
          console.log('Notification opened from quit state:', remoteMessage);
          this.handleNotificationNavigation(remoteMessage, navigation);
        }
      });

    // Handle token refresh
    messaging().onTokenRefresh(async newToken => {
      console.log('FCM Token refreshed:', newToken);
      this.fcmToken = newToken;
      await AsyncStorage.setItem('fcmToken', newToken);
      // Re-register with backend
      const userId = await AsyncStorage.getItem('userId');
      if (userId) {
        await this.registerDeviceToken(parseInt(userId));
      }
    });
  }

  /**
   * Display notification when app is in foreground (using Notifee)
   */
  async displayNotification(remoteMessage) {
    try {
      // Create a channel (Android only)
      const channelId = await notifee.createChannel({
        id: 'chitchat-messages',
        name: 'ChitChat Messages',
        importance: AndroidImportance.HIGH,
        sound: 'default',
        vibration: true,
      });

      // Display the notification
      await notifee.displayNotification({
        title: remoteMessage.notification?.title || 'New Message',
        body: remoteMessage.notification?.body || '',
        data: remoteMessage.data,
        android: {
          channelId,
          smallIcon: 'ic_notification', // Add this icon to android/app/src/main/res/drawable
          pressAction: {
            id: 'default',
          },
          importance: AndroidImportance.HIGH,
        },
        ios: {
          sound: 'default',
        },
      });
    } catch (error) {
      console.error('Display notification error:', error);
    }
  }

  /**
   * Handle navigation when notification is tapped
   */
  handleNotificationNavigation(remoteMessage, navigation) {
    try {
      const {data} = remoteMessage;
      
      // Navigate based on notification type
      if (data?.type === 'MESSAGE') {
        const senderId = data.senderId;
        const messageId = data.messageId;
        
        // Navigate to chat screen with sender
        navigation.navigate('Chat', {
          userId: senderId,
          messageId: messageId,
        });
      } else if (data?.screen) {
        // Navigate to specific screen
        navigation.navigate(data.screen, data);
      }
    } catch (error) {
      console.error('Navigation error:', error);
    }
  }

  /**
   * Initialize notification service
   */
  async initialize(navigation) {
    try {
      // Request permission
      const hasPermission = await this.requestPermission();
      if (!hasPermission) {
        console.warn('Notification permission denied');
        return false;
      }

      // Get FCM token
      await this.getToken();

      // Setup listeners
      this.setupListeners(navigation);

      return true;
    } catch (error) {
      console.error('Notification initialization error:', error);
      return false;
    }
  }

  /**
   * Unregister device token (on logout)
   */
  async unregisterDeviceToken() {
    try {
      const token = await AsyncStorage.getItem('fcmToken');
      if (token) {
        await api.delete('/api/notifications/unregister', {
          data: {deviceId: token},
        });
        await AsyncStorage.removeItem('fcmToken');
        console.log('Device token unregistered');
      }
    } catch (error) {
      console.error('Unregister device token error:', error);
    }
  }
}

export default new NotificationService();
```

---

## 🚀 Step 4: Usage in Your App

### App.js / Main Navigator

```javascript
import React, {useEffect} from 'react';
import {NavigationContainer} from '@react-navigation/native';
import messaging from '@react-native-firebase/messaging';
import NotificationService from './src/services/NotificationService';

function App() {
  const navigationRef = React.useRef();

  useEffect(() => {
    // Request permission and setup notifications
    setupNotifications();

    // Background message handler
    messaging().setBackgroundMessageHandler(async remoteMessage => {
      console.log('Background message:', remoteMessage);
      // Message will be handled by OS
    });
  }, []);

  const setupNotifications = async () => {
    // Initialize notification service
    await NotificationService.initialize(navigationRef.current);
  };

  return (
    <NavigationContainer ref={navigationRef}>
      {/* Your app navigation */}
    </NavigationContainer>
  );
}

export default App();
```

### Login Screen (Register Device Token)

```javascript
import NotificationService from '../services/NotificationService';

const LoginScreen = () => {
  const handleLogin = async (phoneNumber, otp) => {
    try {
      // Login with OTP
      const response = await api.post('/api/users/verify-otp', {
        phoneNumber,
        otp,
      });

      const {accessToken, refreshToken, user} = response.data.data;

      // Save tokens
      await AsyncStorage.multiSet([
        ['accessToken', accessToken],
        ['refreshToken', refreshToken],
        ['userId', user.id.toString()],
      ]);

      // Register device token for push notifications
      await NotificationService.registerDeviceToken(user.id);

      // Navigate to home
      navigation.navigate('Home');
    } catch (error) {
      console.error('Login failed:', error);
    }
  };

  return (
    // Your login UI
  );
};
```

### Logout (Unregister Device Token)

```javascript
const handleLogout = async () => {
  try {
    // Unregister device token
    await NotificationService.unregisterDeviceToken();

    // Clear storage
    await AsyncStorage.multiRemove(['accessToken', 'refreshToken', 'userId']);

    // Navigate to login
    navigation.navigate('Login');
  } catch (error) {
    console.error('Logout failed:', error);
  }
};
```

---

## 📨 Step 5: Backend Integration

### Register Device Token API

```bash
POST http://65.1.185.194:9101/api/notifications/register
Authorization: Bearer YOUR_JWT_TOKEN
Content-Type: application/json

{
  "deviceToken": "FCM_DEVICE_TOKEN_HERE",
  "platform": "ANDROID",  // or "IOS"
  "deviceId": "unique-device-id",
  "deviceName": "Samsung Galaxy S21"
}
```

### Unregister Device Token API

```bash
DELETE http://65.1.185.194:9101/api/notifications/unregister
Authorization: Bearer YOUR_JWT_TOKEN
Content-Type: application/json

{
  "deviceId": "unique-device-id"
}
```

---

## 📱 Step 6: Test Notifications

### Test from Backend

```bash
# Send test notification
curl -X POST http://65.1.185.194:9101/api/notifications/send \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "userId": 8,
    "title": "Test Notification",
    "body": "This is a test message",
    "type": "MESSAGE"
  }'
```

### Test from Firebase Console

1. Go to Firebase Console → Cloud Messaging
2. Click "Send test message"
3. Enter your FCM token
4. Send notification

---

## 🔔 Notification Payload Structure

### From Backend (Firebase)

```json
{
  "notification": {
    "title": "John Doe",
    "body": "Hey, how are you?",
    "image": "https://example.com/avatar.jpg"
  },
  "data": {
    "type": "MESSAGE",
    "senderId": "123",
    "messageId": "msg456",
    "screen": "chat"
  }
}
```

### Handling in React Native

```javascript
// In setupListeners
messaging().onMessage(async remoteMessage => {
  const {notification, data} = remoteMessage;
  
  console.log('Title:', notification?.title);
  console.log('Body:', notification?.body);
  console.log('Data:', data);
  
  // Show local notification
  await NotificationService.displayNotification(remoteMessage);
});
```

---

## ✅ Testing Checklist

- [ ] Permission requested on app first launch
- [ ] FCM token generated successfully
- [ ] Device token registered with backend
- [ ] Notification received when app is in foreground
- [ ] Notification received when app is in background
- [ ] Notification received when app is killed
- [ ] Tapping notification navigates to correct screen
- [ ] Token refresh handled correctly
- [ ] Device token unregistered on logout

---

## 🐛 Troubleshooting

### iOS Not Receiving Notifications
1. Check if APNs certificate is uploaded to Firebase
2. Verify `GoogleService-Info.plist` is added
3. Enable Push Notifications capability in Xcode
4. Test on real device (not simulator)

### Android Not Receiving Notifications
1. Verify `google-services.json` is in `android/app/`
2. Check if Google Play Services is installed
3. Ensure internet permission in `AndroidManifest.xml`
4. Test with Firebase Console test message first

### Token Not Registering
1. Check network connection
2. Verify authentication token is valid
3. Check backend logs for registration errors
4. Ensure notification service is running

---

## 📚 Additional Resources

- [React Native Firebase Messaging Docs](https://rnfirebase.io/messaging/usage)
- [Notifee Documentation](https://notifee.app/react-native/docs/overview)
- [FCM Setup Guide](https://firebase.google.com/docs/cloud-messaging)

---

## 🎯 Next Steps

1. Install dependencies
2. Setup Firebase project
3. Implement NotificationService
4. Test on real devices
5. Monitor notification delivery rates

**Created:** October 3, 2025

