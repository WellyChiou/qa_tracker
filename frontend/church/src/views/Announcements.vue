<template>
  <div>
    <section class="page-hero" data-hero="true">
      <div class="container hero-surface">
        <div class="hero-inner">
          <div class="badge">Announcements</div>
          <h1 class="h1" style="margin-top:12px">最新消息</h1>
          <p class="lead" style="margin-top:10px">教會重要公告與最新資訊。</p>
        </div>
      </div>
    </section>

    <section class="section section--tight">
      <div class="container">
        <div v-if="isLoading" class="loading"><p>載入中...</p></div>

        <div v-else-if="announcementsWithFormattedData.length > 0" class="grid grid-3">
          <article class="card card--hover card--activity" v-for="announcement in announcementsWithFormattedData" :key="announcement.id">
            <div class="card__body">
              <div class="tags" style="margin-bottom:10px">
                <span v-if="announcement.isPinned" class="badge badge--accent">📌 置頂</span>
                <span v-if="announcement.category" class="badge">{{ announcement.category }}</span>
                <span class="badge">📅 {{ announcement.date }}</span>
              </div>

              <h3 class="card__title h3">{{ announcement.title }}</h3>

              <p class="muted card__desc">
                {{ announcement.content }}
              </p>

              <div v-if="announcement.expireDate" class="card__footer">
                <span class="badge">到期：{{ announcement.expireDateFormatted }}</span>
              </div>
            </div>
          </article>
        </div>

        <div v-else class="loading"><p>目前沒有公告</p></div>
      </div>
    </section>
  </div>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue'
import { apiRequest } from '@/utils/api'

const announcements = ref([])
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

const announcementsWithFormattedData = computed(() => {
  // 先排序：置頂的在前，然後按發布日期降序
  const sorted = [...announcements.value].sort((a, b) => {
    if (a.isPinned && !b.isPinned) return -1
    if (!a.isPinned && b.isPinned) return 1
    if (!a.publishDate && !b.publishDate) return 0
    if (!a.publishDate) return 1
    if (!b.publishDate) return -1
    return new Date(b.publishDate) - new Date(a.publishDate)
  })
  
  return sorted.map(announcement => ({
    ...announcement,
    date: formatDate(announcement.publishDate),
    expireDateFormatted: announcement.expireDate ? formatDate(announcement.expireDate) : null
  }))
})

const loadAnnouncements = async () => {
  isLoading.value = true
  try {
    const data = await apiRequest('/church/public/announcements', { method: 'GET' }, '載入公告', false)
    announcements.value = Array.isArray(data) ? data : []
  } catch (error) {
    console.error('載入公告失敗:', error)
  } finally {
    isLoading.value = false
  }
}

onMounted(loadAnnouncements)
</script>
