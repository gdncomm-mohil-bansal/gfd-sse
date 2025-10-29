# Quick Start Guide - Dummy Off2On Redis

Get up and running with the Off2On Redis SSE service in under 5 minutes!

## Prerequisites

- ✅ Java 21 installed
- ✅ Redis running on localhost:6379
- ✅ dummy-off2on service running on port 8080

## 1. Start Redis

```bash
# macOS
brew services start redis

# Or Docker
docker run -d -p 6379:6379 redis:latest

# Verify
redis-cli ping  # Should return PONG
```

## 2. Start the Service

```bash
cd dummy-off2on-redis
mvn spring-boot:run
```

Wait for: `Started DummyOff2onRedisApplication in X seconds`

## 3. Test the Service

### Quick Health Check

```bash
curl http://localhost:8081/api/health
```

Expected: `{"status":"UP",...}`

### Run Full Test Suite

```bash
./test-api.sh
```

This will test:
- ✓ Health check
- ✓ OTP generation
- ✓ OTP validation
- ✓ SSE connection

## 4. Test End-to-End Flow

### Terminal 1: Start SSE Listener

```bash
./test-sse-connection.sh
```

This will:
1. Generate an OTP
2. Connect to SSE
3. Display real-time events

### Terminal 2: Trigger Events

```bash
# Add product to cart (make sure dummy-off2on is running on port 8080)
curl -X POST http://localhost:8080/api/cart/add \
  -H "Content-Type: application/json" \
  -d '{
    "userId": "test-user-XXX",
    "productId": 1,
    "quantity": 2
  }'
```

**Important**: Replace `test-user-XXX` with the User ID shown in Terminal 1!

You should see the cart event appear in Terminal 1 immediately! 🎉

## 5. Test with Postman

1. Import `postman_collection.json`
2. Run "Integration Test - Full Flow"
3. Watch the tests pass! ✅

## API Endpoints Quick Reference

| Endpoint | Method | Purpose |
|----------|--------|---------|
| `/api/health` | GET | Health check |
| `/api/otp/generate` | POST | Generate OTP |
| `/api/otp/validate` | POST | Validate OTP |
| `/api/sse/connect` | GET | Connect SSE |
| `/api/sse/status/{userId}` | GET | Check status |

## Common Issues

### Port 8081 already in use
```bash
# Find and kill process
lsof -ti:8081 | xargs kill -9
```

### Redis connection refused
```bash
# Check if Redis is running
redis-cli ping

# Start Redis
brew services start redis
```

### No events received
- ✓ Check dummy-off2on is running on port 8080
- ✓ Ensure userId matches in both services
- ✓ Verify Redis channels match in both application.properties

## What's Next?

- 📖 Read full [README.md](README.md) for detailed documentation
- 🏗️ Check [ARCHITECTURE.md](ARCHITECTURE.md) for architecture details
- 🔧 Customize `application.properties` for your needs

## Architecture Overview

```
┌─────────────────┐     ┌─────────────────┐     ┌─────────────────┐
│  Front-liner    │────▶│   dummy-off2on  │────▶│   Redis Pub/Sub │
│      PWA        │     │   (Port 8080)   │     │                 │
└─────────────────┘     └─────────────────┘     └────────┬────────┘
                                                           │
                                                           ▼
┌─────────────────┐     ┌─────────────────┐     ┌─────────────────┐
│     GFD PWA     │◀───▶│ dummy-off2on-   │◀────│   Subscribes to │
│                 │ SSE │    redis        │     │   Redis Events  │
│  (OTP Auth)     │     │   (Port 8081)   │     └─────────────────┘
└─────────────────┘     └─────────────────┘
```

## Success! 🎊

You now have:
- ✅ OTP-based authentication working
- ✅ SSE connections established
- ✅ Real-time events flowing from dummy-off2on to GFD PWA
- ✅ Redis Pub/Sub integration working

Happy coding! 🚀

