<template>
  <div>
    <section class="page-hero" data-hero="true">
      <div class="container hero-surface">
        <div class="hero-inner">
          <div class="badge">Activities</div>
          <h1 class="h1" style="margin-top:12px">活動資訊</h1>
          <p class="lead" style="margin-top:10px">一起參與、一起連結、一起成長。</p>
        </div>

        <div class="activities-hero-side">
          <article class="activities-hero-card">
            <span>目前活動</span>
            <strong>{{ activitiesWithFormattedData.length }}</strong>
            <p>公開站台目前可瀏覽的活動項目數量。</p>
          </article>
          <article class="activities-hero-card activities-hero-card--accent">
            <span>參與節奏</span>
            <strong>聚會、課程、連結</strong>
            <p>從一次活動開始，逐步找到適合你的參與節奏與群體連結。</p>
          </article>
        </div>
      </div>
    </section>

    <section class="section section--tight">
      <div class="container">
        <div v-if="isLoading" class="loading"><p>載入中...</p></div>

        <div v-else-if="activitiesWithFormattedData.length > 0" class="activities-grid">
          <article class="card card--hover card--activity" v-for="activity in activitiesWithFormattedData" :key="activity.id">
            <div v-if="activity.imageUrl" class="media" style="height:190px">
              <img :src="activity.imageUrl" :alt="activity.title" />
            </div>

            <div class="card__body">
              <h3 class="card__title h3">{{ activity.title }}</h3>

              <div class="card__meta" style="margin-top:6px">
                <!-- 顯示多個時間段（課程） -->
                <div v-if="activity.sessions && activity.sessions.length > 0" style="margin-bottom:8px">
                  <div v-for="(session, index) in activity.sessions" :key="index" style="margin-bottom:4px">
                    <span>🕒 {{ formatSessionTime(session) }}</span>
                    <span v-if="activity.location && index === 0" style="margin-left:12px">📍 {{ activity.location }}</span>
                  </div>
                </div>
                <!-- 顯示單一時間（一般活動） -->
                <div v-else>
                  <span v-if="activity.timeDisplay">🕒 {{ activity.timeDisplay }}</span>
                  <span v-if="activity.location" :style="activity.timeDisplay ? 'margin-left:12px' : ''">📍 {{ activity.location }}</span>
                </div>
              </div>

              <p class="muted card__desc">
                {{ activity.description }}
              </p>

              <div class="tags card__tags" v-if="activity.tags && activity.tags.length">
                <span class="tag" v-for="tag in activity.tags" :key="tag">{{ tag }}</span>
              </div>

              <div class="card__footer">
                <!-- 多個時間段顯示所有日期 -->
                <div v-if="activity.sessions && activity.sessions.length > 0">
                  <span class="badge" v-for="(session, index) in activity.sessions" :key="index" :style="index > 0 ? 'margin-left:8px' : ''">
                    📅 {{ formatSessionDate(session) }}
                  </span>
                </div>
                <!-- 單一日期顯示 -->
                <span v-else class="badge">📅 {{ activity.date }}</span>
              </div>
            </div>
          </article>
        </div>

        <div v-else class="loading"><p>目前沒有活動資訊</p></div>
      </div>
    </section>
  </div>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue'
import { apiRequest } from '@/utils/api'

const activities = ref([])
const isLoading = ref(false)

const formatDate = (dateString) => {
  if (!dateString) return ''
  const date = new Date(dateString)
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  const dayOfWeek = date.getDay()
  const weekdays = ['日', '一', '二', '三', '四', '五', '六']
  return `${year}/${month}/${day}(${weekdays[dayOfWeek]})`
}

const parseTags = (tagsJson) => {
  if (!tagsJson) return []
  try {
    return JSON.parse(tagsJson)
  } catch (e) {
    return []
  }
}

const parseSessions = (sessionsJson) => {
  if (!sessionsJson) return []
  try {
    return JSON.parse(sessionsJson)
  } catch (e) {
    return []
  }
}

const formatSessionTime = (session) => {
  if (!session) return ''
  if (session.startTime && session.endTime) {
    return `${session.startTime} ~ ${session.endTime}`
  } else if (session.startTime) {
    return session.startTime
  }
  return ''
}

const formatSessionDate = (session) => {
  if (!session || !session.date) return ''
  const date = new Date(session.date)
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  const dayOfWeek = date.getDay()
  const weekdays = ['日', '一', '二', '三', '四', '五', '六']
  return `${year}/${month}/${day}(${weekdays[dayOfWeek]})`
}

const formatTimeRange = (startTime, endTime) => {
  if (startTime && endTime) {
    return `${startTime} ~ ${endTime}`
  } else if (startTime) {
    return startTime
  } else if (endTime) {
    return endTime
  }
  return ''
}

const activitiesWithFormattedData = computed(() => {
  return activities.value.map((activity) => {
    const sessions = parseSessions(activity.activitySessions)
    const hasMultipleSessions = sessions && sessions.length > 0
    
    return {
      ...activity,
      date: formatDate(activity.activityDate),
      timeDisplay: formatTimeRange(activity.startTime, activity.endTime),
      location: activity.location || '',
      tags: parseTags(activity.tags),
      sessions: hasMultipleSessions ? sessions : null
    }
  })
})

const loadActivities = async () => {
  isLoading.value = true
  try {
    const data = await apiRequest('/church/public/activities', { method: 'GET' }, '載入活動資訊', false)
    activities.value = Array.isArray(data) ? data : []
  } catch (error) {
    console.error('載入活動資訊失敗:', error)
  } finally {
    isLoading.value = false
  }
}

onMounted(loadActivities)
</script>

<style scoped>
.hero-surface {
  display: grid;
  grid-template-columns: minmax(0, 1.25fr) minmax(280px, 0.8fr);
  gap: 1rem;
}

.activities-hero-side {
  display: grid;
  gap: 1rem;
}

.activities-hero-card {
  padding: 1.25rem;
  border-radius: 24px;
  border: 1px solid rgba(23, 33, 47, 0.08);
  background: rgba(255, 255, 255, 0.76);
  box-shadow: 0 20px 48px rgba(23, 33, 47, 0.08);
  backdrop-filter: blur(14px);
}

.activities-hero-card span {
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

.activities-hero-card strong {
  display: block;
  margin-top: 0.8rem;
  color: #17212f;
  font-size: 1.9rem;
  line-height: 1.05;
  letter-spacing: -0.05em;
}

.activities-hero-card p {
  margin: 0.55rem 0 0;
  color: rgba(23, 33, 47, 0.62);
  font-weight: 600;
  line-height: 1.7;
}

.activities-hero-card--accent {
  background: linear-gradient(140deg, rgba(28, 76, 130, 0.96), rgba(70, 126, 188, 0.9));
}

.activities-hero-card--accent span,
.activities-hero-card--accent strong,
.activities-hero-card--accent p {
  color: white;
}

.activities-hero-card--accent span {
  background: rgba(255, 255, 255, 0.14);
}

.activities-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 1rem;
}

.card--activity {
  overflow: hidden;
}

.card__desc {
  min-height: 72px;
  margin-top: 0.9rem;
  line-height: 1.75;
}

.card__tags {
  margin-top: 1rem;
}

.tag {
  display: inline-flex;
  align-items: center;
  padding: 0.42rem 0.7rem;
  border-radius: 999px;
  background: rgba(28, 76, 130, 0.08);
  color: #214f86;
  font-size: 0.78rem;
  font-weight: 800;
}

.card__footer {
  margin-top: 1rem;
}

@media (max-width: 980px) {
  .hero-surface,
  .activities-grid {
    grid-template-columns: 1fr;
  }
}
</style>
