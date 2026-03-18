<template>
  <AdminLayout>
    <div class="line-groups-page">
      <div class="page-header">
        <div>
          <h2>LINE 群組管理</h2>
          <p>管理 personal 通知群組、群組代碼與成員名單，維持 personal 站台自己的 LINE ownership。</p>
        </div>
        <button class="btn btn-primary" @click="openCreateGroupModal">+ 新增群組</button>
      </div>

      <section class="overview-strip">
        <article class="overview-card overview-card--accent">
          <span>群組總數</span>
          <strong>{{ groups.length }}</strong>
          <p>目前 personal 後台可管理的 LINE 群組數量。</p>
        </article>
        <article class="overview-card">
          <span>啟用群組</span>
          <strong>{{ activeGroupCount }}</strong>
          <p>目前仍在啟用中的通知與互動群組。</p>
        </article>
        <article class="overview-card">
          <span>目前檢視</span>
          <strong>{{ selectedGroup ? (selectedGroup.groupName || selectedGroup.groupId) : '未選取' }}</strong>
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
            >
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
                  <th>群組 ID</th>
                  <th>群組名稱</th>
                  <th>群組代碼</th>
                  <th>成員數</th>
                  <th>狀態</th>
                  <th>建立時間</th>
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
                  <td>{{ group.groupId }}</td>
                  <td>{{ group.groupName || '未命名群組' }}</td>
                  <td>{{ group.groupCode || '-' }}</td>
                  <td>{{ group.memberCount || 0 }}</td>
                  <td>
                    <span :class="['status-badge', group.isActive ? 'status-active' : 'status-inactive']">
                      {{ group.isActive ? '啟用' : '停用' }}
                    </span>
                  </td>
                  <td>{{ formatDate(group.createdAt) }}</td>
                  <td>
                    <div class="table-actions" @click.stop>
                      <button class="btn btn-edit" @click="selectGroup(group)">成員</button>
                      <button class="btn btn-edit" @click="editGroup(group)">編輯</button>
                      <button class="btn btn-delete" @click="deleteGroup(group)">刪除</button>
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
            <button class="btn btn-primary" :disabled="!selectedGroup" @click="openAddMemberModal">+ 新增成員</button>
          </div>

          <div v-if="!selectedGroup" class="empty-state">
            <p>尚未選取群組。</p>
          </div>

          <div v-else-if="selectedGroupMembers.length === 0" class="empty-state">
            <p>這個群組目前沒有啟用中的成員。</p>
          </div>

          <div v-else class="table-wrap">
            <table>
              <thead>
                <tr>
                  <th>User ID</th>
                  <th>顯示名稱</th>
                  <th>管理員</th>
                  <th>加入時間</th>
                  <th>操作</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="member in selectedGroupMembers" :key="member.id">
                  <td>{{ member.userId || '-' }}</td>
                  <td>{{ member.displayName || '-' }}</td>
                  <td>{{ member.isAdmin ? '是' : '否' }}</td>
                  <td>{{ formatDate(member.joinedAt) }}</td>
                  <td>
                    <div class="table-actions">
                      <button class="btn btn-edit" @click="editMember(member)">編輯</button>
                      <button class="btn btn-delete" @click="deleteMember(member)">移除</button>
                    </div>
                  </td>
                </tr>
              </tbody>
            </table>
          </div>
        </section>
      </div>

      <div class="modal fade" id="groupModal" tabindex="-1" aria-hidden="true">
        <div class="modal-dialog modal-dialog-centered">
          <div class="modal-content line-modal">
            <div class="modal-head">
              <h3>{{ isEditingGroup ? '編輯群組' : '新增群組' }}</h3>
              <button type="button" class="modal-close" data-bs-dismiss="modal">×</button>
            </div>
            <div class="line-modal-body">
              <label class="field">
                <span>群組 ID</span>
                <input v-model.trim="groupForm.groupId" class="form-input" :disabled="isEditingGroup" required>
              </label>
              <label class="field">
                <span>群組名稱</span>
                <input v-model.trim="groupForm.groupName" class="form-input">
              </label>
              <label class="field">
                <span>群組代碼</span>
                <select v-model="groupForm.groupCode" class="form-input">
                  <option value="">（無）</option>
                  <option value="PERSONAL">PERSONAL（個人）</option>
                  <option value="CHURCH_TECH_CONTROL">CHURCH_TECH_CONTROL（教會技術控制）</option>
                </select>
              </label>
              <label class="field checkbox-field">
                <input type="checkbox" v-model="groupForm.isActive">
                <span>啟用此群組</span>
              </label>
            </div>
            <div class="modal-actions">
              <button type="button" class="btn btn-secondary" data-bs-dismiss="modal">取消</button>
              <button type="button" class="btn btn-primary" @click="saveGroup">儲存</button>
            </div>
          </div>
        </div>
      </div>

      <div class="modal fade" id="memberModal" tabindex="-1" aria-hidden="true">
        <div class="modal-dialog modal-dialog-centered">
          <div class="modal-content line-modal">
            <div class="modal-head">
              <h3>{{ isEditingMember ? '編輯成員' : '新增成員' }}</h3>
              <button type="button" class="modal-close" data-bs-dismiss="modal">×</button>
            </div>
            <div class="line-modal-body">
              <label class="field">
                <span>User ID</span>
                <input v-model.trim="memberForm.userId" class="form-input" :disabled="isEditingMember" required>
              </label>
              <label class="field">
                <span>顯示名稱</span>
                <input v-model.trim="memberForm.displayName" class="form-input">
              </label>
              <label class="field checkbox-field">
                <input type="checkbox" v-model="memberForm.isAdmin">
                <span>設為管理員</span>
              </label>
            </div>
            <div class="modal-actions">
              <button type="button" class="btn btn-secondary" data-bs-dismiss="modal">取消</button>
              <button type="button" class="btn btn-primary" @click="saveMember">儲存</button>
            </div>
          </div>
        </div>
      </div>
    </div>
  </AdminLayout>
</template>

<script setup>
import AdminLayout from '@/components/AdminLayout.vue'
import { ref, onMounted, computed } from 'vue'
import { Modal } from 'bootstrap'
import { apiService } from '@/composables/useApi'
import { toast } from '@shared/composables/useToast'

const groups = ref([])
const selectedGroup = ref(null)
const selectedGroupMembers = ref([])
const keyword = ref('')
const statusFilter = ref('')

const groupForm = ref({ groupId: '', groupName: '', groupCode: '', isActive: true })
const memberForm = ref({ userId: '', displayName: '', isAdmin: false })
const isEditingGroup = ref(false)
const isEditingMember = ref(false)

const activeGroupCount = computed(() => groups.value.filter(group => group.isActive).length)
const filteredGroups = computed(() => {
  const normalizedKeyword = keyword.value.trim().toLowerCase()
  return groups.value.filter(group => {
    const hitKeyword = !normalizedKeyword || [group.groupName, group.groupId, group.groupCode]
      .filter(Boolean)
      .some(value => String(value).toLowerCase().includes(normalizedKeyword))

    const hitStatus = !statusFilter.value
      || (statusFilter.value === 'active' && group.isActive)
      || (statusFilter.value === 'inactive' && !group.isActive)

    return hitKeyword && hitStatus
  })
})

let groupModal = null
let memberModal = null

onMounted(async () => {
  groupModal = new Modal(document.getElementById('groupModal'))
  memberModal = new Modal(document.getElementById('memberModal'))
  await fetchGroups(false)
})

const formatDate = (dateStr) => {
  if (!dateStr) return '-'
  return new Date(dateStr).toLocaleString()
}

const fetchGroups = async (preserveSelection = true) => {
  try {
    const data = await apiService.getLineGroups()
    groups.value = Array.isArray(data) ? data : []

    const selectedGroupId = selectedGroup.value?.groupId
    const nextSelectedGroup = preserveSelection && selectedGroupId
      ? groups.value.find(group => group.groupId === selectedGroupId)
      : null

    if (nextSelectedGroup) {
      await selectGroup(nextSelectedGroup)
      return
    }

    if (groups.value.length > 0) {
      await selectGroup(groups.value[0])
    } else {
      selectedGroup.value = null
      selectedGroupMembers.value = []
    }
  } catch (error) {
    console.error('Failed to fetch groups', error)
    toast.error('載入群組失敗')
  }
}

const selectGroup = async (group) => {
  selectedGroup.value = group
  try {
    const members = await apiService.getLineGroupMembers(group.groupId)
    selectedGroupMembers.value = Array.isArray(members) ? members : []
  } catch (error) {
    console.error('Failed to fetch members', error)
    selectedGroupMembers.value = []
    toast.error('載入成員失敗')
  }
}

const openCreateGroupModal = () => {
  isEditingGroup.value = false
  groupForm.value = { groupId: '', groupName: '', groupCode: '', isActive: true }
  groupModal.show()
}

const editGroup = (group) => {
  isEditingGroup.value = true
  groupForm.value = {
    groupId: group.groupId,
    groupName: group.groupName || '',
    groupCode: group.groupCode || '',
    isActive: !!group.isActive
  }
  groupModal.show()
}

const saveGroup = async () => {
  try {
    if (isEditingGroup.value) {
      await apiService.updateLineGroup(groupForm.value.groupId, groupForm.value)
    } else {
      await apiService.createLineGroup(groupForm.value)
    }

    groupModal.hide()
    toast.success(isEditingGroup.value ? '群組已更新' : '群組已新增')
    await fetchGroups(true)
  } catch (error) {
    console.error('Failed to save group', error)
    toast.error('儲存群組失敗: ' + (error.message || '未知錯誤'))
  }
}

const deleteGroup = async (group) => {
  if (!confirm(`確定要刪除群組 ${group.groupName || group.groupId} 嗎？此操作無法復原。`)) return
  try {
    await apiService.deleteLineGroup(group.groupId)
    toast.success('群組已刪除')
    await fetchGroups(true)
  } catch (error) {
    console.error('Failed to delete group', error)
    toast.error('刪除群組失敗')
  }
}

const openAddMemberModal = () => {
  if (!selectedGroup.value) {
    toast.error('請先選取群組')
    return
  }

  isEditingMember.value = false
  memberForm.value = { userId: '', displayName: '', isAdmin: false }
  memberModal.show()
}

const editMember = (member) => {
  isEditingMember.value = true
  memberForm.value = { ...member }
  memberModal.show()
}

const saveMember = async () => {
  if (!selectedGroup.value) return

  try {
    if (isEditingMember.value) {
      await apiService.updateLineGroupMember(selectedGroup.value.groupId, memberForm.value.id, memberForm.value)
    } else {
      await apiService.addLineGroupMember(selectedGroup.value.groupId, memberForm.value)
    }

    memberModal.hide()
    toast.success(isEditingMember.value ? '成員已更新' : '成員已新增')
    await fetchGroups(true)
  } catch (error) {
    console.error('Failed to save member', error)
    toast.error('儲存成員失敗: ' + (error.message || '未知錯誤'))
  }
}

const deleteMember = async (member) => {
  if (!selectedGroup.value) return
  if (!confirm(`確定要從群組中移除 ${member.displayName || member.userId} 嗎？`)) return

  try {
    await apiService.deleteLineGroupMember(selectedGroup.value.groupId, member.id)
    toast.success('成員已移除')
    await fetchGroups(true)
  } catch (error) {
    console.error('Failed to delete member', error)
    toast.error('移除成員失敗')
  }
}
</script>

<style scoped>
.line-groups-page {
  display: grid;
  gap: 20px;
  padding: 12px 4px 28px;
}

.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 14px;
  padding: 12px 14px;
  border-radius: 16px;
  background: rgba(255, 255, 255, 0.92);
  border: 1px solid rgba(2, 6, 23, 0.08);
  box-shadow: 0 14px 32px rgba(15, 23, 42, 0.06);
}

.page-header h2 {
  margin: 0 0 6px;
}

.page-header p {
  margin: 0;
  color: var(--muted-text, #667085);
}

.workspace-grid {
  display: grid;
  grid-template-columns: 1.08fr 0.92fr;
  gap: 18px;
}

.card.surface-card {
  padding: 14px;
}

.card.surface-card .card__body {
  padding: 0;
}

.overview-strip {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
  gap: 12px;
}

.overview-card {
  padding: 12px;
  border-radius: 14px;
  border: 1px solid rgba(2, 6, 23, 0.08);
  background: rgba(255, 255, 255, 0.92);
  box-shadow: 0 12px 26px rgba(15, 23, 42, 0.06);
}

.overview-card span {
  display: block;
  font-size: 12px;
  letter-spacing: 0.08em;
  text-transform: uppercase;
  color: rgba(15, 23, 42, 0.6);
  font-weight: 800;
}

.overview-card strong {
  display: block;
  margin: 4px 0 6px;
  font-size: 24px;
  letter-spacing: -0.02em;
}

.overview-card p {
  margin: 0;
  color: rgba(15, 23, 42, 0.62);
  font-weight: 600;
}

.overview-card--accent {
  background: linear-gradient(140deg, rgba(15, 23, 42, 0.96), rgba(29, 78, 216, 0.92));
}

.overview-card--accent span,
.overview-card--accent strong,
.overview-card--accent p {
  color: #fff;
}

.overview-card--accent p {
  color: rgba(255, 255, 255, 0.76);
}

.panel-head,
.toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.panel-head {
  margin-bottom: 14px;
}

.panel-head h3 {
  margin: 0;
}

.panel-head p {
  margin: 6px 0 0;
  color: var(--muted-text, #667085);
}

.toolbar {
  margin-bottom: 12px;
  padding: 0 4px;
  flex-wrap: wrap;
  row-gap: 8px;
}

.select-small {
  max-width: 160px;
}

.table-wrap {
  overflow-x: auto;
  border: 1px solid rgba(2, 6, 23, 0.08);
  border-radius: 14px;
  background: rgba(255, 255, 255, 0.92);
  box-shadow: 0 14px 30px rgba(15, 23, 42, 0.05);
  padding: 6px;
}

table {
  width: 100%;
  border-collapse: separate;
  border-spacing: 0 6px;
}

th, td {
  padding: 10px 12px;
  vertical-align: middle;
}

th {
  font-size: 13px;
  color: rgba(15, 23, 42, 0.6);
  font-weight: 800;
  letter-spacing: 0.02em;
}

tbody tr.is-selected {
  background: rgba(37, 99, 235, 0.08);
}

.status-badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 56px;
  padding: 4px 10px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 700;
}

.status-active {
  color: #15803d;
  background: rgba(22, 163, 74, 0.16);
}

.status-inactive {
  color: #64748b;
  background: rgba(148, 163, 184, 0.22);
}

.table-actions {
  display: flex;
  align-items: center;
  gap: 6px;
}

.table-actions .btn {
  min-width: 46px;
  height: 36px;
  padding: 6px 10px;
  font-size: 13px;
}

.empty-state {
  border: 1px dashed rgba(2, 6, 23, 0.16);
  background: rgba(255, 255, 255, 0.72);
  border-radius: 14px;
  padding: 28px 18px;
  text-align: center;
  color: rgba(2, 6, 23, 0.7);
  box-shadow: 0 10px 24px rgba(15, 23, 42, 0.06);
}

.empty-state p {
  margin: 0;
  font-weight: 800;
}

.line-modal {
  border-radius: 20px;
  box-shadow: 0 24px 70px rgba(15, 23, 42, 0.18);
  overflow: hidden;
}

.line-modal .modal-head,
.line-modal .modal-actions {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 18px 22px;
}

.line-modal .modal-head {
  border-bottom: 1px solid rgba(15, 23, 42, 0.08);
}

.line-modal .modal-head h3 {
  margin: 0;
  font-size: 1.05rem;
  font-weight: 800;
}

.line-modal .modal-close {
  border: 0;
  background: transparent;
  font-size: 28px;
  line-height: 1;
  cursor: pointer;
}

.line-modal-body {
  padding: 18px;
  display: grid;
  gap: 14px;
}

.field {
  display: grid;
  gap: 6px;
}

.field span {
  font-size: 14px;
  color: #344054;
}

.checkbox-field {
  grid-template-columns: auto 1fr;
  align-items: center;
}

.line-modal .modal-actions {
  border-top: 1px solid rgba(15, 23, 42, 0.08);
  justify-content: flex-end;
}

@media (max-width: 1100px) {
  .workspace-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 720px) {
  .toolbar {
    flex-direction: column;
    align-items: stretch;
  }

  .select-small {
    max-width: none;
    width: 100%;
  }

  th, td {
    padding: 10px;
  }

  table {
    display: block;
    overflow-x: auto;
  }
}
</style>
