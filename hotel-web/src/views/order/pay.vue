<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { CreditCard, House, RefreshRight, Tickets } from '@element-plus/icons-vue'
import { payOrder } from '@/api/order'

const route = useRoute()
const router = useRouter()
const orderId = computed(() => String(route.params.id || ''))
const isValidOrderId = computed(() => /^\d+$/.test(orderId.value))
const loading = ref(false)
const payFailed = ref(false)
const payMessage = ref('')

onMounted(async () => {
  if (!isValidOrderId.value) return
  await submitPayment()
})

async function submitPayment() {
  loading.value = true
  payFailed.value = false
  payMessage.value = ''

  try {
    const data = await payOrder(orderId.value, { silent: true })
    const payForm = data?.payForm
    if (!payForm) {
      throw new Error('支付表单生成失败')
    }

    const container = document.createElement('div')
    container.innerHTML = payForm
    document.body.appendChild(container)

    const form = container.querySelector('form')
    if (!form) {
      throw new Error('支付表单结构无效')
    }
    form.submit()
  } catch (error) {
    const message = error.message || error.response?.data?.message || '当前无法发起支付，请稍后再试'
    if (message.includes('支付宝配置未完成')) {
      await router.replace({
        path: '/order/result',
        query: { orderId: orderId.value }
      })
      return
    }
    payFailed.value = true
    payMessage.value = message
    ElMessage.error(payMessage.value)
  } finally {
    loading.value = false
  }
}

function goOrders() {
  router.push('/user/orders')
}
</script>

<template>
  <div class="pay-page page-container narrow">
    <section v-if="isValidOrderId" class="pay-card soft-card" v-loading="loading">
      <div class="gold-chip">Payment Status</div>
      <h1>{{ payFailed ? '支付暂时不可用' : '正在跳转支付' }}</h1>
      <p class="sub-copy">
        {{ payFailed ? payMessage : '正在为你拉起支付宝支付，请稍候。如果浏览器拦截了跳转，可以重新尝试。' }}
      </p>

      <div class="journey-strip">
        <div class="journey-item" :class="{ active: !payFailed }">
          <span>1</span>
          <strong>拉起支付</strong>
        </div>
        <div class="journey-item" :class="{ active: !payFailed }">
          <span>2</span>
          <strong>完成回跳</strong>
        </div>
        <div class="journey-item" :class="{ active: !payFailed }">
          <span>3</span>
          <strong>同步订单状态</strong>
        </div>
      </div>

      <div class="state-panel">
        <div class="state-item">
          <span>订单编号</span>
          <strong>{{ orderId }}</strong>
        </div>
        <div class="state-item">
          <span>当前状态</span>
          <strong>{{ payFailed ? '支付发起失败' : '正在发起支付宝支付' }}</strong>
        </div>
        <div class="state-item">
          <span>你接下来可以做什么</span>
          <strong>{{ payFailed ? '检查支付宝沙箱配置后重试，或先回到订单中心查看状态' : '等待支付宝页面打开，完成支付后回到订单中心查看状态' }}</strong>
        </div>
      </div>

      <div class="support-copy">
        系统会在支付成功后自动同步订单状态，后续可继续查看入住安排、申请退房和评价记录。
      </div>

      <div class="actions">
        <el-button v-if="payFailed" type="primary" class="primary-btn" :icon="RefreshRight" @click="submitPayment">重新发起支付</el-button>
        <el-button type="primary" class="primary-btn" :icon="Tickets" @click="goOrders">查看我的订单</el-button>
        <el-button :icon="CreditCard" @click="$router.push('/search')">继续浏览酒店</el-button>
        <el-button :icon="House" @click="$router.push('/')">返回首页</el-button>
      </div>
    </section>

    <section v-else class="pay-card soft-card">
      <el-result
        icon="warning"
        title="没有找到有效订单"
        sub-title="当前页面缺少可识别的订单编号。你可以回到订单中心或重新选择酒店。"
      >
        <template #extra>
          <el-button type="primary" class="primary-btn" @click="goOrders">查看我的订单</el-button>
          <el-button @click="$router.push('/search')">返回搜索</el-button>
        </template>
      </el-result>
    </section>
  </div>
</template>

<style scoped>
.pay-page {
  padding: 32px 0 0;
}

.pay-card {
  padding: 32px;
  background: rgba(255, 252, 247, 0.92);
}

.pay-card h1 {
  margin: 18px 0 8px;
  font-size: clamp(30px, 4vw, 44px);
}

.sub-copy {
  margin: 0;
  color: var(--text-secondary);
  line-height: 1.8;
}

.journey-strip {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
  margin-top: 22px;
}

.journey-item {
  padding: 14px 16px;
  border-radius: 18px;
  border: 1px solid rgba(109, 90, 75, 0.08);
  background: rgba(255, 255, 255, 0.52);
}

.journey-item span {
  display: inline-grid;
  place-items: center;
  width: 24px;
  height: 24px;
  border-radius: 999px;
  background: rgba(109, 90, 75, 0.08);
  color: var(--text-secondary);
  font-size: 12px;
  font-weight: 700;
}

.journey-item strong {
  display: block;
  margin-top: 12px;
}

.journey-item.active {
  background: rgba(184, 149, 103, 0.14);
  border-color: rgba(184, 149, 103, 0.26);
}

.state-panel {
  margin-top: 24px;
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 14px;
}

.state-item {
  padding: 18px;
  border-radius: 18px;
  background: rgba(109, 90, 75, 0.06);
}

.state-item span {
  display: block;
  font-size: 12px;
  color: var(--text-muted);
}

.state-item strong {
  display: block;
  margin-top: 10px;
}

.support-copy {
  margin-top: 18px;
  color: var(--text-secondary);
  line-height: 1.8;
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
  .journey-strip,
  .state-panel {
    grid-template-columns: 1fr;
  }

  .actions {
    flex-direction: column;
  }
}
</style>
