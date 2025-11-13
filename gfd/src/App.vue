<script setup lang="ts">
import { computed, ref } from 'vue'
import { storeToRefs } from 'pinia'
import { useGFDStore } from './stores/gfdStore'
import ConnectionForm from './components/ConnectionForm.vue'
import CartDisplay from './components/CartDisplay.vue'
import CheckoutDisplay from './components/CheckoutDisplay.vue'

const gfdStore = useGFDStore()
const { isConnected, isCheckoutMode, showAdminDialog } = storeToRefs(gfdStore)

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

// Admin dialog state
const adminPasscode = ref('')
const passcodeError = ref('')

/**
 * Verify admin passcode for GFD disconnect
 */
const verifyPasscode = () => {
  const isValid = gfdStore.verifyAdminPasscode(adminPasscode.value)

  if (!isValid) {
    passcodeError.value = 'Invalid passcode'

    // Clear error after 3 seconds
    setTimeout(() => {
      passcodeError.value = ''
    }, 3000)
  } else {
    adminPasscode.value = ''
    passcodeError.value = ''
  }
}

/**
 * Close admin dialog and reset state
 */
const closeAdminDialogHandler = () => {
  adminPasscode.value = ''
  passcodeError.value = ''
  gfdStore.closeAdminDialog()
}
</script>

<template>
  <div class="app-container">
    <!-- Connection Form -->
    <ConnectionForm v-if="currentView === 'connection'" />

    <!-- Cart Display -->
    <CartDisplay v-if="currentView === 'cart'" />

    <!-- Checkout Overlay -->
    <CheckoutDisplay v-if="isCheckoutMode" />

    <!-- GFD Admin Disconnect Dialog (Global) -->
    <div v-if="showAdminDialog" class="admin-dialog-overlay" @click="closeAdminDialogHandler">
      <div class="admin-dialog" @click.stop>
        <h3 class="admin-dialog-title">Admin Access</h3>
        <p class="admin-dialog-description">
          Enter admin passcode to disconnect
        </p>

        <input
          v-model="adminPasscode"
          type="password"
          class="admin-passcode-input"
          placeholder="Enter passcode"
          @keyup.enter="verifyPasscode"
          autofocus
        />
        <p v-if="passcodeError" class="error-message">{{ passcodeError }}</p>

        <div class="admin-dialog-buttons">
          <button class="btn btn-secondary" @click="closeAdminDialogHandler">
            Cancel
          </button>
          <button class="btn btn-danger" @click="verifyPasscode">
            Disconnect
          </button>
        </div>
      </div>
    </div>
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

/* GFD Admin Dialog Styles */
.admin-dialog-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 10000;
  padding: 24px;
}

.admin-dialog {
  background: white;
  border-radius: 16px;
  padding: 24px;
  max-width: 400px;
  width: 100%;
  box-shadow: 0 20px 25px -5px rgba(0, 0, 0, 0.1);
  animation: slideIn 0.3s ease-out;
}

@keyframes slideIn {
  from {
    opacity: 0;
    transform: scale(0.9) translateY(-20px);
  }
  to {
    opacity: 1;
    transform: scale(1) translateY(0);
  }
}

.admin-dialog-title {
  font-size: 20px;
  font-weight: 700;
  color: #1f2937;
  margin: 0 0 12px 0;
}

.admin-dialog-description {
  font-size: 14px;
  color: #6b7280;
  margin: 0 0 20px 0;
}

.admin-passcode-input {
  width: 100%;
  padding: 12px 16px;
  font-size: 16px;
  border: 2px solid #e5e7eb;
  border-radius: 8px;
  margin-bottom: 8px;
  box-sizing: border-box;
  transition: all 0.2s ease;
}

.admin-passcode-input:focus {
  outline: none;
  border-color: #667eea;
  box-shadow: 0 0 0 3px rgba(102, 126, 234, 0.1);
}

.error-message {
  margin: 0 0 16px 0;
  font-size: 14px;
  color: #ef4444;
  font-weight: 500;
}

.admin-dialog-buttons {
  display: flex;
  gap: 12px;
}

.btn {
  flex: 1;
  padding: 12px 24px;
  font-size: 16px;
  font-weight: 600;
  border: none;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.2s;
}

.btn-secondary {
  background: #f3f4f6;
  color: #374151;
}

.btn-secondary:hover {
  background: #e5e7eb;
}

.btn-danger {
  background: #ef4444;
  color: white;
}

.btn-danger:hover {
  background: #dc2626;
}
</style>
