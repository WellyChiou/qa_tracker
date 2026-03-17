<template>
  <div>
    <section class="page-hero" data-hero="true">
      <div class="container hero-surface">
        <div class="hero-inner">
          <div class="badge">Prayers</div>
          <h1 class="h1" style="margin-top:12px">代禱事項</h1>
          <p class="lead" style="margin-top:10px">讓我們一起為這些需要代禱。</p>
        </div>
      </div>
    </section>

    <section class="section section--tight">
      <div class="container">
        <div v-if="isLoading" class="loading"><p>載入中...</p></div>

        <div v-else-if="prayerRequestsWithFormattedData.length > 0" class="grid grid-3">
          <article class="card card--hover card--activity" v-for="prayer in prayerRequestsWithFormattedData" :key="prayer.id">
            <div class="card__body">
              <div class="tags" style="margin-bottom:10px">
                <span v-if="prayer.isUrgent" class="badge badge--accent">🔥 緊急</span>
                <span v-if="prayer.category" class="badge">{{ prayer.category }}</span>
              </div>

              <h3 class="card__title h3">{{ prayer.title }}</h3>

              <p class="muted card__desc">
                {{ prayer.content }}
              </p>

              <div class="card__footer">
                <span class="badge">📅 {{ prayer.date }}</span>
              </div>
            </div>
          </article>
        </div>

        <div v-else class="loading"><p>目前沒有代禱事項</p></div>
      </div>
    </section>
  </div>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue'
import { apiRequest } from '@/utils/api'

const prayerRequests = ref([])
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

const prayerRequestsWithFormattedData = computed(() => {
  // 先排序：緊急的在前，然後按建立時間降序
  const sorted = [...prayerRequests.value].sort((a, b) => {
    if (a.isUrgent && !b.isUrgent) return -1
    if (!a.isUrgent && b.isUrgent) return 1
    return new Date(b.createdAt) - new Date(a.createdAt)
  })
  
  return sorted.map(prayer => ({
    ...prayer,
    date: formatDate(prayer.createdAt)
  }))
})

const loadPrayerRequests = async () => {
  isLoading.value = true
  try {
    const data = await apiRequest('/church/public/prayer-requests', { method: 'GET' }, '載入代禱事項', false)
    prayerRequests.value = Array.isArray(data) ? data : []
  } catch (error) {
    console.error('載入代禱事項失敗:', error)
  } finally {
    isLoading.value = false
  }
}

onMounted(loadPrayerRequests)
</script>
