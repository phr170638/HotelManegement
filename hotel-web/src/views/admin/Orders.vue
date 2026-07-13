<template>
  <div>
    <h3 style="margin-bottom:16px">订单管理</h3>
    <el-table :data="orders" v-loading="loading" border>
      <el-table-column prop="orderNo" label="订单号" width="180" />
      <el-table-column prop="guestName" label="入住人" width="100" />
      <el-table-column prop="totalAmount" label="金额" width="100" />
      <el-table-column label="状态" width="100">
        <template #default="{ row }">
          <el-tag :type="statusType(row.status)">{{ statusText(row.status) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="createTime" label="创建时间" width="180" />
      <el-table-column label="操作">
        <template #default="{ row }">
          <el-button size="small" type="warning" @click="handleRefund(row.id)" v-if="row.status === 1">退票</el-button>
        </template>
      </el-table-column>
    </el-table>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { getAdminOrders, refundOrder } from '@/api/order'
import { ElMessage } from 'element-plus'

const orders = ref([])
const loading = ref(false)

onMounted(() => fetchOrders())

async function fetchOrders() {
  loading.value = true
  try { orders.value = (await getAdminOrders({ page: 1, size: 50 })).records || [] }
  finally { loading.value = false }
}

async function handleRefund(id) {
  try {
    await refundOrder(id, '管理员退票')
    ElMessage.success('退票成功')
    fetchOrders()
  } catch (e) { ElMessage.error(e.message || '操作失败') }
}

function statusType(s) { return { 0:'warning',1:'success',2:'info',6:'success' }[s] || 'info' }
function statusText(s) { return { 0:'待支付',1:'已支付',2:'已取消',3:'已入住',4:'退房申请中',5:'已退房',6:'已完成' }[s] || '未知' }
</script>
