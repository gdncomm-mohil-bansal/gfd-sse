# GFD SSE - Front-liner E-Commerce System

A proof-of-concept (POC) real-time e-commerce system that demonstrates Server-Sent Events (SSE) with Redis Pub/Sub for connecting front-liner operations to backend monitoring systems.

## 🎯 Project Overview

This POC demonstrates a complete architecture for real-time communication between:
- **Front-liner PWA**: Customer-facing application where front-liners interact
- **dummy-off2on**: Backend service handling e-commerce operations
- **Redis Pub/Sub**: Message broker for event distribution
- **dummy-off2on-redis**: SSE service for real-time updates
- **GFD PWA**: Monitoring dashboard receiving live updates

## 🏗️ Architecture

```
┌─────────────────┐     ┌─────────────────┐     ┌─────────────────┐
│  Front-liner    │────▶│   dummy-off2on  │────▶│   Redis Pub/Sub │
│      PWA        │ REST│   (Port 8080)   │     │   (Port 6379)   │
│  (Customer UI)  │     │                 │     └────────┬────────┘
└─────────────────┘     └─────────────────┘              │
                                                          │ Subscribe
                                                          ▼
┌─────────────────┐     ┌─────────────────────────────────────────┐
│     GFD PWA     │◀───▶│   dummy-off2on-redis (Port 8081)        │
│  (Monitor UI)   │ SSE │   - OTP Authentication                  │
│  + Front-liner  │     │   - SSE Connection Management           │
│      PWA        │     │   - Real-time Event Broadcasting        │
└─────────────────┘     └─────────────────────────────────────────┘
```

### Component Responsibilities

| Component | Technology | Port | Purpose |
|-----------|-----------|------|---------|
| **dummy-off2on** | Spring Boot | 8080 | E-commerce backend, REST APIs, Redis publisher |
| **dummy-off2on-redis** | Spring Boot | 8081 | Redis subscriber, SSE server, OTP authentication |
| **Redis** | Redis | 6379 | Message broker for Pub/Sub |
| **Front-liner PWA** | Vue.js | TBD | Customer-facing app |
| **GFD PWA** | Vue.js | TBD | Monitoring dashboard |

## 🚀 Quick Start

### Prerequisites

- Java 21
- Maven 3.x
- Redis Server
- Docker (optional)

### Option 1: Local Development

#### Step 1: Start Redis

```bash
# macOS
brew services start redis

# Or Docker
docker run -d -p 6379:6379 redis:latest
```

#### Step 2: Start dummy-off2on

```bash
cd dummy-off2on
mvn spring-boot:run
```

Service will start on `http://localhost:8080`

#### Step 3: Start dummy-off2on-redis

```bash
cd dummy-off2on-redis
mvn spring-boot:run
```

Service will start on `http://localhost:8081`

#### Step 4: Test the Flow

```bash
# Terminal 1: Connect to SSE
cd dummy-off2on-redis
./test-sse-connection.sh test-user-123

# Terminal 2: Trigger an event
curl -X POST http://localhost:8080/api/cart/add \
  -H "Content-Type: application/json" \
  -d '{"userId":"test-user-123","productId":1,"quantity":2}'
```

Watch the event appear in Terminal 1! 🎉

### Option 2: Docker Compose

```bash
cd dummy-off2on-redis
docker-compose up -d
```

This starts:
- Redis on port 6379
- dummy-off2on on port 8080
- dummy-off2on-redis on port 8081

## 📚 Documentation

### dummy-off2on Service

- [README](dummy-off2on/README.md) - Setup and usage
- [QUICKSTART](dummy-off2on/QUICKSTART.md) - Quick start guide
- [ARCHITECTURE](dummy-off2on/ARCHITECTURE.md) - Architecture details
- [Postman Collection](dummy-off2on/postman_collection.json) - API tests

**Key Features:**
- REST APIs for products, cart, checkout
- In-memory data storage
- Redis event publishing
- CORS enabled

### dummy-off2on-redis Service

- [README](dummy-off2on-redis/README.md) - Setup and usage
- [QUICKSTART](dummy-off2on-redis/QUICKSTART.md) - 5-minute setup
- [ARCHITECTURE](dummy-off2on-redis/ARCHITECTURE.md) - Technical details
- [E2E_TESTING](dummy-off2on-redis/E2E_TESTING.md) - Test scenarios
- [PROJECT_SUMMARY](dummy-off2on-redis/PROJECT_SUMMARY.md) - Overview
- [Postman Collection](dummy-off2on-redis/postman_collection.json) - API tests

**Key Features:**
- OTP-based authentication (6-digit)
- SSE connection management
- Redis Pub/Sub subscriber
- Real-time event forwarding
- Heartbeat mechanism

## 🔐 Authentication Flow

The system uses a unique OTP-based authentication mechanism similar to Apple Screen Cast:

```
1. GFD PWA requests OTP from dummy-off2on-redis
   ↓
2. System generates 6-digit OTP (valid for 5 minutes)
   ↓
3. GFD PWA displays OTP to user
   ↓
4. User enters OTP in Front-liner PWA
   ↓
5. Front-liner PWA connects to SSE with OTP
   ↓
6. System validates OTP and establishes connection
   ↓
7. OTP is invalidated (one-time use)
   ↓
8. Real-time events flow to Front-liner PWA
```

## 🔄 Event Flow

### Cart Event Example

```
1. Front-liner adds product to cart via Front-liner PWA
   ↓
2. POST /api/cart/add → dummy-off2on
   ↓
3. dummy-off2on processes request and publishes to Redis
   ↓ PUBLISH cart-events {"eventType":"CART_ITEM_ADDED",...}
4. Redis broadcasts to subscribers
   ↓
5. dummy-off2on-redis receives event
   ↓
6. Event routed to correct user's SSE connection
   ↓
7. Front-liner PWA receives real-time update via SSE
   ↓
8. UI updates automatically showing cart change
```

## 🧪 Testing

### Quick Health Checks

```bash
# Check dummy-off2on
curl http://localhost:8080/api/health

# Check dummy-off2on-redis
curl http://localhost:8081/api/health

# Check Redis
redis-cli ping
```

### Run Test Suites

```bash
# Test dummy-off2on APIs
cd dummy-off2on
./test-api.sh

# Test dummy-off2on-redis
cd dummy-off2on-redis
./test-api.sh

# Run end-to-end tests
cd dummy-off2on-redis
# Follow instructions in E2E_TESTING.md
```

### Import Postman Collections

1. Import `dummy-off2on/postman_collection.json`
2. Import `dummy-off2on-redis/postman_collection.json`
3. Run collections or individual requests

## 📊 API Endpoints

### dummy-off2on (Port 8080)

| Endpoint | Method | Purpose |
|----------|--------|---------|
| `/api/health` | GET | Health check |
| `/api/products` | GET | List all products |
| `/api/products/{id}` | GET | Get product by ID |
| `/api/cart/add` | POST | Add item to cart |
| `/api/cart/{userId}` | GET | Get user's cart |
| `/api/cart/checkout` | POST | Checkout cart |

### dummy-off2on-redis (Port 8081)

| Endpoint | Method | Purpose |
|----------|--------|---------|
| `/api/health` | GET | Health check |
| `/api/otp/generate` | POST | Generate OTP |
| `/api/otp/validate` | POST | Validate OTP |
| `/api/sse/connect` | GET | Establish SSE connection |
| `/api/sse/disconnect/{userId}` | POST | Close connection |
| `/api/sse/status/{userId}` | GET | Check connection status |
| `/api/sse/connections/count` | GET | Active connections |

## 🎯 Event Types

| Event Type | Source | Description |
|------------|--------|-------------|
| `CONNECTION_ESTABLISHED` | SSE Service | Connection successful |
| `HEARTBEAT` | SSE Service | Keep-alive ping (every 15s) |
| `CART_ITEM_ADDED` | Cart Service | Product added to cart |
| `CART_ITEM_REMOVED` | Cart Service | Product removed from cart |
| `CART_UPDATED` | Cart Service | Cart modified |
| `CHECKOUT_INITIATED` | Cart Service | Checkout started |
| `CHECKOUT_COMPLETED` | Cart Service | Order placed successfully |
| `CHECKOUT_FAILED` | Cart Service | Checkout error |

## 🔧 Configuration

### dummy-off2on (application.properties)

```properties
server.port=8080
spring.data.redis.host=localhost
spring.data.redis.port=6379
redis.channel.cart-events=cart-events
redis.channel.product-events=product-events
redis.channel.checkout-events=checkout-events
```

### dummy-off2on-redis (application.properties)

```properties
server.port=8081
spring.data.redis.host=localhost
spring.data.redis.port=6379

# OTP Configuration
otp.expiration.minutes=5
otp.length=6

# SSE Configuration
sse.timeout.minutes=30
sse.keepalive.interval.seconds=15
```

## 🐳 Docker Deployment

### Build Images

```bash
# Build dummy-off2on
cd dummy-off2on
docker build -t dummy-off2on:latest .

# Build dummy-off2on-redis
cd ../dummy-off2on-redis
docker build -t dummy-off2on-redis:latest .
```

### Run with Docker Compose

```bash
cd dummy-off2on-redis
docker-compose up -d

# View logs
docker-compose logs -f

# Stop services
docker-compose down
```

## 🛠️ Development

### Project Structure

```
gfd-sse/
├── dummy-off2on/              # E-commerce backend service
│   ├── src/
│   ├── pom.xml
│   ├── Dockerfile
│   ├── README.md
│   └── postman_collection.json
├── dummy-off2on-redis/        # SSE & Redis subscriber service
│   ├── src/
│   ├── pom.xml
│   ├── Dockerfile
│   ├── docker-compose.yml
│   ├── README.md
│   ├── QUICKSTART.md
│   ├── ARCHITECTURE.md
│   ├── E2E_TESTING.md
│   └── postman_collection.json
└── README.md                   # This file
```

### Technology Stack

- **Backend**: Spring Boot 3.5.6, Java 21
- **Messaging**: Redis Pub/Sub
- **Serialization**: Jackson
- **Build**: Maven
- **Utilities**: Lombok
- **Containerization**: Docker

### Key Dependencies

```xml
<!-- Spring Boot -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-web</artifactId>
</dependency>

<!-- Redis -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-redis</artifactId>
</dependency>

<!-- Jackson for JSON -->
<dependency>
    <groupId>com.fasterxml.jackson.core</groupId>
    <artifactId>jackson-databind</artifactId>
</dependency>

<!-- Lombok -->
<dependency>
    <groupId>org.projectlombok</groupId>
    <artifactId>lombok</artifactId>
</dependency>
```

## 🔒 Security Considerations

### Current Implementation (POC)

- ✅ Secure OTP generation (SecureRandom)
- ✅ Time-based OTP expiration
- ✅ One-time OTP use
- ✅ Connection timeout
- ⚠️ CORS allows all origins
- ⚠️ No rate limiting
- ⚠️ No Redis authentication

### Production Checklist

- [ ] Restrict CORS to specific origins
- [ ] Add rate limiting for OTP generation
- [ ] Implement Redis authentication
- [ ] Use HTTPS/TLS
- [ ] Add JWT tokens for long-lived auth
- [ ] Implement proper logging (no OTP in logs)
- [ ] Add security headers
- [ ] Enable Redis encryption
- [ ] Add input sanitization
- [ ] Implement API key authentication

## 📈 Performance & Scalability

### Current Limitations

- Single instance architecture
- In-memory state (OTP, connections)
- No horizontal scaling support
- Connection bound to instance

### Scaling Strategy (Future)

1. **Redis-based Session Storage**
   - Store OTPs in Redis
   - Share connection state

2. **Load Balancer with Sticky Sessions**
   - Route user to same instance
   - Preserve SSE connections

3. **WebSocket Upgrade**
   - Bi-directional communication
   - Better scaling characteristics

4. **Kubernetes Deployment**
   - Auto-scaling
   - Health checks
   - Rolling updates

## 🐛 Troubleshooting

### Redis Connection Issues

```bash
# Check if Redis is running
redis-cli ping

# Check Redis logs
redis-cli info

# Monitor Redis commands
redis-cli monitor
```

### No Events Received

1. Verify Redis is running
2. Check userId matches in both requests
3. Verify SSE connection is active
4. Check service logs for errors

### OTP Issues

- OTPs expire after 5 minutes
- OTPs are one-time use
- Generate new OTP if expired
- Check system time is synchronized

### Port Conflicts

```bash
# Check port usage
lsof -i :8080  # dummy-off2on
lsof -i :8081  # dummy-off2on-redis
lsof -i :6379  # Redis

# Kill process on port
lsof -ti:8080 | xargs kill -9
```

## 📝 Next Steps

### Phase 1: Complete Backend (Current) ✅
- [x] Build dummy-off2on service
- [x] Implement Redis Pub/Sub
- [x] Build dummy-off2on-redis service
- [x] Implement SSE with OTP auth
- [x] Create comprehensive documentation
- [x] Add test tools and scripts

### Phase 2: Frontend Integration
- [ ] Build Front-liner PWA (Vue.js)
- [ ] Build GFD PWA (Vue.js)
- [ ] Implement OTP display UI
- [ ] Add SSE client integration
- [ ] Real-time UI updates

### Phase 3: Production Ready
- [ ] Add security enhancements
- [ ] Implement monitoring & alerting
- [ ] Add metrics & analytics
- [ ] Load testing & optimization
- [ ] CI/CD pipeline
- [ ] Production deployment

## 🤝 Contributing

This is an internal POC project. For questions or improvements:

1. Review documentation
2. Run test suites
3. Check existing issues
4. Propose changes

## 📄 License

Internal project - All rights reserved

## 👥 Team

GFD SSE POC Team

---

**Version**: 1.0.0-SNAPSHOT  
**Last Updated**: October 2025  
**Status**: Active Development (POC)

## 🎓 Learning Resources

- [Server-Sent Events (SSE) - MDN](https://developer.mozilla.org/en-US/docs/Web/API/Server-sent_events)
- [Redis Pub/Sub](https://redis.io/docs/manual/pubsub/)
- [Spring Boot SSE](https://spring.io/guides/gs/messaging-sse/)
- [OTP Best Practices](https://www.twilio.com/docs/glossary/totp)

---

**Happy Coding! 🚀**

For detailed documentation, check individual service README files.

