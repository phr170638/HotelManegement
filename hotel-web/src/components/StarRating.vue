<template>
  <span class="star-rating" :style="{ fontSize: size + 'px' }">
    <span v-for="i in 5" :key="i" :class="i <= rating ? 'filled' : 'empty'">★</span>
  </span>
</template>

<script setup>
defineProps({
  rating: { type: Number, default: 0 },
  size: { type: Number, default: 16 }
})
</script>

<style scoped>
.star-rating { display: inline-flex; }
.filled { color: #E6A23C; }
.empty { color: #DCDFE6; }
</style>
