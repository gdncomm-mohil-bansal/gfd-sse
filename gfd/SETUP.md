# GFD Setup Guide

## Quick Start

### 1. Install Dependencies
```bash
cd gfd
npm install
```

### 2. Configure API URL

Create a `.env` file in the `gfd` directory:

```env
VITE_API_URL=http://localhost:8081
```

Or update the URL in `src/App.vue` directly:
```typescript
const API_URL = 'http://localhost:8081'
```

### 3. Start Development Server
```bash
npm run dev
```

The app will open at `http://localhost:5173`

## Testing the Application

### Full Flow Test

1. **Start All Services**:
   ```bash
   # Terminal 1: Start Redis
   docker run -p 6379:6379 redis:latest
   
   # Terminal 2: Start dummy-off2on (Port 8080)
   cd dummy-off2on
   mvn spring-boot:run
   
   # Terminal 3: Start dummy-off2on-redis (Port 8081)
   cd dummy-off2on-redis
   mvn spring-boot:run
   
   # Terminal 4: Start GFD
   cd gfd
   npm run dev
   ```

2. **Generate OTP** (via Postman or curl):
   ```bash
   curl -X POST http://localhost:8080/api/otp/generate \
     -H "Content-Type: application/json" \
     -d '{"userId": "user123"}'
   ```
   
   Response will contain the 6-digit OTP.

3. **Connect GFD**:
   - Open `http://localhost:5173` in browser
   - Enter Session ID: `user123`
   - Enter the 6-digit OTP from step 2
   - Click "Connect"

4. **Add Items to Cart**:
   ```bash
   curl -X POST http://localhost:8080/api/cart/add \
     -H "Content-Type: application/json" \
     -d '{
       "userId": "user123",
       "productId": 1,
       "quantity": 2
     }'
   ```
   
   You should see the cart update in real-time on the GFD!

5. **Checkout**:
   ```bash
   curl -X POST http://localhost:8080/api/cart/checkout \
     -H "Content-Type: application/json" \
     -d '{
       "userId": "user123",
       "paymentMethod": "Credit Card"
     }'
   ```
   
   The checkout modal will appear on the GFD.

## Environment Variables

Create a `.env` file with:

```env
# Backend API URL (dummy-off2on-redis)
VITE_API_URL=http://localhost:8081
```

For production, update this to your actual backend URL.

## Available Scripts

- `npm run dev` - Start development server
- `npm run build` - Build for production
- `npm run preview` - Preview production build
- `npm run type-check` - Run TypeScript type checking
- `npm run lint` - Run linter

## Browser Compatibility

- Chrome/Edge 90+
- Firefox 88+
- Safari 14+
- EventSource API support required

## Common Issues

### 1. CORS Errors
Make sure the backend has CORS enabled for your frontend URL:
```java
@CrossOrigin(origins = "*")
```

### 2. SSE Connection Fails
- Check that backend is running on port 8081
- Verify OTP is valid and not expired
- Check browser console for detailed errors

### 3. No Real-time Updates
- Ensure Redis is running
- Check that both backend services are connected to Redis
- Verify the cart events are being published to Redis

### 4. OTP Invalid
- OTPs expire after 5 minutes
- OTPs are single-use only
- Generate a new OTP if expired

## Production Build

```bash
npm run build
```

Deploy the `dist/` folder to any static hosting service:
- Vercel
- Netlify
- AWS S3 + CloudFront
- Nginx
- Apache

Update the API URL environment variable for production:
```env
VITE_API_URL=https://your-backend-api.com
```

## Development Tips

### Hot Module Replacement
Changes to Vue components will hot-reload automatically during development.

### TypeScript
Full TypeScript support is included. Types are defined in `src/types/index.ts`.

### State Management
Uses Pinia for state management. Store is in `src/stores/gfdStore.ts`.

### SSE Service
SSE connection logic is abstracted in `src/services/sseService.ts`.

## Customization

### Change Theme Colors
Update the gradient colors in component styles:
```css
background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
```

### Add Product Images
Update the image mapping in `CartItemCard.vue`:
```typescript
const imageMap: Record<number, string> = {
  1: 'https://your-image-url.com/product1.jpg',
  // ...
}
```

### Modify Event Handling
Update `gfdStore.ts` `handleSSEMessage()` function to handle custom events.

## Architecture

```
User Browser (GFD)
    ↓
SSE Connection (/api/sse/connect)
    ↓
dummy-off2on-redis (Port 8081)
    ↓
Redis Pub/Sub
    ↑
dummy-off2on (Port 8080)
    ↑
POS/Cashier Actions
```

## Next Steps

After getting the basic setup working:

1. Customize the UI to match your brand
2. Add authentication if needed
3. Integrate with your actual POS system
4. Add analytics/monitoring
5. Deploy to production

Happy coding! 🚀

