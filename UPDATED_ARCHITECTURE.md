# Updated Architecture - OTP Generation & SSE Connection Strategy

## 🔄 Architecture Changes

### Previous Flow (Less Logical)
```
GFD PWA
  ↓ (requests OTP)
dummy-off2on-redis (generates OTP)
  ↓ (displays OTP)
GFD PWA
  ↓ (Front-liner enters OTP)
dummy-off2on-redis (validates & connects SSE)
```

### **NEW FLOW (More Logical)** ✅
```
Front-liner PWA
  ↓ (requests OTP)
dummy-off2on (generates OTP, stores in Redis)
  ↓ (Front-liner displays OTP to GFD user)
GFD PWA
  ↓ (enters OTP, connects to SSE)
dummy-off2on-redis (validates OTP from Redis, establishes SSE)
  ↓ (receives real-time events)
GFD PWA (displays updates)
```

## 🎯 Why This is Better

| Aspect | Old Approach | New Approach |
|--------|-------------|--------------|
| **Logic Flow** | GFD requests monitoring of itself | Front-liner initiates monitoring session |
| **Service Separation** | Mixed concerns | Clear: dummy-off2on = business, dummy-off2on-redis = monitoring |
| **User Experience** | Confusing ownership | Natural: Front-liner controls who monitors |
| **OTP Ownership** | OTP represents GFD session | OTP represents Front-liner session |
| **Storage** | In-memory (not shared) | Redis (shared between services) |

## 🏗️ Updated Architecture Diagram

```
┌─────────────────────────────────────────────────────────────────────┐
│                         Front-liner PWA                              │
│                      (Customer-facing app)                           │
└────────────┬────────────────────────────────────────────────────────┘
             │
             │ 1. Add to cart, checkout, etc.
             │ 2. Generate OTP to share with GFD
             ▼
┌─────────────────────────────────────────────────────────────────────┐
│                        dummy-off2on (Port 8080)                      │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────────────────┐  │
│  │ Cart Service │  │ OTP Service  │  │ Redis Publisher Service  │  │
│  └──────────────┘  └──────┬───────┘  └─────────┬────────────────┘  │
└────────────────────────────┼──────────────────────┼──────────────────┘
                             │                      │
                             │ Store OTP           │ Publish Events
                             ▼                      ▼
                    ┌────────────────────────────────────┐
                    │         Redis (Port 6379)          │
                    │  ┌──────────────────────────────┐  │
                    │  │  OTP Storage (5 min TTL)    │  │
                    │  │    otp:XXXXXX → userId      │  │
                    │  │    otp:user:userId → OTP    │  │
                    │  └──────────────────────────────┘  │
                    │  ┌──────────────────────────────┐  │
                    │  │  Pub/Sub Channels            │  │
                    │  │    - cart-events             │  │
                    │  │    - checkout-events         │  │
                    │  │    - product-events          │  │
                    │  └──────────────────────────────┘  │
                    └───────────┬────────────────────────┘
                                │
                                │ Subscribe & Validate OTP
                                ▼
                    ┌─────────────────────────────────────┐
                    │  dummy-off2on-redis (Port 8081)     │
                    │  ┌────────────────────────────────┐ │
                    │  │  OTP Validation Service        │ │
                    │  │  (validates from Redis)        │ │
                    │  └──────────┬─────────────────────┘ │
                    │             │                        │
                    │  ┌──────────▼─────────────────────┐ │
                    │  │  SSE Service                    │ │
                    │  │  - Connection Management        │ │
                    │  │  - Event Routing (per userId)   │ │
                    │  │  - Heartbeat (15s)              │ │
                    │  └──────────┬─────────────────────┘ │
                    └─────────────┼───────────────────────┘
                                  │
                                  │ SSE Stream (one per userId)
                                  ▼
                    ┌─────────────────────────────────────┐
                    │          GFD PWA                     │
                    │      (Monitoring Dashboard)          │
                    │  1. Enter OTP from Front-liner      │
                    │  2. Establish SSE connection        │
                    │  3. Receive real-time events        │
                    │  4. Display Front-liner's actions   │
                    └─────────────────────────────────────┘
```

## 🔐 OTP Flow - Detailed

### Step 1: OTP Generation (dummy-off2on)

```
Front-liner PWA → POST /api/otp/generate
{
  "userId": "FL123",
  "deviceInfo": "iPhone 15 Pro",
  "sessionId": "session-abc-123"
}

dummy-off2on:
1. Generate secure 6-digit OTP (e.g., "485721")
2. Store in Redis:
   - Key: "otp:485721" → Value: "FL123" (TTL: 5 minutes)
   - Key: "otp:user:FL123" → Value: "485721" (TTL: 5 minutes)
3. Return OTP to Front-liner

Response:
{
  "success": true,
  "otp": "485721",
  "userId": "FL123",
  "expiresAt": 1698765432000,
  "message": "OTP generated successfully",
  "instructions": "Share this OTP with GFD to monitor your session. Valid for 5 minutes."
}
```

### Step 2: Front-liner Displays OTP

```
Front-liner PWA shows big display:
╔══════════════════╗
║   Share Code     ║
║                  ║
║    4 8 5 7 2 1   ║
║                  ║
║ Expires in 4:45  ║
╚══════════════════╝

"Share this code with the person monitoring you"
```

### Step 3: GFD Enters OTP & Connects

```
GFD PWA → GET /api/sse/connect?userId=FL123&otp=485721

dummy-off2on-redis:
1. Query Redis: Get value of "otp:485721"
2. Redis returns: "FL123"
3. Compare: provided userId ("FL123") == stored userId ("FL123") ✓
4. Establish SSE connection for userId "FL123"
5. Delete OTP from Redis (one-time use):
   - Delete "otp:485721"
   - Delete "otp:user:FL123"
6. Send CONNECTION_ESTABLISHED event to GFD PWA

Response: SSE Stream (keeps connection open)
```

### Step 4: Events Flow

```
Front-liner adds product to cart
  ↓
dummy-off2on publishes to Redis "cart-events" channel
  ↓
dummy-off2on-redis receives event
  ↓
Extract userId from event ("FL123")
  ↓
Check if "FL123" has active SSE connection (YES)
  ↓
Forward event to SSE connection for "FL123"
  ↓
GFD PWA receives event and updates UI
```

## 🎯 SSE Connection Strategy

### **Chosen Strategy: One SSE per userId** ✅

```
Mapping: userId → SSE Connection

Example:
- Front-liner "FL123" generates OTP "485721"
- GFD device connects with OTP → SSE connection for "FL123"
- All events for "FL123" go to this SSE connection
- If another GFD device connects with new OTP for "FL123":
  → Old SSE connection is closed
  → New SSE connection established
```

### Why This Strategy?

**Use Case Match:**
- One Front-liner (userId) is monitored by one GFD device at a time
- Simple and clear: "Who am I monitoring?" = one userId
- Easy to understand and implement
- Matches the business requirement

**Characteristics:**
- ✅ Simple implementation
- ✅ Clear one-to-one mapping
- ✅ Easy to debug and monitor
- ✅ No ambiguity in event routing
- ⚠️ Only one GFD device can monitor a Front-liner at a time
- ⚠️ If GFD device disconnects, need new OTP to reconnect

### Alternative Strategies (Not Implemented)

#### Option 2: One SSE per userId + deviceId

```
Mapping: (userId, deviceId) → SSE Connection

Example:
- Multiple GFD devices can monitor same Front-liner
- Device A: SSE for ("FL123", "device-A")
- Device B: SSE for ("FL123", "device-B")
- Both receive same events

When to use:
- Multiple supervisors monitoring same Front-liner
- Training scenarios (trainer + trainee both monitoring)
- Redundant monitoring setups

Complexity:
- Need to manage deviceId
- OTP needs to encode both userId and deviceId
- More connections to manage
- Broadcasting to multiple connections per user
```

#### Option 3: One SSE per event type

```
Mapping: (userId, eventType[]) → SSE Connection

Example:
- GFD connects and subscribes to specific events
- Connection 1: ("FL123", ["cart-events"])
- Connection 2: ("FL123", ["checkout-events"])
- Selective event delivery

When to use:
- Different dashboards for different event types
- Performance optimization (reduce bandwidth)
- Granular monitoring needs

Complexity:
- Multiple connections per client
- Need subscription management
- Event filtering logic
- Client-side connection management
```

#### Option 4: One SSE per session/tab

```
Mapping: (userId, sessionId, tabId) → SSE Connection

Example:
- Multiple tabs in same browser get separate connections
- Tab 1: SSE for ("FL123", "session-abc", "tab-1")
- Tab 2: SSE for ("FL123", "session-abc", "tab-2")

When to use:
- Multi-tab support required
- Different views in different tabs
- Tab-specific state management

Complexity:
- Session management required
- Tab lifecycle tracking
- Potential duplicate event delivery
- More server resources
```

## 🔄 Connection Lifecycle

### 1. Connection Establishment

```java
// GFD PWA connects
GET /api/sse/connect?userId=FL123&otp=485721

// Server validates OTP
String validatedUserId = otpService.validateAndGetUserId(otp);
// Returns "FL123" if valid, null if invalid/expired

// Check for existing connection
if (sseService.hasActiveConnection(userId)) {
    sseService.removeEmitter(userId); // Close old connection
}

// Create new SSE emitter
SseEmitter emitter = sseService.createEmitter(userId);

// Invalidate OTP (one-time use)
otpService.invalidateOTP(otp);

// Return SSE stream
return ResponseEntity.ok(emitter);
```

### 2. Event Routing

```java
// Event received from Redis
CartEvent event = parseEvent(message);
String userId = event.getUserId(); // "FL123"

// Check if user has active connection
if (sseService.hasActiveConnection(userId)) {
    // Send event to specific user's SSE connection
    sseService.sendEventToUser(userId, event);
} else {
    // No active connection, event is dropped
    logger.debug("User {} has no active SSE connection", userId);
}
```

### 3. Heartbeat

```java
// Every 15 seconds
@Scheduled(fixedDelayString = "15000")
public void sendHeartbeat() {
    emitters.forEach((userId, emitter) -> {
        try {
            emitter.send(SseEmitter.event()
                .name("heartbeat")
                .data("ping"));
        } catch (IOException e) {
            // Connection dead, remove it
            removeEmitter(userId);
        }
    });
}
```

### 4. Connection Termination

```
Reasons for termination:
1. Client closes connection (browser closed, navigated away)
2. Connection timeout (30 minutes default)
3. Network error
4. Server restart
5. Manual disconnect
6. New OTP used (replaces old connection)

On termination:
- onComplete() callback → remove from map
- onTimeout() callback → remove from map
- onError() callback → remove from map
```

## 📊 Connection State Management

### In-Memory Storage (dummy-off2on-redis)

```java
// Map: userId → SseEmitter
private final Map<String, SseEmitter> emitters = new ConcurrentHashMap<>();

// Store connection
emitters.put("FL123", sseEmitter);

// Check connection
boolean active = emitters.containsKey("FL123");

// Remove connection
SseEmitter emitter = emitters.remove("FL123");
emitter.complete();

// Get connection count
int count = emitters.size();
```

### Redis Storage (OTPs)

```
# OTP storage with TTL
SET otp:485721 "FL123" EX 300  # Expires in 300 seconds (5 minutes)
SET otp:user:FL123 "485721" EX 300

# Validate OTP
GET otp:485721  # Returns "FL123" or nil

# Invalidate OTP
DEL otp:485721
DEL otp:user:FL123
```

## 🔍 Monitoring & Debugging

### Check Active Connections

```bash
# Get count of active SSE connections
curl http://localhost:8081/api/sse/connections/count
# Response: 3

# Check specific user's connection status
curl http://localhost:8081/api/sse/status/FL123
# Response: "Connected" or "Not connected"
```

### Check OTP Status

```bash
# Check if OTP exists (dummy-off2on)
curl http://localhost:8080/api/otp/exists/485721
# Response: {"success":true,"message":"OTP exists","data":true}

# Check if OTP exists (dummy-off2on-redis)
curl http://localhost:8081/api/otp/exists/485721
# Response: {"success":true,"message":"OTP exists in Redis","data":true}
```

### Redis Monitoring

```bash
# Connect to Redis CLI
redis-cli

# Check OTP keys
KEYS otp:*

# Get OTP value
GET otp:485721

# Check TTL
TTL otp:485721  # Returns seconds remaining
```

## 🚦 Error Scenarios

### Scenario 1: OTP Expired

```
GFD connects with expired OTP
  ↓
Redis returns nil for "otp:485721"
  ↓
Server returns 401 Unauthorized
  ↓
GFD displays: "OTP expired. Please get a new code from Front-liner."
```

### Scenario 2: OTP Already Used

```
GFD-1 connects with OTP "485721" (Success, OTP deleted)
  ↓
GFD-2 tries same OTP "485721"
  ↓
Redis returns nil (OTP was deleted)
  ↓
Server returns 401 Unauthorized
  ↓
GFD-2 displays: "OTP already used or expired."
```

### Scenario 3: Wrong userId

```
OTP "485721" belongs to "FL123"
GFD connects with userId="FL999" and otp="485721"
  ↓
Redis returns "FL123" for otp
  ↓
Server compares: "FL999" ≠ "FL123"
  ↓
Server returns 401 Unauthorized
  ↓
GFD displays: "User ID does not match OTP."
```

### Scenario 4: Connection Lost

```
SSE connection active for "FL123"
  ↓
Network interruption / Browser closed
  ↓
Heartbeat fails to send
  ↓
onError() callback triggered
  ↓
Connection removed from map
  ↓
New OTP required to reconnect
```

## 🎯 Benefits of Updated Architecture

### 1. **Clear Separation of Concerns**
```
dummy-off2on: Business logic + OTP generation
dummy-off2on-redis: Monitoring + SSE + OTP validation
Redis: Shared state + Message broker
```

### 2. **Natural User Flow**
```
Front-liner initiates → Generates OTP → Shares with GFD
(Not: GFD requests → Gets OTP → Waits for Front-liner)
```

### 3. **Scalability**
```
- Redis-based OTP storage (shared across instances)
- OTPs automatically expire (no manual cleanup)
- Stateless OTP validation (any instance can validate)
```

### 4. **Security**
```
- One-time use OTPs
- Time-based expiration (5 minutes)
- Secure random generation
- Can't reuse or guess OTPs
```

### 5. **Debugging & Monitoring**
```
- OTP status visible in both services
- Connection status endpoints
- Redis for state inspection
- Clear logging at each step
```

## 📝 Updated API Documentation

### dummy-off2on (Port 8080)

```
POST /api/otp/generate
  Request: {"userId": "FL123", "deviceInfo": "iPhone"}
  Response: {"otp": "485721", "expiresAt": 1698765432000}

GET /api/otp/exists/{otp}
  Response: {"data": true/false}
```

### dummy-off2on-redis (Port 8081)

```
GET /api/sse/connect?userId={userId}&otp={otp}
  Returns: SSE Stream

POST /api/otp/validate
  Request: {"userId": "FL123", "otp": "485721"}
  Response: {"valid": true/false}

GET /api/otp/exists/{otp}
  Response: {"data": true/false}

GET /api/sse/status/{userId}
  Response: "Connected" or "Not connected"

GET /api/sse/connections/count
  Response: 3
```

## 🎊 Summary

**What Changed:**
- ✅ OTP generation moved to dummy-off2on
- ✅ OTP storage moved to Redis (shared state)
- ✅ OTP validation reads from Redis
- ✅ Clear service separation
- ✅ Natural user flow

**Connection Strategy:**
- ✅ One SSE connection per userId (Front-liner session)
- ✅ Simple and clear mapping
- ✅ Matches business requirements
- ✅ Easy to debug and monitor

**Next Steps:**
- Test the updated flow
- Update frontend integrations
- Consider multi-device support for Phase 2
- Add monitoring and analytics

---

**Architecture Version**: 2.0  
**Last Updated**: October 26, 2025  
**Status**: ✅ Implemented and Ready for Testing

