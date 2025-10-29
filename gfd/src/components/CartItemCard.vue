<template>
  <div class="cart-item-card">
    <div class="item-image">
      <img :src="productImage" :alt="item.productName" />
    </div>

    <div class="item-details">
      <h3 class="item-name">{{ item.productName }}</h3>
      <p class="item-price">${{ formatPrice(item.price) }} each</p>
    </div>

    <div class="item-quantity">
      <div class="quantity-badge">
        <span class="qty-label">Qty</span>
        <span class="qty-value">{{ item.quantity }}</span>
      </div>
    </div>

    <div class="item-subtotal">
      <p class="subtotal-label">Subtotal</p>
      <p class="subtotal-value">${{ formatPrice(item.subtotal) }}</p>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import type { CartItem } from '@/types'

interface Props {
  item: CartItem
}

const props = defineProps<Props>()

const formatPrice = (price: number): string => {
  return price.toFixed(2)
}

// Generate a product image based on product ID
// In a real app, you'd fetch actual product images
const productImage = computed(() => {
  const imageMap: Record<number, string> = {
    1: 'https://images.unsplash.com/photo-1496181133206-80ce9b88a853?w=200&h=200&fit=crop', // Laptop
    2: 'https://images.unsplash.com/photo-1523275335684-37898b6baf30?w=200&h=200&fit=crop', // Watch
    3: 'https://images.unsplash.com/photo-1572635196237-14b3f281503f?w=200&h=200&fit=crop', // Sunglasses
    4: 'https://images.unsplash.com/photo-1505740420928-5e560c06d30e?w=200&h=200&fit=crop', // Headphones
    5: 'https://images.unsplash.com/photo-1546868871-7041f2a55e12?w=200&h=200&fit=crop' // Smartwatch
  }

  return (
    imageMap[props.item.productId] ||
    `https://ui-avatars.com/api/?name=${encodeURIComponent(props.item.productName)}&size=200&background=667eea&color=fff&bold=true`
  )
})
</script>

<style scoped>
.cart-item-card {
  background: white;
  border-radius: 12px;
  padding: 20px;
  display: grid;
  grid-template-columns: 100px 1fr auto auto;
  gap: 20px;
  align-items: center;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
  transition: all 0.3s;
  animation: slideIn 0.5s ease-out;
}

@keyframes slideIn {
  from {
    opacity: 0;
    transform: translateY(20px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.cart-item-card:hover {
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.1);
  transform: translateY(-2px);
}

.item-image {
  width: 100px;
  height: 100px;
  border-radius: 8px;
  overflow: hidden;
  background: #f7fafc;
  display: flex;
  align-items: center;
  justify-content: center;
}

.item-image img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.item-details {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.item-name {
  font-size: 18px;
  font-weight: 600;
  color: #1a202c;
  margin: 0;
}

.item-price {
  font-size: 14px;
  color: #718096;
  margin: 0;
}

.item-quantity {
  display: flex;
  align-items: center;
}

.quantity-badge {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  padding: 12px 20px;
  border-radius: 8px;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
  min-width: 80px;
}

.qty-label {
  font-size: 11px;
  text-transform: uppercase;
  letter-spacing: 0.5px;
  opacity: 0.9;
}

.qty-value {
  font-size: 24px;
  font-weight: 700;
}

.item-subtotal {
  text-align: right;
  min-width: 120px;
}

.subtotal-label {
  font-size: 13px;
  color: #718096;
  margin: 0 0 4px;
}

.subtotal-value {
  font-size: 22px;
  font-weight: 700;
  color: #2d3748;
  margin: 0;
}

@media (max-width: 768px) {
  .cart-item-card {
    grid-template-columns: 80px 1fr;
    grid-template-rows: auto auto;
    gap: 16px;
  }

  .item-image {
    width: 80px;
    height: 80px;
  }

  .item-details {
    grid-column: 2;
  }

  .item-quantity {
    grid-column: 1;
  }

  .item-subtotal {
    grid-column: 2;
    text-align: left;
  }
}
</style>

