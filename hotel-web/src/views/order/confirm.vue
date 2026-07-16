<script setup>
import { computed, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { createOrder } from '@/api/order'
import { validatePhone } from '@/utils/validate'

const route = useRoute()
const router = useRouter()
const submitting = ref(false)
const submitError = ref('')

const hotelId = Number(route.query.hotelId)
const roomId = Number(route.query.roomId)
const price = Number(route.query.price) || 698
const roomName = String(route.query.roomName || '高级大床房')
const hotelName = String(route.query.hotelName || '精选酒店')
const hasValidParams = Number.isFinite(hotelId) && hotelId > 0 && Number.isFinite(roomId) && roomId > 0

const form = reactive({
  guestName: '',
  guestPhone: '',
  checkInDate: '',
  checkOutDate: '',
  quantity: 1
})

const totalPrice = computed(() => {
  const nights = stayNights.value > 0 ? stayNights.value : 0
  return (price * form.quantity * nights).toFixed(2)
})
const stayNights = computed(() => {
  if (!form.checkInDate || !form.checkOutDate) return 0
  const start = new Date(form.checkInDate)
  const end = new Date(form.checkOutDate)
  if (Number.isNaN(start.getTime()) || Number.isNaN(end.getTime())) return 0
  return Math.floor((end - start) / (24 * 60 * 60 * 1000))
})

function disableCheckInDate(date) {
  const today = new Date()
  today.setHours(0, 0, 0, 0)
  return date.getTime() < today.getTime()
}

function disableCheckOutDate(date) {
  if (!form.checkInDate) {
    return disableCheckInDate(date)
  }

  const checkIn = new Date(form.checkInDate)
  checkIn.setHours(0, 0, 0, 0)
  return date.getTime() <= checkIn.getTime()
}

function validateGuestPhone() {
  return new Promise((resolve, reject) => {
    validatePhone(null, form.guestPhone, (error) => {
      if (error) {
        reject(error)
      } else {
        resolve()
      }
    })
  })
}

async function submitOrder() {
  submitError.value = ''

  if (!hasValidParams) {
    ElMessage.error('当前订单信息不完整，请返回酒店详情页重新选择房型')
    return
  }

  if (!form.guestName || !form.guestPhone || !form.checkInDate || !form.checkOutDate) {
    ElMessage.warning('请填写完整信息')
    return
  }

  if (form.quantity < 1) {
    ElMessage.warning('房间数量至少为 1')
    return
  }

  if (stayNights.value <= 0) {
    ElMessage.warning('离店日期必须晚于入住日期')
    return
  }

  try {
    await validateGuestPhone()
  } catch (error) {
    ElMessage.warning(error.message || '请输入正确的手机号')
    return
  }

  submitting.value = true
  try {
    const data = await createOrder({
      hotelId,
      checkInDate: form.checkInDate,
      checkOutDate: form.checkOutDate,
      roomCount: form.quantity,
      guestName: form.guestName,
      guestPhone: form.guestPhone,
      items: [{ roomId, quantity: form.quantity }]
    })
    ElMessage.success('订单创建成功')
    router.push(`/order/pay/${data.id}`)
  } catch (error) {
    submitError.value = error.message || '订单创建失败，请稍后重试'
    ElMessage.error(submitError.value)
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <div class="order-page page-container narrow">
    <div class="page-actions">
      <el-button @click="$router.back()">返回上一页</el-button>
      <el-button @click="hasValidParams ? $router.push(`/hotel/${hotelId}`) : $router.push('/search')">
        {{ hasValidParams ? '返回酒店详情' : '返回搜索' }}
      </el-button>
    </div>

    <div class="order-layout">
      <section class="summary-card soft-card">
        <div class="gold-chip">Order Summary</div>
        <h1>确认订单</h1>
        <p>填写入住信息后即可创建订单，并进入支付确认页面继续后续流程。</p>

        <el-alert
          v-if="!hasValidParams"
          class="page-alert"
          type="error"
          :closable="false"
          title="当前房型信息不完整，请返回详情页重新选择"
        />

        <el-alert
          v-if="submitError"
          class="page-alert"
          type="error"
          :closable="false"
          :title="submitError"
        />

        <div class="summary-list">
          <div class="summary-item">
            <span>酒店名称</span>
            <strong>{{ hotelName }}</strong>
          </div>
          <div class="summary-item">
            <span>房型</span>
            <strong>{{ roomName }}</strong>
          </div>
          <div class="summary-item">
            <span>单价</span>
            <strong>¥{{ price }}</strong>
          </div>
          <div class="summary-item">
            <span>总价</span>
            <strong class="price">{{ stayNights > 0 ? `¥${totalPrice}` : '待选择日期后计算' }}</strong>
          </div>
          <div class="summary-item">
            <span>入住晚数</span>
            <strong>{{ stayNights > 0 ? `${stayNights} 晚` : '待选择日期' }}</strong>
          </div>
        </div>
      </section>

      <section class="form-card soft-card">
        <el-form :model="form" label-position="top" @submit.prevent="submitOrder">
          <el-form-item label="入住人姓名">
            <el-input v-model="form.guestName" placeholder="请输入入住人姓名" />
          </el-form-item>
          <el-form-item label="手机号">
            <el-input v-model="form.guestPhone" placeholder="请输入入住手机号" />
          </el-form-item>
          <div class="date-grid">
            <el-form-item label="入住日期">
              <el-date-picker
                v-model="form.checkInDate"
                type="date"
                value-format="YYYY-MM-DD"
                placeholder="选择入住日期"
                :disabled-date="disableCheckInDate"
              />
            </el-form-item>
            <el-form-item label="退房日期">
              <el-date-picker
                v-model="form.checkOutDate"
                type="date"
                value-format="YYYY-MM-DD"
                placeholder="选择离店日期"
                :disabled-date="disableCheckOutDate"
              />
            </el-form-item>
          </div>
          <el-form-item label="房间数量">
            <el-input-number v-model="form.quantity" :min="1" :max="5" />
          </el-form-item>
          <div class="form-actions">
            <el-button @click="hasValidParams ? $router.push(`/hotel/${hotelId}`) : $router.push('/search')">取消并返回</el-button>
            <el-button type="primary" native-type="submit" class="submit-btn" :loading="submitting" :disabled="!hasValidParams">
              提交订单
            </el-button>
          </div>
        </el-form>
      </section>
    </div>
  </div>
</template>

<style scoped>
.order-page {
  padding: 24px 0 0;
}

.page-actions {
  display: flex;
  gap: 12px;
  margin-bottom: 18px;
}

.order-layout {
  display: grid;
  grid-template-columns: 0.92fr 1.08fr;
  gap: 20px;
}

.summary-card,
.form-card {
  padding: 28px;
  background: rgba(255, 252, 247, 0.92);
}

.summary-card h1 {
  margin: 18px 0 8px;
  font-size: clamp(28px, 4vw, 40px);
}

.summary-card p {
  margin: 0;
  color: var(--text-secondary);
  line-height: 1.8;
}

.summary-list {
  margin-top: 24px;
  display: grid;
  gap: 12px;
}

.page-alert {
  margin-top: 20px;
}

.summary-item {
  padding: 18px;
  border-radius: 18px;
  background: rgba(109, 90, 75, 0.06);
}

.summary-item span {
  display: block;
  font-size: 12px;
  color: var(--text-muted);
}

.summary-item strong {
  display: block;
  margin-top: 10px;
  font-size: 18px;
}

.price {
  color: var(--brand-danger);
}

.date-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.date-grid :deep(.el-date-editor),
.date-grid :deep(.el-input-number) {
  width: 100%;
}

.submit-btn {
  --el-button-bg-color: var(--brand-primary);
  --el-button-border-color: var(--brand-primary);
  --el-button-hover-bg-color: #7b6857;
  --el-button-hover-border-color: #7b6857;
}

.form-actions {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  flex-wrap: wrap;
}

@media (max-width: 860px) {
  .order-layout,
  .date-grid {
    grid-template-columns: 1fr;
  }

  .page-actions,
  .form-actions {
    flex-direction: column;
  }
}
</style>
