<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { CircleCheck, OfficeBuilding, Search, Tickets } from '@element-plus/icons-vue'
import { getHotCities, getHotels } from '@/api/resource'
import HotelCard from '@/components/HotelCard.vue'
import { getCityImage, heroBannerImage } from '@/utils/hotel'
import { useUserStore } from '@/store/user'

const router = useRouter()
const userStore = useUserStore()
const keyword = ref('')
const hotCities = ref([])
const hotels = ref([])
const cityLoadFailed = ref(false)
const hotelLoadFailed = ref(false)

const featureMetrics = [
  { value: '多城', label: '按城市、商圈与品牌快速找房' },
  { value: '24h', label: '随时提交预订并查看订单进度' },
  { value: '安心订', label: '房型、价格与预订规则清楚展示' }
]

const serviceHighlights = [
  {
    title: '先看位置，再选房型',
    desc: '搜索结果会优先展示城市、地段、价格和评分，方便你先缩小范围。'
  },
  {
    title: '订前信息看得明白',
    desc: '房型、早餐、入住人数和价格放在同一页，不需要来回切换页面确认。'
  },
  {
    title: '下单流程更直接',
    desc: '从酒店详情进入预订后，关键信息会继续保留，减少重复填写。'
  },
  {
    title: '订单状态随时可查',
    desc: '登录后可以回看自己的订单记录，确认是否待支付、待入住或已完成。'
  }
]

const shortcutEntries = computed(() => {
  const entries = [
    { key: 'search', label: '搜索酒店', desc: '按城市、品牌和关键词快速筛选', action: () => router.push('/search'), icon: Search }
  ]

  if (userStore.isLoggedIn) {
    entries.push({
      key: 'orders',
      label: '我的订单',
      desc: '回看待支付、待入住和历史订单',
      action: () => router.push('/user/orders'),
      icon: Tickets
    })
  }

  if (userStore.roles.includes('admin')) {
    entries.push({
      key: 'admin',
      label: '后台管理',
      desc: '进入酒店、订单和用户管理工作台',
      action: () => router.push('/admin/hotels'),
      icon: OfficeBuilding
    })
  }

  return entries
})

onMounted(async () => {
  await fetchHomeData()
})

async function fetchHomeData() {
  const [cityResult, hotelResult] = await Promise.allSettled([
    getHotCities({ silent: true }),
    getHotels({ page: 1, size: 6 }, { silent: true })
  ])

  if (cityResult.status === 'fulfilled') {
    hotCities.value = (cityResult.value || []).map((city) => ({
      ...city,
      image: city.image || getCityImage(city.nameCn || city.nameEn)
    }))
    cityLoadFailed.value = false
  } else {
    hotCities.value = []
    cityLoadFailed.value = true
  }

  if (hotelResult.status === 'fulfilled') {
    hotels.value = hotelResult.value.records || []
    hotelLoadFailed.value = false
  } else {
    hotels.value = []
    hotelLoadFailed.value = true
  }
}

function goSearch() {
  router.push({ path: '/search', query: { keyword: keyword.value || undefined } })
}

function searchByCity(cityId) {
  router.push({ path: '/search', query: { cityId } })
}
</script>

<template>
  <div class="home-page">
    <section class="page-section hero">
      <div class="page-hero hero-card">
        <img :src="heroBannerImage" alt="酒店大堂视觉" class="image-cover" />
        <div class="hero-overlay" />
        <div class="hero-content">
          <div class="gold-chip hero-chip">住客预订</div>
          <h1 class="page-title">自在入住，从一间更懂旅人的酒店开始</h1>
          <p class="page-subtitle">
            围绕搜索、看房型、比价格和提交订单这几件事，把住客最常用的预订动作放在一条顺手的路径里。
          </p>

          <div class="search-panel glass-panel">
            <el-input
              v-model="keyword"
              size="large"
              clearable
              placeholder="搜索酒店名称、品牌、城市或地标"
              @keyup.enter="goSearch"
            >
              <template #prefix>
                <el-icon><Search /></el-icon>
              </template>
            </el-input>
            <el-button type="primary" class="hero-btn" @click="goSearch">立即搜索</el-button>
          </div>

          <div class="hero-metrics">
            <div v-for="item in featureMetrics" :key="item.label" class="metric-card">
              <strong>{{ item.value }}</strong>
              <span>{{ item.label }}</span>
            </div>
          </div>

          <div v-if="shortcutEntries.length" class="shortcut-grid">
            <button
              v-for="item in shortcutEntries"
              :key="item.key"
              type="button"
              class="shortcut-card"
              @click="item.action"
            >
              <el-icon><component :is="item.icon" /></el-icon>
              <div>
                <strong>{{ item.label }}</strong>
                <span>{{ item.desc }}</span>
              </div>
            </button>
          </div>
        </div>
      </div>
    </section>

    <section class="page-section overview">
      <div class="section-heading">
        <div class="section-copy">
          <div class="gold-chip">住客服务</div>
          <h2>订房前，你最关心的信息都在这里</h2>
          <p>不是讲系统能力，而是把普通住客真正会用到的找房、看房、下单和查订单体验讲清楚。</p>
        </div>
      </div>

      <div class="overview-grid">
        <div v-for="item in serviceHighlights" :key="item.title" class="overview-card soft-card">
          <el-icon><CircleCheck /></el-icon>
          <h3>{{ item.title }}</h3>
          <p>{{ item.desc }}</p>
        </div>
      </div>
    </section>

    <section class="page-section city-section">
      <div class="section-heading">
        <div class="section-copy">
          <div class="gold-chip">热门城市</div>
          <h2>从热门目的地开始找房</h2>
          <p>先按出行城市进入，再缩小到酒店、房型和价格，会更接近日常订房习惯。</p>
        </div>
      </div>

      <div v-if="hotCities.length" class="city-grid">
        <article v-for="city in hotCities" :key="city.id" class="city-card soft-card" @click="searchByCity(city.id)">
          <img :src="city.image" :alt="city.nameCn" class="image-cover" />
          <div class="city-mask">
            <div>
              <h3>{{ city.nameCn }}</h3>
              <p>{{ city.nameEn }}</p>
            </div>
            <el-button text>查看酒店</el-button>
          </div>
        </article>
      </div>
      <el-empty
        v-else
        class="section-empty"
        :description="cityLoadFailed ? '热门城市暂时加载失败，请稍后刷新重试' : '当前暂无热门城市数据'"
      >
        <el-button v-if="cityLoadFailed" class="more-btn" @click="fetchHomeData">重新加载</el-button>
      </el-empty>
    </section>

    <section class="page-section recommend-section">
      <div class="section-heading">
        <div class="section-copy">
          <div class="gold-chip">精选酒店</div>
          <h2>先看看现在可以预订的酒店</h2>
          <p>把位置、评分、价格和基础设施放在同一屏，方便你快速比较，再决定要不要点进详情。</p>
        </div>
        <el-button class="more-btn section-action-btn" @click="goSearch">查看全部酒店</el-button>
      </div>

      <div v-if="hotels.length" class="hotel-grid">
        <HotelCard v-for="hotel in hotels" :key="hotel.id" :hotel="hotel" />
      </div>
      <el-empty
        v-else
        class="section-empty"
        :description="hotelLoadFailed ? '推荐酒店暂时加载失败，请稍后刷新重试' : '当前暂无推荐酒店数据'"
      >
        <el-button v-if="hotelLoadFailed" class="more-btn" @click="fetchHomeData">重新加载</el-button>
      </el-empty>
    </section>
  </div>
</template>

<style scoped>
.home-page {
  padding: 24px 0 0;
}

.hero-card {
  min-height: 640px;
}

.hero-content {
  position: relative;
  z-index: 1;
  min-height: 640px;
  padding: 64px;
  display: flex;
  flex-direction: column;
  justify-content: center;
  max-width: 820px;
}

.hero-chip {
  background: rgba(242, 226, 203, 0.3);
  color: #f0cc92;
}

.search-panel {
  margin-top: 28px;
  padding: 18px;
  border-radius: 24px;
  display: grid;
  grid-template-columns: 1fr auto;
  gap: 14px;
  background: rgba(255, 250, 244, 0.92);
  border-color: rgba(255, 244, 230, 0.42);
}

.hero-btn,
.more-btn {
  --el-button-bg-color: var(--brand-primary);
  --el-button-border-color: var(--brand-primary);
  --el-button-text-color: #fff8f0;
  --el-button-hover-bg-color: #7b6857;
  --el-button-hover-border-color: #7b6857;
  --el-button-hover-text-color: #fffdfa;
  --el-button-active-text-color: #fffdfa;
  min-height: 42px;
  padding-inline: 18px;
}

.section-copy {
  max-width: 620px;
}

.section-action-btn {
  align-self: flex-end;
  margin-bottom: 6px;
}

.hero-metrics {
  margin-top: 28px;
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 16px;
}

.shortcut-grid {
  margin-top: 18px;
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 14px;
}

.shortcut-card {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  width: 100%;
  padding: 16px 18px;
  border: 1px solid rgba(255, 243, 231, 0.2);
  border-radius: 20px;
  background: rgba(255, 248, 240, 0.1);
  color: #fff;
  cursor: pointer;
  text-align: left;
  transition: transform 0.2s ease, background-color 0.2s ease, border-color 0.2s ease;
}

.shortcut-card:hover {
  transform: translateY(-1px);
  background: rgba(255, 248, 240, 0.18);
  border-color: rgba(255, 243, 231, 0.32);
}

.shortcut-card .el-icon {
  margin-top: 2px;
  font-size: 18px;
  color: #f0cc92;
}

.shortcut-card strong,
.shortcut-card span {
  display: block;
}

.shortcut-card strong {
  font-size: 15px;
}

.shortcut-card span {
  margin-top: 6px;
  color: rgba(255, 249, 243, 0.88);
  font-size: 12px;
  line-height: 1.7;
}

.metric-card {
  padding: 20px 22px;
  border-radius: 24px;
  background: rgba(255, 248, 240, 0.14);
  backdrop-filter: blur(14px);
  border: 1px solid rgba(255, 243, 231, 0.18);
  box-shadow: 0 18px 36px rgba(43, 30, 18, 0.08);
}

.metric-card strong {
  display: block;
  color: #fffaf4;
  font-size: 26px;
  line-height: 1.1;
}

.metric-card span {
  display: block;
  margin-top: 10px;
  color: rgba(255, 249, 243, 0.92);
  font-size: 13px;
  line-height: 1.7;
}

.overview,
.city-section,
.recommend-section {
  padding: 48px 0 0;
}

.overview-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 18px;
}

.overview-card {
  padding: 30px 34px;
  background: rgba(255, 252, 247, 0.96);
}

.overview-card .el-icon {
  font-size: 24px;
  color: var(--brand-secondary-strong);
}

.overview-card h3 {
  margin: 18px 0 10px;
  font-size: 18px;
  color: var(--text-primary);
}

.overview-card p {
  margin: 0;
  color: var(--text-secondary);
  line-height: 1.8;
}

.city-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 18px;
}

.city-card {
  position: relative;
  height: 280px;
  overflow: hidden;
  cursor: pointer;
}

.city-mask {
  position: absolute;
  inset: 0;
  padding: 22px;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  background: linear-gradient(180deg, rgba(57, 43, 31, 0.1), rgba(69, 54, 42, 0.72));
  color: #fff;
}

.city-mask h3 {
  margin: 0;
  font-size: 22px;
}

.city-mask p {
  margin: 8px 0 0;
  font-size: 13px;
  opacity: 0.84;
}

.hotel-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 20px;
}

.section-empty {
  min-height: 320px;
  padding: 36px 0 12px;
}

.section-empty :deep(.el-empty__description) {
  margin-top: 18px;
}

.section-empty :deep(.el-empty__bottom) {
  margin-top: 22px;
}

.section-empty :deep(.el-empty__image) {
  width: 180px;
}

@media (max-width: 1100px) {
  .hero-content {
    padding: 40px 24px;
  }

  .hero-metrics,
  .shortcut-grid,
  .city-grid,
  .hotel-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 720px) {
  .hero-card,
  .hero-content {
    min-height: 560px;
  }

  .section-action-btn {
    align-self: stretch;
    margin-bottom: 0;
  }

  .search-panel,
  .hero-metrics,
  .shortcut-grid,
  .overview-grid,
  .city-grid,
  .hotel-grid {
    grid-template-columns: 1fr;
  }
}
</style>
