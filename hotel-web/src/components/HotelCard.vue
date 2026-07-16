<script setup>
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { Location, OfficeBuilding, Position, Tickets } from '@element-plus/icons-vue'
import StarRating from './StarRating.vue'
import {
  buildHotelMapUrl,
  formatHotelReviewCount,
  formatHotelScore,
  getHotelAddress,
  getHotelBrand,
  getFacilityList,
  getHotelCover,
  getHotelDisplayName,
  getHotelSecondaryName
} from '@/utils/hotel'

const props = defineProps({
  hotel: {
    type: Object,
    required: true
  }
})

const router = useRouter()

const hotelCover = computed(() => getHotelCover(props.hotel))
const facilities = computed(() => getFacilityList(props.hotel.facilities))
const price = computed(() => props.hotel.minPrice || props.hotel.price || 0)
const hotelName = computed(() => getHotelDisplayName(props.hotel))
const secondaryName = computed(() => getHotelSecondaryName(props.hotel))
const reviewText = computed(() => formatHotelReviewCount(props.hotel.reviewCount))
const scoreText = computed(() => formatHotelScore(props.hotel.score))
const mapUrl = computed(() => buildHotelMapUrl(props.hotel))
const hotelAddress = computed(() => getHotelAddress(props.hotel))
const hasGuestScore = computed(() => Number(props.hotel.reviewCount || 0) > 0 && Number(props.hotel.score || 0) > 0)
const summaryItems = computed(() => [
  { key: 'city', label: props.hotel.cityName || '所在城市', icon: Location },
  { key: 'brand', label: getHotelBrand(props.hotel), icon: OfficeBuilding },
  { key: 'reviews', label: reviewText.value, icon: Tickets }
])

function goDetail() {
  router.push(`/hotel/${props.hotel.id}`)
}

function openMap() {
  window.open(mapUrl.value, '_blank', 'noopener')
}
</script>

<template>
  <article class="hotel-card soft-card" role="button" tabindex="0" @click="goDetail" @keyup.enter="goDetail">
    <div class="image-wrap">
      <img :src="hotelCover" :alt="hotelName" class="image-cover" />
      <div class="image-badge">
        <span>{{ hotel.cityName || '优选酒店' }}</span>
        <strong>{{ reviewText }}</strong>
      </div>
    </div>

    <div class="card-content">
      <div class="title-row">
        <div>
          <h3>{{ hotelName }}</h3>
          <p v-if="secondaryName">{{ secondaryName }}</p>
        </div>
        <div class="score-box">
          <strong>{{ scoreText }}</strong>
          <span>{{ hotel.score > 0 ? '住客评分' : '待评价' }}</span>
        </div>
      </div>

      <StarRating v-if="hasGuestScore" :rating="Number(props.hotel.score)" :size="15" />
      <p v-else class="rating-placeholder">当前暂无住客评分</p>
      <p class="address line-clamp-2">{{ hotelAddress }}</p>

      <div class="summary-list">
        <div v-for="item in summaryItems" :key="item.key" class="summary-item">
          <el-icon><component :is="item.icon" /></el-icon>
          <span>{{ item.label }}</span>
        </div>
      </div>

      <div class="facility-list">
        <span v-for="facility in facilities" :key="facility">{{ facility }}</span>
      </div>

      <div class="bottom-row">
        <div>
          <small>每晚起价</small>
          <div class="price">¥{{ price }}</div>
        </div>
        <div class="action-group">
          <el-button type="primary" class="book-btn" @click.stop="goDetail">查看详情</el-button>
          <el-button class="map-btn" :icon="Position" @click.stop="openMap">地图</el-button>
        </div>
      </div>
    </div>
  </article>
</template>

<style scoped>
.hotel-card {
  overflow: hidden;
  cursor: pointer;
  background: rgba(255, 252, 247, 0.92);
  transition: transform 0.26s ease, box-shadow 0.26s ease;
}

.hotel-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 24px 40px rgba(88, 68, 47, 0.16);
}

.image-wrap {
  position: relative;
  height: 220px;
  overflow: hidden;
}

.image-badge {
  position: absolute;
  left: 16px;
  right: 16px;
  bottom: 16px;
  display: flex;
  justify-content: space-between;
  gap: 10px;
  padding: 10px 14px;
  border-radius: 999px;
  background: rgba(78, 60, 45, 0.42);
  color: #fff;
  backdrop-filter: blur(10px);
  font-size: 12px;
}

.card-content {
  padding: 22px;
}

.title-row {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  align-items: flex-start;
  margin-bottom: 12px;
}

.title-row h3 {
  margin: 0;
  font-size: 22px;
}

.title-row p {
  margin: 6px 0 0;
  font-size: 12px;
  color: var(--text-muted);
}

.score-box {
  min-width: 104px;
  padding: 10px 12px;
  border-radius: 16px;
  background: rgba(184, 149, 103, 0.14);
  text-align: center;
}

.score-box strong {
  display: block;
  font-size: 18px;
  color: var(--brand-primary);
}

.score-box span {
  font-size: 12px;
  color: var(--brand-secondary-strong);
}

.address {
  margin: 14px 0;
  color: var(--text-secondary);
  line-height: 1.7;
}

.rating-placeholder {
  margin: 0;
  color: var(--text-muted);
  font-size: 13px;
}

.summary-list {
  display: grid;
  gap: 10px;
}

.summary-item {
  display: flex;
  align-items: center;
  gap: 8px;
  color: var(--text-secondary);
  font-size: 13px;
}

.summary-item .el-icon {
  color: var(--brand-secondary-strong);
}

.facility-list {
  margin-top: 16px;
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.facility-list span {
  padding: 8px 12px;
  border-radius: 999px;
  background: rgba(109, 90, 75, 0.08);
  color: var(--text-secondary);
  font-size: 12px;
}

.bottom-row {
  margin-top: 20px;
  display: flex;
  justify-content: space-between;
  align-items: end;
  gap: 16px;
}

.action-group {
  display: flex;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: 10px;
}

.bottom-row small {
  font-size: 12px;
  color: var(--text-muted);
}

.price {
  margin-top: 4px;
  font-size: 30px;
  color: var(--brand-danger);
  font-weight: 700;
}

.book-btn {
  --el-button-bg-color: var(--brand-primary);
  --el-button-border-color: var(--brand-primary);
  --el-button-hover-bg-color: #7b6857;
  --el-button-hover-border-color: #7b6857;
  min-width: 124px;
}

.map-btn {
  --el-button-border-color: rgba(109, 90, 75, 0.14);
  --el-button-text-color: var(--brand-primary);
  --el-button-bg-color: rgba(255, 255, 255, 0.72);
}

@media (max-width: 640px) {
  .image-wrap {
    height: 200px;
  }

  .card-content {
    padding: 18px;
  }

  .title-row {
    flex-direction: column;
  }

  .bottom-row {
    flex-direction: column;
    align-items: flex-start;
  }

  .action-group {
    width: 100%;
  }
}
</style>
