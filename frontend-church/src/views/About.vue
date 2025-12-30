<template>
  <div>
    <!-- Hero -->
    <section class="page-hero" data-hero="true">
      <div class="container hero-surface">
        <div class="hero-inner">
          <div class="badge">About</div>
          <h1 class="h1" style="margin-top:12px">關於我們</h1>
          <p class="lead" style="margin-top:10px">認識我們的故事、異象與核心價值。</p>
        </div>
      </div>
    </section>

    <div class="page-stack" v-reveal="{ mode: 'page', stagger: 70 }">
      <section class="section section--tight">
        <div class="container">
          <!-- Loading -->
          <div v-if="isLoading" class="loading">
            <p>載入中...</p>
          </div>

          <!-- Empty -->
          <div v-else-if="aboutInfoList.length === 0" class="loading">
            <p>目前沒有內容</p>
          </div>

          <!-- Content -->
          <div v-else class="grid">
            <article class="card" v-for="info in aboutInfoList" :key="info.key">
              <h2 class="h2" style="margin:0 0 10px">{{ info.title }}</h2>

              <p v-if="info.key !== 'values'" class="content-text muted">
                {{ info.content }}
              </p>

              <ul v-else class="values-list">
                <li v-for="(line, index) in parseValuesContent(info.content)" :key="index">
                  <span class="badge" style="margin-right:8px">{{ line.label }}</span>
                  <span class="muted">{{ line.value }}</span>
                </li>
              </ul>
            </article>
          </div>
        </div>
      </section>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { apiRequest } from '@/utils/api'

const aboutInfoList = ref([])

const parseValuesContent = (content) => {
  if (!content) return []
  return content
    .split('\n')
    .filter((line) => line.trim())
    .map((line) => {
      const parts = line.split('：')
      return {
        label: parts[0] || '',
        value: parts.slice(1).join('：') || ''
      }
    })
}

const loadAboutInfo = async () => {
  try {
    const response = await apiRequest('/church/public/about-info', { method: 'GET' }, '載入關於我們資訊', false)
    if (response?.ok) {
      const data = await response.json()
      const sections = data?.success ? data?.data?.sections : null

      if (Array.isArray(sections)) {
        // ✅ 確保順序穩定（order 小到大）
        aboutInfoList.value = sections
          .slice()
          .sort((a, b) => (a.order ?? 0) - (b.order ?? 0))
      } else {
        aboutInfoList.value = []
      }
    } else {
      aboutInfoList.value = []
    }
  } catch (error) {
    console.error('載入關於我們資訊失敗:', error)
  }
}

onMounted(loadAboutInfo)
</script>

<style scoped>
.content-text {
  white-space: pre-line;
  margin: 0;
}

.values-list {
  list-style: none;
  padding: 0;
  margin: 0;
  display: grid;
  gap: 10px;
}

.values-list li {
  padding: 12px;
  border-radius: 14px;
  border: 1px solid var(--border);
  background: var(--surface-2);
}
</style>
