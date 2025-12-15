<template>
  <div class="admin-page">
    <TopNavbar />
    <div class="container-fluid px-4 py-4">
      <h1 class="mt-4">LINE 群組管理</h1>
      <ol class="breadcrumb mb-4">
        <li class="breadcrumb-item"><router-link to="/admin">管理首頁</router-link></li>
        <li class="breadcrumb-item active">LINE 群組管理</li>
      </ol>

      <!-- 群組列表 -->
    <div class="card mb-4" v-if="!currentGroupMembers">
      <div class="card-header">
        <div class="d-flex justify-content-between align-items-center">
          <div>
            <i class="fas fa-users me-1"></i>
            LINE 群組列表
          </div>
          <button class="btn btn-primary btn-sm" @click="openCreateGroupModal">
            <i class="fas fa-plus"></i> 新增群組
          </button>
        </div>
      </div>
      <div class="card-body">
        <table class="table table-bordered table-striped">
          <thead>
            <tr>
              <th>群組 ID</th>
              <th>群組名稱</th>
              <th>成員數</th>
              <th>狀態</th>
              <th>建立時間</th>
              <th>操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="group in groups" :key="group.groupId">
              <td>{{ group.groupId }}</td>
              <td>{{ group.groupName || '未命名' }}</td>
              <td>{{ group.memberCount }}</td>
              <td>
                <span :class="group.isActive ? 'badge bg-success' : 'badge bg-secondary'">
                  {{ group.isActive ? '啟用' : '停用' }}
                </span>
              </td>
              <td>{{ formatDate(group.createdAt) }}</td>
              <td>
                <button class="btn btn-info btn-sm me-2" @click="viewMembers(group)">
                  <i class="fas fa-user-friends"></i> 成員
                </button>
                <button class="btn btn-warning btn-sm me-2" @click="editGroup(group)">
                  <i class="fas fa-edit"></i> 編輯
                </button>
                <button class="btn btn-danger btn-sm" @click="deleteGroup(group)">
                  <i class="fas fa-trash"></i> 刪除
                </button>
              </td>
            </tr>
            <tr v-if="groups.length === 0">
              <td colspan="6" class="text-center">尚無群組資料</td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>

    <!-- 群組成員管理 -->
    <div class="card mb-4" v-else>
      <div class="card-header">
        <div class="d-flex justify-content-between align-items-center">
          <div>
            <i class="fas fa-users-cog me-1"></i>
            成員管理 - {{ currentGroup.groupName || currentGroup.groupId }}
          </div>
          <div>
            <button class="btn btn-secondary btn-sm me-2" @click="closeMembersView">
              <i class="fas fa-arrow-left"></i> 返回群組列表
            </button>
            <button class="btn btn-primary btn-sm" @click="openAddMemberModal">
              <i class="fas fa-plus"></i> 新增成員
            </button>
          </div>
        </div>
      </div>
      <div class="card-body">
        <table class="table table-bordered table-striped">
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
            <tr v-for="member in currentGroupMembers" :key="member.id">
              <td>{{ member.userId }}</td>
              <td>{{ member.displayName }}</td>
              <td>
                <span :class="member.isAdmin ? 'badge bg-primary' : 'badge bg-secondary'">
                  {{ member.isAdmin ? '是' : '否' }}
                </span>
              </td>
              <td>{{ formatDate(member.joinedAt) }}</td>
              <td>
                <button class="btn btn-warning btn-sm me-2" @click="editMember(member)">
                  <i class="fas fa-edit"></i> 編輯
                </button>
                <button class="btn btn-danger btn-sm" @click="deleteMember(member)">
                  <i class="fas fa-trash"></i> 移除
                </button>
              </td>
            </tr>
            <tr v-if="currentGroupMembers.length === 0">
              <td colspan="5" class="text-center">此群組尚無成員資料</td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>

    <!-- Group Modal -->
    <div class="modal fade" id="groupModal" tabindex="-1">
      <div class="modal-dialog">
        <div class="modal-content">
          <div class="modal-header">
            <h5 class="modal-title">{{ isEditingGroup ? '編輯群組' : '新增群組' }}</h5>
            <button type="button" class="btn-close" data-bs-dismiss="modal"></button>
          </div>
          <div class="modal-body">
            <form @submit.prevent="saveGroup">
              <div class="mb-3">
                <label class="form-label">群組 ID</label>
                <input type="text" class="form-control" v-model="groupForm.groupId" :disabled="isEditingGroup" required>
                <div class="form-text" v-if="!isEditingGroup">請輸入 LINE 群組 ID (通常以 C 開頭)</div>
              </div>
              <div class="mb-3">
                <label class="form-label">群組名稱</label>
                <input type="text" class="form-control" v-model="groupForm.groupName">
              </div>
              <div class="mb-3 form-check">
                <input type="checkbox" class="form-check-input" id="groupActive" v-model="groupForm.isActive">
                <label class="form-check-label" for="groupActive">啟用通知</label>
              </div>
            </form>
          </div>
          <div class="modal-footer">
            <button type="button" class="btn btn-secondary" data-bs-dismiss="modal">取消</button>
            <button type="button" class="btn btn-primary" @click="saveGroup">儲存</button>
          </div>
        </div>
      </div>
    </div>

    <!-- Member Modal -->
    <div class="modal fade" id="memberModal" tabindex="-1">
      <div class="modal-dialog">
        <div class="modal-content">
          <div class="modal-header">
            <h5 class="modal-title">{{ isEditingMember ? '編輯成員' : '新增成員' }}</h5>
            <button type="button" class="btn-close" data-bs-dismiss="modal"></button>
          </div>
          <div class="modal-body">
            <form @submit.prevent="saveMember">
              <div class="mb-3">
                <label class="form-label">User ID</label>
                <input type="text" class="form-control" v-model="memberForm.userId" :disabled="isEditingMember" required>
                <div class="form-text" v-if="!isEditingMember">請輸入 LINE User ID (通常以 U 開頭)</div>
              </div>
              <div class="mb-3">
                <label class="form-label">顯示名稱</label>
                <input type="text" class="form-control" v-model="memberForm.displayName">
              </div>
              <div class="mb-3 form-check">
                <input type="checkbox" class="form-check-input" id="memberAdmin" v-model="memberForm.isAdmin">
                <label class="form-check-label" for="memberAdmin">設定為管理員</label>
              </div>
            </form>
          </div>
          <div class="modal-footer">
            <button type="button" class="btn btn-secondary" data-bs-dismiss="modal">取消</button>
            <button type="button" class="btn btn-primary" @click="saveMember">儲存</button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue';
import { Modal } from 'bootstrap';
import TopNavbar from '@/components/TopNavbar.vue';
import { apiService } from '@/composables/useApi';

// Data
const groups = ref([]);
const currentGroup = ref(null);
const currentGroupMembers = ref(null);

// Form Data
const groupForm = ref({ groupId: '', groupName: '', isActive: true });
const memberForm = ref({ userId: '', displayName: '', isAdmin: false });
const isEditingGroup = ref(false);
const isEditingMember = ref(false);

// Modals
let groupModal = null;
let memberModal = null;

onMounted(() => {
  groupModal = new Modal(document.getElementById('groupModal'));
  memberModal = new Modal(document.getElementById('memberModal'));
  fetchGroups();
});

// Helper
const formatDate = (dateStr) => {
  if (!dateStr) return '-';
  return new Date(dateStr).toLocaleString();
};

// Group Operations
const fetchGroups = async () => {
  try {
    groups.value = await apiService.getLineGroups();
  } catch (error) {
    console.error('Failed to fetch groups', error);
    alert('載入群組失敗');
  }
};

const openCreateGroupModal = () => {
  isEditingGroup.value = false;
  groupForm.value = { groupId: '', groupName: '', isActive: true };
  groupModal.show();
};

const editGroup = (group) => {
  isEditingGroup.value = true;
  groupForm.value = { ...group };
  groupModal.show();
};

const saveGroup = async () => {
  try {
    if (isEditingGroup.value) {
      await apiService.updateLineGroup(groupForm.value.groupId, groupForm.value);
    } else {
      await apiService.createLineGroup(groupForm.value);
    }
    groupModal.hide();
    fetchGroups();
  } catch (error) {
    console.error('Failed to save group', error);
    alert('儲存群組失敗: ' + (error.message || '未知錯誤'));
  }
};

const deleteGroup = async (group) => {
  if (!confirm(`確定要刪除群組 ${group.groupName || group.groupId} 嗎？此操作無法復原。`)) return;
  try {
    await apiService.deleteLineGroup(group.groupId);
    fetchGroups();
  } catch (error) {
    console.error('Failed to delete group', error);
    alert('刪除群組失敗');
  }
};

// Member Operations
const viewMembers = async (group) => {
  currentGroup.value = group;
  try {
    currentGroupMembers.value = await apiService.getLineGroupMembers(group.groupId);
  } catch (error) {
    console.error('Failed to fetch members', error);
    alert('載入成員失敗');
  }
};

const closeMembersView = () => {
  currentGroupMembers.value = null;
  currentGroup.value = null;
  fetchGroups(); // Refresh groups to update member counts
};

const openAddMemberModal = () => {
  isEditingMember.value = false;
  memberForm.value = { userId: '', displayName: '', isAdmin: false };
  memberModal.show();
};

const editMember = (member) => {
  isEditingMember.value = true;
  memberForm.value = { ...member };
  memberModal.show();
};

const saveMember = async () => {
  if (!currentGroup.value) return;
  try {
    if (isEditingMember.value) {
      await apiService.updateLineGroupMember(currentGroup.value.groupId, memberForm.value.id, memberForm.value);
    } else {
      await apiService.addLineGroupMember(currentGroup.value.groupId, memberForm.value);
    }
    memberModal.hide();
    // Refresh members list
    currentGroupMembers.value = await apiService.getLineGroupMembers(currentGroup.value.groupId);
  } catch (error) {
    console.error('Failed to save member', error);
    alert('儲存成員失敗: ' + (error.message || '未知錯誤'));
  }
};

const deleteMember = async (member) => {
  if (!confirm(`確定要從群組中移除 ${member.displayName || member.userId} 嗎？`)) return;
  try {
    await apiService.deleteLineGroupMember(currentGroup.value.groupId, member.id);
    // Refresh members list
    currentGroupMembers.value = await apiService.getLineGroupMembers(currentGroup.value.groupId);
  } catch (error) {
    console.error('Failed to delete member', error);
    alert('移除成員失敗');
  }
};
</script>

<style scoped>
.admin-page {
  min-height: 100vh;
  background: #f5f5f5;
  padding-bottom: 2rem;
}

.badge {
  font-size: 0.9em;
}
</style>
