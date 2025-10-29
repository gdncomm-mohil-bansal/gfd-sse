// Event types from backend
export enum EventType {
  PRODUCT_VIEWED = 'PRODUCT_VIEWED',
  CART_ITEM_ADDED = 'CART_ITEM_ADDED',
  CART_ITEM_REMOVED = 'CART_ITEM_REMOVED',
  CART_UPDATED = 'CART_UPDATED',
  VOUCHER_APPLIED = 'VOUCHER_APPLIED',
  VOUCHER_UNAPPLIED = 'VOUCHER_UNAPPLIED',
  DISCOUNT_APPLIED = 'DISCOUNT_APPLIED',
  DISCOUNT_REMOVED = 'DISCOUNT_REMOVED',
  CHECKOUT_INITIATED = 'CHECKOUT_INITIATED',
  CHECKOUT_CANCELLED = 'CHECKOUT_CANCELLED',
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

// Voucher interface
export interface Voucher {
  voucherCode: string
  voucherType: 'PERCENTAGE' | 'FIXED_AMOUNT' | 'FREE_SHIPPING'
  discountValue: number
  discountAmount: number
  description?: string
  appliedAt?: number
}

// Discount interface
export interface Discount {
  discountId: string
  discountName: string
  discountType: 'PERCENTAGE' | 'FIXED_AMOUNT' | 'BUY_X_GET_Y'
  discountValue: number
  discountAmount: number
  description?: string
  appliedAt?: number
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
  voucher?: Voucher
  discount?: Discount
}

// Checkout data interface
export interface CheckoutData {
  orderId?: string
  timestamp?: number
  paymentMethod?: string
  [key: string]: unknown
}

// Connection state
export interface ConnectionState {
  isConnected: boolean
  userId: string | null
  error: string | null
}

