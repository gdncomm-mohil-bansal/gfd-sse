# End-to-End Testing Guide

This guide provides comprehensive instructions for testing the complete flow from Front-liner PWA → dummy-off2on → Redis → dummy-off2on-redis → GFD PWA.

## Prerequisites

Before starting, ensure you have:

- ✅ Redis running on localhost:6379
- ✅ dummy-off2on service running on port 8080
- ✅ dummy-off2on-redis service running on port 8081
- ✅ Terminal windows ready (3 recommended)

## Setup Instructions

### Terminal 1: Start Redis

```bash
# Start Redis
redis-cli

# Once connected, subscribe to cart events to see them in real-time
SUBSCRIBE cart-events checkout-events
```

Keep this terminal open to monitor Redis messages.

### Terminal 2: Start dummy-off2on-redis with SSE Connection

```bash
cd dummy-off2on-redis

# Run the SSE connection test script
./test-sse-connection.sh test-user-123

# Or manually:
# Step 1: Generate OTP
curl -X POST http://localhost:8081/api/otp/generate \
  -H "Content-Type: application/json" \
  -d '{"userId":"test-user-123","deviceInfo":"E2E Test"}'

# Step 2: Connect to SSE (replace OTP_HERE with actual OTP from step 1)
curl -N -H "Accept: text/event-stream" \
  "http://localhost:8081/api/sse/connect?userId=test-user-123&otp=OTP_HERE"
```

You should see:
```
event:CONNECTION_ESTABLISHED
data:{"eventId":"...","eventType":"CONNECTION_ESTABLISHED",...}

event:heartbeat
data:ping

event:heartbeat
data:ping
```

Keep this terminal open to watch events arrive.

### Terminal 3: Trigger Actions on dummy-off2on

Now we'll trigger various actions to see events flow through the system.

## Test Scenarios

### Test 1: Add Product to Cart

```bash
curl -X POST http://localhost:8080/api/cart/add \
  -H "Content-Type: application/json" \
  -d '{
    "userId": "test-user-123",
    "productId": 1,
    "quantity": 2
  }'
```

**Expected Results:**

**Terminal 1 (Redis):**
```
1) "message"
2) "cart-events"
3) "{\"eventId\":\"...\",\"eventType\":\"CART_ITEM_ADDED\",\"userId\":\"test-user-123\",...}"
```

**Terminal 2 (SSE Connection):**
```
event:CART_ITEM_ADDED
data:{"eventId":"...","eventType":"CART_ITEM_ADDED","userId":"test-user-123","message":"Added 2 x Product 1 to cart",...}
```

**Terminal 3 (Response):**
```json
{
  "success": true,
  "message": "Product added to cart successfully",
  "userId": "test-user-123",
  "cartItems": [...],
  "totalAmount": 29.98,
  "totalItems": 2
}
```

---

### Test 2: Add Another Product

```bash
curl -X POST http://localhost:8080/api/cart/add \
  -H "Content-Type: application/json" \
  -d '{
    "userId": "test-user-123",
    "productId": 2,
    "quantity": 1
  }'
```

Watch Terminal 2 for another `CART_ITEM_ADDED` event!

---

### Test 3: Add More Quantity of Existing Product

```bash
curl -X POST http://localhost:8080/api/cart/add \
  -H "Content-Type: application/json" \
  -d '{
    "userId": "test-user-123",
    "productId": 1,
    "quantity": 1
  }'
```

The cart should now have 3 units of Product 1.

---

### Test 4: View All Products

```bash
curl http://localhost:8080/api/products
```

**Expected:** List of all products (no event should be triggered)

---

### Test 5: Checkout Cart

```bash
curl -X POST http://localhost:8080/api/cart/checkout \
  -H "Content-Type: application/json" \
  -d '{
    "userId": "test-user-123"
  }'
```

**Expected Results:**

**Terminal 1 (Redis):**
```
1) "message"
2) "checkout-events"
3) "{\"eventId\":\"...\",\"eventType\":\"CHECKOUT_COMPLETED\",\"userId\":\"test-user-123\",...}"
```

**Terminal 2 (SSE Connection):**
```
event:CHECKOUT_COMPLETED
data:{"eventId":"...","eventType":"CHECKOUT_COMPLETED","userId":"test-user-123","message":"Checkout completed successfully",...}
```

**Terminal 3 (Response):**
```json
{
  "success": true,
  "message": "Order placed successfully",
  "orderId": "ORD-XXXXXXXX",
  "userId": "test-user-123",
  "totalAmount": 44.97,
  "orderStatus": "CONFIRMED"
}
```

---

### Test 6: Multiple Users

Open a new terminal and test with a different user:

```bash
# Terminal 4: User 2 SSE Connection
curl -X POST http://localhost:8081/api/otp/generate \
  -H "Content-Type: application/json" \
  -d '{"userId":"test-user-456","deviceInfo":"E2E Test User 2"}'

# Get OTP and connect
curl -N -H "Accept: text/event-stream" \
  "http://localhost:8081/api/sse/connect?userId=test-user-456&otp=OTP_HERE"
```

Now trigger actions for user 456:

```bash
# Terminal 5: Add to cart for user 456
curl -X POST http://localhost:8080/api/cart/add \
  -H "Content-Type: application/json" \
  -d '{
    "userId": "test-user-456",
    "productId": 3,
    "quantity": 5
  }'
```

**Expected:** Only Terminal 4 (user 456's SSE connection) should receive the event, not Terminal 2 (user 123).

---

## Verification Checklist

After running all tests, verify:

- [ ] Each action on dummy-off2on publishes an event to Redis
- [ ] Redis receives the event (visible in Terminal 1)
- [ ] dummy-off2on-redis receives the event from Redis
- [ ] SSE connection receives the event (visible in Terminal 2)
- [ ] Events are only sent to the correct user (not broadcast to all)
- [ ] Heartbeat events are sent every 15 seconds
- [ ] OTP is invalidated after successful connection
- [ ] Multiple users can connect simultaneously

---

## Common Issues and Solutions

### Issue: SSE connection immediately closes

**Solution:**
- Check if OTP is valid (not expired, not already used)
- Generate a new OTP before connecting
- Ensure userId and OTP match in the request

### Issue: No events received on SSE

**Solution:**
1. Verify userId in cart operations matches SSE connection userId
2. Check if Redis is running: `redis-cli ping`
3. Verify dummy-off2on is publishing events: check Terminal 1 (Redis)
4. Check dummy-off2on-redis logs for subscription errors

### Issue: Events going to wrong user

**Solution:**
- Ensure userId is correctly set in all requests
- Check dummy-off2on-redis logs for event routing

### Issue: Connection timeout

**Solution:**
- SSE timeout is 30 minutes by default
- Reconnect by generating a new OTP
- Check network connectivity

---

## Performance Testing

### Test Multiple Concurrent Connections

```bash
# Script to create 10 concurrent SSE connections
for i in {1..10}; do
  # Generate OTP
  response=$(curl -s -X POST http://localhost:8081/api/otp/generate \
    -H "Content-Type: application/json" \
    -d "{\"userId\":\"load-test-user-$i\",\"deviceInfo\":\"Load Test\"}")
  
  otp=$(echo "$response" | grep -o '"otp":"[^"]*' | cut -d'"' -f4)
  
  # Connect to SSE in background
  curl -N -H "Accept: text/event-stream" \
    "http://localhost:8081/api/sse/connect?userId=load-test-user-$i&otp=$otp" \
    > "/tmp/sse-$i.log" 2>&1 &
  
  echo "Started connection for user $i"
done

# Check active connections
sleep 2
curl http://localhost:8081/api/sse/connections/count
```

### Generate Load Events

```bash
# Send 100 cart add events rapidly
for i in {1..100}; do
  curl -s -X POST http://localhost:8080/api/cart/add \
    -H "Content-Type: application/json" \
    -d "{\"userId\":\"load-test-user-1\",\"productId\":$((1 + RANDOM % 10)),\"quantity\":1}" \
    > /dev/null
  echo "Sent event $i"
done
```

### Cleanup Load Test

```bash
# Kill all background SSE connections
pkill -f "curl.*sse/connect"

# Clean up log files
rm /tmp/sse-*.log
```

---

## Monitoring During Tests

### Check Service Health

```bash
# dummy-off2on health
curl http://localhost:8080/api/health

# dummy-off2on-redis health
curl http://localhost:8081/api/health
```

### Check Active Connections

```bash
curl http://localhost:8081/api/sse/connections/count
```

### Check Specific User Status

```bash
curl http://localhost:8081/api/sse/status/test-user-123
```

### Monitor Redis

```bash
# Check Redis stats
redis-cli info stats

# Monitor Redis commands in real-time
redis-cli monitor
```

---

## Automated E2E Test Script

Create a file `e2e-test.sh`:

```bash
#!/bin/bash

USER_ID="e2e-test-$(date +%s)"

echo "==================================="
echo "End-to-End Test"
echo "==================================="
echo "User ID: $USER_ID"
echo ""

# Generate OTP
echo "1. Generating OTP..."
response=$(curl -s -X POST http://localhost:8081/api/otp/generate \
  -H "Content-Type: application/json" \
  -d "{\"userId\":\"$USER_ID\",\"deviceInfo\":\"E2E Test\"}")

OTP=$(echo "$response" | grep -o '"otp":"[^"]*' | cut -d'"' -f4)
echo "   OTP: $OTP"
echo ""

# Connect to SSE in background
echo "2. Connecting to SSE..."
curl -N -H "Accept: text/event-stream" \
  "http://localhost:8081/api/sse/connect?userId=$USER_ID&otp=$OTP" \
  > /tmp/e2e-sse.log 2>&1 &

SSE_PID=$!
sleep 2
echo "   Connected (PID: $SSE_PID)"
echo ""

# Add products to cart
echo "3. Adding products to cart..."
curl -s -X POST http://localhost:8080/api/cart/add \
  -H "Content-Type: application/json" \
  -d "{\"userId\":\"$USER_ID\",\"productId\":1,\"quantity\":2}" > /dev/null
echo "   Added Product 1 x2"

sleep 1

curl -s -X POST http://localhost:8080/api/cart/add \
  -H "Content-Type: application/json" \
  -d "{\"userId\":\"$USER_ID\",\"productId\":2,\"quantity\":1}" > /dev/null
echo "   Added Product 2 x1"
echo ""

# Checkout
sleep 1
echo "4. Checking out..."
curl -s -X POST http://localhost:8080/api/cart/checkout \
  -H "Content-Type: application/json" \
  -d "{\"userId\":\"$USER_ID\"}" > /dev/null
echo "   Checkout completed"
echo ""

# Wait and check results
sleep 2
echo "5. Checking SSE events received..."
grep -c "CART_ITEM_ADDED" /tmp/e2e-sse.log && echo "   ✓ Cart events received" || echo "   ✗ No cart events"
grep -c "CHECKOUT_COMPLETED" /tmp/e2e-sse.log && echo "   ✓ Checkout event received" || echo "   ✗ No checkout event"
echo ""

# Cleanup
kill $SSE_PID 2>/dev/null
rm /tmp/e2e-sse.log

echo "==================================="
echo "E2E Test Completed"
echo "==================================="
```

Run it:
```bash
chmod +x e2e-test.sh
./e2e-test.sh
```

---

## Next Steps

After successful E2E testing:

1. ✅ Document any issues found
2. ✅ Test error scenarios (invalid OTP, network issues)
3. ✅ Performance test with realistic load
4. ✅ Integrate with actual GFD PWA frontend
5. ✅ Add monitoring and alerting
6. ✅ Security audit of OTP mechanism

---

## Troubleshooting Commands

```bash
# Check if services are running
lsof -i :8080  # dummy-off2on
lsof -i :8081  # dummy-off2on-redis
lsof -i :6379  # Redis

# Check Redis connectivity
redis-cli ping

# View service logs
# (if running with systemd or similar)
journalctl -f -u dummy-off2on
journalctl -f -u dummy-off2on-redis

# Test Redis pub/sub manually
redis-cli
> PUBLISH cart-events '{"test":"message"}'
```

Good luck with your testing! 🚀

