<template>
  <span
    :class="['truncated-text', { 'truncated-text--expandable': expandable && needsTruncation }]"
    :title="expandable && needsTruncation ? text : undefined"
    @mouseenter="handleMouseEnter"
    @mouseleave="handleMouseLeave"
  >
    {{ displayText }}
  </span>
</template>

<script setup>
import { ref, computed } from 'vue'

const props = defineProps({
  text: {
    type: String,
    default: ''
  },
  maxLength: {
    type: Number,
    default: 50
  },
  expandable: {
    type: Boolean,
    default: true
  },
  expandWidth: {
    type: String,
    default: '400px'
  }
})

const isHovered = ref(false)

const needsTruncation = computed(() => {
  return props.text && props.text.length > props.maxLength
})

const handleMouseEnter = () => {
  if (props.expandable && needsTruncation.value) {
    isHovered.value = true
  }
}

const handleMouseLeave = () => {
  isHovered.value = false
}

const displayText = computed(() => {
  if (!props.text) return '-'
  // 如果不需要截斷，直接顯示完整文字
  if (!props.expandable || !needsTruncation.value) {
    return props.text
  }
  // 如果需要截斷，顯示截斷後的文字（但實際顯示會由 CSS 控制）
  return props.text
})
</script>

<style scoped>
.truncated-text {
  display: block;
}

/* 只有在需要截斷且可展開時才應用這些樣式 */
.truncated-text--expandable {
  display: block;
  max-width: 100%;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  position: relative;
  cursor: help;
  word-wrap: break-word;
  overflow-wrap: break-word;
}

/* 當 hover 時展開顯示完整內容 */
.truncated-text--expandable:hover {
  white-space: normal;
  overflow: visible;
  z-index: 10;
  background: white;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.2);
  padding: 1rem;
  border-radius: 0.5rem;
  max-width: v-bind(expandWidth);
  word-wrap: break-word;
  overflow-wrap: break-word;
  position: absolute;
  left: 0;
  top: 0;
  min-width: 200px;
  width: max-content;
}
</style>
