<template>
  <AdminLayout>
    <div class="admin-page" data-ui="admin-v1">
      <div class="container container--admin">
        <div class="page-head">
          <div>
            <div class="page-title">教會管理系統</div>
            <div class="page-desc">歡迎使用教會管理系統</div>
          </div>
        </div>

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

        <div class="card">
          <div class="card__head">
            <div>
              <div class="section-title">快速操作</div>
              <div class="muted">常用功能快速入口</div>
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
      </div>
    </div>
  </AdminLayout>
</template>

<script setup>
import AdminLayout from '@/components/AdminLayout.vue'
import { ref, onMounted } from 'vue'
import { apiRequest } from '@/utils/api'

const scheduleCount = ref(0)
const personCount = ref(0)
const positionCount = ref(0)
const quickActions = ref([])

const loadStats = async () => {
  try {
    // 載入統計數據
    const [schedulesRes, personsRes, positionsRes] = await Promise.all([
      apiRequest('/church/service-schedules', { method: 'GET', credentials: 'include' }),
      apiRequest('/church/persons', { method: 'GET', credentials: 'include' }),
      apiRequest('/church/positions', { method: 'GET', credentials: 'include' })
    ])
    
    if (schedulesRes.ok) {
      const schedules = await schedulesRes.json()
      // 後端可能返回數組或物件
      scheduleCount.value = Array.isArray(schedules) ? schedules.length : (schedules.length || 0)
    }
    
    if (personsRes.ok) {
      const data = await personsRes.json()
      // 後端返回格式：{ "persons": [...], "message": "..." }
      const persons = data.persons || data
      personCount.value = Array.isArray(persons) ? persons.length : 0
    }
    
    if (positionsRes.ok) {
      const data = await positionsRes.json()
      // 後端返回格式：{ "positions": [...], "message": "..." }
      const positions = data.positions || data
      positionCount.value = Array.isArray(positions) ? positions.length : 0
    }
  } catch (error) {
    console.error('載入統計數據失敗:', error)
  }
}

const loadQuickActions = async () => {
  try {
    const response = await apiRequest('/church/menus/dashboard', {
      method: 'GET',
      credentials: 'include'
    })
    
    if (response.ok) {
      quickActions.value = await response.json()
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
.dashboard-stats{
  display:grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap:12px;
}
.stat-card{
  padding:14px 14px;
  display:flex;
  align-items:center;
  justify-content:space-between;
  gap:12px;
}
.stat-left{display:flex; align-items:center; gap:12px; min-width:0}
.stat-badge{
  width:42px;
  height:42px;
  display:grid;
  place-items:center;
  border-radius:14px;
  background:rgba(2,6,23,.04);
  border:1px solid rgba(2,6,23,.06);
  font-size:18px;
  flex:0 0 auto;
}
.stat-label{font-size:12px;color:rgba(2,6,23,.60);font-weight:800}
.stat-value{font-size:26px;font-weight:900;letter-spacing:-0.02em;line-height:1.1}
.stat-hint{font-size:12px;white-space:nowrap}

.section-title{font-size:14px;font-weight:900;letter-spacing:-0.01em}

.action-grid{
  display:grid;
  grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
  gap:12px;
}
.action-tile{
  display:flex;
  flex-direction:column;
  gap:6px;
  padding:14px;
  border-radius:16px;
  border:1px solid rgba(2,6,23,.10);
  background:rgba(255,255,255,.85);
  text-decoration:none;
  color:inherit;
  transition:transform .12s ease, box-shadow .12s ease;
}
.action-tile:hover{transform:translateY(-1px); box-shadow:var(--shadow-sm)}
.tile-icon{
  width:38px;
  height:38px;
  display:grid;
  place-items:center;
  border-radius:14px;
  background:rgba(2,6,23,.04);
  border:1px solid rgba(2,6,23,.06);
  font-size:18px;
}
.tile-title{font-weight:900;letter-spacing:-0.01em}
.tile-desc{font-size:12px;color:rgba(2,6,23,.60);font-weight:700;line-height:1.3}

@media (max-width: 900px){
  .dashboard-stats{grid-template-columns:1fr}
  .stat-hint{display:none}
}
</style>
