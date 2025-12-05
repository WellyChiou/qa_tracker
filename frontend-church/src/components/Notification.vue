<template>
  <TransitionGroup name="notification" tag="div" class="notification-container">
    <div
      v-for="notification in notifications"
      :key="notification.id"
      :class="['notification', `notification-${notification.type}`]"
    >
      <div class="notification-content">
        <span class="notification-icon">
          <span v-if="notification.type === 'success'">✓</span>
          <span v-else-if="notification.type === 'error'">✕</span>
          <span v-else-if="notification.type === 'warning'">⚠</span>
          <span v-else>ℹ</span>
        </span>
        <span class="notification-message">{{ notification.message }}</span>
      </div>
      <button class="notification-close" @click="removeNotification(notification.id)">×</button>
    </div>
  </TransitionGroup>
</template>

<script setup>
import { ref } from 'vue'

const notifications = ref([])
let notificationId = 0

const showNotification = (message, type = 'info', duration = 3000) => {
  const id = ++notificationId
  const notification = {
    id,
    message,
    type
  }
  
  notifications.value.push(notification)
  
  if (duration > 0) {
    setTimeout(() => {
      removeNotification(id)
    }, duration)
  }
  
  return id
}

const removeNotification = (id) => {
  const index = notifications.value.findIndex(n => n.id === id)
  if (index > -1) {
    notifications.value.splice(index, 1)
  }
}

// 導出方法供外部使用
defineExpose({
  showNotification,
  removeNotification
})
</script>

<style scoped>
.notification-container {
  position: fixed;
  bottom: 20px;
  left: 20px;
  z-index: 10000;
  display: flex;
  flex-direction: column;
  gap: 10px;
  max-width: 400px;
}

.notification {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 16px;
  border-radius: 8px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
  background: white;
  min-width: 300px;
  animation: slideIn 0.3s ease-out;
}

.notification-content {
  display: flex;
  align-items: center;
  gap: 12px;
  flex: 1;
}

.notification-icon {
  font-size: 18px;
  font-weight: bold;
  width: 24px;
  height: 24px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  flex-shrink: 0;
}

.notification-message {
  flex: 1;
  font-size: 14px;
  line-height: 1.5;
  white-space: pre-line;
}

.notification-close {
  background: none;
  border: none;
  font-size: 24px;
  color: #999;
  cursor: pointer;
  padding: 0;
  width: 24px;
  height: 24px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  transition: color 0.2s;
}

.notification-close:hover {
  color: #333;
}

.notification-success {
  border-left: 4px solid #28a745;
}

.notification-success .notification-icon {
  background: #28a745;
  color: white;
}

.notification-error {
  border-left: 4px solid #dc3545;
}

.notification-error .notification-icon {
  background: #dc3545;
  color: white;
}

.notification-warning {
  border-left: 4px solid #ffc107;
}

.notification-warning .notification-icon {
  background: #ffc107;
  color: white;
}

.notification-info {
  border-left: 4px solid #17a2b8;
}

.notification-info .notification-icon {
  background: #17a2b8;
  color: white;
}

@keyframes slideIn {
  from {
    transform: translateX(-100%);
    opacity: 0;
  }
  to {
    transform: translateX(0);
    opacity: 1;
  }
}

.notification-enter-active,
.notification-leave-active {
  transition: all 0.3s ease;
}

.notification-enter-from {
  transform: translateX(-100%);
  opacity: 0;
}

.notification-leave-to {
  transform: translateX(-100%);
  opacity: 0;
}

.notification-move {
  transition: transform 0.3s ease;
}
</style>

