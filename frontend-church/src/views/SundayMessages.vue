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
          <article class="card card--hover message-card" v-for="message in messagesWithFormattedData" :key="message.id">
            <div v-if="message.imageUrl" class="media" style="height:190px">
              <img :src="message.imageUrl" :alt="message.title || '主日信息'" />
            </div>

            <div class="message-card__body">
              <div class="message-card__header">
                <div class="message-card__date">
                  <span class="message-card__date-label">{{ message.dateLabel }}</span>
                  <span class="message-card__date-day">
                    {{ message.dateDay }}
                    <small v-if="message.dateWeekday">（{{ message.dateWeekday }}）</small>
                  </span>
                </div>
                <div class="message-card__type">
                  <span class="badge" :class="message.typeClass">{{ message.typeText }}</span>
                  <span class="message-card__time">{{ message.displayTime }}</span>
                </div>
              </div>

              <h3 class="card__title h3">{{ message.title || '主日信息' }}</h3>

              <div class="message-card__meta">
                <span v-if="message.scripture">📖 {{ message.scripture }}</span>
                <span v-if="message.speaker">🎤 {{ message.speaker }}</span>
              </div>

              <p v-if="message.content" class="muted message-content">
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

const resolveDateFields = (serviceDate, createdAt, weekdays) => {
  const fallbackDate = serviceDate || createdAt
  if (!fallbackDate) {
    return ['日期未設定', '——', '']
  }

  const date = new Date(fallbackDate)
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  const dayOfWeek = weekdays[date.getDay()]
  return [`${year}/${month}`, day, dayOfWeek]
}

const resolveServiceInfo = (type) => {
  if (type === 'SATURDAY') {
    return { label: '週六晚崇', displayTime: '晚上 7:00', typeClass: 'type-saturday' }
  }
  return { label: '週日早崇', displayTime: '上午 10:00', typeClass: 'type-sunday' }
}

const messagesWithFormattedData = computed(() => {
  // 先排序：最新到最舊（按 serviceDate 降序）
  const sortedMessages = [...messages.value].sort((a, b) => {
    const dateA = a.serviceDate || a.messageDate
    const dateB = b.serviceDate || b.messageDate
    if (!dateA && !dateB) return 0
    if (!dateA) return 1
    if (!dateB) return -1
    return new Date(dateB) - new Date(dateA)
  })
  
  const weekdays = ['日', '一', '二', '三', '四', '五', '六']

  return sortedMessages.map(message => {
    const rawDate = message.serviceDate || message.messageDate
    const [dateLabel, dateDay, dateWeekday] = resolveDateFields(rawDate, message.createdAt, weekdays)
    const { label: typeText, displayTime, typeClass } = resolveServiceInfo(message.serviceType)

    return {
      ...message,
      dateLabel,
      dateDay,
      dateWeekday,
      typeText,
      typeClass,
      displayTime
    }
  })
})

const loadMessages = async () => {
  isLoading.value = true
  try {
    const data = await apiRequest('/church/public/sunday-messages', {
      method: 'GET'
    }, '載入主日信息', false)
    messages.value = Array.isArray(data) ? data : []
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

.message-card__body {
  margin-top: 14px;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.message-card__header {
  display: flex;
  justify-content: space-between;
  gap: 1rem;
  flex-wrap: wrap;
}

.message-card__date {
  display: flex;
  flex-direction: column;
}

.message-card__date-label {
  font-size: 0.75rem;
  color: rgba(23, 33, 47, 0.56);
  letter-spacing: 0.1em;
  text-transform: uppercase;
}

.message-card__date-day {
  font-size: 1.55rem;
  font-weight: 700;
  line-height: 1;
}

.message-card__type {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 0.35rem;
}

.message-card__time {
  font-size: 0.95rem;
  color: rgba(23, 33, 47, 0.75);
  font-weight: 600;
}

.message-card__meta {
  display: flex;
  flex-direction: column;
  gap: 0.35rem;
  color: rgba(23, 33, 47, 0.7);
  font-weight: 600;
}
</style>
