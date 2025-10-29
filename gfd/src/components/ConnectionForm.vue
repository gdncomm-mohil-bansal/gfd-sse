<template>
  <div class="connection-container">
    <div class="connection-card">
      <div class="logo-section">
        <div class="logo-circle">
          <svg
            xmlns="http://www.w3.org/2000/svg"
            fill="none"
            viewBox="0 0 24 24"
            stroke="currentColor"
            class="logo-icon"
          >
            <path
              stroke-linecap="round"
              stroke-linejoin="round"
              stroke-width="2"
              d="M9.75 17L9 20l-1 1h8l-1-1-.75-3M3 13h18M5 17h14a2 2 0 002-2V5a2 2 0 00-2-2H5a2 2 0 00-2 2v10a2 2 0 002 2z"
            />
          </svg>
        </div>
        <h1>Guest Facing Display</h1>
        <p class="subtitle">Connect to your shopping session</p>
      </div>

      <form @submit.prevent="handleConnect" class="connection-form">
        <div class="form-group">
          <label for="userId">Session ID</label>
          <input
            id="userId"
            v-model="formData.userId"
            type="text"
            placeholder="Enter your session ID"
            required
            :disabled="isConnecting"
          />
        </div>

        <div class="form-group">
          <label for="otp">6-Digit Code</label>
          <input
            id="otp"
            v-model="formData.otp"
            type="text"
            inputmode="numeric"
            pattern="[0-9]{6}"
            maxlength="6"
            placeholder="000000"
            required
            :disabled="isConnecting"
            class="otp-input"
          />
          <small>Enter the 6-digit code shown on the POS terminal</small>
        </div>

        <div v-if="error" class="error-message">
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
              d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z"
            />
          </svg>
          {{ error }}
        </div>

        <button type="submit" class="connect-button" :disabled="isConnecting">
          <span v-if="!isConnecting">Connect</span>
          <span v-else class="connecting">
            <span class="spinner"></span>
            Connecting...
          </span>
        </button>
      </form>

      <div class="info-section">
        <p>
          <strong>How to connect:</strong>
        </p>
        <ol>
          <li>Ask the cashier to generate a display code</li>
          <li>Enter your Session ID and the 6-digit code shown</li>
          <li>Click Connect to start viewing your cart in real-time</li>
        </ol>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useGFDStore } from '@/stores/gfdStore'
import { storeToRefs } from 'pinia'

const gfdStore = useGFDStore()
const { connectionError } = storeToRefs(gfdStore)

const formData = ref({
  userId: '',
  otp: ''
})

const isConnecting = ref(false)
const error = ref<string | null>(null)

const handleConnect = async () => {
  error.value = null
  gfdStore.clearError()

  if (!formData.value.userId.trim()) {
    error.value = 'Please enter your Session ID'
    return
  }

  if (!formData.value.otp.trim() || formData.value.otp.length !== 6) {
    error.value = 'Please enter a valid 6-digit code'
    return
  }

  isConnecting.value = true

  try {
    gfdStore.connect(formData.value.userId.trim(), formData.value.otp.trim())

    // Wait a bit to see if connection succeeds
    await new Promise((resolve) => setTimeout(resolve, 2000))

    if (connectionError.value) {
      error.value = connectionError.value
      isConnecting.value = false
    }
  } catch (err: any) {
    error.value = err.message || 'Failed to connect. Please try again.'
    isConnecting.value = false
  }
}
</script>

<style scoped>
.connection-container {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  padding: 20px;
}

.connection-card {
  background: white;
  border-radius: 20px;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.3);
  padding: 40px;
  max-width: 480px;
  width: 100%;
  animation: slideUp 0.5s ease-out;
}

@keyframes slideUp {
  from {
    opacity: 0;
    transform: translateY(30px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.logo-section {
  text-align: center;
  margin-bottom: 32px;
}

.logo-circle {
  width: 80px;
  height: 80px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  margin: 0 auto 16px;
}

.logo-icon {
  width: 40px;
  height: 40px;
  color: white;
}

h1 {
  font-size: 28px;
  font-weight: 700;
  color: #1a202c;
  margin: 0 0 8px;
}

.subtitle {
  color: #718096;
  font-size: 16px;
  margin: 0;
}

.connection-form {
  margin-bottom: 24px;
}

.form-group {
  margin-bottom: 20px;
}

label {
  display: block;
  font-weight: 600;
  color: #2d3748;
  margin-bottom: 8px;
  font-size: 14px;
}

input {
  width: 100%;
  padding: 12px 16px;
  border: 2px solid #e2e8f0;
  border-radius: 10px;
  font-size: 16px;
  transition: all 0.3s;
  box-sizing: border-box;
}

input:focus {
  outline: none;
  border-color: #667eea;
  box-shadow: 0 0 0 3px rgba(102, 126, 234, 0.1);
}

input:disabled {
  background-color: #f7fafc;
  cursor: not-allowed;
}

.otp-input {
  font-size: 24px;
  font-weight: 600;
  letter-spacing: 8px;
  text-align: center;
}

small {
  display: block;
  margin-top: 6px;
  color: #718096;
  font-size: 13px;
}

.connect-button {
  width: 100%;
  padding: 14px 24px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  border: none;
  border-radius: 10px;
  font-size: 16px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.3s;
}

.connect-button:hover:not(:disabled) {
  transform: translateY(-2px);
  box-shadow: 0 10px 25px rgba(102, 126, 234, 0.4);
}

.connect-button:active:not(:disabled) {
  transform: translateY(0);
}

.connect-button:disabled {
  opacity: 0.7;
  cursor: not-allowed;
}

.connecting {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
}

.spinner {
  width: 16px;
  height: 16px;
  border: 2px solid rgba(255, 255, 255, 0.3);
  border-top-color: white;
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
}

@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}

.error-message {
  background: #fed7d7;
  color: #c53030;
  padding: 12px 16px;
  border-radius: 10px;
  margin-bottom: 16px;
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 14px;
}

.error-message svg {
  width: 20px;
  height: 20px;
  flex-shrink: 0;
}

.info-section {
  background: #f7fafc;
  border-radius: 10px;
  padding: 20px;
  font-size: 14px;
  color: #4a5568;
}

.info-section p {
  margin: 0 0 12px;
}

.info-section ol {
  margin: 0;
  padding-left: 20px;
}

.info-section li {
  margin-bottom: 8px;
  line-height: 1.5;
}

.info-section li:last-child {
  margin-bottom: 0;
}
</style>

