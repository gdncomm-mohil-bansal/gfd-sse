// Event types from backend
export enum EventType {
  PRODUCT_VIEWED = 'PRODUCT_VIEWED',
  CART_ITEM_ADDED = 'CART_ITEM_ADDED',
  CART_ITEM_REMOVED = 'CART_ITEM_REMOVED',
  CART_UPDATED = 'CART_UPDATED',
  CHECKOUT_INITIATED = 'CHECKOUT_INITIATED',
  CHECKOUT_COMPLETED = 'CHECKOUT_COMPLETED',
  CHECKOUT_FAILED = 'CHECKOUT_FAILED',
  CONNECTION_ESTABLISHED = 'CONNECTION_ESTABLISHED',
  HEARTBEAT = 'HEARTBEAT'
}

// Cart Item interface
export interface CartItem {
  productId: number
  productName: string
  price: number
  quantity: number
  subtotal: number
}

// Cart Event from SSE
export interface CartEvent {
  eventId: string
  eventType: EventType
  userId: string
  timestamp: number
  cartItems: CartItem[] | null
  totalAmount: number | null
  totalItems: number | null
  message: string | null
  metadata?: any
}

// Connection state
export interface ConnectionState {
  isConnected: boolean
  userId: string | null
  error: string | null
}

