<script setup>
import { computed } from 'vue'
import { getOrderStatusMeta } from '@/utils/order'

const props = defineProps({
  status: {
    type: Number,
    default: undefined
  },
  text: {
    type: String,
    default: ''
  }
})

const meta = computed(() => getOrderStatusMeta(props.status))
const label = computed(() => props.text || meta.value.label)
</script>

<template>
  <span :class="['status-badge', `status-badge--${meta.tone}`]">
    {{ label }}
  </span>
</template>

<style scoped>
.status-badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 84px;
  padding: 7px 12px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.02em;
}

.status-badge--warning {
  color: #8b5b1d;
  background: rgba(217, 130, 43, 0.16);
}

.status-badge--success {
  color: #216845;
  background: rgba(46, 139, 87, 0.16);
}

.status-badge--info {
  color: #54606d;
  background: rgba(88, 102, 117, 0.16);
}

.status-badge--danger {
  color: #9f3935;
  background: rgba(194, 79, 74, 0.16);
}
</style>
