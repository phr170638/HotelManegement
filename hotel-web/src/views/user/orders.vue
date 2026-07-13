<template>
  <div class="orders-page">
    <h2>我的订单</h2>
    <el-tabs v-model="activeTab" @tab-change="handleTabChange">
      <el-tab-pane label="全部" name="" />
      <el-tab-pane label="待支付" name="0" />
      <el-tab-pane label="已支付" name="1" />
      <el-tab-pane label="已取消" name="2" />
      <el-tab-pane label="已完成" name="6" />
    </el-tabs>

    <div v-loading="loading">
      <el-card v-for="order in orders" :key="order.id" class="order-card">
        <div class="order-header">
          <span>订单号：{{ order.orderNo }}</span>
          <el-tag :type="statusType(order.status)">{{ order.statusText }}</el-tag>
        </div>
        <div class="order-body">
          <div>
            <h4>{{ order.hotelName }}</h4>
            <p>{{ order.checkInDate }} ~ {{ order.checkOutDate }}</p>
          </div>
          <div class="order-amount">¥{{ order.totalAmount }}</div>
        </div>
        <div class="order-actions">
          <el-button v-if="order.status === 0" type="primary" size="small" @click="$router.push(`/order/pay/${order.id}`)">去支付</el-button>
          <el-button v-if="order.status === 0" size="small" @click="cancelOrder(order.id)">取消</el-button>
        </div>
      </el-card>
      <el-empty v-if="!loading && orders.length === 0" description="暂无订单" />
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { getMyOrders } from '@/api/user'
import { cancelOrder as cancelOrderApi } from '@/api/order'
import { ElMessage } from 'element-plus'

const activeTab = ref('')
const orders = ref([])
const loading = ref(false)

onMounted(() => fetchOrders())

function handleTabChange(tab) {
  activeTab.value = tab
  fetchOrders()
}

async function fetchOrders() {
  loading.value = true
  try {
    const status = activeTab.value ? Number(activeTab.value) : null
    const data = await getMyOrders(1, 20, status)
    orders.value = data.records || []
  } catch (e) {
    orders.value = []
  } finally {
    loading.value = false
  }
}

async function cancelOrder(id) {
  try {
    await cancelOrderApi(id)
    ElMessage.success('订单已取消')
    fetchOrders()
  } catch (e) {
    ElMessage.error(e.message || '取消失败')
  }
}

function statusType(status) {
  return { 0: 'warning', 1: 'success', 2: 'info', 3: '', 4: 'warning', 5: '', 6: 'success' }[status] || 'info'
}
</script>

<style scoped>
.orders-page { max-width: 800px; margin: 40px auto; padding: 0 20px; }
.order-card { margin-bottom: 12px; }
.order-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 12px; }
.order-body { display: flex; justify-content: space-between; align-items: center; }
.order-amount { font-size: 18px; color: #F56C6C; font-weight: bold; }
.order-actions { margin-top: 12px; display: flex; gap: 8px; justify-content: flex-end; }
</style>
