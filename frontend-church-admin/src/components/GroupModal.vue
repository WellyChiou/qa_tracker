<template>
  <div v-if="show" class="modal-overlay" @click.self="close">
    <div class="modal-content group-modal">
      <div class="modal-header">
        <h3>{{ editingGroup ? '編輯小組' : '新增小組' }}</h3>
        <button @click="close" class="close-btn">×</button>
      </div>

      <form @submit.prevent="save" class="modal-body">
        <!-- 基本資訊 -->
        <div class="form-section">
          <h4>基本資訊</h4>
          <div class="form-group">
            <label>小組名稱 <span class="required">*</span></label>
            <input
              v-model="form.groupName"
              type="text"
              class="form-input"
              placeholder="例如：第一小組"
              required
            />
          </div>

          <div class="form-group">
            <label>描述</label>
            <textarea
              v-model="form.description"
              class="form-input"
              placeholder="小組描述（可選）"
              rows="3"
            ></textarea>
          </div>

          <div class="form-group">
            <label>聚會頻率</label>
            <input
              v-model="form.meetingFrequency"
              type="text"
              class="form-input"
              placeholder="例如：一週一次、兩週一次、三週一次、四周一次"
            />
          </div>

          <div class="form-group">
            <label>區分</label>
            <input
              v-model="form.category"
              type="text"
              class="form-input"
              placeholder="例如：學生(國高)、學生(大專)、社青、家萱區、遲靜區"
            />
          </div>

          <div class="form-group">
            <label>聚會地點</label>
            <input
              v-model="form.meetingLocation"
              type="text"
              class="form-input"
              placeholder="例如：極光基地、榮耀堂"
            />
          </div>

          <div class="form-group">
            <label>狀態</label>
            <select v-model="form.isActive" class="form-input">
              <option :value="true">啟用</option>
              <option :value="false">停用</option>
            </select>
          </div>
        </div>

        <!-- 人員選擇 -->
        <div class="form-section">
          <h4>人員管理</h4>
          <div class="person-selector">
            <!-- 左框：未加入小組的人員 -->
            <div class="person-box">
              <div class="person-box-header">
                <h5>尚未加入人員</h5>
                <input
                  v-model="leftSearch"
                  type="text"
                  placeholder="搜尋人員..."
                  class="search-input"
                />
              </div>
              <div class="person-list">
                <div
                  v-for="person in filteredLeftPersons"
                  :key="person.id"
                  class="person-item"
                  :class="{ selected: isLeftSelected(person.id) }"
                  @click="toggleLeftSelection(person.id)"
                >
                  <input
                    type="checkbox"
                    :checked="isLeftSelected(person.id)"
                    @change="toggleLeftSelection(person.id)"
                    @click.stop
                  />
                  <span>{{ person.personName || person.displayName || '-' }}</span>
                  <span class="member-no" v-if="person.memberNo">({{ person.memberNo }})</span>
                </div>
                <div v-if="filteredLeftPersons.length === 0" class="empty-message">
                  無可選人員
                </div>
              </div>
            </div>

            <!-- 中間按鈕 -->
            <div class="person-actions">
              <button
                type="button"
                @click="addSelected"
                class="btn-action"
                :disabled="leftSelected.length === 0"
              >
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <path d="M12 5v14M5 12h14"/>
                </svg>
              </button>
              <button
                type="button"
                @click="removeSelected"
                class="btn-action"
                :disabled="rightSelected.length === 0"
              >
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <path d="M5 12h14"/>
                </svg>
              </button>
              <button
                type="button"
                @click="addAll"
                class="btn-action"
                :disabled="filteredLeftPersons.length === 0"
              >
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <path d="M12 5v14M5 12h14"/>
                </svg>
                <span>全部</span>
              </button>
              <button
                type="button"
                @click="removeAll"
                class="btn-action"
                :disabled="rightPersons.length === 0"
              >
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <path d="M5 12h14"/>
                </svg>
                <span>全部</span>
              </button>
            </div>

            <!-- 右框：已加入小組的人員 -->
            <div class="person-box">
              <div class="person-box-header">
                <h5>已加入人員 ({{ rightPersons.length }})</h5>
                <input
                  v-model="rightSearch"
                  type="text"
                  placeholder="搜尋人員..."
                  class="search-input"
                />
              </div>
              <div class="person-list">
                <div
                  v-for="person in filteredRightPersons"
                  :key="person.id"
                  class="person-item"
                  :class="{ selected: isRightSelected(person.id) }"
                  @click="toggleRightSelection(person.id)"
                >
                  <input
                    type="checkbox"
                    :checked="isRightSelected(person.id)"
                    @change="toggleRightSelection(person.id)"
                    @click.stop
                  />
                  <span>{{ person.personName || person.displayName || '-' }}</span>
                  <span class="member-no" v-if="person.memberNo">({{ person.memberNo }})</span>
                  <select
                    v-model="person.role"
                    @click.stop
                    @change="updatePersonRole(person.id, person.role)"
                    class="role-select"
                  >
                    <option value="MEMBER">一般成員</option>
                    <option value="LEADER">小組長</option>
                    <option value="ASSISTANT_LEADER">實習小組長</option>
                  </select>
                </div>
                <div v-if="filteredRightPersons.length === 0" class="empty-message">
                  尚無人員
                </div>
              </div>
            </div>
          </div>
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
import { ref, computed, watch, onMounted } from 'vue'
import { toast } from '@/composables/useToast'
import { apiRequest } from '@/utils/api'

const props = defineProps({
  show: {
    type: Boolean,
    default: false
  },
  group: {
    type: Object,
    default: null
  }
})

const emit = defineEmits(['close', 'saved'])

const form = ref({
  groupName: '',
  description: '',
  meetingFrequency: '',
  category: '',
  meetingLocation: '',
  isActive: true
})

const leftPersons = ref([])
const rightPersons = ref([])
const leftSelected = ref([])
const rightSelected = ref([])
const leftSearch = ref('')
const rightSearch = ref('')
const saving = ref(false)

const editingGroup = computed(() => props.group)

// 過濾左框人員
const filteredLeftPersons = computed(() => {
  let filtered = leftPersons.value
  if (leftSearch.value) {
    const search = leftSearch.value.toLowerCase()
    filtered = filtered.filter(p => 
      (p.personName || '').toLowerCase().includes(search) ||
      (p.displayName || '').toLowerCase().includes(search) ||
      (p.memberNo || '').toLowerCase().includes(search)
    )
  }
  return filtered
})

// 過濾右框人員
const filteredRightPersons = computed(() => {
  let filtered = rightPersons.value
  if (rightSearch.value) {
    const search = rightSearch.value.toLowerCase()
    filtered = filtered.filter(p => 
      (p.personName || '').toLowerCase().includes(search) ||
      (p.displayName || '').toLowerCase().includes(search) ||
      (p.memberNo || '').toLowerCase().includes(search)
    )
  }
  return filtered
})

const isLeftSelected = (personId) => {
  return leftSelected.value.includes(personId)
}

const isRightSelected = (personId) => {
  return rightSelected.value.includes(personId)
}

const toggleLeftSelection = (personId) => {
  const index = leftSelected.value.indexOf(personId)
  if (index > -1) {
    leftSelected.value.splice(index, 1)
  } else {
    leftSelected.value.push(personId)
  }
}

const toggleRightSelection = (personId) => {
  const index = rightSelected.value.indexOf(personId)
  if (index > -1) {
    rightSelected.value.splice(index, 1)
  } else {
    rightSelected.value.push(personId)
  }
}

const addSelected = () => {
  const selected = leftPersons.value.filter(p => leftSelected.value.includes(p.id))
  // 為新加入的成員設定預設角色
  selected.forEach(p => {
    if (!p.role) {
      p.role = 'MEMBER'
    }
  })
  rightPersons.value.push(...selected)
  leftPersons.value = leftPersons.value.filter(p => !leftSelected.value.includes(p.id))
  leftSelected.value = []
}

const removeSelected = () => {
  const selected = rightPersons.value.filter(p => rightSelected.value.includes(p.id))
  leftPersons.value.push(...selected)
  rightPersons.value = rightPersons.value.filter(p => !rightSelected.value.includes(p.id))
  rightSelected.value = []
}

const addAll = () => {
  const toAdd = filteredLeftPersons.value.map(p => ({ ...p, role: p.role || 'MEMBER' }))
  rightPersons.value.push(...toAdd)
  leftPersons.value = leftPersons.value.filter(p => !toAdd.find(fp => fp.id === p.id))
  leftSelected.value = []
}

const removeAll = () => {
  leftPersons.value.push(...rightPersons.value)
  rightPersons.value = []
  rightSelected.value = []
}

const loadNonMembers = async (groupId) => {
  try {
    const response = await apiRequest(`/church/groups/${groupId}/non-members`, {
      method: 'GET',
      credentials: 'include'
    })
    
    if (response.ok) {
      const data = await response.json()
      leftPersons.value = data.nonMembers || []
    }
  } catch (error) {
    console.error('載入未加入人員失敗:', error)
  }
}

const loadMembers = async (groupId) => {
  try {
    const response = await apiRequest(`/church/groups/${groupId}/members`, {
      method: 'GET',
      credentials: 'include'
    })
    
    if (response.ok) {
      const data = await response.json()
      // 確保每個成員都有 role 欄位
      rightPersons.value = (data.members || []).map(m => ({
        ...m,
        role: m.role || 'MEMBER'
      }))
    }
  } catch (error) {
    console.error('載入成員失敗:', error)
  }
}

const updatePersonRole = async (personId, role) => {
  if (!editingGroup.value) return
  
  try {
    const response = await apiRequest(`/church/groups/${editingGroup.value.id}/members/${personId}/role`, {
      method: 'PUT',
      headers: { 'Content-Type': 'application/json' },
      credentials: 'include',
      body: JSON.stringify({ role })
    })
    
    if (response.ok) {
      // 角色已更新，不需要額外處理
    }
  } catch (error) {
    console.error('更新成員角色失敗:', error)
    toast.error('更新成員角色失敗', '錯誤')
  }
}

const loadAllPersons = async () => {
  try {
    // 使用新的 API 端點，只獲取未加入任何小組的人員
    const response = await apiRequest('/church/groups/non-members', {
      method: 'GET',
      credentials: 'include'
    })
    
    if (response.ok) {
      const data = await response.json()
      leftPersons.value = data.nonMembers || []
      rightPersons.value = []
    }
  } catch (error) {
    console.error('載入人員失敗:', error)
  }
}

const save = async () => {
  saving.value = true
  try {
    const groupData = {
      groupName: form.value.groupName,
      description: form.value.description,
      meetingFrequency: form.value.meetingFrequency,
      category: form.value.category,
      meetingLocation: form.value.meetingLocation,
      isActive: form.value.isActive
    }

    let groupId
    if (editingGroup.value) {
      // 更新小組
      const response = await apiRequest(`/church/groups/${editingGroup.value.id}`, {
        method: 'PUT',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(groupData)
      }, '更新中...', true)
      
      if (response.ok) {
        const data = await response.json()
        groupId = data.group.id
        toast.success('小組更新成功', '成功')
      } else {
        throw new Error('更新失敗')
      }
    } else {
      // 創建小組
      const response = await apiRequest('/church/groups', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(groupData)
      }, '創建中...', true)
      
      if (response.ok) {
        const data = await response.json()
        groupId = data.group.id
        toast.success('小組創建成功', '成功')
      } else {
        throw new Error('創建失敗')
      }
    }

    // 更新成員（包含角色）
    if (groupId && rightPersons.value.length > 0) {
      const personIds = rightPersons.value.map(p => p.id)
      const personRoles = {}
      rightPersons.value.forEach(p => {
        if (p.role) {
          personRoles[p.id] = p.role
        }
      })
      await apiRequest(`/church/groups/${groupId}/members`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        credentials: 'include',
        body: JSON.stringify({ 
          personIds,
          personRoles: Object.keys(personRoles).length > 0 ? personRoles : null
        })
      }, '更新成員中...', true)
    }

    emit('saved')
    close()
  } catch (error) {
    console.error('儲存失敗:', error)
    toast.error('儲存失敗', '錯誤')
  } finally {
    saving.value = false
  }
}

const close = () => {
  emit('close')
  // 重置表單
  form.value = {
    groupName: '',
    description: '',
    meetingFrequency: '',
    category: '',
    meetingLocation: '',
    isActive: true
  }
  leftPersons.value = []
  rightPersons.value = []
  leftSelected.value = []
  rightSelected.value = []
  leftSearch.value = ''
  rightSearch.value = ''
}

// 當 modal 顯示或 group 資料變化時，載入資料
watch([() => props.show, () => props.group], ([showVal, groupVal]) => {
  if (showVal) {
    if (groupVal) {
      // 編輯模式：載入小組資料
      form.value = {
        groupName: groupVal.groupName || '',
        description: groupVal.description || '',
        meetingFrequency: groupVal.meetingFrequency || '',
        category: groupVal.category || '',
        meetingLocation: groupVal.meetingLocation || '',
        isActive: groupVal.isActive !== undefined ? groupVal.isActive : true
      }
      loadNonMembers(groupVal.id)
      loadMembers(groupVal.id)
    } else {
      // 新增模式：載入所有人員
      form.value = {
        groupName: '',
        description: '',
        meetingFrequency: '',
        category: '',
        meetingLocation: '',
        isActive: true
      }
      loadAllPersons()
    }
  }
}, { immediate: true })

onMounted(() => {
  if (props.show) {
    if (props.group) {
      form.value = {
        groupName: props.group.groupName || '',
        description: props.group.description || '',
        meetingFrequency: props.group.meetingFrequency || '',
        category: props.group.category || '',
        meetingLocation: props.group.meetingLocation || '',
        isActive: props.group.isActive !== undefined ? props.group.isActive : true
      }
      loadNonMembers(props.group.id)
      loadMembers(props.group.id)
    } else {
      loadAllPersons()
    }
  }
})
</script>

<style scoped>
.modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  z-index: 1300;
  background: rgba(0, 0, 0, 0.5);
  backdrop-filter: blur(4px);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 20px;
}

.modal-content {
  background: #ffffff;
  border-radius: 12px;
  width: 90%;
  max-width: 1200px;
  max-height: 90vh;
  overflow-y: auto;
  box-shadow: 0 20px 25px -5px rgba(0, 0, 0, 0.1), 0 10px 10px -5px rgba(0, 0, 0, 0.04);
  display: flex;
  flex-direction: column;
}

.modal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 20px 24px;
  border-bottom: 1px solid #eee;
}

.modal-header h3 {
  margin: 0;
  font-size: 20px;
  font-weight: 600;
  color: #333;
}

.close-btn {
  background: none;
  border: none;
  font-size: 28px;
  line-height: 1;
  color: #666;
  cursor: pointer;
  padding: 0;
  width: 32px;
  height: 32px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 4px;
  transition: all 0.2s;
}

.close-btn:hover {
  background: #f0f0f0;
  color: #333;
}

.modal-body {
  padding: 24px;
  flex: 1;
  overflow-y: auto;
}

.group-modal {
  max-width: 1200px;
  width: 100%;
  max-height: 90vh;
  overflow-y: auto;
}

.form-section {
  margin-bottom: 24px;
}

.form-section h4 {
  margin: 0 0 16px 0;
  font-size: 16px;
  font-weight: 600;
  color: #333;
  border-bottom: 2px solid #eee;
  padding-bottom: 8px;
}

.person-selector {
  display: flex;
  gap: 16px;
  min-height: 400px;
}

.person-box {
  flex: 1;
  border: 1px solid #ddd;
  border-radius: 8px;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.person-box-header {
  padding: 12px;
  background: #f5f5f5;
  border-bottom: 1px solid #ddd;
}

.person-box-header h5 {
  margin: 0 0 8px 0;
  font-size: 14px;
  font-weight: 600;
}

.search-input {
  width: 100%;
  padding: 6px 10px;
  border: 1px solid #ddd;
  border-radius: 4px;
  font-size: 14px;
}

.person-list {
  flex: 1;
  overflow-y: auto;
  padding: 8px;
}

.person-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px;
  border-radius: 4px;
  cursor: pointer;
  transition: background 0.2s;
}

.person-item:hover {
  background: #f0f0f0;
}

.person-item.selected {
  background: #e3f2fd;
}

.person-item input[type="checkbox"] {
  cursor: pointer;
}

.person-item span {
  flex: 1;
}

.member-no {
  color: #666;
  font-size: 12px;
}

.role-select {
  margin-left: auto;
  padding: 4px 8px;
  border: 1px solid #ddd;
  border-radius: 4px;
  font-size: 12px;
  background: white;
  cursor: pointer;
}

.role-select:hover {
  border-color: #999;
}

.empty-message {
  text-align: center;
  padding: 40px;
  color: #999;
}

.person-actions {
  display: flex;
  flex-direction: column;
  justify-content: center;
  gap: 8px;
  padding: 0 8px;
}

.btn-action {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 4px;
  padding: 8px 12px;
  border: 1px solid #ddd;
  border-radius: 4px;
  background: white;
  cursor: pointer;
  transition: all 0.2s;
  min-width: 80px;
}

.btn-action:hover:not(:disabled) {
  background: #f0f0f0;
  border-color: #999;
}

.btn-action:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.btn-action svg {
  width: 16px;
  height: 16px;
}

.modal-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  padding-top: 16px;
  border-top: 1px solid #eee;
  margin-top: 24px;
}
</style>

