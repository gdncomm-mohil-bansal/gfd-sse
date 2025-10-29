<template>
  <div class="cart-item-card">
    <!-- Product Image -->
    <div class="item-image-container">
      <img :src="productImage" :alt="item.productName" class="product-image" />
    </div>

    <!-- Product Details -->
    <div class="item-info">
      <h3 class="product-name">{{ item.productName }}</h3>
      <div class="product-meta">
        <span class="quantity-text">Qty: {{ item.quantity }}</span>
        <span class="price-each">${{ formatPrice(item.price) }} each</span>
      </div>
    </div>

    <!-- Subtotal -->
    <div class="item-subtotal">
      <span class="subtotal-amount">${{ formatPrice(item.subtotal) }}</span>
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
  border-radius: 16px;
  padding: 16px;
  display: flex;
  align-items: center;
  gap: 16px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.08);
  transition: all 0.3s ease;
  animation: slideIn 0.4s ease-out;
  border: 1px solid #f0f0f0;
}

@keyframes slideIn {
  from {
    opacity: 0;
    transform: translateX(-20px);
  }
  to {
    opacity: 1;
    transform: translateX(0);
  }
}

.cart-item-card:hover {
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.12);
  border-color: #e0e0e0;
}

/* Product Image */
.item-image-container {
  flex-shrink: 0;
  width: 80px;
  height: 80px;
  border-radius: 12px;
  overflow: hidden;
  background: linear-gradient(135deg, #f5f7fa 0%, #e9ecef 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  border: 2px solid #f8f9fa;
}

.product-image {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 0.3s ease;
}

.cart-item-card:hover .product-image {
  transform: scale(1.05);
}

/* Product Info */
.item-info {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 6px;
  min-width: 0;
}

.product-name {
  font-size: 16px;
  font-weight: 600;
  color: #1a1a1a;
  margin: 0;
  line-height: 1.4;
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}

.product-meta {
  display: flex;
  align-items: center;
  gap: 12px;
  font-size: 14px;
}

.quantity-text {
  display: inline-flex;
  align-items: center;
  padding: 4px 12px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  border-radius: 20px;
  font-weight: 600;
  font-size: 13px;
  letter-spacing: 0.3px;
}

.price-each {
  color: #6b7280;
  font-size: 13px;
  font-weight: 500;
}

/* Subtotal */
.item-subtotal {
  flex-shrink: 0;
  text-align: right;
  padding-left: 16px;
}

.subtotal-amount {
  font-size: 20px;
  font-weight: 700;
  color: #10b981;
  display: block;
  white-space: nowrap;
}

/* Responsive Design */
@media (max-width: 768px) {
  .cart-item-card {
    flex-wrap: wrap;
    gap: 12px;
  }

  .item-image-container {
    width: 70px;
    height: 70px;
  }

  .item-info {
    flex: 1;
    min-width: calc(100% - 86px);
  }

  .product-name {
    font-size: 15px;
  }

  .product-meta {
    flex-direction: column;
    align-items: flex-start;
    gap: 6px;
  }

  .item-subtotal {
    width: 100%;
    text-align: right;
    padding-left: 0;
    padding-top: 8px;
    border-top: 1px solid #f0f0f0;
  }

  .subtotal-amount {
    font-size: 18px;
  }
}

@media (max-width: 480px) {
  .cart-item-card {
    padding: 12px;
  }

  .item-image-container {
    width: 60px;
    height: 60px;
  }

  .product-name {
    font-size: 14px;
  }

  .subtotal-amount {
    font-size: 16px;
  }
}
</style>

