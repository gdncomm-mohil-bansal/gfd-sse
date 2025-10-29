# Postman Testing Guide - Updated Architecture

## 📦 What's Updated

Both Postman collections have been updated to reflect the new architecture:

1. **dummy-off2on** collection - Now includes OTP generation (Front-liner side)
2. **dummy-off2on-redis** collection - Only validates OTP and manages SSE (GFD side)

## 🚀 Quick Test (3 Steps)

### Prerequisites
- Both services running (dummy-off2on on 8080, dummy-off2on-redis on 8081)
- Redis running on 6379
- Both Postman collections imported

---

## 📋 Method 1: Simple Flow (Recommended for First Test)

### Step 1: Generate OTP (dummy-off2on collection)

1. Open **"Off2On Service API - Updated"** collection
2. Navigate to: `OTP Management` → `Generate OTP for Front-liner Session`
3. Click **Send**

**Expected Response:**
```json
{
  "success": true,
  "otp": "485721",
  "userId": "user123",
  "expiresAt": 1698765432000,
  "message": "OTP generated successfully",
  "instructions": "Share this OTP with GFD..."
}
```

**✅ OTP is automatically saved to collection variable!**

Console shows:
```
✅ OTP Generated: 485721
📋 Share this OTP with GFD to monitor this session
⏰ Expires at: [timestamp]
```

### Step 2: Connect SSE (dummy-off2on-redis collection)

1. Switch to **"Dummy Off2On Redis - SSE Service (Updated)"** collection
2. Go to: `E2E Test - GFD Flow` → `Step 1: Validate OTP (GFD)`
3. The OTP variable should already be set from Step 1
4. Click **Send** to validate

**Expected Response:**
```json
{
  "valid": true,
  "userId": "user123",
  "message": "OTP is valid"
}
```

5. Now go to: `Step 2: Connect to SSE (Keep this running!)`
6. Click **Send** and **KEEP THIS TAB OPEN**

**Expected Stream:**
```
event:CONNECTION_ESTABLISHED
data:{"eventId":"...","eventType":"CONNECTION_ESTABLISHED",...}

event:heartbeat
data:ping
```

### Step 3: Trigger Events (dummy-off2on collection)

1. Switch back to **"Off2On Service API - Updated"** collection
2. Go to: `Cart Operations` → `Add Product to Cart`
3. Click **Send**

**Watch Step 2 tab in dummy-off2on-redis collection - you should see:**
```
event:CART_ITEM_ADDED
data:{"eventType":"CART_ITEM_ADDED","userId":"user123",...}
```

4. Try checkout:
   - Go to: `Cart Operations` → `Checkout Cart`
   - Click **Send**

**Watch Step 2 tab - you should see:**
```
event:CHECKOUT_COMPLETED
data:{"eventType":"CHECKOUT_COMPLETED","orderId":"ORD-XXXX",...}
```

🎉 **Success! Events are flowing in real-time!**

---

## 📋 Method 2: Automated E2E Test

### Option A: Run Front-liner Flow First

1. Open **"Off2On Service API - Updated"** collection
2. Navigate to folder: `E2E Test - Front-liner Flow`
3. Right-click on the folder → **Run folder**
4. Watch all steps execute:
   - ✅ Step 1: Generate OTP
   - ✅ Step 2: Add Product to Cart
   - ✅ Step 3: Add Another Product
   - ✅ Step 4: Checkout

**Console Output:**
```
✅ OTP GENERATED: 485721
📋 Next Steps:
1. Copy this OTP: 485721
2. Open dummy-off2on-redis Postman collection
3. Run 'E2E Test - GFD Flow' with this OTP
```

### Option B: Run GFD Flow (After Front-liner Flow)

1. Open **"Dummy Off2On Redis - SSE Service (Updated)"** collection
2. Navigate to folder: `E2E Test - GFD Flow (Complete)`
3. Right-click on the folder → **Run folder**

**⚠️ Important Notes:**
- Step 2 (Connect to SSE) will timeout in automated runs (this is normal)
- To see real-time events, run Step 2 manually and keep it open
- Then trigger Steps 3 and 4 manually

**Console Output:**
```
📱 Front-liner Generated OTP: 485721
✅ OTP validated successfully
🛒 Cart event triggered!
📢 Event published to Redis: CART_ITEM_ADDED
👁️  Check Step 2 tab - you should see the event!
🎉 E2E TEST COMPLETE!
```

---

## 📊 Collection Structure

### dummy-off2on Collection (Port 8080)

```
📁 Off2On Service API - Updated
  📁 Health & Status
    └─ Health Check
  
  📁 OTP Management (Front-liner generates OTP)
    ├─ Generate OTP for Front-liner Session ⭐
    └─ Check if OTP Exists
  
  📁 Products
    ├─ Get All Products
    ├─ Get Product By ID
    └─ Get Products By Category
  
  📁 Cart Operations
    ├─ Add Product to Cart ⭐ (triggers event)
    ├─ Add Another Product
    ├─ Get Cart
    ├─ Checkout Cart ⭐ (triggers event)
    └─ Clear Cart
  
  📁 E2E Test - Front-liner Flow ⭐⭐⭐
    ├─ Step 1: Generate OTP (Share with GFD)
    ├─ Step 2: Add Product to Cart
    ├─ Step 3: Add Another Product
    └─ Step 4: Checkout
```

### dummy-off2on-redis Collection (Port 8081)

```
📁 Dummy Off2On Redis - SSE Service (Updated)
  📁 Health & Status
    ├─ Health Check
    └─ Welcome
  
  📁 OTP Validation (OTPs generated in dummy-off2on)
    ├─ Validate OTP from Redis
    └─ Check if OTP Exists in Redis
  
  📁 SSE Connection Management
    ├─ Connect to SSE (with OTP from Front-liner) ⭐
    ├─ Disconnect SSE
    ├─ Check Connection Status
    └─ Get Active Connection Count
  
  📁 E2E Test - GFD Flow (Complete) ⭐⭐⭐
    ├─ Step 0: Generate OTP in dummy-off2on
    ├─ Step 1: Validate OTP (GFD)
    ├─ Step 2: Connect to SSE (Keep running!) ⭐
    ├─ Step 3: Trigger Cart Event
    ├─ Step 4: Trigger Checkout Event
    └─ Step 5: Verify OTP Was Invalidated
```

---

## 🎯 Test Scenarios

### Scenario 1: Happy Path (Recommended First Test)

**Steps:**
1. dummy-off2on: Generate OTP → Get OTP "485721"
2. dummy-off2on-redis: Validate OTP → Valid ✅
3. dummy-off2on-redis: Connect SSE → Connected ✅
4. dummy-off2on: Add to cart → Event received ✅
5. dummy-off2on: Checkout → Event received ✅

**Expected Results:**
- All steps succeed
- Events appear in SSE stream
- OTP invalidated after connection

### Scenario 2: OTP Expiration

**Steps:**
1. dummy-off2on: Generate OTP
2. Wait 6 minutes (or set shorter TTL for testing)
3. dummy-off2on-redis: Try to connect with expired OTP

**Expected Result:**
- 401 Unauthorized
- Message: "Invalid or expired OTP"

### Scenario 3: OTP Reuse (Security Test)

**Steps:**
1. dummy-off2on: Generate OTP
2. dummy-off2on-redis: Connect SSE (Success, OTP invalidated)
3. dummy-off2on-redis: Try to connect again with same OTP

**Expected Result:**
- Second connection fails
- OTP no longer exists in Redis
- Message: "Invalid or expired OTP"

### Scenario 4: Multiple Users

**Steps:**
1. Set userId to "user-A" in both collections
2. Generate OTP for user-A
3. Connect SSE for user-A
4. Change userId to "user-B"
5. Generate OTP for user-B
6. Connect SSE for user-B (in new tab)
7. Trigger cart event for user-A
8. Trigger cart event for user-B

**Expected Result:**
- Each user only receives their own events
- Events are correctly routed by userId

---

## 🔍 Debugging

### Issue: OTP Not Found

**Check:**
```bash
# In terminal
redis-cli GET otp:485721
```

**If nil:**
- OTP might have expired (5-minute TTL)
- Generate new OTP
- Check dummy-off2on logs for errors

### Issue: No Events Received

**Check:**
1. Is SSE connection still active?
   - Look for heartbeat events every 15 seconds
   - If no heartbeats, reconnect

2. Does userId match?
   - Cart operation userId must match SSE connection userId
   - Check collection variables

3. Is Redis Pub/Sub working?
   ```bash
   redis-cli SUBSCRIBE cart-events
   # Trigger cart event, see if it appears
   ```

### Issue: 401 Unauthorized on SSE Connect

**Reasons:**
- OTP expired (5 minutes)
- OTP already used (one-time use)
- OTP doesn't exist in Redis
- Wrong userId with OTP

**Solution:**
- Generate fresh OTP
- Use immediately (within 5 minutes)
- Ensure userId matches

---

## 📝 Collection Variables

### dummy-off2on Collection

| Variable | Default | Description |
|----------|---------|-------------|
| `base_url` | `http://localhost:8080` | dummy-off2on service |
| `userId` | `user123` | Front-liner user ID |
| `otp` | `` | Auto-populated after generation |

### dummy-off2on-redis Collection

| Variable | Default | Description |
|----------|---------|-------------|
| `base_url` | `http://localhost:8081` | dummy-off2on-redis service |
| `off2on_url` | `http://localhost:8080` | For triggering events |
| `userId` | `user123` | Must match Front-liner userId |
| `otp` | `` | Copy from dummy-off2on |

---

## 🎓 Tips & Best Practices

### 1. Keep SSE Tab Open
- SSE requests will run indefinitely (until timeout or disconnect)
- Open in a separate Postman tab
- Watch real-time events appear

### 2. Test Scripts Auto-Save Variables
- OTP is automatically saved when generated
- No need to manually copy/paste
- Works across both collections

### 3. Use Console for Debugging
- Open Postman Console (View → Show Postman Console)
- All test scripts log helpful information
- See exactly what's happening at each step

### 4. Test in Order
- Run E2E tests step-by-step first
- Understand the flow before customizing
- Check console output after each step

### 5. Reset Between Tests
- Generate new OTP for each test run
- OTPs are one-time use
- Clear cart if needed: `Cart Operations` → `Clear Cart`

---

## 🎉 Success Checklist

After running tests, verify:

- [ ] OTP generated in dummy-off2on (Port 8080)
- [ ] OTP stored in Redis (check with redis-cli)
- [ ] OTP validated in dummy-off2on-redis
- [ ] SSE connection established (Port 8081)
- [ ] CONNECTION_ESTABLISHED event received
- [ ] Heartbeat events every 15 seconds
- [ ] Cart events received in real-time
- [ ] Checkout events received
- [ ] OTP invalidated after connection
- [ ] Events only go to correct userId

All checked? **You're ready for production integration! 🚀**

---

## 📚 Related Documentation

- `UPDATED_ARCHITECTURE.md` - Complete architecture guide
- `TESTING_UPDATED_FLOW.md` - Shell script testing
- `README.md` - Project overview
- `SETUP_GUIDE.md` - Setup instructions

---

**Last Updated**: October 26, 2025  
**Version**: 2.0  
**Status**: ✅ Ready to Use

