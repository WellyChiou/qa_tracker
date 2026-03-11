<template>
  <AdminLayout>
    <div class="admin-page" data-ui="admin-v1">
      <div class="container container--admin">
        <section class="dashboard-hero card">
          <div class="dashboard-hero__copy">
            <span class="dashboard-kicker">Operations Overview</span>
            <h2>把教會內容、服事排班與現場作業放在同一個總覽。</h2>
            <p>
              這裡是後台的起點。先看整體量體，再從常用捷徑進到人員、崗位、服事表與內容維運。
            </p>

            <div class="dashboard-hero__metrics">
              <article class="metric-chip">
                <span>服事表</span>
                <strong>{{ scheduleCount }}</strong>
              </article>
              <article class="metric-chip">
                <span>人員</span>
                <strong>{{ personCount }}</strong>
              </article>
              <article class="metric-chip">
                <span>崗位</span>
                <strong>{{ positionCount }}</strong>
              </article>
            </div>
          </div>

          <div class="dashboard-hero__panel">
            <article class="panel-card panel-card--primary">
              <span>今日重點</span>
              <strong>{{ summaryHeadline }}</strong>
              <p>{{ summaryText }}</p>
            </article>

            <div class="panel-grid">
              <article class="panel-card">
                <span>資料規模</span>
                <strong>{{ totalManagedItems }}</strong>
                <p>目前已納入管理的核心資料總數。</p>
              </article>
              <article class="panel-card">
                <span>快速入口</span>
                <strong>{{ quickActions.length }}</strong>
                <p>後台已開啟的常用操作捷徑。</p>
              </article>
            </div>
          </div>
        </section>

        <div class="dashboard-stats">
          <div class="card stat-card">
            <div class="stat-left">
              <div class="stat-badge">📋</div>
              <div>
                <div class="stat-label">服事表</div>
                <div class="stat-value">{{ scheduleCount }}</div>
              </div>
            </div>
            <div class="stat-hint muted">近期待辦與排班</div>
          </div>

          <div class="card stat-card">
            <div class="stat-left">
              <div class="stat-badge">👥</div>
              <div>
                <div class="stat-label">人員</div>
                <div class="stat-value">{{ personCount }}</div>
              </div>
            </div>
            <div class="stat-hint muted">同工 / 會眾資料</div>
          </div>

          <div class="card stat-card">
            <div class="stat-left">
              <div class="stat-badge">🎯</div>
              <div>
                <div class="stat-label">崗位</div>
                <div class="stat-value">{{ positionCount }}</div>
              </div>
            </div>
            <div class="stat-hint muted">服事角色與分工</div>
          </div>
        </div>

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
.dashboard-hero {
  display: grid;
  grid-template-columns: minmax(0, 1.15fr) minmax(300px, 0.85fr);
  gap: 0.85rem;
  padding: 0.9rem;
  background: linear-gradient(140deg, rgba(15, 23, 42, 0.96), rgba(29, 78, 216, 0.92));
  color: white;
  overflow: hidden;
}

.dashboard-kicker {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: fit-content;
  padding: 0.34rem 0.56rem;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.12);
  border: 1px solid rgba(255, 255, 255, 0.12);
  font-size: 0.66rem;
  font-weight: 900;
  letter-spacing: 0.1em;
  text-transform: uppercase;
}

.dashboard-hero h2 {
  margin: 0.75rem 0 0.65rem;
  max-width: 14ch;
  font-size: clamp(1.6rem, 3vw, 2.5rem);
  line-height: 1;
  letter-spacing: -0.05em;
}

.dashboard-hero p {
  margin: 0;
  max-width: 42rem;
  color: rgba(255, 255, 255, 0.78);
  font-size: 0.9rem;
  line-height: 1.65;
  font-weight: 600;
}

.dashboard-hero__metrics {
  margin-top: 1rem;
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 0.8rem;
}

.metric-chip,
.panel-card {
  padding: 0.85rem;
  border-radius: 14px;
  border: 1px solid rgba(255, 255, 255, 0.12);
  background: rgba(255, 255, 255, 0.1);
  backdrop-filter: blur(12px);
}

.metric-chip span,
.panel-card span {
  display: block;
  color: rgba(255, 255, 255, 0.62);
  font-size: 0.66rem;
  font-weight: 800;
  letter-spacing: 0.09em;
  text-transform: uppercase;
}

.metric-chip strong,
.panel-card strong {
  display: block;
  margin-top: 0.45rem;
  font-size: 1.55rem;
  line-height: 1;
  letter-spacing: -0.05em;
}

.metric-chip strong {
  font-size: 1.7rem;
}

.panel-card p {
  margin-top: 0.35rem;
  color: rgba(255, 255, 255, 0.74);
  font-size: 0.8rem;
  line-height: 1.55;
}

.dashboard-hero__panel {
  display: flex;
  flex-direction: column;
  gap: 0.85rem;
}

.panel-card--primary {
  min-height: 148px;
  background: linear-gradient(145deg, rgba(255, 255, 255, 0.18), rgba(255, 255, 255, 0.08));
}

.panel-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 0.85rem;
}

.dashboard-stats {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
}

.stat-card {
  padding: 12px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.stat-left {
  display: flex;
  align-items: center;
  gap: 12px;
  min-width: 0;
}

.stat-badge {
  width: 42px;
  height: 42px;
  display: grid;
  place-items: center;
  border-radius: 14px;
  background: rgba(2, 6, 23, 0.04);
  border: 1px solid rgba(2, 6, 23, 0.06);
  font-size: 18px;
  flex: 0 0 auto;
}

.stat-label {
  font-size: 12px;
  color: rgba(2, 6, 23, 0.6);
  font-weight: 800;
}

.stat-value {
  font-size: 22px;
  font-weight: 900;
  letter-spacing: -0.02em;
  line-height: 1.1;
}

.stat-hint {
  font-size: 12px;
  white-space: nowrap;
}

.section-title {
  font-size: 14px;
  font-weight: 900;
  letter-spacing: -0.01em;
}

.dashboard-grid {
  display: grid;
  grid-template-columns: minmax(0, 1.45fr) minmax(280px, 0.9fr);
  gap: 12px;
}

.action-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
  gap: 12px;
}

.action-tile {
  display: flex;
  flex-direction: column;
  gap: 6px;
  padding: 12px;
  border-radius: 12px;
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
}

.insights-list {
  display: grid;
  gap: 12px;
}

.insight-item {
  padding: 12px;
  border-radius: 12px;
  border: 1px solid rgba(2, 6, 23, 0.08);
  background: rgba(248, 250, 252, 0.9);
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
  .dashboard-hero,
  .dashboard-grid,
  .dashboard-hero__metrics,
  .panel-grid,
  .dashboard-stats {
    grid-template-columns: 1fr;
  }

  .stat-hint {
    display: none;
  }
}
</style>
