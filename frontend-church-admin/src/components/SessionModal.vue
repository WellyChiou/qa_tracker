<template>
  <div v-if="show" class="modal-overlay" @click.self="close">
    <div class="modal-content">
      <div class="modal-header">
        <h3>{{ editingSession ? '編輯場次' : '新增場次' }}</h3>
        <button @click="close" class="close-btn">×</button>
      </div>

      <form @submit.prevent="save" class="modal-body">
        <div class="form-group">
          <label>場次代碼 <span class="required">*</span></label>
          <input
            v-model="form.sessionCode"
            type="text"
            class="form-input"
            placeholder="系統自動生成 UUID"
            disabled
            required
          />
          <div class="form-hint">場次代碼由系統自動生成，無法變更</div>
        </div>

        <div class="form-group">
          <label>標題 <span class="required">*</span></label>
          <input
            v-model="form.title"
            type="text"
            class="form-input"
            placeholder="例如：主日崇拜"
            required
          />
        </div>

        <div class="form-group">
          <label>類型</label>
          <select v-model="form.sessionType" class="form-input" @change="onSessionTypeChange">
            <option value="">請選擇</option>
            <option value="SATURDAY">週六晚崇</option>
            <option value="SUNDAY">週日早崇</option>
            <option value="WEEKDAY">小組</option>
            <option value="SPECIAL">活動</option>
          </select>
        </div>

        <div class="form-group" v-if="form.sessionType === 'WEEKDAY'">
          <label>參與小組</label>
          <select v-model="form.groupIds" multiple class="form-input" style="min-height: 120px;">
            <option v-for="group in activeGroups" :key="group.id" :value="group.id">
              {{ group.groupName }}
            </option>
          </select>
          <div class="form-hint">可選擇多個小組（支援聯合小組），按住 Ctrl (Windows) 或 Cmd (Mac) 進行多選</div>
        </div>

        <div class="form-group">
          <label>日期 <span class="required">*</span></label>
          <input
            v-model="form.sessionDate"
            type="date"
            class="form-input"
            required
          />
        </div>

        <div class="form-row">
          <div class="form-group">
            <label>開始時間</label>
            <input
              v-model="form.openAt"
              type="datetime-local"
              class="form-input"
            />
          </div>

          <div class="form-group">
            <label>結束時間</label>
            <input
              v-model="form.closeAt"
              type="datetime-local"
              class="form-input"
            />
          </div>
        </div>

        <div class="form-group">
          <label>狀態</label>
          <select v-model="form.status" class="form-input">
            <option value="DRAFT">草稿</option>
            <option value="ACTIVE">啟用</option>
            <option value="INACTIVE">停用</option>
          </select>
        </div>

        <div class="modal-footer">
          <button type="button" @click="close" class="btn btn-secondary">取消</button>
          <button type="submit" class="btn btn-primary" :disabled="saving">
            {{ saving ? '儲存中...' : '儲存' }}
          </button>
        </div>
      </form>
    </div>
  </div>
</template>

<script setup>
import { ref, watch, computed, onMounted } from 'vue'
import { toast } from '@shared/composables/useToast'
import { apiRequest } from '@/utils/api'

const props = defineProps({
  show: {
    type: Boolean,
    default: false
  },
  session: {
    type: Object,
    default: null
  }
})

const emit = defineEmits(['close', 'saved'])

const form = ref({
  sessionCode: '',
  title: '',
  sessionType: '',
  sessionDate: '',
  openAt: '',
  closeAt: '',
  status: 'DRAFT',
  groupIds: []
})

const saving = ref(false)
const activeGroups = ref([])

// 生成 UUID 作為 session code（不帶連字符，URL 友好）
function generateUUID() {
  return 'xxxxxxxxxxxx4xxxyxxxxxxxxxxxxxxx'.replace(/[xy]/g, function(c) {
    const r = Math.random() * 16 | 0
    const v = c === 'x' ? r : (r & 0x3 | 0x8)
    return v.toString(16).toUpperCase()
  })
}

function generateSessionCode() {
  form.value.sessionCode = generateUUID()
}

const loadActiveGroups = async () => {
  try {
    const response = await apiRequest('/church/groups/active', {
      method: 'GET',
      credentials: 'include'
    })
    
    if (response.ok) {
      const data = await response.json()
      activeGroups.value = data.groups || []
    }
  } catch (error) {
    console.error('載入小組列表失敗:', error)
  }
}

const loadSessionGroups = async (sessionId) => {
  try {
    const response = await apiRequest(`/church/checkin/admin/sessions/${sessionId}/groups`, {
      method: 'GET',
      credentials: 'include'
    })
    
    if (response.ok) {
      const data = await response.json()
      form.value.groupIds = (data.groups || []).map(g => g.id)
    }
  } catch (error) {
    console.error('載入場次關聯的小組失敗:', error)
  }
}

const onSessionTypeChange = () => {
  if (form.value.sessionType !== 'WEEKDAY') {
    form.value.groupIds = []
  }
}

watch(() => props.session, async (newSession) => {
  if (newSession) {
    // 編輯模式：載入現有資料
    form.value = {
      sessionCode: newSession.sessionCode || '',
      title: newSession.title || '',
      sessionType: newSession.sessionType || '',
      sessionDate: newSession.sessionDate ? formatDateForInput(newSession.sessionDate) : '',
      openAt: newSession.openAt ? formatDateTimeForInput(newSession.openAt) : '',
      closeAt: newSession.closeAt ? formatDateTimeForInput(newSession.closeAt) : '',
      status: newSession.status || 'DRAFT',
      groupIds: []
    }
    // 載入場次關聯的小組
    if (newSession.id) {
      await loadSessionGroups(newSession.id)
    }
  } else if (props.show) {
    // 新增模式：自動生成 UUID
    resetForm()
  }
}, { immediate: true })

watch(() => props.show, (newVal) => {
  if (newVal) {
    loadActiveGroups()
    if (!props.session) {
      // 打開新增模式時，自動生成 session code
      resetForm()
    }
  } else if (!newVal) {
    // 關閉時重置表單
    resetForm()
  }
})

onMounted(() => {
  loadActiveGroups()
})

const editingSession = computed(() => props.session)

function resetForm() {
  form.value = {
    sessionCode: generateUUID(), // 新增時自動生成 UUID
    title: '',
    sessionType: '',
    sessionDate: '',
    openAt: '',
    closeAt: '',
    status: 'DRAFT',
    groupIds: []
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

async function save() {
  saving.value = true
  try {
    const payload = {
      sessionCode: form.value.sessionCode.trim(),
      title: form.value.title.trim(),
      sessionType: form.value.sessionType || null,
      sessionDate: form.value.sessionDate,
      openAt: form.value.openAt || null,
      closeAt: form.value.closeAt || null,
      status: form.value.status || 'DRAFT'
    }

    let sessionId
    if (props.session && props.session.id) {
      // 更新
      const response = await apiRequest(`/church/checkin/admin/sessions/${props.session.id}`, {
        method: 'PUT',
        body: JSON.stringify(payload)
      }, '儲存中...', true)
      sessionId = props.session.id
      toast.success('場次已更新')
    } else {
      // 新增
      const response = await apiRequest('/church/checkin/admin/sessions', {
        method: 'POST',
        body: JSON.stringify(payload)
      }, '儲存中...', true)
      
      if (response.ok) {
        const data = await response.json()
        // 後端返回的是 Session 對象，直接包含 id
        sessionId = data.id
        if (!sessionId) {
          console.error('新增場次成功但未返回 ID:', data)
          throw new Error('新增場次成功但未返回 ID')
        }
        toast.success('場次已新增')
      } else {
        throw new Error('新增場次失敗')
      }
    }

    // 更新場次關聯的小組（僅當類型為小組時）
    if (sessionId) {
      if (form.value.sessionType === 'WEEKDAY' && form.value.groupIds && form.value.groupIds.length > 0) {
        // 確保 groupIds 是數字數組
        const groupIds = form.value.groupIds.map(id => typeof id === 'string' ? parseInt(id) : id)
        await apiRequest(`/church/checkin/admin/sessions/${sessionId}/groups`, {
          method: 'PUT',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({ groupIds: groupIds })
        }, '更新小組關聯中...', true)
      } else {
        // 如果類型不是小組，或沒有選擇小組，清除所有小組關聯
        await apiRequest(`/church/checkin/admin/sessions/${sessionId}/groups`, {
          method: 'PUT',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({ groupIds: [] })
        }, '更新小組關聯中...', true)
      }
    }

    emit('saved')
    close()
  } catch (error) {
    console.error('儲存場次失敗:', error)
    toast.error('儲存場次失敗')
  } finally {
    saving.value = false
  }
}

function close() {
  emit('close')
}
</script>

<style scoped>
.modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
}

.modal-content {
  background: #ffffff;
  border: 1px solid var(--border);
  border-radius: 16px;
  width: 90%;
  max-width: 600px;
  max-height: 90vh;
  overflow-y: auto;
  box-shadow: none;
}

.modal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 20px 24px;
  border-bottom: 1px solid var(--border);
}

.modal-header h3 {
  margin: 0;
  font-size: 20px;
  font-weight: 700;
  color: var(--text);
}

.close-btn {
  background: none;
  border: none;
  font-size: 32px;
  line-height: 1;
  color: var(--text);
  cursor: pointer;
  opacity: 0.7;
  transition: opacity 0.2s ease;
  padding: 0;
  width: 32px;
  height: 32px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.close-btn:hover {
  opacity: 1;
}

.modal-body {
  padding: 24px;
}

.form-group {
  margin-bottom: 20px;
}

.form-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
}

.form-group label {
  display: block;
  margin-bottom: 8px;
  font-size: 14px;
  font-weight: 600;
  color: var(--text);
}

.required {
  color: #ef4444;
}

.form-input {
  width: 100%;
  padding: 10px 12px;
  border: 1px solid var(--border);
  border-radius: 8px;
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
}

.btn-link {
  background: none;
  border: none;
  color: var(--primary);
  cursor: pointer;
  font-size: 12px;
  padding: 0;
  text-decoration: underline;
}

.btn-link:hover {
  color: var(--primary-hover);
}

.modal-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  margin-top: 24px;
  padding-top: 20px;
  border-top: 1px solid var(--border);
}

.btn {
  padding: 10px 20px;
  border-radius: 8px;
  border: 1px solid var(--border);
  background: var(--btn-bg);
  color: var(--text);
  cursor: pointer;
  font-size: 14px;
  font-weight: 600;
  transition: all 0.2s ease;
}

.btn:hover:not(:disabled) {
  background: var(--btn-hover-bg);
  transform: translateY(-1px);
}

.btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.btn-primary {
  background: var(--primary);
  color: white;
  border-color: var(--primary);
}

.btn-primary:hover:not(:disabled) {
  background: var(--primary-hover);
}

.btn-secondary {
  background: var(--bg);
  color: var(--text);
}
</style>

