<script setup>
import { computed, onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import request from '@/api/request'

const users = ref([])
const loading = ref(false)
const loadFailed = ref(false)

const summaryStats = computed(() => [
  { label: '用户总量', value: users.value.length },
  { label: '正常用户', value: users.value.filter((item) => item.status === 1).length },
  { label: '禁用用户', value: users.value.filter((item) => item.status !== 1).length }
])

onMounted(() => fetchUsers())

async function fetchUsers() {
  loading.value = true
  try {
    const data = await request.get('/admin/users', { params: { page: 1, size: 50 }, silent: true })
    users.value = data.records || []
    loadFailed.value = false
  } catch (error) {
    users.value = []
    loadFailed.value = true
  } finally {
    loading.value = false
  }
}

async function toggleStatus(row) {
  const newStatus = row.status === 1 ? 0 : 1
  try {
    await ElMessageBox.confirm(
      row.status === 1 ? '禁用后该用户将无法继续登录，确定继续吗？' : '确认恢复该用户的登录权限吗？',
      row.status === 1 ? '确认禁用用户' : '确认启用用户',
      {
        type: 'warning',
        confirmButtonText: row.status === 1 ? '确认禁用' : '确认启用',
        cancelButtonText: '取消'
      }
    )
    await request.put(`/admin/users/${row.id}/status`, null, { params: { status: newStatus } })
    ElMessage.success('操作成功')
    fetchUsers()
  } catch (error) {
    if (error === 'cancel') return
    ElMessage.error(error.message || '状态更新失败，请稍后重试')
  }
}
</script>

<template>
  <div class="admin-users">
    <div class="stats-grid">
      <div v-for="stat in summaryStats" :key="stat.label" class="stat-card">
        <span>{{ stat.label }}</span>
        <strong>{{ stat.value }}</strong>
      </div>
    </div>

    <section class="table-panel soft-card">
      <div class="toolbar">
        <div>
          <h3>用户管理</h3>
          <p>支持查看用户信息，并对账号状态进行启用或禁用管理。</p>
        </div>
      </div>

      <el-alert
        v-if="loadFailed"
        class="page-alert"
        type="warning"
        :closable="false"
        title="用户数据暂时加载失败，请稍后重试"
      />

      <el-table :data="users" v-loading="loading">
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="phone" label="手机号" min-width="150" />
        <el-table-column prop="email" label="邮箱" min-width="180" />
        <el-table-column prop="nickname" label="昵称" width="140" />
        <el-table-column label="状态" width="120">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'danger'">
              {{ row.status === 1 ? '正常' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="注册时间" min-width="180" />
        <el-table-column label="操作" width="140">
          <template #default="{ row }">
            <el-button size="small" :type="row.status === 1 ? 'danger' : 'success'" @click="toggleStatus(row)">
              {{ row.status === 1 ? '禁用' : '启用' }}
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-empty v-if="!loading && users.length === 0" :description="loadFailed ? '用户服务暂时不可用' : '当前暂无用户数据'">
        <el-button type="primary" class="primary-btn" @click="fetchUsers">重新加载</el-button>
      </el-empty>
    </section>
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
}

.page-alert {
  margin-bottom: 16px;
}

.primary-btn {
  --el-button-bg-color: var(--brand-primary);
  --el-button-border-color: var(--brand-primary);
  --el-button-hover-bg-color: #7b6857;
  --el-button-hover-border-color: #7b6857;
}

.toolbar h3 {
  margin: 0;
  font-size: 24px;
}

.toolbar p {
  margin: 8px 0 18px;
  color: var(--text-secondary);
}

@media (max-width: 860px) {
  .stats-grid {
    grid-template-columns: 1fr;
  }
}
</style>
