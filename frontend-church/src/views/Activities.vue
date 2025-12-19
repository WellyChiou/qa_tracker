<template>
  <div>
    <section class="page-hero" data-hero="true">
      <div class="container hero-surface">
        <div class="hero-inner">
          <div class="badge">Activities</div>
          <h1 class="h1" style="margin-top:12px">活動資訊</h1>
          <p class="lead" style="margin-top:10px">一起參與、一起連結、一起成長。</p>
        </div>
      </div>
    </section>

    <section class="section section--tight">
      <div class="container">
        <div v-if="isLoading" class="loading"><p>載入中...</p></div>

        <div v-else-if="activitiesWithFormattedData.length > 0" class="grid grid-3">
          <article class="card card--hover card--activity" v-for="activity in activitiesWithFormattedData" :key="activity.id">
            <div v-if="activity.imageUrl" class="media" style="height:190px">
              <img :src="activity.imageUrl" :alt="activity.title" />
            </div>

            <div class="card__body">
              <h3 class="card__title h3">{{ activity.title }}</h3>

              <div class="card__meta" style="margin-top:6px">
                <span v-if="activity.time">🕒 {{ activity.time }}</span>
                <span v-if="activity.location">📍 {{ activity.location }}</span>
              </div>

              <p class="muted card__desc">
                {{ activity.description }}
              </p>

              <div class="tags card__tags" v-if="activity.tags && activity.tags.length">
                <span class="tag" v-for="tag in activity.tags" :key="tag">{{ tag }}</span>
              </div>

              <div class="card__footer">
                <span class="badge">📅 {{ activity.date }}</span>
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

const activitiesWithFormattedData = computed(() => {
  return activities.value.map((activity) => ({
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
    const response = await apiRequest('/church/public/activities', { method: 'GET' }, '載入活動資訊', false)

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

onMounted(loadActivities)
</script>
