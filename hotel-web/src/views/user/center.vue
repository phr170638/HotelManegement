<script setup>
import { computed, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/store/user'
import { getMyOrders } from '@/api/user'

const router = useRouter()
const userStore = useUserStore()

const userInfo = ref(null)
const orderCount = ref(0)
const pendingCount = ref(0)
const completedCount = ref(0)
const loading = ref(false)
const loadFailed = ref(false)

const quickStats = computed(() => [
  { label: '累计订单', value: String(orderCount.value).padStart(2, '0') },
  { label: '待支付', value: String(pendingCount.value).padStart(2, '0') },
  { label: '已完成', value: String(completedCount.value).padStart(2, '0') }
])

async function fetchProfileData() {
  loading.value = true
  try {
    const [_, orderData] = await Promise.all([
      userStore.fetchUserInfo(),
      getMyOrders(1, 100, null, { silent: true })
    ])
    userInfo.value = userStore.userInfo
    const records = orderData.records || []
    orderCount.value = orderData.total || records.length
    pendingCount.value = records.filter((item) => item.status === 0).length
    completedCount.value = records.filter((item) => item.status === 6).length
    loadFailed.value = false
  } catch (error) {
    userInfo.value = userStore.userInfo
    orderCount.value = 0
    pendingCount.value = 0
    completedCount.value = 0
    loadFailed.value = true
  } finally {
    loading.value = false
  }
}

function handleLogout() {
  userStore.logout()
  ElMessage.success('已退出登录')
  router.push('/')
}

onMounted(fetchProfileData)
</script>

<template>
  <div class="center-page page-container narrow">
    <section class="profile-panel soft-card" v-loading="loading">
      <div class="profile-header">
        <div class="avatar">{{ (userInfo?.nickname || 'U').slice(0, 1) }}</div>
        <div>
          <div class="gold-chip">My Profile</div>
          <h1>{{ userInfo?.nickname || '我的账户' }}</h1>
          <p>这里可以查看账户信息、订单概览，并快速前往常用操作。</p>
        </div>
      </div>

      <el-alert
        v-if="loadFailed"
        class="page-alert"
        type="warning"
        :closable="false"
        title="个人信息或订单统计暂时加载失败，你仍可以前往订单页继续查看"
      />

      <div class="stats-grid">
        <div v-for="stat in quickStats" :key="stat.label" class="stat-card">
          <strong>{{ stat.value }}</strong>
          <span>{{ stat.label }}</span>
        </div>
      </div>

      <div class="info-grid">
        <div class="info-card">
          <span>手机号</span>
          <strong>{{ userInfo?.phone || '暂未获取' }}</strong>
        </div>
        <div class="info-card">
          <span>邮箱</span>
          <strong>{{ userInfo?.email || '暂未获取' }}</strong>
        </div>
        <div class="info-card">
          <span>角色</span>
          <strong>{{ userInfo?.roles?.includes('admin') ? '管理员' : '普通用户' }}</strong>
        </div>
        <div class="info-card">
          <span>注册时间</span>
          <strong>{{ userInfo?.createTime || '暂未获取' }}</strong>
        </div>
      </div>

      <div class="profile-actions">
        <el-button type="primary" class="primary-btn" @click="$router.push('/user/orders')">
          查看我的订单（{{ orderCount }}）
        </el-button>
        <el-button @click="fetchProfileData">重新加载</el-button>
        <el-button @click="$router.push('/')">返回首页</el-button>
        <el-button @click="handleLogout">退出登录</el-button>
      </div>
    </section>
  </div>
</template>

<style scoped>
.center-page {
  padding: 24px 0 0;
}

.profile-panel {
  padding: 28px;
  background: rgba(255, 252, 247, 0.92);
}

.profile-header {
  display: flex;
  gap: 20px;
  align-items: center;
}

.avatar {
  width: 92px;
  height: 92px;
  border-radius: 28px;
  display: grid;
  place-items: center;
  background: linear-gradient(145deg, #6d5a4b, #b89567);
  color: #fff;
  font-size: 32px;
  font-weight: 700;
}

.profile-header h1 {
  margin: 16px 0 8px;
  font-size: clamp(28px, 4vw, 40px);
}

.profile-header p {
  margin: 0;
  color: var(--text-secondary);
}

.page-alert {
  margin-top: 20px;
}

.stats-grid,
.info-grid {
  margin-top: 24px;
  display: grid;
  gap: 16px;
}

.stats-grid {
  grid-template-columns: repeat(3, minmax(0, 1fr));
}

.stat-card,
.info-card {
  padding: 20px;
  border-radius: 20px;
}

.stat-card {
  background: rgba(109, 90, 75, 0.06);
}

.stat-card strong,
.info-card strong {
  display: block;
}

.stat-card strong {
  font-size: 30px;
  color: var(--brand-primary);
}

.stat-card span,
.info-card span {
  display: block;
  margin-top: 8px;
  font-size: 12px;
  color: var(--text-muted);
}

.info-grid {
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.info-card {
  background: rgba(255, 255, 255, 0.62);
  border: 1px solid rgba(109, 90, 75, 0.08);
}

.info-card strong {
  margin-top: 10px;
  line-height: 1.6;
}

.profile-actions {
  margin-top: 26px;
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

@media (max-width: 720px) {
  .profile-header {
    flex-direction: column;
    align-items: flex-start;
  }

  .stats-grid,
  .info-grid {
    grid-template-columns: 1fr;
  }

  .profile-actions {
    flex-direction: column;
  }
}
</style>
