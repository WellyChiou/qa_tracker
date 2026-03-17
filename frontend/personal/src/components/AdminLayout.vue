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
  background: var(--bg-primary);
}

.admin-main {
  padding: var(--spacing-xl) 0;
}

.admin-content {
  max-width: 1400px;
  margin: 0 auto;
  padding: 0 var(--spacing-xl) var(--spacing-2xl);
}

@media (max-width: 768px) {
  .admin-content {
    padding: 0 var(--spacing-lg) var(--spacing-2xl);
  }
}
</style>
