<template>
  <div class="admin-layout">
    <TopNavbar />
    <main class="admin-main">
      <div class="admin-content">
        <slot />
      </div>
    </main>
  </div>
</template>

<script setup>
import TopNavbar from '@/components/TopNavbar.vue'
</script>

<style scoped>
.admin-layout {
  min-height: 100vh;
  background: #f5f5f5;
}

.admin-main {
  min-height: calc(100vh - 80px);
}

.admin-content {
  max-width: 1400px;
  margin: 0 auto;
  padding: 2rem;
}
</style>

