# Dummy Off2On Redis - SSE Subscriber Service

This is the Redis subscriber service that maintains SSE (Server-Sent Events) connections with GFD PWA clients. It subscribes to Redis Pub/Sub channels and forwards real-time events to connected clients using OTP-based authentication.

## Features

- **OTP-Based Authentication**: 6-digit OTP system for secure connection establishment
- **SSE Connection Management**: Maintains long-lived SSE connections with GFD PWA clients
- **Redis Pub/Sub Integration**: Subscribes to cart and checkout events from dummy-off2on service
- **Real-time Event Forwarding**: Pushes events to connected clients in real-time
- **Connection Monitoring**: Heartbeat mechanism to detect and remove dead connections
- **Automatic Cleanup**: Periodic cleanup of expired OTPs and dead connections

## Architecture

```
dummy-off2on (Port 8080)
    ↓ (publishes events)
Redis Pub/Sub
    ↓ (subscribes to events)
dummy-off2on-redis (Port 8081)
    ↓ (SSE connection)
GFD PWA (Client)
```

## Prerequisites

- Java 21
- Maven 3.x
- Redis Server running on localhost:6379
- dummy-off2on service running on port 8080

## Setup

### 1. Start Redis Server

```bash
# On macOS
brew services start redis

# Or using Docker
docker run -d -p 6379:6379 redis:latest

# Verify Redis is running
redis-cli ping
# Should return: PONG
```

### 2. Build the Project

```bash
cd dummy-off2on-redis
mvn clean install
```

### 3. Run the Application

```bash
mvn spring-boot:run
```

The service will start on port 8081.

## API Endpoints

### Health & Status

- **GET** `/api/health` - Health check endpoint
- **GET** `/api/` - Welcome message

### OTP Management

- **POST** `/api/otp/generate` - Generate OTP for a user
  ```json
  {
    "userId": "user123",
    "deviceInfo": "GFD PWA - Chrome on Mac"
  }
  ```

- **POST** `/api/otp/validate` - Validate an OTP
  ```json
  {
    "userId": "user123",
    "otp": "123456"
  }
  ```

- **POST** `/api/otp/invalidate/{userId}` - Invalidate OTP for a user

### SSE Connection

- **GET** `/api/sse/connect?userId={userId}&otp={otp}` - Establish SSE connection with OTP
  - Headers: `Accept: text/event-stream`
  - Returns: SSE stream

- **POST** `/api/sse/disconnect/{userId}` - Close SSE connection
- **GET** `/api/sse/status/{userId}` - Check connection status
- **GET** `/api/sse/connections/count` - Get active connection count

## Testing Flow

### 1. Using Postman

1. Import `postman_collection.json` into Postman
2. Run the "Integration Test - Full Flow" folder:
   - Step 1: Generate OTP
   - Step 2: Validate OTP
   - Step 3: Connect to SSE

### 2. Using cURL

#### Step 1: Generate OTP
```bash
curl -X POST http://localhost:8081/api/otp/generate \
  -H "Content-Type: application/json" \
  -d '{"userId":"user123","deviceInfo":"Test Device"}'
```

Response:
```json
{
  "success": true,
  "otp": "123456",
  "userId": "user123",
  "expiresAt": 1698765432000,
  "message": "OTP generated successfully. Valid for 5 minutes."
}
```

#### Step 2: Connect to SSE (use OTP from Step 1)
```bash
curl -N -H "Accept: text/event-stream" \
  "http://localhost:8081/api/sse/connect?userId=user123&otp=123456"
```

You should see:
- Initial connection established event
- Heartbeat events every 15 seconds
- Real-time cart/checkout events when actions occur on dummy-off2on

### 3. End-to-End Testing

#### Terminal 1: Start dummy-off2on-redis and connect SSE
```bash
# Generate OTP
curl -X POST http://localhost:8081/api/otp/generate \
  -H "Content-Type: application/json" \
  -d '{"userId":"user123","deviceInfo":"Test Device"}'

# Connect to SSE (replace OTP_FROM_ABOVE with actual OTP)
curl -N -H "Accept: text/event-stream" \
  "http://localhost:8081/api/sse/connect?userId=user123&otp=OTP_FROM_ABOVE"
```

#### Terminal 2: Trigger events from dummy-off2on
```bash
# Add product to cart (this will publish event to Redis)
curl -X POST http://localhost:8080/api/cart/add \
  -H "Content-Type: application/json" \
  -d '{
    "userId": "user123",
    "productId": 1,
    "quantity": 2
  }'
```

You should see the cart event appear in Terminal 1's SSE stream!

## Event Types

The service forwards the following event types:

- `CONNECTION_ESTABLISHED` - SSE connection established
- `HEARTBEAT` - Keep-alive heartbeat
- `CART_ITEM_ADDED` - Product added to cart
- `CART_ITEM_REMOVED` - Product removed from cart
- `CART_UPDATED` - Cart updated
- `CHECKOUT_COMPLETED` - Checkout successful
- `CHECKOUT_FAILED` - Checkout failed

## Configuration

Edit `src/main/resources/application.properties`:

```properties
# Server port
server.port=8081

# Redis configuration
spring.data.redis.host=localhost
spring.data.redis.port=6379

# OTP settings
otp.expiration.minutes=5
otp.length=6

# SSE settings
sse.timeout.minutes=30
sse.keepalive.interval.seconds=15
```

## OTP Security Flow

1. **GFD PWA** requests OTP from `/api/otp/generate`
2. **Service** generates 6-digit OTP, stores it with expiration (5 minutes)
3. **GFD PWA** displays OTP to user (similar to Apple Screen Cast)
4. **Front-liner** enters OTP in their PWA
5. **Front-liner PWA** connects to SSE using `/api/sse/connect?userId={userId}&otp={otp}`
6. **Service** validates OTP and establishes SSE connection
7. **Service** invalidates OTP after successful connection (one-time use)
8. Events are pushed to Front-liner PWA in real-time

## Troubleshooting

### Redis Connection Issues
```bash
# Check if Redis is running
redis-cli ping

# Check Redis logs
redis-cli info

# Test Redis pub/sub manually
redis-cli
> SUBSCRIBE cart-events
```

### No Events Received
- Ensure dummy-off2on service is running on port 8080
- Verify Redis channels match between both services
- Check logs for subscription errors
- Ensure userId in cart operations matches SSE connection userId

### OTP Issues
- OTPs expire after 5 minutes
- OTPs are one-time use (invalidated after successful connection)
- Check server logs for validation errors

## Monitoring

Check active connections:
```bash
curl http://localhost:8081/api/sse/connections/count
```

Check service health:
```bash
curl http://localhost:8081/api/health
```

Check specific user status:
```bash
curl http://localhost:8081/api/sse/status/user123
```

## Logs

The service provides detailed logging:
- OTP generation and validation
- SSE connection establishment and termination
- Redis event reception and forwarding
- Heartbeat and cleanup operations

Check logs in console output or configure file logging in `application.properties`.

## Next Steps

1. Integrate with GFD PWA for real SSE connection
2. Add more event types (product views, etc.)
3. Add user session management
4. Implement rate limiting for OTP generation
5. Add metrics and monitoring

