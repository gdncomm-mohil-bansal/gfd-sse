<template>
  <div class="cart-display">
    <!-- Header with connection status -->
    <div class="header">
      <div class="store-info">
        <div class="store-logo" @click="handleCartIconTap" title="Tap 7 times for admin options">
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
        <div class="store-text">
          <h1>Shopping Cart</h1>
          <p class="session-id">Session ID: {{ userId }}</p>
        </div>
      </div>
    </div>

    <!-- Connection status badge -->
    <div class="status-bar">
      <div class="status-badge connected">
        <span class="pulse"></span>
        <span>Live Updates Active</span>
      </div>
      <div v-if="lastMessage" class="last-message">
        <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke="currentColor" class="message-icon">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M13 16h-1v-4h-1m1-4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
        </svg>
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
              d="M16 11V7a4 4 0 00-8 0v4M5 9h14l1 12H4L5 9z"
            />
          </svg>
        </div>
        <h2>Your Cart is Empty</h2>
        <p>Waiting for items from the cashier...</p>
        <div class="loading-dots">
          <span></span>
          <span></span>
          <span></span>
        </div>
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

          <!-- Voucher & Discount Cards -->
          <VoucherDiscountCard
            :voucher="appliedVoucher"
            :discount="appliedDiscount"
            :totalSavings="totalSavings"
          />
        </div>

        <!-- Cart summary -->
        <div class="cart-summary">
          <h3 class="summary-title">Order Summary</h3>
          <div class="summary-divider"></div>
          <div class="summary-row">
            <span>Total Items</span>
            <span class="value">{{ totalItems }}</span>
          </div>
          <div class="summary-row">
            <span>Subtotal</span>
            <span class="value">Rp {{ formatPrice(originalAmount > 0 ? originalAmount : calculatedSubtotal) }}</span>
          </div>
          <div class="summary-row savings" v-if="totalSavings > 0">
            <span>Total Savings</span>
            <span class="value savings-value">-Rp {{ formatPrice(totalSavings) }}</span>
          </div>
          <div class="summary-divider"></div>
          <div class="summary-row total">
            <span>Total Amount</span>
            <span class="value">Rp {{ formatPrice(totalAmount) }}</span>
          </div>
          <div class="summary-note">
            <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M13 16h-1v-4h-1m1-4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
            </svg>
            <span>Prices are updated in real-time</span>
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
import VoucherDiscountCard from './VoucherDiscountCard.vue'
import { formatRupiah } from '@/utils/currency'

const gfdStore = useGFDStore()
const {
  cartItems,
  totalAmount,
  totalItems,
  userId,
  lastMessage,
  appliedVoucher,
  appliedDiscount,
  totalSavings,
  originalAmount,
  calculatedSubtotal
} = storeToRefs(gfdStore)

/**
 * Handle cart icon tap for admin disconnect feature
 * Requires 7 taps within 2 seconds to open admin dialog
 */
const handleCartIconTap = () => {
  gfdStore.handleCartIconTap()
}

const formatPrice = (price: number): string => {
  return formatRupiah(price)
}
</script>

<style scoped>
.cart-display {
  min-height: 100vh;
  background: linear-gradient(to bottom, #f7fafc 0%, #edf2f7 100%);
}

.header {
  background: white;
  padding: 24px 32px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.06);
  border-bottom: 1px solid #f0f0f0;
}

.store-info {
  display: flex;
  align-items: center;
  gap: 16px;
}

.store-logo {
  width: 60px;
  height: 60px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border-radius: 14px;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 4px 12px rgba(102, 126, 234, 0.25);
  cursor: pointer;
  transition: all 0.2s ease;
  user-select: none;
}

.store-logo:hover {
  transform: scale(1.05);
  box-shadow: 0 6px 16px rgba(102, 126, 234, 0.35);
}

.store-logo:active {
  transform: scale(0.95);
}

.store-logo svg {
  width: 34px;
  height: 34px;
  color: white;
}

.store-text h1 {
  font-size: 26px;
  font-weight: 700;
  color: #111827;
  margin: 0 0 4px;
  letter-spacing: -0.5px;
}

.session-id {
  font-size: 13px;
  color: #9ca3af;
  margin: 0;
  font-weight: 500;
}

.status-bar {
  background: linear-gradient(to bottom, #fafbfc 0%, #ffffff 100%);
  padding: 14px 32px;
  display: flex;
  align-items: center;
  gap: 16px;
  border-bottom: 1px solid #f0f0f0;
}

.status-badge {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 16px;
  border-radius: 24px;
  font-size: 13px;
  font-weight: 600;
  letter-spacing: 0.3px;
}

.status-badge.connected {
  background: linear-gradient(135deg, #d1fae5 0%, #a7f3d0 100%);
  color: #065f46;
  border: 1px solid #a7f3d0;
}

.pulse {
  width: 8px;
  height: 8px;
  background: #10b981;
  border-radius: 50%;
  animation: pulse 2s infinite;
  box-shadow: 0 0 0 0 rgba(16, 185, 129, 0.7);
}

@keyframes pulse {
  0% {
    box-shadow: 0 0 0 0 rgba(16, 185, 129, 0.7);
  }
  50% {
    box-shadow: 0 0 0 6px rgba(16, 185, 129, 0);
  }
  100% {
    box-shadow: 0 0 0 0 rgba(16, 185, 129, 0);
  }
}

.last-message {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
  color: #6b7280;
  padding: 6px 14px;
  background: #f9fafb;
  border-radius: 8px;
  border: 1px solid #e5e7eb;
}

.message-icon {
  width: 16px;
  height: 16px;
  flex-shrink: 0;
}

.content {
  padding: 24px;
  max-width: 1200px;
  margin: 0 auto;
}

.empty-cart {
  background: white;
  border-radius: 20px;
  padding: 80px 40px;
  text-align: center;
  border: 2px dashed #e5e7eb;
}

.empty-icon {
  width: 140px;
  height: 140px;
  background: linear-gradient(135deg, #f3f4f6 0%, #e5e7eb 100%);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  margin: 0 auto 28px;
  border: 3px solid #f9fafb;
}

.empty-icon svg {
  width: 70px;
  height: 70px;
  color: #9ca3af;
}

.empty-cart h2 {
  font-size: 26px;
  font-weight: 700;
  color: #111827;
  margin: 0 0 12px;
  letter-spacing: -0.5px;
}

.empty-cart p {
  font-size: 16px;
  color: #6b7280;
  margin: 0 0 24px;
}

.loading-dots {
  display: flex;
  justify-content: center;
  gap: 8px;
}

.loading-dots span {
  width: 10px;
  height: 10px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border-radius: 50%;
  animation: bounce 1.4s infinite ease-in-out both;
}

.loading-dots span:nth-child(1) {
  animation-delay: -0.32s;
}

.loading-dots span:nth-child(2) {
  animation-delay: -0.16s;
}

@keyframes bounce {
  0%,
  80%,
  100% {
    transform: scale(0);
    opacity: 0.5;
  }
  40% {
    transform: scale(1);
    opacity: 1;
  }
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
  border-radius: 20px;
  padding: 28px;
  height: fit-content;
  position: sticky;
  top: 24px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
  border: 1px solid #f0f0f0;
}

.summary-title {
  font-size: 20px;
  font-weight: 700;
  color: #111827;
  margin: 0 0 16px;
  letter-spacing: -0.3px;
}

.summary-divider {
  height: 1px;
  background: linear-gradient(to right, transparent, #e5e7eb, transparent);
  margin: 16px 0;
}

.summary-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 10px 0;
  font-size: 15px;
  color: #6b7280;
}

.summary-row.total {
  font-size: 24px;
  font-weight: 700;
  color: #111827;
  padding: 16px 0 0;
  margin-top: 4px;
}

.summary-row.total .value {
  color: #10b981;
}

.summary-row.savings {
  color: #10b981;
  font-weight: 600;
}

.savings-value {
  color: #10b981 !important;
  font-weight: 700;
}

.value {
  font-weight: 600;
  color: #374151;
}

.summary-note {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-top: 20px;
  padding: 12px;
  background: linear-gradient(135deg, #fef3c7 0%, #fde68a 100%);
  border-radius: 10px;
  font-size: 13px;
  color: #92400e;
  border: 1px solid #fcd34d;
}

.summary-note svg {
  width: 18px;
  height: 18px;
  flex-shrink: 0;
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

