<script setup>
import { computed, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ArrowLeft, Calendar, Location, Position, Search, StarFilled, Tickets } from '@element-plus/icons-vue'
import { getHotelDetail } from '@/api/resource'
import { getHotelReviews } from '@/api/review'
import StarRating from '@/components/StarRating.vue'
import {
  buildHotelMapUrl,
  formatHotelReviewCount,
  formatHotelScore,
  getHotelAddress,
  getHotelBrand,
  getHotelDescription,
  getHotelDisplayName,
  getHotelGallery,
  getHotelSecondaryName,
  getRoomCover
} from '@/utils/hotel'

const route = useRoute()
const router = useRouter()
const loading = ref(true)
const hotel = ref(null)
const reviews = ref([])
const gallery = ref([])
const hotelLoadFailed = ref(false)
const reviewLoadFailed = ref(false)
const hotelName = computed(() => getHotelDisplayName(hotel.value))
const secondaryName = computed(() => getHotelSecondaryName(hotel.value))
const scoreText = computed(() => formatHotelScore(hotel.value?.score))
const reviewText = computed(() => formatHotelReviewCount(hotel.value?.reviewCount))
const mapUrl = computed(() => buildHotelMapUrl(hotel.value))
const hotelAddress = computed(() => getHotelAddress(hotel.value))
const hotelBrand = computed(() => getHotelBrand(hotel.value))
const hotelDescription = computed(() => getHotelDescription(hotel.value))
const hasGuestScore = computed(() => Number(hotel.value?.reviewCount || 0) > 0 && Number(hotel.value?.score || 0) > 0)

const detailHighlights = computed(() => {
  const roomList = hotel.value?.rooms || []
  const lowestPrice = roomList.length ? Math.min(...roomList.map((room) => Number(room.price) || 0).filter((price) => price > 0)) : null

  return [
    { label: '可订房型', value: `${roomList.length} 种` },
    { label: '住客评分', value: scoreText.value },
    { label: '参考起价', value: lowestPrice ? `¥${lowestPrice}` : '待更新' },
    { label: '评价数量', value: reviewText.value }
  ]
})

async function loadHotel() {
  const id = Number(route.params.id)
  loading.value = true
  hotelLoadFailed.value = false
  reviewLoadFailed.value = false
  hotel.value = null
  reviews.value = []
  gallery.value = []

  if (!Number.isFinite(id) || id <= 0) {
    hotelLoadFailed.value = true
    loading.value = false
    return
  }

  try {
    hotel.value = await getHotelDetail(id, { silent: true })

    try {
      const reviewData = await getHotelReviews(id, { page: 1, size: 10 }, { silent: true })
      reviews.value = reviewData?.records || []
    } catch (error) {
      reviews.value = []
      reviewLoadFailed.value = true
    }
  } catch (error) {
    hotel.value = null
    hotelLoadFailed.value = true
  } finally {
    gallery.value = hotel.value ? getHotelGallery(hotel.value) : []
    loading.value = false
  }
}

watch(
  () => route.params.id,
  () => {
    loadHotel()
  },
  { immediate: true }
)

function bookRoom(room) {
  if (!hotel.value?.id || !room?.id) return
  router.push({
    path: '/order/confirm',
    query: {
      hotelId: hotel.value.id,
      roomId: room.id,
      price: room.price,
      roomName: room.name,
      hotelName: getHotelDisplayName(hotel.value)
    }
  })
}

function scrollToRooms() {
  document.getElementById('room-section')?.scrollIntoView({ behavior: 'smooth', block: 'start' })
}

function openMap() {
  window.open(mapUrl.value, '_blank', 'noopener')
}
</script>

<template>
  <div class="detail-page page-container narrow" v-loading="loading">
    <div class="detail-actions">
      <el-button :icon="ArrowLeft" @click="$router.back()">返回上一页</el-button>
      <el-button :icon="Search" @click="$router.push('/search')">返回搜索</el-button>
    </div>

    <template v-if="hotel">
      <section class="hero-panel soft-card">
        <div class="gallery-grid">
          <div class="hero-image">
            <img :src="gallery[0]" :alt="hotelName" class="image-cover" />
          </div>
          <div class="sub-gallery">
            <img v-for="image in gallery.slice(1)" :key="image" :src="image" :alt="hotelName" class="image-cover" />
          </div>
        </div>

        <div class="hero-info">
          <div>
            <div class="gold-chip">Hotel Detail</div>
            <h1>{{ hotelName }}</h1>
            <p v-if="secondaryName">{{ secondaryName }}</p>
          </div>

          <div class="highlight-grid">
            <div v-for="item in detailHighlights" :key="item.label" class="highlight-card">
              <span>{{ item.label }}</span>
              <strong>{{ item.value }}</strong>
            </div>
          </div>

          <div class="info-grid">
            <div class="info-item">
              <el-icon><Location /></el-icon>
              <span>地址</span>
              <strong>{{ hotelAddress }}</strong>
              <button type="button" class="map-link" @click="openMap">查看地图</button>
            </div>
            <div class="info-item">
              <el-icon><StarFilled /></el-icon>
              <span>评分</span>
              <strong>{{ scoreText }}</strong>
            </div>
            <div class="info-item">
              <el-icon><Tickets /></el-icon>
              <span>品牌</span>
              <strong>{{ hotelBrand }}</strong>
            </div>
          </div>

          <StarRating v-if="hasGuestScore" :rating="Number(hotel.score)" />
          <p v-else class="rating-placeholder">当前暂无住客评分</p>

          <div class="facility-list">
            <span v-for="facility in hotel.facilities || []" :key="facility">{{ facility }}</span>
          </div>

          <p class="description">{{ hotelDescription }}</p>

          <div class="hero-cta">
            <el-button type="primary" class="book-btn" :icon="Calendar" @click="scrollToRooms">查看可订房型</el-button>
            <el-button :icon="Position" @click="openMap">地图导航</el-button>
            <el-button @click="$router.push('/user/orders')">查看我的订单</el-button>
          </div>
        </div>
      </section>

      <section id="room-section" class="room-section">
        <div class="section-heading">
          <div>
            <div class="gold-chip">Room Selection</div>
            <h2>可选房型</h2>
            <p>选择适合的房型后，即可直接进入预订流程。</p>
          </div>
        </div>

        <article v-for="room in hotel.rooms || []" :key="room.id" class="room-card soft-card">
          <div class="room-visual">
            <img :src="getRoomCover(room, hotel)" :alt="room.name" class="image-cover" />
          </div>
          <div class="room-content">
            <div class="room-top">
              <div>
                <h3>{{ room.name }}</h3>
                <p>{{ room.bedType }} · {{ room.breakfast }} · {{ room.area }} · 最多 {{ room.maxGuests }} 人</p>
              </div>
              <div class="room-price">
                <span>¥{{ room.price }}</span>
                <small>/间/晚</small>
              </div>
            </div>

            <div class="room-tags">
              <span v-for="facility in room.facilities || []" :key="facility">{{ facility }}</span>
            </div>

            <div class="room-bottom">
              <div class="room-policy">
                <strong>{{ room.cancelable ? '支持取消' : '不可取消' }}</strong>
                <span>取消费用：¥{{ room.cancelPenalty || 0 }}</span>
              </div>
              <el-button type="primary" class="book-btn" @click="bookRoom(room)">立即预订</el-button>
            </div>
          </div>
        </article>

        <el-empty v-if="!(hotel.rooms || []).length" description="当前暂无可预订房型">
          <el-button type="primary" class="book-btn" @click="$router.push('/search')">返回搜索其他酒店</el-button>
        </el-empty>
      </section>

      <section class="review-section">
        <div class="section-heading">
          <div>
            <div class="gold-chip">Guest Reviews</div>
            <h2>住客评价</h2>
            <p>来自真实住客的入住反馈，帮助你了解酒店体验与服务细节。</p>
          </div>
        </div>

        <el-alert
          v-if="reviewLoadFailed"
          class="section-alert"
          type="warning"
          :closable="false"
          title="住客评价暂时加载失败，酒店信息仍可继续查看"
        />

        <div v-if="reviews.length" class="review-list">
          <article v-for="review in reviews" :key="review.id" class="review-card soft-card">
            <div class="review-head">
              <div>
                <strong>{{ review.anonymous ? '匿名住客' : (review.user?.nickname || '匿名住客') }}</strong>
                <span>{{ review.createTime }}</span>
              </div>
              <StarRating :rating="review.score" :size="14" />
            </div>
            <p>{{ review.content || '该用户仅打分，未填写文字评价。' }}</p>
          </article>
        </div>
        <el-empty v-else description="暂无评价" />
      </section>
    </template>

    <section v-else-if="hotelLoadFailed" class="error-panel soft-card">
      <el-result
        icon="warning"
        title="没有找到这家酒店"
        sub-title="可能是酒店不存在，或者当前服务暂时不可用。你可以返回搜索页重新选择。"
      >
        <template #extra>
          <el-button type="primary" class="book-btn" @click="loadHotel">重新加载</el-button>
          <el-button @click="$router.push('/search')">返回搜索</el-button>
          <el-button @click="$router.push('/')">回到首页</el-button>
        </template>
      </el-result>
    </section>
  </div>
</template>

<style scoped>
.detail-page {
  padding: 24px 0 0;
}

.detail-actions {
  display: flex;
  gap: 12px;
  margin-bottom: 18px;
  flex-wrap: wrap;
}

.hero-panel,
.room-card,
.review-card,
.error-panel {
  background: rgba(255, 252, 247, 0.92);
}

.hero-panel {
  overflow: hidden;
}

.gallery-grid {
  display: grid;
  grid-template-columns: 1.4fr 1fr;
  gap: 10px;
  min-height: 360px;
}

.hero-image,
.sub-gallery img {
  width: 100%;
  height: 100%;
}

.sub-gallery {
  display: grid;
  grid-template-rows: repeat(2, 1fr);
  gap: 10px;
}

.hero-info {
  padding: 28px;
}

.hero-info h1 {
  margin: 18px 0 8px;
  font-size: clamp(30px, 4vw, 44px);
}

.hero-info p {
  margin: 0;
  color: var(--text-secondary);
  line-height: 1.8;
}

.highlight-grid {
  margin: 22px 0 0;
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
}

.highlight-card {
  padding: 16px;
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.64);
  border: 1px solid rgba(109, 90, 75, 0.08);
}

.highlight-card span,
.highlight-card strong {
  display: block;
}

.highlight-card span {
  font-size: 12px;
  color: var(--text-muted);
}

.highlight-card strong {
  margin-top: 10px;
  color: var(--brand-primary);
  font-size: 20px;
}

.info-grid {
  margin: 20px 0;
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
}

.info-item {
  padding: 16px;
  border-radius: 18px;
  background: rgba(109, 90, 75, 0.06);
}

.info-item .el-icon {
  font-size: 16px;
  color: var(--brand-secondary-strong);
}

.info-item span {
  display: block;
  margin-top: 8px;
  font-size: 12px;
  color: var(--text-muted);
}

.info-item strong {
  display: block;
  margin-top: 10px;
  font-size: 16px;
}

.map-link {
  margin-top: 10px;
  padding: 0;
  border: none;
  background: transparent;
  color: var(--brand-secondary-strong);
  cursor: pointer;
  font-size: 13px;
}

.facility-list,
.room-tags {
  margin-top: 18px;
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.facility-list span,
.room-tags span {
  padding: 8px 12px;
  border-radius: 999px;
  background: rgba(184, 149, 103, 0.14);
  color: var(--brand-secondary-strong);
  font-size: 12px;
}

.description {
  margin-top: 18px;
}

.rating-placeholder {
  margin-top: 18px;
  color: var(--text-muted);
}

.hero-cta {
  margin-top: 20px;
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
}

.room-section,
.review-section {
  margin-top: 32px;
}

.section-alert,
.error-panel {
  margin-top: 18px;
}

.room-card {
  display: grid;
  grid-template-columns: 280px 1fr;
  gap: 20px;
  overflow: hidden;
  margin-bottom: 18px;
}

.room-visual {
  min-height: 220px;
}

.room-content {
  padding: 24px 24px 24px 0;
}

.room-top,
.review-head,
.room-bottom {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  align-items: flex-start;
}

.room-top h3 {
  margin: 0;
  font-size: 24px;
}

.room-top p {
  margin-top: 8px;
  color: var(--text-secondary);
}

.room-price {
  text-align: right;
}

.room-price span {
  display: block;
  font-size: 32px;
  color: var(--brand-danger);
  font-weight: 700;
}

.room-price small,
.room-policy span,
.review-head span {
  color: var(--text-muted);
}

.room-bottom {
  margin-top: 22px;
  align-items: center;
}

.room-policy strong,
.review-head strong {
  display: block;
  margin-bottom: 6px;
}

.book-btn {
  --el-button-bg-color: var(--brand-primary);
  --el-button-border-color: var(--brand-primary);
  --el-button-hover-bg-color: #7b6857;
  --el-button-hover-border-color: #7b6857;
}

.review-card {
  padding: 22px;
  margin-bottom: 16px;
}

.review-card p {
  margin: 16px 0 0;
  color: var(--text-secondary);
  line-height: 1.8;
}

@media (max-width: 860px) {
  .gallery-grid,
  .highlight-grid,
  .info-grid,
  .room-card {
    grid-template-columns: 1fr;
  }

  .room-content {
    padding: 0 18px 18px;
  }

  .room-top,
  .room-bottom,
  .review-head {
    flex-direction: column;
  }

  .detail-actions {
    flex-direction: column;
    align-items: stretch;
  }

  .hero-cta {
    flex-direction: column;
  }
}
</style>
