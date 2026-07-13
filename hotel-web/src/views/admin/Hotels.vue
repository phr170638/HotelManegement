<template>
  <div class="admin-hotels">
    <div class="toolbar">
      <h3>酒店管理</h3>
      <el-button type="primary" @click="showDialog = true">新增酒店</el-button>
    </div>
    <el-table :data="hotels" v-loading="loading" border>
      <el-table-column prop="id" label="ID" width="60" />
      <el-table-column prop="nameCn" label="酒店名称" />
      <el-table-column prop="starLevel" label="星级" width="80" />
      <el-table-column prop="score" label="评分" width="80" />
      <el-table-column label="操作" width="160">
        <template #default="{ row }">
          <el-button size="small" type="danger" @click="handleDelete(row.id)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- 新增酒店弹窗 -->
    <el-dialog v-model="showDialog" title="新增酒店" width="500px">
      <el-form :model="form" label-width="80px">
        <el-form-item label="酒店名称">
          <el-input v-model="form.nameCn" />
        </el-form-item>
        <el-form-item label="城市ID">
          <el-input-number v-model="form.cityId" :min="1" />
        </el-form-item>
        <el-form-item label="星级">
          <el-input-number v-model="form.starLevel" :min="1" :max="5" />
        </el-form-item>
        <el-form-item label="地址">
          <el-input v-model="form.address" />
        </el-form-item>
        <el-form-item label="品牌">
          <el-input v-model="form.brand" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="form.description" type="textarea" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showDialog = false">取消</el-button>
        <el-button type="primary" @click="handleCreate">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { getHotels, createHotel, deleteHotel } from '@/api/resource'
import { ElMessage } from 'element-plus'

const hotels = ref([])
const loading = ref(false)
const showDialog = ref(false)
const form = ref({ nameCn: '', cityId: 1, starLevel: 4, address: '', brand: '', description: '' })

onMounted(() => fetchHotels())

async function fetchHotels() {
  loading.value = true
  try {
    hotels.value = (await getHotels({ page: 1, size: 100 })).records || []
  } finally {
    loading.value = false
  }
}

async function handleCreate() {
  try {
    await createHotel(form.value)
    ElMessage.success('新增成功')
    showDialog.value = false
    fetchHotels()
  } catch (e) {
    ElMessage.error(e.message || '新增失败')
  }
}

async function handleDelete(id) {
  try {
    await deleteHotel(id)
    ElMessage.success('删除成功')
    fetchHotels()
  } catch (e) {
    ElMessage.error(e.message || '删除失败')
  }
}
</script>

<style scoped>
.toolbar { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px; }
</style>
