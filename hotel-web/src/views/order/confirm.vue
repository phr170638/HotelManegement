<template>
  <div class="order-page">
    <el-card class="container">
      <h2>订单确认</h2>
      <el-form :model="form" label-width="100px" style="margin-top:20px">
        <el-form-item label="入住人">
          <el-input v-model="form.guestName" placeholder="请输入入住人姓名" />
        </el-form-item>
        <el-form-item label="手机号">
          <el-input v-model="form.guestPhone" placeholder="请输入入住人手机号" />
        </el-form-item>
        <el-form-item label="入住日期">
          <el-date-picker v-model="form.checkInDate" type="date" value-format="YYYY-MM-DD" />
        </el-form-item>
        <el-form-item label="退房日期">
          <el-date-picker v-model="form.checkOutDate" type="date" value-format="YYYY-MM-DD" />
        </el-form-item>
        <el-form-item label="房型">
          <span>{{ roomName }}</span>
        </el-form-item>
        <el-form-item label="单价">
          <span class="price">¥{{ price }}</span>
        </el-form-item>
        <el-form-item label="数量">
          <el-input-number v-model="form.quantity" :min="1" :max="5" />
        </el-form-item>
        <el-form-item label="总价">
          <span class="price total">¥{{ (price * form.quantity).toFixed(2) }}</span>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" size="large" @click="submitOrder" :loading="submitting">提交订单</el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { createOrder } from '@/api/order'
import { ElMessage } from 'element-plus'

const route = useRoute()
const router = useRouter()
const submitting = ref(false)

const hotelId = Number(route.query.hotelId)
const roomId = Number(route.query.roomId)
const price = Number(route.query.price) || 0
const roomName = route.query.roomName || ''

const form = reactive({
  guestName: '',
  guestPhone: '',
  checkInDate: '',
  checkOutDate: '',
  quantity: 1
})

async function submitOrder() {
  if (!form.guestName || !form.guestPhone || !form.checkInDate || !form.checkOutDate) {
    ElMessage.warning('请填写完整信息')
    return
  }
  submitting.value = true
  try {
    const data = await createOrder({
      hotelId,
      checkInDate: form.checkInDate,
      checkOutDate: form.checkOutDate,
      roomCount: 1,
      guestName: form.guestName,
      guestPhone: form.guestPhone,
      items: [{ roomId, quantity: form.quantity }]
    })
    ElMessage.success('订单创建成功')
    router.push(`/order/pay/${data.id}`)
  } catch (e) {
    ElMessage.error(e.message || '创建订单失败')
  } finally {
    submitting.value = false
  }
}
</script>

<style scoped>
.order-page { max-width: 600px; margin: 40px auto; padding: 0 20px; }
.price { color: #F56C6C; font-weight: bold; }
.total { font-size: 24px; }
</style>
