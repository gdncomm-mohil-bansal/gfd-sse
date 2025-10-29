<template>
  <div class="voucher-discount-section" v-if="hasVoucherOrDiscount">
    <!-- Voucher Card -->
    <transition name="slide-fade">
      <div v-if="voucher" class="savings-card voucher-card">
        <div class="card-icon">
          <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="currentColor">
            <path stroke-linecap="round" stroke-linejoin="round" d="M9 14.25l6-6m4.5-3.493V21.75l-3.75-1.5-3.75 1.5-3.75-1.5-3.75 1.5V4.757c0-1.108.806-2.057 1.907-2.185a48.507 48.507 0 0111.186 0c1.1.128 1.907 1.077 1.907 2.185zM9.75 9h.008v.008H9.75V9zm.375 0a.375.375 0 11-.75 0 .375.375 0 01.75 0zm4.125 4.5h.008v.008h-.008V13.5zm.375 0a.375.375 0 11-.75 0 .375.375 0 01.75 0z" />
          </svg>
        </div>
        <div class="card-content">
          <div class="card-header">
            <h3 class="card-title">Voucher Applied</h3>
            <span class="card-badge">{{ formatVoucherType(voucher.voucherType) }}</span>
          </div>
          <p class="card-code">{{ voucher.voucherCode }}</p>
          <p class="card-description" v-if="voucher.description">{{ voucher.description }}</p>
          <div class="card-savings">
            <span class="savings-label">Savings:</span>
            <span class="savings-amount">-Rp {{ formatPrice(voucher.discountAmount) }}</span>
          </div>
        </div>
      </div>
    </transition>

    <!-- Discount Card -->
    <transition name="slide-fade">
      <div v-if="discount" class="savings-card discount-card">
        <div class="card-icon">
          <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="currentColor">
            <path stroke-linecap="round" stroke-linejoin="round" d="M9.568 3H5.25A2.25 2.25 0 003 5.25v4.318c0 .597.237 1.17.659 1.591l9.581 9.581c.699.699 1.78.872 2.607.33a18.095 18.095 0 005.223-5.223c.542-.827.369-1.908-.33-2.607L11.16 3.66A2.25 2.25 0 009.568 3z" />
            <path stroke-linecap="round" stroke-linejoin="round" d="M6 6h.008v.008H6V6z" />
          </svg>
        </div>
        <div class="card-content">
          <div class="card-header">
            <h3 class="card-title">Discount Applied</h3>
            <span class="card-badge">{{ formatDiscountType(discount.discountType) }}</span>
          </div>
          <p class="card-code">{{ discount.discountName }}</p>
          <p class="card-description" v-if="discount.description">{{ discount.description }}</p>
          <div class="card-savings">
            <span class="savings-label">Savings:</span>
            <span class="savings-amount">-Rp {{ formatPrice(discount.discountAmount) }}</span>
          </div>
        </div>
      </div>
    </transition>

    <!-- Total Savings Summary -->
    <transition name="scale-fade">
      <div v-if="totalSavings > 0" class="total-savings-card">
        <div class="total-savings-content">
          <span class="total-savings-label">
            <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="currentColor" class="savings-icon">
              <path stroke-linecap="round" stroke-linejoin="round" d="M9 12.75L11.25 15 15 9.75M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
            </svg>
            Total Savings
          </span>
          <span class="total-savings-amount">-Rp {{ formatPrice(totalSavings) }}</span>
        </div>
      </div>
    </transition>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import type { Voucher, Discount } from '@/types'
import { formatRupiah } from '@/utils/currency'

interface Props {
  voucher: Voucher | null
  discount: Discount | null
  totalSavings: number
}

const props = defineProps<Props>()

const hasVoucherOrDiscount = computed(() => {
  return props.voucher !== null || props.discount !== null
})

const formatPrice = (price: number): string => {
  return formatRupiah(price)
}

const formatVoucherType = (type: string): string => {
  switch (type) {
    case 'PERCENTAGE':
      return 'Percentage Off'
    case 'FIXED_AMOUNT':
      return 'Fixed Amount'
    case 'FREE_SHIPPING':
      return 'Free Shipping'
    default:
      return type
  }
}

const formatDiscountType = (type: string): string => {
  switch (type) {
    case 'PERCENTAGE':
      return 'Percentage Off'
    case 'FIXED_AMOUNT':
      return 'Fixed Amount'
    case 'BUY_X_GET_Y':
      return 'Buy X Get Y'
    default:
      return type
  }
}
</script>

<style scoped>
.voucher-discount-section {
  display: flex;
  flex-direction: column;
  gap: 12px;
  margin-bottom: 20px;
}

/* Savings Card Base Styles */
.savings-card {
  background: white;
  border-radius: 16px;
  padding: 16px;
  display: flex;
  gap: 16px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
  border: 2px solid;
  transition: all 0.3s ease;
}

.voucher-card {
  border-color: #f59e0b;
  background: linear-gradient(135deg, #fffbeb 0%, #fef3c7 100%);
}

.discount-card {
  border-color: #8b5cf6;
  background: linear-gradient(135deg, #faf5ff 0%, #ede9fe 100%);
}

.savings-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.12);
}

/* Card Icon */
.card-icon {
  flex-shrink: 0;
  width: 48px;
  height: 48px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: transform 0.3s ease;
}

.voucher-card .card-icon {
  background: linear-gradient(135deg, #f59e0b 0%, #d97706 100%);
  color: white;
}

.discount-card .card-icon {
  background: linear-gradient(135deg, #8b5cf6 0%, #7c3aed 100%);
  color: white;
}

.card-icon svg {
  width: 28px;
  height: 28px;
}

.savings-card:hover .card-icon {
  transform: scale(1.1) rotate(5deg);
}

/* Card Content */
.card-content {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
}

.card-title {
  font-size: 16px;
  font-weight: 700;
  color: #1a1a1a;
  margin: 0;
}

.card-badge {
  font-size: 11px;
  font-weight: 600;
  padding: 4px 10px;
  border-radius: 12px;
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.voucher-card .card-badge {
  background: #f59e0b;
  color: white;
}

.discount-card .card-badge {
  background: #8b5cf6;
  color: white;
}

.card-code {
  font-size: 18px;
  font-weight: 800;
  font-family: 'Courier New', monospace;
  letter-spacing: 1px;
  margin: 0;
}

.voucher-card .card-code {
  color: #b45309;
}

.discount-card .card-code {
  color: #6d28d9;
}

.card-description {
  font-size: 13px;
  color: #6b7280;
  margin: 0;
  line-height: 1.4;
}

.card-savings {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding-top: 8px;
  border-top: 1px solid rgba(0, 0, 0, 0.08);
  margin-top: 4px;
}

.savings-label {
  font-size: 13px;
  font-weight: 600;
  color: #6b7280;
}

.savings-amount {
  font-size: 18px;
  font-weight: 800;
  color: #10b981;
}

/* Total Savings Card */
.total-savings-card {
  background: linear-gradient(135deg, #10b981 0%, #059669 100%);
  border-radius: 16px;
  padding: 16px 20px;
  box-shadow: 0 4px 12px rgba(16, 185, 129, 0.3);
  animation: pulse-glow 2s ease-in-out infinite;
}

@keyframes pulse-glow {
  0%, 100% {
    box-shadow: 0 4px 12px rgba(16, 185, 129, 0.3);
  }
  50% {
    box-shadow: 0 8px 24px rgba(16, 185, 129, 0.5);
  }
}

.total-savings-content {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.total-savings-label {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 16px;
  font-weight: 700;
  color: white;
}

.savings-icon {
  width: 24px;
  height: 24px;
}

.total-savings-amount {
  font-size: 24px;
  font-weight: 900;
  color: white;
  text-shadow: 0 2px 4px rgba(0, 0, 0, 0.2);
}

/* Animations */
.slide-fade-enter-active {
  transition: all 0.5s ease;
}

.slide-fade-leave-active {
  transition: all 0.3s ease;
}

.slide-fade-enter-from {
  transform: translateX(-30px);
  opacity: 0;
}

.slide-fade-leave-to {
  transform: translateX(30px);
  opacity: 0;
}

.scale-fade-enter-active {
  transition: all 0.5s ease;
}

.scale-fade-leave-active {
  transition: all 0.3s ease;
}

.scale-fade-enter-from {
  transform: scale(0.9);
  opacity: 0;
}

.scale-fade-leave-to {
  transform: scale(0.9);
  opacity: 0;
}

/* Responsive Design */
@media (max-width: 768px) {
  .savings-card {
    padding: 14px;
  }

  .card-icon {
    width: 44px;
    height: 44px;
  }

  .card-icon svg {
    width: 24px;
    height: 24px;
  }

  .card-title {
    font-size: 15px;
  }

  .card-code {
    font-size: 16px;
  }

  .savings-amount {
    font-size: 16px;
  }

  .total-savings-amount {
    font-size: 20px;
  }
}

@media (max-width: 480px) {
  .card-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 6px;
  }

  .card-savings {
    flex-direction: column;
    align-items: flex-start;
    gap: 4px;
  }
}
</style>

