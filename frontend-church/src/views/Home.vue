<template>
  <div class="home" v-if="pageContent">
    <div v-html="pageContent.content"></div>
  </div>
  <div v-else class="loading">
    <p>載入中...</p>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { apiRequest } from '@/utils/api'

const pageContent = ref(null)

const loadPageContent = async () => {
  try {
    const response = await apiRequest('/church/pages/home', {
      method: 'GET'
    })
    
    if (response.ok) {
      const data = await response.json()
      if (data.success && data.data) {
        pageContent.value = data.data
      }
    }
  } catch (error) {
    console.error('載入首頁內容失敗:', error)
  }
}

onMounted(() => {
  loadPageContent()
})
</script>

<style scoped>
.hero {
  position: relative;
  color: white;
  padding: 6rem 2rem;
  text-align: center;
  overflow: hidden;
}

.hero-background {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  z-index: 0;
  opacity: 0.3;
}

.hero-bg-image {
  width: 100%;
  height: 100%;
  object-fit: cover;
  object-position: center;
}

.hero-content {
  position: relative;
  z-index: 1;
  background: rgba(0, 0, 0, 0.3);
  padding: 2rem;
  border-radius: 10px;
  backdrop-filter: blur(5px);
}

.hero-content h1 {
  font-size: 3rem;
  margin-bottom: 1rem;
}

.hero-subtitle {
  font-size: 1.5rem;
  margin-bottom: 2rem;
  opacity: 0.9;
}

.news-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(300px, 1fr));
  gap: 2rem;
}

.news-date {
  color: #666;
  font-size: 0.9rem;
  margin-bottom: 1rem;
}

.schedule-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(280px, 1fr));
  gap: 2rem;
}

.bg-light {
  background: #f8f9fa;
  padding: 4rem 0;
}

.card h3 {
  color: #667eea;
  margin-bottom: 1rem;
}

.card p {
  margin-bottom: 0.5rem;
}
</style>

