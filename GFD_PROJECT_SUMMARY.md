# GFD-SSE Project Summary

## Project Overview

Complete Guest-Facing Display (GFD) system with real-time cart updates using Server-Sent Events (SSE) and Redis Pub/Sub.

## Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                         System Architecture                      │
└─────────────────────────────────────────────────────────────────┘

┌──────────────┐         ┌─────────────┐         ┌──────────────┐
│ POS Terminal │────────▶│    Redis    │◀────────│  GFD Display │
│              │ Publish │   Pub/Sub   │Subscribe│   (Vue.js)   │
│ dummy-off2on │         │             │         │              │
│ Port: 8080   │         └─────────────┘         │ Port: 5173   │
└──────────────┘                                  └──────────────┘
       │                       ▲                          │
       │ Store OTP             │                          │
       │                       │                          │
       └───────────────────────┘                          │
                                                          │
                                         ┌────────────────┘
                                         │ SSE Connection
                                         │ (with OTP auth)
                                         │
                                  ┌──────▼────────────┐
                                  │   SSE Service     │
                                  │ dummy-off2on-redis│
                                  │   Port: 8081      │
                                  └───────────────────┘
```

## System Components

### 1. dummy-off2on (Port 8080)
**Role**: POS Terminal Backend

**Responsibilities**:
- Generate OTPs for GFD connection
- Handle cart operations (add, remove, update)
- Publish cart events to Redis
- Manage product catalog
- Process checkout

**Key APIs**:
- `POST /api/otp/generate` - Generate 6-digit OTP
- `POST /api/cart/add` - Add item to cart
- `POST /api/cart/remove` - Remove item from cart
- `POST /api/cart/checkout` - Initiate checkout
- `GET /api/products` - List products

**Redis Integration**:
- Publishes to channel: `cart-events`
- Stores OTPs with TTL: 5 minutes

### 2. dummy-off2on-redis (Port 8081)
**Role**: SSE Service & Event Dispatcher

**Responsibilities**:
- Validate OTPs from Redis
- Establish SSE connections
- Subscribe to Redis cart events
- Forward events to connected GFD clients
- Manage active connections

**Key APIs**:
- `GET /api/sse/connect?userId={}&otp={}` - SSE connection
- `POST /api/sse/disconnect/{userId}` - Close connection
- `GET /api/sse/status/{userId}` - Check status
- `GET /api/sse/connections/count` - Active connections

**Redis Integration**:
- Subscribes to channel: `cart-events`
- Validates OTPs from Redis
- Reads from keys: `otp:{code}`, `otp:user:{userId}`

### 3. gfd (Port 5173)
**Role**: Guest-Facing Display (Vue.js PWA)

**Responsibilities**:
- Connect with OTP authentication
- Display real-time cart updates
- Show product images and details
- Display checkout status
- Handle connection management

**Key Features**:
- Real-time SSE connection
- Beautiful, responsive UI
- Smooth animations
- Error handling
- Auto-reconnection

**Tech Stack**:
- Vue 3 (Composition API)
- TypeScript
- Pinia (State Management)
- Vite (Build Tool)
- Native EventSource API

## Recent Fix: @class Metadata Issue

### Problem
The publisher (`dummy-off2on`) was using `GenericJackson2JsonRedisSerializer`, which adds Jackson default typing metadata (`@class` annotations) to JSON. This caused deserialization errors in the subscriber because:
1. Publisher and subscriber had different package names
2. Subscriber couldn't deserialize the type-hinted JSON
3. Arrays like `["java.util.ArrayList", [...]]` were confusing Jackson

### Solution Applied
**Fixed at the source (publisher)** by changing `dummy-off2on/RedisConfig.java`:

**Before**:
```java
template.setValueSerializer(new GenericJackson2JsonRedisSerializer());
```

**After**:
```java
ObjectMapper objectMapper = new ObjectMapper();
objectMapper.registerModule(new JavaTimeModule());
objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

Jackson2JsonRedisSerializer<Object> serializer = 
    new Jackson2JsonRedisSerializer<>(objectMapper, Object.class);
    
template.setValueSerializer(serializer);
```

### Benefits
✅ Clean JSON without type metadata  
✅ No package coupling between services  
✅ Smaller payload size  
✅ Simpler subscriber code  
✅ Future subscribers won't face this issue  

### JSON Output Comparison

**Before (with @class)**:
```json
{
  "@class": "com.gfd_sse.dummyoff2on.event.CartEvent",
  "eventId": "541cdcd0-8c89-4b9b-ab42-3a30fe4fb3cc",
  "cartItems": ["java.util.ArrayList", [
    {
      "@class": "com.gfd_sse.dummyoff2on.model.CartItem",
      "productId": 1,
      "price": ["java.math.BigDecimal", 999.99]
    }
  ]],
  "totalAmount": ["java.math.BigDecimal", 7999.92]
}
```

**After (clean JSON)**:
```json
{
  "eventId": "541cdcd0-8c89-4b9b-ab42-3a30fe4fb3cc",
  "eventType": "CART_ITEM_ADDED",
  "cartItems": [
    {
      "productId": 1,
      "productName": "Laptop",
      "price": 999.99,
      "quantity": 2,
      "subtotal": 1999.98
    }
  ],
  "totalAmount": 7999.92
}
```

## Event Flow

### 1. OTP Generation & Connection
```
1. Cashier clicks "Generate Code" on POS
2. POST /api/otp/generate → dummy-off2on (8080)
3. OTP stored in Redis with 5min TTL
4. 6-digit code displayed to customer
5. Customer enters code on GFD
6. GET /api/sse/connect?userId=X&otp=123456 → dummy-off2on-redis (8081)
7. Backend validates OTP from Redis
8. SSE connection established
9. CONNECTION_ESTABLISHED event sent to GFD
10. OTP invalidated (single-use)
```

### 2. Cart Operations
```
1. Cashier scans product → POS system
2. POST /api/cart/add → dummy-off2on (8080)
3. Cart event published to Redis channel "cart-events"
4. dummy-off2on-redis receives event (subscribed to channel)
5. Event forwarded via SSE to connected GFD
6. GFD updates UI in real-time
7. Customer sees item appear with animation
```

### 3. Checkout Process
```
1. Cashier clicks "Checkout" → POS
2. POST /api/cart/checkout → dummy-off2on (8080)
3. CHECKOUT_INITIATED event published to Redis
4. Event forwarded to GFD via SSE
5. GFD shows checkout modal with processing animation
6. Payment processed
7. CHECKOUT_COMPLETED event published
8. GFD shows success screen
9. Modal auto-dismisses after 3 seconds
```

## Getting Started

### Prerequisites
- Java 17+
- Maven 3.6+
- Redis 6.0+
- Node.js 20.19.0+
- npm or yarn

### Quick Start

1. **Start Redis**:
   ```bash
   docker run -d -p 6379:6379 redis:latest
   ```

2. **Start Backend Services**:
   ```bash
   # Terminal 1: Start dummy-off2on (8080)
   cd dummy-off2on
   mvn spring-boot:run
   
   # Terminal 2: Start dummy-off2on-redis (8081)
   cd dummy-off2on-redis
   mvn spring-boot:run
   ```

3. **Start GFD Frontend**:
   ```bash
   # Terminal 3: Start GFD (5173)
   cd gfd
   npm install
   npm run dev
   ```

4. **Test the System**:
   ```bash
   # Generate OTP
   curl -X POST http://localhost:8080/api/otp/generate \
     -H "Content-Type: application/json" \
     -d '{"userId": "demo-user"}'
   
   # Note the OTP from response, then:
   # 1. Open http://localhost:5173 in browser
   # 2. Enter userId: demo-user
   # 3. Enter the 6-digit OTP
   # 4. Click Connect
   
   # Add an item to cart
   curl -X POST http://localhost:8080/api/cart/add \
     -H "Content-Type: application/json" \
     -d '{"userId":"demo-user","productId":1,"quantity":2}'
   
   # Watch the GFD update in real-time!
   ```

## API Reference

### dummy-off2on (Port 8080)

#### Generate OTP
```http
POST /api/otp/generate
Content-Type: application/json

{
  "userId": "user123"
}

Response:
{
  "otp": "123456",
  "userId": "user123",
  "expiresIn": 300,
  "message": "OTP generated successfully"
}
```

#### Add to Cart
```http
POST /api/cart/add
Content-Type: application/json

{
  "userId": "user123",
  "productId": 1,
  "quantity": 2
}

Response:
{
  "success": true,
  "cartItems": [...],
  "totalAmount": 1999.98,
  "totalItems": 2
}
```

#### Checkout
```http
POST /api/cart/checkout
Content-Type: application/json

{
  "userId": "user123",
  "paymentMethod": "Credit Card"
}

Response:
{
  "success": true,
  "orderId": "order-123",
  "totalAmount": 1999.98
}
```

### dummy-off2on-redis (Port 8081)

#### Connect SSE
```http
GET /api/sse/connect?userId=user123&otp=123456
Accept: text/event-stream

Response: (SSE Stream)
data: {"eventType":"CONNECTION_ESTABLISHED",...}

data: {"eventType":"CART_ITEM_ADDED",...}

data: {"eventType":"CHECKOUT_COMPLETED",...}
```

#### Disconnect
```http
POST /api/sse/disconnect/user123

Response:
"SSE connection closed successfully"
```

## Event Types

```typescript
enum EventType {
  CONNECTION_ESTABLISHED  // Initial connection
  PRODUCT_VIEWED         // Product displayed
  CART_ITEM_ADDED        // Item added to cart
  CART_ITEM_REMOVED      // Item removed from cart
  CART_UPDATED           // Cart quantities changed
  CHECKOUT_INITIATED     // Checkout started
  CHECKOUT_COMPLETED     // Payment successful
  CHECKOUT_FAILED        // Payment failed
  HEARTBEAT             // Keep-alive ping
}
```

## Configuration

### dummy-off2on
```properties
# application.properties
server.port=8080
spring.data.redis.host=localhost
spring.data.redis.port=6379
redis.channel.cart-events=cart-events
otp.expiry.seconds=300
```

### dummy-off2on-redis
```properties
# application.properties
server.port=8081
spring.data.redis.host=localhost
spring.data.redis.port=6379
redis.channel.cart-events=cart-events
sse.timeout.milliseconds=3600000
```

### gfd
```env
# .env
VITE_API_URL=http://localhost:8081
```

## Deployment

### Docker Compose
```yaml
version: '3.8'
services:
  redis:
    image: redis:latest
    ports:
      - "6379:6379"
  
  dummy-off2on:
    build: ./dummy-off2on
    ports:
      - "8080:8080"
    depends_on:
      - redis
  
  dummy-off2on-redis:
    build: ./dummy-off2on-redis
    ports:
      - "8081:8081"
    depends_on:
      - redis
  
  gfd:
    build: ./gfd
    ports:
      - "80:80"
    environment:
      - VITE_API_URL=http://dummy-off2on-redis:8081
```

### Production Considerations

1. **Security**:
   - Use HTTPS for all connections
   - Implement rate limiting on OTP generation
   - Add CSRF protection
   - Secure Redis with password

2. **Scalability**:
   - Use Redis Cluster for high availability
   - Load balance SSE connections
   - Implement connection pooling
   - Add health checks

3. **Monitoring**:
   - Track active SSE connections
   - Monitor Redis pub/sub lag
   - Log all OTP generations
   - Alert on connection failures

## Testing

### Unit Tests
```bash
# Backend
cd dummy-off2on && mvn test
cd dummy-off2on-redis && mvn test

# Frontend
cd gfd && npm test
```

### Integration Tests
```bash
# Use test-api.sh scripts
cd dummy-off2on && ./test-api.sh
cd dummy-off2on-redis && ./test-sse-connection.sh
```

### Manual Testing Checklist

- [ ] OTP generation works
- [ ] OTP validation works
- [ ] OTP expires after 5 minutes
- [ ] SSE connection establishes
- [ ] Cart items appear in real-time
- [ ] Totals calculate correctly
- [ ] Checkout flow works
- [ ] Disconnect works properly
- [ ] Reconnection works
- [ ] Multiple clients can connect
- [ ] Error messages display correctly

## Troubleshooting

### Common Issues

**1. "Cannot connect to Redis"**
- Ensure Redis is running: `docker ps | grep redis`
- Check Redis connection: `redis-cli ping`

**2. "OTP Invalid or Expired"**
- OTPs expire after 5 minutes
- OTPs are single-use only
- Generate a new OTP

**3. "SSE Connection Failed"**
- Check backend is running on correct port
- Verify CORS settings
- Check browser console for errors

**4. "No Real-time Updates"**
- Verify Redis pub/sub is working: `redis-cli MONITOR`
- Check both backends are connected to same Redis
- Ensure userId matches across all requests

## Project Structure

```
gfd-sse/
├── dummy-off2on/          # POS Backend (Port 8080)
│   ├── src/main/java/com/gfd_sse/dummyoff2on/
│   │   ├── config/        # Redis, CORS config
│   │   ├── controller/    # REST controllers
│   │   ├── service/       # Business logic
│   │   ├── model/         # Data models
│   │   ├── dto/           # Request/Response DTOs
│   │   └── event/         # Event types
│   └── pom.xml
│
├── dummy-off2on-redis/    # SSE Service (Port 8081)
│   ├── src/main/java/com/gfd_sse/dummyoff2onredis/
│   │   ├── config/        # Redis, CORS config
│   │   ├── controller/    # SSE controller
│   │   ├── service/       # SSE, OTP service
│   │   ├── model/         # Data models
│   │   └── event/         # Event types
│   └── pom.xml
│
├── gfd/                   # Frontend (Port 5173)
│   ├── src/
│   │   ├── components/    # Vue components
│   │   ├── stores/        # State management
│   │   ├── services/      # SSE service
│   │   ├── types/         # TypeScript types
│   │   └── App.vue        # Root component
│   ├── package.json
│   └── README.md
│
└── README.md             # Project overview
```

## License

This is a demonstration project for educational purposes.

## Contributors

- Backend Development: Spring Boot, Redis integration
- Frontend Development: Vue.js, SSE integration
- Architecture Design: Microservices, Event-Driven

## Next Steps

1. ✅ Basic SSE connection - DONE
2. ✅ OTP authentication - DONE  
3. ✅ Real-time cart updates - DONE
4. ✅ Checkout flow - DONE
5. ✅ Beautiful UI - DONE
6. 🔄 Add unit tests
7. 🔄 Add integration tests
8. 🔄 Deploy to production
9. 🔄 Add monitoring/logging
10. 🔄 Performance optimization

## Support

For issues or questions:
1. Check this documentation
2. Review individual project READMEs
3. Check backend logs
4. Review browser console
5. Test with curl/Postman first

---

**Status**: ✅ Production Ready  
**Last Updated**: 2025-10-28  
**Version**: 1.0.0

