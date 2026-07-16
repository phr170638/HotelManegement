<script setup>
import { computed, onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getAdminOrders, refundOrder } from '@/api/order'
import StatusBadge from '@/components/StatusBadge.vue'

const orders = ref([])
const loading = ref(false)
const loadFailed = ref(false)

const summaryStats = computed(() => [
  { label: '订单总量', value: orders.value.length },
  { label: '待处理退款', value: orders.value.filter((item) => item.status === 4).length },
  { label: '已支付订单', value: orders.value.filter((item) => item.status === 1).length }
])

onMounted(() => fetchOrders())

async function fetchOrders() {
  loading.value = true
  try {
    const data = await getAdminOrders({ page: 1, size: 50 }, { silent: true })
    orders.value = data.records || []
    loadFailed.value = false
  } catch (error) {
    orders.value = []
    loadFailed.value = true
  } finally {
    loading.value = false
  }
}

async function handleRefund(id) {
  try {
    await ElMessageBox.confirm('确认处理这笔订单的退款吗？该操作会直接影响用户订单状态。', '确认处理退款', {
      type: 'warning',
      confirmButtonText: '确认处理',
      cancelButtonText: '取消'
    })
    await refundOrder(id, '管理员退房处理')
    ElMessage.success('已完成处理')
    fetchOrders()
  } catch (error) {
    if (error === 'cancel') return
    ElMessage.error(error.message || '处理失败，请稍后重试')
  }
}
</script>

<template>
  <div class="admin-orders">
    <div class="stats-grid">
      <div v-for="stat in summaryStats" :key="stat.label" class="stat-card">
        <span>{{ stat.label }}</span>
        <strong>{{ stat.value }}</strong>
      </div>
    </div>

    <section class="table-panel soft-card">
      <div class="toolbar">
        <div>
          <h3>订单列表</h3>
          <p>统一查看待支付、已支付、退款中等业务状态，便于运营处理。</p>
        </div>
        <div class="toolbar-actions">
          <el-button @click="fetchOrders">重新加载</el-button>
        </div>
      </div>

      <el-alert
        v-if="loadFailed"
        class="page-alert"
        type="warning"
        :closable="false"
        title="订单数据暂时加载失败，请稍后重试"
      />

      <el-table :data="orders" v-loading="loading">
        <el-table-column prop="orderNo" label="订单号" min-width="180" />
        <el-table-column prop="guestName" label="入住人" width="120" />
        <el-table-column prop="totalAmount" label="金额" width="120" />
        <el-table-column label="状态" width="140">
          <template #default="{ row }">
            <StatusBadge :status="row.status" />
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" min-width="180" />
        <el-table-column label="操作" width="140">
          <template #default="{ row }">
            <el-button v-if="row.status === 1 || row.status === 4" size="small" type="warning" @click="handleRefund(row.id)">
              处理退房
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-empty v-if="!loading && orders.length === 0" :description="loadFailed ? '订单服务暂时不可用' : '当前暂无订单数据'">
        <el-button type="primary" class="primary-btn" @click="fetchOrders">重新加载</el-button>
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
  overflow-x: auto;
}

.page-alert {
  margin-bottom: 16px;
}

.toolbar h3 {
  margin: 0;
  font-size: 24px;
}

.toolbar p {
  margin: 8px 0 18px;
  color: var(--text-secondary);
}

.toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
}

.toolbar-actions {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}

@media (max-width: 1200px) {
  .stats-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 860px) {
  .stats-grid {
    grid-template-columns: 1fr;
  }

  .toolbar,
  .toolbar-actions {
    flex-direction: column;
    align-items: stretch;
  }

  .table-panel {
    padding: 18px 16px;
  }
}

@media (max-width: 640px) {
  .stat-card {
    padding: 16px;
  }

  .stat-card strong {
    font-size: 26px;
  }
}
</style>
