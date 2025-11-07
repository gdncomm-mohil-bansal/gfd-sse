# Quick Start Guide - Replica Service

## Purpose

This service demonstrates why SSE events cannot cross pods without Redis Pub/Sub.

## 5-Minute Demo

### Step 1: Start the Replica Service

```bash
cd dummy-off2on-redis-replica
./mvnw spring-boot:run
```

Wait for: `Started DummyOff2onRedisReplicaApplication`

### Step 2: Run the Test Script

```bash
./test-replica.sh
```

This will test all API endpoints.

### Step 3: Test Same-Pod SSE (Works ✅)

**Terminal 1** - Open SSE connection:
```bash
curl -N "http://localhost:8082/api/replica/connect?userId=user123"
```

Expected output:
```
id:abc-123
event:CONNECTION_ESTABLISHED
data:{"eventId":"abc-123","eventType":"CONNECTION_ESTABLISHED",...}
```

**Terminal 2** - Trigger event:
```bash
curl -X POST "http://localhost:8082/api/replica/checkout/user123"
```

Expected in Terminal 1:
```
id:xyz-456
event:CHECKOUT_INITIATED
data:{"eventId":"xyz-456","eventType":"CHECKOUT_INITIATED",...}
```

✅ **Success!** Event reaches the client because both SSE and API are on the same pod.

### Step 4: Test Cross-Pod SSE (Fails ❌)

You need the main service running for this test.

**Terminal 1** - Start main service:
```bash
cd ../dummy-off2on-redis
./mvnw spring-boot:run
# Runs on port 8081
```

**Terminal 2** - Connect to main service (Pod A):
```bash
# First, get an OTP from dummy-off2on service (port 8080)
# Then connect:
curl -N "http://localhost:8081/api/sse/connect?userId=user123&otp=YOUR_OTP"
```

**Terminal 3** - Try to send event from replica (Pod B):
```bash
curl -X POST "http://localhost:8082/api/replica/checkout/user123"
```

Expected response:
```json
{
  "success": false,
  "message": "Cannot send event - User not connected to this pod"
}
```

❌ **Fails!** Event does NOT reach the client because:
- Client is connected to Pod A (port 8081)
- API request went to Pod B (port 8082)
- Pod B doesn't have the user's emitter

## Understanding the Results

| Scenario | Client Connection | API Request | Result |
|----------|-------------------|-------------|--------|
| Same Pod | Pod B (8082) | Pod B (8082) | ✅ Works |
| Cross Pod | Pod A (8081) | Pod B (8082) | ❌ Fails |

## The Solution

See `dummy-off2on-redis` for the Redis Pub/Sub implementation that solves this problem:

1. Client connects to Pod A
2. API request goes to Pod B
3. Pod B publishes to Redis
4. Pod A (subscribed) receives from Redis
5. Pod A sends to client ✅

## Next Steps

1. ✅ Run the tests above
2. ✅ Read [CROSS_POD_TESTING_GUIDE.md](./CROSS_POD_TESTING_GUIDE.md) for detailed explanations
3. ✅ Review [../dummy-off2on-redis/](../dummy-off2on-redis/) to see the Redis solution
4. ✅ Keep Redis Pub/Sub in your production architecture

## API Endpoints Summary

| Endpoint | Method | Purpose |
|----------|--------|---------|
| `/api/replica/connect?userId=X` | GET | Open SSE connection |
| `/api/replica/checkout/{userId}` | POST | Trigger test event |
| `/api/replica/status/{userId}` | GET | Check if user connected |
| `/api/replica/connections/count` | GET | Get connection count |
| `/api/replica/disconnect/{userId}` | POST | Close connection |

## Troubleshooting

### "Connection refused"
- Make sure service is running: `./mvnw spring-boot:run`
- Check port 8082 is not in use: `lsof -i :8082`

### "Service is NOT running"
- Wait for startup to complete
- Check logs for errors
- Ensure Java 17+ is installed: `java -version`

### SSE connection closes immediately
- Use `-N` flag with curl: `curl -N "http://..."`
- Or use a proper SSE client (browser EventSource, Postman, etc.)

## Key Learnings

1. **One Emitter per Connection**: You must create a new emitter for each `/connect` request
2. **Emitters are Pod-Specific**: Cannot send events to clients on different pods
3. **Redis Pub/Sub is Essential**: Required for distributed SSE architecture
4. **Same-Pod Works Fine**: Events work perfectly when client and API are on the same pod

---

**Remember**: This is a demonstration service. Use `dummy-off2on-redis` for production!

