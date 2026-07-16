<script setup>
import { computed, onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Location, PriceTag, Search, StarFilled } from '@element-plus/icons-vue'
import { searchHotels } from '@/api/search'
import { getCities } from '@/api/resource'
import HotelCard from '@/components/HotelCard.vue'
import {
  buildSearchRequestParams,
  buildSearchRouteQuery,
  DEFAULT_PRICE_RANGE,
  isAbortError,
  isSameRouteQuery,
  normalizeSearchState
} from './state'

const route = useRoute()
const router = useRouter()
const loading = ref(false)
const results = ref([])
const total = ref(0)
const cities = ref([])
const cityLoadFailed = ref(false)
const searchFailed = ref(false)
const syncingFromRoute = ref(false)
let searchController = null
let latestSearchToken = 0

const priceRange = ref([...DEFAULT_PRICE_RANGE])

const searchParams = reactive({
  cityId: null,
  keyword: '',
  page: 1,
  size: 9
})

const filters = reactive({
  starLevel: null,
  sortBy: 'recommended'
})

const selectedCityName = computed(() => {
  const city = cities.value.find((item) => item.id === searchParams.cityId)
  return city?.nameCn || ''
})

const activeFilters = computed(() => {
  const tags = []

  if (searchParams.keyword) tags.push({ key: 'keyword', label: searchParams.keyword })
  if (selectedCityName.value) tags.push({ key: 'cityId', label: selectedCityName.value })
  if (filters.starLevel) tags.push({ key: 'starLevel', label: `${filters.starLevel} 星酒店` })
  if (priceRange.value[0] !== DEFAULT_PRICE_RANGE[0] || priceRange.value[1] !== DEFAULT_PRICE_RANGE[1]) {
    tags.push({ key: 'priceRange', label: `￥${priceRange.value[0]} - ￥${priceRange.value[1]}` })
  }
  if (filters.sortBy !== 'recommended') {
    tags.push({
      key: 'sortBy',
      label: filters.sortBy === 'price' ? '价格优先' : '评分优先'
    })
  }

  return tags
})

const summaryCards = computed(() => [
  {
    key: 'total',
    label: '匹配酒店',
    value: `${total.value} 家`,
    icon: Search
  },
  {
    key: 'city',
    label: '当前城市',
    value: selectedCityName.value || '不限城市',
    icon: Location
  },
  {
    key: 'price',
    label: '价格区间',
    value: `¥${priceRange.value[0]} - ¥${priceRange.value[1]}`,
    icon: PriceTag
  },
  {
    key: 'star',
    label: '星级偏好',
    value: filters.starLevel ? `${filters.starLevel} 星及以上` : '不限星级',
    icon: StarFilled
  }
])

function syncFromQuery(query) {
  syncingFromRoute.value = true
  const nextState = normalizeSearchState(query)

  Object.assign(searchParams, nextState.searchParams)
  Object.assign(filters, nextState.filters)
  priceRange.value = [...nextState.priceRange]

  syncingFromRoute.value = false
}

onMounted(async () => {
  syncFromQuery(route.query)
  try {
    const cityData = await getCities({ page: 1, size: 100 }, { silent: true })
    cities.value = cityData.records || []
    cityLoadFailed.value = false
  } catch (error) {
    cities.value = []
    cityLoadFailed.value = true
  }
  await doSearch()
})

watch(
  () => route.query,
  async (query) => {
    if (syncingFromRoute.value) return
    syncFromQuery(query)
    await doSearch(searchParams.page, false)
  }
)

onBeforeUnmount(() => {
  searchController?.abort()
})

async function doSearch(page = searchParams.page, syncRoute = true) {
  searchParams.page = page
  const searchToken = ++latestSearchToken
  loading.value = true

  try {
    const params = buildSearchRequestParams(searchParams, filters, priceRange.value)

    if (syncRoute) {
      const nextQuery = buildSearchRouteQuery(searchParams, filters, priceRange.value)
      if (!isSameRouteQuery(nextQuery, route.query)) {
        syncingFromRoute.value = true
        try {
          await router.replace({
            path: '/search',
            query: nextQuery
          })
        } finally {
          syncingFromRoute.value = false
        }
      }
    }

    searchController?.abort()
    searchController = new AbortController()

    const data = await searchHotels(params, { silent: true, signal: searchController.signal })
    if (searchToken !== latestSearchToken) return
    results.value = data.records || []
    total.value = data.total || 0
    searchFailed.value = false
  } catch (error) {
    if (isAbortError(error)) return
    if (searchToken !== latestSearchToken) return
    results.value = []
    total.value = 0
    searchFailed.value = true
  } finally {
    if (searchToken === latestSearchToken) {
      loading.value = false
    }
  }
}

function resetFilters() {
  searchParams.cityId = null
  searchParams.keyword = ''
  filters.starLevel = null
  filters.sortBy = 'recommended'
  priceRange.value = [...DEFAULT_PRICE_RANGE]
  doSearch(1)
}

async function reloadCurrentSearch() {
  await doSearch(searchParams.page, false)
}

function handleCityChange() {
  doSearch(1)
}

function handleSortChange() {
  doSearch(1)
}

function toggleStarLevel(star) {
  filters.starLevel = filters.starLevel === star ? null : star
  doSearch(1)
}

function handlePriceChange() {
  doSearch(1)
}

function handleKeywordClear() {
  doSearch(1)
}

function removeFilter(key) {
  if (key === 'keyword') searchParams.keyword = ''
  if (key === 'cityId') searchParams.cityId = null
  if (key === 'starLevel') filters.starLevel = null
  if (key === 'priceRange') priceRange.value = [...DEFAULT_PRICE_RANGE]
  if (key === 'sortBy') filters.sortBy = 'recommended'
  doSearch(1)
}
</script>

<template>
  <div class="search-page page-section">
    <section class="search-top soft-card">
      <div>
        <div class="gold-chip">Smart Search</div>
        <h1>寻找更适合当下行程的酒店</h1>
        <p>支持关键词、城市、价格与星级筛选，帮助你更快锁定心仪房源。</p>
      </div>

      <div class="search-toolbar glass-panel">
        <el-select
          v-model="searchParams.cityId"
          placeholder="选择城市"
          clearable
          :disabled="cityLoadFailed && cities.length === 0"
          @change="handleCityChange"
        >
          <el-option v-for="city in cities" :key="city.id" :label="city.nameCn" :value="city.id" />
        </el-select>
        <el-input
          v-model="searchParams.keyword"
          clearable
          placeholder="搜索酒店名称、品牌或商圈"
          @keyup.enter="doSearch(1)"
          @clear="handleKeywordClear"
        />
        <el-select v-model="filters.sortBy" @change="handleSortChange">
          <el-option label="默认推荐" value="recommended" />
          <el-option label="价格升序" value="price" />
          <el-option label="评分优先" value="score" />
        </el-select>
        <el-button type="primary" class="search-btn" @click="doSearch(1)">开始搜索</el-button>
      </div>
    </section>

    <section class="search-body">
      <aside class="filter-panel soft-card">
        <div class="filter-heading">
          <h3>筛选条件</h3>
          <span>共 {{ total }} 家酒店</span>
        </div>

        <div class="filter-group">
          <label>星级筛选</label>
          <div class="star-options">
            <button
              v-for="star in [5, 4, 3]"
              :key="star"
              type="button"
              :class="['chip-btn', { active: filters.starLevel === star }]"
              @click="toggleStarLevel(star)"
            >
              {{ '★'.repeat(star) }}
            </button>
          </div>
        </div>

        <div class="filter-group">
          <label>价格区间</label>
          <el-slider v-model="priceRange" range :min="200" :max="2000" :step="50" @change="handlePriceChange" />
          <div class="price-row">
            <span>¥{{ priceRange[0] }}</span>
            <span>¥{{ priceRange[1] }}</span>
          </div>
        </div>

        <div v-if="activeFilters.length" class="filter-group">
          <label>已应用条件</label>
          <div class="active-filter-list">
            <button
              v-for="item in activeFilters"
              :key="item.key"
              type="button"
              class="active-filter-chip"
              @click="removeFilter(item.key)"
            >
              {{ item.label }} ×
            </button>
          </div>
        </div>

        <div class="filter-actions">
          <el-button type="primary" class="search-btn" @click="doSearch(1)">刷新结果</el-button>
          <el-button @click="resetFilters">重置条件</el-button>
        </div>
      </aside>

      <main class="result-panel">
        <div class="result-summary">
          <strong>搜索结果</strong>
          <p>
            {{ searchParams.keyword || '全部酒店' }}
            <span v-if="selectedCityName"> · {{ selectedCityName }}</span>
          </p>
        </div>

        <div class="result-metrics">
          <div v-for="item in summaryCards" :key="item.key" class="metric-card soft-card">
            <el-icon><component :is="item.icon" /></el-icon>
            <div>
              <span>{{ item.label }}</span>
              <strong>{{ item.value }}</strong>
            </div>
          </div>
        </div>

        <el-alert
          v-if="cityLoadFailed"
          class="result-alert"
          type="warning"
          :closable="false"
          title="城市筛选暂时不可用，已为你保留其他搜索条件"
        />

        <el-alert
          v-if="searchFailed"
          class="result-alert"
          type="error"
          :closable="false"
          title="当前无法连接酒店搜索服务，请稍后重试"
        />

        <div class="result-grid" v-loading="loading">
          <HotelCard v-for="hotel in results" :key="hotel.id" :hotel="hotel" />
        </div>

        <el-empty
          v-if="!loading && results.length === 0"
          :description="searchFailed ? '搜索服务暂时不可用，请稍后再试' : '未找到符合条件的酒店'"
        >
          <el-button v-if="searchFailed" type="primary" class="search-btn" @click="reloadCurrentSearch">重新搜索</el-button>
          <el-button v-else @click="resetFilters">清空筛选</el-button>
        </el-empty>

        <div class="pagination" v-if="total > searchParams.size">
          <el-pagination
            v-model:current-page="searchParams.page"
            :page-size="searchParams.size"
            :total="total"
            layout="prev, pager, next"
            @current-change="doSearch"
          />
        </div>
      </main>
    </section>
  </div>
</template>

<style scoped>
.search-page {
  padding: 24px 0 0;
}

.search-top {
  padding: 28px;
  background: linear-gradient(135deg, rgba(98, 81, 66, 0.96), rgba(132, 109, 87, 0.9));
  color: #fff;
}

.search-top h1 {
  margin: 18px 0 8px;
  font-size: clamp(28px, 4vw, 44px);
}

.search-top p {
  margin: 0;
  max-width: 640px;
  color: rgba(247, 243, 238, 0.82);
  line-height: 1.8;
}

.search-toolbar {
  margin-top: 24px;
  padding: 16px;
  display: grid;
  grid-template-columns: 160px 1fr 160px 120px;
  gap: 12px;
  align-items: stretch;
  border-radius: 22px;
}

.search-toolbar :deep(.el-select),
.search-toolbar :deep(.el-input),
.search-toolbar :deep(.el-input__wrapper),
.search-toolbar :deep(.el-select__wrapper) {
  width: 100%;
  min-height: 44px;
  height: 44px;
}

.search-toolbar :deep(.el-button) {
  min-height: 44px;
}

.search-body {
  margin-top: 24px;
  display: grid;
  grid-template-columns: 280px 1fr;
  gap: 20px;
}

.filter-panel {
  padding: 22px;
  background: rgba(255, 252, 247, 0.92);
  height: fit-content;
  position: sticky;
  top: 96px;
}

.filter-heading,
.result-summary {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  align-items: end;
}

.filter-heading h3,
.result-summary strong {
  margin: 0;
  font-size: 20px;
}

.filter-heading span,
.result-summary p {
  margin: 0;
  font-size: 13px;
  color: var(--text-muted);
}

.filter-group {
  margin-top: 22px;
}

.filter-group label {
  display: block;
  margin-bottom: 10px;
  font-size: 13px;
  font-weight: 700;
  color: var(--text-secondary);
}

.star-options {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 8px;
}

.chip-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border: 1px solid rgba(109, 90, 75, 0.12);
  background: rgba(109, 90, 75, 0.04);
  min-height: 48px;
  padding: 10px 12px;
  border-radius: 20px;
  cursor: pointer;
  color: var(--brand-primary);
}

.chip-btn.active {
  background: rgba(184, 149, 103, 0.18);
  border-color: rgba(184, 149, 103, 0.28);
}

.price-row {
  display: flex;
  justify-content: space-between;
  font-size: 13px;
  color: var(--text-secondary);
}

.active-filter-list {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.active-filter-chip {
  border: 1px solid rgba(184, 149, 103, 0.3);
  background: rgba(184, 149, 103, 0.14);
  color: var(--brand-primary);
  padding: 8px 12px;
  border-radius: 999px;
  cursor: pointer;
}

.filter-actions {
  margin-top: 24px;
  display: grid;
  gap: 10px;
}

.search-btn {
  --el-button-bg-color: var(--brand-primary);
  --el-button-border-color: var(--brand-primary);
  --el-button-text-color: #fff8f0;
  --el-button-hover-bg-color: #7b6857;
  --el-button-hover-border-color: #7b6857;
  --el-button-hover-text-color: #fffdfa;
}

.result-panel {
  min-width: 0;
}

.result-grid {
  margin-top: 16px;
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 20px;
}

.result-metrics {
  margin-top: 16px;
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
}

.metric-card {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  padding: 16px;
  background: rgba(255, 252, 247, 0.92);
}

.metric-card .el-icon {
  margin-top: 2px;
  color: var(--brand-secondary-strong);
  font-size: 18px;
}

.metric-card span,
.metric-card strong {
  display: block;
}

.metric-card span {
  font-size: 12px;
  color: var(--text-muted);
}

.metric-card strong {
  margin-top: 8px;
  line-height: 1.6;
  color: var(--text-primary);
}

.result-alert {
  margin-top: 16px;
}

.pagination {
  margin: 28px 0 12px;
  display: flex;
  justify-content: center;
}

@media (max-width: 1280px) {
  .search-toolbar {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 1100px) {
  .search-body,
  .result-metrics,
  .result-grid {
    grid-template-columns: 1fr;
  }

  .filter-panel {
    position: static;
  }
}

@media (max-width: 760px) {
  .search-toolbar {
    grid-template-columns: 1fr;
  }

  .star-options {
    grid-template-columns: 1fr 1fr;
  }

  .filter-heading,
  .result-summary {
    flex-direction: column;
    align-items: flex-start;
  }
}
</style>
