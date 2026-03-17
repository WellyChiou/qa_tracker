<template>
  <AdminLayout>
    <div class="admin-checkin-manual">
      <div class="page-header">
        <div>
          <h2>補登稽核</h2>
          <p>集中檢視人工補登、取消與匯出記錄，保留完整稽核脈絡供後台追查。</p>
        </div>
        <div class="actions">
          <button class="btn btn-secondary" @click="back">返回後台</button>
          <button class="btn btn-primary" @click="exportExcel">匯出 Excel</button>
        </div>
      </div>

    <section class="overview-strip">
      <article class="overview-card overview-card--accent">
        <span>補登筆數</span>
        <strong>{{ rows.length }}</strong>
        <p>目前條件下列出的補登與取消稽核記錄。</p>
      </article>
      <article class="overview-card">
        <span>查詢區間</span>
        <strong>{{ from || '-' }}</strong>
        <p>{{ to ? `至 ${to}` : '未設定結束日期' }}</p>
      </article>
      <article class="overview-card">
        <span>稽核模式</span>
        <strong>{{ includeCanceled ? '含取消' : '有效記錄' }}</strong>
        <p>取消補登採軟取消，不會直接刪除原始資料。</p>
      </article>
    </section>

    <div class="filters card surface-card">
      <input class="input" v-model="q" placeholder="搜尋：姓名 / 會員編號 / 場次" @keyup.enter="load"/>
      <div class="dates">
        <input class="input" type="date" v-model="from" />
        <input class="input" type="date" v-model="to" />
      </div>
      <label class="chk"><input type="checkbox" v-model="includeCanceled" /> 顯示已取消</label>
      <button class="btn" @click="load">搜尋</button>
    </div>

    <div class="hint">取消補登採「軟取消」保留稽核：不會真的刪資料。</div>

    <div class="card surface-card">
    <table class="table">
      <thead>
        <tr>
          <th>場次</th>
          <th>會員</th>
          <th>姓名</th>
          <th>補登時間</th>
          <th>操作人</th>
          <th>備註</th>
          <th>裝置</th>
          <th>IP</th>
          <th>狀態</th>
          <th v-if="showCanceledTime">取消時間</th>
          <th></th>
        </tr>
      </thead>
      <tbody>
        <tr v-if="rows.length === 0">
          <td :colspan="showCanceledTime ? 11 : 10" style="text-align:center;opacity:0.6;padding:20px">暫無補登記錄</td>
        </tr>
        <tr v-for="r in rows" :key="r.id" :class="{ canceled: r.canceled }">
          <td>
            <div class="t">{{ r.sessionTitle }}</div>
            <div class="s">{{ r.sessionDate }}</div>
          </td>
          <td>{{ r.memberNo }}</td>
          <td>{{ r.memberName }}</td>
          <td>{{ formatDateTime(r.checkedInAt) }}</td>
          <td>{{ r.manualBy }}</td>
          <td class="note" :title="r.manualNote">{{ r.manualNote }}</td>
          <td>{{ getDeviceType(r.userAgent) }}</td>
          <td>{{ r.ip }}</td>
          <td>
            <span v-if="r.canceled">已取消</span>
            <span v-else>有效</span>
          </td>
          <td v-if="showCanceledTime">
            <span v-if="r.canceled && r.canceledAt">{{ formatDateTime(r.canceledAt) }}</span>
            <span v-else-if="r.canceled">-</span>
            <span v-else>-</span>
          </td>
          <td>
            <button class="btn small danger" v-if="!r.canceled" @click="cancel(r)">取消</button>
          </td>
        </tr>
      </tbody>
    </table>
    </div>
    </div>
  </AdminLayout>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { apiRequest } from '@/utils/api'
import { useRouter } from 'vue-router'
import AdminLayout from '@/components/AdminLayout.vue'
import { toast } from '@shared/composables/useToast'

const router = useRouter()
const rows = ref([])
const q = ref('')
const from = ref('')
const to = ref('')
const includeCanceled = ref(false)

// 根據查詢結果判斷是否顯示取消時間欄位（只有當結果中有已取消的記錄時才顯示）
const showCanceledTime = computed(() => {
  return rows.value.some(r => r.canceled)
})

function formatDateTime(dt){
  if(!dt) return '-'
  const d = new Date(dt)
  if(isNaN(d.getTime())) return dt
  const y = d.getFullYear()
  const m = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  const h = String(d.getHours()).padStart(2, '0')
  const min = String(d.getMinutes()).padStart(2, '0')
  const sec = String(d.getSeconds()).padStart(2, '0')
  return `${y}-${m}-${day} ${h}:${min}:${sec}`
}

function getDeviceType(userAgent) {
  if (!userAgent) return '-'
  
  const ua = userAgent.toLowerCase()
  
  // 手機
  if (/mobile|android|iphone|ipod|blackberry|iemobile|opera mini/i.test(ua)) {
    if (/iphone|ipod/i.test(ua)) return '📱 iPhone'
    if (/android/i.test(ua)) return '📱 Android'
    if (/ipad/i.test(ua)) return '📱 iPad'
    return '📱 手機'
  }
  
  // 平板
  if (/tablet|ipad|playbook|silk/i.test(ua)) {
    if (/ipad/i.test(ua)) return '📱 iPad'
    return '📱 平板'
  }
  
  // 電腦 - 識別作業系統
  if (/windows/i.test(ua)) {
    return '💻 電腦(Windows)'
  }
  
  if (/macintosh|mac os x|mac_powerpc/i.test(ua)) {
    return '💻 電腦(Mac)'
  }
  
  if (/linux/i.test(ua)) {
    return '💻 電腦(Linux)'
  }
  
  return '❓ 未知'
}

function back(){ router.push('/checkin/admin/sessions') }

// 計算上週一的日期
function getLastMonday() {
  const today = new Date()
  const dayOfWeek = today.getDay() // 0 = 星期日, 1 = 星期一, ..., 6 = 星期六
  // 計算上週一：今天往前推 (dayOfWeek + 6) 天
  const daysToSubtract = dayOfWeek === 0 ? 6 : dayOfWeek + 6
  const lastMonday = new Date(today)
  lastMonday.setDate(today.getDate() - daysToSubtract)
  return lastMonday
}

// 格式化日期為 YYYY-MM-DD
function formatDateForInput(date) {
  const y = date.getFullYear()
  const m = String(date.getMonth() + 1).padStart(2, '0')
  const d = String(date.getDate()).padStart(2, '0')
  return `${y}-${m}-${d}`
}

// 初始化預設日期
function initDefaultDates() {
  const lastMonday = getLastMonday()
  const today = new Date()
  
  from.value = formatDateForInput(lastMonday)
  to.value = formatDateForInput(today)
}

async function load(){
  try {
    const params = new URLSearchParams()
    if(q.value) params.set('q', q.value)
    if(from.value) params.set('from', from.value)
    if(to.value) params.set('to', to.value)
    if(includeCanceled.value) params.set('includeCanceled', 'true')
    
    // apiRequest 現在會自動返回解析後的資料
    const data = await apiRequest(`/church/checkin/admin/manual-checkins?${params.toString()}`, {
      method: 'GET'
    })
    
    if (data) {
      rows.value = Array.isArray(data) ? data : (data.content || [])
      toast.success(`查詢成功，共 ${rows.value.length} 筆補登記錄`, '補登稽核')
    } else {
      rows.value = []
    }
  } catch (error) {
    console.error('查詢補登記錄失敗:', error)
    toast.error('查詢補登記錄失敗', '補登稽核')
    rows.value = []
  }
}

async function exportExcel(){
  try {
    const params = new URLSearchParams()
    if(q.value) params.set('q', q.value)
    if(from.value) params.set('from', from.value)
    if(to.value) params.set('to', to.value)
    if(includeCanceled.value) params.set('includeCanceled', 'true')
    
    // 匯出 Excel 需要直接使用 fetch，因為返回的是 blob，不是 JSON
    const { getApiBaseUrl, getAccessToken } = await import('@/utils/api')
    const apiUrl = `${getApiBaseUrl()}/church/checkin/admin/manual-checkins/export.xlsx?${params.toString()}`
    const token = getAccessToken()
    const headers = {}
    if (token) {
      headers['Authorization'] = `Bearer ${token}`
    }
    
    const res = await fetch(apiUrl, {
      method: 'GET',
      headers,
      credentials: 'include'
    })
    
    if (!res.ok) {
      const errorData = await res.json().catch(() => ({}))
      throw new Error(errorData.error || '匯出失敗')
    }
    
    const blob = await res.blob()
    const url = window.URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = `manual-checkins-${new Date().toISOString().split('T')[0]}.xlsx`
    document.body.appendChild(a)
    a.click()
    document.body.removeChild(a)
    window.URL.revokeObjectURL(url)
    toast.success('Excel 匯出成功')
  } catch (error) {
    console.error('匯出 Excel 失敗:', error)
    toast.error('匯出 Excel 失敗: ' + (error.message || '未知錯誤'))
  }
}

async function cancel(row){
  const ok = window.confirm(`確定取消補登？\n${row.memberName} (${row.memberNo})\n場次：${row.sessionTitle}`)
  if(!ok) return
  const note = window.prompt('取消原因（可空白）：', '') || ''
  try {
    await apiRequest(`/church/checkin/admin/manual-checkins/${row.id}/cancel`, {
      method: 'PATCH',
      body: JSON.stringify({ note })
    }, '取消中...', true)
    toast.success('補登記錄已取消', '補登稽核')
    await load()
  } catch (error) {
    console.error('取消補登記錄失敗:', error)
    toast.error('取消補登記錄失敗', '補登稽核')
  }
}

// 組件載入時初始化預設日期並執行查詢
onMounted(() => {
  initDefaultDates()
  load()
})
</script>

<style scoped>
.admin-checkin-manual {
  width: 100%;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 16px;
  margin-bottom: 24px;
}

.page-header h2 {
  margin: 0;
  font-size: 24px;
  font-weight: 700;
  color: var(--text);
}

.page-header p {
  margin: 8px 0 0;
  max-width: 680px;
  color: var(--text-muted);
  line-height: 1.6;
}

.overview-strip {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 16px;
  margin-bottom: 24px;
}

.overview-card {
  padding: 20px;
  border-radius: 18px;
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.96), rgba(248, 250, 252, 0.92));
  border: 1px solid rgba(148, 163, 184, 0.2);
  box-shadow: 0 18px 40px rgba(15, 23, 42, 0.06);
}

.overview-card span {
  display: block;
  font-size: 13px;
  font-weight: 700;
  letter-spacing: 0.08em;
  color: var(--text-muted);
}

.overview-card strong {
  display: block;
  margin-top: 10px;
  font-size: 32px;
  line-height: 1;
  color: var(--text);
}

.overview-card p {
  margin: 12px 0 0;
  color: var(--text-muted);
  line-height: 1.55;
}

.overview-card--accent {
  background: linear-gradient(135deg, rgba(15, 118, 110, 0.92), rgba(21, 128, 61, 0.92));
  border-color: transparent;
}

.overview-card--accent span,
.overview-card--accent strong,
.overview-card--accent p {
  color: #f8fafc;
}

.surface-card {
  border: 1px solid rgba(148, 163, 184, 0.2);
  border-radius: 22px;
  background: rgba(255, 255, 255, 0.92);
  box-shadow: 0 24px 50px rgba(15, 23, 42, 0.08);
}

.actions {
  display: flex;
  gap: 10px;
}

.filters {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
  margin: 16px 0;
  padding: 18px;
}

.dates {
  display: flex;
  gap: 10px;
}

.input {
  padding: 10px 12px;
  border: 1px solid var(--border);
  border-radius: 10px;
  background: var(--input-bg);
  color: var(--text);
  font-size: 14px;
}

.input:focus {
  outline: none;
  border-color: var(--primary);
}

.chk {
  display: flex;
  align-items: center;
  gap: 6px;
  opacity: 0.85;
  color: var(--text);
}

.hint {
  opacity: 0.8;
  margin: 8px 0 16px 0;
  font-size: 14px;
  color: var(--text-muted);
}

.table {
  width: 100%;
  border-collapse: collapse;
  background: transparent;
  border: 0;
  border-radius: 20px;
  overflow: hidden;
}

.table th,
.table td {
  border-bottom: 1px solid var(--border);
  padding: 12px 10px;
  font-size: 13px;
  text-align: left;
  vertical-align: top;
}

.table th {
  font-weight: 600;
  color: var(--text);
  background: var(--bg);
}

.table td {
  color: var(--text);
}

.t {
  font-weight: 700;
  color: var(--text);
}

.s {
  opacity: 0.75;
  font-size: 12px;
  margin-top: 4px;
  color: var(--text-muted);
}

.note {
  max-width: 260px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.btn {
  padding: 10px 16px;
  border-radius: 10px;
  border: 1px solid var(--border);
  background: var(--btn-bg);
  color: var(--text);
  cursor: pointer;
  font-size: 14px;
  font-weight: 600;
  transition: all 0.2s ease;
}

.btn:hover {
  background: var(--btn-hover-bg);
  transform: translateY(-1px);
}

.btn-primary {
  background: var(--primary);
  color: white;
  border-color: var(--primary);
}

.btn-primary:hover {
  background: var(--primary-hover);
}

.btn-secondary {
  background: var(--bg);
  color: var(--text);
}

.btn.small {
  padding: 6px 12px;
  font-size: 13px;
}

.btn.danger {
  border-color: #e03131;
  color: #e03131;
}

.btn.danger:hover {
  background: rgba(224, 49, 49, 0.1);
}

.canceled {
  opacity: 0.55;
}

@media (max-width: 900px) {
  .page-header {
    flex-direction: column;
    align-items: stretch;
  }

  .actions,
  .dates {
    width: 100%;
  }

  .overview-strip {
    grid-template-columns: 1fr;
  }
}
</style>
