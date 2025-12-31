<template>
  <div v-if="show" class="modal-overlay" @click.self="close">
    <div class="modal-content group-modal">
      <div class="modal-header">
        <h3>{{ editingGroup ? '編輯小組' : '新增小組' }}</h3>
        <div class="header-actions">
          <button type="button" @click="close" class="btn btn-secondary">取消</button>
          <button type="submit" form="group-form" class="btn btn-primary" :disabled="saving">
            {{ saving ? '儲存中...' : '儲存' }}
          </button>
          <button @click="close" class="close-btn">×</button>
        </div>
      </div>

      <form id="group-form" @submit.prevent="save" class="modal-body">
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
                <div class="person-box-header-actions">
                  <button
                    type="button"
                    @click="addAll"
                    class="btn-action-header"
                    :disabled="filteredLeftPersons.length === 0"
                  >
                    全部加入
                  </button>
                </div>
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
                  @click="addPerson(person)"
                >
                  <span>{{ person.personName || person.displayName || '-' }}</span>
                  <span class="member-no" v-if="person.memberNo">({{ person.memberNo }})</span>
                </div>
                <div v-if="filteredLeftPersons.length === 0" class="empty-message">
                  無可選人員
                </div>
              </div>
            </div>

            <!-- 右框：已加入小組的人員 -->
            <div class="person-box">
              <div class="person-box-header">
                <h5>已加入人員 ({{ rightPersons.length }})</h5>
                <div class="person-box-header-actions">
                  <button
                    type="button"
                    @click="removeAll"
                    class="btn-action-header"
                    :disabled="rightPersons.length === 0"
                  >
                    全部移除
                  </button>
                </div>
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
                  @click.stop="removePerson(person)"
                >
                  <div class="person-info">
                    <span>{{ person.personName || person.displayName || '-' }}</span>
                    <span class="member-no" v-if="person.memberNo">({{ person.memberNo }})</span>
                  </div>
                  <select
                    v-model="person.role"
                    @click.stop
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
const leftSearch = ref('')
const rightSearch = ref('')
const saving = ref(false)
const initialMemberIds = ref([]) // 保存初始載入時的成員 ID 列表

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

// 加入單個人員
const addPerson = (person) => {
  if (!person.role) {
    person.role = 'MEMBER'
  }
  rightPersons.value.push(person)
  leftPersons.value = leftPersons.value.filter(p => p.id !== person.id)
}

// 移除單個人員
const removePerson = (person) => {
  leftPersons.value.push(person)
  rightPersons.value = rightPersons.value.filter(p => p.id !== person.id)
}

const addAll = () => {
  const toAdd = filteredLeftPersons.value.map(p => ({ ...p, role: p.role || 'MEMBER' }))
  rightPersons.value.push(...toAdd)
  leftPersons.value = leftPersons.value.filter(p => !toAdd.find(fp => fp.id === p.id))
}

const removeAll = () => {
  leftPersons.value.push(...rightPersons.value)
  rightPersons.value = []
}

const loadNonMembers = async (groupId) => {
  try {
    // 載入所有人員，然後過濾掉已經在該小組的成員
    const allPersonsResponse = await apiRequest('/church/persons', {
      method: 'GET',
      credentials: 'include'
    })
    
    if (allPersonsResponse.ok) {
      const allPersonsData = await allPersonsResponse.json()
      const allPersons = allPersonsData.persons || []
      
      // 獲取該小組的成員 ID 列表
      const membersResponse = await apiRequest(`/church/groups/${groupId}/members`, {
        method: 'GET',
        credentials: 'include'
      })
      
      if (membersResponse.ok) {
        const membersData = await membersResponse.json()
        const memberIds = (membersData.members || []).map(m => m.id)
        
        // 過濾掉已經在該小組的成員
        leftPersons.value = allPersons.filter(p => !memberIds.includes(p.id))
      } else {
        // 如果獲取成員失敗，仍顯示所有人員
        leftPersons.value = allPersons
      }
    }
  } catch (error) {
    console.error('載入未加入人員失敗:', error)
    toast.error('載入人員失敗', '錯誤')
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
      // 保存初始成員 ID 列表，用於後續比較
      initialMemberIds.value = rightPersons.value.map(p => p.id)
    }
  } catch (error) {
    console.error('載入成員失敗:', error)
  }
}

const loadAllPersons = async () => {
  try {
    // 載入所有人員（不排除已有小組的人員，因為每個人可以參加多個小組）
    const response = await apiRequest('/church/persons', {
      method: 'GET',
      credentials: 'include'
    })
    
    if (response.ok) {
      const data = await response.json()
      leftPersons.value = data.persons || []
      rightPersons.value = []
    }
  } catch (error) {
    console.error('載入人員失敗:', error)
    toast.error('載入人員失敗', '錯誤')
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

    // 更新成員（包含新增、移除和角色更新）
    if (groupId) {
      const currentMemberIds = rightPersons.value.map(p => p.id)
      
      // 計算需要移除的成員（在初始列表中但不在當前列表中的）
      const membersToRemove = initialMemberIds.value.filter(id => !currentMemberIds.includes(id))
      
      // 移除成員
      for (const personId of membersToRemove) {
        try {
          await apiRequest(`/church/groups/${groupId}/members/${personId}`, {
            method: 'DELETE',
            credentials: 'include'
          })
        } catch (error) {
          console.error(`移除成員 ${personId} 失敗:`, error)
        }
      }
      
      // 新增成員並更新角色（POST API 會處理新增和角色設定）
      if (currentMemberIds.length > 0) {
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
            personIds: currentMemberIds,
            personRoles: Object.keys(personRoles).length > 0 ? personRoles : null
          })
        }, '更新成員中...', true)
      }
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
  leftSearch.value = ''
  rightSearch.value = ''
  initialMemberIds.value = []
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
  gap: 16px;
}

.modal-header h3 {
  margin: 0;
  font-size: 20px;
  font-weight: 600;
  color: #333;
  flex: 1;
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 12px;
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
  margin-left: 8px;
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
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.person-box-header h5 {
  margin: 0;
  font-size: 14px;
  font-weight: 600;
}

.person-box-header-actions {
  display: flex;
  gap: 8px;
}

.btn-action-header {
  padding: 6px 12px;
  border: 1px solid #ddd;
  border-radius: 4px;
  background: white;
  cursor: pointer;
  transition: all 0.2s;
  font-size: 13px;
  font-weight: 500;
}

.btn-action-header:hover:not(:disabled) {
  background: #f0f0f0;
  border-color: #999;
}

.btn-action-header:disabled {
  opacity: 0.5;
  cursor: not-allowed;
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
  flex-wrap: wrap;
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

.person-info {
  display: flex;
  align-items: center;
  gap: 4px;
  flex: 1;
  min-width: 0;
}

.person-item span {
  flex-shrink: 0;
}

.member-no {
  color: #666;
  font-size: 12px;
}

.role-select {
  width: 100%;
  padding: 4px 8px;
  border: 1px solid #ddd;
  border-radius: 4px;
  font-size: 12px;
  background: white;
  cursor: pointer;
  margin-left: 0;
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


.btn {
  padding: 8px 16px;
  border: none;
  border-radius: 6px;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s;
  display: inline-flex;
  align-items: center;
  justify-content: center;
}

.btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.btn-primary {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
}

.btn-primary:hover:not(:disabled) {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(102, 126, 234, 0.4);
}

.btn-secondary {
  background: #6c757d;
  color: white;
}

.btn-secondary:hover {
  background: #5a6268;
  transform: translateY(-2px);
}
</style>

