<template>
  <div class="cart-display">
    <!-- Header with connection status -->
    <div class="header">
      <div class="store-info">
        <div class="store-logo">
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
              d="M16 11V7a4 4 0 00-8 0v4M5 9h14l1 12H4L5 9z"
            />
          </svg>
        </div>
        <div>
          <h1>Your Cart</h1>
          <p class="session-id">Session: {{ userId }}</p>
        </div>
      </div>

      <button @click="handleDisconnect" class="disconnect-button">
        <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke="currentColor">
          <path
            stroke-linecap="round"
            stroke-linejoin="round"
            stroke-width="2"
            d="M17 16l4-4m0 0l-4-4m4 4H7m6 4v1a3 3 0 01-3 3H6a3 3 0 01-3-3V7a3 3 0 013-3h4a3 3 0 013 3v1"
          />
        </svg>
        Disconnect
      </button>
    </div>

    <!-- Connection status badge -->
    <div class="status-bar">
      <div class="status-badge connected">
        <span class="pulse"></span>
        Connected
      </div>
      <div v-if="lastMessage" class="last-message">
        {{ lastMessage }}
      </div>
    </div>

    <!-- Main content -->
    <div class="content">
      <!-- Empty cart state -->
      <div v-if="cartItems.length === 0" class="empty-cart">
        <div class="empty-icon">
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
              d="M3 3h2l.4 2M7 13h10l4-8H5.4M7 13L5.4 5M7 13l-2.293 2.293c-.63.63-.184 1.707.707 1.707H17m0 0a2 2 0 100 4 2 2 0 000-4zm-8 2a2 2 0 11-4 0 2 2 0 014 0z"
            />
          </svg>
        </div>
        <h2>Your cart is empty</h2>
        <p>Items added by the cashier will appear here</p>
      </div>

      <!-- Cart items -->
      <div v-else class="cart-items-container">
        <div class="cart-items-list">
          <TransitionGroup name="list">
            <CartItemCard
              v-for="item in cartItems"
              :key="item.productId"
              :item="item"
            />
          </TransitionGroup>
        </div>

        <!-- Cart summary -->
        <div class="cart-summary">
          <div class="summary-row">
            <span>Items</span>
            <span class="value">{{ totalItems }}</span>
          </div>
          <div class="summary-row subtotal">
            <span>Subtotal</span>
            <span class="value">${{ formatPrice(totalAmount) }}</span>
          </div>
          <div class="summary-row total">
            <span>Total</span>
            <span class="value">${{ formatPrice(totalAmount) }}</span>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { storeToRefs } from 'pinia'
import { useGFDStore } from '@/stores/gfdStore'
import CartItemCard from './CartItemCard.vue'

const gfdStore = useGFDStore()
const { cartItems, totalAmount, totalItems, userId, lastMessage } = storeToRefs(gfdStore)

const handleDisconnect = () => {
  gfdStore.disconnect()
}

const formatPrice = (price: number): string => {
  return price.toFixed(2)
}
</script>

<style scoped>
.cart-display {
  min-height: 100vh;
  background: linear-gradient(to bottom, #f7fafc 0%, #edf2f7 100%);
}

.header {
  background: white;
  padding: 20px 24px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
}

.store-info {
  display: flex;
  align-items: center;
  gap: 16px;
}

.store-logo {
  width: 56px;
  height: 56px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.store-logo svg {
  width: 32px;
  height: 32px;
  color: white;
}

.store-info h1 {
  font-size: 24px;
  font-weight: 700;
  color: #1a202c;
  margin: 0 0 4px;
}

.session-id {
  font-size: 14px;
  color: #718096;
  margin: 0;
}

.disconnect-button {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 20px;
  background: white;
  border: 2px solid #e2e8f0;
  border-radius: 8px;
  color: #4a5568;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.3s;
}

.disconnect-button svg {
  width: 20px;
  height: 20px;
}

.disconnect-button:hover {
  border-color: #cbd5e0;
  background: #f7fafc;
}

.status-bar {
  background: white;
  padding: 12px 24px;
  display: flex;
  align-items: center;
  gap: 16px;
  border-bottom: 1px solid #e2e8f0;
}

.status-badge {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 6px 12px;
  border-radius: 20px;
  font-size: 13px;
  font-weight: 600;
}

.status-badge.connected {
  background: #c6f6d5;
  color: #22543d;
}

.pulse {
  width: 8px;
  height: 8px;
  background: #48bb78;
  border-radius: 50%;
  animation: pulse 2s infinite;
}

@keyframes pulse {
  0%,
  100% {
    opacity: 1;
  }
  50% {
    opacity: 0.5;
  }
}

.last-message {
  font-size: 14px;
  color: #4a5568;
  font-style: italic;
}

.content {
  padding: 24px;
  max-width: 1200px;
  margin: 0 auto;
}

.empty-cart {
  background: white;
  border-radius: 16px;
  padding: 60px 40px;
  text-align: center;
}

.empty-icon {
  width: 120px;
  height: 120px;
  background: #f7fafc;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  margin: 0 auto 24px;
}

.empty-icon svg {
  width: 60px;
  height: 60px;
  color: #cbd5e0;
}

.empty-cart h2 {
  font-size: 24px;
  font-weight: 700;
  color: #2d3748;
  margin: 0 0 8px;
}

.empty-cart p {
  font-size: 16px;
  color: #718096;
  margin: 0;
}

.cart-items-container {
  display: grid;
  grid-template-columns: 1fr 380px;
  gap: 24px;
}

@media (max-width: 968px) {
  .cart-items-container {
    grid-template-columns: 1fr;
  }
}

.cart-items-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.cart-summary {
  background: white;
  border-radius: 16px;
  padding: 24px;
  height: fit-content;
  position: sticky;
  top: 24px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.05);
}

.summary-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 0;
  font-size: 16px;
  color: #4a5568;
}

.summary-row.subtotal {
  border-top: 1px solid #e2e8f0;
  margin-top: 8px;
  padding-top: 20px;
  font-weight: 600;
}

.summary-row.total {
  font-size: 24px;
  font-weight: 700;
  color: #1a202c;
  border-top: 2px solid #e2e8f0;
  margin-top: 8px;
  padding-top: 20px;
}

.value {
  font-weight: 600;
}

/* List transition animations */
.list-enter-active,
.list-leave-active {
  transition: all 0.5s ease;
}

.list-enter-from {
  opacity: 0;
  transform: translateX(-30px);
}

.list-leave-to {
  opacity: 0;
  transform: translateX(30px);
}

.list-move {
  transition: transform 0.5s ease;
}
</style>

