<script setup>
import { computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Document, OfficeBuilding, User } from '@element-plus/icons-vue'
import { adminVisualImage } from '@/utils/hotel'
import { useUserStore } from '@/store/user'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const titleMap = {
  '/admin/hotels': '酒店管理',
  '/admin/orders': '订单管理',
  '/admin/users': '用户管理'
}

const currentTitle = computed(() => titleMap[route.path] || '酒店后台')
const currentUserName = computed(() => userStore.userInfo?.nickname || '管理员')
const currentUserRole = computed(() => (userStore.roles.includes('admin') ? '系统管理员' : '运营账号'))

onMounted(async () => {
  if (!userStore.userInfo?.nickname) {
    await userStore.fetchUserInfo().catch(() => {})
  }
})

function goHome() {
  router.push('/')
}

function handleLogout() {
  userStore.logout()
  router.replace('/login')
}
</script>

<template>
  <el-container class="admin-layout">
    <el-aside width="260px" class="sidebar">
      <div class="brand-block">
        <div class="brand-mark">HM</div>
        <div>
          <strong>酒店管理后台</strong>
          <span>Hotel Operations Center</span>
        </div>
      </div>

      <el-menu :default-active="route.path" router class="admin-menu">
        <el-menu-item index="/admin/hotels">
          <el-icon><OfficeBuilding /></el-icon>
          <span>酒店管理</span>
        </el-menu-item>
        <el-menu-item index="/admin/orders">
          <el-icon><Document /></el-icon>
          <span>订单管理</span>
        </el-menu-item>
        <el-menu-item index="/admin/users">
          <el-icon><User /></el-icon>
          <span>用户管理</span>
        </el-menu-item>
      </el-menu>

      <div class="sidebar-card">
        <img :src="adminVisualImage" alt="酒店后台视觉图" class="image-cover" />
        <div class="sidebar-card__mask">
          <strong>运营管理中心</strong>
          <p>统一查看酒店资源、订单流转与用户状态。</p>
        </div>
      </div>
    </el-aside>

    <el-container>
      <el-header class="admin-header">
        <div>
          <div class="gold-chip">Admin Workspace</div>
          <h1>{{ currentTitle }}</h1>
        </div>

        <div class="header-actions">
          <div class="account-card">
            <div class="account-avatar">{{ currentUserName.slice(0, 1) }}</div>
            <div>
              <strong>{{ currentUserName }}</strong>
              <span>{{ currentUserRole }}</span>
            </div>
          </div>
          <el-button @click="goHome">返回前台首页</el-button>
          <el-button @click="handleLogout">退出后台</el-button>
        </div>
      </el-header>

      <el-main class="admin-main">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<style scoped>
.admin-layout {
  min-height: 100vh;
  background: linear-gradient(180deg, #f6f0e8 0%, #f0ebe4 100%);
}

.sidebar {
  padding: 24px 18px;
  background: linear-gradient(180deg, #5f4f43 0%, #7c6958 100%);
  color: #fff;
}

.brand-block {
  display: flex;
  align-items: center;
  gap: 12px;
}

.brand-mark {
  width: 48px;
  height: 48px;
  display: grid;
  place-items: center;
  border-radius: 16px;
  background: linear-gradient(145deg, rgba(184, 149, 103, 0.82), rgba(255, 255, 255, 0.18));
  font-weight: 700;
}

.brand-block strong,
.sidebar-card__mask strong {
  display: block;
}

.brand-block span,
.sidebar-card__mask p {
  display: block;
  margin-top: 6px;
  font-size: 12px;
  color: rgba(255, 255, 255, 0.7);
}

.admin-menu {
  margin-top: 26px;
  border: none;
  background: transparent;
}

:deep(.admin-menu .el-menu-item) {
  margin-bottom: 8px;
  border-radius: 14px;
  color: rgba(255, 255, 255, 0.8);
}

:deep(.admin-menu .el-menu-item.is-active) {
  color: #fff;
  background: rgba(255, 255, 255, 0.12);
}

.sidebar-card {
  position: relative;
  margin-top: 28px;
  overflow: hidden;
  border-radius: 20px;
  height: 240px;
}

.sidebar-card__mask {
  position: absolute;
  inset: 0;
  padding: 18px;
  display: flex;
  flex-direction: column;
  justify-content: end;
  background: linear-gradient(180deg, rgba(0, 0, 0, 0.08), rgba(0, 0, 0, 0.66));
}

.admin-header {
  min-height: 92px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 24px 28px;
  background: transparent;
}

.admin-header h1 {
  margin: 14px 0 0;
  font-size: 32px;
  color: var(--brand-primary);
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 12px;
}

.account-card {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px 14px;
  border-radius: 18px;
  background: rgba(255, 252, 247, 0.92);
  border: 1px solid rgba(109, 90, 75, 0.08);
}

.account-avatar {
  width: 38px;
  height: 38px;
  border-radius: 14px;
  display: grid;
  place-items: center;
  background: linear-gradient(145deg, #6d5a4b, #b89567);
  color: #fff;
  font-weight: 700;
}

.account-card strong,
.account-card span {
  display: block;
}

.account-card span {
  margin-top: 4px;
  font-size: 12px;
  color: var(--text-muted);
}

.admin-main {
  padding: 0 28px 28px;
}

@media (max-width: 960px) {
  .admin-layout {
    display: block;
  }

  .sidebar {
    width: 100%;
  }

  .admin-header,
  .header-actions {
    flex-direction: column;
    align-items: flex-start;
  }

  .header-actions {
    width: 100%;
  }
}
</style>
