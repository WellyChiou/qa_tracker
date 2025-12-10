<template>
  <div class="activities">
    <section class="section">
      <div class="container">
        <h1 class="section-title">活動資訊</h1>
        
        <div v-if="isLoading" class="loading">
          <p>載入中...</p>
        </div>
        <div v-else-if="activitiesWithFormattedData.length > 0" class="activities-grid">
          <div class="card activity-card" v-for="activity in activitiesWithFormattedData" :key="activity.id">
            <div v-if="activity.imageUrl" class="activity-image">
              <img :src="activity.imageUrl" :alt="activity.title" />
            </div>
            <div class="activity-header">
              <h3>{{ activity.title }}</h3>
            </div>
            <p class="activity-time">時間：{{ activity.time }}</p>
            <p class="activity-location">地點：{{ activity.location }}</p>
            <p class="activity-description">{{ activity.description }}</p>
            <div class="activity-tags">
              <span class="tag" v-for="tag in activity.tags" :key="tag">{{ tag }}</span>
            </div>
            <span class="activity-date">{{ activity.date }}</span>
          </div>
        </div>
        <div v-else class="no-activities">
          <p>目前沒有活動資訊</p>
        </div>
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

const activitiesWithFormattedData = computed(() => {
  return activities.value.map(activity => ({
    ...activity,
    date: formatDate(activity.activityDate),
    time: activity.activityTime || '',
    location: activity.location || '',
    tags: parseTags(activity.tags)
  }))
})

const loadActivities = async () => {
  isLoading.value = true
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
  } finally {
    isLoading.value = false
  }
}

onMounted(() => {
  loadActivities()
})
</script>

<style scoped>
.activities-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(320px, 1fr));
  gap: 2rem;
}

.activity-card {
  position: relative;
  transition: transform 0.3s, box-shadow 0.3s;
  padding-bottom: 3.5rem; /* 為日期預留空間 */
  overflow: hidden;
}

.activity-card:hover {
  transform: translateY(-5px);
  box-shadow: 0 8px 16px rgba(0, 0, 0, 0.15);
}

.activity-image {
  width: calc(100% + 4rem);
  height: 200px;
  overflow: hidden;
  margin: -2rem -2rem 1rem -2rem;
  border-radius: 10px 10px 0 0;
}

.activity-image img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 0.3s;
  display: block;
}

.activity-card:hover .activity-image img {
  transform: scale(1.05);
}

.activity-header {
  margin-bottom: 1rem;
}

.activity-header h3 {
  color: #667eea;
  font-size: 1.5rem;
  margin: 0;
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
  z-index: 1;
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

.activity-tags {
  display: flex;
  gap: 0.5rem;
  flex-wrap: wrap;
  margin-bottom: 0.5rem; /* 與日期保持距離 */
  max-width: calc(100% - 150px); /* 為日期預留空間，避免重疊 */
}

.tag {
  background: #f0f0f0;
  color: #667eea;
  padding: 0.25rem 0.75rem;
  border-radius: 15px;
  font-size: 0.85rem;
}

.loading,
.no-activities {
  text-align: center;
  padding: 2rem;
  color: #666;
}
</style>

