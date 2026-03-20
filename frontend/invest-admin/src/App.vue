<template>
  <div id="app">
    <router-view />
    <ToastHost />
    <LoadingSpinner />
  </div>
</template>

<script setup>
import { useLoading } from '@/composables/useLoading'
import ToastHost from '@shared/components/ToastHost.vue'
import LoadingSpinner from '@/components/LoadingSpinner.vue'

useLoading()
</script>

<style scoped>
#app {
  min-height: 100vh;
}
</style>
