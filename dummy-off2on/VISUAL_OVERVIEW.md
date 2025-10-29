# Off2On Service - Visual Overview

## System Flow Diagram

```
┌──────────────────────────────────────────────────────────────────────────┐
│                         Front-liner PWA (Vue.js)                         │
│                    User Interface for E-commerce                         │
└────────────────────────────┬─────────────────────────────────────────────┘
                             │
                             │ HTTP REST API
                             │ (GET /products, POST /cart/add, etc.)
                             │
                             ▼
┌──────────────────────────────────────────────────────────────────────────┐
│                        Off2On Service (THIS)                             │
│                         Spring Boot 3.5.6                                │
│  ┌────────────────────────────────────────────────────────────────────┐ │
│  │  Controllers Layer                                                  │ │
│  │  ┌──────────────────┐           ┌──────────────────┐              │ │
│  │  │ ProductController│           │  CartController  │              │ │
│  │  │  /api/products   │           │   /api/cart      │              │ │
│  │  └────────┬─────────┘           └────────┬─────────┘              │ │
│  └───────────┼──────────────────────────────┼────────────────────────┘ │
│              │                               │                          │
│  ┌───────────┼──────────────────────────────┼────────────────────────┐ │
│  │  Service Layer                            │                        │ │
│  │  ┌────────▼─────────┐           ┌────────▼─────────┐             │ │
│  │  │ ProductService   │           │   CartService    │             │ │
│  │  │ - Get products   │           │ - Add to cart    │             │ │
│  │  │ - View product   │           │ - Checkout       │             │ │
│  │  └──────────────────┘           └────────┬─────────┘             │ │
│  │                                           │                        │ │
│  │                                  ┌────────▼──────────┐            │ │
│  │                                  │ RedisPublisher    │            │ │
│  │                                  │ Service           │            │ │
│  │                                  └────────┬──────────┘            │ │
│  └───────────────────────────────────────────┼────────────────────────┘ │
│                                               │                          │
│  ┌───────────────────────────────────────────┼────────────────────────┐ │
│  │  Event System                             │                        │ │
│  │  ┌────────────────────────────────────────▼──────────────────────┐│ │
│  │  │ CartEvent { eventType, userId, cartItems, totalAmount, ... }  ││ │
│  │  └───────────────────────────────────────────────────────────────┘│ │
│  └───────────────────────────────────────────┼────────────────────────┘ │
└────────────────────────────────────────────┬─┼────────────────────────┬─┘
                                             │ │                        │
                           Publishes to:     │ │                        │
                           - cart-events     │ │                        │
                           - product-events  │ │                        │
                           - checkout-events │ │                        │
                                             │ │                        │
                                             ▼ ▼                        │
                              ┌─────────────────────────────┐           │
                              │    Redis Pub/Sub Server     │           │
                              │         Port: 6379          │           │
                              │                             │           │
                              │  Channels:                  │           │
                              │  • cart-events              │           │
                              │  • product-events           │           │
                              │  • checkout-events          │           │
                              └──────────────┬──────────────┘           │
                                             │                          │
                                             │ Subscribes               │
                                             ▼                          │
                              ┌─────────────────────────────┐           │
                              │   Off2On-replica Service    │           │
                              │      (To be built)          │           │
                              │    Redis Subscriber         │           │
                              └──────────────┬──────────────┘           │
                                             │                          │
                                             │ SSE (Server-Sent Events) │
                                             ▼                          │
                              ┌─────────────────────────────┐           │
                              │      GFD PWA (Vue.js)       │           │
                              │  (Admin/Monitoring Interface)           │
                              │  Real-time Event Display    │           │
                              └─────────────────────────────┘           │
                                                                         │
                              ┌──────────────────────────────────────────┘
                              │ Future: Authentication/Session
                              ▼
```

## Component Interaction Diagram

### Add to Cart Flow

```
User Action                  Off2On Service              Redis              Off2On-replica
    │                              │                       │                      │
    │  1. Click "Add to Cart"      │                       │                      │
    ├──────────────────────────────▶                       │                      │
    │  POST /api/cart/add          │                       │                      │
    │                              │                       │                      │
    │                              │ 2. Validate           │                      │
    │                              │    Product            │                      │
    │                              │                       │                      │
    │                              │ 3. Update             │                      │
    │                              │    Cart               │                      │
    │                              │                       │                      │
    │                              │ 4. Publish Event      │                      │
    │                              ├───────────────────────▶                      │
    │                              │  CART_ITEM_ADDED      │                      │
    │                              │                       │                      │
    │  5. Success Response         │                       │ 6. Broadcast         │
    │◀──────────────────────────────                       ├──────────────────────▶
    │  { success: true, ... }      │                       │  Event to Subscribers │
    │                              │                       │                      │
    │                              │                       │                      │
    │                              │                       │  7. Forward via SSE  │
    │                              │                       │  to GFD PWA          │
    │                              │                       │                      │
```

### Checkout Flow

```
User Action                  Off2On Service              Redis              Off2On-replica
    │                              │                       │                      │
    │  1. Click "Checkout"         │                       │                      │
    ├──────────────────────────────▶                       │                      │
    │  POST /api/cart/checkout     │                       │                      │
    │                              │                       │                      │
    │                              │ 2. Validate Cart      │                      │
    │                              │                       │                      │
    │                              │ 3. Calculate Total    │                      │
    │                              │                       │                      │
    │                              │ 4. Generate Order ID  │                      │
    │                              │                       │                      │
    │                              │ 5. Publish Event      │                      │
    │                              ├───────────────────────▶                      │
    │                              │  CHECKOUT_COMPLETED   │                      │
    │                              │                       │                      │
    │                              │ 6. Clear Cart         │                      │
    │                              │                       │ 7. Broadcast         │
    │  8. Order Confirmation       │                       ├──────────────────────▶
    │◀──────────────────────────────                       │  Event to Subscribers │
    │  { orderId: "ORD-XXX", ... } │                       │                      │
    │                              │                       │                      │
    │                              │                       │  9. Notify GFD PWA   │
    │                              │                       │  via SSE             │
```

## Data Models

### Product Model
```
┌─────────────────────────┐
│       Product           │
├─────────────────────────┤
│ • id: Long              │
│ • name: String          │
│ • description: String   │
│ • price: BigDecimal     │
│ • category: String      │
│ • stockQuantity: Integer│
│ • imageUrl: String      │
└─────────────────────────┘
```

### Cart Item Model
```
┌─────────────────────────┐
│       CartItem          │
├─────────────────────────┤
│ • productId: Long       │
│ • productName: String   │
│ • price: BigDecimal     │
│ • quantity: Integer     │
│ • subtotal: BigDecimal  │
└─────────────────────────┘
```

### Cart Event Model
```
┌─────────────────────────────────────┐
│           CartEvent                 │
├─────────────────────────────────────┤
│ • eventId: String (UUID)            │
│ • eventType: EventType              │
│ • userId: String                    │
│ • timestamp: Long                   │
│ • cartItems: List<CartItem>         │
│ • totalAmount: BigDecimal           │
│ • totalItems: Integer               │
│ • message: String                   │
│ • metadata: Object                  │
└─────────────────────────────────────┘
```

## API Endpoint Map

```
Off2On Service (Port 8080)
│
├── /api/products
│   ├── GET    /                    → Get all products
│   ├── GET    /{id}                → Get product by ID
│   ├── GET    /category/{category} → Get products by category
│   └── GET    /health              → Health check
│
└── /api/cart
    ├── POST   /add                 → Add product to cart
    ├── GET    /{userId}            → Get user's cart
    ├── POST   /checkout            → Checkout cart
    ├── DELETE /{userId}            → Clear cart
    └── GET    /health              → Health check
```

## Redis Channel Structure

```
Redis Pub/Sub
│
├── cart-events
│   ├── CART_ITEM_ADDED
│   ├── CART_ITEM_REMOVED
│   └── CART_UPDATED
│
├── product-events
│   └── PRODUCT_VIEWED
│
└── checkout-events
    ├── CHECKOUT_INITIATED
    ├── CHECKOUT_COMPLETED
    └── CHECKOUT_FAILED
```

## Technology Stack Layers

```
┌─────────────────────────────────────────────┐
│         Presentation Layer                  │
│  Spring Web Controllers + REST APIs         │
└─────────────────────────────────────────────┘
                    ▼
┌─────────────────────────────────────────────┐
│          Business Logic Layer               │
│  Service Classes (Cart, Product, Redis)    │
└─────────────────────────────────────────────┘
                    ▼
┌─────────────────────────────────────────────┐
│         Data Access Layer                   │
│  In-Memory Storage (ConcurrentHashMap)      │
│  Redis Operations (Pub/Sub)                 │
└─────────────────────────────────────────────┘
                    ▼
┌─────────────────────────────────────────────┐
│         Infrastructure Layer                │
│  Redis Server                               │
│  Spring Boot Embedded Tomcat                │
└─────────────────────────────────────────────┘
```

## Request/Response Flow

```
HTTP Request
    │
    ├──▶ @RestController
    │       │
    │       ├──▶ Request Validation
    │       │
    │       ├──▶ @Service
    │       │       │
    │       │       ├──▶ Business Logic
    │       │       │
    │       │       ├──▶ Update State (In-Memory)
    │       │       │
    │       │       └──▶ RedisPublisher
    │       │               │
    │       │               ├──▶ Serialize to JSON
    │       │               │
    │       │               └──▶ Publish to Channel
    │       │
    │       └──▶ Build Response
    │
    └──▶ HTTP Response (JSON)
```

## Thread Management

```
Tomcat Thread Pool (Default: 200 threads)
    │
    ├──▶ Thread 1: Handle Request A
    │       │
    │       ├──▶ Execute Business Logic
    │       ├──▶ Publish to Redis (Blocking)
    │       └──▶ Return Response
    │
    ├──▶ Thread 2: Handle Request B
    │       │
    │       ├──▶ Execute Business Logic
    │       ├──▶ Publish to Redis (Blocking)
    │       └──▶ Return Response
    │
    └──▶ Thread N: Handle Request N
            └──▶ ...

Note: Each request gets its own thread from the pool.
      Redis operations are blocking but fast (~10-20ms).
```

## Storage Architecture

```
┌────────────────────────────────────────────────────┐
│            Application Memory                      │
│  ┌──────────────────────────────────────────────┐ │
│  │  Product Storage (HashMap)                   │ │
│  │  ┌──────────────────────────────────────┐   │ │
│  │  │ ID → Product                         │   │ │
│  │  │ 1  → Laptop                          │   │ │
│  │  │ 2  → Mouse                           │   │ │
│  │  │ 3  → Keyboard                        │   │ │
│  │  │ ...                                  │   │ │
│  │  └──────────────────────────────────────┘   │ │
│  └──────────────────────────────────────────────┘ │
│                                                    │
│  ┌──────────────────────────────────────────────┐ │
│  │  Cart Storage (ConcurrentHashMap)           │ │
│  │  ┌──────────────────────────────────────┐   │ │
│  │  │ UserId → List<CartItem>              │   │ │
│  │  │ user1  → [Laptop x2, Mouse x1]       │   │ │
│  │  │ user2  → [Keyboard x1]               │   │ │
│  │  │ ...                                  │   │ │
│  │  └──────────────────────────────────────┘   │ │
│  └──────────────────────────────────────────────┘ │
└────────────────────────────────────────────────────┘
```

## File Organization

```
src/main/java/com/gfd_sse/dummyoff2on/
│
├── 📁 config/              Configuration classes
│   ├── CorsConfig         CORS setup
│   └── RedisConfig        Redis template
│
├── 📁 controller/         REST endpoints
│   ├── CartController     Cart APIs
│   └── ProductController  Product APIs
│
├── 📁 dto/                Data transfer objects
│   ├── Request DTOs       API request models
│   └── Response DTOs      API response models
│
├── 📁 event/              Event system
│   ├── CartEvent          Event model
│   └── EventType          Event types enum
│
├── 📁 exception/          Error handling
│   └── GlobalExceptionHandler
│
├── 📁 model/              Domain models
│   ├── Product            Product entity
│   └── CartItem           Cart item entity
│
├── 📁 service/            Business logic
│   ├── CartService        Cart operations
│   ├── ProductService     Product operations
│   └── RedisPublisher     Event publishing
│
└── DummyOff2onApplication Main class
```

## Quick Commands Reference

```bash
# Start Redis
docker-compose up -d

# Build project
mvn clean install

# Run application
mvn spring-boot:run

# Test APIs
./test-api.sh

# Monitor events
redis-cli
SUBSCRIBE cart-events

# Check health
curl http://localhost:8080/api/products/health

# Get products
curl http://localhost:8080/api/products

# Add to cart
curl -X POST http://localhost:8080/api/cart/add \
  -H "Content-Type: application/json" \
  -d '{"userId":"user123","productId":1,"quantity":2}'
```

## Success Indicators

✅ Service starts on port 8080
✅ Redis connection established
✅ All endpoints respond with 200 OK
✅ Events published to Redis channels
✅ No compilation errors
✅ No linter warnings
✅ Documentation complete
✅ Tests passing

---

**This visual overview provides a quick reference to understand the Off2On service architecture and flow.**

