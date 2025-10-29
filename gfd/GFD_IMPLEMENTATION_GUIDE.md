# GFD (Guest-Facing Display) Implementation Guide

## Overview

The GFD application is a real-time Vue.js web application that displays shopping cart updates to customers via Server-Sent Events (SSE). It connects to the `dummy-off2on-redis` backend service using a secure 6-digit OTP.

## Complete File Structure

```
gfd/
├── src/
│   ├── components/
│   │   ├── ConnectionForm.vue       # OTP entry & connection UI
│   │   ├── CartDisplay.vue          # Main cart display with header
│   │   ├── CartItemCard.vue         # Individual product card
│   │   └── CheckoutDisplay.vue      # Checkout modal overlay
│   ├── stores/
│   │   └── gfdStore.ts              # Pinia state management
│   ├── services/
│   │   └── sseService.ts            # SSE connection logic
│   ├── types/
│   │   └── index.ts                 # TypeScript interfaces
│   ├── App.vue                      # Root component
│   ├── main.ts                      # Application entry
│   └── env.d.ts                     # Environment types
├── package.json
├── README.md
├── SETUP.md
└── GFD_IMPLEMENTATION_GUIDE.md (this file)
```

## Component Breakdown

### 1. ConnectionForm.vue
**Purpose**: Initial connection screen for entering OTP

**Features**:
- Session ID input
- 6-digit OTP input with validation
- Visual feedback during connection
- Error handling and display
- Beautiful gradient design

**Key Functions**:
- Validates OTP format (6 digits)
- Calls `gfdStore.connect(userId, otp)`
- Shows connection status

### 2. CartDisplay.vue
**Purpose**: Main display showing cart items and totals

**Features**:
- Store logo and branding
- Session ID display
- Connected status badge
- Last message display
- Cart items list with transitions
- Running totals (items, subtotal, total)
- Disconnect button

**Computed Properties**:
- `cartItems` - Array of cart items
- `totalAmount` - Total cart value
- `totalItems` - Total item count
- `lastMessage` - Latest event message

### 3. CartItemCard.vue
**Purpose**: Display individual product in cart

**Features**:
- Product image (with fallback to Unsplash or avatars)
- Product name and price
- Quantity badge with gradient
- Subtotal calculation
- Smooth animations
- Responsive layout

**Props**:
- `item: CartItem` - Product data

### 4. CheckoutDisplay.vue
**Purpose**: Modal overlay during checkout process

**Features**:
- Processing/Success/Failed states
- Animated status icons
- Order summary
- Pricing breakdown
- Discount display (if applicable)
- Tax calculation
- Payment method indicator
- Auto-dismiss on completion

**States**:
- `CHECKOUT_INITIATED` - Processing animation
- `CHECKOUT_COMPLETED` - Success checkmark
- `CHECKOUT_FAILED` - Error icon

## State Management (gfdStore.ts)

### State Variables
```typescript
connectionState: {
  isConnected: boolean
  userId: string | null
  error: string | null
}
cartItems: CartItem[]
totalAmount: number
totalItems: number
lastMessage: string
lastEventType: EventType | null
isCheckoutMode: boolean
checkoutData: any
API_BASE_URL: string
```

### Key Actions

#### connect(userId: string, otp: string)
Establishes SSE connection with backend
- Validates OTP via backend
- Sets up event listeners
- Updates connection state

#### disconnect()
Closes SSE connection
- Calls backend disconnect endpoint
- Resets all state
- Closes EventSource

#### handleSSEMessage(event: CartEvent)
Processes incoming SSE events
- Updates cart items
- Updates totals
- Handles checkout mode
- Updates UI based on event type

## SSE Service (sseService.ts)

### Methods

#### connect()
```typescript
connect(
  userId: string,
  otp: string,
  baseUrl: string,
  onMessage: (event: CartEvent) => void,
  onError: (error: string) => void,
  onConnectionEstablished: () => void
): void
```

#### disconnect()
Closes EventSource connection

#### disconnectFromServer()
Calls backend disconnect API

#### isConnected()
Returns connection status

### Reconnection Logic
- Attempts 3 reconnections
- 2-second delay between attempts
- Notifies user after max attempts

## Types (types/index.ts)

### EventType Enum
```typescript
enum EventType {
  PRODUCT_VIEWED
  CART_ITEM_ADDED
  CART_ITEM_REMOVED
  CART_UPDATED
  CHECKOUT_INITIATED
  CHECKOUT_COMPLETED
  CHECKOUT_FAILED
  CONNECTION_ESTABLISHED
  HEARTBEAT
}
```

### CartItem Interface
```typescript
interface CartItem {
  productId: number
  productName: string
  price: number
  quantity: number
  subtotal: number
}
```

### CartEvent Interface
```typescript
interface CartEvent {
  eventId: string
  eventType: EventType
  userId: string
  timestamp: number
  cartItems: CartItem[]
  totalAmount: number
  totalItems: number
  message: string
  metadata?: any
}
```

## Event Flow

### 1. Initial Connection
```
User enters OTP
    ↓
GFD calls /api/sse/connect?userId=X&otp=123456
    ↓
Backend validates OTP from Redis
    ↓
SSE connection established
    ↓
CONNECTION_ESTABLISHED event sent
    ↓
GFD shows cart display
```

### 2. Cart Updates
```
Cashier adds item (dummy-off2on)
    ↓
Cart event published to Redis
    ↓
dummy-off2on-redis receives event
    ↓
Event forwarded via SSE to GFD
    ↓
GFD updates UI in real-time
```

### 3. Checkout Flow
```
Cashier initiates checkout
    ↓
CHECKOUT_INITIATED event sent
    ↓
GFD shows checkout modal
    ↓
Processing animation displayed
    ↓
Payment completes
    ↓
CHECKOUT_COMPLETED event sent
    ↓
Success screen shown
    ↓
Modal auto-dismisses after 3s
```

## API Integration

### SSE Endpoint
```
GET http://localhost:8081/api/sse/connect
Query Parameters:
  - userId: string
  - otp: string (6 digits)
```

### Disconnect Endpoint
```
POST http://localhost:8081/api/sse/disconnect/{userId}
```

### Status Check Endpoint
```
GET http://localhost:8081/api/sse/status/{userId}
```

## Configuration

### Environment Variables

Create `.env` file:
```env
VITE_API_URL=http://localhost:8081
```

Or update in `App.vue`:
```typescript
const API_URL = import.meta.env.VITE_API_URL || 'http://localhost:8081'
gfdStore.setApiBaseUrl(API_URL)
```

### Default Configuration
- **SSE Base URL**: `http://localhost:8081`
- **Reconnect Attempts**: 3
- **Reconnect Delay**: 2000ms
- **Checkout Auto-Dismiss**: 3000ms

## Styling & Design

### Color Scheme
- **Primary Gradient**: `#667eea` → `#764ba2`
- **Success**: `#48bb78`
- **Error**: `#f56565`
- **Background**: `#f7fafc` → `#edf2f7`

### Animations
- **Slide Up**: Entry animation for modals
- **Fade In**: Overlay transitions
- **List Transitions**: Cart item add/remove
- **Pulse**: Connection status indicator
- **Spin**: Loading spinner

### Responsive Breakpoints
- **Mobile**: < 768px
- **Tablet**: 768px - 968px
- **Desktop**: > 968px

## Testing Workflow

### Manual Testing Steps

1. **Start Services**:
   ```bash
   # Redis
   docker run -p 6379:6379 redis
   
   # Backend 1 (Port 8080)
   cd dummy-off2on && mvn spring-boot:run
   
   # Backend 2 (Port 8081)
   cd dummy-off2on-redis && mvn spring-boot:run
   
   # GFD
   cd gfd && npm run dev
   ```

2. **Generate OTP**:
   ```bash
   curl -X POST http://localhost:8080/api/otp/generate \
     -H "Content-Type: application/json" \
     -d '{"userId": "testuser"}'
   ```

3. **Connect GFD**:
   - Open browser to `http://localhost:5173`
   - Enter userId: `testuser`
   - Enter 6-digit OTP from step 2
   - Click Connect

4. **Test Cart Operations**:
   ```bash
   # Add product 1
   curl -X POST http://localhost:8080/api/cart/add \
     -H "Content-Type: application/json" \
     -d '{"userId":"testuser","productId":1,"quantity":2}'
   
   # Add product 2
   curl -X POST http://localhost:8080/api/cart/add \
     -H "Content-Type: application/json" \
     -d '{"userId":"testuser","productId":2,"quantity":1}'
   ```

5. **Test Checkout**:
   ```bash
   curl -X POST http://localhost:8080/api/cart/checkout \
     -H "Content-Type: application/json" \
     -d '{"userId":"testuser","paymentMethod":"Card"}'
   ```

### Expected Results

✅ **Connection**: 
- Shows connected badge
- Displays session ID
- Empty cart message appears

✅ **Adding Items**:
- Items appear with animation
- Totals update immediately
- Images load correctly

✅ **Checkout**:
- Modal appears
- Processing animation shows
- Success message displays
- Modal auto-closes

## Production Deployment

### Build Steps
```bash
cd gfd
npm run build
```

### Output
Static files in `dist/` directory

### Deployment Options

1. **Vercel**:
   ```bash
   npm i -g vercel
   vercel --prod
   ```

2. **Netlify**:
   ```bash
   npm i -g netlify-cli
   netlify deploy --prod --dir=dist
   ```

3. **Docker**:
   ```dockerfile
   FROM nginx:alpine
   COPY dist /usr/share/nginx/html
   EXPOSE 80
   ```

4. **AWS S3 + CloudFront**:
   ```bash
   aws s3 sync dist/ s3://your-bucket/
   aws cloudfront create-invalidation --distribution-id XXX --paths "/*"
   ```

### Environment Configuration
Update API URL for production:
```env
VITE_API_URL=https://api.yourdomain.com
```

## Troubleshooting

### Issue: SSE Connection Fails
**Solution**: 
- Check backend is running on port 8081
- Verify CORS is enabled
- Check OTP is valid
- Review browser console errors

### Issue: No Real-time Updates
**Solution**:
- Verify Redis is running
- Check backend subscribed to correct channel
- Ensure userId matches between systems
- Check network tab for SSE messages

### Issue: OTP Invalid/Expired
**Solution**:
- OTPs expire after 5 minutes
- OTPs are single-use
- Generate new OTP and try again

### Issue: Images Not Loading
**Solution**:
- Check internet connection (Unsplash requires internet)
- Fallback avatars should still work
- Add local images in `CartItemCard.vue`

## Best Practices

### Performance
- Use Vue 3 Composition API for better tree-shaking
- Lazy load images with loading states
- Debounce rapid state updates
- Use `keep-alive` for component caching

### Security
- Always validate OTP server-side
- Use HTTPS in production
- Implement rate limiting
- Sanitize user inputs

### UX
- Show loading states during connection
- Provide clear error messages
- Use smooth transitions
- Maintain connection status visibility

### Maintenance
- Keep dependencies updated
- Monitor SSE connection health
- Log errors appropriately
- Test across browsers

## Future Enhancements

Potential features to add:
- [ ] Multiple language support
- [ ] Dark mode toggle
- [ ] Sound notifications
- [ ] Product categories
- [ ] Barcode scanner integration
- [ ] Receipt printing
- [ ] Loyalty points display
- [ ] Order history
- [ ] Customer feedback
- [ ] Analytics dashboard

## Support & Resources

- **Backend API Docs**: See `dummy-off2on-redis` project
- **Vue.js Docs**: https://vuejs.org/
- **Pinia Docs**: https://pinia.vuejs.org/
- **SSE Spec**: https://html.spec.whatwg.org/multipage/server-sent-events.html

## Summary

The GFD application provides a modern, real-time shopping experience for customers. It leverages SSE for instant updates, Vue 3 for reactive UI, and follows best practices for security and UX. The modular architecture makes it easy to customize and extend for specific business needs.

**Key Highlights**:
- 🔒 Secure OTP authentication
- ⚡ Real-time SSE updates
- 🎨 Beautiful, modern UI
- 📱 Responsive design
- 🔧 Easy to customize
- 🚀 Production-ready

Happy building! 🎉

