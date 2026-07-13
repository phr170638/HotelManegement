<template>
  <div>
    <h3 style="margin-bottom:16px">用户管理</h3>
    <el-table :data="users" v-loading="loading" border>
      <el-table-column prop="id" label="ID" width="60" />
      <el-table-column prop="phone" label="手机号" width="140" />
      <el-table-column prop="email" label="邮箱" width="180" />
      <el-table-column prop="nickname" label="昵称" width="120" />
      <el-table-column label="状态" width="80">
        <template #default="{ row }">
          <el-tag :type="row.status === 1 ? 'success' : 'danger'">{{ row.status === 1 ? '正常' : '禁用' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="createTime" label="注册时间" width="180" />
      <el-table-column label="操作">
        <template #default="{ row }">
          <el-button size="small" :type="row.status === 1 ? 'danger' : 'success'" @click="toggleStatus(row)">
            {{ row.status === 1 ? '禁用' : '启用' }}
          </el-button>
        </template>
      </el-table-column>
    </el-table>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import request from '@/api/request'
import { ElMessage } from 'element-plus'

const users = ref([])
const loading = ref(false)

onMounted(() => fetchUsers())

async function fetchUsers() {
  loading.value = true
  try { users.value = (await request.get('/admin/users', { params: { page: 1, size: 50 } })).records || [] }
  finally { loading.value = false }
}

async function toggleStatus(row) {
  const newStatus = row.status === 1 ? 0 : 1
  try {
    await request.put(`/admin/users/${row.id}/status`, null, { params: { status: newStatus } })
    ElMessage.success('操作成功')
    fetchUsers()
  } catch (e) { ElMessage.error(e.message || '操作失败') }
}
</script>
