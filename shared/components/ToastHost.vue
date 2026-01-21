<template>
  <div class="toast-host" aria-live="polite" aria-atomic="true">
    <TransitionGroup name="toast" tag="div" class="toast-group">
      <div v-for="t in state.toasts" :key="t.id" class="toast" :class="'toast--' + t.type">
        <div class="toast__row">
          <div class="toast__icon">
            <span v-if="t.type === 'success'">✓</span>
            <span v-else-if="t.type === 'error'">!</span>
            <span v-else-if="t.type === 'warning'">!</span>
            <span v-else>i</span>
          </div>

          <div style="min-width:0">
            <div class="toast__title">{{ t.title }}</div>
            <div class="toast__msg">{{ t.message }}</div>
          </div>

          <button class="toast__close" @click="toast.remove(t.id)" aria-label="close">✕</button>
        </div>
      </div>
    </TransitionGroup>
  </div>
</template>

<script setup>
import { useToastState, toast } from '../composables/useToast.js'
import { onMounted } from 'vue'

const state = useToastState()

onMounted(() => {
  // 確保 ToastHost 元素可見（處理可能的樣式衝突或渲染時機問題）
  const hostEl = document.querySelector('.toast-host')
  if (hostEl && hostEl.offsetParent === null) {
    // 如果元素不可見，強制設置樣式確保顯示
    hostEl.style.position = 'fixed'
    hostEl.style.left = '18px'
    hostEl.style.bottom = '18px'
    hostEl.style.zIndex = '99999'
    hostEl.style.display = 'flex'
    hostEl.style.visibility = 'visible'
    hostEl.style.opacity = '1'
  }
})
</script>
