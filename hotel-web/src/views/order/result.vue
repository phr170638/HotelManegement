<script setup>
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { House, OfficeBuilding, Tickets } from '@element-plus/icons-vue'
import { getOrderDetail, syncPaymentStatus } from '@/api/order'

const route = useRoute()
const router = useRouter()

const orderId = computed(() => String(route.query.orderId || route.query.id || ''))
const hasOrderId = computed(() => /^\d+$/.test(orderId.value))
const syncing = ref(false)
const orderStatus = ref(null)
const orderStatusText = ref('待确认')
const maxPollCount = 8
let pollTimer = null
let pollCount = 0

const pageTitle = computed(() => (orderStatus.value === 1 ? '支付成功' : '支付结果确认中'))
const pageDescription = computed(() => (
  orderStatus.value === 1
    ? '你的订单支付已完成，可以继续查看订单状态或返回首页。'
    : '系统正在等待支付回调或主动对账同步完成，请稍候刷新订单状态。'
))
const statusTitle = computed(() => (orderStatus.value === 1 ? '支付成功' : '处理中'))
const statusDescription = computed(() => (
  orderStatus.value === 1
    ? '订单已完成支付，后续可在订单中心继续查看入住安排。'
    : '支付结果以服务端异步通知或主动对账结果为准，请耐心等待几秒。'
))
const alertTitle = computed(() => (
  orderStatus.value === 1
    ? '支付回调和订单更新已完成，你现在可以前往订单中心查看后续入住安排。'
    : '支付结果正在通过服务端异步链路与主动对账同步，页面会自动查询最新订单状态。'
))

onMounted(async () => {
  if (hasOrderId.value) {
    await syncOrderStatus(true)
  }
})

onBeforeUnmount(() => {
  window.clearTimeout(pollTimer)
})

async function syncOrderStatus(shouldPoll = false) {
  syncing.value = true
  try {
    const order = shouldPoll
      ? await syncPaymentStatus(orderId.value, { silent: true })
      : await getOrderDetail(orderId.value, { silent: true })
    orderStatus.value = order?.status ?? null
    orderStatusText.value = order?.statusText || '待确认'

    if (orderStatus.value === 1 || !shouldPoll || pollCount >= maxPollCount) {
      window.clearTimeout(pollTimer)
      pollTimer = null
      return
    }

    pollCount += 1
    pollTimer = window.setTimeout(() => {
      syncOrderStatus(true)
    }, 2000)
  } catch (error) {
    ElMessage.error(error.message || '订单状态查询失败')
  } finally {
    syncing.value = false
  }
}

function goOrders() {
  router.push('/user/orders')
}

function goCenter() {
  router.push('/user/center')
}
</script>

<template>
  <div class="result-page page-container narrow">
    <section class="result-card soft-card" v-loading="syncing">
      <div class="gold-chip">Payment Result</div>
      <h1>{{ pageTitle }}</h1>
      <p class="sub-copy">{{ pageDescription }}</p>

      <el-alert
        class="status-alert"
        :type="orderStatus === 1 ? 'success' : 'info'"
        show-icon
        :closable="false"
        :title="alertTitle"
      />

      <el-result
        icon="success"
        :title="statusTitle"
        :sub-title="statusDescription"
      />

      <div class="info-grid">
        <div class="info-card">
          <span>订单编号</span>
          <strong>{{ hasOrderId ? orderId : '未识别到订单编号' }}</strong>
        </div>
        <div class="info-card">
          <span>支付方式</span>
          <strong>支付宝</strong>
        </div>
        <div class="info-card">
          <span>订单状态</span>
          <strong>{{ orderStatusText }}</strong>
        </div>
      </div>

      <div class="actions">
        <el-button type="primary" class="primary-btn" :icon="Tickets" @click="goOrders">查看我的订单</el-button>
        <el-button :icon="OfficeBuilding" @click="goCenter">前往个人中心</el-button>
        <el-button :loading="syncing" @click="syncOrderStatus(false)">刷新订单状态</el-button>
        <el-button @click="$router.push('/search')">继续浏览酒店</el-button>
        <el-button :icon="House" @click="$router.push('/')">返回首页</el-button>
      </div>
    </section>
  </div>
</template>

<style scoped>
.result-page {
  padding: 32px 0 0;
}

.result-card {
  padding: 32px;
  background: rgba(255, 252, 247, 0.92);
}

.result-card h1 {
  margin: 18px 0 8px;
  font-size: clamp(30px, 4vw, 44px);
}

.sub-copy {
  margin: 0;
  color: var(--text-secondary);
  line-height: 1.8;
}

.status-alert {
  margin-top: 22px;
  border-radius: 18px;
}

.info-grid {
  margin-top: 8px;
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 14px;
}

.info-card {
  padding: 18px;
  border-radius: 18px;
  background: rgba(109, 90, 75, 0.06);
}

.info-card span {
  display: block;
  font-size: 12px;
  color: var(--text-muted);
}

.info-card strong {
  display: block;
  margin-top: 10px;
}

.actions {
  margin-top: 24px;
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
}

.primary-btn {
  --el-button-bg-color: var(--brand-primary);
  --el-button-border-color: var(--brand-primary);
  --el-button-hover-bg-color: #7b6857;
  --el-button-hover-border-color: #7b6857;
}

@media (max-width: 700px) {
  .info-grid {
    grid-template-columns: 1fr;
  }

  .actions {
    flex-direction: column;
  }
}
</style>
