<template>
  <div class="search-page">
    <!-- 搜索栏 -->
    <div class="search-header">
      <div class="search-bar">
        <el-input v-model="searchParams.keyword" placeholder="搜索酒店" clearable @clear="doSearch" @keyup.enter="doSearch">
          <template #prepend>
            <el-select v-model="searchParams.cityId" placeholder="选择城市" clearable style="width:160px">
              <el-option v-for="c in cities" :key="c.id" :label="c.nameCn" :value="c.id" />
            </el-select>
          </template>
        </el-input>
        <el-button type="primary" @click="doSearch" style="margin-left:12px">搜索</el-button>
      </div>
    </div>

    <div class="search-body">
      <!-- 筛选面板 -->
      <aside class="filter-panel">
        <el-card header="筛选条件">
          <div class="filter-group">
            <label>星级</label>
            <el-checkbox-group v-model="filters.starLevels">
              <el-checkbox v-for="s in [5,4,3,2,1]" :key="s" :label="s" :value="s">
                {{ '★'.repeat(s) }}
              </el-checkbox>
            </el-checkbox-group>
          </div>
          <div class="filter-group">
            <label>价格区间</label>
            <el-slider
              v-model="priceRange"
              range
              :min="0"
              :max="5000"
              :step="100"
              @change="doSearch"
            />
            <span class="price-label">¥{{ priceRange[0] }} - ¥{{ priceRange[1] }}</span>
          </div>
          <el-button type="primary" @click="doSearch" style="width:100%">筛选</el-button>
          <el-button @click="resetFilters" style="width:100%;margin-top:8px">重置</el-button>
        </el-card>
      </aside>

      <!-- 搜索结果 -->
      <main class="result-list" v-loading="loading">
        <div class="result-header">
          <span>共 {{ total }} 家酒店</span>
        </div>
        <HotelCard v-for="hotel in results" :key="hotel.id" :hotel="hotel" />
        <el-empty v-if="!loading && results.length === 0" description="未找到符合条件的酒店" />
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
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { searchHotels } from '@/api/search'
import { getCities } from '@/api/resource'
import HotelCard from '@/components/HotelCard.vue'

const route = useRoute()
const loading = ref(false)
const results = ref([])
const total = ref(0)
const cities = ref([])
const priceRange = ref([0, 5000])

const searchParams = reactive({
  cityId: route.query.cityId || null,
  keyword: route.query.keyword || '',
  page: 1,
  size: 10
})

const filters = reactive({
  starLevels: []
})

onMounted(async () => {
  try {
    cities.value = (await getCities({ page: 1, size: 100 })).records || []
  } catch (e) { /* ignore */ }
  if (searchParams.keyword || searchParams.cityId) doSearch()
})

async function doSearch() {
  loading.value = true
  try {
    const params = {
      ...searchParams,
      minPrice: priceRange.value[0] || null,
      maxPrice: priceRange.value[1] < 5000 ? priceRange.value[1] : null,
      starLevel: filters.starLevels.length === 1 ? filters.starLevels[0] : null
    }
    const data = await searchHotels(params)
    results.value = data.records || []
    total.value = data.total || 0
  } catch (e) {
    results.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

function resetFilters() {
  filters.starLevels = []
  priceRange.value = [0, 5000]
  doSearch()
}
</script>

<style scoped>
.search-page { max-width: 1200px; margin: 0 auto; padding: 20px; }
.search-header { margin-bottom: 20px; }
.search-bar { display: flex; }
.search-body { display: flex; gap: 20px; }
.filter-panel { width: 240px; flex-shrink: 0; }
.filter-group { margin-bottom: 16px; }
.filter-group label { display: block; font-size: 14px; color: #606266; margin-bottom: 8px; }
.price-label { font-size: 12px; color: #909399; }
.result-list { flex: 1; }
.result-header { margin-bottom: 12px; font-size: 14px; color: #909399; }
.pagination { margin-top: 20px; display: flex; justify-content: center; }
</style>
