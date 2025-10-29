# Off2On Service

## Overview
Off2On service is the backend service for the Front-liner e-commerce system. It handles business logic for product management and cart operations, and publishes events to Redis Pub/Sub for real-time communication with the Off2On-replica service.

## Architecture
This service is built with:
- **Spring Boot 3.5.6** - Main framework
- **Spring Data Redis** - Redis integration for Pub/Sub messaging
- **Lombok** - Reduce boilerplate code
- **Java 21** - Programming language

## Features

### REST APIs

#### Products
- `GET /api/products` - Get all products
- `GET /api/products/{id}` - Get product by ID
- `GET /api/products/category/{category}` - Get products by category
- `GET /api/products/health` - Health check

#### Cart
- `POST /api/cart/add` - Add product to cart
- `GET /api/cart/{userId}` - Get cart for user
- `POST /api/cart/checkout` - Checkout cart
- `DELETE /api/cart/{userId}` - Clear cart
- `GET /api/cart/health` - Health check

### Redis Pub/Sub Integration
The service publishes events to Redis channels:
- **cart-events** - Cart-related events (add, remove, update)
- **product-events** - Product-related events (view, update)
- **checkout-events** - Checkout-related events (initiate, complete, fail)

### Event Types
- `PRODUCT_VIEWED` - When a product is viewed
- `CART_ITEM_ADDED` - When an item is added to cart
- `CART_ITEM_REMOVED` - When an item is removed from cart
- `CART_UPDATED` - When cart is updated
- `CHECKOUT_INITIATED` - When checkout process starts
- `CHECKOUT_COMPLETED` - When checkout completes successfully
- `CHECKOUT_FAILED` - When checkout fails

## Prerequisites
- Java 21 or higher
- Maven 3.6+
- Redis server running on localhost:6379

## Configuration
Update `src/main/resources/application.properties` to configure:

```properties
# Server port
server.port=8089

# Redis configuration
spring.data.redis.host=localhost
spring.data.redis.port=6379

# Redis channels
redis.channel.cart-events=cart-events
redis.channel.product-events=product-events
redis.channel.checkout-events=checkout-events
```

## Running the Application

### 1. Start Redis Server
```bash
# Using Docker
docker run -d -p 6379:6379 redis:latest

# Or using local installation
redis-server
```

### 2. Build and Run
```bash
# Build the project
mvn clean install

# Run the application
mvn spring-boot:run

# Or run the JAR
java -jar target/dummy-off2on-0.0.1-SNAPSHOT.jar
```

The service will start on `http://localhost:8080`

## Testing the API

### Get all products
```bash
curl http://localhost:8080/api/products
```

### Add product to cart
```bash
curl -X POST http://localhost:8080/api/cart/add \
  -H "Content-Type: application/json" \
  -d '{
    "userId": "user123",
    "productId": 1,
    "quantity": 2
  }'
```

### Get cart
```bash
curl http://localhost:8080/api/cart/user123
```

### Checkout
```bash
curl -X POST http://localhost:8080/api/cart/checkout \
  -H "Content-Type: application/json" \
  -d '{
    "userId": "user123",
    "paymentMethod": "credit_card",
    "shippingAddress": "123 Main St, City, Country"
  }'
```

## Monitoring Redis Events

You can monitor Redis events using Redis CLI:

```bash
# Connect to Redis
redis-cli

# Subscribe to cart events
SUBSCRIBE cart-events

# In another terminal, subscribe to product events
redis-cli
SUBSCRIBE product-events

# In another terminal, subscribe to checkout events
redis-cli
SUBSCRIBE checkout-events
```

## Project Structure
```
src/main/java/com/gfd_sse/dummyoff2on/
├── config/              # Configuration classes
│   ├── CorsConfig.java
│   └── RedisConfig.java
├── controller/          # REST controllers
│   ├── CartController.java
│   └── ProductController.java
├── dto/                 # Data Transfer Objects
│   ├── AddToCartRequest.java
│   ├── AddToCartResponse.java
│   ├── ApiResponse.java
│   ├── CheckoutRequest.java
│   └── CheckoutResponse.java
├── event/               # Event models
│   ├── CartEvent.java
│   └── EventType.java
├── exception/           # Exception handlers
│   └── GlobalExceptionHandler.java
├── model/               # Domain models
│   ├── CartItem.java
│   └── Product.java
├── service/             # Business logic
│   ├── CartService.java
│   ├── ProductService.java
│   └── RedisPublisherService.java
└── DummyOff2onApplication.java
```

## Sample Products
The service initializes with 5 sample products:
1. Laptop - $999.99
2. Wireless Mouse - $29.99
3. Mechanical Keyboard - $89.99
4. USB-C Hub - $49.99
5. Noise Cancelling Headphones - $249.99

## Next Steps
This service integrates with:
- **Off2On-replica service** - Subscribes to Redis events and forwards to GFD PWA via SSE
- **Front-liner PWA** - Vue.js frontend that consumes these REST APIs
- **GFD PWA** - Vue.js admin interface that receives real-time updates

## Notes
- In-memory storage is used for cart data (suitable for POC)
- For production, consider using Redis or database for cart persistence
- Thread management is handled by Spring Boot's default configuration
- CORS is enabled for all origins (restrict in production)

