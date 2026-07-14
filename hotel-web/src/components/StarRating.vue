<script setup>
defineProps({
  rating: {
    type: Number,
    default: 0
  },
  size: {
    type: Number,
    default: 16
  }
})
</script>

<template>
  <div class="star-rating" :style="{ fontSize: `${size || 16}px` }">
    <span class="stars">
      <span v-for="index in 5" :key="index" :class="index <= (rating || 0) ? 'filled' : 'empty'">★</span>
    </span>
    <span v-if="rating" class="score-text">{{ rating.toFixed(1) }}</span>
  </div>
</template>

<style scoped>
.star-rating {
  display: inline-flex;
  align-items: center;
  gap: 8px;
}

.stars {
  display: inline-flex;
  letter-spacing: 0.08em;
}

.filled {
  color: var(--brand-secondary);
  text-shadow: 0 2px 10px rgba(184, 149, 103, 0.26);
}

.empty {
  color: rgba(109, 90, 75, 0.14);
}

.score-text {
  font-size: 12px;
  color: var(--text-muted);
}
</style>
