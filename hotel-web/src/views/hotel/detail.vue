<template>
  <div class="detail-page" v-loading="loading">
    <div class="container" v-if="hotel">
      <!-- 酒店基本信息 -->
      <el-card class="info-card">
        <div class="hotel-header">
          <div class="hotel-info">
            <h1>{{ hotel.nameCn }}</h1>
            <p class="en-name" v-if="hotel.nameEn">{{ hotel.nameEn }}</p>
            <StarRating :rating="hotel.starLevel" />
            <p class="address"><el-icon><Location /></el-icon> {{ hotel.address }}</p>
            <div class="score" v-if="hotel.score > 0">
              <span class="score-num">{{ hotel.score }}</span>
              <span class="score-label">分</span>
            </div>
          </div>
        </div>
        <div class="facilities" v-if="hotel.facilities?.length">
          <el-tag v-for="f in hotel.facilities" :key="f" style="margin-right:8px">{{ f }}</el-tag>
        </div>
        <div class="description" v-if="hotel.description">
          <h3>酒店介绍</h3>
          <p>{{ hotel.description }}</p>
        </div>
      </el-card>

      <!-- 房型列表 -->
      <h2 class="section-title">可选房型</h2>
      <el-card v-for="room in hotel.rooms" :key="room.id" class="room-card">
        <div class="room-info">
          <div>
            <h3>{{ room.name }}</h3>
            <p class="room-meta">
              {{ room.bedType }} · {{ room.breakfast }} · {{ room.area }} · 最多{{ room.maxGuests }}人
            </p>
            <div class="room-facilities" v-if="room.facilities?.length">
              <el-tag size="small" v-for="f in room.facilities" :key="f" style="margin-right:6px">{{ f }}</el-tag>
            </div>
          </div>
          <div class="room-price">
            <span class="price">¥{{ room.price }}</span>
            <span class="unit">/间/晚</span>
            <el-button type="primary" @click="bookRoom(room)">预订</el-button>
          </div>
        </div>
      </el-card>

      <!-- 评价 -->
      <h2 class="section-title">住客评价</h2>
      <div v-if="reviews.length">
        <el-card v-for="r in reviews" :key="r.id" class="review-card">
          <div class="review-header">
            <span class="reviewer">{{ r.user?.nickname }}</span>
            <StarRating :rating="r.score" :size="14" />
          </div>
          <p class="review-content">{{ r.content }}</p>
          <span class="review-time">{{ r.createTime }}</span>
        </el-card>
      </div>
      <el-empty v-else description="暂无评价" />
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getHotelDetail } from '@/api/resource'
import { getHotelReviews } from '@/api/review'
import StarRating from '@/components/StarRating.vue'

const route = useRoute()
const router = useRouter()
const loading = ref(true)
const hotel = ref(null)
const reviews = ref([])

onMounted(async () => {
  const id = route.params.id
  try {
    hotel.value = await getHotelDetail(id)
    const reviewData = await getHotelReviews(id, { page: 1, size: 10 })
    reviews.value = reviewData?.records || []
  } catch (e) {
    console.error(e)
  } finally {
    loading.value = false
  }
})

function bookRoom(room) {
  router.push({ path: '/order/confirm', query: { hotelId: hotel.value.id, roomId: room.id, price: room.price, roomName: room.name } })
}
</script>

<style scoped>
.detail-page { max-width: 1200px; margin: 0 auto; padding: 20px; }
.container { max-width: 900px; margin: 0 auto; }
.info-card { margin-bottom: 24px; }
.hotel-header { display: flex; gap: 20px; }
.hotel-info h1 { font-size: 24px; margin-bottom: 4px; }
.en-name { color: #909399; font-size: 14px; margin-bottom: 8px; }
.address { color: #606266; margin: 8px 0; display: flex; align-items: center; gap: 4px; }
.score { margin-top: 8px; }
.score-num { font-size: 28px; color: #E6A23C; font-weight: bold; }
.score-label { font-size: 14px; color: #909399; }
.facilities { margin-top: 16px; }
.description { margin-top: 20px; }
.description h3 { margin-bottom: 8px; font-size: 16px; }
.section-title { font-size: 20px; margin: 32px 0 16px; }
.room-card { margin-bottom: 12px; }
.room-info { display: flex; justify-content: space-between; align-items: center; }
.room-meta { font-size: 13px; color: #909399; margin: 4px 0 8px; }
.room-price { text-align: right; }
.price { font-size: 22px; color: #F56C6C; font-weight: bold; }
.unit { font-size: 12px; color: #909399; display: block; margin-bottom: 8px; }
.review-card { margin-bottom: 12px; }
.review-header { display: flex; align-items: center; gap: 12px; margin-bottom: 8px; }
.reviewer { font-weight: 500; }
.review-content { color: #606266; margin-bottom: 4px; }
.review-time { font-size: 12px; color: #C0C4CC; }
</style>
