<template>
  <AdminLayout>
    <div class="line-groups-page">
      <div class="page-header">
        <div>
          <h2>LINE 群組管理</h2>
          <p>管理教會通知群組、群組代碼與成員名單，維持 church 站台自己的 LINE ownership。</p>
        </div>
        <button class="btn btn-primary" @click="openGroupModal()">+ 新增群組</button>
      </div>

      <section class="overview-strip">
        <article class="overview-card overview-card--accent">
          <span>群組總數</span>
          <strong>{{ groups.length }}</strong>
          <p>目前 church 後台可管理的 LINE 群組數量。</p>
        </article>
        <article class="overview-card">
          <span>啟用群組</span>
          <strong>{{ activeGroupCount }}</strong>
          <p>目前仍在啟用中的通知與互動群組。</p>
        </article>
        <article class="overview-card">
          <span>目前檢視</span>
          <strong>{{ selectedGroup ? selectedGroup.groupName || selectedGroup.groupId : '未選取' }}</strong>
          <p>右側成員清單會跟著目前選取的群組切換。</p>
        </article>
      </section>

      <div class="workspace-grid">
        <section class="card surface-card">
          <div class="panel-head">
            <div>
              <h3>群組列表</h3>
              <p>可編輯群組名稱、群組代碼與啟用狀態。</p>
            </div>
          </div>

          <div class="toolbar">
            <input
              v-model.trim="keyword"
              type="text"
              class="form-input"
              placeholder="搜尋群組名稱、群組 ID、群組代碼"
            />
            <select v-model="statusFilter" class="form-input select-small">
              <option value="">全部狀態</option>
              <option value="active">啟用</option>
              <option value="inactive">停用</option>
            </select>
          </div>

          <div v-if="filteredGroups.length === 0" class="empty-state">
            <p>目前沒有符合條件的 LINE 群組。</p>
          </div>

          <div v-else class="table-wrap">
            <table>
              <thead>
                <tr>
                  <th>群組名稱</th>
                  <th>群組 ID</th>
                  <th>群組代碼</th>
                  <th>成員數</th>
                  <th>狀態</th>
                  <th>操作</th>
                </tr>
              </thead>
              <tbody>
                <tr
                  v-for="group in filteredGroups"
                  :key="group.groupId"
                  :class="{ 'is-selected': selectedGroup?.groupId === group.groupId }"
                  @click="selectGroup(group)"
                >
                  <td>{{ group.groupName || '未命名群組' }}</td>
                  <td class="mono">{{ group.groupId }}</td>
                  <td>{{ group.groupCode || '-' }}</td>
                  <td>{{ group.memberCount || 0 }}</td>
                  <td>
                    <span :class="['status-badge', group.isActive ? 'status-active' : 'status-inactive']">
                      {{ group.isActive ? '啟用' : '停用' }}
                    </span>
                  </td>
                  <td>
                    <div class="table-actions">
                      <button class="btn btn-edit" @click.stop="openGroupModal(group)">編輯</button>
                      <button class="btn btn-delete" @click.stop="removeGroup(group)">刪除</button>
                    </div>
                  </td>
                </tr>
              </tbody>
            </table>
          </div>
        </section>

        <section class="card surface-card">
          <div class="panel-head">
            <div>
              <h3>成員管理</h3>
              <p v-if="selectedGroup">目前群組：{{ selectedGroup.groupName || selectedGroup.groupId }}</p>
              <p v-else>先從左側選一個群組，再管理成員。</p>
            </div>
            <button class="btn btn-primary" :disabled="!selectedGroup" @click="openMemberModal()">+ 新增成員</button>
          </div>

          <div v-if="!selectedGroup" class="empty-state">
            <p>尚未選取群組。</p>
          </div>

          <div v-else-if="members.length === 0" class="empty-state">
            <p>這個群組目前沒有啟用中的成員。</p>
          </div>

          <div v-else class="table-wrap">
            <table>
              <thead>
                <tr>
                  <th>顯示名稱</th>
                  <th>LINE User ID</th>
                  <th>管理員</th>
                  <th>加入時間</th>
                  <th>操作</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="member in members" :key="member.id">
                  <td>{{ member.displayName || '-' }}</td>
                  <td class="mono">{{ member.userId }}</td>
                  <td>{{ member.isAdmin ? '是' : '否' }}</td>
                  <td>{{ formatDateTime(member.joinedAt) }}</td>
                  <td>
                    <div class="table-actions">
                      <button class="btn btn-edit" @click="openMemberModal(member)">編輯</button>
                      <button class="btn btn-delete" @click="removeMember(member)">移除</button>
                    </div>
                  </td>
                </tr>
              </tbody>
            </table>
          </div>
        </section>
      </div>

      <div v-if="showGroupModal" class="modal-backdrop" @click.self="closeGroupModal">
        <div class="modal-card">
          <div class="modal-head">
            <h3>{{ editingGroup ? '編輯群組' : '新增群組' }}</h3>
            <button class="modal-close" @click="closeGroupModal">×</button>
          </div>
          <div class="modal-body">
            <label class="field">
              <span>群組 ID</span>
              <input v-model.trim="groupForm.groupId" class="form-input" :disabled="!!editingGroup" />
            </label>
            <label class="field">
              <span>群組名稱</span>
              <input v-model.trim="groupForm.groupName" class="form-input" />
            </label>
            <label class="field">
              <span>群組代碼</span>
              <input v-model.trim="groupForm.groupCode" class="form-input" placeholder="例如 CHURCH_TECH_CONTROL" />
            </label>
            <label class="field checkbox-field">
              <input v-model="groupForm.isActive" type="checkbox" />
              <span>啟用此群組</span>
            </label>
          </div>
          <div class="modal-actions">
            <button class="btn btn-secondary" @click="closeGroupModal">取消</button>
            <button class="btn btn-primary" @click="saveGroup">儲存</button>
          </div>
        </div>
      </div>

      <div v-if="showMemberModal" class="modal-backdrop" @click.self="closeMemberModal">
        <div class="modal-card">
          <div class="modal-head">
            <h3>{{ editingMember ? '編輯成員' : '新增成員' }}</h3>
            <button class="modal-close" @click="closeMemberModal">×</button>
          </div>
          <div class="modal-body">
            <label class="field">
              <span>LINE User ID</span>
              <input v-model.trim="memberForm.userId" class="form-input" :disabled="!!editingMember" />
            </label>
            <label class="field">
              <span>顯示名稱</span>
              <input v-model.trim="memberForm.displayName" class="form-input" />
            </label>
            <label class="field checkbox-field">
              <input v-model="memberForm.isAdmin" type="checkbox" />
              <span>設為管理員</span>
            </label>
          </div>
          <div class="modal-actions">
            <button class="btn btn-secondary" @click="closeMemberModal">取消</button>
            <button class="btn btn-primary" @click="saveMember">儲存</button>
          </div>
        </div>
      </div>
    </div>
  </AdminLayout>
</template>

<script setup>
import { toast } from '@shared/composables/useToast'
import { computed, onMounted, ref } from 'vue'
import AdminLayout from '@/components/AdminLayout.vue'
import { apiRequest } from '@/utils/api'

const groups = ref([])
const members = ref([])
const selectedGroup = ref(null)
const keyword = ref('')
const statusFilter = ref('')

const showGroupModal = ref(false)
const editingGroup = ref(null)
const groupForm = ref(createEmptyGroup())

const showMemberModal = ref(false)
const editingMember = ref(null)
const memberForm = ref(createEmptyMember())

const activeGroupCount = computed(() => groups.value.filter(group => group.isActive).length)
const filteredGroups = computed(() => {
  return groups.value.filter(group => {
    const matchKeyword = !keyword.value || [group.groupName, group.groupId, group.groupCode]
      .filter(Boolean)
      .some(value => value.toLowerCase().includes(keyword.value.toLowerCase()))
    const matchStatus = !statusFilter.value
      || (statusFilter.value === 'active' && group.isActive)
      || (statusFilter.value === 'inactive' && !group.isActive)
    return matchKeyword && matchStatus
  })
})

function createEmptyGroup() {
  return {
    groupId: '',
    groupName: '',
    groupCode: '',
    isActive: true
  }
}

function createEmptyMember() {
  return {
    userId: '',
    displayName: '',
    isAdmin: false
  }
}

function formatDateTime(value) {
  if (!value) return '-'
  return new Date(value).toLocaleString('zh-TW', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  })
}

async function loadGroups(preserveSelection = true) {
  const data = await apiRequest('/church/admin/line-groups', { method: 'GET' }, '載入 LINE 群組中...')
  groups.value = Array.isArray(data) ? data : []

  if (preserveSelection && selectedGroup.value) {
    const matched = groups.value.find(group => group.groupId === selectedGroup.value.groupId)
    if (matched) {
      await selectGroup(matched)
      return
    }
  }

  if (groups.value.length > 0) {
    await selectGroup(groups.value[0])
  } else {
    selectedGroup.value = null
    members.value = []
  }
}

async function selectGroup(group) {
  selectedGroup.value = group
  const data = await apiRequest(`/church/admin/line-groups/${group.groupId}/members`, { method: 'GET' }, '載入群組成員中...')
  members.value = Array.isArray(data) ? data : []
}

function openGroupModal(group = null) {
  editingGroup.value = group
  groupForm.value = group
    ? {
        groupId: group.groupId,
        groupName: group.groupName || '',
        groupCode: group.groupCode || '',
        isActive: !!group.isActive
      }
    : createEmptyGroup()
  showGroupModal.value = true
}

function closeGroupModal() {
  showGroupModal.value = false
  editingGroup.value = null
  groupForm.value = createEmptyGroup()
}

async function saveGroup() {
  if (!groupForm.value.groupId) {
    toast.error('群組 ID 不能為空')
    return
  }

  const isEditing = !!editingGroup.value
  const url = isEditing
    ? `/church/admin/line-groups/${editingGroup.value.groupId}`
    : '/church/admin/line-groups'
  const method = isEditing ? 'PUT' : 'POST'

  await apiRequest(url, {
    method,
    body: JSON.stringify(groupForm.value)
  }, isEditing ? '更新群組中...' : '建立群組中...')

  toast.success(isEditing ? '群組已更新' : '群組已建立')
  closeGroupModal()
  await loadGroups(true)
}

async function removeGroup(group) {
  if (!window.confirm(`確定要刪除群組「${group.groupName || group.groupId}」嗎？`)) {
    return
  }

  await apiRequest(`/church/admin/line-groups/${group.groupId}`, {
    method: 'DELETE'
  }, '刪除群組中...')

  toast.success('群組已刪除')
  await loadGroups(false)
}

function openMemberModal(member = null) {
  if (!selectedGroup.value) return

  editingMember.value = member
  memberForm.value = member
    ? {
        userId: member.userId,
        displayName: member.displayName || '',
        isAdmin: !!member.isAdmin
      }
    : createEmptyMember()
  showMemberModal.value = true
}

function closeMemberModal() {
  showMemberModal.value = false
  editingMember.value = null
  memberForm.value = createEmptyMember()
}

async function saveMember() {
  if (!selectedGroup.value) return
  if (!memberForm.value.userId) {
    toast.error('LINE User ID 不能為空')
    return
  }

  const isEditing = !!editingMember.value
  const url = isEditing
    ? `/church/admin/line-groups/${selectedGroup.value.groupId}/members/${editingMember.value.id}`
    : `/church/admin/line-groups/${selectedGroup.value.groupId}/members`
  const method = isEditing ? 'PUT' : 'POST'

  await apiRequest(url, {
    method,
    body: JSON.stringify(memberForm.value)
  }, isEditing ? '更新成員中...' : '新增成員中...')

  toast.success(isEditing ? '成員已更新' : '成員已新增')
  closeMemberModal()
  await loadGroups(true)
}

async function removeMember(member) {
  if (!selectedGroup.value) return
  if (!window.confirm(`確定要移除成員「${member.displayName || member.userId}」嗎？`)) {
    return
  }

  await apiRequest(`/church/admin/line-groups/${selectedGroup.value.groupId}/members/${member.id}`, {
    method: 'DELETE'
  }, '移除成員中...')

  toast.success('成員已移除')
  await loadGroups(true)
}

onMounted(async () => {
  await loadGroups(false)
})
</script>

<style scoped>
.line-groups-page {
  display: grid;
  gap: 24px;
}

.workspace-grid {
  display: grid;
  grid-template-columns: 1.05fr 0.95fr;
  gap: 24px;
}

.panel-head,
.toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
}

.panel-head {
  margin-bottom: 18px;
}

.panel-head h3 {
  margin: 0;
}

.panel-head p {
  margin: 6px 0 0;
  color: var(--muted-text, #667085);
}

.toolbar {
  margin-bottom: 16px;
}

.select-small {
  max-width: 160px;
}

.table-wrap {
  overflow-x: auto;
}

.mono {
  font-family: "SFMono-Regular", Consolas, monospace;
  font-size: 12px;
}

tbody tr.is-selected {
  background: rgba(14, 116, 144, 0.08);
}

.modal-backdrop {
  position: fixed;
  inset: 0;
  background: rgba(15, 23, 42, 0.42);
  display: grid;
  place-items: center;
  z-index: 999;
  padding: 24px;
}

.modal-card {
  width: min(560px, 100%);
  background: #fff;
  border-radius: 20px;
  box-shadow: 0 24px 70px rgba(15, 23, 42, 0.18);
  overflow: hidden;
}

.modal-head,
.modal-actions {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 18px 22px;
}

.modal-head {
  border-bottom: 1px solid rgba(15, 23, 42, 0.08);
}

.modal-head h3 {
  margin: 0;
}

.modal-close {
  border: 0;
  background: transparent;
  font-size: 28px;
  line-height: 1;
  cursor: pointer;
}

.modal-body {
  padding: 22px;
  display: grid;
  gap: 16px;
}

.field {
  display: grid;
  gap: 8px;
}

.field span {
  font-size: 14px;
  color: #344054;
}

.checkbox-field {
  grid-template-columns: auto 1fr;
  align-items: center;
}

.modal-actions {
  border-top: 1px solid rgba(15, 23, 42, 0.08);
  justify-content: flex-end;
}

@media (max-width: 1100px) {
  .workspace-grid {
    grid-template-columns: 1fr;
  }
}
</style>
