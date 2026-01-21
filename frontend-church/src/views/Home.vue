<template>
  <div>
    <!-- Hero / 首屏：只換版面，不動資料來源 -->
    <section class="page-hero" data-hero="true" v-if="churchInfo">
      <div class="container hero-surface">
        <div class="hero-inner">
          <div class="badge badge--accent">Welcome</div>
          <h1 class="h1" style="margin-top:12px">
            {{ churchInfo.home_welcome_title || '歡迎來到極光教會-PLC' }}
          </h1>
          <p class="lead" style="margin-top:10px">
            {{ churchInfo.home_welcome_subtitle || '讓我們在基督的愛及聖經真理中成長茁壯' }}
          </p>

          <div class="hero-actions">
            <router-link to="/activities" class="btn btn-primary">查看最新活動</router-link>
            <router-link to="/about" class="btn btn-ghost">了解教會</router-link>
            <router-link to="/contact" class="btn btn-ghost">聯絡我們</router-link>
          </div>
        </div>
      </div>
    </section>

    <!-- 聚會時間 -->
    <section class="section" v-reveal v-if="churchInfo">
      <div class="container">
        <div class="center" style="margin-bottom:18px">
          <h2 class="h2">聚會時間</h2>
          <p class="muted" style="margin-top:8px">歡迎你與我們一起敬拜，一同經歷神的同在。</p>
        </div>

        <div class="grid grid-2">
          <div class="card card--hover" v-if="churchInfo.home_saturday_service_time || churchInfo.home_saturday_service_location">
            <h3 class="card__title h3">晚崇聚會</h3>
            <div class="card__meta">
              <span v-if="churchInfo.home_saturday_service_time">🕒 {{ churchInfo.home_saturday_service_time }}</span>
              <span v-if="churchInfo.home_saturday_service_location">📍 {{ churchInfo.home_saturday_service_location }}</span>
            </div>
            <div class="card__content muted">適合週末較晚到的你，歡迎輕鬆加入。</div>
          </div>

          <div class="card card--hover">
            <h3 class="card__title h3">早崇聚會</h3>
            <div class="card__meta">
              <span>🕒 {{ churchInfo.home_main_service_time || '每週日上午 10:00' }}</span>
              <span>📍 {{ churchInfo.home_main_service_location || '榮耀堂' }}</span>
            </div>
            <div class="card__content muted">一起敬拜、一起學習、一起成長。</div>
          </div>
        </div>
      </div>
    </section>

    <!-- 重要通知（只顯示置頂公告） -->
    <section class="section section--tight" v-reveal v-if="pinnedAnnouncements && pinnedAnnouncements.length > 0">
      <div class="container">
        <div class="center" style="margin-bottom:18px">
          <h2 class="h2">重要通知</h2>
          <p class="muted" style="margin-top:8px">教會重要公告與最新資訊。</p>
        </div>

        <div class="grid grid-3">
          <article class="card card--hover" v-for="announcement in pinnedAnnouncements.slice(0, 3)" :key="announcement.id">
            <div class="card__body">
              <div class="tags" style="margin-bottom:10px">
                <span class="badge badge--accent">📌 置頂</span>
                <span v-if="announcement.category" class="badge">{{ announcement.category }}</span>
                <span class="badge">📅 {{ announcement.publishDate ? formatDate(announcement.publishDate) : formatDate(announcement.createdAt) }}</span>
              </div>

              <h3 class="card__title h3">{{ announcement.title }}</h3>

              <p class="muted" style="margin-top:10px; margin-bottom:0">
                <TruncatedText :text="announcement.content" :max-length="100" />
              </p>

              <div v-if="announcement.expireDate" style="margin-top:12px">
                <span class="badge">到期：{{ formatDate(announcement.expireDate) }}</span>
              </div>
            </div>
          </article>
        </div>

        <div class="center" style="margin-top:18px">
          <router-link to="/announcements" class="btn btn-ghost">查看全部消息 →</router-link>
        </div>
      </div>
    </section>

    <!-- 緊急代禱事項 -->
    <section class="section section--tight urgent-prayers" v-reveal v-if="urgentPrayerRequests && urgentPrayerRequests.length > 0">
      <div class="container">
        <div class="center" style="margin-bottom:18px">
          <h2 class="h2">🙏 緊急代禱</h2>
          <p class="muted" style="margin-top:8px">讓我們一起為這些需要代禱。</p>
        </div>

        <div class="grid grid-3">
          <article class="card card--hover card--urgent" v-for="prayer in urgentPrayerRequests.slice(0, 3)" :key="prayer.id">
            <div class="card__body">
              <div class="tags" style="margin-bottom:10px">
                <span class="badge badge--accent">🔥 緊急</span>
                <span v-if="prayer.category" class="badge">{{ prayer.category }}</span>
                <span class="badge">📅 {{ formatDate(prayer.createdAt) }}</span>
              </div>

              <h3 class="card__title h3">{{ prayer.title }}</h3>

              <p class="muted" style="margin-top:10px; margin-bottom:0">
                <TruncatedText :text="prayer.content" :max-length="100" />
              </p>
            </div>
          </article>
        </div>

        <div class="center" style="margin-top:18px">
          <router-link to="/prayer-requests" class="btn btn-ghost">查看全部代禱事項 →</router-link>
        </div>
      </div>
    </section>

    <!-- 最新活動 -->
    <section class="section section--tight" v-reveal v-if="upcomingActivities && upcomingActivities.length > 0">
      <div class="container">
        <div class="center" style="margin-bottom:18px">
          <h2 class="h2">最新活動</h2>
          <p class="muted" style="margin-top:8px">把握每次連結與成長的機會。</p>
        </div>

        <div class="grid grid-3">
          <article class="card card--hover" v-for="activity in upcomingActivities.slice(0, 3)" :key="activity.id">
            <div v-if="activity.imageUrl" class="media" style="height:190px">
              <img :src="activity.imageUrl" :alt="activity.title" />
            </div>

            <div style="margin-top:14px">
              <h3 class="card__title h3">{{ activity.title }}</h3>

              <div class="card__meta" style="margin-top:6px">
                <span v-if="activity.startTime || activity.endTime">🕒 {{ formatActivityTime(activity.startTime, activity.endTime) }}</span>
                <span v-if="activity.location">📍 {{ activity.location }}</span>
              </div>

              <p class="muted" style="margin-top:10px; margin-bottom:0">
                {{ activity.description }}
              </p>

              <div style="margin-top:12px">
                <span class="badge">📅 {{ activity.activityDate ? formatDate(activity.activityDate) : '' }}</span>
              </div>
            </div>
          </article>
        </div>

        <div class="center" style="margin-top:18px">
          <router-link to="/activities" class="btn btn-ghost">查看全部活動 →</router-link>
        </div>
      </div>
    </section>

    <div v-if="isLoading" class="loading">
      <p>載入中...</p>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue'
import TruncatedText from '@/components/TruncatedText.vue'
import { apiRequest } from '@/utils/api'

const churchInfo = ref(null)
const activities = ref([])
const announcements = ref([])
const prayerRequests = ref([])
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

const upcomingActivities = computed(() => {
  if (!activities.value || activities.value.length === 0) return []
  const today = new Date()
  today.setHours(0, 0, 0, 0)

  return activities.value
    .filter((activity) => {
      if (!activity.activityDate) return false
      const activityDate = new Date(activity.activityDate)
      activityDate.setHours(0, 0, 0, 0)
      return activityDate >= today
    })
    .sort((a, b) => new Date(a.activityDate) - new Date(b.activityDate))
})

const pinnedAnnouncements = computed(() => {
  if (!announcements.value || announcements.value.length === 0) return []
  
  // 只顯示置頂公告，按發布日期降序
  return announcements.value
    .filter(a => a.isPinned)
    .sort((a, b) => {
      const dateA = a.publishDate ? new Date(a.publishDate) : new Date(a.createdAt)
      const dateB = b.publishDate ? new Date(b.publishDate) : new Date(b.createdAt)
      return dateB - dateA
    })
})

const urgentPrayerRequests = computed(() => {
  if (!prayerRequests.value || prayerRequests.value.length === 0) return []
  
  // 只顯示緊急代禱事項，按建立日期降序
  return prayerRequests.value
    .filter(p => p.isUrgent)
    .sort((a, b) => new Date(b.createdAt) - new Date(a.createdAt))
})

// truncateText 函數已由 TruncatedText 元件取代

const formatActivityTime = (startTime, endTime) => {
  if (startTime && endTime) {
    return `${startTime} ~ ${endTime}`
  } else if (startTime) {
    return startTime
  } else if (endTime) {
    return endTime
  }
  return ''
}

const loadChurchInfo = async () => {
  try {
    const response = await apiRequest('/church/public/church-info', { method: 'GET' }, '載入教會資訊', false)

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
    const response = await apiRequest('/church/public/activities', { method: 'GET' }, '載入活動資訊', false)

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

const loadAnnouncements = async () => {
  try {
    const response = await apiRequest('/church/public/announcements', { method: 'GET' }, '載入公告', false)

    if (response.ok) {
      const data = await response.json()
      if (data.success && data.data) {
        announcements.value = data.data
      }
    }
  } catch (error) {
    console.error('載入公告失敗:', error)
  }
}

const loadPrayerRequests = async () => {
  try {
    const response = await apiRequest('/church/public/prayer-requests', { method: 'GET' }, '載入代禱事項', false)

    if (response.ok) {
      const data = await response.json()
      if (data.success && data.data) {
        prayerRequests.value = data.data
      }
    }
  } catch (error) {
    console.error('載入代禱事項失敗:', error)
  }
}

const loadData = async () => {
  isLoading.value = true
  try {
    await Promise.all([loadChurchInfo(), loadActivities(), loadAnnouncements(), loadPrayerRequests()])
  } finally {
    isLoading.value = false
  }
}

onMounted(loadData)
</script>
