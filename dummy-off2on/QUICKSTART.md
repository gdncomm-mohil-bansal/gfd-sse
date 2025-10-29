# Off2On Service - Quick Start Guide

## Prerequisites
- Java 21 or higher
- Maven 3.6+
- Docker (for Redis) OR Redis installed locally

## Step-by-Step Setup

### 1. Start Redis

#### Option A: Using Docker Compose (Recommended)
```bash
docker-compose up -d
```

#### Option B: Using Docker directly
```bash
docker run -d -p 6379:6379 --name redis redis:7-alpine
```

#### Option C: Using local Redis installation
```bash
redis-server
```

### 2. Verify Redis is Running
```bash
# Check if Redis is responding
redis-cli ping
# Should return: PONG
```

### 3. Build the Application
```bash
mvn clean install
```

### 4. Run the Application
```bash
mvn spring-boot:run
```

The application will start on **http://localhost:8080**

### 5. Verify the Application is Running

#### Health Check
```bash
curl http://localhost:8080/api/products/health
```

Expected response:
```json
{
  "success": true,
  "message": "Product service is healthy",
  "data": "OK",
  "timestamp": 1234567890
}
```

## Testing the API

### 1. Get All Products
```bash
curl http://localhost:8080/api/products
```

### 2. Add a Product to Cart
```bash
curl -X POST http://localhost:8080/api/cart/add \
  -H "Content-Type: application/json" \
  -d '{
    "userId": "user123",
    "productId": 1,
    "quantity": 2
  }'
```

### 3. View Cart
```bash
curl http://localhost:8080/api/cart/user123
```

### 4. Add Another Product
```bash
curl -X POST http://localhost:8080/api/cart/add \
  -H "Content-Type: application/json" \
  -d '{
    "userId": "user123",
    "productId": 2,
    "quantity": 1
  }'
```

### 5. Checkout
```bash
curl -X POST http://localhost:8080/api/cart/checkout \
  -H "Content-Type: application/json" \
  -d '{
    "userId": "user123",
    "paymentMethod": "credit_card",
    "shippingAddress": "123 Main St, San Francisco, CA 94102"
  }'
```

## Monitoring Redis Events

To see the events being published to Redis in real-time:

### Terminal 1 - Monitor Cart Events
```bash
redis-cli
SUBSCRIBE cart-events
```

### Terminal 2 - Monitor Product Events
```bash
redis-cli
SUBSCRIBE product-events
```

### Terminal 3 - Monitor Checkout Events
```bash
redis-cli
SUBSCRIBE checkout-events
```

Now when you perform API operations (add to cart, checkout, etc.), you'll see the events being published to Redis in real-time.

## Using Postman

Import the `postman_collection.json` file into Postman for a complete collection of API requests.

## Available Products

The service comes with 5 pre-loaded products:

| ID | Name | Price | Category |
|----|------|-------|----------|
| 1 | Laptop | $999.99 | Electronics |
| 2 | Wireless Mouse | $29.99 | Electronics |
| 3 | Mechanical Keyboard | $89.99 | Electronics |
| 4 | USB-C Hub | $49.99 | Accessories |
| 5 | Noise Cancelling Headphones | $249.99 | Audio |

## Troubleshooting

### Redis Connection Error
If you see `Connection refused` errors:
1. Verify Redis is running: `redis-cli ping`
2. Check Redis is on port 6379: `netstat -an | grep 6379`
3. Check application.properties has correct Redis configuration

### Port Already in Use
If port 8080 is already in use:
1. Change the port in `application.properties`: `server.port=8081`
2. Or stop the other application using port 8080

### Build Errors
If you encounter build errors:
1. Ensure Java 21 is installed: `java -version`
2. Ensure Maven is installed: `mvn -version`
3. Clean and rebuild: `mvn clean install -U`

## Stopping Services

### Stop Spring Boot Application
Press `Ctrl+C` in the terminal where the app is running

### Stop Redis (Docker)
```bash
docker-compose down
```

Or if using Docker directly:
```bash
docker stop redis
```

## Next Steps

After getting Off2On service running:
1. Set up **Off2On-replica** service to subscribe to Redis events
2. Set up **GFD PWA** to receive SSE events from Off2On-replica
3. Set up **Front-liner PWA** to interact with Off2On APIs

## Support

For issues or questions, refer to the main README.md file.

