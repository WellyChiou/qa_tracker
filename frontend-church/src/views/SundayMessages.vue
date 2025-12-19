<template>
  <div>
    <section class="page-hero" data-hero="true">
      <div class="container hero-surface">
        <div class="hero-inner">
          <div class="badge">Messages</div>
          <h1 class="h1" style="margin-top:12px">主日信息</h1>
          <p class="lead" style="margin-top:10px">回顧信息、再次默想神的話。</p>
        </div>
      </div>
    </section>

    <section class="section section--tight">
      <div class="container">
        <div v-if="isLoading" class="loading"><p>載入中...</p></div>

        <div v-else-if="messagesWithFormattedData.length > 0" class="grid grid-3">
          <article class="card card--hover" v-for="message in messagesWithFormattedData" :key="message.id">
            <div v-if="message.imageUrl" class="media" style="height:190px">
              <img :src="message.imageUrl" :alt="message.title || '主日信息'" />
            </div>

            <div style="margin-top:14px">
              <div class="tags" style="margin-bottom:10px">
                <span class="badge" :class="message.typeClass">{{ message.typeText }}</span>
                <span class="badge badge--accent">📅 {{ message.date }}</span>
              </div>

              <h3 class="card__title h3">{{ message.title || '主日信息' }}</h3>

              <div class="card__meta" style="margin-top:6px">
                <span v-if="message.scripture">📖 {{ message.scripture }}</span>
                <span v-if="message.speaker">🎤 {{ message.speaker }}</span>
              </div>

              <p v-if="message.content" class="muted message-content" style="margin-top:10px; margin-bottom:0">
                {{ message.content }}
              </p>
            </div>
          </article>
        </div>

        <div v-else class="loading"><p>目前沒有主日信息</p></div>
      </div>
    </section>
  </div>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue'
import { apiRequest } from '@/utils/api'

const messages = ref([])
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

const messagesWithFormattedData = computed(() => {
  // 先排序：最新到最舊（按 serviceDate 降序）
  const sortedMessages = [...messages.value].sort((a, b) => {
    if (!a.serviceDate && !b.serviceDate) return 0
    if (!a.serviceDate) return 1  // 沒有日期的排在後面
    if (!b.serviceDate) return -1 // 沒有日期的排在後面
    return new Date(b.serviceDate) - new Date(a.serviceDate) // 降序：最新在前
  })
  
  return sortedMessages.map(message => ({
    ...message,
    date: formatDate(message.serviceDate),
    typeText: message.serviceType === 'SATURDAY' ? '週六晚崇' : '週日早崇',
    typeClass: message.serviceType === 'SATURDAY' ? 'type-saturday' : 'type-sunday'
  }))
})

const loadMessages = async () => {
  isLoading.value = true
  try {
    const response = await apiRequest('/church/public/sunday-messages', {
      method: 'GET'
    }, '載入主日信息', false)
    
    if (response.ok) {
      const data = await response.json()
      if (data.success && data.data) {
        messages.value = data.data
      }
    }
  } catch (error) {
    console.error('載入主日信息失敗:', error)
  } finally {
    isLoading.value = false
  }
}

onMounted(() => {
  loadMessages()
})
</script>


<style scoped>
.type-saturday {
  background: rgba(244, 180, 0, 0.12);
  border: 1px solid rgba(244, 180, 0, 0.25);
  color: #7a5a00;
}
.type-sunday {
  background: rgba(31, 157, 106, 0.10);
  border: 1px solid rgba(31, 157, 106, 0.20);
  color: var(--primary-2);
}
.message-content { white-space: pre-wrap; }
</style>
