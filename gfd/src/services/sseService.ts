import type { CartEvent } from '@/types'

export class SSEService {
  private eventSource: EventSource | null = null
  private reconnectAttempts = 0
  private maxReconnectAttempts = 3
  private reconnectDelay = 2000
  private deviceId: string = ''

  /**
   * Get or create deviceId for this GFD device
   */
  private getDeviceId(): string {
    if (this.deviceId) {
      return this.deviceId
    }

    // Try to get from cookie
    const cookie = document.cookie
      .split('; ')
      .find(row => row.startsWith('deviceId='))

    if (cookie) {
      const cookieValue = cookie.split('=')[1]
      if (cookieValue) {
        this.deviceId = cookieValue
      return this.deviceId
      }
    }

    // Generate new deviceId
    this.deviceId = `gfd-device-${Date.now()}-${Math.random().toString(36).substring(7)}`

    // Set cookie (expires in 30 days)
    const expiryDate = new Date()
    expiryDate.setDate(expiryDate.getDate() + 30)
    document.cookie = `deviceId=${this.deviceId}; expires=${expiryDate.toUTCString()}; path=/; SameSite=Lax`

    console.log('Generated new deviceId:', this.deviceId)
    return this.deviceId
  }

  connect(
    otp: string | null,
    baseUrl: string,
    onMessage: (event: CartEvent) => void,
    onError: (error: string) => void,
    onConnectionEstablished: () => void
  ): void {
    // Close existing connection if any
    this.disconnect()

    // Ensure we have a deviceId
    const deviceId = this.getDeviceId()

    // Build URL with optional OTP
    let url = `${baseUrl}/api/sse/connect`
    if (otp) {
      url += `?otp=${encodeURIComponent(otp)}`
    }

    console.log('Connecting to SSE:', url, 'with deviceId:', deviceId)

    // EventSource with credentials to send cookies
    this.eventSource = new EventSource(url, { withCredentials: true })

    this.eventSource.onopen = () => {
      console.log('SSE connection opened')
      this.reconnectAttempts = 0
      onConnectionEstablished()
    }

    // Handle default message events (without custom event type)
    this.eventSource.onmessage = (event) => {
      try {
        const data: CartEvent = JSON.parse(event.data)
        console.log('Received SSE message (default):', data)
        onMessage(data)
      } catch (error) {
        console.error('Error parsing SSE message:', error)
      }
    }

    // Handle custom event types
    const eventTypes = [
      'CONNECTION_ESTABLISHED',
      'PRODUCT_VIEWED',
      'CART_ITEM_ADDED',
      'CART_ITEM_REMOVED',
      'CART_UPDATED',
      'VOUCHER_APPLIED',
      'VOUCHER_UNAPPLIED',
      'DISCOUNT_APPLIED',
      'DISCOUNT_REMOVED',
      'CHECKOUT_INITIATED',
      'CHECKOUT_CANCELLED',
      'CHECKOUT_COMPLETED',
      'CHECKOUT_FAILED',
      'HEARTBEAT',
      'GFD_DISCONNECTED'
    ]

    eventTypes.forEach((eventType) => {
      this.eventSource!.addEventListener(eventType, (event: MessageEvent) => {
        try {
          // Skip heartbeat events
          if (eventType === 'HEARTBEAT' || event.data === 'ping') {
            console.log('Received heartbeat')
            return
          }
          if (eventType === 'GFD_DISCONNECTED') {
            console.log('Received GFD disconnected event')
            this.disconnect()
            onError('Disconnected by Front-liner')
            return
          }

          const data: CartEvent = JSON.parse(event.data)
          console.log(`Received SSE event [${eventType}]:`, data)
          onMessage(data)
        } catch (error) {
          console.error(`Error parsing SSE event [${eventType}]:`, error)
        }
      })
    })

    this.eventSource.onerror = (error) => {
      console.error('SSE error:', error)

      if (this.eventSource?.readyState === EventSource.CLOSED) {
        // Connection closed - report error but DO NOT auto-retry
        // User must manually click Connect or Reconnect button
        onError('Connection closed by server')
        // Close the connection to prevent any further retries
        this.disconnect()
      } else if (this.eventSource?.readyState === EventSource.CONNECTING) {
        // Still connecting - wait for it to complete or fail
        console.log('SSE connecting...')
      } else {
        // Connection failed - report error but DO NOT auto-retry
        onError('Connection failed. Please try again.')
        // Close the connection to prevent any further retries
        this.disconnect()
    }
  }
  }

  /**
   * @deprecated This method is no longer used for auto-reconnect.
   * Auto-reconnect has been disabled - users must manually click Connect/Reconnect.
   * This method is kept for backward compatibility but does nothing.
   */
  private handleReconnect(
    otp: string | null,
    baseUrl: string,
    onMessage: (event: CartEvent) => void,
    onError: (error: string) => void,
    onConnectionEstablished: () => void
  ): void {
    // Auto-reconnect disabled - user must manually click Connect/Reconnect button
    // This prevents infinite retry loops when OTP is invalid or connection fails
    console.log('Auto-reconnect disabled. User must manually click Connect/Reconnect.')
    onError('Connection failed. Please click Connect or Reconnect to try again.')
    this.disconnect()
  }

  disconnect(): void {
    if (this.eventSource) {
      console.log('Closing SSE connection')
      this.eventSource.close()
      this.eventSource = null
      this.reconnectAttempts = 0
    }
  }

  isConnected(): boolean {
    return this.eventSource !== null && this.eventSource.readyState === EventSource.OPEN
  }

  async disconnectFromServer(baseUrl: string): Promise<void> {
    try {
      const response = await fetch(`${baseUrl}/api/sse/disconnect`, {
        method: 'POST',
        credentials: 'include' // Send cookies
      })

      if (!response.ok) {
        throw new Error('Failed to disconnect from server')
      }

      console.log('Disconnected from server successfully')
    } catch (error) {
      console.error('Error disconnecting from server:', error)
      throw error
    } finally {
      this.disconnect()
    }
  }

  getStoredDeviceId(): string | null {
    return this.deviceId || null
  }
}

export const sseService = new SSEService()

