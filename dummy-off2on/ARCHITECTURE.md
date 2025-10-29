# Off2On Service - Architecture Documentation

## Overview

The Off2On service is the primary backend service in the Front-liner e-commerce POC system. It handles business logic for product management and cart operations, publishing events to Redis Pub/Sub channels for real-time communication with other services.

## System Architecture

```
┌─────────────────┐
│ Front-liner PWA │ (Vue.js)
└────────┬────────┘
         │ REST API
         ▼
┌─────────────────┐         ┌───────────┐
│  Off2On Service │────────▶│   Redis   │
│  (Spring Boot)  │ Publish │  Pub/Sub  │
└─────────────────┘         └─────┬─────┘
                                  │ Subscribe
                                  ▼
                          ┌────────────────┐
                          │ Off2On-replica │
                          │ (Spring Boot)  │
                          └────────┬───────┘
                                   │ SSE
                                   ▼
                            ┌──────────┐
                            │ GFD PWA  │ (Vue.js)
                            └──────────┘
```

## Component Architecture

### 1. Controllers Layer
Handles HTTP requests and responses.

- **ProductController** (`/api/products`)
  - GET `/` - Get all products
  - GET `/{id}` - Get product by ID
  - GET `/category/{category}` - Get products by category
  - GET `/health` - Health check

- **CartController** (`/api/cart`)
  - POST `/add` - Add product to cart
  - GET `/{userId}` - Get user's cart
  - POST `/checkout` - Checkout cart
  - DELETE `/{userId}` - Clear cart
  - GET `/health` - Health check

### 2. Service Layer
Contains business logic.

- **ProductService**
  - Manages product catalog
  - In-memory product storage (5 pre-loaded products)
  - Publishes `PRODUCT_VIEWED` events

- **CartService**
  - Manages shopping cart operations
  - In-memory cart storage (ConcurrentHashMap)
  - Publishes `CART_ITEM_ADDED`, `CHECKOUT_COMPLETED`, `CHECKOUT_FAILED` events

- **RedisPublisherService**
  - Centralized Redis event publishing
  - Publishes to three channels:
    - `cart-events`
    - `product-events`
    - `checkout-events`

### 3. Model Layer

- **Product** - Product entity
  - id, name, description, price, category, stockQuantity, imageUrl

- **CartItem** - Cart item representation
  - productId, productName, price, quantity, subtotal

### 4. Event System

**EventType Enum:**
- `PRODUCT_VIEWED`
- `CART_ITEM_ADDED`
- `CART_ITEM_REMOVED`
- `CART_UPDATED`
- `CHECKOUT_INITIATED`
- `CHECKOUT_COMPLETED`
- `CHECKOUT_FAILED`

**CartEvent Structure:**
```json
{
  "eventId": "uuid",
  "eventType": "CART_ITEM_ADDED",
  "userId": "user123",
  "timestamp": 1234567890,
  "cartItems": [...],
  "totalAmount": 99.99,
  "totalItems": 2,
  "message": "Added 2 x Laptop to cart",
  "metadata": {...}
}
```

### 5. Configuration

- **RedisConfig** - Redis template configuration with JSON serialization
- **CorsConfig** - CORS configuration for cross-origin requests

## Data Flow

### Add to Cart Flow

```
1. Front-liner PWA
   │
   ├─▶ POST /api/cart/add
   │   Body: { userId, productId, quantity }
   │
2. CartController
   │
   ├─▶ Validates request
   │
3. CartService
   │
   ├─▶ Validates product exists (ProductService)
   ├─▶ Checks stock availability
   ├─▶ Updates in-memory cart
   ├─▶ Calculates totals
   │
4. RedisPublisherService
   │
   ├─▶ Serializes CartEvent to JSON
   ├─▶ Publishes to 'cart-events' channel
   │
5. Redis Pub/Sub
   │
   └─▶ Broadcasts event to subscribers
       (Off2On-replica service)
```

### Checkout Flow

```
1. Front-liner PWA
   │
   ├─▶ POST /api/cart/checkout
   │   Body: { userId, paymentMethod, shippingAddress }
   │
2. CartController
   │
   ├─▶ Validates request
   │
3. CartService
   │
   ├─▶ Validates cart is not empty
   ├─▶ Calculates total amount
   ├─▶ Generates order ID
   ├─▶ Creates CheckoutResponse
   │
4. RedisPublisherService
   │
   ├─▶ Publishes CHECKOUT_COMPLETED event
   │
5. CartService
   │
   └─▶ Clears user's cart
```

## Redis Integration

### Pub/Sub Channels

| Channel | Purpose | Event Types |
|---------|---------|-------------|
| `cart-events` | Cart operations | CART_ITEM_ADDED, CART_UPDATED |
| `product-events` | Product operations | PRODUCT_VIEWED |
| `checkout-events` | Checkout operations | CHECKOUT_COMPLETED, CHECKOUT_FAILED |

### Redis Configuration

```properties
spring.data.redis.host=localhost
spring.data.redis.port=6379
spring.data.redis.timeout=60000
```

### Event Publishing Mechanism

1. **Serialization**: Events are serialized to JSON using Jackson ObjectMapper
2. **Publishing**: Redis template's `convertAndSend()` method publishes to channels
3. **Non-blocking**: Publishing is fire-and-forget (no acknowledgment required)
4. **Error Handling**: Failed publishes are logged but don't interrupt the main flow

## Storage Strategy

### In-Memory Storage

For this POC, we use in-memory storage:

- **Products**: `Map<Long, Product>` - Pre-populated with 5 sample products
- **Carts**: `ConcurrentHashMap<String, List<CartItem>>` - Thread-safe cart storage

**Note**: For production, replace with:
- Product catalog: Database (PostgreSQL, MySQL)
- Cart storage: Redis with TTL or database
- Session management: Redis or database

## Thread Safety

### Concurrent Operations

- **ConcurrentHashMap**: Used for cart storage to handle concurrent user operations
- **Stateless Services**: All service methods are stateless
- **Spring Boot Default**: Servlet container thread pool (default: 200 threads)

### No Explicit Thread Pools Needed

Since we're using traditional Spring MVC (not WebFlux), Spring Boot manages thread pools automatically:
- **Tomcat Thread Pool**: Handles HTTP requests
- **Redis Operations**: Blocking I/O operations execute on request threads
- **Event Publishing**: Synchronous within the request thread

## Error Handling

### Exception Hierarchy

```
GlobalExceptionHandler
├── IllegalArgumentException → 400 Bad Request
├── RuntimeException → 500 Internal Server Error
└── Exception → 500 Internal Server Error
```

### Error Response Format

```json
{
  "success": false,
  "message": "Error description",
  "data": null,
  "timestamp": 1234567890
}
```

## API Response Format

All API responses follow a consistent format:

```json
{
  "success": true,
  "message": "Operation description",
  "data": { ... },
  "timestamp": 1234567890
}
```

## Security Considerations (For Production)

Current POC has minimal security. For production, add:

1. **Authentication**
   - JWT tokens or OAuth 2.0
   - User authentication middleware
   - Session management

2. **Authorization**
   - Role-based access control (RBAC)
   - User ID validation (ensure users can only access their own carts)

3. **Input Validation**
   - Request body validation
   - SQL injection prevention (when using database)
   - XSS prevention

4. **CORS**
   - Currently allows all origins (`*`)
   - Restrict to specific frontend domains

5. **Rate Limiting**
   - Prevent abuse of APIs
   - Redis-based rate limiting

## Scalability Considerations

### Current Limitations

1. **In-Memory Storage**: Not suitable for horizontal scaling
2. **Stateful**: Cart data stored in application memory
3. **Single Instance**: No load balancing support

### Production Scalability Solutions

1. **Externalize State**
   - Move cart data to Redis or database
   - Use distributed session management

2. **Horizontal Scaling**
   - Deploy multiple instances behind load balancer
   - Stateless application design

3. **Redis Clustering**
   - Redis Cluster for high availability
   - Redis Sentinel for automatic failover

4. **Database**
   - Add PostgreSQL/MySQL for persistent storage
   - Connection pooling (HikariCP)

5. **Caching**
   - Redis cache for frequently accessed products
   - Cache invalidation strategy

## Monitoring and Observability

### Logging

- SLF4J with Logback
- Log levels: DEBUG for development, INFO for production
- Structured logging for easy parsing

### Metrics (To Add)

- Spring Boot Actuator
- Prometheus metrics
- Request/response times
- Error rates
- Redis connection pool metrics

### Health Checks

- `/api/products/health`
- `/api/cart/health`
- Can extend with actuator endpoints

## Testing Strategy

### Current Testing

- Manual testing via cURL
- Postman collection
- `test-api.sh` script

### Recommended Testing (To Add)

1. **Unit Tests**
   - Service layer logic
   - Event creation and serialization

2. **Integration Tests**
   - Controller tests with MockMvc
   - Redis integration tests with Testcontainers

3. **E2E Tests**
   - Complete user flows
   - Cart operations end-to-end

## Performance Characteristics

### Expected Performance

- **Products API**: <50ms response time
- **Add to Cart**: <100ms (includes Redis publish)
- **Checkout**: <200ms (includes Redis publish and cart clear)

### Bottlenecks

1. **Redis Publishing**: Synchronous, adds ~10-20ms per event
2. **JSON Serialization**: Minimal overhead (~1-5ms)
3. **Thread Pool**: Default 200 threads, suitable for moderate load

### Optimization Tips

1. **Async Publishing**: Use CompletableFuture for Redis publishing
2. **Connection Pooling**: Configure Lettuce connection pool
3. **Caching**: Cache product data in Redis
4. **Batch Operations**: Batch multiple cart updates

## Integration Points

### Upstream Services

- **Front-liner PWA**: Consumes REST APIs

### Downstream Services

- **Redis**: Pub/Sub event bus
- **Off2On-replica**: Subscribes to Redis events

### Future Integrations

- **Payment Gateway**: For real checkout
- **Inventory Service**: For stock management
- **User Service**: For authentication/authorization
- **Order Service**: For order management

## Configuration Management

### Environment-Specific Configuration

Create profiles for different environments:

- `application.properties` - Default
- `application-dev.properties` - Development
- `application-prod.properties` - Production

### Externalized Configuration

For production, use:
- Environment variables
- Spring Cloud Config Server
- Kubernetes ConfigMaps/Secrets

## Deployment

### Local Development
```bash
mvn spring-boot:run
```

### Docker (To Add)
```dockerfile
FROM openjdk:21-jdk-slim
COPY target/dummy-off2on-0.0.1-SNAPSHOT.jar app.jar
ENTRYPOINT ["java","-jar","/app.jar"]
```

### Kubernetes (To Add)
- Deployment with multiple replicas
- Service for load balancing
- ConfigMap for configuration
- Secret for sensitive data

## Next Steps

1. **Implement Off2On-replica service**
2. **Add SSE support in Off2On-replica**
3. **Build GFD PWA frontend**
4. **Build Front-liner PWA frontend**
5. **Add authentication/authorization**
6. **Implement persistent storage**
7. **Add comprehensive testing**
8. **Set up CI/CD pipeline**

## References

- [Spring Boot Documentation](https://spring.io/projects/spring-boot)
- [Spring Data Redis](https://spring.io/projects/spring-data-redis)
- [Redis Pub/Sub](https://redis.io/topics/pubsub)
- [Project README](README.md)
- [Quick Start Guide](QUICKSTART.md)

