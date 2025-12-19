<template>
  <div class="toast-host" aria-live="polite" aria-atomic="true">
    <TransitionGroup name="toast" tag="div">
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
import { useToastState, toast } from '@/composables/useToast'
const state = useToastState()
</script>
