import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import type { CartItem, CartEvent, ConnectionState, EventType, Voucher, Discount, CheckoutData } from '@/types'
import { sseService } from '@/services/sseService'

export const useGFDStore = defineStore('gfd', () => {
  // State
  const connectionState = ref<ConnectionState>({
    isConnected: false,
    userId: null, // Deprecated: keeping for backward compatibility
    error: null
  })

  const cartItems = ref<CartItem[]>([])
  const totalAmount = ref<number>(0)
  const totalItems = ref<number>(0)
  const lastMessage = ref<string>('')
  const lastEventType = ref<EventType | null>(null)
  const isCheckoutMode = ref<boolean>(false)
  const checkoutData = ref<CheckoutData | null>(null)
  const subtotalAmount = ref<number>(0)

  // Voucher and Discount state
  const appliedVoucher = ref<Voucher | null>(null)
  const appliedDiscount = ref<Discount | null>(null)
  const originalAmount = ref<number>(0)

  // API Base URL - configure this based on your backend
  const API_BASE_URL = ref('http://localhost:8081')

  // Connection attempt tracking (to prevent multiple simultaneous calls)
  const connectionAttemptInProgress = ref<boolean>(false)

  // Admin disconnect feature state
  const cartIconTapCount = ref<number>(0)
  const lastCartIconTapTime = ref<number | null>(null)
  const showAdminDialog = ref<boolean>(false)

  // Computed
  const isConnected = computed(() => connectionState.value.isConnected)
  const userId = computed(() => connectionState.value.userId) // Deprecated
  const connectionError = computed(() => connectionState.value.error)
  const hasVoucher = computed(() => appliedVoucher.value !== null)
  const hasDiscount = computed(() => appliedDiscount.value !== null)
  const totalSavings = computed(() => {
    let savings = 0
    if (appliedVoucher.value) {
      savings += appliedVoucher.value.discountAmount
    }
    if (appliedDiscount.value) {
      savings += appliedDiscount.value.discountAmount
    }
    return savings
  })

  // Calculate subtotal from cart items if not provided by backend
  const calculatedSubtotal = computed(() => {
    if (subtotalAmount.value > 0) {
      return subtotalAmount.value
    }
    // Calculate from cart items
    return cartItems.value.reduce((sum, item) => sum + item.subtotal, 0)
  })

  // Actions
  function setApiBaseUrl(url: string) {
    API_BASE_URL.value = url
  }

  function connect(otp: string) {
    // Prevent multiple simultaneous connection attempts
    if (connectionAttemptInProgress.value) {
      console.warn('Connection attempt already in progress')
      return
    }

    connectionState.value.error = null
    connectionAttemptInProgress.value = true

    sseService.connect(
      otp,
      API_BASE_URL.value,
      handleSSEMessage,
      handleSSEError,
      handleConnectionEstablished
    )
  }

  function reconnect() {
    // Prevent multiple simultaneous connection attempts
    if (connectionAttemptInProgress.value) {
      console.warn('Connection attempt already in progress')
      return
    }

    connectionState.value.error = null
    connectionAttemptInProgress.value = true

    // Connect without OTP - will use existing device mapping
    sseService.connect(
      null,
      API_BASE_URL.value,
      handleSSEMessage,
      handleSSEError,
      handleConnectionEstablished
    )
  }

  function handleConnectionEstablished() {
    connectionState.value.isConnected = true
    connectionState.value.error = null
    connectionAttemptInProgress.value = false
    console.log('Connection established successfully')
  }

  function handleSSEMessage(event: CartEvent) {
    console.log('Processing SSE event:', event)

    lastEventType.value = event.eventType
    lastMessage.value = event.message || ''

    // Store subtotal if provided
    if (event.metadata?.subtotal !== null && event.metadata?.subtotal !== undefined) {
      subtotalAmount.value = event.metadata.subtotal
    }

    // Update cart data based on event (only if not null)
    if (event.cartItems !== null && event.cartItems !== undefined) {
      cartItems.value = event.cartItems
    } else if (
      event.eventType === 'CART_ITEM_ADDED' ||
      event.eventType === 'CART_UPDATED'
    ) {
      // WORKAROUND: If cartItems is null but we have metadata with item info,
      // try to reconstruct a basic cart item from metadata
      console.warn('Received cart event with null cartItems, attempting to reconstruct from metadata')

      if (event.metadata && event.totalItems && event.totalAmount) {
        // Create a placeholder item from metadata
        const placeholderItem = {
          productId: event.metadata.productId || 0,
          productName: event.metadata.productName || event.metadata.itemSku || 'Unknown Item',
          price: event.metadata.price || (event.totalAmount / event.totalItems),
          quantity: event.metadata.quantity || event.totalItems,
          subtotal: event.totalAmount
        }

        // If we already have items, try to merge intelligently
        if (cartItems.value.length > 0) {
          // Find existing item
          const existingIndex = cartItems.value.findIndex(
            item => item.productId === placeholderItem.productId
          )

          if (existingIndex >= 0) {
            // Update existing item
            cartItems.value[existingIndex] = placeholderItem
          } else {
            // Add new item
            cartItems.value.push(placeholderItem)
          }
        } else {
          // First item
          cartItems.value = [placeholderItem]
        }

        console.log('Reconstructed cart item from metadata:', placeholderItem)
      }
    }

    // Only update totalAmount if it's a valid number (not 0, null, or undefined)
    // This prevents resetting to 0 during checkout when backend doesn't send these values
    if (event.totalAmount !== null && event.totalAmount !== undefined && event.totalAmount > 0) {
      totalAmount.value = event.totalAmount
    } else if (event.totalAmount === 0 && event.eventType !== 'CHECKOUT_INITIATED' && event.eventType !== 'CHECKOUT_CANCELLED') {
      // Only set to 0 if it's explicitly 0 and not a checkout event
      totalAmount.value = 0
    }

    // Only update totalItems if it's a valid number
    if (event.totalItems !== null && event.totalItems !== undefined && event.totalItems > 0) {
      totalItems.value = event.totalItems
    } else if (event.totalItems === 0 && event.eventType !== 'CHECKOUT_INITIATED' && event.eventType !== 'CHECKOUT_CANCELLED') {
      // Only set to 0 if it's explicitly 0 and not a checkout event
      totalItems.value = 0
    }

    // Handle voucher events
    if (event.eventType === 'VOUCHER_APPLIED' && event.voucher) {
      appliedVoucher.value = event.voucher
      if (event.metadata?.originalAmount) {
        originalAmount.value = event.metadata.originalAmount
      }
      console.log('Voucher applied:', event.voucher)
    } else if (event.eventType === 'VOUCHER_UNAPPLIED') {
      appliedVoucher.value = null
      console.log('Voucher removed')
    }

    // Handle discount events
    if (event.eventType === 'DISCOUNT_APPLIED' && event.discount) {
      appliedDiscount.value = event.discount
      if (event.metadata?.originalAmount) {
        originalAmount.value = event.metadata.originalAmount
      }
      console.log('Discount applied:', event.discount)
    } else if (event.eventType === 'DISCOUNT_REMOVED') {
      appliedDiscount.value = null
      console.log('Discount removed')
    }

    // Handle checkout events
    if (event.eventType === 'CHECKOUT_INITIATED') {
      isCheckoutMode.value = true
      checkoutData.value = event.metadata
    } else if (event.eventType === 'CHECKOUT_CANCELLED') {
      // Immediately close checkout dialog and return to cart
      isCheckoutMode.value = false
      checkoutData.value = null
      console.log('Checkout cancelled by user')
    } else if (event.eventType === 'CHECKOUT_COMPLETED' || event.eventType === 'CHECKOUT_FAILED') {
      // Keep checkout mode for a few seconds to show result
      setTimeout(() => {
        isCheckoutMode.value = false
        checkoutData.value = null
        // Clear vouchers/discounts after checkout
        appliedVoucher.value = null
        appliedDiscount.value = null
        originalAmount.value = 0
        subtotalAmount.value = 0
      }, 3000)
    }
  }

  // function handleSSEError(error: string) {
  //   connectionState.value.error = error
  //   connectionState.value.isConnected = false
  //   connectionAttemptInProgress.value = false
  // }
  function handleSSEError(error: string) {
    // Treat server-initiated disconnects as graceful resets
    if (error === 'Disconnected by Front-liner' || error === 'Connection closed by server') {
      console.log('Server-initiated disconnect:', error)
      connectionState.value.error = null
      connectionState.value.isConnected = false
      connectionAttemptInProgress.value = false

      // Reset all state so UI returns to connection screen cleanly
      cartItems.value = []
      totalAmount.value = 0
      totalItems.value = 0
      lastMessage.value = error
      lastEventType.value = null
      isCheckoutMode.value = false
      checkoutData.value = null
      appliedVoucher.value = null
      appliedDiscount.value = null
      originalAmount.value = 0
      subtotalAmount.value = 0
    } else {
      // This is an actual error
      connectionState.value.error = error
      connectionState.value.isConnected = false
      connectionAttemptInProgress.value = false
    }
  }

  async function disconnect() {
      try {
      await sseService.disconnectFromServer(API_BASE_URL.value)
      } catch (error) {
        console.error('Error during disconnect:', error)
      sseService.disconnect()
    }

    // Reset state
    connectionState.value = {
      isConnected: false,
      userId: null,
      error: null
    }
    cartItems.value = []
    totalAmount.value = 0
    totalItems.value = 0
    lastMessage.value = ''
    lastEventType.value = null
    isCheckoutMode.value = false
    checkoutData.value = null
    appliedVoucher.value = null
    appliedDiscount.value = null
    originalAmount.value = 0
    subtotalAmount.value = 0
    connectionAttemptInProgress.value = false
    cartIconTapCount.value = 0
    lastCartIconTapTime.value = null
  }

  function clearError() {
    connectionState.value.error = null
  }

  /**
   * Handle cart icon tap for admin disconnect feature
   * Requires 7 taps within 2 seconds to open admin dialog
   */
  function handleCartIconTap() {
    const now = Date.now()
    const tapTimeout = 2000 // 2 seconds window for 7 taps

    // Reset count if last tap was more than 2 seconds ago
    if (lastCartIconTapTime.value && (now - lastCartIconTapTime.value) > tapTimeout) {
      cartIconTapCount.value = 0
    }

    cartIconTapCount.value++
    lastCartIconTapTime.value = now

    console.log(`Cart icon tap count: ${cartIconTapCount.value}`)

    // Open admin dialog after 7 taps
    if (cartIconTapCount.value >= 7) {
      showAdminDialog.value = true
      cartIconTapCount.value = 0 // Reset count
    }
  }

  /**
   * Verify admin passcode and disconnect
   * @param passcode - Admin passcode
   * @returns True if passcode is correct
   */
  function verifyAdminPasscode(passcode: string): boolean {
    const correctPasscode = '#Admin007'

    if (passcode === correctPasscode) {
      disconnect()
      showAdminDialog.value = false
      return true
    }

    return false
  }

  /**
   * Close admin dialog
   */
  function closeAdminDialog() {
    showAdminDialog.value = false
    cartIconTapCount.value = 0
  }

  return {
    // State
    connectionState,
    cartItems,
    totalAmount,
    totalItems,
    lastMessage,
    lastEventType,
    isCheckoutMode,
    checkoutData,
    appliedVoucher,
    appliedDiscount,
    originalAmount,
    subtotalAmount,
    API_BASE_URL,
    connectionAttemptInProgress,
    showAdminDialog,

    // Computed
    isConnected,
    userId,
    connectionError,
    hasVoucher,
    hasDiscount,
    totalSavings,
    calculatedSubtotal,

    // Actions
    setApiBaseUrl,
    connect,
    reconnect,
    disconnect,
    clearError,
    handleCartIconTap,
    verifyAdminPasscode,
    closeAdminDialog
  }
})

