<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { CreditCard, RefreshRight, Star, Tickets } from '@element-plus/icons-vue'
import { getMyOrders } from '@/api/user'
import { cancelOrder as cancelOrderApi, confirmCancelOrder, preCancelOrder } from '@/api/order'
import { createReview } from '@/api/review'
import StatusBadge from '@/components/StatusBadge.vue'
import { getOrderStatusMeta } from '@/utils/order'

const activeTab = ref('')
const orders = ref([])
const loading = ref(false)
const loadFailed = ref(false)
const reviewDialogVisible = ref(false)
const reviewSubmitting = ref(false)
const currentReviewOrder = ref(null)
const reviewFormRef = ref()
const reviewForm = reactive({
  score: 5,
  content: '',
  anonymous: false
})

const reviewRules = {
  score: [{ required: true, message: '请选择评分', trigger: 'change' }]
}

const filteredOrders = computed(() => {
  if (!activeTab.value) return orders.value
  return orders.value.filter((item) => String(item.status) === activeTab.value)
})

const orderSummary = computed(() => [
  { key: 'all', label: '全部订单', value: orders.value.length, icon: Tickets },
  { key: 'pending', label: '待支付', value: orders.value.filter((item) => item.status === 0).length, icon: CreditCard },
  { key: 'reviewable', label: '可评价', value: orders.value.filter((item) => item.status === 5 || item.status === 6).length, icon: Star }
])

function formatOrderHotelName(order) {
  const hotelName = String(order?.hotelName || '').trim()
  if (!hotelName || /^[?？]+$/.test(hotelName) || hotelName.includes('�')) {
    return `酒店订单 #${order?.hotelId || order?.id || ''}`
  }
  return hotelName
}

onMounted(() => fetchOrders())

async function fetchOrders() {
  loading.value = true
  try {
    const data = await getMyOrders(1, 50, null, { silent: true })
    orders.value = data.records || []
    loadFailed.value = false
  } catch (error) {
    orders.value = []
    loadFailed.value = true
  } finally {
    loading.value = false
  }
}

async function cancelOrder(id) {
  try {
    await ElMessageBox.confirm('取消后将失去当前房间的预订资格，确定继续吗？', '确认取消订单', {
      type: 'warning',
      confirmButtonText: '确认取消',
      cancelButtonText: '再想想'
    })
    await cancelOrderApi(id)
    ElMessage.success('订单已取消')
    fetchOrders()
  } catch (error) {
    if (error === 'cancel') return
    ElMessage.error(error.message || '取消失败')
  }
}

async function handleCheckout(order) {
  try {
    await ElMessageBox.confirm('确认要发起退房申请吗？提交后会进入后台处理流程。', '确认申请退房', {
      type: 'warning',
      confirmButtonText: '提交申请',
      cancelButtonText: '暂不申请'
    })
    const data = await preCancelOrder(order.id)
    await confirmCancelOrder(order.id, data.cancelConfirmId)
    ElMessage.success('已提交退房申请')
    fetchOrders()
  } catch (error) {
    if (error === 'cancel') return
    ElMessage.error(error.message || '退房申请失败，请稍后重试')
  }
}

function openReviewDialog(order) {
  currentReviewOrder.value = order
  reviewForm.score = 5
  reviewForm.content = ''
  reviewForm.anonymous = false
  reviewDialogVisible.value = true
}

async function submitReview() {
  reviewForm.content = reviewForm.content.trim()
  const valid = await reviewFormRef.value?.validate().catch(() => false)
  if (!valid || !currentReviewOrder.value) return

  reviewSubmitting.value = true
  try {
    await createReview({
      hotelId: currentReviewOrder.value.hotelId,
      orderId: currentReviewOrder.value.id,
      score: reviewForm.score,
      content: reviewForm.content || null,
      anonymous: reviewForm.anonymous
    })
    ElMessage.success('评价已提交')
    reviewDialogVisible.value = false
  } catch (error) {
    ElMessage.error(error.message || '评价提交失败')
  } finally {
    reviewSubmitting.value = false
  }
}
</script>

<template>
  <div class="orders-page page-container narrow">
    <section class="orders-header soft-card">
      <div>
        <div class="gold-chip">My Orders</div>
        <h1>订单管理</h1>
        <p>统一查看待支付、已支付与已完成订单，方便随时跟进行程安排。</p>
      </div>
      <div class="summary-grid">
        <div v-for="item in orderSummary" :key="item.key" class="summary-card">
          <el-icon><component :is="item.icon" /></el-icon>
          <div>
            <span>{{ item.label }}</span>
            <strong>{{ item.value }}</strong>
          </div>
        </div>
      </div>
      <el-tabs v-model="activeTab" class="status-tabs" @tab-change="fetchOrders">
        <el-tab-pane label="全部" name="" />
        <el-tab-pane label="待支付" name="0" />
        <el-tab-pane label="已支付" name="1" />
        <el-tab-pane label="已取消" name="2" />
        <el-tab-pane label="已退房/完成" name="5" />
        <el-tab-pane label="已完成" name="6" />
      </el-tabs>
    </section>

    <section class="order-list" v-loading="loading">
      <el-alert
        v-if="loadFailed"
        class="page-alert"
        type="warning"
        :closable="false"
        title="订单列表暂时加载失败，请稍后重试"
      />

      <article v-for="order in filteredOrders" :key="order.id" class="order-card soft-card">
        <div class="order-top">
          <div>
            <span class="order-no">订单号：{{ order.orderNo }}</span>
            <h3>{{ formatOrderHotelName(order) }}</h3>
            <p class="order-copy">{{ getOrderStatusMeta(order.status).description }}</p>
          </div>
          <StatusBadge :status="order.status" :text="order.statusText" />
        </div>

        <div class="order-meta">
          <div class="meta-block">
            <span>入住日期</span>
            <strong>{{ order.checkInDate }}</strong>
          </div>
          <div class="meta-block">
            <span>退房日期</span>
            <strong>{{ order.checkOutDate }}</strong>
          </div>
          <div class="meta-block">
            <span>订单金额</span>
            <strong class="price">¥{{ order.totalAmount }}</strong>
          </div>
          <div class="meta-block">
            <span>状态说明</span>
            <strong>{{ getOrderStatusMeta(order.status).description }}</strong>
          </div>
        </div>

        <div class="order-actions">
          <el-button v-if="order.status === 0" type="primary" class="primary-btn" :icon="CreditCard" @click="$router.push(`/order/pay/${order.id}`)">
            去支付
          </el-button>
          <el-button v-if="order.status === 0" @click="cancelOrder(order.id)">取消订单</el-button>
          <el-button v-if="order.status === 1" @click="handleCheckout(order)">申请退房</el-button>
          <el-button v-if="order.status === 5 || order.status === 6" type="primary" class="primary-btn" :icon="Star" @click="openReviewDialog(order)">写评价</el-button>
        </div>
      </article>

      <el-empty v-if="!loading && filteredOrders.length === 0" :description="loadFailed ? '订单服务暂时不可用' : '暂无订单数据'">
        <el-button type="primary" class="primary-btn" :icon="RefreshRight" @click="fetchOrders">重新加载</el-button>
        <el-button @click="$router.push('/search')">去搜索酒店</el-button>
      </el-empty>
    </section>

    <el-dialog v-model="reviewDialogVisible" title="提交住客评价" width="520px">
      <el-form ref="reviewFormRef" :model="reviewForm" :rules="reviewRules" label-position="top">
        <el-form-item label="评分" prop="score">
          <el-rate v-model="reviewForm.score" :max="5" />
        </el-form-item>
        <el-form-item label="评价内容" prop="content">
          <el-input
            v-model="reviewForm.content"
            type="textarea"
            :rows="5"
            maxlength="300"
            show-word-limit
            placeholder="可以写入住体验，也可以留空仅打分。"
          />
        </el-form-item>
        <el-form-item>
          <el-checkbox v-model="reviewForm.anonymous">匿名评价</el-checkbox>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="reviewDialogVisible = false">取消</el-button>
        <el-button type="primary" class="primary-btn" :loading="reviewSubmitting" @click="submitReview">提交评价</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.orders-page {
  padding: 24px 0 0;
}

.orders-header,
.order-card {
  background: rgba(255, 252, 247, 0.92);
}

.orders-header {
  padding: 28px;
}

.orders-header h1 {
  margin: 18px 0 8px;
  font-size: clamp(28px, 4vw, 40px);
}

.orders-header p {
  margin: 0;
  color: var(--text-secondary);
  line-height: 1.8;
}

.status-tabs {
  margin-top: 20px;
}

.summary-grid {
  margin-top: 22px;
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 14px;
}

.summary-card {
  position: relative;
  display: flex;
  align-items: flex-start;
  gap: 12px;
  padding: 16px 18px;
  border-radius: 20px;
  border: 1px solid rgba(109, 90, 75, 0.08);
  background: rgba(255, 255, 255, 0.72);
  overflow: hidden;
}

.summary-card::before {
  content: '';
  position: absolute;
  left: 0;
  right: 0;
  top: 0;
  height: 3px;
  background: linear-gradient(90deg, #6d5a4b, #b89567);
}

.summary-card .el-icon {
  margin-top: 2px;
  font-size: 18px;
  color: var(--brand-secondary-strong);
}

.summary-card span,
.summary-card strong {
  display: block;
}

.summary-card span {
  font-size: 12px;
  color: var(--text-muted);
}

.summary-card strong {
  margin-top: 8px;
  font-size: 22px;
  color: var(--brand-primary);
}

.order-list {
  margin-top: 20px;
}

.page-alert {
  margin-bottom: 16px;
}

.order-card {
  padding: 24px;
  margin-bottom: 16px;
}

.order-top,
.order-actions {
  display: flex;
  justify-content: space-between;
  gap: 16px;
}

.order-top h3 {
  margin: 10px 0 0;
  font-size: 22px;
}

.order-copy {
  margin: 10px 0 0;
  color: var(--text-secondary);
  line-height: 1.7;
}

.order-no {
  color: var(--text-muted);
  font-size: 13px;
}

.order-meta {
  margin-top: 22px;
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 16px;
}

.meta-block {
  padding: 16px;
  border-radius: 18px;
  background: rgba(109, 90, 75, 0.06);
}

.meta-block span {
  display: block;
  font-size: 12px;
  color: var(--text-muted);
}

.meta-block strong {
  display: block;
  margin-top: 10px;
  line-height: 1.7;
}

.price {
  color: var(--brand-danger);
}

.order-actions {
  margin-top: 22px;
  justify-content: flex-end;
}

.primary-btn {
  --el-button-bg-color: var(--brand-primary);
  --el-button-border-color: var(--brand-primary);
  --el-button-hover-bg-color: #7b6857;
  --el-button-hover-border-color: #7b6857;
}

@media (max-width: 900px) {
  .order-top,
  .order-actions {
    flex-direction: column;
    align-items: flex-start;
  }

  .summary-grid,
  .order-meta {
    grid-template-columns: 1fr;
  }
}
</style>
