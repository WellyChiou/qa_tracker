<template>
  <AdminLayout>
    <div class="admin-dashboard">
    <div class="dashboard-header">
      <h1>教會管理系統</h1>
      <p>歡迎使用教會管理系統</p>
    </div>
    
    <div class="dashboard-stats">
      <div class="stat-card">
        <div class="stat-icon">📋</div>
        <div class="stat-info">
          <div class="stat-value">{{ scheduleCount }}</div>
          <div class="stat-label">服事表</div>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon">👥</div>
        <div class="stat-info">
          <div class="stat-value">{{ personCount }}</div>
          <div class="stat-label">人員</div>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon">🎯</div>
        <div class="stat-info">
          <div class="stat-value">{{ positionCount }}</div>
          <div class="stat-label">崗位</div>
        </div>
      </div>
    </div>

    <div class="dashboard-actions">
      <h2>快速操作</h2>
      <div class="action-grid">
        <router-link to="/admin/service-schedule" class="action-card">
          <div class="action-icon">📋</div>
          <div class="action-title">服事表管理</div>
          <div class="action-desc">新增、編輯、刪除服事表</div>
        </router-link>
        <router-link to="/admin/persons" class="action-card">
          <div class="action-icon">👥</div>
          <div class="action-title">人員管理</div>
          <div class="action-desc">管理教會人員資訊</div>
        </router-link>
        <router-link to="/admin/positions" class="action-card">
          <div class="action-icon">🎯</div>
          <div class="action-title">崗位管理</div>
          <div class="action-desc">管理服事崗位配置</div>
        </router-link>
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

onMounted(() => {
  loadStats()
})
</script>

<style scoped>
.admin-dashboard {
  max-width: 1200px;
  margin: 0 auto;
}

.dashboard-header {
  margin-bottom: 2rem;
}

.dashboard-header h1 {
  font-size: 2rem;
  color: #333;
  margin-bottom: 0.5rem;
}

.dashboard-header p {
  color: #666;
  font-size: 1.1rem;
}

.dashboard-stats {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 1.5rem;
  margin-bottom: 3rem;
}

.stat-card {
  background: white;
  border-radius: 12px;
  padding: 1.5rem;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  display: flex;
  align-items: center;
  gap: 1rem;
}

.stat-icon {
  font-size: 3rem;
}

.stat-info {
  flex: 1;
}

.stat-value {
  font-size: 2rem;
  font-weight: 700;
  color: #333;
}

.stat-label {
  color: #666;
  font-size: 0.9rem;
}

.dashboard-actions h2 {
  font-size: 1.5rem;
  color: #333;
  margin-bottom: 1.5rem;
}

.action-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(250px, 1fr));
  gap: 1.5rem;
}

.action-card {
  background: white;
  border-radius: 12px;
  padding: 2rem;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  text-decoration: none;
  color: inherit;
  transition: transform 0.2s, box-shadow 0.2s;
  display: flex;
  flex-direction: column;
  align-items: center;
  text-align: center;
}

.action-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.15);
}

.action-icon {
  font-size: 3rem;
  margin-bottom: 1rem;
}

.action-title {
  font-size: 1.2rem;
  font-weight: 600;
  color: #333;
  margin-bottom: 0.5rem;
}

.action-desc {
  color: #666;
  font-size: 0.9rem;
}
</style>

