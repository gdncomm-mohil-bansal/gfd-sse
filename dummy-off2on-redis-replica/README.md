# Dummy Off2On Redis Replica Service

## Purpose

This is a **demonstration/testing service** designed to help understand SSE (Server-Sent Events) behavior across multiple pods/service instances in a distributed system.

## ⚠️ Important Notice

**This service is NOT intended for production use.** It's a learning tool to demonstrate why Redis Pub/Sub is necessary for distributed SSE architectures.

## What This Service Does

This service simulates a "Pod B" scenario where:
- The client's SSE connection is on a different pod (Pod A)
- API requests land on this pod (Pod B)
- You can observe that events **CANNOT** reach the client across pods without a message broker

## Key Differences from Main Service

| Feature | Main Service (dummy-off2on-redis) | Replica Service |
|---------|-----------------------------------|-----------------|
| Port | 8081 | 8082 |
| OTP Authentication | ✅ Required | ❌ Not implemented (simplified for testing) |
| Redis Pub/Sub | ✅ Implemented | ❌ Not implemented (to demonstrate the problem) |
| Purpose | Production-ready | Testing/demonstration only |
| Base URL | `/api/sse` | `/api/replica` |

## Running the Service

### Prerequisites

- Java 17 or higher
- Maven 3.6+

### Start the Service

```bash
cd dummy-off2on-redis-replica
./mvnw spring-boot:run
```

The service will start on port **8082**.

## Testing

See [CROSS_POD_TESTING_GUIDE.md](./CROSS_POD_TESTING_GUIDE.md) for comprehensive testing scenarios.

### Quick Test - Same Pod (Works ✅)

```bash
# Terminal 1: Connect to replica pod
curl -N "http://localhost:8082/api/replica/connect?userId=user123"

# Terminal 2: Trigger event on same pod
curl -X POST "http://localhost:8082/api/replica/checkout/user123"

# Result: Client receives the event ✅
```

### Quick Test - Cross Pod (Fails ❌)

```bash
# Terminal 1: Connect to main service (Pod A) on port 8081
curl -N "http://localhost:8081/api/sse/connect?userId=user123&otp=YOUR_OTP"

# Terminal 2: Trigger event on replica service (Pod B) on port 8082
curl -X POST "http://localhost:8082/api/replica/checkout/user123"

# Result: Client does NOT receive the event ❌
# API returns: "Cannot send event - User not connected to this pod"
```

## API Endpoints

### Connect to SSE
```
GET /api/replica/connect?userId={userId}
```
Establish SSE connection (simplified, no OTP required for testing).

### Trigger Checkout Event
```
POST /api/replica/checkout/{userId}
```
Trigger a test checkout event. Only succeeds if user is connected to THIS pod.

### Check Connection Status
```
GET /api/replica/status/{userId}
```
Check if a specific user is connected to this pod.

### Get Connection Count
```
GET /api/replica/connections/count
```
Get the number of active SSE connections on this pod.

### Disconnect
```
POST /api/replica/disconnect/{userId}
```
Disconnect a user from this pod.

## Architecture Learning Points

### What You'll Learn

1. **SSE Emitters are Pod-Specific**: Each pod maintains its own in-memory map of emitters
2. **Cross-Pod Communication Requires a Broker**: Without Redis Pub/Sub, events cannot reach clients on different pods
3. **Why Redis Pub/Sub is the Standard Solution**: It's the industry-standard way to solve this problem

### The Problem This Demonstrates

```
┌─────────────┐
│   Client    │
└──────┬──────┘
       │
       │ SSE Connection to Pod A (8081)
       ↓
┌─────────────┐        ┌─────────────┐
│   POD A     │        │   POD B     │
│  (Main)     │        │ (Replica)   │
│  Port 8081  │        │  Port 8082  │
│             │        │             │
│ Has user's  │   ❌   │ No user's   │
│ emitter     │  ←───  │ emitter     │
│             │        │             │
│             │        │ ⬆ API       │
│             │        │   request   │
└─────────────┘        └─────────────┘
                              ⬆
                              │
                       Load Balancer
                       routes here
```

**Without Redis**: Pod B cannot send events to the client because it doesn't have the user's emitter.

**With Redis Pub/Sub**: Pod B publishes to Redis → Pod A receives → Pod A sends to client ✅

## Configuration

See `src/main/resources/application.properties`:

```properties
server.port=8082
sse.timeout.minutes=30
```

## Code Structure

```
src/main/java/com/gfd_sse/dummyoff2onredisreplica/
├── controller/
│   └── SseController.java          # SSE and event endpoints
├── service/
│   ├── SseService.java             # SSE emitter management
│   └── CheckoutService.java        # Event creation and sending
├── event/
│   ├── CartEvent.java              # Event model
│   └── EventType.java              # Event types enum
├── dto/
│   ├── request/                    # Request DTOs
│   └── response/                   # Response DTOs (ApiResponse)
└── model/
    └── CartItem.java               # Cart item model
```

## Logging

The service uses a special log prefix `[REPLICA-POD]` to distinguish logs from the main service:

```
2024-01-15 10:30:45 - [REPLICA-POD] - SSE connection request from user: user123 to REPLICA pod
2024-01-15 10:30:50 - [REPLICA-POD] - User user123 IS connected to THIS pod. Sending event directly.
```

## Comparison with Main Service

### What's Removed (Simplified)

- ❌ OTP authentication (for easier testing)
- ❌ Redis integration (to demonstrate the problem)
- ❌ Pub/Sub messaging (the whole point of this demo)
- ❌ Keep-alive heartbeat (not essential for demo)

### What's the Same

- ✅ SSE emitter management
- ✅ Event sending logic
- ✅ Connection tracking
- ✅ Error handling

## When to Use This Service

### ✅ DO Use for:
- Learning how SSE works
- Understanding multi-pod challenges
- Testing SSE behavior locally
- Demonstrating the need for Redis Pub/Sub

### ❌ DON'T Use for:
- Production deployments
- Actual multi-pod deployments with load balancers
- Systems requiring reliable event delivery
- Security-sensitive applications (no OTP)

## Recommended Next Steps

After testing with this replica service:

1. ✅ **Understand the problem**: See why cross-pod events don't work
2. ✅ **Review the solution**: Study `dummy-off2on-redis` to see Redis Pub/Sub implementation
3. ✅ **Keep Redis Pub/Sub**: Use it in your production architecture
4. ✅ **Consider alternatives**: Only if you have specific requirements (see testing guide)

## Contributing

This is a demonstration service. If you find issues or have suggestions for better demonstrations, feel free to improve it.

## Related Documentation

- [CROSS_POD_TESTING_GUIDE.md](./CROSS_POD_TESTING_GUIDE.md) - Comprehensive testing scenarios
- [../dummy-off2on-redis/README.md](../dummy-off2on-redis/README.md) - Production-ready service with Redis
- [../UPDATED_ARCHITECTURE.md](../UPDATED_ARCHITECTURE.md) - Overall architecture documentation

## License

Same as the main project.

