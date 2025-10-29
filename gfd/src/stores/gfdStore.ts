import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import type { CartItem, CartEvent, ConnectionState, EventType } from '@/types'
import { sseService } from '@/services/sseService'

export const useGFDStore = defineStore('gfd', () => {
  // State
  const connectionState = ref<ConnectionState>({
    isConnected: false,
    userId: null,
    error: null
  })

  const cartItems = ref<CartItem[]>([])
  const totalAmount = ref<number>(0)
  const totalItems = ref<number>(0)
  const lastMessage = ref<string>('')
  const lastEventType = ref<EventType | null>(null)
  const isCheckoutMode = ref<boolean>(false)
  const checkoutData = ref<any>(null)

  // API Base URL - configure this based on your backend
  const API_BASE_URL = ref('http://localhost:8081')

  // Computed
  const isConnected = computed(() => connectionState.value.isConnected)
  const userId = computed(() => connectionState.value.userId)
  const connectionError = computed(() => connectionState.value.error)

  // Actions
  function setApiBaseUrl(url: string) {
    API_BASE_URL.value = url
  }

  function connect(userId: string, otp: string) {
    connectionState.value.error = null

    sseService.connect(
      userId,
      otp,
      API_BASE_URL.value,
      handleSSEMessage,
      handleSSEError,
      handleConnectionEstablished
    )

    connectionState.value.userId = userId
  }

  function handleConnectionEstablished() {
    connectionState.value.isConnected = true
    connectionState.value.error = null
    console.log('Connection established successfully')
  }

  function handleSSEMessage(event: CartEvent) {
    console.log('Processing SSE event:', event)

    lastEventType.value = event.eventType
    lastMessage.value = event.message || ''

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

    if (event.totalAmount !== null && event.totalAmount !== undefined) {
      totalAmount.value = event.totalAmount
    }

    if (event.totalItems !== null && event.totalItems !== undefined) {
      totalItems.value = event.totalItems
    }

    // Handle checkout events
    if (event.eventType === 'CHECKOUT_INITIATED') {
      isCheckoutMode.value = true
      checkoutData.value = event.metadata
    } else if (event.eventType === 'CHECKOUT_COMPLETED' || event.eventType === 'CHECKOUT_FAILED') {
      // Keep checkout mode for a few seconds to show result
      setTimeout(() => {
        isCheckoutMode.value = false
        checkoutData.value = null
      }, 3000)
    }
  }

  function handleSSEError(error: string) {
    connectionState.value.error = error
    connectionState.value.isConnected = false
  }

  async function disconnect() {
    if (connectionState.value.userId) {
      try {
        await sseService.disconnectFromServer(
          connectionState.value.userId,
          API_BASE_URL.value
        )
      } catch (error) {
        console.error('Error during disconnect:', error)
      }
    } else {
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
  }

  function clearError() {
    connectionState.value.error = null
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
    API_BASE_URL,

    // Computed
    isConnected,
    userId,
    connectionError,

    // Actions
    setApiBaseUrl,
    connect,
    disconnect,
    clearError
  }
})

