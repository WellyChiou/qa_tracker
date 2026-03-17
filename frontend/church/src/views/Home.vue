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
            <router-link to="/activities" class="btn btn-primary">近期活動</router-link>
            <router-link to="/about" class="btn btn-ghost">認識教會</router-link>
            <router-link to="/contact" class="btn btn-ghost">聯絡</router-link>
          </div>
        </div>

        <div class="hero-side">
          <div class="hero-side__card" v-if="nextService">
            <span class="hero-side__label">本週節奏</span>
            <strong>{{ nextService.serviceTypeLabel }}</strong>
            <p>
              {{ nextService.displayDate }}
              <span v-if="nextService.displayWeekday">（{{ nextService.displayWeekday }}）</span>
            </p>
            <p>{{ nextService.displayTime }}</p>
          </div>

          <div class="hero-side__grid">
            <article class="hero-side__mini">
              <span>活動</span>
              <strong>{{ upcomingActivities.length }}</strong>
              <small>近期可參與</small>
            </article>
            <article class="hero-side__mini">
              <span>公告</span>
              <strong>{{ pinnedAnnouncements.length }}</strong>
              <small>置頂更新</small>
            </article>
            <article class="hero-side__mini">
              <span>代禱</span>
              <strong>{{ urgentPrayerRequests.length }}</strong>
              <small>緊急事項</small>
            </article>
          </div>
        </div>
      </div>
    </section>

    <section class="section section--tight" v-if="churchInfo">
      <div class="container">
        <div class="welcome-strip">
          <article class="welcome-strip__card" v-if="nextService">
            <span class="welcome-strip__eyebrow">{{ nextService.serviceTypeLabel }}</span>
            <strong>
              {{ nextService.displayDate }}
              <span v-if="nextService.displayWeekday">（{{ nextService.displayWeekday }}）</span>
            </strong>
            <p>{{ nextService.displayTime }}</p>
          </article>

          <article
            class="welcome-strip__card"
            v-if="churchInfo.home_saturday_service_time || churchInfo.home_saturday_service_location"
          >
            <span class="welcome-strip__eyebrow">晚崇聚會</span>
            <strong>{{ churchInfo.home_saturday_service_time || '歡迎參與' }}</strong>
            <p>{{ churchInfo.home_saturday_service_location || '適合週末較晚到的你。' }}</p>
          </article>

          <article class="welcome-strip__card welcome-strip__card--accent">
            <span class="welcome-strip__eyebrow">初次來訪</span>
            <strong>先從活動與聯絡開始</strong>
            <p>先認識教會，再找到適合你的聚會節奏與連結方式。</p>
          </article>
        </div>
      </div>
    </section>

    <!-- 聚會時間 -->
    <section class="section" v-reveal v-if="nextService">
      <div class="container">
        <div class="center" style="margin-bottom:18px">
          <h2 class="h2">聚會時間</h2>
          <p class="muted" style="margin-top:8px">歡迎你與我們一起敬拜，一同經歷神的同在。</p>
        </div>

        <div class="grid grid-2">
          <div class="card card--hover">
            <h3 class="card__title h3">{{ nextService.serviceTypeLabel }}</h3>
            <div class="card__meta">
              <span>
                📅 {{ nextService.displayDate }}
                <span v-if="nextService.displayWeekday">（{{ nextService.displayWeekday }}）</span>
              </span>
              <span>🕒 {{ nextService.displayTime }}</span>
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
          <router-link to="/announcements" class="btn btn-ghost">全部消息</router-link>
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
          <router-link to="/prayer-requests" class="btn btn-ghost">全部代禱</router-link>
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
          <router-link to="/activities" class="btn btn-ghost">全部活動</router-link>
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
const nextService = ref(null)
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
    const data = await apiRequest('/church/public/church-info', { method: 'GET' }, '載入教會資訊', false)
    churchInfo.value = data || null
  } catch (error) {
    console.error('載入教會資訊失敗:', error)
  }
}

const loadActivities = async () => {
  try {
    const data = await apiRequest('/church/public/activities', { method: 'GET' }, '載入活動資訊', false)
    activities.value = Array.isArray(data) ? data : []
  } catch (error) {
    console.error('載入活動資訊失敗:', error)
  }
}

const loadAnnouncements = async () => {
  try {
    const data = await apiRequest('/church/public/announcements', { method: 'GET' }, '載入公告', false)
    announcements.value = Array.isArray(data) ? data : []
  } catch (error) {
    console.error('載入公告失敗:', error)
  }
}

const loadPrayerRequests = async () => {
  try {
    const data = await apiRequest('/church/public/prayer-requests', { method: 'GET' }, '載入代禱事項', false)
    prayerRequests.value = Array.isArray(data) ? data : []
  } catch (error) {
    console.error('載入代禱事項失敗:', error)
  }
}

const loadNextService = async () => {
  try {
    const data = await apiRequest('/church/public/next-service', { method: 'GET' }, '載入聚會資訊', false)
    nextService.value = data || null
  } catch (error) {
    console.error('載入下一場聚會失敗:', error)
    nextService.value = null
  }
}

const loadData = async () => {
  isLoading.value = true
  try {
    await Promise.all([
      loadChurchInfo(),
      loadActivities(),
      loadAnnouncements(),
      loadPrayerRequests(),
      loadNextService()
    ])
  } finally {
    isLoading.value = false
  }
}

onMounted(loadData)
</script>

<style scoped>
.hero-surface {
  display: grid;
  grid-template-columns: minmax(0, 1.35fr) minmax(280px, 0.8fr);
  gap: 1rem;
  align-items: stretch;
}

.hero-side {
  display: flex;
  flex-direction: column;
  gap: 0.85rem;
}

.hero-side__card,
.hero-side__mini,
.welcome-strip__card {
  border-radius: 16px;
  border: 1px solid rgba(23, 33, 47, 0.08);
  background: rgba(255, 255, 255, 0.74);
  box-shadow: 0 14px 30px rgba(23, 33, 47, 0.08);
  backdrop-filter: blur(14px);
}

.hero-side__card {
  padding: 1.05rem;
  min-height: 150px;
  display: flex;
  flex-direction: column;
  justify-content: flex-end;
  background:
    linear-gradient(160deg, rgba(255, 255, 255, 0.96), rgba(244, 237, 223, 0.84));
}

.hero-side__label,
.welcome-strip__eyebrow {
  display: inline-flex;
  width: fit-content;
  padding: 0.32rem 0.56rem;
  border-radius: 999px;
  background: rgba(190, 140, 66, 0.12);
  color: #8a5a1f;
  font-size: 0.66rem;
  font-weight: 900;
  letter-spacing: 0.1em;
  text-transform: uppercase;
}

.hero-side__card strong,
.welcome-strip__card strong {
  margin-top: 0.7rem;
  color: #17212f;
  font-size: 1.35rem;
  line-height: 1.05;
  letter-spacing: -0.04em;
}

.hero-side__card p,
.welcome-strip__card p {
  margin: 0.45rem 0 0;
  color: rgba(23, 33, 47, 0.62);
  font-weight: 600;
  line-height: 1.65;
}

.hero-side__grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 0.7rem;
}

.hero-side__mini {
  padding: 0.85rem 0.8rem;
  min-height: 112px;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
}

.hero-side__mini span {
  color: rgba(23, 33, 47, 0.56);
  font-size: 0.68rem;
  font-weight: 800;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.hero-side__mini strong {
  color: #17212f;
  font-size: 1.6rem;
  line-height: 1;
  letter-spacing: -0.05em;
}

.hero-side__mini small {
  color: rgba(23, 33, 47, 0.56);
  font-size: 0.74rem;
  font-weight: 700;
}

.welcome-strip {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 0.85rem;
}

.welcome-strip__card {
  padding: 1rem;
  min-height: 142px;
}

.welcome-strip__card--accent {
  background:
    linear-gradient(140deg, rgba(28, 76, 130, 0.96), rgba(70, 126, 188, 0.9));
}

.welcome-strip__card--accent .welcome-strip__eyebrow {
  background: rgba(255, 255, 255, 0.14);
  color: rgba(255, 255, 255, 0.86);
}

.welcome-strip__card--accent strong,
.welcome-strip__card--accent p {
  color: white;
}

.welcome-strip__card--accent p {
  color: rgba(255, 255, 255, 0.78);
}

@media (max-width: 980px) {
  .hero-surface,
  .welcome-strip {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 720px) {
  .hero-side__grid {
    grid-template-columns: 1fr;
  }

  .hero-side__mini,
  .hero-side__card,
  .welcome-strip__card {
    min-height: auto;
  }
}
</style>
