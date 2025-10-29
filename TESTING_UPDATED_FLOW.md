# Testing Guide - Updated OTP & SSE Flow

## 🎯 Overview

This guide explains how to test the updated architecture where:
1. **OTP Generation** happens in `dummy-off2on` (Front-liner's service)
2. **OTP Validation & SSE** happens in `dummy-off2on-redis` (GFD's monitoring service)
3. **OTP Storage** is shared via Redis

## 🚀 Quick Test (5 Minutes)

### Prerequisites
```bash
# Ensure all services are running
# Terminal 1: Redis
redis-server

# Terminal 2: dummy-off2on
cd dummy-off2on
mvn spring-boot:run

# Terminal 3: dummy-off2on-redis
cd dummy-off2on-redis
mvn spring-boot:run
```

### Step-by-Step Test

#### Step 1: Front-liner Generates OTP

```bash
# Front-liner PWA requests OTP
curl -X POST http://localhost:8080/api/otp/generate \
  -H "Content-Type: application/json" \
  -d '{
    "userId": "FL123",
    "deviceInfo": "Front-liner iPhone",
    "sessionId": "session-abc-123"
  }'
```

**Expected Response:**
```json
{
  "success": true,
  "otp": "485721",
  "userId": "FL123",
  "expiresAt": 1698765432000,
  "message": "OTP generated successfully",
  "instructions": "Share this OTP with GFD to monitor your session. Valid for 5 minutes."
}
```

**💾 Save the OTP** - You'll need it in the next step!

#### Step 2: Verify OTP in Redis

```bash
# Check that OTP was stored in Redis
redis-cli GET otp:485721
# Should return: "FL123"

# Check reverse mapping
redis-cli GET otp:user:FL123
# Should return: "485721"

# Check TTL (time to live)
redis-cli TTL otp:485721
# Should return: ~300 seconds (5 minutes)
```

#### Step 3: GFD Validates OTP (Optional - for debugging)

```bash
# GFD can validate OTP before connecting
curl -X POST http://localhost:8081/api/otp/validate \
  -H "Content-Type: application/json" \
  -d '{
    "userId": "FL123",
    "otp": "485721"
  }'
```

**Expected Response:**
```json
{
  "valid": true,
  "userId": "FL123",
  "message": "OTP is valid"
}
```

#### Step 4: GFD Connects to SSE

```bash
# GFD PWA establishes SSE connection
# Replace 485721 with your actual OTP from Step 1
curl -N -H "Accept: text/event-stream" \
  "http://localhost:8081/api/sse/connect?userId=FL123&otp=485721"
```

**Expected Output:**
```
event:CONNECTION_ESTABLISHED
data:{"eventId":"...","eventType":"CONNECTION_ESTABLISHED","userId":"FL123",...}

event:heartbeat
data:ping

event:heartbeat
data:ping
```

Keep this terminal open! You'll see events here.

#### Step 5: Verify OTP was Invalidated

```bash
# In another terminal, check that OTP no longer exists in Redis
redis-cli GET otp:485721
# Should return: (nil)

# Try to reuse OTP (should fail)
curl -X POST http://localhost:8081/api/otp/validate \
  -H "Content-Type: application/json" \
  -d '{
    "userId": "FL123",
    "otp": "485721"
  }'

# Response should show invalid
```

#### Step 6: Trigger Events from Front-liner

```bash
# Front-liner adds product to cart
curl -X POST http://localhost:8080/api/cart/add \
  -H "Content-Type: application/json" \
  -d '{
    "userId": "FL123",
    "productId": 1,
    "quantity": 2
  }'
```

**Watch Terminal from Step 4** - You should see:
```
event:CART_ITEM_ADDED
data:{"eventId":"...","eventType":"CART_ITEM_ADDED","userId":"FL123","message":"Added 2 x Product 1 to cart",...}
```

#### Step 7: Trigger Checkout Event

```bash
# Front-liner checks out
curl -X POST http://localhost:8080/api/cart/checkout \
  -H "Content-Type: application/json" \
  -d '{
    "userId": "FL123"
  }'
```

**Watch Terminal from Step 4** - You should see:
```
event:CHECKOUT_COMPLETED
data:{"eventId":"...","eventType":"CHECKOUT_COMPLETED","userId":"FL123","orderId":"ORD-XXXXXXXX",...}
```

## 🎉 Success Criteria

- [x] OTP generated in dummy-off2on (Port 8080)
- [x] OTP stored in Redis with TTL
- [x] OTP validated from Redis in dummy-off2on-redis
- [x] SSE connection established successfully
- [x] OTP invalidated after connection (one-time use)
- [x] Events from Front-liner flow to GFD via SSE
- [x] Only events for correct userId are received

## 🧪 Advanced Test Scenarios

### Test 1: OTP Expiration

```bash
# Generate OTP
curl -X POST http://localhost:8080/api/otp/generate \
  -H "Content-Type: application/json" \
  -d '{"userId": "FL999", "deviceInfo": "Test"}'

# Wait 6 minutes (or set shorter TTL for testing)

# Try to connect with expired OTP
curl -N -H "Accept: text/event-stream" \
  "http://localhost:8081/api/sse/connect?userId=FL999&otp=XXXXXX"

# Should get: 401 Unauthorized - "Invalid or expired OTP"
```

### Test 2: OTP Reuse (Should Fail)

```bash
# Generate OTP
RESPONSE=$(curl -s -X POST http://localhost:8080/api/otp/generate \
  -H "Content-Type: application/json" \
  -d '{"userId": "FL888", "deviceInfo": "Test"}')

OTP=$(echo $RESPONSE | grep -o '"otp":"[^"]*' | cut -d'"' -f4)

# First connection (should succeed)
curl -N -H "Accept: text/event-stream" \
  "http://localhost:8081/api/sse/connect?userId=FL888&otp=$OTP" &

# Wait 2 seconds
sleep 2

# Second connection with same OTP (should fail)
curl -N -H "Accept: text/event-stream" \
  "http://localhost:8081/api/sse/connect?userId=FL888&otp=$OTP"

# Should get: 401 Unauthorized
```

### Test 3: Wrong UserId with Valid OTP

```bash
# Generate OTP for FL123
RESPONSE=$(curl -s -X POST http://localhost:8080/api/otp/generate \
  -H "Content-Type: application/json" \
  -d '{"userId": "FL123", "deviceInfo": "Test"}')

OTP=$(echo $RESPONSE | grep -o '"otp":"[^"]*' | cut -d'"' -f4)

# Try to connect with different userId
curl -N -H "Accept: text/event-stream" \
  "http://localhost:8081/api/sse/connect?userId=FL999&otp=$OTP"

# Should get: 401 Unauthorized - "User ID does not match OTP"
```

### Test 4: Multiple Users Simultaneously

```bash
# Terminal 1: Front-liner FL123
RESPONSE1=$(curl -s -X POST http://localhost:8080/api/otp/generate \
  -H "Content-Type: application/json" \
  -d '{"userId": "FL123", "deviceInfo": "Test"}')
OTP1=$(echo $RESPONSE1 | grep -o '"otp":"[^"]*' | cut -d'"' -f4)

curl -N -H "Accept: text/event-stream" \
  "http://localhost:8081/api/sse/connect?userId=FL123&otp=$OTP1"

# Terminal 2: Front-liner FL456
RESPONSE2=$(curl -s -X POST http://localhost:8080/api/otp/generate \
  -H "Content-Type: application/json" \
  -d '{"userId": "FL456", "deviceInfo": "Test"}')
OTP2=$(echo $RESPONSE2 | grep -o '"otp":"[^"]*' | cut -d'"' -f4)

curl -N -H "Accept: text/event-stream" \
  "http://localhost:8081/api/sse/connect?userId=FL456&otp=$OTP2"

# Terminal 3: Trigger events for FL123
curl -X POST http://localhost:8080/api/cart/add \
  -H "Content-Type: application/json" \
  -d '{"userId": "FL123", "productId": 1, "quantity": 1}'

# Terminal 4: Trigger events for FL456
curl -X POST http://localhost:8080/api/cart/add \
  -H "Content-Type: application/json" \
  -d '{"userId": "FL456", "productId": 2, "quantity": 2}'

# Verify: Terminal 1 only sees FL123 events
# Verify: Terminal 2 only sees FL456 events
```

### Test 5: Connection Replacement

```bash
# Generate OTP
RESPONSE=$(curl -s -X POST http://localhost:8080/api/otp/generate \
  -H "Content-Type: application/json" \
  -d '{"userId": "FL777", "deviceInfo": "Test"}')
OTP1=$(echo $RESPONSE | grep -o '"otp":"[^"]*' | cut -d'"' -f4)

# Establish first connection
curl -N -H "Accept: text/event-stream" \
  "http://localhost:8081/api/sse/connect?userId=FL777&otp=$OTP1" &

PID1=$!

# Wait a bit
sleep 3

# Generate new OTP for same user
RESPONSE2=$(curl -s -X POST http://localhost:8080/api/otp/generate \
  -H "Content-Type: application/json" \
  -d '{"userId": "FL777", "deviceInfo": "Test"}')
OTP2=$(echo $RESPONSE2 | grep -o '"otp":"[^"]*' | cut -d'"' -f4)

# Establish second connection (should close first one)
curl -N -H "Accept: text/event-stream" \
  "http://localhost:8081/api/sse/connect?userId=FL777&otp=$OTP2"

# First connection (PID1) should be closed
```

## 📊 Monitoring During Tests

### Check Active Connections

```bash
# Get count
curl http://localhost:8081/api/sse/connections/count

# Check specific user
curl http://localhost:8081/api/sse/status/FL123
```

### Monitor Redis

```bash
# Watch OTP keys in real-time
redis-cli
> MONITOR

# Or list all OTP keys
> KEYS otp:*

# Get all info about an OTP
> GET otp:485721
> TTL otp:485721
```

### Monitor Events in Redis

```bash
# Subscribe to cart events
redis-cli SUBSCRIBE cart-events

# In another terminal, trigger cart events
curl -X POST http://localhost:8080/api/cart/add \
  -H "Content-Type: application/json" \
  -d '{"userId": "FL123", "productId": 1, "quantity": 1}'

# Watch events appear in Redis subscriber
```

## 🐛 Troubleshooting

### Issue: OTP Not Found in Redis

**Symptoms:**
```
curl http://localhost:8081/api/sse/connect?userId=FL123&otp=485721
Response: 401 Unauthorized
```

**Check:**
```bash
# Is Redis running?
redis-cli ping

# Does OTP exist?
redis-cli GET otp:485721

# Check dummy-off2on logs for OTP generation
```

**Solution:**
- Ensure Redis is running
- Generate new OTP
- Check that dummy-off2on is connecting to correct Redis

### Issue: Events Not Received on SSE

**Symptoms:**
- SSE connection established
- Events triggered
- No events received

**Check:**
```bash
# Is userId correct in both places?
# Check dummy-off2on-redis logs

# Verify event has correct userId
curl -X POST http://localhost:8080/api/cart/add \
  -H "Content-Type: application/json" \
  -d '{"userId": "FL123", ...}'  # Must match SSE connection userId
```

**Solution:**
- Ensure userId matches between OTP generation, SSE connection, and cart operations
- Check Redis Pub/Sub is working: `redis-cli SUBSCRIBE cart-events`
- Verify dummy-off2on-redis is subscribed to correct channels

### Issue: OTP Already Used

**Symptoms:**
```
First connection: Success
Second connection with same OTP: 401 Unauthorized
```

**This is correct behavior!** OTPs are one-time use.

**Solution:**
- Generate a new OTP for each connection
- OTPs are invalidated after successful SSE connection

## 📝 Complete Test Script

Save this as `test-updated-flow.sh`:

```bash
#!/bin/bash

BASE_URL_OFF2ON="http://localhost:8080"
BASE_URL_REDIS="http://localhost:8081"
USER_ID="test-user-$(date +%s)"

echo "======================================"
echo "Testing Updated OTP & SSE Flow"
echo "======================================"
echo "User ID: $USER_ID"
echo ""

# Step 1: Generate OTP (dummy-off2on)
echo "Step 1: Generating OTP in dummy-off2on..."
RESPONSE=$(curl -s -X POST $BASE_URL_OFF2ON/api/otp/generate \
  -H "Content-Type: application/json" \
  -d "{\"userId\":\"$USER_ID\",\"deviceInfo\":\"Test Script\"}")

OTP=$(echo $RESPONSE | grep -o '"otp":"[^"]*' | cut -d'"' -f4)

if [ -z "$OTP" ]; then
  echo "❌ Failed to generate OTP"
  echo "Response: $RESPONSE"
  exit 1
fi

echo "✅ OTP Generated: $OTP"
echo ""

# Step 2: Verify OTP in Redis
echo "Step 2: Verifying OTP in Redis..."
REDIS_VALUE=$(redis-cli GET otp:$OTP)

if [ "$REDIS_VALUE" == "$USER_ID" ]; then
  echo "✅ OTP found in Redis: $REDIS_VALUE"
else
  echo "❌ OTP not found in Redis"
  exit 1
fi
echo ""

# Step 3: Validate OTP (dummy-off2on-redis)
echo "Step 3: Validating OTP in dummy-off2on-redis..."
VALIDATION=$(curl -s -X POST $BASE_URL_REDIS/api/otp/validate \
  -H "Content-Type: application/json" \
  -d "{\"userId\":\"$USER_ID\",\"otp\":\"$OTP\"}")

if echo $VALIDATION | grep -q '"valid":true'; then
  echo "✅ OTP validated successfully"
else
  echo "❌ OTP validation failed"
  echo "Response: $VALIDATION"
  exit 1
fi
echo ""

# Step 4: Connect to SSE
echo "Step 4: Connecting to SSE..."
echo "(Will run for 10 seconds to capture events)"
timeout 10s curl -N -H "Accept: text/event-stream" \
  "$BASE_URL_REDIS/api/sse/connect?userId=$USER_ID&otp=$OTP" > /tmp/sse-test.log 2>&1 &

SSE_PID=$!
sleep 2

# Step 5: Trigger cart event
echo "Step 5: Triggering cart event..."
curl -s -X POST $BASE_URL_OFF2ON/api/cart/add \
  -H "Content-Type: application/json" \
  -d "{\"userId\":\"$USER_ID\",\"productId\":1,\"quantity\":2}" > /dev/null

sleep 2

# Step 6: Trigger checkout event
echo "Step 6: Triggering checkout event..."
curl -s -X POST $BASE_URL_OFF2ON/api/cart/checkout \
  -H "Content-Type: application/json" \
  -d "{\"userId\":\"$USER_ID\"}" > /dev/null

sleep 3

# Wait for SSE process to complete
wait $SSE_PID 2>/dev/null

# Step 7: Verify events received
echo ""
echo "Step 7: Verifying events received..."

if grep -q "CART_ITEM_ADDED" /tmp/sse-test.log; then
  echo "✅ Cart event received"
else
  echo "❌ Cart event NOT received"
fi

if grep -q "CHECKOUT_COMPLETED" /tmp/sse-test.log; then
  echo "✅ Checkout event received"
else
  echo "❌ Checkout event NOT received"
fi

# Step 8: Verify OTP was invalidated
echo ""
echo "Step 8: Verifying OTP was invalidated..."
REDIS_VALUE_AFTER=$(redis-cli GET otp:$OTP)

if [ -z "$REDIS_VALUE_AFTER" ]; then
  echo "✅ OTP invalidated (one-time use)"
else
  echo "❌ OTP still exists in Redis"
fi

# Cleanup
rm /tmp/sse-test.log

echo ""
echo "======================================"
echo "Test Complete!"
echo "======================================"
```

Make it executable and run:
```bash
chmod +x test-updated-flow.sh
./test-updated-flow.sh
```

## 🎊 Summary

The updated flow now properly separates concerns:
1. **Front-liner generates OTP** (dummy-off2on)
2. **OTP stored in Redis** (shared state)
3. **GFD validates OTP and connects** (dummy-off2on-redis)
4. **Events flow to correct SSE connection** (per userId)

All tests should pass if the architecture is working correctly! 🚀

---

**Updated**: October 26, 2025  
**Version**: 2.0  
**Status**: ✅ Ready for Testing

