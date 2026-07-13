<template>
  <el-card class="hotel-card" shadow="hover" @click="$router.push(`/hotel/${hotel.id}`)">
    <div class="card-body">
      <div class="card-info">
        <h3 class="hotel-name">{{ hotel.nameCn }}</h3>
        <p class="hotel-en" v-if="hotel.nameEn">{{ hotel.nameEn }}</p>
        <StarRating :rating="hotel.starLevel" />
        <p class="hotel-address" v-if="hotel.address">{{ hotel.address }}</p>
        <p class="brand" v-if="hotel.brand">{{ hotel.brand }}</p>
      </div>
      <div class="card-right">
        <div class="score" v-if="hotel.score">
          <span class="score-num">{{ hotel.score }}</span>
          <span class="score-label">分</span>
        </div>
        <div class="price" v-if="hotel.minPrice">
          <span class="price-symbol">¥</span>
          <span class="price-num">{{ hotel.minPrice }}</span>
          <span class="price-label">起</span>
        </div>
      </div>
    </div>
  </el-card>
</template>

<script setup>
import StarRating from './StarRating.vue'

defineProps({
  hotel: {
    type: Object,
    required: true
  }
})
</script>

<style scoped>
.hotel-card { cursor: pointer; margin-bottom: 12px; transition: all 0.3s; }
.hotel-card:hover { transform: translateY(-2px); }
.card-body { display: flex; justify-content: space-between; }
.hotel-name { font-size: 16px; color: #303133; margin-bottom: 2px; }
.hotel-en { font-size: 12px; color: #909399; margin-bottom: 4px; }
.hotel-address { font-size: 13px; color: #606266; margin-top: 6px; }
.brand { font-size: 12px; color: #C0C4CC; margin-top: 4px; }
.card-right { text-align: right; }
.score-num { font-size: 22px; color: #E6A23C; font-weight: bold; }
.score-label { font-size: 12px; color: #909399; }
.price { margin-top: 8px; }
.price-symbol { font-size: 14px; color: #F56C6C; }
.price-num { font-size: 22px; color: #F56C6C; font-weight: bold; }
.price-label { font-size: 12px; color: #909399; }
</style>
