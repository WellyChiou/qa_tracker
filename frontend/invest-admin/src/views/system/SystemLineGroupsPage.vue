<template>
  <AdminLayout>
    <div class="line-groups-page">
      <div class="page-header">
        <div>
          <h2>LINE 群組管理</h2>
          <p>管理 invest 通知群組、群組代碼與成員名單，維持 invest 系統自己的 LINE ownership。</p>
        </div>
        <button class="btn btn-primary" :disabled="!canEditLineGroups" @click="openGroupModal()">+ 新增群組</button>
      </div>

      <section class="overview-strip">
        <article class="overview-card overview-card--accent">
          <span>群組總數</span>
          <strong>{{ groups.length }}</strong>
          <p>目前 invest 後台可管理的 LINE 群組數量。</p>
        </article>
        <article class="overview-card">
          <span>啟用群組</span>
          <strong>{{ activeGroupCount }}</strong>
          <p>目前仍在啟用中的通知群組。</p>
        </article>
        <article class="overview-card">
          <span>目前檢視</span>
          <strong>{{ selectedGroup ? selectedGroup.groupName || selectedGroup.groupId : '未選取' }}</strong>
          <p>右側成員清單會跟著目前選取群組切換。</p>
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
                  <td>{{ group.memberCount || 0 }}</td>
                  <td>
                    <span :class="['status-badge', group.isActive ? 'status-active' : 'status-inactive']">
                      {{ group.isActive ? '啟用' : '停用' }}
                    </span>
                  </td>
                  <td>
                    <div class="table-actions">
                      <button class="btn btn-edit" :disabled="!canEditLineGroups" @click.stop="openGroupModal(group)">編輯</button>
                      <button class="btn btn-delete" :disabled="!canEditLineGroups" @click.stop="removeGroup(group)">刪除</button>
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
            <button class="btn btn-primary" :disabled="!selectedGroup || !canEditLineGroups" @click="openMemberModal()">+ 新增成員</button>
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
                  <th>管理員</th>
                  <th>加入時間</th>
                  <th>操作</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="member in members" :key="member.id">
                  <td>{{ member.displayName || '-' }}</td>
                  <td>{{ member.isAdmin ? '是' : '否' }}</td>
                  <td>{{ formatDateTime(member.joinedAt) }}</td>
                  <td>
                    <div class="table-actions">
                      <button class="btn btn-edit" :disabled="!canEditLineGroups" @click="openMemberModal(member)">編輯</button>
                      <button class="btn btn-delete" :disabled="!canEditLineGroups" @click="removeMember(member)">移除</button>
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
              <input v-model.trim="groupForm.groupCode" class="form-input" placeholder="例如 INVEST_ALERT_GROUP" />
            </label>
            <label class="field checkbox-field">
              <input v-model="groupForm.isActive" type="checkbox" />
              <span>啟用此群組</span>
            </label>
          </div>
          <div class="modal-actions">
            <button class="btn btn-secondary" @click="closeGroupModal">取消</button>
            <button class="btn btn-primary" :disabled="!canEditLineGroups" @click="saveGroup">儲存</button>
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
            <button class="btn btn-primary" :disabled="!canEditLineGroups" @click="saveMember">儲存</button>
          </div>
        </div>
      </div>
    </div>
  </AdminLayout>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { toast } from '@shared/composables/useToast'
import { hasPermission } from '@shared/utils/permission'
import AdminLayout from '@/components/AdminLayout.vue'
import { useAuth } from '@/composables/useAuth'
import { investApiService } from '@/composables/useInvestApi'

const { currentUser } = useAuth()
const canEditLineGroups = computed(() => hasPermission(currentUser.value, 'INVEST_SYS_LINE_GROUPS_EDIT'))

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
  if (!value) {
    return '-'
  }

  return new Date(value).toLocaleString('zh-TW', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  })
}

async function loadGroups(preserveSelection = true) {
  const data = await investApiService.getSystemLineGroups()
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
  const data = await investApiService.getSystemLineGroupMembers(group.groupId)
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

  try {
    if (editingGroup.value) {
      await investApiService.updateSystemLineGroup(editingGroup.value.groupId, groupForm.value)
      toast.success('群組更新成功')
    } else {
      await investApiService.createSystemLineGroup(groupForm.value)
      toast.success('群組建立成功')
    }

    closeGroupModal()
    await loadGroups(true)
  } catch (error) {
    toast.error(error.message || '儲存群組失敗')
  }
}

async function removeGroup(group) {
  if (!window.confirm(`確定要刪除群組「${group.groupName || group.groupId}」嗎？`)) {
    return
  }

  try {
    await investApiService.deleteSystemLineGroup(group.groupId)
    toast.success('群組刪除成功')
    await loadGroups(false)
  } catch (error) {
    toast.error(error.message || '刪除群組失敗')
  }
}

function openMemberModal(member = null) {
  if (!selectedGroup.value) {
    return
  }

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
  if (!selectedGroup.value) {
    return
  }
  if (!memberForm.value.userId) {
    toast.error('LINE User ID 不能為空')
    return
  }

  try {
    if (editingMember.value) {
      await investApiService.updateSystemLineGroupMember(
        selectedGroup.value.groupId,
        editingMember.value.id,
        memberForm.value
      )
      toast.success('成員更新成功')
    } else {
      await investApiService.addSystemLineGroupMember(selectedGroup.value.groupId, memberForm.value)
      toast.success('成員新增成功')
    }

    closeMemberModal()
    await loadGroups(true)
  } catch (error) {
    toast.error(error.message || '儲存成員失敗')
  }
}

async function removeMember(member) {
  if (!selectedGroup.value) {
    return
  }
  if (!window.confirm(`確定要移除成員「${member.displayName || member.userId}」嗎？`)) {
    return
  }

  try {
    await investApiService.deleteSystemLineGroupMember(selectedGroup.value.groupId, member.id)
    toast.success('成員移除成功')
    await loadGroups(true)
  } catch (error) {
    toast.error(error.message || '移除成員失敗')
  }
}

onMounted(async () => {
  try {
    await loadGroups(false)
  } catch (error) {
    toast.error(error.message || '載入 LINE 群組失敗')
  }
})
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

th,
td {
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
  border: 1px dashed rgba(148, 163, 184, 0.4);
  border-radius: 12px;
  padding: 22px;
  text-align: center;
  color: #64748b;
  background: rgba(248, 250, 252, 0.8);
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
  font-size: 24px;
  cursor: pointer;
}

.modal-body {
  padding: 18px 22px;
  display: grid;
  gap: 10px;
}

.field {
  display: grid;
  gap: 6px;
}

.field > span {
  font-size: 13px;
  color: rgba(15, 23, 42, 0.72);
  font-weight: 700;
}

.form-input {
  width: 100%;
  padding: 10px 12px;
  border-radius: 10px;
  border: 1px solid rgba(148, 163, 184, 0.5);
  background: rgba(255, 255, 255, 0.92);
  color: #0f172a;
}

.checkbox-field {
  display: flex;
  align-items: center;
  gap: 8px;
}

@media (max-width: 1080px) {
  .workspace-grid {
    grid-template-columns: 1fr;
  }
}
</style>
