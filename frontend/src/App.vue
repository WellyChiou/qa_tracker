<template>
  <div id="app">
    <router-view />
    <LoadingSpinner />
  </div>
</template>

<script setup>
import { onMounted } from 'vue'
import { useAuth } from '@/composables/useAuth'
import { useLoading } from '@/composables/useLoading'
import LoadingSpinner from '@/components/LoadingSpinner.vue'

// 初始化 loading 系統（註冊回調到 API 服務）
useLoading()

const { checkAuth } = useAuth()

onMounted(async () => {
  await checkAuth()
})
</script>

<style>
* {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
}

body {
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif;
  -webkit-font-smoothing: antialiased;
  -moz-osx-font-smoothing: grayscale;
}

#app {
  min-height: 100vh;
}
</style>

