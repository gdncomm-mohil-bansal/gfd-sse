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
REDIS_VALUE=$(redis-cli GET otp:$OTP 2>/dev/null)

if [ "$REDIS_VALUE" == "$USER_ID" ]; then
  echo "✅ OTP found in Redis: $REDIS_VALUE"
else
  echo "⚠️  Could not verify in Redis (redis-cli may not be in PATH)"
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
sleep 3

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

# Cleanup
rm /tmp/sse-test.log 2>/dev/null

echo ""
echo "======================================"
echo "Test Complete!"
echo "======================================"
