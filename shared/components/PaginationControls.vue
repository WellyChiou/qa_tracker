<template>
  <div class="pagination">
    <div class="pagination-left">
      <label :for="pageSizeId" class="pagination-label">顯示筆數：</label>
      <select
        :id="pageSizeId"
        :value="pageSize"
        class="page-size-select"
        @change="handlePageSizeChange"
      >
        <option
          v-for="size in pageSizeOptions"
          :key="size"
          :value="size"
        >
          {{ size }}
        </option>
      </select>
      <span class="pagination-info">
        共 {{ totalRecords }} 筆 (第 {{ currentPage }}/{{ totalPages }} 頁)
      </span>
    </div>
    <div class="pagination-right">
      <button :class="buttonClass" :disabled="currentPage === 1" @click="$emit('first')">第一頁</button>
      <button :class="buttonClass" :disabled="currentPage === 1" @click="$emit('previous')">上一頁</button>
      <div class="page-jump">
        <span class="pagination-label">到第</span>
        <input
          type="number"
          class="page-input"
          :value="jumpPage"
          min="1"
          :max="totalPages"
          @input="handleJumpPageInput"
          @keyup.enter="$emit('jump')"
        />
        <span class="pagination-label">頁</span>
      </div>
      <button :class="buttonClass" :disabled="currentPage === totalPages" @click="$emit('next')">下一頁</button>
      <button :class="buttonClass" :disabled="currentPage === totalPages" @click="$emit('last')">最後頁</button>
    </div>
  </div>
</template>

<script setup>
defineProps({
  pageSize: {
    type: Number,
    required: true
  },
  jumpPage: {
    type: [Number, String],
    required: true
  },
  totalRecords: {
    type: Number,
    required: true
  },
  currentPage: {
    type: Number,
    required: true
  },
  totalPages: {
    type: Number,
    required: true
  },
  pageSizeOptions: {
    type: Array,
    default: () => [10, 20, 50, 100]
  },
  pageSizeId: {
    type: String,
    default: 'pageSize'
  },
  buttonClass: {
    type: String,
    default: 'btn btn-secondary'
  }
})

const emit = defineEmits([
  'update:pageSize',
  'update:jumpPage',
  'first',
  'previous',
  'next',
  'last',
  'jump'
])

const handlePageSizeChange = (event) => {
  emit('update:pageSize', Number(event.target.value))
}

const handleJumpPageInput = (event) => {
  emit('update:jumpPage', Number(event.target.value))
}
</script>
