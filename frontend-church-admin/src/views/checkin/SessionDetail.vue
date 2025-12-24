<template>
  <AdminLayout>
    <div class="admin-checkin">
      <div class="page-header">
        <h2>{{ isEditMode ? '編輯場次' : '簽到管理' }}</h2>
        <div class="actions">
          <button v-if="isEditMode" class="btn btn-secondary" @click="goBack">返回列表</button>
          <button v-else class="btn btn-secondary" @click="load">重新整理</button>
          <button v-if="!isEditMode" class="btn btn-primary" @click="goManual">補登紀錄</button>
        </div>
      </div>

      <!-- 編輯表單 -->
      <div v-if="isEditMode && showEditForm" class="edit-form-card">
        <h3>場次資訊</h3>
        <form @submit.prevent="saveSession" class="edit-form">
          <div class="form-row">
            <div class="form-group">
              <label>場次代碼 <span class="required">*</span></label>
              <input 
                v-model="editForm.sessionCode" 
                type="text" 
                class="form-input" 
                disabled
                required 
              />
              <div class="form-hint">場次代碼不可變更</div>
            </div>
            <div class="form-group">
              <label>標題 <span class="required">*</span></label>
              <input v-model="editForm.title" type="text" class="form-input" required />
            </div>
          </div>
          <div class="form-row">
            <div class="form-group">
              <label>類型</label>
              <select v-model="editForm.sessionType" class="form-input">
                <option value="">請選擇</option>
                <option value="SATURDAY">週六晚崇</option>
                <option value="SUNDAY">週日早崇</option>
                <option value="WEEKDAY">小組</option>
                <option value="SPECIAL">活動</option>
              </select>
            </div>
            <div class="form-group">
              <label>日期 <span class="required">*</span></label>
              <input v-model="editForm.sessionDate" type="date" class="form-input" required />
            </div>
          </div>
          <div class="form-row">
            <div class="form-group">
              <label>開始時間</label>
              <input v-model="editForm.openAt" type="datetime-local" class="form-input" />
            </div>
            <div class="form-group">
              <label>結束時間</label>
              <input v-model="editForm.closeAt" type="datetime-local" class="form-input" />
            </div>
          </div>
          <div class="form-row">
            <div class="form-group">
              <label>狀態</label>
              <select v-model="editForm.status" class="form-input">
                <option value="DRAFT">草稿</option>
                <option value="ACTIVE">啟用</option>
                <option value="INACTIVE">停用</option>
              </select>
            </div>
          </div>
          <div class="form-actions">
            <button type="submit" class="btn btn-primary" :disabled="saving">
              {{ saving ? '儲存中...' : '儲存' }}
            </button>
          </div>
        </form>
      </div>

      <div v-if="!isEditMode" class="hint">每 5 秒自動更新（穩定、CP 值最高）。</div>

    <div v-if="!isEditMode && sessions.length===0" class="empty">今天沒有場次（或尚未建立）。</div>

    <div v-for="s in sessions" :key="s.id" class="card">
      <div class="row">
        <div>
          <div class="title">{{ s.title || ('Session #' + s.id) }}</div>
          <div class="sub">{{ s.sessionDate }} ｜ {{ s.sessionCode }}</div>
        </div>
        <div class="right">
          <div class="stat">已簽：<strong>{{ stats[s.id]?.checked ?? '-' }}</strong></div>
          <button class="btn small" @click="showQrCode(s)">顯示 QR Code</button>
            <button class="btn small" @click="exportExcel(s.id)">匯出 Excel</button>
        </div>
      </div>

      <div class="row2">
        <div class="box">
          <div class="boxTitle">
            簽到名單
            <label class="show-canceled-checkbox">
              <input type="checkbox" v-model="showCanceled[s.id]" @change="refreshCheckins(s.id)" />
              <span>顯示已取消的補登記錄</span>
            </label>
          </div>
          <table class="table">
            <thead>
                <tr><th>會員</th><th>姓名</th><th>時間</th><th>來源</th><th>裝置</th><th>IP</th><th>操作</th></tr>
            </thead>
            <tbody>
              <tr v-if="!checkins[s.id] || checkins[s.id].length === 0">
                  <td colspan="7" style="text-align:center;opacity:0.6;padding:20px">暫無簽到記錄</td>
              </tr>
                <tr v-for="c in checkins[s.id] || []" :key="c.id" :class="{ canceled: c.canceled }">
                <td>{{ c.memberNo || '-' }}</td>
                <td>{{ c.memberName || '-' }}</td>
                <td>{{ formatDateTime(c.checkedInAt) }}</td>
                  <td>
                    <span v-if="c.canceled" style="opacity:0.6;text-decoration:line-through">{{ c.manual ? ('補登(' + (c.manualBy||'') + ')') : '自助' }}</span>
                    <span v-else>{{ c.manual ? ('補登(' + (c.manualBy||'') + ')') : '自助' }}</span>
                  </td>
                  <td>{{ getDeviceType(c.userAgent) }}</td>
                <td>{{ c.ip || '-' }}</td>
                  <td>
                    <div class="table-actions">
                      <button v-if="c.canceled" class="btn btn-restore small" @click="restoreCheckin(s.id, c.id, c.memberName || c.memberNo)">
                        <span class="btn__icon">
                          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                            <path d="M3 12a9 9 0 0 1 9-9 9.75 9.75 0 0 1 6.74 2.74L21 8"/>
                            <path d="M21 3v5h-5"/>
                            <path d="M21 12a9 9 0 0 1-9 9 9.75 9.75 0 0 1-6.74-2.74L3 16"/>
                            <path d="M3 21v-5h5"/>
                          </svg>
                        </span>
                        <span>恢復</span>
                      </button>
                      <button v-if="c.manual && !c.canceled" class="btn btn-cancel small" @click="cancelCheckin(s.id, c.id, c.memberName || c.memberNo)">
                        <span class="btn__icon">
                          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                            <circle cx="12" cy="12" r="10"/>
                            <path d="M15 9l-6 6"/>
                            <path d="M9 9l6 6"/>
                          </svg>
                        </span>
                        <span>取消</span>
                      </button>
                      <button class="btn btn-delete small" @click="deleteCheckin(s.id, c.id, c.memberName || c.memberNo)">
                        <span class="btn__icon">
                          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                            <polyline points="3 6 5 6 21 6"/>
                            <path d="M19 6l-1 14a2 2 0 0 1-2 2H8a2 2 0 0 1-2-2L5 6"/>
                            <path d="M10 11v6"/>
                            <path d="M14 11v6"/>
                            <path d="M9 6V4a1 1 0 0 1 1-1h4a1 1 0 0 1 1 1v2"/>
                          </svg>
                        </span>
                        <span>刪除</span>
                      </button>
                    </div>
                  </td>
              </tr>
            </tbody>
          </table>
        </div>

        <div class="box">
          <div class="boxTitle">
            <span>同工補登</span>
            <button class="btn btn-primary small" @click="batchManualCheckin(s.id)" :disabled="!hasSelectedPersons(s.id)">
              執行補登
            </button>
          </div>
          <div v-if="uncheckedPersons[s.id] && uncheckedPersons[s.id].length > 0" class="unchecked-list">
            <table class="table unchecked-table">
              <thead>
                <tr>
                  <th style="width: 40px;">
                    <input 
                      type="checkbox" 
                      :checked="isAllSelected(s.id)" 
                      @change="toggleSelectAll(s.id, $event.target.checked)"
                    />
                  </th>
                  <th>會員編號</th>
                  <th>姓名</th>
                  <th>備註</th>
                </tr>
              </thead>
              <tbody>
                <tr 
                  v-for="person in uncheckedPersons[s.id]" 
                  :key="person.id"
                  @click="togglePersonSelection(s.id, person.id, !isPersonSelected(s.id, person.id))"
                  class="unchecked-row"
                >
                  <td @click.stop>
                    <input 
                      type="checkbox" 
                      :checked="isPersonSelected(s.id, person.id)"
                      @change="togglePersonSelection(s.id, person.id, $event.target.checked)"
                    />
                  </td>
                  <td>{{ person.memberNo || '-' }}</td>
                  <td>{{ person.memberName || '-' }}</td>
                  <td @click.stop>
                    <input 
                      type="text" 
                      class="input-small" 
                      v-model="personNotes[s.id][person.id]"
                      placeholder="備註（可空白）"
                      @click.stop
                    />
                  </td>
                </tr>
              </tbody>
            </table>
          </div>
          <div v-else class="empty-unchecked">
            <p>所有人員都已簽到</p>
          </div>
          <div class="hint2">操作人會從你的 JWT 登入身份自動帶入。</div>
        </div>
      </div>
    </div>

    <!-- QR Code 彈窗 -->
    <div v-if="showQrModal" class="modal" @click.self="showQrModal = false">
      <div class="modalContent">
        <div class="modalHeader">
          <h3>簽到 QR Code</h3>
          <button class="closeBtn" @click="showQrModal = false">×</button>
        </div>
        <div class="qrContainer">
          <div class="qrTitle">{{ currentQrSession?.title || '簽到頁面' }}</div>
          <canvas ref="qrCodeRef" class="qrCode"></canvas>
          <div class="qrUrl">{{ currentQrUrl }}</div>
          <div class="qrHint">會眾掃描此 QR Code 即可進入簽到頁面</div>
        </div>
        </div>
      </div>
    </div>
  </AdminLayout>
</template>

<script setup>
import { onBeforeUnmount, ref, nextTick, computed, onMounted } from 'vue'
import { apiRequest } from '@/utils/api'
import { useRouter, useRoute } from 'vue-router'
import QRCode from 'qrcode'
import AdminLayout from '@/components/AdminLayout.vue'
import { toast } from '@/composables/useToast'

const router = useRouter()
const route = useRoute()
const sessions = ref([])
const stats = ref({})
const checkins = ref({})
const currentSession = ref(null)
const showEditForm = ref(false)
const saving = ref(false)

// 判斷是否為編輯模式
const isEditMode = computed(() => !!route.params.id)

// 編輯表單
const editForm = ref({
  sessionCode: '',
  title: '',
  sessionType: '',
  sessionDate: '',
  openAt: '',
  closeAt: '',
  status: 'DRAFT'
})

const showCanceled = ref({}) // 控制是否顯示已取消的補登記錄
const uncheckedPersons = ref({}) // 尚未簽到的人員列表
const selectedPersons = ref({}) // 選中的人員 { sessionId: { personId: true } }
const personNotes = ref({}) // 人員備註 { sessionId: { personId: 'note' } }

const showQrModal = ref(false)
const currentQrSession = ref(null)
const currentQrUrl = ref('')
const qrCodeRef = ref(null)

let timer = null

function goManual(){ router.push('/checkin/admin/manual') }

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

async function load(){
  if (isEditMode.value) {
    // 編輯模式：載入單一場次
    await loadSession()
  } else {
    // 列表模式：載入今天的場次
    try {
      const res = await apiRequest('/church/checkin/admin/sessions/today', {
        method: 'GET'
      }, '載入中...', true)
      const data = await res.json()
      sessions.value = data || []
      toast.success(`查詢成功，共 ${data?.length || 0} 筆場次`, '簽到管理')
      await refreshAll()
    } catch (error) {
      console.error('載入場次失敗:', error)
      toast.error('查詢場次失敗', '簽到管理')
    }
  }
}

async function loadSession() {
  try {
    const sessionId = route.params.id
    const res = await apiRequest(`/church/checkin/admin/sessions/${sessionId}`, {
      method: 'GET'
    }, '載入中...', true)
    currentSession.value = await res.json()
    
    // 填充編輯表單
    if (currentSession.value) {
      editForm.value = {
        sessionCode: currentSession.value.sessionCode || '',
        title: currentSession.value.title || '',
        sessionType: currentSession.value.sessionType || '',
        sessionDate: formatDateForInput(currentSession.value.sessionDate),
        openAt: formatDateTimeForInput(currentSession.value.openAt),
        closeAt: formatDateTimeForInput(currentSession.value.closeAt),
        status: currentSession.value.status || 'DRAFT'
      }
      showEditForm.value = true
      
      // 載入該場次的統計和簽到資料
      sessions.value = [currentSession.value]
      toast.success('查詢場次成功', '場次管理')
      await refreshAll()
    }
  } catch (error) {
    console.error('載入場次失敗:', error)
    toast.error('查詢場次失敗', '場次管理')
    router.push('/checkin/admin/sessions')
  }
}

function formatDateForInput(dateStr) {
  if (!dateStr) return ''
  const d = new Date(dateStr)
  if (isNaN(d.getTime())) return dateStr
  const y = d.getFullYear()
  const m = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  return `${y}-${m}-${day}`
}

function formatDateTimeForInput(dtStr) {
  if (!dtStr) return ''
  const d = new Date(dtStr)
  if (isNaN(d.getTime())) return dtStr
  const y = d.getFullYear()
  const m = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  const h = String(d.getHours()).padStart(2, '0')
  const min = String(d.getMinutes()).padStart(2, '0')
  return `${y}-${m}-${day}T${h}:${min}`
}

async function saveSession() {
  saving.value = true
  try {
    const sessionId = route.params.id
    const payload = {
      sessionCode: editForm.value.sessionCode.trim(),
      title: editForm.value.title.trim(),
      sessionType: editForm.value.sessionType || null,
      sessionDate: editForm.value.sessionDate,
      openAt: editForm.value.openAt || null,
      closeAt: editForm.value.closeAt || null,
      status: editForm.value.status || 'DRAFT'
    }

    await apiRequest(`/church/checkin/admin/sessions/${sessionId}`, {
      method: 'PUT',
      body: JSON.stringify(payload)
    }, '儲存中...', true)
    
    toast.success('場次已更新')
    await loadSession()
  } catch (error) {
    console.error('儲存場次失敗:', error)
    toast.error('儲存場次失敗')
  } finally {
    saving.value = false
  }
}

function goBack() {
  router.push('/checkin/admin/sessions')
}

async function refreshAll(){
  for(const s of sessions.value){
    await refreshCheckins(s.id)
  }
}

async function refreshCheckins(sessionId){
  try {
    const statsRes = await apiRequest(`/church/checkin/admin/sessions/${sessionId}/stats`, {
      method: 'GET'
    }, '', true)
    if(statsRes.ok){
      stats.value[sessionId] = await statsRes.json()
    }
    
    const includeCanceled = showCanceled.value[sessionId] || false
    const checkinsRes = await apiRequest(`/church/checkin/admin/sessions/${sessionId}/checkins?includeCanceled=${includeCanceled}`, {
      method: 'GET'
    }, '', true)
    if(checkinsRes.ok){
      const checkinsData = await checkinsRes.json() || []
      checkins.value[sessionId] = Array.isArray(checkinsData) ? checkinsData : []
      // 查詢成功時不顯示 toast，避免頻繁提示（因為有自動刷新）
    } else {
      console.error(`場次 ${sessionId} 獲取簽到列表失敗:`, checkinsRes.status)
      checkins.value[sessionId] = []
      toast.error('查詢簽到記錄失敗', '簽到管理')
    }
    
    // 載入尚未簽到的人員列表
    await loadUncheckedPersons(sessionId)
  } catch(error) {
    console.error(`場次 ${sessionId} 刷新失敗:`, error)
    checkins.value[sessionId] = []
    toast.error('查詢簽到記錄失敗', '簽到管理')
  }
}

async function loadUncheckedPersons(sessionId) {
  try {
    const res = await apiRequest(`/church/checkin/admin/sessions/${sessionId}/unchecked-persons`, {
      method: 'GET'
    }, '', true)
    if (res.ok) {
      const data = await res.json() || []
      uncheckedPersons.value[sessionId] = data
      // 初始化選中和備註狀態
      if (!selectedPersons.value[sessionId]) {
        selectedPersons.value[sessionId] = {}
      }
      if (!personNotes.value[sessionId]) {
        personNotes.value[sessionId] = {}
      }
    }
  } catch (error) {
    console.error(`載入尚未簽到人員失敗:`, error)
    uncheckedPersons.value[sessionId] = []
  }
}

function isPersonSelected(sessionId, personId) {
  return selectedPersons.value[sessionId]?.[personId] || false
}

function togglePersonSelection(sessionId, personId, checked) {
  if (!selectedPersons.value[sessionId]) {
    selectedPersons.value[sessionId] = {}
  }
  if (checked) {
    selectedPersons.value[sessionId][personId] = true
  } else {
    delete selectedPersons.value[sessionId][personId]
  }
}

function isAllSelected(sessionId) {
  const persons = uncheckedPersons.value[sessionId] || []
  if (persons.length === 0) return false
  return persons.every(p => isPersonSelected(sessionId, p.id))
}

function toggleSelectAll(sessionId, checked) {
  const persons = uncheckedPersons.value[sessionId] || []
  if (!selectedPersons.value[sessionId]) {
    selectedPersons.value[sessionId] = {}
  }
  if (checked) {
    persons.forEach(p => {
      selectedPersons.value[sessionId][p.id] = true
    })
  } else {
    selectedPersons.value[sessionId] = {}
  }
}

function hasSelectedPersons(sessionId) {
  const selected = selectedPersons.value[sessionId] || {}
  return Object.keys(selected).length > 0
}

async function batchManualCheckin(sessionId) {
  const selected = selectedPersons.value[sessionId] || {}
  const selectedIds = Object.keys(selected).map(id => parseInt(id))
  const persons = uncheckedPersons.value[sessionId] || []
  
  const requests = persons
    .filter(p => selectedIds.includes(p.id))
    .map(p => ({
      memberNo: p.memberNo,
      note: personNotes.value[sessionId]?.[p.id] || ''
    }))
  
  if (requests.length === 0) {
    toast.warning('請至少選擇一個人員', '補登')
    return
  }
  
  try {
    await apiRequest(`/church/checkin/admin/sessions/${sessionId}/batch-checkin`, {
      method: 'POST',
      body: JSON.stringify(requests)
    }, '補登中...', true)
    
    toast.success(`成功補登 ${requests.length} 位人員`, '補登')
    
    // 清空選中和備註
    selectedPersons.value[sessionId] = {}
    personNotes.value[sessionId] = {}
    
    // 重新載入數據
    await refreshCheckins(sessionId)
  } catch (error) {
    console.error('批量補登失敗:', error)
    let message = '批量補登失敗'
    if (error.response) {
      try {
        const json = await error.response.json()
        if (json.code === 'BATCH_CHECKIN_PARTIAL_FAILURE') {
          // 部分失敗的情況，顯示詳細錯誤信息
          message = json.message || '批量補登部分失敗，請查看詳情'
          toast.warning(message, '補登', { duration: 0 }) // 錯誤信息不自動消失
        } else {
          message = json.message || '批量補登失敗'
          toast.error(message, '補登')
        }
      } catch (parseError) {
        toast.error('批量補登失敗', '補登')
      }
    } else {
      toast.error('批量補登失敗', '補登')
    }
  }
}

async function exportExcel(sessionId){
  try {
    const res = await apiRequest(`/church/checkin/admin/sessions/${sessionId}/checkins/export.xlsx`, {
      method: 'GET'
    }, '匯出中...', true)
    
    if (!res.ok) {
      const errorData = await res.json().catch(() => ({}))
      throw new Error(errorData.error || '匯出失敗')
    }
    
    const blob = await res.blob()
    const url = window.URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = `session-checkins-${sessionId}-${new Date().toISOString().split('T')[0]}.xlsx`
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

async function restoreCheckin(sessionId, checkinId, memberName){
  const confirmMsg = `確定要恢復「${memberName}」的簽到記錄嗎？`
  if(!window.confirm(confirmMsg)) return
  
  try {
    await apiRequest(`/church/checkin/admin/sessions/${sessionId}/checkins/${checkinId}/restore`, {
      method: 'PATCH'
    }, '恢復中...', true)
    toast.success('簽到記錄已恢復')
    await refreshCheckins(sessionId)
  } catch(error) {
    console.error('恢復簽到記錄失敗:', error)
    toast.error('恢復簽到記錄失敗')
  }
}

async function cancelCheckin(sessionId, checkinId, memberName){
  const confirmMsg = `確定要取消「${memberName}」的簽到記錄嗎？\n取消後記錄仍會保留，但不會計入統計。`
  if(!window.confirm(confirmMsg)) return
  
  const note = window.prompt('取消原因（可空白）：', '') || ''
  
  try {
    await apiRequest(`/church/checkin/admin/sessions/${sessionId}/checkins/${checkinId}/cancel`, {
      method: 'PATCH',
      body: JSON.stringify({ note })
    }, '取消中...', true)
    toast.success('簽到記錄已取消')
    await refreshCheckins(sessionId)
  } catch(error) {
    console.error('取消簽到記錄失敗:', error)
    toast.error('取消簽到記錄失敗')
  }
}

async function deleteCheckin(sessionId, checkinId, memberName){
  const confirmMsg = `確定要刪除「${memberName}」的簽到記錄嗎？\n此操作無法復原。`
  if(!window.confirm(confirmMsg)) return
  
  try {
    await apiRequest(`/church/checkin/admin/sessions/${sessionId}/checkins/${checkinId}`, {
      method: 'DELETE'
    }, '刪除中...', true)
    toast.success('簽到記錄已刪除')
    await refreshCheckins(sessionId)
  } catch(error) {
    console.error('刪除簽到記錄失敗:', error)
    toast.error('刪除簽到記錄失敗')
  }
}

async function showQrCode(session){
  currentQrSession.value = session
  // 生成簽到頁面的完整 URL
  const baseUrl = window.location.origin
  currentQrUrl.value = `${baseUrl}/church-admin/${session.sessionCode}`
  
  showQrModal.value = true
  
  // 等待 DOM 更新後生成 QR Code
  await nextTick()
  const canvas = qrCodeRef.value
  if(canvas && canvas instanceof HTMLCanvasElement){
    // 清空之前的內容
    const ctx = canvas.getContext('2d')
    if(ctx){
      ctx.clearRect(0, 0, canvas.width, canvas.height)
    }
    
    QRCode.toCanvas(canvas, currentQrUrl.value, {
      width: 300,
      margin: 2,
      color: {
        dark: '#000000',
        light: '#FFFFFF'
      }
    }, (error) => {
      if(error){
        console.error('QR Code 生成失敗:', error)
        alert('QR Code 生成失敗，請稍後再試')
      }
    })
  } else {
    console.error('QR Code canvas 元素未找到')
  }
}

async function manualAdd(sessionId){
  const m = (manualMemberNo.value[sessionId] || '').trim().toUpperCase()
  if(!m){ 
    toast.warning('請輸入會員編號', '補登')
    return
  }
  const note = (manualNote.value[sessionId] || '').trim()
  try{
    await apiRequest('/church/checkin/admin/manual-checkins', {
      method: 'POST',
      body: JSON.stringify({ sessionId, memberNo: m, note })
    }, '補登中...', true)
    toast.success('補登成功', '補登')
    manualMemberNo.value[sessionId] = ''
    manualNote.value[sessionId] = ''
    await refreshCheckins(sessionId)
  }catch(e){
    let code = 'FAILED'
    let message = '補登失敗'
    let toastType = 'error'
    if (e.response) {
      try {
        const json = await e.response.json()
        code = json.code || 'FAILED'
        // 如果有後端返回的 message，優先使用
        message = json.message || message
        if (code === 'CANCELED_EXISTS') {
          message = json.message || '此會員的簽到記錄已存在但已被取消，請從簽到名單中恢復簽到狀態。'
          toastType = 'warning' // 使用 warning 類型，因為這不是真正的錯誤，而是提示
        } else if (code === 'ALREADY_CHECKED_IN_MANUAL') {
          message = json.message || '已簽到(補登)'
          toastType = 'info' // 使用 info 類型，因為這只是提示信息
        } else if (code === 'ALREADY_CHECKED_IN') {
          message = json.message || '此會員已簽到（或已補登）'
          toastType = 'warning' // 使用 warning 類型
        } else if (code === 'MEMBER_NOT_FOUND') {
          message = json.message || '查無此會員編號'
          toastType = 'error'
        }
      } catch (parseError) {
        // 如果無法解析 JSON，使用默認錯誤訊息
        console.error('解析錯誤響應失敗:', parseError)
        message = '補登失敗，請稍後再試'
        toastType = 'error'
      }
    } else {
      // 如果沒有 response 對象，顯示通用錯誤訊息
      console.error('補登錯誤:', e)
      message = e.message || '補登失敗，請稍後再試'
      toastType = 'error'
    }
    // 根據類型顯示不同的 toast
    if (toastType === 'warning') {
      toast.warning(message, '補登')
    } else if (toastType === 'info') {
      toast.info(message, '補登')
    } else {
      toast.error(message, '補登')
    }
  }
}

function startPolling(){
  if(timer) clearInterval(timer)
  if (!isEditMode.value) {
    timer = setInterval(async () => {
      try { await refreshAll() } catch {}
    }, 5000)
  }
}

onMounted(() => {
  load().then(startPolling)
})

onBeforeUnmount(() => { if(timer) clearInterval(timer) })
</script>

<style scoped>
.admin-checkin {
  width: 100%;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.page-header h2 {
  margin: 0;
  font-size: 24px;
  font-weight: 700;
  color: var(--text);
}

.actions {
  display: flex;
  gap: 10px;
}

.hint {
  opacity: 0.8;
  margin: 8px 0 16px 0;
  font-size: 14px;
  color: var(--text-muted);
}

.empty {
  opacity: 0.8;
  padding: 20px;
  border: 1px dashed var(--border);
  border-radius: 12px;
  text-align: center;
  color: var(--text-muted);
}

.card {
  margin: 16px 0;
  padding: 20px;
  border: 1px solid var(--border);
  border-radius: 16px;
  background: var(--card-bg);
}

.row {
  display: flex;
  justify-content: space-between;
  gap: 14px;
  align-items: flex-start;
  margin-bottom: 12px;
}

.title {
  font-size: 18px;
  font-weight: 700;
  color: var(--text);
}

.sub {
  opacity: 0.8;
  font-size: 13px;
  margin-top: 4px;
  color: var(--text-muted);
}

.right {
  text-align: right;
  display: flex;
  align-items: center;
  gap: 10px;
}

.stat {
  margin-bottom: 8px;
  font-size: 14px;
  color: var(--text);
}

.row2 {
  display: grid;
  grid-template-columns: 1.7fr 1fr;
  gap: 16px;
  margin-top: 16px;
}

.box {
  border: 1px solid var(--border);
  border-radius: 14px;
  padding: 16px;
  background: var(--card-bg);
}

.boxTitle {
  font-weight: 700;
  margin-bottom: 12px;
  font-size: 16px;
  color: var(--text);
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.show-canceled-checkbox {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 14px;
  font-weight: 400;
  cursor: pointer;
}

.show-canceled-checkbox input[type="checkbox"] {
  cursor: pointer;
}

.table {
  width: 100%;
  border-collapse: collapse;
}

.table th,
.table td {
  border-bottom: 1px solid var(--border);
  padding: 10px 8px;
  font-size: 13px;
  text-align: left;
}

.table th {
  font-weight: 600;
  color: var(--text);
  background: var(--bg);
}

.table td {
  color: var(--text);
}

.input {
  width: 100%;
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

.form-input {
  width: 100%;
  padding: 10px 12px;
  border: 1px solid var(--border);
  border-radius: 10px;
  background: var(--input-bg);
  color: var(--text);
  font-size: 14px;
}

.form-input:focus {
  outline: none;
  border-color: var(--primary);
}

.form-input:disabled {
  opacity: 0.6;
  cursor: not-allowed;
  background: var(--bg);
}

.form-hint {
  margin-top: 4px;
  font-size: 12px;
  opacity: 0.7;
  color: var(--text-muted);
}

.formRow {
  margin-bottom: 12px;
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

.btn-cancel {
  background: rgba(251, 191, 36, 0.1);
  color: #fbbf24;
  border-color: rgba(251, 191, 36, 0.2);
}

.btn-cancel:hover {
  background: rgba(251, 191, 36, 0.15);
}

.btn-restore {
  background: rgba(34, 197, 94, 0.1);
  color: #22c55e;
  border-color: rgba(34, 197, 94, 0.3);
}

.btn-restore:hover {
  background: rgba(34, 197, 94, 0.15);
}

.table-actions {
  display: flex;
  gap: 8px;
}

.canceled {
  opacity: 0.6;
}

.msg {
  margin-top: 10px;
  opacity: 0.9;
  font-size: 14px;
}

.hint2 {
  opacity: 0.75;
  margin-top: 10px;
  font-size: 12px;
  color: var(--text-muted);
}

.unchecked-list {
  margin-top: 12px;
  max-height: 400px;
  overflow-y: auto;
}

.unchecked-table {
  width: 100%;
  border-collapse: collapse;
}

.unchecked-table th,
.unchecked-table td {
  padding: 8px;
  text-align: left;
  border-bottom: 1px solid var(--border);
  font-size: 13px;
}

.unchecked-table th {
  font-weight: 600;
  background: var(--bg);
  position: sticky;
  top: 0;
  z-index: 1;
}

.unchecked-table tbody tr:hover {
  background: rgba(255, 255, 255, 0.05);
}

.input-small {
  width: 100%;
  padding: 6px 8px;
  border: 1px solid var(--border);
  border-radius: 6px;
  background: var(--input-bg);
  color: var(--text);
  font-size: 13px;
}

.input-small:focus {
  outline: none;
  border-color: var(--primary);
}

.empty-unchecked {
  text-align: center;
  padding: 40px 20px;
  opacity: 0.6;
  color: var(--text-muted);
}

.empty-unchecked p {
  margin: 0;
  font-size: 14px;
}

.unchecked-row {
  cursor: pointer;
  transition: background-color 0.2s ease;
}

.unchecked-row:hover {
  background: rgba(255, 255, 255, 0.08);
}

.unchecked-row td {
  user-select: none;
}

.edit-form-card {
  background: var(--card-bg);
  border: 1px solid var(--border);
  border-radius: 16px;
  padding: 24px;
  margin-bottom: 24px;
}

.edit-form-card h3 {
  margin: 0 0 20px 0;
  font-size: 18px;
  font-weight: 700;
  color: var(--text);
}

.edit-form {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.form-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
}

.form-group {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.form-group label {
  font-size: 14px;
  font-weight: 600;
  color: var(--text);
}

.required {
  color: #ef4444;
}

.form-actions {
  display: flex;
  justify-content: flex-end;
  margin-top: 8px;
}

/* QR Code 彈窗樣式 */
.modal {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.7);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
}

.modalContent {
  background: var(--card-bg);
  border: 1px solid var(--border);
  border-radius: 16px;
  padding: 24px;
  max-width: 400px;
  width: 90%;
}

.modalHeader {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.modalHeader h3 {
  margin: 0;
  font-size: 20px;
  color: var(--text);
}

.closeBtn {
  background: none;
  border: none;
  color: var(--text);
  font-size: 32px;
  cursor: pointer;
  line-height: 1;
  opacity: 0.7;
  transition: opacity 0.2s ease;
}

.closeBtn:hover {
  opacity: 1;
}

.qrContainer {
  text-align: center;
}

.qrTitle {
  font-size: 16px;
  font-weight: 600;
  margin-bottom: 16px;
  color: var(--text);
}

.qrCode {
  display: block;
  margin: 20px auto;
  padding: 16px;
  background: #fff;
  border-radius: 12px;
  max-width: 100%;
}

.qrUrl {
  margin-top: 16px;
  padding: 10px;
  background: var(--bg);
  border-radius: 8px;
  font-size: 12px;
  word-break: break-all;
  opacity: 0.8;
  color: var(--text);
}

.qrHint {
  margin-top: 12px;
  opacity: 0.7;
  font-size: 13px;
  color: var(--text-muted);
}
</style>

