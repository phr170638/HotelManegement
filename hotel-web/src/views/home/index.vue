<template>
  <div class="home-page">
    <!-- Hero Banner -->
    <div class="hero-banner">
      <div class="hero-content">
        <h1>全球酒店，轻松预订</h1>
        <p>海量酒店资源，最优价格保障</p>
        <div class="search-box">
          <el-input
            v-model="keyword"
            size="large"
            placeholder="搜索酒店名称、品牌或地标"
            prefix-icon="Search"
            @keyup.enter="goSearch"
          />
          <el-button type="primary" size="large" @click="goSearch">搜索</el-button>
        </div>
      </div>
    </div>

    <!-- 热门城市 -->
    <div class="section">
      <h2 class="section-title">热门城市</h2>
      <div class="city-grid">
        <div
          v-for="city in hotCities"
          :key="city.id"
          class="city-card"
          @click="searchByCity(city.id)"
        >
          <h3>{{ city.nameCn }}</h3>
          <p>{{ city.nameEn }}</p>
        </div>
      </div>
    </div>

    <!-- 推荐酒店 -->
    <div class="section">
      <h2 class="section-title">精品推荐</h2>
      <div class="hotel-grid">
        <HotelCard
          v-for="hotel in hotels"
          :key="hotel.id"
          :hotel="hotel"
        />
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { getHotCities, getHotels } from '@/api/resource'
import HotelCard from '@/components/HotelCard.vue'

const router = useRouter()
const keyword = ref('')
const hotCities = ref([])
const hotels = ref([])

onMounted(async () => {
  try {
    const [cities, hotelData] = await Promise.all([
      getHotCities().catch(() => []),
      getHotels({ page: 1, size: 6 }).catch(() => ({ records: [] }))
    ])
    hotCities.value = cities
    hotels.value = hotelData.records || []
  } catch (e) {
    // 后端未启动时静默失败
  }
})

function goSearch() {
  router.push({ path: '/search', query: { keyword: keyword.value } })
}

function searchByCity(cityId) {
  router.push({ path: '/search', query: { cityId } })
}
</script>

<style scoped>
.hero-banner {
  background: linear-gradient(135deg, #409EFF 0%, #337ECC 100%);
  padding: 80px 20px;
  text-align: center;
  color: #fff;
}
.hero-content h1 {
  font-size: 36px;
  margin-bottom: 12px;
}
.hero-content p {
  font-size: 16px;
  margin-bottom: 32px;
  opacity: 0.85;
}
.search-box {
  display: flex;
  max-width: 600px;
  margin: 0 auto;
  gap: 12px;
}
.section {
  max-width: 1200px;
  margin: 0 auto;
  padding: 40px 20px;
}
.section-title {
  font-size: 22px;
  margin-bottom: 20px;
  color: #303133;
}
.city-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(180px, 1fr));
  gap: 16px;
}
.city-card {
  padding: 24px;
  background: #fff;
  border-radius: 8px;
  text-align: center;
  cursor: pointer;
  transition: all 0.3s;
  box-shadow: 0 2px 8px rgba(0,0,0,0.06);
}
.city-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 16px rgba(0,0,0,0.12);
}
.city-card h3 { font-size: 18px; color: #303133; }
.city-card p { font-size: 12px; color: #909399; margin-top: 4px; }
.hotel-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(360px, 1fr));
  gap: 20px;
}
</style>
