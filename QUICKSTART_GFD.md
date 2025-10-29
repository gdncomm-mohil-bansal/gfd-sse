# GFD Quick Start Guide

## 🚀 Get Running in 5 Minutes

### Step 1: Start Redis
```bash
docker run -d -p 6379:6379 redis:latest
```

### Step 2: Start Backend Services

**Terminal 1 - Start POS Backend (Port 8080)**:
```bash
cd dummy-off2on
mvn spring-boot:run
```

**Terminal 2 - Start SSE Service (Port 8081)**:
```bash
cd dummy-off2on-redis
mvn spring-boot:run
```

### Step 3: Start GFD Frontend

**Terminal 3 - Start GFD (Port 5173)**:
```bash
cd gfd
npm install  # First time only
npm run dev
```

### Step 4: Test the System

**Generate OTP**:
```bash
curl -X POST http://localhost:8080/api/otp/generate \
  -H "Content-Type: application/json" \
  -d '{"userId": "customer1"}'
```

**Output**:
```json
{
  "otp": "123456",
  "userId": "customer1",
  "expiresIn": 300,
  "message": "OTP generated successfully"
}
```

### Step 5: Connect GFD

1. Open browser to: `http://localhost:5173`
2. Enter Session ID: `customer1`
3. Enter OTP: `123456` (from Step 4)
4. Click **Connect**
5. You should see "Connected" badge ✅

### Step 6: Add Items to Cart

```bash
# Add a Laptop (Product ID: 1)
curl -X POST http://localhost:8080/api/cart/add \
  -H "Content-Type: application/json" \
  -d '{"userId":"customer1","productId":1,"quantity":2}'

# Add a Watch (Product ID: 2)
curl -X POST http://localhost:8080/api/cart/add \
  -H "Content-Type: application/json" \
  -d '{"userId":"customer1","productId":2,"quantity":1}'
```

**Result**: Items appear instantly on GFD with animations! 🎉

### Step 7: Test Checkout

```bash
curl -X POST http://localhost:8080/api/cart/checkout \
  -H "Content-Type: application/json" \
  -d '{"userId":"customer1","paymentMethod":"Credit Card"}'
```

**Result**: Checkout modal appears with success animation! ✨

## 🎯 What You Should See

### 1. Connection Screen
<img src="https://via.placeholder.com/600x400/667eea/ffffff?text=Connection+Form" width="600"/>

- Beautiful gradient background
- Session ID input
- 6-digit OTP input
- Connect button

### 2. Cart Display
<img src="https://via.placeholder.com/600x400/f7fafc/1a202c?text=Cart+Display" width="600"/>

- Product cards with images
- Quantity badges
- Running totals
- Connected status indicator

### 3. Checkout Modal
<img src="https://via.placeholder.com/600x400/48bb78/ffffff?text=Checkout+Success" width="600"/>

- Processing animation
- Order summary
- Success/failure indication

## 📦 What's Included

### Backend (Java Spring Boot)
- ✅ OTP generation with Redis storage
- ✅ Cart management APIs
- ✅ Redis Pub/Sub event publishing
- ✅ SSE connection management
- ✅ Real-time event forwarding

### Frontend (Vue.js)
- ✅ OTP authentication
- ✅ SSE connection handling
- ✅ Real-time cart updates
- ✅ Beautiful, responsive UI
- ✅ Smooth animations
- ✅ Error handling

## 🔧 Configuration

### Change Backend URL
Edit `gfd/src/App.vue`:
```typescript
const API_URL = 'http://your-backend-url:8081'
```

Or create `gfd/.env`:
```env
VITE_API_URL=http://your-backend-url:8081
```

### Change Redis Connection
Edit `application.properties` in both backend projects:
```properties
spring.data.redis.host=your-redis-host
spring.data.redis.port=6379
```

## 🐛 Troubleshooting

### Issue: "Cannot connect to Redis"
```bash
# Check Redis is running
docker ps | grep redis

# Test Redis connection
redis-cli ping
# Should output: PONG
```

### Issue: "OTP Invalid or Expired"
- OTPs expire after 5 minutes
- OTPs are single-use
- Generate a new OTP and try again

### Issue: "No real-time updates"
```bash
# Check Redis pub/sub is working
redis-cli
> SUBSCRIBE cart-events
# In another terminal, add an item
# You should see messages appear
```

### Issue: "Port already in use"
```bash
# Find process using port
lsof -ti:8080  # or 8081, 5173

# Kill the process
kill -9 $(lsof -ti:8080)
```

## 📝 Available Products

| ID | Name       | Price    |
|----|------------|----------|
| 1  | Laptop     | $999.99  |
| 2  | Watch      | $299.99  |
| 3  | Sunglasses | $149.99  |
| 4  | Headphones | $199.99  |
| 5  | Smartwatch | $399.99  |

## 🧪 Test Commands

### Full Test Script
```bash
# Generate OTP
OTP_RESPONSE=$(curl -s -X POST http://localhost:8080/api/otp/generate \
  -H "Content-Type: application/json" \
  -d '{"userId":"test123"}')

OTP=$(echo $OTP_RESPONSE | jq -r '.otp')
echo "Generated OTP: $OTP"

# Now connect GFD with userId: test123 and the OTP

# Add items
curl -X POST http://localhost:8080/api/cart/add \
  -H "Content-Type: application/json" \
  -d '{"userId":"test123","productId":1,"quantity":1}'

curl -X POST http://localhost:8080/api/cart/add \
  -H "Content-Type: application/json" \
  -d '{"userId":"test123","productId":2,"quantity":2}'

# Checkout
curl -X POST http://localhost:8080/api/cart/checkout \
  -H "Content-Type: application/json" \
  -d '{"userId":"test123","paymentMethod":"Card"}'
```

## 🎨 UI Features

### Animations
- ✨ Slide-up animations for modals
- 🔄 Smooth list transitions
- 💫 Loading spinners
- 🎯 Pulsing connection indicator
- 📱 Responsive hover effects

### Responsive Design
- 📱 Mobile (< 768px)
- 💻 Tablet (768px - 968px)
- 🖥️ Desktop (> 968px)

### Color Scheme
- Primary: Purple gradient (#667eea → #764ba2)
- Success: Green (#48bb78)
- Error: Red (#f56565)
- Background: Light gray (#f7fafc)

## 📚 Documentation

- **Full Guide**: `GFD_PROJECT_SUMMARY.md`
- **Implementation**: `gfd/GFD_IMPLEMENTATION_GUIDE.md`
- **Setup Details**: `gfd/SETUP.md`
- **API Docs**: `gfd/README.md`

## 🚀 Production Deployment

```bash
# Build GFD
cd gfd
npm run build

# Output in dist/ folder
# Deploy to:
# - Vercel
# - Netlify  
# - AWS S3 + CloudFront
# - Any static hosting
```

## ✅ Success Checklist

After following this guide, you should have:

- [ ] Redis running on port 6379
- [ ] dummy-off2on running on port 8080
- [ ] dummy-off2on-redis running on port 8081
- [ ] GFD running on port 5173
- [ ] Can generate OTPs
- [ ] Can connect GFD with OTP
- [ ] See real-time cart updates
- [ ] Checkout flow works
- [ ] Can disconnect properly

## 🎉 Next Steps

1. **Customize UI**: Update colors, fonts, images
2. **Add Features**: Order history, loyalty points
3. **Integrate**: Connect to real POS system
4. **Deploy**: Put it in production
5. **Monitor**: Add analytics and logging

## 💡 Tips

- Use Chrome DevTools Network tab to see SSE messages
- Check Redis with `redis-cli MONITOR` to see all operations
- Use browser console to debug frontend issues
- Check backend logs for detailed error messages

## 🤝 Need Help?

1. Check the documentation files
2. Review console/logs for errors
3. Test with curl to isolate issues
4. Ensure all services are running
5. Verify Redis connection

---

**Happy Building! 🎊**

If everything worked, you now have a fully functional real-time Guest-Facing Display system!

