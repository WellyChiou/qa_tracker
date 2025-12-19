<template>
  <div id="app">
    <router-view />
    <ToastHost />
    <LoadingSpinner />
  </div>
</template>

<script setup>
import { useLoading } from '@/composables/useLoading'
import ToastHost from '@/components/ToastHost.vue'
import LoadingSpinner from '@/components/LoadingSpinner.vue'

// 初始化 loading 系統（註冊回調到 API 服務）
useLoading()
</script>

<style scoped>
#app {
  min-height: 100vh;
}
</style>

