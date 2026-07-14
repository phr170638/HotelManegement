<script setup>
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'

const route = useRoute()
const router = useRouter()
const orderId = computed(() => String(route.params.id || ''))
const isValidOrderId = computed(() => /^\d+$/.test(orderId.value))

function goOrders() {
  router.push('/user/orders')
}
</script>

<template>
  <div class="pay-page page-container narrow">
    <section v-if="isValidOrderId" class="pay-card soft-card">
      <div class="gold-chip">Payment Status</div>
      <h1>订单已提交</h1>
      <p class="sub-copy">
        当前在线支付能力正在接入中，你可以先查看订单状态或返回继续浏览酒店，后续将支持完整支付流程。
      </p>

      <div class="state-panel">
        <div class="state-item">
          <span>订单编号</span>
          <strong>{{ orderId }}</strong>
        </div>
        <div class="state-item">
          <span>当前状态</span>
          <strong>待支付能力接入</strong>
        </div>
        <div class="state-item">
          <span>你接下来可以做什么</span>
          <strong>查看订单状态、继续浏览酒店或稍后回到订单中心</strong>
        </div>
      </div>

      <div class="actions">
        <el-button type="primary" class="primary-btn" @click="goOrders">查看我的订单</el-button>
        <el-button @click="$router.push('/search')">继续浏览酒店</el-button>
        <el-button @click="$router.push('/')">返回首页</el-button>
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

.state-panel {
  margin-top: 24px;
  display: grid;
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

.actions {
  margin-top: 24px;
  display: flex;
  gap: 12px;
}

.primary-btn {
  --el-button-bg-color: var(--brand-primary);
  --el-button-border-color: var(--brand-primary);
  --el-button-hover-bg-color: #7b6857;
  --el-button-hover-border-color: #7b6857;
}

@media (max-width: 700px) {
  .actions {
    flex-direction: column;
  }
}
</style>
