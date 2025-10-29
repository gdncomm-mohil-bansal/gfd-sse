<template>
  <div class="checkout-overlay">
    <div class="checkout-modal">
      <!-- Checkout header -->
      <div class="checkout-header" :class="statusClass">
        <div class="status-icon">
          <svg
            v-if="eventType === 'CHECKOUT_INITIATED'"
            xmlns="http://www.w3.org/2000/svg"
            fill="none"
            viewBox="0 0 24 24"
            stroke="currentColor"
            class="spinner-icon"
          >
            <path
              stroke-linecap="round"
              stroke-linejoin="round"
              stroke-width="2"
              d="M4 4v5h.582m15.356 2A8.001 8.001 0 004.582 9m0 0H9m11 11v-5h-.581m0 0a8.003 8.003 0 01-15.357-2m15.357 2H15"
            />
          </svg>
          <svg
            v-else-if="eventType === 'CHECKOUT_COMPLETED'"
            xmlns="http://www.w3.org/2000/svg"
            fill="none"
            viewBox="0 0 24 24"
            stroke="currentColor"
          >
            <path
              stroke-linecap="round"
              stroke-linejoin="round"
              stroke-width="2"
              d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z"
            />
          </svg>
          <svg
            v-else
            xmlns="http://www.w3.org/2000/svg"
            fill="none"
            viewBox="0 0 24 24"
            stroke="currentColor"
          >
            <path
              stroke-linecap="round"
              stroke-linejoin="round"
              stroke-width="2"
              d="M10 14l2-2m0 0l2-2m-2 2l-2-2m2 2l2 2m7-2a9 9 0 11-18 0 9 9 0 0118 0z"
            />
          </svg>
        </div>
        <h2>{{ statusTitle }}</h2>
        <p>{{ statusMessage }}</p>
      </div>

      <!-- Checkout details -->
      <div class="checkout-body">
        <!-- Cart summary -->
        <div class="summary-section">
          <h3>Order Summary</h3>
          <div class="summary-items">
            <div v-for="item in cartItems" :key="item.productId" class="summary-item">
              <span class="item-info">
                {{ item.productName }} <span class="item-qty">× {{ item.quantity }}</span>
              </span>
              <span class="item-price">${{ formatPrice(item.subtotal) }}</span>
            </div>
          </div>
        </div>

        <!-- Pricing breakdown -->
        <div class="pricing-section">
          <div class="price-row">
            <span>Subtotal</span>
            <span>${{ formatPrice(totalAmount) }}</span>
          </div>
          <div class="price-row" v-if="discountAmount > 0">
            <span class="discount-label">Discount</span>
            <span class="discount-value">-${{ formatPrice(discountAmount) }}</span>
          </div>
          <div class="price-row" v-if="taxAmount > 0">
            <span>Tax</span>
            <span>${{ formatPrice(taxAmount) }}</span>
          </div>
          <div class="price-row total">
            <span>Total</span>
            <span>${{ formatPrice(finalTotal) }}</span>
          </div>
        </div>

        <!-- Payment info -->
        <div v-if="checkoutData && checkoutData.paymentMethod" class="payment-info">
          <div class="info-badge">
            <svg
              xmlns="http://www.w3.org/2000/svg"
              fill="none"
              viewBox="0 0 24 24"
              stroke="currentColor"
            >
              <path
                stroke-linecap="round"
                stroke-linejoin="round"
                stroke-width="2"
                d="M3 10h18M7 15h1m4 0h1m-7 4h12a3 3 0 003-3V8a3 3 0 00-3-3H6a3 3 0 00-3 3v8a3 3 0 003 3z"
              />
            </svg>
            <span>{{ checkoutData.paymentMethod }}</span>
          </div>
        </div>
      </div>

      <!-- Action button -->
      <div v-if="eventType === 'CHECKOUT_COMPLETED'" class="checkout-footer">
        <p class="thank-you">Thank you for your purchase!</p>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { storeToRefs } from 'pinia'
import { useGFDStore } from '@/stores/gfdStore'
import type { EventType } from '@/types'

const gfdStore = useGFDStore()
const { cartItems, totalAmount, lastEventType, checkoutData } = storeToRefs(gfdStore)

const eventType = computed((): EventType | null => lastEventType.value)

const statusClass = computed(() => {
  if (eventType.value === 'CHECKOUT_COMPLETED') return 'success'
  if (eventType.value === 'CHECKOUT_FAILED') return 'error'
  return 'processing'
})

const statusTitle = computed(() => {
  if (eventType.value === 'CHECKOUT_COMPLETED') return 'Payment Successful!'
  if (eventType.value === 'CHECKOUT_FAILED') return 'Payment Failed'
  return 'Processing Payment...'
})

const statusMessage = computed(() => {
  if (eventType.value === 'CHECKOUT_COMPLETED') return 'Your order has been completed successfully'
  if (eventType.value === 'CHECKOUT_FAILED') return 'There was an issue processing your payment'
  return 'Please wait while we process your payment'
})

const discountAmount = computed((): number => {
  return checkoutData.value?.discount || 0
})

const taxAmount = computed((): number => {
  return checkoutData.value?.tax || 0
})

const finalTotal = computed((): number => {
  return totalAmount.value - discountAmount.value + taxAmount.value
})

const formatPrice = (price: number): string => {
  return price.toFixed(2)
}
</script>

<style scoped>
.checkout-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.7);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
  animation: fadeIn 0.3s ease-out;
}

@keyframes fadeIn {
  from {
    opacity: 0;
  }
  to {
    opacity: 1;
  }
}

.checkout-modal {
  background: white;
  border-radius: 20px;
  max-width: 600px;
  width: 90%;
  max-height: 90vh;
  overflow: auto;
  animation: slideUp 0.4s ease-out;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.3);
}

@keyframes slideUp {
  from {
    transform: translateY(50px);
    opacity: 0;
  }
  to {
    transform: translateY(0);
    opacity: 1;
  }
}

.checkout-header {
  padding: 40px;
  text-align: center;
  border-radius: 20px 20px 0 0;
}

.checkout-header.processing {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
}

.checkout-header.success {
  background: linear-gradient(135deg, #48bb78 0%, #38a169 100%);
  color: white;
}

.checkout-header.error {
  background: linear-gradient(135deg, #f56565 0%, #c53030 100%);
  color: white;
}

.status-icon {
  width: 80px;
  height: 80px;
  margin: 0 auto 20px;
  background: rgba(255, 255, 255, 0.2);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
}

.status-icon svg {
  width: 48px;
  height: 48px;
}

.spinner-icon {
  animation: spin 2s linear infinite;
}

@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}

.checkout-header h2 {
  font-size: 28px;
  font-weight: 700;
  margin: 0 0 8px;
}

.checkout-header p {
  font-size: 16px;
  margin: 0;
  opacity: 0.9;
}

.checkout-body {
  padding: 32px 40px;
}

.summary-section {
  margin-bottom: 24px;
}

.summary-section h3 {
  font-size: 18px;
  font-weight: 600;
  color: #2d3748;
  margin: 0 0 16px;
}

.summary-items {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.summary-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px;
  background: #f7fafc;
  border-radius: 8px;
}

.item-info {
  font-size: 15px;
  color: #2d3748;
}

.item-qty {
  color: #718096;
  font-size: 14px;
}

.item-price {
  font-weight: 600;
  color: #2d3748;
}

.pricing-section {
  border-top: 2px solid #e2e8f0;
  padding-top: 20px;
  margin-bottom: 24px;
}

.price-row {
  display: flex;
  justify-content: space-between;
  padding: 12px 0;
  font-size: 16px;
  color: #4a5568;
}

.discount-label {
  color: #48bb78;
  font-weight: 600;
}

.discount-value {
  color: #48bb78;
  font-weight: 600;
}

.price-row.total {
  font-size: 24px;
  font-weight: 700;
  color: #1a202c;
  border-top: 2px solid #e2e8f0;
  margin-top: 8px;
  padding-top: 20px;
}

.payment-info {
  display: flex;
  justify-content: center;
  margin-top: 24px;
}

.info-badge {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 12px 24px;
  background: #edf2f7;
  border-radius: 8px;
  font-weight: 600;
  color: #2d3748;
}

.info-badge svg {
  width: 20px;
  height: 20px;
}

.checkout-footer {
  padding: 24px 40px 40px;
  text-align: center;
}

.thank-you {
  font-size: 18px;
  font-weight: 600;
  color: #48bb78;
  margin: 0;
}
</style>

