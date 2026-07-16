<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { createHotel, deleteHotel, getCities, getHotels, updateHotel } from '@/api/resource'
import { getHotelDisplayName, getHotelSecondaryName } from '@/utils/hotel'

const hotels = ref([])
const cities = ref([])
const loading = ref(false)
const cityLoading = ref(false)
const submitting = ref(false)
const showDialog = ref(false)
const loadFailed = ref(false)
const cityLoadFailed = ref(false)
const editingHotelId = ref(null)
const formRef = ref()

const defaultForm = () => ({
  nameCn: '',
  nameEn: '',
  cityId: null,
  starLevel: 4,
  address: '',
  longitude: '',
  latitude: '',
  brand: '',
  description: '',
  imageUrlsText: '',
  facilitiesText: ''
})

const form = reactive(defaultForm())

function createTrimmedRequiredRule(label) {
  return {
    trigger: 'blur',
    validator: (rule, value, callback) => {
      if (!String(value || '').trim()) {
        callback(new Error(`请输入${label}`))
        return
      }
      callback()
    }
  }
}

function validateHotelName(rule, value, callback) {
  const nameCn = String(form.nameCn || '').trim()
  const nameEn = String(form.nameEn || '').trim()
  if (!nameCn && !nameEn) {
    callback(new Error('中文名和英文名至少填写一个'))
    return
  }
  callback()
}

const rules = {
  nameCn: [{ trigger: 'blur', validator: validateHotelName }],
  nameEn: [{ trigger: 'blur', validator: validateHotelName }],
  cityId: [{ required: true, message: '请选择所属城市', trigger: 'change' }],
  starLevel: [{ required: true, message: '请选择酒店星级', trigger: 'change' }],
  brand: [createTrimmedRequiredRule('酒店品牌')],
  address: [createTrimmedRequiredRule('酒店地址')],
  description: [
    createTrimmedRequiredRule('酒店描述'),
    { min: 10, message: '酒店描述至少 10 个字', trigger: 'blur' }
  ]
}

const isEditing = computed(() => editingHotelId.value !== null)
const cityNameMap = computed(() =>
  cities.value.reduce((map, city) => {
    map[city.id] = city.nameCn
    return map
  }, {})
)

const displayHotels = computed(() =>
  hotels.value.map((hotel) => ({
    ...hotel,
    nameDisplay: getHotelDisplayName(hotel),
    secondaryNameDisplay: getHotelSecondaryName(hotel),
    cityNameDisplay: hotel.cityName || cityNameMap.value[hotel.cityId] || '未关联城市',
    scoreDisplay: hotel.score == null ? '暂无' : Number(hotel.score).toFixed(1),
    brandDisplay: hotel.brand || '未填写品牌',
    addressDisplay: hotel.address || '未填写地址'
  }))
)

const summaryStats = computed(() => [
  { label: '酒店总数', value: hotels.value.length },
  { label: '覆盖城市', value: new Set(hotels.value.map((item) => item.cityId).filter(Boolean)).size },
  {
    label: '平均评分',
    value: hotels.value.filter((item) => item.score != null).length
      ? (
          hotels.value.reduce((sum, item) => sum + Number(item.score || 0), 0) /
          hotels.value.filter((item) => item.score != null).length
        ).toFixed(1)
      : '--'
  }
])

onMounted(async () => {
  await Promise.all([fetchHotels(), fetchCities()])
})

async function fetchHotels() {
  loading.value = true
  try {
    const data = await getHotels({ page: 1, size: 100 }, { silent: true })
    hotels.value = data.records || []
    loadFailed.value = false
  } catch (error) {
    hotels.value = []
    loadFailed.value = true
  } finally {
    loading.value = false
  }
}

async function fetchCities() {
  cityLoading.value = true
  try {
    const data = await getCities({ page: 1, size: 100 }, { silent: true })
    cities.value = data.records || []
    cityLoadFailed.value = false
    if (!form.cityId && cities.value.length) {
      form.cityId = cities.value[0].id
    }
  } catch (error) {
    cities.value = []
    cityLoadFailed.value = true
  } finally {
    cityLoading.value = false
  }
}

function resetForm() {
  Object.assign(form, defaultForm())
  editingHotelId.value = null
  if (cities.value.length) {
    form.cityId = cities.value[0].id
  }
  formRef.value?.clearValidate()
}

function openCreateDialog() {
  resetForm()
  showDialog.value = true
}

function openEditDialog(row) {
  editingHotelId.value = row.id
  form.nameCn = row.nameCn || ''
  form.nameEn = row.nameEn || ''
  form.cityId = row.cityId || cities.value.find((item) => item.nameCn === row.cityName)?.id || cities.value[0]?.id || null
  form.starLevel = Number(row.starLevel || 4)
  form.address = row.address || ''
  form.longitude = row.longitude ?? ''
  form.latitude = row.latitude ?? ''
  form.brand = row.brand || ''
  form.description = row.description || ''
  form.imageUrlsText = Array.isArray(row.images) ? row.images.map((item) => item.url).filter(Boolean).join('\n') : ''
  form.facilitiesText = Array.isArray(row.facilities) ? row.facilities.filter(Boolean).join('、') : ''
  formRef.value?.clearValidate()
  showDialog.value = true
}

function normalizeFormTextFields() {
  form.nameCn = form.nameCn.trim()
  form.nameEn = form.nameEn.trim()
  form.address = form.address.trim()
  form.brand = form.brand.trim()
  form.description = form.description.trim()
  form.imageUrlsText = form.imageUrlsText.trim()
  form.facilitiesText = form.facilitiesText.trim()
}

function splitTextList(value) {
  return String(value || '')
    .split(/[\n,，、]+/)
    .map((item) => item.trim())
    .filter(Boolean)
}

function toNullableNumber(value) {
  const normalized = String(value ?? '').trim()
  if (!normalized) return null
  const numericValue = Number(normalized)
  return Number.isFinite(numericValue) ? numericValue : null
}

async function handleSubmit() {
  normalizeFormTextFields()
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return

  submitting.value = true
  try {
    const payload = {
      ...form,
      longitude: toNullableNumber(form.longitude),
      latitude: toNullableNumber(form.latitude),
      imageUrls: splitTextList(form.imageUrlsText),
      facilities: splitTextList(form.facilitiesText)
    }

    delete payload.imageUrlsText
    delete payload.facilitiesText

    if (isEditing.value) {
      await updateHotel(editingHotelId.value, payload)
      ElMessage.success('酒店信息已更新')
    } else {
      await createHotel(payload)
      ElMessage.success('新增成功')
    }

    showDialog.value = false
    resetForm()
    await fetchHotels()
  } catch (error) {
    ElMessage.error(error.message || (isEditing.value ? '更新失败，请稍后重试' : '新增失败，请稍后重试'))
  } finally {
    submitting.value = false
  }
}

async function handleDelete(id) {
  try {
    await ElMessageBox.confirm('删除后该酒店将从后台列表中移除，确定继续吗？', '确认删除酒店', {
      type: 'warning',
      confirmButtonText: '确认删除',
      cancelButtonText: '取消'
    })
    await deleteHotel(id)
    ElMessage.success('删除成功')
    await fetchHotels()
  } catch (error) {
    if (error === 'cancel' || error === 'close') return
    ElMessage.error(error.message || '删除失败，请稍后重试')
  }
}
</script>

<template>
  <div class="admin-hotels">
    <div class="stats-grid">
      <div v-for="stat in summaryStats" :key="stat.label" class="stat-card">
        <span>{{ stat.label }}</span>
        <strong>{{ stat.value }}</strong>
      </div>
    </div>

    <section class="table-panel soft-card">
      <div class="toolbar">
        <div>
          <h3>酒店列表</h3>
          <p>支持查看、新增、编辑、删除酒店，方便维护真实世界酒店资料、地址和坐标。</p>
        </div>
        <div class="toolbar-actions">
          <el-button @click="fetchHotels">重新加载</el-button>
          <el-button type="primary" class="primary-btn" @click="openCreateDialog">新增酒店</el-button>
        </div>
      </div>

      <el-alert
        v-if="loadFailed"
        class="page-alert"
        type="warning"
        :closable="false"
        title="酒店数据暂时加载失败，请稍后重试"
      />

      <el-table :data="displayHotels" v-loading="loading">
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column label="酒店名称" min-width="220">
          <template #default="{ row }">
            <div class="hotel-name-cell">
              <strong>{{ row.nameDisplay }}</strong>
              <span v-if="row.secondaryNameDisplay">{{ row.secondaryNameDisplay }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="cityNameDisplay" label="城市" width="120" />
        <el-table-column prop="brandDisplay" label="品牌" min-width="140" />
        <el-table-column prop="starLevel" label="星级" width="100" />
        <el-table-column prop="scoreDisplay" label="评分" width="100" />
        <el-table-column prop="addressDisplay" label="地址" min-width="220" show-overflow-tooltip />
        <el-table-column label="操作" width="180">
          <template #default="{ row }">
            <div class="row-actions">
              <el-button size="small" @click="openEditDialog(row)">编辑</el-button>
              <el-button size="small" type="danger" @click="handleDelete(row.id)">删除</el-button>
            </div>
          </template>
        </el-table-column>
      </el-table>

      <el-empty v-if="!loading && hotels.length === 0" :description="loadFailed ? '酒店数据暂时不可用' : '当前暂无酒店数据'">
        <el-button v-if="loadFailed" type="primary" class="primary-btn" @click="fetchHotels">重新加载</el-button>
        <el-button v-else type="primary" class="primary-btn" @click="openCreateDialog">立即新增第一家酒店</el-button>
      </el-empty>
    </section>

    <el-dialog
      v-model="showDialog"
      :title="isEditing ? '编辑酒店' : '新增酒店'"
      width="640px"
      :close-on-click-modal="false"
      @closed="resetForm"
    >
      <el-alert
        v-if="cityLoadFailed"
        class="page-alert"
        type="warning"
        :closable="false"
        title="城市列表暂时加载失败，请先恢复资源服务后再提交酒店信息"
      />

      <el-form ref="formRef" :model="form" :rules="rules" label-position="top">
        <div class="dialog-grid">
          <el-form-item label="中文名称" prop="nameCn">
            <el-input v-model="form.nameCn" placeholder="例如：北京中国大饭店" />
          </el-form-item>

          <el-form-item label="英文名称" prop="nameEn">
            <el-input v-model="form.nameEn" placeholder="例如：China World Hotel, Beijing" />
          </el-form-item>

          <el-form-item label="所属城市" prop="cityId">
            <el-select
              v-model="form.cityId"
              class="field-full"
              placeholder="请选择城市"
              filterable
              :loading="cityLoading"
              :disabled="cityLoadFailed"
            >
              <el-option v-for="city in cities" :key="city.id" :label="city.nameCn" :value="city.id" />
            </el-select>
          </el-form-item>

          <el-form-item label="星级" prop="starLevel">
            <el-select v-model="form.starLevel" class="field-full" placeholder="请选择星级">
              <el-option v-for="star in [5, 4, 3, 2, 1]" :key="star" :label="`${star} 星`" :value="star" />
            </el-select>
          </el-form-item>

          <el-form-item label="品牌" prop="brand">
            <el-input v-model="form.brand" placeholder="例如：香格里拉集团" />
          </el-form-item>

          <el-form-item label="经度">
            <el-input v-model="form.longitude" placeholder="例如：116.4579" />
          </el-form-item>

          <el-form-item label="纬度">
            <el-input v-model="form.latitude" placeholder="例如：39.9085" />
          </el-form-item>
        </div>

        <el-form-item label="地址" prop="address">
          <el-input v-model="form.address" placeholder="请输入详细地址" />
        </el-form-item>

        <el-form-item label="酒店描述" prop="description">
          <el-input v-model="form.description" type="textarea" :rows="4" placeholder="请填写酒店位置、服务和房型亮点" />
        </el-form-item>

        <el-form-item label="酒店图片链接">
          <el-input
            v-model="form.imageUrlsText"
            type="textarea"
            :rows="3"
            placeholder="每行一条图片链接，首张会优先作为酒店封面"
          />
        </el-form-item>

        <el-form-item label="酒店设施">
          <el-input
            v-model="form.facilitiesText"
            type="textarea"
            :rows="3"
            placeholder="可输入免费 WiFi、洗衣房、健身房等，支持顿号、逗号或换行分隔"
          />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="showDialog = false">取消</el-button>
        <el-button
          type="primary"
          class="primary-btn"
          :loading="submitting"
          :disabled="cityLoadFailed"
          @click="handleSubmit"
        >
          {{ isEditing ? '保存修改' : '确认创建' }}
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.stats-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 16px;
}

.stat-card,
.table-panel {
  background: rgba(255, 252, 247, 0.92);
}

.stat-card {
  padding: 20px;
  border-radius: 20px;
}

.stat-card span {
  display: block;
  font-size: 12px;
  color: var(--text-muted);
}

.stat-card strong {
  display: block;
  margin-top: 10px;
  font-size: 30px;
  color: var(--brand-primary);
}

.table-panel {
  margin-top: 18px;
  padding: 22px;
  overflow-x: auto;
}

.page-alert {
  margin-bottom: 16px;
}

.toolbar {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  align-items: center;
  margin-bottom: 18px;
}

.toolbar-actions,
.row-actions {
  display: flex;
  gap: 8px;
}

.toolbar h3 {
  margin: 0;
  font-size: 24px;
}

.toolbar p {
  margin: 8px 0 0;
  color: var(--text-secondary);
}

.row-actions {
  flex-wrap: wrap;
}

.hotel-name-cell strong,
.hotel-name-cell span {
  display: block;
}

.hotel-name-cell span {
  margin-top: 4px;
  font-size: 12px;
  color: var(--text-muted);
}

.dialog-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.field-full {
  width: 100%;
}

.primary-btn {
  --el-button-bg-color: var(--brand-primary);
  --el-button-border-color: var(--brand-primary);
  --el-button-text-color: #fff8f0;
  --el-button-hover-bg-color: #7b6857;
  --el-button-hover-border-color: #7b6857;
  --el-button-hover-text-color: #fffdfa;
}

@media (max-width: 1200px) {
  .stats-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 860px) {
  .stats-grid,
  .dialog-grid {
    grid-template-columns: 1fr;
  }

  .toolbar,
  .toolbar-actions,
  .row-actions {
    flex-direction: column;
    align-items: stretch;
  }

  .table-panel {
    padding: 18px 16px;
  }

  .toolbar h3 {
    font-size: 22px;
  }
}

@media (max-width: 640px) {
  .stat-card {
    padding: 16px;
  }

  .stat-card strong {
    font-size: 26px;
  }

  .table-panel {
    margin-top: 14px;
    border-radius: 18px;
  }

  :deep(.el-dialog) {
    width: calc(100vw - 24px) !important;
    max-width: 640px;
  }
}
</style>
