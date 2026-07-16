<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { House, OfficeBuilding, Tickets } from '@element-plus/icons-vue'
import { mockPaySuccess } from '@/api/order'

const route = useRoute()
const router = useRouter()

const orderId = computed(() => String(route.query.orderId || route.query.id || ''))
const hasOrderId = computed(() => /^\d+$/.test(orderId.value))
const syncing = ref(false)

const pageTitle = computed(() => '支付成功')
const pageDescription = computed(() => '你的订单支付已完成，可以继续查看订单状态或返回首页。')
const statusTitle = computed(() => '支付成功')
const statusDescription = computed(() => '订单已完成支付，后续可在订单中心继续查看入住安排。')

onMounted(async () => {
  if (hasOrderId.value) {
    syncing.value = true
    try {
      await mockPaySuccess(orderId.value)
    } catch (error) {
      ElMessage.error(error.message || '订单支付状态同步失败')
    } finally {
      syncing.value = false
    }
  }
})

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
        type="success"
        show-icon
        :closable="false"
        title="订单支付状态已进入同步流程，完成后可在订单中心继续查看后续入住安排。"
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
          <strong>已支付</strong>
        </div>
      </div>

      <div class="actions">
        <el-button type="primary" class="primary-btn" :icon="Tickets" @click="goOrders">查看我的订单</el-button>
        <el-button :icon="OfficeBuilding" @click="goCenter">前往个人中心</el-button>
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
