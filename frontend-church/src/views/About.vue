<template>
  <div class="about">
    <section class="section">
      <div class="container container--narrow">
        <h1 class="section-title">關於我們</h1>
        
        <div v-if="aboutInfoList && aboutInfoList.length > 0">
          <div class="stack">
            <div class="card prose" v-for="info in aboutInfoList" :key="info.id">
              <h2>{{ info.title }}</h2>
              <p v-if="info.sectionKey !== 'values'" class="content-text">{{ info.content }}</p>
              <ul v-else class="values-list">
                <li v-for="(line, index) in parseValuesContent(info.content)" :key="index">
                  <strong>{{ line.label }}：</strong>{{ line.value }}
                </li>
              </ul>
            </div>
          </div>
        </div>
        <div v-else class="loading">
          <p>載入中...</p>
        </div>
      </div>
    </section>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { apiRequest } from '@/utils/api'

const aboutInfoList = ref([])

const parseValuesContent = (content) => {
  if (!content) return []
  // 將內容按行分割，每行格式為 "標籤：值"
  return content.split('\n')
    .filter(line => line.trim())
    .map(line => {
      const parts = line.split('：')
      return {
        label: parts[0] || '',
        value: parts.slice(1).join('：') || ''
      }
    })
}

const loadAboutInfo = async () => {
  try {
    const response = await apiRequest('/church/public/about-info', {
      method: 'GET'
    }, '載入關於我們資訊', false)
    
    if (response.ok) {
      const data = await response.json()
      if (data.success && data.data) {
        aboutInfoList.value = data.data
      }
    }
  } catch (error) {
    console.error('載入關於我們資訊失敗:', error)
  }
}

onMounted(() => {
  loadAboutInfo()
})
</script>

<style scoped>
.card{
  padding: 2rem;
}

.values-list {
  list-style: none;
  padding: 0;
}

.values-list li {
  padding: 0.95rem 0;
  border-bottom: 1px solid var(--border);
}

.values-list li:last-child {
  border-bottom: none;
}

.card h2 {
  color: var(--text);
  margin-bottom: 1rem;
  font-size: 1.85rem;
  letter-spacing: -0.3px;
}

.content-text {
  color: var(--muted);
  margin-bottom: 0;
  white-space: pre-line;
}

.card p{ margin-bottom: 0; }

.loading {
  text-align: center;
  padding: 2rem;
  color: var(--muted);
}

@media (max-width: 640px){
  .card{ padding: 1.35rem; }
}
</style>

