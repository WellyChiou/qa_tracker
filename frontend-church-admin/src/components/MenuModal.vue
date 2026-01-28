<template>
  <div v-if="show" class="modal-overlay" @click.self="close">
    <div class="modal-content">
      <div class="modal-header">
        <h3>{{ isEdit ? '編輯菜單' : '新增菜單' }}</h3>
        <button @click="close" class="close-btn">×</button>
      </div>
      
      <form @submit.prevent="handleSubmit" class="modal-body">
        <div class="form-group">
          <label>菜單代碼 <span class="required">*</span></label>
          <input
            v-model="formData.menuCode"
            type="text"
            required
            :disabled="isEdit"
            placeholder="例如: ADMIN_DASHBOARD"
          />
        </div>
        
        <div class="form-group">
          <label>菜單名稱 <span class="required">*</span></label>
          <input
            v-model="formData.menuName"
            type="text"
            required
            placeholder="例如: 儀表板"
          />
        </div>
        
        <div class="form-row">
          <div class="form-group">
            <label>圖標</label>
            <input
              v-model="formData.icon"
              type="text"
              placeholder="例如: 📊"
            />
          </div>
          
          <div class="form-group">
            <label>排序</label>
            <input
              v-model.number="formData.orderIndex"
              type="number"
              min="0"
              placeholder="0"
            />
          </div>
        </div>
        
        <div class="form-group">
          <label>URL</label>
          <input
            v-model="formData.url"
            type="text"
            placeholder="例如: /admin"
          />
        </div>
        
        <div class="form-row">
          <div class="form-group">
            <label>菜單類型</label>
            <select v-model="formData.menuType">
              <option value="frontend">前台</option>
              <option value="admin">後台</option>
            </select>
          </div>
          
          <div class="form-group">
            <label>父菜單</label>
            <select v-model="formData.parentId">
              <option :value="null">無（根菜單）</option>
              <option v-for="menu in availableMenus" :key="menu.id" :value="menu.id">
                {{ menu.menuName }}
              </option>
            </select>
          </div>
        </div>
        
        <div class="form-group">
          <label>所需權限</label>
          <input
            v-model="formData.requiredPermission"
            type="text"
            placeholder="例如: CHURCH_ADMIN（留空表示無需權限）"
          />
        </div>
        
        <div class="form-group">
          <label>描述</label>
          <textarea
            v-model="formData.description"
            rows="3"
            placeholder="請輸入菜單描述"
          ></textarea>
        </div>
        
        <div class="form-row">
        <div class="form-group">
          <label>狀態</label>
          <div class="radio-group">
            <label class="radio-label">
              <input
                type="radio"
                :value="true"
                v-model="formData.isActive"
              />
              <span>啟用</span>
            </label>
            <label class="radio-label">
              <input
                type="radio"
                :value="false"
                v-model="formData.isActive"
              />
              <span>停用</span>
            </label>
            </div>
          </div>
          
          <div class="form-group" v-if="formData.menuType === 'admin' && !formData.parentId">
            <label>顯示在儀表板</label>
            <div class="checkbox-group">
              <label class="checkbox-label">
                <input
                  type="checkbox"
                  v-model="formData.showInDashboard"
                />
                <span>在儀表板快速操作中顯示</span>
              </label>
              <small class="form-hint">僅根菜單（無父菜單）可顯示在儀表板</small>
            </div>
          </div>
        </div>
        
        <div v-if="error" class="error-message">{{ error }}</div>
        
        <div class="modal-footer">
          <button type="button" @click="close" class="btn btn-cancel">取消</button>
          <button type="submit" :disabled="loading" class="btn btn-submit">
            {{ loading ? '處理中...' : (isEdit ? '更新' : '新增') }}
          </button>
        </div>
      </form>
    </div>
  </div>
</template>

<script setup>
import { ref, watch } from 'vue'
import { apiRequest } from '@/utils/api'

const props = defineProps({
  show: Boolean,
  menu: Object,
  availableMenus: {
    type: Array,
    default: () => []
  }
})

const emit = defineEmits(['close', 'saved'])

const isEdit = ref(false)
const loading = ref(false)
const error = ref('')

const formData = ref({
  menuCode: '',
  menuName: '',
  icon: '',
  url: '',
  parentId: null,
  orderIndex: 0,
  menuType: 'frontend',
  requiredPermission: '',
  description: '',
  isActive: true,
  showInDashboard: true
})

watch(() => props.show, (newVal) => {
  if (newVal) {
    if (props.menu) {
      isEdit.value = true
      formData.value = {
        menuCode: props.menu.menuCode || '',
        menuName: props.menu.menuName || '',
        icon: props.menu.icon || '',
        url: props.menu.url || '',
        parentId: props.menu.parentId || null,
        orderIndex: props.menu.orderIndex || 0,
        menuType: props.menu.menuType || 'frontend',
        requiredPermission: props.menu.requiredPermission || '',
        description: props.menu.description || '',
        isActive: props.menu.isActive !== false,
        showInDashboard: props.menu.showInDashboard !== false
      }
    } else {
      isEdit.value = false
      formData.value = {
        menuCode: '',
        menuName: '',
        icon: '',
        url: '',
        parentId: null,
        orderIndex: 0,
        menuType: 'frontend',
        requiredPermission: '',
        description: '',
        isActive: true,
        showInDashboard: true
      }
    }
    error.value = ''
  }
})

const close = () => {
  emit('close')
}

const handleSubmit = async () => {
  error.value = ''
  loading.value = true
  
  try {
    const url = isEdit.value 
      ? `/church/menus/${props.menu.id}`
      : '/church/menus'
    
    const method = isEdit.value ? 'PUT' : 'POST'
    
    const payload = {
      menuCode: formData.value.menuCode,
      menuName: formData.value.menuName,
      icon: formData.value.icon || null,
      url: formData.value.url || null,
      parentId: formData.value.parentId || null,
      orderIndex: formData.value.orderIndex || 0,
      menuType: formData.value.menuType,
      requiredPermission: formData.value.requiredPermission || null,
      description: formData.value.description || null,
      isActive: formData.value.isActive,
      showInDashboard: formData.value.menuType === 'admin' && !formData.value.parentId 
        ? formData.value.showInDashboard 
        : false
    }
    
    const data = await apiRequest(url, {
      method,
      body: JSON.stringify(payload),
      credentials: 'include'
    })
    
    if (data !== null) {
      emit('saved', data)
      close()
    } else {
      error.value = '操作失敗'
    }
  } catch (err) {
    error.value = err.message || '操作失敗'
  } finally {
    loading.value = false
  }
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
  background: white;
  border-radius: 12px;
  width: 90%;
  max-width: 700px;
  max-height: 90vh;
  overflow-y: auto;
  box-shadow: 0 10px 40px rgba(0, 0, 0, 0.2);
}

.modal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 1.5rem;
  border-bottom: 1px solid #e0e0e0;
}

.modal-header h3 {
  margin: 0;
  font-size: 1.5rem;
  color: #333;
}

.close-btn {
  background: none;
  border: none;
  font-size: 2rem;
  color: #666;
  cursor: pointer;
  padding: 0;
  width: 32px;
  height: 32px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 4px;
  transition: background 0.2s;
}

.close-btn:hover {
  background: #f0f0f0;
}

.modal-body {
  padding: 1.5rem;
}

.form-group {
  margin-bottom: 1.5rem;
}

.form-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 1rem;
}

.form-group label {
  display: block;
  margin-bottom: 0.5rem;
  font-weight: 600;
  color: #333;
}

.required {
  color: #ef4444;
}

.form-group input[type="text"],
.form-group input[type="number"],
.form-group select,
.form-group textarea {
  width: 100%;
  padding: 0.75rem;
  border: 2px solid #e0e0e0;
  border-radius: 8px;
  font-size: 1rem;
  transition: border-color 0.2s;
  font-family: inherit;
}

.form-group input:focus,
.form-group select:focus,
.form-group textarea:focus {
  outline: none;
  border-color: #667eea;
}

.radio-group {
  display: flex;
  gap: 1rem;
}

.radio-label {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  cursor: pointer;
  font-weight: normal;
}

.radio-label input {
  width: auto;
  margin: 0;
}

.checkbox-group {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.checkbox-label {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  cursor: pointer;
  font-weight: normal;
}

.checkbox-label input {
  width: auto;
  margin: 0;
}

.form-hint {
  display: block;
  color: #666;
  font-size: 0.875rem;
  margin-top: 0.25rem;
}

.error-message {
  background: #fee2e2;
  color: #ef4444;
  padding: 0.75rem;
  border-radius: 8px;
  margin-bottom: 1rem;
  border: 1px solid #ef4444;
}

.modal-footer {
  display: flex;
  justify-content: flex-end;
  gap: 1rem;
  margin-top: 2rem;
  padding-top: 1.5rem;
  border-top: 1px solid #e0e0e0;
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

.btn-cancel {
  background: #f0f0f0;
  color: #333;
}

.btn-cancel:hover {
  background: #e0e0e0;
}

.btn-submit {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
}

.btn-submit:hover:not(:disabled) {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(102, 126, 234, 0.3);
}

.btn-submit:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

/* Improved scroll shadows (prevents fixed shadow overlay issue) */
.modal-body{
  overflow:auto;
  background:
    linear-gradient(#fff 30%, rgba(255,255,255,0)),
    linear-gradient(rgba(255,255,255,0), #fff 70%),
    radial-gradient(farthest-side at 50% 0, rgba(2,6,23,.16), rgba(2,6,23,0)),
    radial-gradient(farthest-side at 50% 100%, rgba(2,6,23,.16), rgba(2,6,23,0));
  background-repeat:no-repeat;
  background-size:100% 40px, 100% 40px, 100% 14px, 100% 14px;
  background-attachment:local, local, scroll, scroll;
  background-position:0 0, 0 100%, 0 0, 0 100%;
}
</style>

