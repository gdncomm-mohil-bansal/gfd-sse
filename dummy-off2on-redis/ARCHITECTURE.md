# Architecture - Dummy Off2On Redis Service

## Overview

The dummy-off2on-redis service is a Spring Boot application that acts as a bridge between the dummy-off2on service and GFD PWA clients. It subscribes to Redis Pub/Sub channels and maintains Server-Sent Events (SSE) connections with authenticated clients, forwarding real-time events to them.

## System Architecture

```
┌─────────────────────────────────────────────────────────────────────┐
│                         Front-liner PWA                              │
│                         (Vue.js - Port 3000)                         │
└────────────────┬────────────────────────────────────────────────────┘
                 │
                 │ HTTP REST API
                 ▼
┌─────────────────────────────────────────────────────────────────────┐
│                        dummy-off2on Service                          │
│                       (Spring Boot - Port 8080)                      │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────────────────┐  │
│  │   Product    │  │     Cart     │  │   Redis Publisher        │  │
│  │   Service    │  │   Service    │  │   Service                │  │
│  └──────────────┘  └──────┬───────┘  └─────────┬────────────────┘  │
└────────────────────────────┼──────────────────────┼──────────────────┘
                             │                      │
                             │                      │ Publish Events
                             │                      ▼
                             │           ┌──────────────────────────┐
                             │           │     Redis Pub/Sub        │
                             │           │  ┌────────────────────┐  │
                             │           │  │  cart-events       │  │
                             │           │  │  checkout-events   │  │
                             │           │  │  product-events    │  │
                             │           │  └────────────────────┘  │
                             │           └───────────┬──────────────┘
                             │                       │
                             │                       │ Subscribe
                             │                       ▼
                             │           ┌──────────────────────────────────┐
                             │           │  dummy-off2on-redis Service      │
                             │           │    (Spring Boot - Port 8081)     │
                             │           │  ┌────────────────────────────┐  │
                             │           │  │  Redis Subscriber Service  │  │
                             │           │  └──────────┬─────────────────┘  │
                             │           │             │                    │
                             │           │             ▼                    │
                             │           │  ┌────────────────────────────┐  │
                             │           │  │    SSE Service             │  │
                             │           │  │  - Connection Management   │  │
                             │           │  │  - Event Broadcasting      │  │
                             │           │  │  - Heartbeat Mechanism     │  │
                             │           │  └──────────┬─────────────────┘  │
                             │           │             │                    │
                             │           │  ┌──────────▼─────────────────┐  │
                             │           │  │    OTP Service             │  │
                             │           │  │  - Generation              │  │
                             │           │  │  - Validation              │  │
                             │           │  │  - Expiration Management   │  │
                             │           │  └────────────────────────────┘  │
                             │           └───────────┬──────────────────────┘
                             │                       │
                             │                       │ SSE Stream
                             │                       ▼
┌────────────────────────────┴───────────────────────────────────────────┐
│                              GFD PWA                                    │
│                          (Vue.js - TBD)                                 │
│  ┌──────────────────────────────────────────────────────────────────┐  │
│  │  1. Request OTP                                                  │  │
│  │  2. Display OTP to User                                          │  │
│  │  3. Connect to SSE with OTP                                      │  │
│  │  4. Receive Real-time Events                                     │  │
│  └──────────────────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────────────────┘
```

## Component Details

### 1. OTP Service

**Responsibilities:**
- Generate secure 6-digit OTPs
- Store OTPs with expiration timestamps
- Validate OTPs for SSE connection authentication
- Invalidate OTPs after successful use (one-time use)
- Clean up expired OTPs

**Key Features:**
- Uses `SecureRandom` for cryptographically secure OTP generation
- Stores OTP mappings in-memory using `ConcurrentHashMap`
- Bi-directional mapping: userId ↔ OTP for fast lookups
- Configurable expiration time (default: 5 minutes)
- Thread-safe implementation

**Security:**
- One-time use: OTP is invalidated after successful SSE connection
- Time-based expiration
- Secure random number generation
- No plaintext OTP storage in logs (in production)

### 2. SSE Service

**Responsibilities:**
- Create and manage SSE connections
- Send events to specific users
- Broadcast events to all connected users
- Handle connection lifecycle (completion, timeout, error)
- Send heartbeat messages to detect dead connections
- Track active connections

**Key Features:**
- Long-lived HTTP connections with configurable timeout (default: 30 minutes)
- Automatic cleanup of dead connections
- Heartbeat mechanism (every 15 seconds)
- Per-user connection tracking
- Event routing based on userId

**Connection Lifecycle:**
```
1. OTP Validation
   ↓
2. Create SseEmitter
   ↓
3. Register Callbacks (onComplete, onTimeout, onError)
   ↓
4. Store in Connection Map
   ↓
5. Send CONNECTION_ESTABLISHED Event
   ↓
6. Handle Events from Redis
   ↓
7. Send Heartbeats
   ↓
8. On Close/Error: Cleanup
```

### 3. Redis Subscriber Service

**Responsibilities:**
- Subscribe to Redis Pub/Sub channels
- Parse incoming JSON event messages
- Route events to appropriate users via SSE
- Handle deserialization errors gracefully

**Subscribed Channels:**
- `cart-events`: Cart-related events (add, remove, update)
- `checkout-events`: Checkout events (initiated, completed, failed)
- `product-events`: Product-related events (viewed, etc.)

**Event Flow:**
```
Redis Pub/Sub
   ↓
RedisMessageListenerContainer
   ↓
MessageListenerAdapter
   ↓
RedisSubscriberService.handleXxxEvent()
   ↓
Parse JSON → CartEvent Object
   ↓
Extract userId from event
   ↓
Check if user has active SSE connection
   ↓
SSEService.sendEventToUser()
   ↓
Client receives event via SSE
```

## Data Models

### CartEvent

```java
{
  "eventId": "uuid",
  "eventType": "CART_ITEM_ADDED",
  "userId": "user123",
  "timestamp": 1698765432000,
  "cartItems": [...],
  "totalAmount": 99.99,
  "totalItems": 3,
  "message": "Added 2 x Product Name to cart",
  "metadata": {...}
}
```

### OTP Data

```java
{
  "otp": "123456",
  "userId": "user123",
  "expiresAt": 1698765432000
}
```

## API Flow Diagrams

### OTP Generation Flow

```
GFD PWA                     dummy-off2on-redis
   │                               │
   ├──POST /api/otp/generate──────▶│
   │  { userId, deviceInfo }       │
   │                               │
   │                          OTPService
   │                          ┌─────────┐
   │                          │Generate │
   │                          │6-digit  │
   │                          │OTP      │
   │                          └────┬────┘
   │                               │
   │                          Store with
   │                          expiration
   │                               │
   │◀─────Response─────────────────┤
   │  { otp, expiresAt, ... }      │
   │                               │
```

### SSE Connection Flow

```
GFD PWA                     dummy-off2on-redis
   │                               │
   ├──GET /api/sse/connect─────────▶│
   │  ?userId=xxx&otp=123456       │
   │                               │
   │                          Validate OTP
   │                          ┌─────────┐
   │                          │Valid?   │
   │                          └────┬────┘
   │                               │
   │                          Create SSE
   │                          Emitter
   │                               │
   │                          Invalidate OTP
   │                          (one-time use)
   │                               │
   │◀─────SSE Stream────────────────┤
   │  (Connection maintained)      │
   │                               │
   │◀─────CONNECTION_ESTABLISHED───┤
   │                               │
   │◀─────HEARTBEAT (every 15s)────┤
   │                               │
```

### Event Broadcasting Flow

```
dummy-off2on          Redis           dummy-off2on-redis        GFD PWA
     │                  │                     │                    │
     ├─Publish Event───▶│                     │                    │
     │                  │                     │                    │
     │                  ├─Notify Subscribers─▶│                    │
     │                  │                     │                    │
     │                  │              Parse & Route               │
     │                  │                     │                    │
     │                  │              Check userId                │
     │                  │              has connection?             │
     │                  │                     │                    │
     │                  │                     ├─Send via SSE──────▶│
     │                  │                     │                    │
     │                  │                     │              Display Event
```

## Configuration

### Redis Configuration

```properties
spring.data.redis.host=localhost
spring.data.redis.port=6379
spring.data.redis.timeout=60000

redis.channel.cart-events=cart-events
redis.channel.product-events=product-events
redis.channel.checkout-events=checkout-events
```

### OTP Configuration

```properties
otp.expiration.minutes=5    # OTP valid for 5 minutes
otp.length=6                 # 6-digit OTP
```

### SSE Configuration

```properties
sse.timeout.minutes=30                  # SSE connection timeout
sse.keepalive.interval.seconds=15       # Heartbeat interval
```

## Threading Model

### Non-Blocking I/O
While not using Spring WebFlux, the service uses traditional Spring MVC with efficient thread management:

1. **HTTP Thread Pool**: Handles incoming HTTP requests (OTP, SSE)
2. **Redis Listener Thread Pool**: Processes Redis messages
3. **Scheduled Thread Pool**: Handles heartbeat and cleanup tasks

### Concurrency Considerations

- `ConcurrentHashMap` for thread-safe OTP and SSE connection storage
- Synchronized access to SSE emitters
- Thread-safe event broadcasting
- Atomic operations for connection management

## Scalability Considerations

### Current Limitations (In-Memory Storage)

1. **OTP Storage**: In-memory, lost on restart
2. **SSE Connections**: Bound to single instance
3. **Horizontal Scaling**: Not directly supported

### Future Improvements

1. **Redis-based OTP Storage**: Persistent, shared across instances
2. **Session Affinity**: Route users to same instance
3. **Redis Streams**: More reliable event delivery
4. **WebSocket Alternative**: For bi-directional communication
5. **Connection Pool Management**: Better resource utilization

## Security Considerations

1. **OTP Security**:
   - Secure random generation
   - Time-based expiration
   - One-time use
   - No logging of OTPs in production

2. **CORS Configuration**:
   - Currently allows all origins (*)
   - Should be restricted in production

3. **SSE Connection Security**:
   - OTP-based authentication
   - Connection timeout
   - Automatic cleanup of dead connections

4. **Data Validation**:
   - Input validation for all endpoints
   - Error handling without exposing internal details

## Monitoring & Observability

### Health Endpoints

- `/api/health`: Service health status
- `/api/sse/connections/count`: Active connection count
- `/api/sse/status/{userId}`: User connection status

### Logging

- OTP generation and validation
- SSE connection lifecycle
- Redis event processing
- Error tracking and debugging

### Metrics (Future)

- OTP generation rate
- OTP validation success/failure rate
- SSE connection duration
- Event processing rate
- Connection churn rate

## Error Handling

1. **OTP Errors**:
   - Invalid OTP: Return 401 Unauthorized
   - Expired OTP: Return 401 with message
   - Missing parameters: Return 400 Bad Request

2. **SSE Errors**:
   - Connection timeout: Automatic cleanup
   - Client disconnect: Graceful cleanup
   - Send failure: Remove dead connection

3. **Redis Errors**:
   - Deserialization errors: Log and skip event
   - Connection errors: Automatic reconnection
   - Channel errors: Continue processing other channels

## Testing Strategy

1. **Unit Tests**: Service-level logic
2. **Integration Tests**: Redis and SSE integration
3. **Manual Testing**: Postman collection and shell scripts
4. **Load Testing**: Multiple concurrent connections
5. **End-to-End Testing**: Full flow with dummy-off2on

## Future Enhancements

1. **User Authentication**: Replace OTP with JWT tokens
2. **Event Filtering**: Allow users to subscribe to specific event types
3. **Event History**: Store recent events for new connections
4. **Push Notifications**: Fallback when SSE not available
5. **Analytics**: Track user behavior and event patterns
6. **Rate Limiting**: Prevent abuse of OTP generation
7. **Multi-tenancy**: Support multiple organizations
8. **Event Replay**: Allow replay of missed events

