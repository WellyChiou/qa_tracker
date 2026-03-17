<template>
  <AdminLayout>
    <div class="admin-page" data-ui="admin-v1">
      <div class="container container--admin">
        <section class="page-head card">
          <div>
            <span class="dashboard-kicker">Operations Workspace</span>
            <h2>後台儀表板</h2>
            <p>重點入口與核心數量一眼掌握，直接進到常用操作。</p>
          </div>

          <div class="head-stats">
            <div class="chip">
              <span>服事表</span>
              <strong>{{ scheduleCount }}</strong>
            </div>
            <div class="chip">
              <span>人員</span>
              <strong>{{ personCount }}</strong>
            </div>
            <div class="chip">
              <span>崗位</span>
              <strong>{{ positionCount }}</strong>
            </div>
            <div class="chip ghost">
              <span>常用入口</span>
              <strong>{{ quickActions.length }}</strong>
            </div>
          </div>
        </section>

        <section class="dashboard-grid">
          <div class="card">
            <div class="card__head">
              <div>
                <div class="section-title">快速操作</div>
                <div class="muted">常用入口</div>
              </div>
            </div>
            <div class="card__body">
              <div class="action-grid">
                <router-link
                  v-for="action in quickActions"
                  :key="action.id"
                  :to="action.url"
                  class="action-tile"
                >
                  <div class="tile-icon">{{ action.icon || '📋' }}</div>
                  <div class="tile-title">{{ action.menuName }}</div>
                  <div class="tile-desc">{{ action.description || '快速訪問' }}</div>
                </router-link>
              </div>
            </div>
          </div>

          <div class="card insights-card">
            <div class="card__head">
              <div>
                <div class="section-title">管理提示</div>
                <div class="muted">依資料量整理優先順序</div>
              </div>
            </div>
            <div class="card__body insights-list">
              <article class="insight-item">
                <strong>排班節奏</strong>
                <p v-if="scheduleCount > 0">目前已建立 {{ scheduleCount }} 筆服事表，建議優先檢查近期主日的人力是否完整。</p>
                <p v-else>目前尚未建立服事表，建議先從主日與固定聚會排班開始。</p>
              </article>
              <article class="insight-item">
                <strong>同工資料</strong>
                <p v-if="personCount > 0">共有 {{ personCount }} 位人員資料，適合持續整理角色、群組與服事關聯。</p>
                <p v-else>尚未建立人員資料，建議先匯整核心同工與固定參與者。</p>
              </article>
              <article class="insight-item">
                <strong>崗位配置</strong>
                <p v-if="positionCount > 0">目前維護 {{ positionCount }} 個崗位，可定期檢查職責與排班是否一致。</p>
                <p v-else>尚未建立崗位，建議先盤點主日固定服事項目。</p>
              </article>
            </div>
          </div>
        </section>
      </div>
    </div>
  </AdminLayout>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import AdminLayout from '@/components/AdminLayout.vue'
import { apiRequest } from '@/utils/api'

const scheduleCount = ref(0)
const personCount = ref(0)
const positionCount = ref(0)
const quickActions = ref([])

const totalManagedItems = computed(() => scheduleCount.value + personCount.value + positionCount.value)

const summaryHeadline = computed(() => {
  if (totalManagedItems.value === 0) return '後台剛啟動，適合先建立基礎資料。'
  if (scheduleCount.value === 0) return '先補齊服事表，讓排班流程可視化。'
  if (personCount.value >= positionCount.value * 3) return '資料已逐漸成形，可以開始優化權限與排班。'
  return '核心資料已在線上，建議持續整理人員與崗位對應。'
})

const summaryText = computed(() => {
  if (quickActions.value.length === 0) return '目前尚未載入捷徑選單，仍可從上方導航進入各模組。'
  return `目前有 ${quickActions.value.length} 個常用入口可直接操作，適合從最常維護的模組開始。`
})

const loadStats = async () => {
  try {
    const [schedulesData, personsData, positionsData] = await Promise.all([
      apiRequest('/church/service-schedules', { method: 'GET', credentials: 'include' }),
      apiRequest('/church/persons', { method: 'GET', credentials: 'include' }),
      apiRequest('/church/positions', { method: 'GET', credentials: 'include' })
    ])

    if (schedulesData) {
      const schedules = schedulesData.content || schedulesData || []
      scheduleCount.value = Array.isArray(schedules) ? schedules.length : 0
    }

    if (personsData) {
      const persons = personsData.content || personsData.persons || personsData || []
      personCount.value = Array.isArray(persons) ? persons.length : 0
    }

    if (positionsData) {
      const positions = positionsData.positions || positionsData || []
      positionCount.value = Array.isArray(positions) ? positions.length : 0
    }
  } catch (error) {
    console.error('載入統計數據失敗:', error)
  }
}

const loadQuickActions = async () => {
  try {
    const data = await apiRequest('/church/menus/dashboard', {
      method: 'GET',
      credentials: 'include'
    })

    if (data) {
      quickActions.value = data || []
    }
  } catch (error) {
    console.error('載入快速操作失敗:', error)
  }
}

onMounted(() => {
  loadStats()
  loadQuickActions()
})
</script>

<style scoped>
.page-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 1rem;
  padding: 1.25rem 1.35rem;
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.9);
  border: 1px solid rgba(148, 163, 184, 0.14);
  box-shadow: 0 20px 44px rgba(15, 23, 42, 0.08);
}

.dashboard-kicker {
  display: inline-flex;
  align-items: center;
  padding: 0.32rem 0.6rem;
  border-radius: 999px;
  background: rgba(37, 99, 235, 0.12);
  border: 1px solid rgba(37, 99, 235, 0.18);
  font-size: 0.7rem;
  font-weight: 900;
  letter-spacing: 0.12em;
  text-transform: uppercase;
  color: #1e3a8a;
}

.page-head h2 {
  margin: 0.5rem 0 0.25rem;
  font-size: clamp(1.7rem, 3vw, 2.2rem);
  letter-spacing: -0.03em;
}

.page-head p {
  margin: 0;
  color: rgba(15, 23, 42, 0.62);
  font-weight: 600;
}

.head-stats {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(120px, 1fr));
  gap: 0.65rem;
  min-width: 360px;
}

.chip {
  padding: 0.75rem 0.85rem;
  border-radius: 14px;
  border: 1px solid rgba(148, 163, 184, 0.16);
  background: rgba(248, 250, 252, 0.92);
}

.chip.ghost {
  background: rgba(37, 99, 235, 0.06);
  border-color: rgba(37, 99, 235, 0.14);
  color: #1d4ed8;
}

.chip span {
  display: block;
  font-size: 0.8rem;
  letter-spacing: 0.05em;
  text-transform: uppercase;
  color: rgba(15, 23, 42, 0.58);
  font-weight: 800;
}

.chip strong {
  display: block;
  margin-top: 0.25rem;
  font-size: 1.15rem;
  letter-spacing: -0.02em;
}

.section-title {
  font-size: 14px;
  font-weight: 900;
  letter-spacing: -0.01em;
  padding-left: 6px;
}

.card__head {
  padding: 6px 8px 4px;
}

.dashboard-grid {
  display: grid;
  grid-template-columns: minmax(0, 1.45fr) minmax(280px, 0.9fr);
  gap: 16px;
  margin-top: 12px;
}

.action-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(240px, 1fr));
  gap: 14px;
}

.action-tile {
  display: flex;
  flex-direction: column;
  gap: 6px;
  padding: 14px;
  border-radius: 14px;
  border: 1px solid rgba(2, 6, 23, 0.1);
  background: rgba(255, 255, 255, 0.85);
  text-decoration: none;
  color: inherit;
  transition: transform 0.12s ease, box-shadow 0.12s ease;
}

.action-tile:hover {
  transform: translateY(-1px);
  box-shadow: var(--shadow-sm);
}

.tile-icon {
  width: 38px;
  height: 38px;
  display: grid;
  place-items: center;
  border-radius: 14px;
  background: rgba(2, 6, 23, 0.04);
  border: 1px solid rgba(2, 6, 23, 0.06);
  font-size: 18px;
}

.tile-title {
  font-weight: 900;
  letter-spacing: -0.01em;
  font-size: 14px;
}

.tile-desc {
  font-size: 12px;
  color: rgba(2, 6, 23, 0.6);
  font-weight: 700;
  line-height: 1.3;
}

.insights-card {
  align-self: start;
  min-height: 100%;
}

.insights-list {
  display: grid;
  gap: 14px;
}

.insight-item {
  padding: 14px;
  border-radius: 14px;
  border: 1px solid rgba(2, 6, 23, 0.06);
  background: rgba(248, 250, 252, 0.92);
  box-shadow: 0 12px 28px rgba(15, 23, 42, 0.05);
}

.insight-item strong {
  display: block;
  margin-bottom: 6px;
  font-size: 14px;
  font-weight: 900;
  letter-spacing: -0.01em;
}

.insight-item p {
  margin: 0;
  color: rgba(2, 6, 23, 0.64);
  font-size: 13px;
  line-height: 1.6;
  font-weight: 700;
}

@media (max-width: 900px) {
  .dashboard-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 640px) {
  .page-head {
    flex-direction: column;
    align-items: flex-start;
  }

  .head-stats {
    width: 100%;
    min-width: 0;
  }

  .dashboard-grid {
    margin-top: 8px;
  }
}
</style>
