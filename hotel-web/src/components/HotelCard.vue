<script setup>
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import StarRating from './StarRating.vue'
import { getFacilityList, getHotelCover } from '@/utils/hotel'

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

function goDetail() {
  router.push(`/hotel/${props.hotel.id}`)
}
</script>

<template>
  <article class="hotel-card soft-card" role="button" tabindex="0" @click="goDetail" @keyup.enter="goDetail">
    <div class="image-wrap">
      <img :src="hotelCover" :alt="hotel.nameCn" class="image-cover" />
      <div class="image-badge">
        <span>{{ hotel.cityName || '优选酒店' }}</span>
        <strong>{{ hotel.reviewCount || 120 }}+ 点评</strong>
      </div>
    </div>

    <div class="card-content">
      <div class="title-row">
        <div>
          <h3>{{ hotel.nameCn }}</h3>
          <p v-if="hotel.nameEn">{{ hotel.nameEn }}</p>
        </div>
        <div class="score-box">
          <strong>{{ hotel.score || '4.7' }}</strong>
          <span>推荐</span>
        </div>
      </div>

      <StarRating :rating="hotel.starLevel || 4" :size="15" />
      <p class="address line-clamp-2">{{ hotel.address || '核心商圈旁，交通便利，适合商务与休闲出行。' }}</p>

      <div class="facility-list">
        <span v-for="facility in facilities" :key="facility">{{ facility }}</span>
      </div>

      <div class="bottom-row">
        <div>
          <small>每晚起价</small>
          <div class="price">¥{{ price }}</div>
        </div>
        <el-button type="primary" class="book-btn" @click.stop="goDetail">查看详情</el-button>
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
  min-width: 74px;
  padding: 10px 12px;
  border-radius: 16px;
  background: rgba(184, 149, 103, 0.14);
  text-align: center;
}

.score-box strong {
  display: block;
  font-size: 22px;
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

.facility-list {
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
}
</style>
