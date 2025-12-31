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
          <article class="card card--hover card--group" v-for="group in groupsWithFormattedData" :key="group.id">
            <div class="card__body">
              <!-- 區分和小組名稱在同一行 -->
              <div class="group-header">
                <h3 class="card__title h3 group-title">
                  <span v-if="group.category" class="category-badge">{{ group.category }}</span>
                  <span class="group-name">{{ group.groupName }}</span>
                </h3>
              </div>

              <!-- 時間與地點使用 tag 樣式 -->
              <div class="group-meta-tags" v-if="group.meetingFrequency || group.meetingLocation">
                <span v-if="group.meetingFrequency" class="meta-tag">
                  <svg class="tag-icon" viewBox="0 0 24 24" width="14" height="14" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                    <circle cx="12" cy="12" r="10"/>
                    <polyline points="12 6 12 12 16 14"/>
                  </svg>
                  {{ group.meetingFrequency }}
                </span>
                <span v-if="group.meetingLocation" class="meta-tag">
                  <svg class="tag-icon" viewBox="0 0 24 24" width="14" height="14" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                    <path d="M21 10c0 7-9 13-9 13s-9-6-9-13a9 9 0 0 1 18 0z"/>
                    <circle cx="12" cy="10" r="3"/>
                  </svg>
                  {{ group.meetingLocation }}
                </span>
              </div>

              <!-- 描述使用美化框 -->
              <div v-if="group.description" class="group-description">
                <p class="description-text">{{ group.description }}</p>
              </div>

              <!-- 只顯示小組長和實習小組長 -->
              <div v-if="group.members && group.members.length > 0" class="members-list">
                <div class="member-item" v-for="member in group.members" :key="member.id">
                  <span class="member-name">{{ member.personName || member.displayName || '-' }}</span>
                  <span class="member-role-badge" v-if="member.role === 'LEADER'">
                    <svg class="role-icon" viewBox="0 0 24 24" width="14" height="14" fill="currentColor">
                      <path d="M12 2L2 7l10 5 10-5-10-5zM2 17l10 5 10-5M2 12l10 5 10-5"/>
                    </svg>
                    小組長
                  </span>
                  <span class="member-role-badge member-role-badge--assistant" v-else-if="member.role === 'ASSISTANT_LEADER'">
                    <svg class="role-icon" viewBox="0 0 24 24" width="14" height="14" fill="currentColor">
                      <polygon points="12 2 15.09 8.26 22 9.27 17 14.14 18.18 21.02 12 17.77 5.82 21.02 7 14.14 2 9.27 8.91 8.26 12 2"/>
                    </svg>
                    實習小組長
                  </span>
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
/* 小組卡片美化 */
.card--group {
  transition: transform 0.2s ease, box-shadow 0.2s ease;
}

.card--group:hover {
  transform: translateY(-2px);
  box-shadow: 0 12px 24px rgba(16, 24, 40, 0.12);
}

/* 區分和小組名稱在同一行 */
.group-header {
  margin-bottom: 12px;
}

.group-title {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
  margin: 0;
  line-height: 1.4;
}

.category-badge {
  display: inline-block;
  background: var(--accent);
  color: #7a5a00;
  padding: 4px 10px;
  border-radius: 8px;
  font-size: 13px;
  font-weight: 700;
  white-space: nowrap;
  box-shadow: 0 2px 4px rgba(244, 180, 0, 0.2);
}

.group-name {
  flex: 1;
  min-width: 0;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

/* 時間和地點 tag 樣式 */
.group-meta-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 12px;
}

.meta-tag {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  background: rgba(31, 157, 106, 0.08);
  color: var(--primary-2);
  padding: 6px 12px;
  border-radius: 20px;
  font-size: 13px;
  font-weight: 600;
  border: 1px solid rgba(31, 157, 106, 0.15);
  transition: all 0.2s ease;
}

.meta-tag:hover {
  background: rgba(31, 157, 106, 0.12);
  border-color: rgba(31, 157, 106, 0.25);
}

.tag-icon {
  flex-shrink: 0;
  color: var(--primary);
}

/* 描述美化框 */
.group-description {
  background: linear-gradient(135deg, rgba(31, 157, 106, 0.04), rgba(244, 180, 0, 0.03));
  border-left: 3px solid var(--primary);
  border-radius: 12px;
  padding: 14px 16px;
  margin: 12px 0;
  position: relative;
}

.group-description::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: radial-gradient(circle at top left, rgba(31, 157, 106, 0.05), transparent 50%);
  border-radius: 12px;
  pointer-events: none;
}

.description-text {
  margin: 0;
  color: var(--text);
  font-size: 14px;
  line-height: 1.7;
  white-space: pre-wrap;
  position: relative;
  z-index: 1;
}

/* 成員列表美化 */
.members-list {
  margin-top: 16px;
  padding-top: 16px;
  border-top: 2px solid rgba(31, 157, 106, 0.1);
  font-size: 14px;
}

.member-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 10px 12px;
  margin-bottom: 6px;
  background: rgba(31, 157, 106, 0.03);
  border-radius: 10px;
  border: 1px solid rgba(31, 157, 106, 0.08);
  transition: all 0.2s ease;
}

.member-item:hover {
  background: rgba(31, 157, 106, 0.06);
  border-color: rgba(31, 157, 106, 0.15);
  transform: translateX(2px);
}

.member-item:last-child {
  margin-bottom: 0;
}

.member-name {
  font-weight: 600;
  color: var(--text);
}

.member-role-badge {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  background: linear-gradient(135deg, var(--accent), #f4b400);
  color: #7a5a00;
  padding: 4px 10px;
  border-radius: 12px;
  font-size: 12px;
  font-weight: 700;
  box-shadow: 0 2px 6px rgba(244, 180, 0, 0.25);
}

.member-role-badge--assistant {
  background: linear-gradient(135deg, rgba(31, 157, 106, 0.15), rgba(31, 157, 106, 0.25));
  color: var(--primary-2);
  box-shadow: 0 2px 6px rgba(31, 157, 106, 0.2);
}

.role-icon {
  flex-shrink: 0;
}
</style>
