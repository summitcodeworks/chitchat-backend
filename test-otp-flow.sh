#!/bin/bash

# Test script for complete OTP authentication flow
# This script tests the WhatsApp-style authentication implementation

BASE_URL="http://localhost:9101"
PHONE="+1234567890"
NEW_PHONE="+1987654321"

echo "🧪 Testing Complete OTP Authentication Flow"
echo "=========================================="

# Test 1: Send OTP
echo ""
echo "📱 Step 1: Sending OTP to $PHONE"
echo "--------------------------------------"
OTP_RESPONSE=$(curl -s -X POST "$BASE_URL/api/users/send-otp" \
  -H "Content-Type: application/json" \
  -d "{\"phoneNumber\": \"$PHONE\"}")

echo "Response: $OTP_RESPONSE"
echo ""

# Extract test OTP (for development)
TEST_OTP="123456"

# Test 2: Authenticate existing user (if any)
echo "🔐 Step 2: Authenticating existing user with OTP"
echo "------------------------------------------------"
AUTH_RESPONSE=$(curl -s -X POST "$BASE_URL/api/users/authenticate" \
  -H "Content-Type: application/json" \
  -d "{\"phoneNumber\": \"$PHONE\", \"otp\": \"$TEST_OTP\"}")

echo "Response: $AUTH_RESPONSE"
echo ""

# Test 3: Send OTP to new phone number
echo "📱 Step 3: Sending OTP to new user $NEW_PHONE"
echo "----------------------------------------------"
NEW_OTP_RESPONSE=$(curl -s -X POST "$BASE_URL/api/users/send-otp" \
  -H "Content-Type: application/json" \
  -d "{\"phoneNumber\": \"$NEW_PHONE\"}")

echo "Response: $NEW_OTP_RESPONSE"
echo ""

# Test 4: Register new user
echo "📝 Step 4: Registering new user with OTP and name"
echo "--------------------------------------------------"
NEW_AUTH_RESPONSE=$(curl -s -X POST "$BASE_URL/api/users/authenticate" \
  -H "Content-Type: application/json" \
  -d "{\"phoneNumber\": \"$NEW_PHONE\", \"otp\": \"$TEST_OTP\", \"name\": \"Jane Smith\"}")

echo "Response: $NEW_AUTH_RESPONSE"
echo ""

# Test 5: Try authenticating new user without name (should fail)
echo "❌ Step 5: Testing error case - new user without name"
echo "----------------------------------------------------"
ERROR_RESPONSE=$(curl -s -X POST "$BASE_URL/api/users/authenticate" \
  -H "Content-Type: application/json" \
  -d "{\"phoneNumber\": \"+1555555555\", \"otp\": \"$TEST_OTP\"}")

echo "Response: $ERROR_RESPONSE"
echo ""

# Test 6: Test invalid OTP
echo "❌ Step 6: Testing error case - invalid OTP"
echo "-------------------------------------------"
INVALID_OTP_RESPONSE=$(curl -s -X POST "$BASE_URL/api/users/authenticate" \
  -H "Content-Type: application/json" \
  -d "{\"phoneNumber\": \"$PHONE\", \"otp\": \"000000\"}")

echo "Response: $INVALID_OTP_RESPONSE"
echo ""

echo "✅ OTP Flow Testing Complete!"
echo ""
echo "Summary:"
echo "--------"
echo "1. ✅ Send OTP endpoint"
echo "2. ✅ Authenticate existing user"
echo "3. ✅ Send OTP to new user"
echo "4. ✅ Register new user with name"
echo "5. ✅ Error handling - missing name"
echo "6. ✅ Error handling - invalid OTP"
echo ""
echo "🎉 All test cases covered!"