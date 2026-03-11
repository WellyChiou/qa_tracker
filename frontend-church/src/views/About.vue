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

        <div class="about-hero-side">
          <article class="about-hero-card">
            <span>內容段落</span>
            <strong>{{ aboutInfoList.length }}</strong>
            <p>目前後台已整理的關於我們內容區塊。</p>
          </article>
          <article class="about-hero-card about-hero-card--accent">
            <span>教會方向</span>
            <strong>故事、異象、價值</strong>
            <p>從我們的信念、文化與實際行動，理解這個群體的核心節奏。</p>
          </article>
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
          <div v-else class="grid about-grid">
            <article class="card about-card" v-for="info in aboutInfoList" :key="info.key">
              <span class="about-card__eyebrow">{{ info.title }}</span>
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
    const data = await apiRequest('/church/public/about-info', { method: 'GET' }, '載入關於我們資訊', false)
    const sections = data?.sections

    if (Array.isArray(sections)) {
      aboutInfoList.value = sections
        .slice()
        .sort((a, b) => (a.order ?? 0) - (b.order ?? 0))
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
.hero-surface {
  display: grid;
  grid-template-columns: minmax(0, 1.25fr) minmax(280px, 0.8fr);
  gap: 1rem;
}

.about-hero-side {
  display: grid;
  gap: 1rem;
}

.about-hero-card,
.about-card {
  border-radius: 24px;
  border: 1px solid rgba(23, 33, 47, 0.08);
  background: rgba(255, 255, 255, 0.76);
  box-shadow: 0 20px 48px rgba(23, 33, 47, 0.08);
  backdrop-filter: blur(14px);
}

.about-hero-card {
  padding: 1.25rem;
}

.about-hero-card span,
.about-card__eyebrow {
  display: inline-flex;
  width: fit-content;
  padding: 0.42rem 0.7rem;
  border-radius: 999px;
  background: rgba(190, 140, 66, 0.12);
  color: #8a5a1f;
  font-size: 0.74rem;
  font-weight: 900;
  letter-spacing: 0.14em;
  text-transform: uppercase;
}

.about-hero-card strong {
  display: block;
  margin-top: 0.8rem;
  color: #17212f;
  font-size: 1.9rem;
  line-height: 1.05;
  letter-spacing: -0.05em;
}

.about-hero-card p {
  margin: 0.55rem 0 0;
  color: rgba(23, 33, 47, 0.62);
  font-weight: 600;
  line-height: 1.7;
}

.about-hero-card--accent {
  background: linear-gradient(140deg, rgba(28, 76, 130, 0.96), rgba(70, 126, 188, 0.9));
}

.about-hero-card--accent span,
.about-hero-card--accent strong,
.about-hero-card--accent p {
  color: white;
}

.about-hero-card--accent span {
  background: rgba(255, 255, 255, 0.14);
}

.about-grid {
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.about-card {
  padding: 1.35rem;
}

.content-text {
  white-space: pre-line;
  margin: 0;
  line-height: 1.8;
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

@media (max-width: 980px) {
  .hero-surface,
  .about-grid {
    grid-template-columns: 1fr;
  }
}
</style>
