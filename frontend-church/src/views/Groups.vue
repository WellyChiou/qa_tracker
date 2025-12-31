<template>
  <div>
    <section class="page-hero" data-hero="true">
      <div class="container hero-surface">
        <div class="hero-inner">
          <div class="badge">Groups</div>
          <h1 class="h1" style="margin-top:12px">小組介紹</h1>
          <p class="lead" style="margin-top:10px">認識我們的小組，一起成長、一起服事。</p>
        </div>
      </div>
    </section>

    <section class="section section--tight">
      <div class="container">
        <div v-if="isLoading" class="loading"><p>載入中...</p></div>

        <div v-else-if="groupsWithFormattedData.length > 0" class="grid grid-3">
          <article class="card card--hover" v-for="group in groupsWithFormattedData" :key="group.id">
            <div class="card__body">
              <div class="tags" style="margin-bottom:10px" v-if="group.category || group.meetingFrequency || group.meetingLocation">
                <span class="badge badge--accent" v-if="group.category">{{ group.category }}</span>
                <span class="badge" v-if="group.meetingFrequency">🕒 {{ group.meetingFrequency }}</span>
                <span class="badge" v-if="group.meetingLocation">📍 {{ group.meetingLocation }}</span>
              </div>

              <h3 class="card__title h3">{{ group.groupName }}</h3>

              <p v-if="group.description" class="muted card__desc" style="margin-top:10px">
                {{ group.description }}
              </p>

              <div class="card__footer" style="margin-top:12px">
                <span class="badge">成員數: {{ group.memberCount }}</span>
                <span class="badge" v-if="hasLeaders(group)">👥 {{ getLeaderCount(group) }} 位小組長</span>
              </div>

              <!-- 顯示成員列表（包含角色標記） -->
              <div v-if="group.members && group.members.length > 0" class="members-list" style="margin-top:12px; padding-top:12px; border-top:1px solid var(--border)">
                <div class="member-item" v-for="member in group.members.slice(0, 5)" :key="member.id">
                  <span>{{ member.personName || member.displayName || '-' }}</span>
                  <span class="member-role" v-if="member.role === 'LEADER'">👑 小組長</span>
                  <span class="member-role" v-else-if="member.role === 'ASSISTANT_LEADER'">⭐ 實習小組長</span>
                </div>
                <div v-if="group.members.length > 5" class="muted" style="margin-top:8px; font-size:12px">
                  還有 {{ group.members.length - 5 }} 位成員...
                </div>
              </div>
            </div>
          </article>
        </div>

        <div v-else class="loading"><p>目前沒有小組資訊</p></div>
      </div>
    </section>
  </div>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue'
import { apiRequest } from '@/utils/api'

const groups = ref([])
const isLoading = ref(false)

const groupsWithFormattedData = computed(() => {
  return groups.value.map((group) => ({
    ...group,
    memberCount: group.members ? group.members.length : 0
  }))
})

const hasLeaders = (group) => {
  if (!group.members || group.members.length === 0) return false
  return group.members.some(m => m.role === 'LEADER' || m.role === 'ASSISTANT_LEADER')
}

const getLeaderCount = (group) => {
  if (!group.members || group.members.length === 0) return 0
  return group.members.filter(m => m.role === 'LEADER' || m.role === 'ASSISTANT_LEADER').length
}

const loadGroups = async () => {
  isLoading.value = true
  try {
    const response = await apiRequest('/church/public/groups', { method: 'GET' }, '載入小組資訊', false)

    if (response.ok) {
      const data = await response.json()
      if (data.success && data.data) {
        groups.value = data.data
      }
    }
  } catch (error) {
    console.error('載入小組資訊失敗:', error)
  } finally {
    isLoading.value = false
  }
}

onMounted(loadGroups)
</script>

<style scoped>
/* 可以參考 Activities.vue 的樣式，或根據需要新增 */
.card__desc {
  white-space: pre-wrap;
}

.members-list {
  font-size: 14px;
}

.member-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 6px 0;
  border-bottom: 1px solid rgba(0, 0, 0, 0.05);
}

.member-item:last-child {
  border-bottom: none;
}

.member-role {
  font-size: 12px;
  color: var(--accent);
  font-weight: 600;
  margin-left: 8px;
}

.tags {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}
</style>
