<script setup lang="ts">
import { computed, watch } from 'vue'
import { storeToRefs } from 'pinia'
import { useGFDStore } from './stores/gfdStore'
import ConnectionForm from './components/ConnectionForm.vue'
import CartDisplay from './components/CartDisplay.vue'
import CheckoutDisplay from './components/CheckoutDisplay.vue'

const gfdStore = useGFDStore()
const { isConnected, isCheckoutMode } = storeToRefs(gfdStore)

// Configure API base URL based on environment
// You can change this to match your backend URL
const API_URL = import.meta.env.VITE_API_URL || 'http://localhost:8081'
gfdStore.setApiBaseUrl(API_URL)

// Show appropriate view based on connection state
const currentView = computed(() => {
  if (!isConnected.value) {
    return 'connection'
  }
  return 'cart'
})
</script>

<template>
  <div class="app-container">
    <!-- Connection Form -->
    <ConnectionForm v-if="currentView === 'connection'" />

    <!-- Cart Display -->
    <CartDisplay v-if="currentView === 'cart'" />

    <!-- Checkout Overlay -->
    <CheckoutDisplay v-if="isCheckoutMode" />
  </div>
</template>

<style>
* {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
}

body {
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial,
    sans-serif;
  -webkit-font-smoothing: antialiased;
  -moz-osx-font-smoothing: grayscale;
}

.app-container {
  min-height: 100vh;
}

/* Scrollbar styling */
::-webkit-scrollbar {
  width: 10px;
  height: 10px;
}

::-webkit-scrollbar-track {
  background: #f1f1f1;
}

::-webkit-scrollbar-thumb {
  background: #888;
  border-radius: 5px;
}

::-webkit-scrollbar-thumb:hover {
  background: #555;
}
</style>
