<template>
  <AdminLayout>
    <div class="admin-persons">
      <div class="page-header">
        <h2>人員管理</h2>
        <button @click="openAddModal" class="btn btn-primary">+ 新增人員</button>
      </div>

      <div class="persons-list">
        <div v-if="persons.length === 0" class="empty-state">
          <p>尚無人員資料</p>
        </div>
        <div v-else class="persons-table">
          <table>
            <thead>
              <tr>
                <th>姓名</th>
                <th>顯示名稱</th>
                <th>電話</th>
                <th>電子郵件</th>
                <th>操作</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="person in persons" :key="person.id">
                <td>{{ person.personName }}</td>
                <td>{{ person.displayName || '-' }}</td>
                <td>{{ person.phone || '-' }}</td>
                <td>{{ person.email || '-' }}</td>
                <td>
                  <button @click="editPerson(person.id)" class="btn btn-edit">編輯</button>
                  <button @click="deletePerson(person.id)" class="btn btn-delete">刪除</button>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>

      <!-- 新增人員 Modal -->
      <PersonManagementModal
        v-if="showModal && !editingPerson"
        :show="showModal"
        @close="closeModal"
        @saved="handleSaved"
      />

      <!-- 編輯人員 Modal -->
      <EditPersonModal
        v-if="editingPerson"
        :show="!!editingPerson"
        :person="editingPerson"
        @close="closeEditModal"
        @updated="handleUpdated"
      />
    </div>
  </AdminLayout>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import AdminLayout from '@/components/AdminLayout.vue'
import PersonManagementModal from '@/components/PersonManagementModal.vue'
import EditPersonModal from '@/components/EditPersonModal.vue'
import { apiRequest } from '@/utils/api'

const persons = ref([])
const showModal = ref(false)
const editingPerson = ref(null)

const loadPersons = async () => {
  try {
    const response = await apiRequest('/church/persons', {
      method: 'GET',
      credentials: 'include'
    })
    
    if (response.ok) {
      const data = await response.json()
      // 後端返回格式：{ "persons": [...], "message": "..." }
      persons.value = data.persons || data || []
    }
  } catch (error) {
    console.error('載入人員失敗:', error)
  }
}

const openAddModal = () => {
  showModal.value = true
}

const editPerson = (id) => {
  // 找到要編輯的人員
  const person = persons.value.find(p => p.id === id)
  if (person) {
    editingPerson.value = person
  }
}

const closeEditModal = () => {
  editingPerson.value = null
}

const handleUpdated = () => {
  loadPersons()
  closeEditModal()
}

const closeModal = () => {
  showModal.value = false
}

const handleSaved = () => {
  loadPersons()
  closeModal()
}

const deletePerson = async (id) => {
  if (!confirm('確定要刪除此人員嗎？')) {
    return
  }
  
  try {
    const response = await apiRequest(`/church/persons/${id}`, {
      method: 'DELETE',
      credentials: 'include'
    })
    
    if (response.ok) {
      loadPersons()
    } else {
      alert('刪除失敗')
    }
  } catch (error) {
    console.error('刪除人員失敗:', error)
    alert('刪除失敗: ' + error.message)
  }
}

onMounted(() => {
  loadPersons()
})
</script>

<style scoped>
.admin-persons {
  max-width: 1400px;
  margin: 0 auto;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 2rem;
}

.page-header h2 {
  margin: 0;
  font-size: 1.8rem;
  color: #333;
}

.btn {
  padding: 0.75rem 1.5rem;
  border: none;
  border-radius: 8px;
  font-size: 1rem;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s;
}

.btn-primary {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
}

.btn-primary:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(102, 126, 234, 0.3);
}

.persons-list {
  background: white;
  border-radius: 12px;
  padding: 2rem;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.empty-state {
  text-align: center;
  padding: 3rem;
  color: #666;
}

.persons-table {
  overflow-x: auto;
}

table {
  width: 100%;
  border-collapse: collapse;
}

thead {
  background: #f5f5f5;
}

th, td {
  padding: 1rem;
  text-align: left;
  border-bottom: 1px solid #e0e0e0;
}

th {
  font-weight: 600;
  color: #333;
}

.btn-edit {
  background: #667eea;
  color: white;
  padding: 0.5rem 1rem;
  margin-right: 0.5rem;
}

.btn-edit:hover {
  background: #5568d3;
}

.btn-delete {
  background: #ef4444;
  color: white;
  padding: 0.5rem 1rem;
}

.btn-delete:hover {
  background: #dc2626;
}
</style>

