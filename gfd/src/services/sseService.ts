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
      this.deviceId = cookie.split('=')[1]
      return this.deviceId
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
      'HEARTBEAT'
    ]

    eventTypes.forEach((eventType) => {
      this.eventSource!.addEventListener(eventType, (event: any) => {
        try {
          // Skip heartbeat events
          if (eventType === 'HEARTBEAT' || event.data === 'ping') {
            console.log('Received heartbeat')
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
        onError('Connection closed by server')
        // Try to reconnect without OTP (using existing device mapping)
        this.handleReconnect(null, baseUrl, onMessage, onError, onConnectionEstablished)
      } else if (this.eventSource?.readyState === EventSource.CONNECTING) {
        console.log('SSE reconnecting...')
      }
    }
  }

  private handleReconnect(
    otp: string | null,
    baseUrl: string,
    onMessage: (event: CartEvent) => void,
    onError: (error: string) => void,
    onConnectionEstablished: () => void
  ): void {
    if (this.reconnectAttempts < this.maxReconnectAttempts) {
      this.reconnectAttempts++
      console.log(`Attempting to reconnect (${this.reconnectAttempts}/${this.maxReconnectAttempts})...`)

      setTimeout(() => {
        // Reconnect without OTP (will use existing device mapping)
        this.connect(null, baseUrl, onMessage, onError, onConnectionEstablished)
      }, this.reconnectDelay)
    } else {
      console.log('Max reconnect attempts reached')
      onError('Connection lost. Please enter OTP again to reconnect.')
    }
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

