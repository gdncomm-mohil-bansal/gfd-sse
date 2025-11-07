# Architecture Diagrams - SSE Cross-Pod Communication

## Scenario 1: Same-Pod Communication ✅ WORKS

```
┌─────────────────────────────────────────────────────────────────┐
│                                                                   │
│  Step 1: Client Connects                                         │
│  ┌──────────┐         SSE /connect                              │
│  │ Client   │────────────────────────┐                           │
│  │ user123  │                        │                           │
│  └──────────┘                        ↓                           │
│                            ┌──────────────────┐                  │
│                            │    Pod A         │                  │
│                            │  (Port 8081)     │                  │
│                            │                  │                  │
│                            │  Creates Emitter │                  │
│                            │  emitters = {    │                  │
│                            │    user123:      │                  │
│                            │      Emitter1    │                  │
│                            │  }               │                  │
│                            └──────────────────┘                  │
│                                                                   │
│  Step 2: API Request to Same Pod                                 │
│  ┌──────────┐         POST /checkout/user123                    │
│  │ Client   │────────────────────────┐                           │
│  │ user123  │                        │                           │
│  └──────────┘                        ↓                           │
│      ↑                    ┌──────────────────┐                  │
│      │                    │    Pod A         │                  │
│      │                    │  (Port 8081)     │                  │
│      │                    │                  │                  │
│      │  Event sent via    │  Has emitter? ✅  │                  │
│      └────────────────────│  Sends event     │                  │
│         existing SSE      │  through         │                  │
│         connection        │  Emitter1        │                  │
│                            └──────────────────┘                  │
│                                                                   │
│  Result: ✅ SUCCESS - Event reaches client                       │
│                                                                   │
└─────────────────────────────────────────────────────────────────┘
```

## Scenario 2: Cross-Pod WITHOUT Redis ❌ FAILS

```
┌─────────────────────────────────────────────────────────────────┐
│                                                                   │
│  Step 1: Client Connects to Pod A                                │
│  ┌──────────┐         SSE /connect                              │
│  │ Client   │────────────────┐                                   │
│  │ user123  │                │                                   │
│  └──────────┘                ↓                                   │
│                   ┌──────────────────┐      ┌──────────────────┐│
│                   │    Pod A         │      │    Pod B         ││
│                   │  (Port 8081)     │      │  (Port 8082)     ││
│                   │                  │      │                  ││
│                   │  Creates Emitter │      │  emitters = {}   ││
│                   │  emitters = {    │      │                  ││
│                   │    user123:      │      │                  ││
│                   │      Emitter1    │      │                  ││
│                   │  }               │      │                  ││
│                   └──────────────────┘      └──────────────────┘│
│                                                                   │
│  Step 2: Load Balancer Routes API to Pod B                       │
│  ┌──────────┐         POST /checkout/user123                    │
│  │ Client   │────────────────┐                                   │
│  │ user123  │                │                                   │
│  └──────────┘                ↓                                   │
│      ↑          ┌─────────────────────────┐                     │
│      │          │   Load Balancer         │                     │
│      │          └─────────────────────────┘                     │
│      │                          │                                │
│      │                          │ Routes to Pod B               │
│      │                          ↓                                │
│      │          ┌──────────────────┐      ┌──────────────────┐ │
│      │          │    Pod A         │      │    Pod B         │ │
│      │          │  (Port 8081)     │      │  (Port 8082)     │ │
│      │          │                  │      │                  │ │
│      │          │  Has emitter ✅   │      │  Has emitter? ❌  │ │
│      │          │  but no event!   │      │  Cannot send!    │ │
│      │          │                  │      │                  │ │
│      │          └──────────────────┘      └──────────────────┘ │
│      │                                                           │
│      └───── ❌ NO EVENT RECEIVED ──────────────────────────────  │
│                                                                   │
│  Result: ❌ FAILURE - Event does NOT reach client                │
│  Pod B has no way to communicate with Pod A                      │
│                                                                   │
└─────────────────────────────────────────────────────────────────┘
```

## Scenario 3: Cross-Pod WITH Redis Pub/Sub ✅ WORKS

```
┌─────────────────────────────────────────────────────────────────┐
│                                                                   │
│  Step 1: Client Connects to Pod A, Pods Subscribe to Redis       │
│  ┌──────────┐         SSE /connect                              │
│  │ Client   │────────────────┐                                   │
│  │ user123  │                │                                   │
│  └──────────┘                ↓                                   │
│                   ┌──────────────────┐      ┌──────────────────┐│
│                   │    Pod A         │      │    Pod B         ││
│                   │  (Port 8081)     │      │  (Port 8082)     ││
│                   │                  │      │                  ││
│                   │  Creates Emitter │      │  emitters = {}   ││
│                   │  emitters = {    │      │                  ││
│                   │    user123:      │      │                  ││
│                   │      Emitter1    │      │                  ││
│                   │  }               │      │                  ││
│                   │  Subscribes to   │      │  Subscribes to   ││
│                   │  Redis ↓         │      │  Redis ↓         ││
│                   └────────┼─────────┘      └────────┼─────────┘│
│                            │                         │           │
│                            └─────┐       ┌──────────┘           │
│                                  ↓       ↓                       │
│                        ┌─────────────────────────┐              │
│                        │   Redis Pub/Sub         │              │
│                        │   Channel: cart-events  │              │
│                        └─────────────────────────┘              │
│                                                                   │
│  Step 2: Load Balancer Routes API to Pod B                       │
│  ┌──────────┐         POST /checkout/user123                    │
│  │ Client   │────────────────┐                                   │
│  │ user123  │                │                                   │
│  └──────────┘                ↓                                   │
│      ↑          ┌─────────────────────────┐                     │
│      │          │   Load Balancer         │                     │
│      │          └─────────────────────────┘                     │
│      │                          │                                │
│      │                          │ Routes to Pod B               │
│      │                          ↓                                │
│      │          ┌──────────────────┐      ┌──────────────────┐ │
│      │          │    Pod A         │      │    Pod B         │ │
│      │          │  (Port 8081)     │      │  (Port 8082)     │ │
│      │          │                  │      │                  │ │
│      │          │  Waiting...      │      │  Receives        │ │
│      │          │                  │      │  API request     │ │
│      │          │                  │      │                  │ │
│      │          │                  │      │  Publishes to    │ │
│      │          │                  │      │  Redis ↓         │ │
│      │          └──────────────────┘      └────────┼─────────┘ │
│      │                   ↑                          │           │
│      │                   │                          │           │
│      │                   │         ┌────────────────┘           │
│      │                   │         ↓                            │
│      │                   │   ┌─────────────────────────┐       │
│      │                   │   │   Redis Pub/Sub         │       │
│      │                   │   │   Broadcasts event      │       │
│      │                   │   └─────────────────────────┘       │
│      │                   │         │             │              │
│      │                   └─────────┘             └──────┐       │
│      │                                                  │       │
│      │          ┌──────────────────┐      ┌────────────▼─────┐ │
│      │          │    Pod A         │      │    Pod B         │ │
│      │          │                  │      │                  │ │
│      │          │  Receives event  │      │  Receives event  │ │
│      │          │  from Redis      │      │  from Redis      │ │
│      │          │                  │      │                  │ │
│      │          │  Has user123? ✅  │      │  Has user123? ❌  │ │
│      │          │  Sends via       │      │  Ignores event   │ │
│      │   Event  │  Emitter1 ───────┼──────│  (no emitter)    │ │
│      └──────────│                  │      │                  │ │
│                 └──────────────────┘      └──────────────────┘ │
│                                                                  │
│  Result: ✅ SUCCESS - Event reaches client via Pod A            │
│  Redis Pub/Sub enables cross-pod communication                  │
│                                                                  │
└──────────────────────────────────────────────────────────────────┘
```

## Key Concepts Illustrated

### 1. SseEmitter is Connection-Specific

```
┌──────────┐  HTTP Connection 1  ┌──────────────┐
│ Client 1 │ ←─────────────────→ │ SseEmitter 1 │
└──────────┘                     └──────────────┘

┌──────────┐  HTTP Connection 2  ┌──────────────┐
│ Client 2 │ ←─────────────────→ │ SseEmitter 2 │
└──────────┘                     └──────────────┘

Cannot send Client 1's events through SseEmitter 2!
Cannot send Client 2's events through SseEmitter 1!
```

### 2. In-Memory Storage is Pod-Local

```
┌──────────────────┐      ┌──────────────────┐
│    Pod A         │      │    Pod B         │
│                  │      │                  │
│  ConcurrentHashMap      │  ConcurrentHashMap │
│  emitters = {    │  ❌   │  emitters = {    │
│    user1: Emit1  │  No  │    user3: Emit3  │
│    user2: Emit2  │ Sharing  user4: Emit4  │
│  }               │      │  }               │
└──────────────────┘      └──────────────────┘

Each pod's HashMap is independent - no shared memory!
```

### 3. Load Balancer is Stateless

```
         ┌─────────────────────────┐
         │   Load Balancer         │
         │  (Round Robin)          │
         └─────────────────────────┘
                    │
     ┌──────────────┼──────────────┐
     │              │              │
Request 1     Request 2      Request 3
     ↓              ↓              ↓
  Pod A          Pod B          Pod A

Load balancer doesn't know which pod has which user's SSE connection!
```

### 4. Redis Pub/Sub as Message Broker

```
        ┌──────────┐
        │  Pod A   │──┐
        └──────────┘  │
                      ├──→ Subscribe ──┐
        ┌──────────┐  │                │
        │  Pod B   │──┤                ↓
        └──────────┘  │    ┌────────────────────┐
                      │    │  Redis Pub/Sub     │
        ┌──────────┐  │    │  Channel: events   │
        │  Pod C   │──┘    └────────────────────┘
        └──────────┘                 ↑
                                     │
                          Publish ───┘
                          (from any pod)

All pods receive the event → Each checks if it has the user → Sends if yes
```

## Performance Analysis

### Checking hasActiveConnection(userId)

```java
// ConcurrentHashMap.containsKey() is O(1)
public boolean hasActiveConnection(String userId) {
    return emitters.containsKey(userId);  // ~1 microsecond
}
```

### Cost per Broadcast Event

```
Scenario: 100 pods, 1 event published

Total operations: 100 × hasActiveConnection()
Time per check: ~1 microsecond
Total time: ~100 microseconds = 0.0001 seconds

This is NEGLIGIBLE compared to:
- Network I/O: ~10ms
- Database query: ~10ms
- HTTP request: ~50ms
```

### Why Pub/Sub Scales

```
With 10 pods:   10 checks per event   = 10 microseconds
With 100 pods:  100 checks per event  = 100 microseconds
With 1000 pods: 1000 checks per event = 1000 microseconds = 1ms

Even at 1000 pods, checking is only 1ms - still negligible!
```

## Conclusion

**Redis Pub/Sub is the correct solution** because:

1. ✅ Low latency (milliseconds)
2. ✅ Handles load balancer routing correctly
3. ✅ Scales to many pods efficiently
4. ✅ Industry standard approach
5. ✅ Simple to implement and maintain

**The "overhead" is a myth** - checking 1000 HashMaps takes 1ms, which is negligible in web applications.

---

See [CROSS_POD_TESTING_GUIDE.md](./CROSS_POD_TESTING_GUIDE.md) to test these scenarios yourself!

