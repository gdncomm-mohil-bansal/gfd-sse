# Project Summary - Dummy Off2On Redis

## Overview

The **dummy-off2on-redis** service is a Spring Boot application that acts as a Redis Pub/Sub subscriber and SSE (Server-Sent Events) broadcaster. It bridges the gap between the backend e-commerce service (dummy-off2on) and the GFD PWA frontend by maintaining real-time, authenticated connections.

## Key Features

### 1. **OTP-Based Authentication** 🔐
- Generates secure 6-digit OTPs
- Time-based expiration (5 minutes)
- One-time use (invalidated after successful connection)
- Similar to Apple Screen Cast pairing mechanism

### 2. **Server-Sent Events (SSE)** 📡
- Long-lived HTTP connections for real-time updates
- Automatic heartbeat every 15 seconds
- Per-user connection management
- Graceful error handling and reconnection

### 3. **Redis Pub/Sub Integration** 🔄
- Subscribes to multiple Redis channels
- Real-time event reception from dummy-off2on
- Event routing to correct user connections
- Supports cart, checkout, and product events

### 4. **Robust Connection Management** 🛡️
- Automatic dead connection cleanup
- Connection timeout handling (30 minutes default)
- Multiple concurrent user support
- Connection status monitoring

## Architecture Components

### Services

1. **OTPService**
   - OTP generation with SecureRandom
   - In-memory storage with ConcurrentHashMap
   - Expiration management
   - Validation logic

2. **SSEService**
   - SseEmitter lifecycle management
   - Event broadcasting
   - Heartbeat mechanism
   - Connection tracking

3. **RedisSubscriberService**
   - Redis message listening
   - JSON deserialization
   - Event routing
   - Error handling

### Controllers

1. **OTPController** - `/api/otp/*`
   - Generate OTP
   - Validate OTP
   - Invalidate OTP

2. **SSEController** - `/api/sse/*`
   - Establish SSE connection
   - Disconnect
   - Check status
   - Connection count

3. **HealthController** - `/api/*`
   - Health check
   - Welcome message

## Technology Stack

- **Framework**: Spring Boot 3.5.6
- **Java**: 21
- **Redis**: Spring Data Redis
- **Serialization**: Jackson
- **Build Tool**: Maven
- **Lombok**: Code generation

## Configuration

### Application Properties

```properties
# Server
server.port=8081

# Redis
spring.data.redis.host=localhost
spring.data.redis.port=6379

# OTP
otp.expiration.minutes=5
otp.length=6

# SSE
sse.timeout.minutes=30
sse.keepalive.interval.seconds=15
```

## API Endpoints

### Health & Status
- `GET /api/health` - Service health
- `GET /api/` - Welcome message

### OTP Management
- `POST /api/otp/generate` - Generate OTP
- `POST /api/otp/validate` - Validate OTP
- `POST /api/otp/invalidate/{userId}` - Invalidate OTP

### SSE Connection
- `GET /api/sse/connect?userId={userId}&otp={otp}` - Connect
- `POST /api/sse/disconnect/{userId}` - Disconnect
- `GET /api/sse/status/{userId}` - Check status
- `GET /api/sse/connections/count` - Active connections

## Event Types

- `CONNECTION_ESTABLISHED` - SSE connected
- `HEARTBEAT` - Keep-alive ping
- `CART_ITEM_ADDED` - Product added to cart
- `CART_ITEM_REMOVED` - Product removed
- `CART_UPDATED` - Cart modified
- `CHECKOUT_COMPLETED` - Order placed
- `CHECKOUT_FAILED` - Checkout error

## Data Flow

```
1. GFD PWA requests OTP
   ↓
2. OTP generated and returned
   ↓
3. User enters OTP in Front-liner PWA
   ↓
4. Front-liner PWA connects to SSE with OTP
   ↓
5. OTP validated and SSE connection established
   ↓
6. OTP invalidated (one-time use)
   ↓
7. User actions trigger events in dummy-off2on
   ↓
8. Events published to Redis
   ↓
9. dummy-off2on-redis receives events
   ↓
10. Events forwarded to correct SSE connection
    ↓
11. Front-liner PWA receives real-time updates
```

## Security Considerations

### Implemented
- ✅ Secure OTP generation (SecureRandom)
- ✅ Time-based OTP expiration
- ✅ One-time OTP use
- ✅ Connection timeout
- ✅ Input validation
- ✅ Error handling without info disclosure

### To Implement (Production)
- 🔲 CORS restriction (currently allows all origins)
- 🔲 Rate limiting on OTP generation
- 🔲 JWT-based authentication
- 🔲 HTTPS enforcement
- 🔲 OTP logging restrictions
- 🔲 Redis authentication

## Testing

### Available Test Tools
1. **Postman Collection**: `postman_collection.json`
2. **Shell Scripts**:
   - `test-api.sh` - Comprehensive API tests
   - `test-sse-connection.sh` - SSE connection test
3. **Documentation**:
   - `QUICKSTART.md` - 5-minute setup guide
   - `E2E_TESTING.md` - End-to-end test scenarios
   - `ARCHITECTURE.md` - Detailed architecture

### Test Coverage
- Health checks
- OTP generation and validation
- SSE connection establishment
- Event reception and forwarding
- Multiple user scenarios
- Load testing

## Performance Characteristics

### Current Implementation
- **Concurrent Connections**: Limited by JVM threads
- **OTP Storage**: In-memory (ConcurrentHashMap)
- **SSE Connections**: In-memory per instance
- **Event Processing**: Single-threaded per channel

### Scalability Limitations
- Single instance design
- In-memory state (not shared across instances)
- No session persistence
- Connection bound to instance

### Future Improvements
- Redis-based OTP storage
- Sticky sessions for SSE
- Horizontal scaling with load balancer
- Connection pool management
- WebSocket upgrade option

## Monitoring & Observability

### Available Metrics
- Active connection count
- User connection status
- Service health status

### Logging
- DEBUG level for development
- OTP operations logged
- SSE lifecycle events
- Redis event processing
- Error tracking

### Future Enhancements
- Prometheus metrics
- Distributed tracing
- Custom dashboards
- Alert configuration

## Dependencies

### Runtime
- spring-boot-starter-web
- spring-boot-starter-data-redis
- jackson-databind
- lombok

### Development
- spring-boot-devtools
- spring-boot-starter-test

## Build & Deployment

### Local Development
```bash
mvn spring-boot:run
```

### Production Build
```bash
mvn clean package
java -jar target/dummy-off2on-redis-*.jar
```

### Docker
```bash
docker build -t dummy-off2on-redis .
docker run -p 8081:8081 dummy-off2on-redis
```

### Docker Compose
```bash
docker-compose up -d
```

## Project Structure

```
dummy-off2on-redis/
├── src/main/java/com/gfd_sse/dummyoff2onredis/
│   ├── config/              # Configuration classes
│   │   ├── CorsConfig.java
│   │   └── RedisConfig.java
│   ├── controller/          # REST controllers
│   │   ├── HealthController.java
│   │   ├── OTPController.java
│   │   └── SSEController.java
│   ├── dto/                 # Data Transfer Objects
│   │   ├── ApiResponse.java
│   │   ├── OTPGenerationRequest.java
│   │   ├── OTPGenerationResponse.java
│   │   ├── OTPValidationRequest.java
│   │   └── OTPValidationResponse.java
│   ├── event/               # Event models
│   │   ├── CartEvent.java
│   │   └── EventType.java
│   ├── exception/           # Exception handling
│   │   └── GlobalExceptionHandler.java
│   ├── model/               # Domain models
│   │   └── CartItem.java
│   ├── service/             # Business logic
│   │   ├── OTPService.java
│   │   ├── RedisSubscriberService.java
│   │   └── SSEService.java
│   └── DummyOff2onRedisApplication.java
├── src/main/resources/
│   └── application.properties
├── src/test/
│   └── ...
├── pom.xml
├── Dockerfile
├── docker-compose.yml
├── README.md
├── QUICKSTART.md
├── ARCHITECTURE.md
├── E2E_TESTING.md
├── PROJECT_SUMMARY.md
├── postman_collection.json
├── test-api.sh
└── test-sse-connection.sh
```

## Integration Points

### Upstream (Receives From)
- **dummy-off2on** via Redis Pub/Sub
  - Channels: cart-events, checkout-events, product-events
  - Format: JSON CartEvent objects

### Downstream (Sends To)
- **GFD PWA** via SSE
  - Protocol: Server-Sent Events
  - Authentication: OTP-based

### Infrastructure
- **Redis**: Pub/Sub messaging broker
  - Host: localhost:6379
  - No authentication (configure for production)

## Known Limitations

1. **In-Memory State**: OTPs and connections lost on restart
2. **Single Instance**: Cannot scale horizontally without session affinity
3. **No Persistence**: Event history not stored
4. **CORS**: Currently allows all origins
5. **No Rate Limiting**: OTP generation unrestricted
6. **Thread Model**: Traditional blocking I/O (not reactive)

## Roadmap

### Phase 1 (Current) ✅
- [x] OTP-based authentication
- [x] SSE connection management
- [x] Redis Pub/Sub integration
- [x] Basic event routing
- [x] Health monitoring
- [x] Test tools and documentation

### Phase 2 (Next)
- [ ] Redis-based OTP storage
- [ ] Rate limiting
- [ ] Enhanced security (CORS, HTTPS)
- [ ] Metrics and monitoring
- [ ] Load testing and optimization

### Phase 3 (Future)
- [ ] JWT authentication
- [ ] Event history storage
- [ ] WebSocket support
- [ ] Horizontal scaling
- [ ] Advanced analytics
- [ ] Multi-tenancy

## Support & Maintenance

### Documentation
- README.md - Setup and usage
- QUICKSTART.md - Quick start guide
- ARCHITECTURE.md - Detailed architecture
- E2E_TESTING.md - Testing guide
- PROJECT_SUMMARY.md - This document

### Testing
- Postman collection for API testing
- Shell scripts for automation
- Manual test scenarios

### Logs
- Console output (development)
- Configurable logging levels
- Error tracking

## License & Contributors

- License: Internal project
- Team: GFD SSE POC Team
- Version: 1.0.0-SNAPSHOT

---

**Last Updated**: October 2025  
**Status**: Active Development  
**Environment**: Development/POC

