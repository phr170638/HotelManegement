<script setup>
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '@/store/user'
import { hasRole } from '@/utils/auth'
import { getUserInitial, resolveMediaUrl } from '@/utils/media'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const activePath = computed(() => {
  if (route.path.startsWith('/search')) return '/search'
  if (route.path.startsWith('/user')) return '/user/center'
  return '/'
})

const userName = computed(() => userStore.userInfo?.nickname || '欢迎回来')
const isAdmin = computed(() => hasRole('admin'))
const userAvatar = computed(() => resolveMediaUrl(userStore.userInfo?.avatar))

function goTo(path) {
  router.push(path)
}

function handleLogout() {
  userStore.logout()
  router.push('/')
}
</script>

<template>
  <header class="site-header">
    <div class="page-section header-inner">
      <div class="brand" @click="goTo('/')">
        <div class="brand-mark">HM</div>
        <div>
          <strong>华庭酒店</strong>
          <span>自然自在的旅宿体验</span>
        </div>
      </div>

      <nav class="nav-links">
        <button :class="['nav-item', { active: activePath === '/' }]" @click="goTo('/')">首页</button>
        <button :class="['nav-item', { active: activePath === '/search' }]" @click="goTo('/search')">搜索酒店</button>
        <button :class="['nav-item', { active: activePath === '/user/center' }]" @click="goTo('/user/center')">个人中心</button>
        <button v-if="isAdmin" class="nav-item admin-entry" @click="goTo('/admin/hotels')">后台管理</button>
      </nav>

      <div class="header-actions">
        <template v-if="userStore.isLoggedIn">
          <div class="user-pill">
            <span class="user-pill__avatar">
              <img v-if="userAvatar" :src="userAvatar" :alt="userName" class="user-pill__avatar-image" />
              <span v-else>{{ getUserInitial(userName) }}</span>
            </span>
            <div>
              <strong>{{ userName }}</strong>
              <span>{{ isAdmin ? '管理员' : '已登录用户' }}</span>
            </div>
          </div>
          <el-button class="plain-btn" @click="goTo('/user/orders')">我的订单</el-button>
          <el-button type="primary" class="primary-btn" @click="handleLogout">退出</el-button>
        </template>
        <template v-else>
          <el-button class="plain-btn" @click="goTo('/login')">登录</el-button>
          <el-button type="primary" class="primary-btn" @click="goTo('/register')">注册</el-button>
        </template>
      </div>
    </div>
  </header>
</template>

<style scoped>
.site-header {
  position: sticky;
  top: 0;
  z-index: 30;
  background: rgba(250, 246, 240, 0.82);
  backdrop-filter: blur(18px);
  border-bottom: 1px solid rgba(109, 90, 75, 0.08);
}

.header-inner {
  min-height: 76px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20px;
}

.header-inner > * {
  min-width: 0;
}

.brand {
  display: flex;
  align-items: center;
  gap: 12px;
  cursor: pointer;
  min-width: 0;
}

.brand-mark {
  width: 46px;
  height: 46px;
  display: grid;
  place-items: center;
  border-radius: 14px;
  background: linear-gradient(145deg, #6d5a4b, #b89567);
  color: #f7f3ee;
  font-weight: 700;
  letter-spacing: 0.08em;
  box-shadow: 0 10px 24px rgba(109, 90, 75, 0.2);
}

.brand strong,
.user-pill strong {
  display: block;
  font-size: 15px;
}

.brand span,
.user-pill span {
  display: block;
  font-size: 12px;
  color: var(--text-muted);
}

.brand strong,
.brand span {
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.nav-links {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 6px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.55);
  border: 1px solid rgba(109, 90, 75, 0.08);
  min-width: 0;
}

.nav-item {
  border: none;
  background: transparent;
  padding: 10px 16px;
  border-radius: 999px;
  color: var(--text-secondary);
  cursor: pointer;
  white-space: nowrap;
  transition: all 0.2s ease;
}

.nav-item:hover,
.nav-item.active {
  color: var(--brand-primary);
  background: rgba(109, 90, 75, 0.08);
}

.admin-entry {
  color: var(--brand-secondary-strong);
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
  justify-content: flex-end;
}

.user-pill {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 8px 12px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.55);
  border: 1px solid rgba(109, 90, 75, 0.08);
  min-width: 0;
}

.user-pill__avatar {
  width: 36px;
  height: 36px;
  display: grid;
  place-items: center;
  border-radius: 50%;
  background: linear-gradient(145deg, rgba(184, 149, 103, 0.26), rgba(109, 90, 75, 0.14));
  color: var(--brand-primary);
  font-weight: 700;
  overflow: hidden;
  flex-shrink: 0;
}

.user-pill__avatar-image {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}

.plain-btn {
  --el-button-border-color: rgba(109, 90, 75, 0.12);
  --el-button-text-color: var(--brand-primary);
  --el-button-bg-color: rgba(255, 255, 255, 0.7);
}

.primary-btn {
  --el-button-bg-color: var(--brand-primary);
  --el-button-border-color: var(--brand-primary);
  --el-button-hover-bg-color: #7b6857;
  --el-button-hover-border-color: #7b6857;
}

@media (max-width: 1180px) {
  .header-inner {
    flex-wrap: wrap;
    padding: 14px 0;
  }

  .nav-links {
    order: 3;
    width: 100%;
    justify-content: flex-start;
    overflow-x: auto;
    padding-bottom: 2px;
  }

  .header-actions {
    margin-left: auto;
  }
}

@media (max-width: 720px) {
  .header-actions {
    width: 100%;
    justify-content: stretch;
  }

  .header-actions :deep(.el-button) {
    flex: 1 1 calc(50% - 6px);
    min-width: 132px;
  }

  .user-pill {
    width: 100%;
  }

  .brand {
    width: 100%;
  }

  .brand > div {
    min-width: 0;
  }
}
</style>
