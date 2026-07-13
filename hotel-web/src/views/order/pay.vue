<template>
  <div class="pay-page">
    <el-card class="container">
      <el-result icon="info" title="订单已创建" sub-title="支付功能集成待完成">
        <template #extra>
          <el-button type="primary" @click="goOrders">查看我的订单</el-button>
          <el-button @click="$router.push('/')">返回首页</el-button>
        </template>
      </el-result>
      <p class="tip">TODO: 集成支付宝沙箱支付，此处将显示支付页面或跳转到支付宝</p>
    </el-card>
  </div>
</template>

<script setup>
import { useRouter } from 'vue-router'

const router = useRouter()

function goOrders() {
  router.push('/user/orders')
}
</script>

<style scoped>
.pay-page { max-width: 500px; margin: 60px auto; padding: 0 20px; }
.tip { text-align: center; color: #909399; font-size: 13px; margin-top: 16px; }
</style>
