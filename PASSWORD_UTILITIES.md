# BCrypt Password Utilities

This document provides tools and methods to work with BCrypt encrypted passwords in the ChitChat backend project.

## 🎯 Target Hash Analysis

**Hash:** `$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVEFDi`

### Hash Structure
- **Algorithm:** BCrypt
- **Version:** 2a (current standard)
- **Rounds:** 10 (2^10 = 1,024 iterations)
- **Salt:** `N.zmdr9k7uOCQb376NoUnu`
- **Hash:** `TJ8iAt6Z5EHsM8lE9lBOsl7iKTVEFDi`
- **Security Level:** Medium (10 rounds is moderate protection)

---

## 🛠️ Available Tools

### 1. Spring Boot API Endpoints

#### Verify Specific Password
```bash
curl -X POST http://localhost:9101/api/users/admin/password/verify \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "hashedPassword": "$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVEFDi",
    "plainPassword": "password123"
  }'
```

#### Test Common Passwords
```bash
curl -X POST http://localhost:9101/api/users/admin/password/verify \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "hashedPassword": "$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVEFDi"
  }'
```

#### Get Hash Information
```bash
curl -X POST http://localhost:9101/api/users/admin/password/info \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "hashedPassword": "$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVEFDi"
  }'
```

### 2. Java Utility Class

**Location:** `chitchat-user-service/src/main/java/com/chitchat/user/util/PasswordUtil.java`

#### Usage Examples

```java
// Verify a password
boolean matches = PasswordUtil.verifyPassword("password123", hashedPassword);

// Encode a new password
String encoded = PasswordUtil.encodePassword("newPassword");

// Check if string is valid BCrypt hash
boolean isValid = PasswordUtil.isBCryptHash(someString);

// Get hash information
PasswordUtil.BCryptInfo info = PasswordUtil.getBCryptInfo(hashedPassword);

// Test common passwords
PasswordUtil.PasswordTestResult result = PasswordUtil.testCommonPasswords(hashedPassword);
```

### 3. Standalone Java Tool

**Location:** `SimplePasswordTester.java`

```bash
# Compile and run
javac SimplePasswordTester.java
java SimplePasswordTester

# Run with custom hash
java SimplePasswordTester "$2a$10$YOUR_HASH_HERE"
```

---

## 🔍 Common Passwords to Test

### Basic Passwords
- `password`
- `123456`
- `password123`
- `admin`
- `qwerty`
- `letmein`
- `welcome`
- `monkey`
- `1234567890`
- `abc123`

### Variations
- `Password1`
- `password1`
- `admin123`
- `root`
- `user`
- `test`
- `guest`
- `demo`
- `sample`
- `default`

### Application Specific
- `chitchat`
- `chitchat123`
- `ChitChat`
- `ChitChat123`
- `mobile`
- `mobile123`
- `app`
- `app123`
- `backend`

### Admin Passwords
- `secret`
- `Secret123`
- `pass`
- `pass123`
- `login`
- `Login123`
- `temp`
- `temp123`
- `new`
- `new123`

---

## 📋 Step-by-Step Testing Guide

### Method 1: Using Spring Boot Application

1. **Start the application:**
   ```bash
   cd chitchat-backend
   mvn spring-boot:run -pl chitchat-user-service
   ```

2. **Get an authentication token** (or use admin bypass)

3. **Test common passwords:**
   ```bash
   curl -X POST http://localhost:9102/api/users/admin/password/verify \
     -H "Content-Type: application/json" \
     -d '{
       "hashedPassword": "$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVEFDi"
     }'
   ```

4. **Test specific password:**
   ```bash
   curl -X POST http://localhost:9102/api/users/admin/password/verify \
     -H "Content-Type: application/json" \
     -d '{
       "hashedPassword": "$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVEFDi",
       "plainPassword": "YOUR_GUESS"
     }'
   ```

### Method 2: Using Standalone Tool

1. **Compile the tool:**
   ```bash
   javac SimplePasswordTester.java
   ```

2. **Run hash analysis:**
   ```bash
   java SimplePasswordTester
   ```

3. **Follow the generated curl commands for actual verification**

### Method 3: Manual Testing with curl

Test each password individually:

```bash
# Test password123
curl -X POST http://localhost:9102/api/users/admin/password/verify \
  -H "Content-Type: application/json" \
  -d '{
    "hashedPassword": "$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVEFDi",
    "plainPassword": "password123"
  }'

# Test admin
curl -X POST http://localhost:9102/api/users/admin/password/verify \
  -H "Content-Type: application/json" \
  -d '{
    "hashedPassword": "$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVEFDi",
    "plainPassword": "admin"
  }'

# Test chitchat
curl -X POST http://localhost:9102/api/users/admin/password/verify \
  -H "Content-Type: application/json" \
  -d '{
    "hashedPassword": "$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVEFDi",
    "plainPassword": "chitchat"
  }'
```

---

## 📊 Expected Response Formats

### Successful Password Match
```json
{
  "success": true,
  "message": "Password verification completed",
  "data": {
    "hashedPassword": "$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVEFDi",
    "matchFound": true,
    "matchedPassword": "password123",
    "totalTested": 1
  },
  "timestamp": "2024-09-20T10:30:00Z"
}
```

### No Match Found
```json
{
  "success": true,
  "message": "Password verification completed",
  "data": {
    "hashedPassword": "$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVEFDi",
    "matchFound": false,
    "matchedPassword": null,
    "totalTested": 35
  },
  "timestamp": "2024-09-20T10:30:00Z"
}
```

### Hash Information
```json
{
  "success": true,
  "message": "Password info retrieved",
  "data": {
    "version": "2a",
    "rounds": 10,
    "salt": "N.zmdr9k7uOCQb376NoUnu",
    "hash": "TJ8iAt6Z5E...",
    "fullHash": "$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVEFDi"
  },
  "timestamp": "2024-09-20T10:30:00Z"
}
```

---

## 🔐 Security Considerations

### BCrypt Strength Analysis
- **Rounds 4-6:** Very weak (testing/development only)
- **Rounds 7-9:** Weak (not recommended)
- **Rounds 10-11:** Medium (acceptable for most applications)
- **Rounds 12-15:** Strong (recommended for sensitive data)
- **Rounds 16+:** Very strong (may impact performance)

### Current Hash Security
Your hash uses **10 rounds**, which provides:
- **1,024 iterations** (2^10)
- **Medium security level**
- **Moderate protection** against brute force attacks
- **Reasonable performance** for authentication

### Best Practices
1. **Never log plain text passwords**
2. **Use constant-time comparison** for verification
3. **Consider rate limiting** for password attempts
4. **Implement account lockout** after failed attempts
5. **Use strong salts** (BCrypt handles this automatically)

---

## 🚀 Quick Commands Summary

```bash
# Start user service
mvn spring-boot:run -pl chitchat-user-service

# Test common passwords
curl -X POST http://localhost:9102/api/users/admin/password/verify \
  -H "Content-Type: application/json" \
  -d '{"hashedPassword": "$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVEFDi"}'

# Test specific password
curl -X POST http://localhost:9102/api/users/admin/password/verify \
  -H "Content-Type: application/json" \
  -d '{"hashedPassword": "$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVEFDi", "plainPassword": "YOUR_GUESS"}'

# Get hash info
curl -X POST http://localhost:9102/api/users/admin/password/info \
  -H "Content-Type: application/json" \
  -d '{"hashedPassword": "$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVEFDi"}'

# Run standalone analyzer
javac SimplePasswordTester.java && java SimplePasswordTester
```

---

## 🔧 Troubleshooting

### Common Issues

1. **Service not starting:**
   - Check if port 9102 is available
   - Verify Java and Maven versions
   - Check application.yml configuration

2. **Authentication errors:**
   - Admin endpoints may require valid JWT token
   - Check if admin bypass is enabled for testing

3. **Compilation errors:**
   - Ensure Spring Boot dependencies are available
   - Use Maven for proper dependency management

4. **Connection refused:**
   - Verify service is running on correct port
   - Check if API Gateway is routing correctly

### Debug Commands

```bash
# Check if service is running
curl http://localhost:9102/actuator/health

# Check available endpoints
curl http://localhost:9102/actuator/mappings

# View service logs
mvn spring-boot:run -pl chitchat-user-service -Dspring-boot.run.profiles=debug
```

---

*Created: September 20, 2024*
*Hash: $2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVEFDi*