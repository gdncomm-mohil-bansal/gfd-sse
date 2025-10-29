# Complete Setup Guide - GFD SSE System

This guide will walk you through setting up the complete GFD SSE system from scratch.

## 🎯 What You'll Build

By the end of this guide, you'll have:
- ✅ Redis running locally
- ✅ dummy-off2on service (Port 8080)
- ✅ dummy-off2on-redis service (Port 8081)
- ✅ Real-time event flow working end-to-end

## ⏱️ Time Required

- First-time setup: **15 minutes**
- Subsequent runs: **2 minutes**

## 📋 Prerequisites

Install these before starting:

### 1. Java 21

```bash
# Check if already installed
java -version

# macOS (using Homebrew)
brew install openjdk@21

# Verify
java -version  # Should show version 21
```

### 2. Maven

```bash
# Check if already installed
mvn -version

# macOS
brew install maven

# Verify
mvn -version  # Should show Maven 3.x
```

### 3. Redis

```bash
# macOS
brew install redis

# Start Redis
brew services start redis

# Verify
redis-cli ping  # Should return PONG
```

Alternatively, use Docker:
```bash
docker run -d -p 6379:6379 --name redis redis:latest
redis-cli ping  # Should return PONG
```

## 🚀 Step-by-Step Setup

### Step 1: Clone/Navigate to Project

```bash
cd /path/to/gfd-sse
```

### Step 2: Build Both Services

```bash
# Build dummy-off2on
cd dummy-off2on
mvn clean install
cd ..

# Build dummy-off2on-redis
cd dummy-off2on-redis
mvn clean install
cd ..
```

**Expected Output:**
```
BUILD SUCCESS
Total time: XX.XXX s
```

If you see errors, check:
- Java version is 21
- Maven is properly installed
- Internet connection (for downloading dependencies)

### Step 3: Start Redis (if not already running)

```bash
# Check if Redis is running
redis-cli ping

# If not running:
brew services start redis  # macOS
# OR
redis-server  # Manual start

# Verify
redis-cli ping  # Should return PONG
```

### Step 4: Start dummy-off2on

Open a new terminal window/tab:

```bash
cd /path/to/gfd-sse/dummy-off2on
mvn spring-boot:run
```

**Wait for:**
```
Started DummyOff2onApplication in X.XXX seconds
```

**Verify:**
```bash
# In another terminal
curl http://localhost:8080/api/health
```

Expected response:
```json
{"status":"UP","service":"dummy-off2on","timestamp":...}
```

### Step 5: Start dummy-off2on-redis

Open another new terminal window/tab:

```bash
cd /path/to/gfd-sse/dummy-off2on-redis
mvn spring-boot:run
```

**Wait for:**
```
Started DummyOff2onRedisApplication in X.XXX seconds
```

**Verify:**
```bash
# In another terminal
curl http://localhost:8081/api/health
```

Expected response:
```json
{"status":"UP","service":"dummy-off2on-redis","timestamp":...,"activeConnections":0}
```

## ✅ Verification

Now you should have:
- ✅ **Redis**: Running on port 6379
- ✅ **dummy-off2on**: Running on port 8080
- ✅ **dummy-off2on-redis**: Running on port 8081

### Quick System Check

Run this command:
```bash
echo "Redis:" && redis-cli ping && \
echo "dummy-off2on:" && curl -s http://localhost:8080/api/health | grep -q "UP" && echo "UP" && \
echo "dummy-off2on-redis:" && curl -s http://localhost:8081/api/health | grep -q "UP" && echo "UP"
```

Expected output:
```
Redis:
PONG
dummy-off2on:
UP
dummy-off2on-redis:
UP
```

## 🧪 Test the Complete Flow

Now let's test the end-to-end real-time event flow!

### Terminal 1: Watch Redis Events

```bash
redis-cli
SUBSCRIBE cart-events checkout-events
```

Keep this running.

### Terminal 2: Connect to SSE

```bash
cd /path/to/gfd-sse/dummy-off2on-redis
./test-sse-connection.sh test-user-123
```

You should see:
```
OTP Generated: XXXXXX
Connecting to SSE...
Connected
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
event:CONNECTION_ESTABLISHED
data:{...}

event:heartbeat
data:ping
```

Keep this running.

### Terminal 3: Trigger Events

```bash
# Add product to cart
curl -X POST http://localhost:8080/api/cart/add \
  -H "Content-Type: application/json" \
  -d '{
    "userId": "test-user-123",
    "productId": 1,
    "quantity": 2
  }'
```

**What Should Happen:**

1. **Terminal 3** shows:
```json
{
  "success": true,
  "message": "Product added to cart successfully",
  ...
}
```

2. **Terminal 1 (Redis)** shows:
```
1) "message"
2) "cart-events"
3) "{\"eventType\":\"CART_ITEM_ADDED\",\"userId\":\"test-user-123\",...}"
```

3. **Terminal 2 (SSE)** shows:
```
event:CART_ITEM_ADDED
data:{"eventId":"...","eventType":"CART_ITEM_ADDED","userId":"test-user-123",...}
```

If all three show the event, **CONGRATULATIONS! 🎉** Your system is working perfectly!

### Try More Events

```bash
# Add another product
curl -X POST http://localhost:8080/api/cart/add \
  -H "Content-Type: application/json" \
  -d '{
    "userId": "test-user-123",
    "productId": 2,
    "quantity": 1
  }'

# Checkout
curl -X POST http://localhost:8080/api/cart/checkout \
  -H "Content-Type: application/json" \
  -d '{
    "userId": "test-user-123"
  }'
```

Watch events flow in real-time! 🚀

## 📊 Using Postman

### Import Collections

1. Open Postman
2. Import `dummy-off2on/postman_collection.json`
3. Import `dummy-off2on-redis/postman_collection.json`

### Run Integration Tests

In dummy-off2on-redis collection:
1. Open "Integration Test - Full Flow" folder
2. Click "Run" to execute all tests
3. Watch tests pass! ✅

## 🐳 Docker Alternative

If you prefer Docker:

```bash
cd dummy-off2on-redis
docker-compose up -d

# Wait for services to start (30-60 seconds)
docker-compose logs -f

# When you see "Started" messages, test:
curl http://localhost:8080/api/health
curl http://localhost:8081/api/health
```

Stop services:
```bash
docker-compose down
```

## 🛑 Stopping Services

### Stop Spring Boot Services

In each terminal running the services, press:
```
Ctrl + C
```

### Stop Redis

```bash
# If using brew services
brew services stop redis

# If running manually
redis-cli shutdown
```

### Docker

```bash
docker-compose down
```

## 🔄 Restart Services

Next time you want to start:

```bash
# Terminal 1
cd gfd-sse/dummy-off2on
mvn spring-boot:run

# Terminal 2
cd gfd-sse/dummy-off2on-redis
mvn spring-boot:run

# Terminal 3 - Test
cd gfd-sse/dummy-off2on-redis
./test-sse-connection.sh test-user-123
```

## ❓ Troubleshooting

### "Port already in use"

```bash
# Find what's using the port
lsof -i :8080  # or 8081

# Kill the process
kill -9 <PID>
```

### "Redis connection refused"

```bash
# Check Redis status
redis-cli ping

# Start Redis
brew services start redis
# OR
redis-server
```

### "Cannot find Java"

```bash
# Check Java
java -version

# Should be version 21
# If not, install Java 21
brew install openjdk@21
```

### "Maven command not found"

```bash
# Install Maven
brew install maven

# Verify
mvn -version
```

### Events not appearing

1. Check all services are running:
```bash
curl http://localhost:8080/api/health
curl http://localhost:8081/api/health
redis-cli ping
```

2. Verify userId matches in all requests

3. Check SSE connection is active:
```bash
curl http://localhost:8081/api/sse/status/test-user-123
```

4. Check service logs for errors

### OTP expired

OTPs expire after 5 minutes. Generate a new one:
```bash
curl -X POST http://localhost:8081/api/otp/generate \
  -H "Content-Type: application/json" \
  -d '{"userId":"test-user-123","deviceInfo":"Test"}'
```

## 📚 Next Steps

Now that your system is running:

1. **Explore APIs**: Check [README.md](README.md) for all endpoints
2. **Run Tests**: Execute test scripts in both services
3. **Read Architecture**: Understand the design in [ARCHITECTURE.md](dummy-off2on-redis/ARCHITECTURE.md)
4. **E2E Testing**: Follow [E2E_TESTING.md](dummy-off2on-redis/E2E_TESTING.md)
5. **Build Frontend**: Integrate with Vue.js PWAs

## 🎓 Understanding the System

### Service Ports

| Service | Port | Purpose |
|---------|------|---------|
| Redis | 6379 | Message broker |
| dummy-off2on | 8080 | E-commerce backend |
| dummy-off2on-redis | 8081 | SSE server |

### Event Flow

```
Action → dummy-off2on → Redis → dummy-off2on-redis → SSE → Client
```

### Key Concepts

- **Redis Pub/Sub**: Decouples services, enables real-time messaging
- **SSE**: One-way server-to-client communication for live updates
- **OTP Auth**: Secure, time-limited authentication for SSE connections

## 📞 Getting Help

If stuck:

1. Check logs in service terminals
2. Review troubleshooting section above
3. Verify all prerequisites are installed
4. Check port availability
5. Ensure Redis is running

## ✨ Success Checklist

Before moving forward, ensure:

- [ ] All three services start without errors
- [ ] Health checks return "UP"
- [ ] OTP generation works
- [ ] SSE connection establishes
- [ ] Events flow from dummy-off2on to SSE client
- [ ] Test scripts run successfully
- [ ] Postman collections work

If all checked, you're ready to build the frontend! 🎉

---

**Need Help?** Check the detailed documentation in each service's directory.

**Ready for More?** Read [E2E_TESTING.md](dummy-off2on-redis/E2E_TESTING.md) for advanced testing scenarios.

