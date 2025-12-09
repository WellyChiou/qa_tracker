<template>
  <div class="home">
    <!-- 歡迎區塊 -->
    <section class="hero" v-if="churchInfo">
      <div class="hero-content">
        <h1>{{ churchInfo.home_welcome_title || '歡迎來到極光教會-PLC' }}</h1>
        <p class="hero-subtitle">{{ churchInfo.home_welcome_subtitle || '讓我們在基督的愛及聖經真理中成長茁壯' }}</p>
      </div>
    </section>

    <!-- 聚會時間資訊 -->
    <section class="section" v-if="churchInfo">
      <div class="container">
        <div class="services-grid">
          <!-- 禮拜六晚崇聚會 -->
          <div class="card" v-if="churchInfo.home_saturday_service_time || churchInfo.home_saturday_service_location">
            <h2>晚崇聚會</h2>
            <p v-if="churchInfo.home_saturday_service_time"><strong>時間：</strong>{{ churchInfo.home_saturday_service_time }}</p>
            <p v-if="churchInfo.home_saturday_service_location"><strong>地點：</strong>{{ churchInfo.home_saturday_service_location }}</p>
          </div>
          <!-- 主日崇拜 -->
          <div class="card">
            <h2>主日崇拜</h2>
            <p><strong>時間：</strong>{{ churchInfo.home_main_service_time || '每週日上午 10:00' }}</p>
            <p><strong>地點：</strong>{{ churchInfo.home_main_service_location || '榮耀堂' }}</p>
          </div>
        </div>
      </div>
    </section>

    <!-- 最新活動 -->
    <section class="section bg-light" v-if="upcomingActivities && upcomingActivities.length > 0">
      <div class="container">
        <h2 class="section-title">最新活動</h2>
        <div class="activities-grid">
          <div class="card activity-card" v-for="activity in upcomingActivities.slice(0, 3)" :key="activity.id">
            <div class="activity-header">
              <h3>{{ activity.title }}</h3>
            </div>
            <p class="activity-time" v-if="activity.activityTime">時間：{{ activity.activityTime }}</p>
            <p class="activity-location" v-if="activity.location">地點：{{ activity.location }}</p>
            <p class="activity-description">{{ activity.description }}</p>
            <span class="activity-date" v-if="activity.activityDate">{{ formatDate(activity.activityDate) }}</span>
          </div>
        </div>
      </div>
    </section>

    <!-- 載入中 -->
    <div v-if="isLoading" class="loading">
      <p>載入中...</p>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue'
import { apiRequest } from '@/utils/api'

const churchInfo = ref(null)
const activities = ref([])
const isLoading = ref(true)

const formatDate = (dateString) => {
  if (!dateString) return ''
  const date = new Date(dateString)
  return date.toLocaleDateString('zh-TW', { 
    year: 'numeric', 
    month: '2-digit', 
    day: '2-digit' 
  })
}

// 取得未來或當天的活動（最多顯示 3 個）
const upcomingActivities = computed(() => {
  if (!activities.value || activities.value.length === 0) return []
  const today = new Date()
  today.setHours(0, 0, 0, 0)
  
  return activities.value
    .filter(activity => {
      if (!activity.activityDate) return false
      const activityDate = new Date(activity.activityDate)
      activityDate.setHours(0, 0, 0, 0)
      return activityDate >= today
    })
    .sort((a, b) => {
      const dateA = new Date(a.activityDate)
      const dateB = new Date(b.activityDate)
      return dateA - dateB
    })
})

const loadChurchInfo = async () => {
  try {
    const response = await apiRequest('/church/public/church-info', {
      method: 'GET'
    }, '載入教會資訊', false)
    
    if (response.ok) {
      const data = await response.json()
      if (data.success && data.data) {
        churchInfo.value = data.data
      }
    }
  } catch (error) {
    console.error('載入教會資訊失敗:', error)
  }
}

const loadActivities = async () => {
  try {
    const response = await apiRequest('/church/public/activities', {
      method: 'GET'
    }, '載入活動資訊', false)
    
    if (response.ok) {
      const data = await response.json()
      if (data.success && data.data) {
        activities.value = data.data
      }
    }
  } catch (error) {
    console.error('載入活動資訊失敗:', error)
  }
}

const loadData = async () => {
  isLoading.value = true
  try {
    await Promise.all([loadChurchInfo(), loadActivities()])
  } finally {
    isLoading.value = false
  }
}

onMounted(() => {
  loadData()
})
</script>

<style scoped>
.hero {
  position: relative;
  color: white;
  padding: 6rem 2rem;
  text-align: center;
  overflow: hidden;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}

.hero-content {
  position: relative;
  z-index: 1;
  background: rgba(0, 0, 0, 0.3);
  padding: 2rem;
  border-radius: 10px;
  backdrop-filter: blur(5px);
  max-width: 800px;
  margin: 0 auto;
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

.section {
  padding: 4rem 0;
}

.bg-light {
  background: #f8f9fa;
}

.container {
  max-width: 1200px;
  margin: 0 auto;
  padding: 0 2rem;
}

.section-title {
  text-align: center;
  font-size: 2.5rem;
  margin-bottom: 3rem;
  color: #333;
}

.card {
  background: white;
  padding: 2rem;
  border-radius: 10px;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.1);
  margin-bottom: 2rem;
}

.card h2 {
  color: #667eea;
  margin-bottom: 1rem;
  font-size: 1.8rem;
}

.card h3 {
  color: #667eea;
  margin-bottom: 1rem;
  font-size: 1.5rem;
}

.card p {
  margin-bottom: 0.5rem;
  line-height: 1.6;
}

.services-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(300px, 1fr));
  gap: 2rem;
}

.activities-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(300px, 1fr));
  gap: 2rem;
}

.activity-card {
  position: relative;
  transition: transform 0.3s, box-shadow 0.3s;
  padding-bottom: 3.5rem;
}

.activity-card:hover {
  transform: translateY(-5px);
  box-shadow: 0 8px 16px rgba(0, 0, 0, 0.15);
}

.activity-header {
  margin-bottom: 1rem;
}

.activity-date {
  position: absolute;
  bottom: 1rem;
  right: 1rem;
  background: #667eea;
  color: white;
  padding: 0.25rem 0.75rem;
  border-radius: 5px;
  font-size: 0.9rem;
  white-space: nowrap;
}

.activity-time,
.activity-location {
  color: #666;
  margin-bottom: 0.5rem;
}

.activity-description {
  margin: 1rem 0;
  line-height: 1.6;
}

.loading {
  text-align: center;
  padding: 4rem 2rem;
  color: #666;
}
</style>
