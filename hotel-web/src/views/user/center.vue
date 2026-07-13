<template>
  <div class="center-page">
    <el-card class="container">
      <h2>个人中心</h2>
      <el-descriptions :column="2" border style="margin-top:20px">
        <el-descriptions-item label="昵称">{{ userInfo?.nickname }}</el-descriptions-item>
        <el-descriptions-item label="手机号">{{ userInfo?.phone }}</el-descriptions-item>
        <el-descriptions-item label="邮箱">{{ userInfo?.email || '未设置' }}</el-descriptions-item>
        <el-descriptions-item label="注册时间">{{ userInfo?.createTime }}</el-descriptions-item>
      </el-descriptions>
      <div style="margin-top:20px">
        <el-button type="primary" @click="$router.push('/user/orders')">我的订单</el-button>
        <el-button @click="handleLogout">退出登录</el-button>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/store/user'
import { getUserInfo } from '@/api/user'

const router = useRouter()
const userStore = useUserStore()
const userInfo = ref(null)

onMounted(async () => {
  try {
    await userStore.fetchUserInfo()
    userInfo.value = userStore.userInfo
  } catch (e) {
    userInfo.value = { nickname: '--', phone: '--' }
  }
})

function handleLogout() {
  userStore.logout()
  router.push('/login')
}
</script>

<style scoped>
.center-page { max-width: 600px; margin: 40px auto; padding: 0 20px; }
</style>
