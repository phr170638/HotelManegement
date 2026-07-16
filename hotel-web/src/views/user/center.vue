<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Camera, EditPen, House, OfficeBuilding, RefreshRight, SwitchButton, Tickets } from '@element-plus/icons-vue'
import { useRouter } from 'vue-router'
import { getMyOrders, updateUser, uploadAvatar } from '@/api/user'
import { useUserStore } from '@/store/user'
import { getUserInitial, resolveMediaUrl } from '@/utils/media'

const router = useRouter()
const userStore = useUserStore()

const userInfo = ref(null)
const orderCount = ref(0)
const pendingCount = ref(0)
const paidCount = ref(0)
const loading = ref(false)
const loadFailed = ref(false)
const avatarUploading = ref(false)
const profileSaving = ref(false)
const editDialogVisible = ref(false)
const profileForm = reactive({
  nickname: ''
})

const quickStats = computed(() => [
  { label: '累计订单', value: String(orderCount.value).padStart(2, '0') },
  { label: '待支付', value: String(pendingCount.value).padStart(2, '0') },
  { label: '已支付', value: String(paidCount.value).padStart(2, '0') }
])

const roleLabel = computed(() => {
  if (userInfo.value?.roles?.includes('admin')) return '管理员'
  if (userInfo.value?.roles?.includes('user')) return '普通用户'
  return '身份待同步'
})

const avatarUrl = computed(() => resolveMediaUrl(userInfo.value?.avatar))

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
    paidCount.value = records.filter((item) => item.status === 1).length
    profileForm.nickname = userInfo.value?.nickname || ''
    loadFailed.value = false
  } catch (error) {
    userInfo.value = userStore.userInfo
    orderCount.value = 0
    pendingCount.value = 0
    paidCount.value = 0
    profileForm.nickname = userInfo.value?.nickname || ''
    loadFailed.value = true
  } finally {
    loading.value = false
  }
}

function openProfileEditor() {
  profileForm.nickname = userInfo.value?.nickname || ''
  editDialogVisible.value = true
}

function handleLogout() {
  userStore.logout()
  ElMessage.success('已退出登录')
  router.push('/')
}

async function handleAvatarUpload(options) {
  const file = options.file
  if (!file) return
  avatarUploading.value = true
  try {
    await uploadAvatar(file)
    await userStore.fetchUserInfo()
    userInfo.value = userStore.userInfo
    ElMessage.success('头像已更新')
    options.onSuccess?.()
  } catch (error) {
    ElMessage.error(error.message || '头像上传失败')
    options.onError?.(error)
  } finally {
    avatarUploading.value = false
  }
}

async function saveProfile() {
  const nickname = profileForm.nickname.trim()
  if (nickname.length < 2) {
    ElMessage.warning('用户名至少 2 个字')
    return
  }
  if (nickname.length > 20) {
    ElMessage.warning('用户名最多 20 个字')
    return
  }

  profileSaving.value = true
  try {
    if (nickname !== userInfo.value?.nickname) {
      await updateUser({ nickname })
    }
    await userStore.fetchUserInfo()
    userInfo.value = userStore.userInfo
    profileForm.nickname = userInfo.value?.nickname || ''
    editDialogVisible.value = false
    ElMessage.success('个人信息已更新')
  } catch (error) {
    ElMessage.error(error.message || '个人信息更新失败')
  } finally {
    profileSaving.value = false
  }
}

onMounted(fetchProfileData)
</script>

<template>
  <div class="center-page page-container narrow">
    <section class="profile-panel soft-card" v-loading="loading">
      <div class="profile-hero">
        <div class="profile-identity">
          <div class="avatar">
            <img v-if="avatarUrl" :src="avatarUrl" :alt="userInfo?.nickname || '用户头像'" class="avatar-image" />
            <span v-else>{{ getUserInitial(userInfo?.nickname) }}</span>
          </div>

          <div class="identity-copy">
            <div class="gold-chip">My Profile</div>
            <h1>{{ userInfo?.nickname || '我的账户' }}</h1>
            <div class="identity-meta">
              <span>{{ roleLabel }}</span>
              <span>{{ userInfo?.phone || '未绑定手机号' }}</span>
              <span>{{ userInfo?.email || '未绑定邮箱' }}</span>
            </div>
          </div>
        </div>

        <div class="hero-actions">
          <el-button type="primary" class="primary-btn" :icon="EditPen" @click="openProfileEditor">
            修改个人信息
          </el-button>
          <el-button :icon="Tickets" @click="$router.push('/user/orders')">
            查看我的订单（{{ orderCount }}）
          </el-button>
          <el-button v-if="userInfo?.roles?.includes('admin')" :icon="OfficeBuilding" @click="$router.push('/admin/hotels')">
            管理后台
          </el-button>
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
          <strong>{{ roleLabel }}</strong>
        </div>
        <div class="info-card">
          <span>注册时间</span>
          <strong>{{ userInfo?.createTime || '暂未获取' }}</strong>
        </div>
        <div class="info-card">
          <span>账户状态</span>
          <strong>{{ loadFailed ? '数据待刷新' : '账户正常' }}</strong>
        </div>
        <div class="info-card">
          <span>当前建议</span>
          <strong>{{ pendingCount > 0 ? '优先处理待支付订单' : '可以继续浏览和预订酒店' }}</strong>
        </div>
      </div>

      <div class="profile-actions">
        <el-button :icon="RefreshRight" @click="fetchProfileData">重新加载</el-button>
        <el-button :icon="House" @click="$router.push('/')">返回首页</el-button>
        <el-button :icon="SwitchButton" @click="handleLogout">退出登录</el-button>
      </div>
    </section>

    <el-dialog v-model="editDialogVisible" title="修改个人信息" width="560px">
      <div class="editor-panel">
        <div class="editor-avatar">
          <div class="avatar avatar-large">
            <img v-if="avatarUrl" :src="avatarUrl" :alt="userInfo?.nickname || '用户头像'" class="avatar-image" />
            <span v-else>{{ getUserInitial(userInfo?.nickname) }}</span>
          </div>
          <el-upload
            class="avatar-upload"
            accept="image/png,image/jpeg,image/webp,image/gif"
            :show-file-list="false"
            :auto-upload="true"
            :http-request="handleAvatarUpload"
            :disabled="avatarUploading"
          >
            <button type="button" class="upload-trigger" :disabled="avatarUploading">
              <el-icon><Camera /></el-icon>
              <span>{{ avatarUploading ? '上传中...' : '更换头像' }}</span>
            </button>
          </el-upload>
          <small>支持 JPG、PNG、WebP、GIF</small>
        </div>

        <div class="editor-form">
          <span class="editor-label">用户名</span>
          <el-input
            v-model="profileForm.nickname"
            maxlength="20"
            clearable
            placeholder="请输入用户名"
            @keyup.enter="saveProfile"
          />

          <div class="readonly-grid">
            <div class="readonly-card">
              <span>手机号</span>
              <strong>{{ userInfo?.phone || '暂未获取' }}</strong>
            </div>
            <div class="readonly-card">
              <span>邮箱</span>
              <strong>{{ userInfo?.email || '暂未获取' }}</strong>
            </div>
          </div>
        </div>
      </div>

      <template #footer>
        <el-button @click="editDialogVisible = false">取消</el-button>
        <el-button type="primary" class="primary-btn" :loading="profileSaving" @click="saveProfile">保存修改</el-button>
      </template>
    </el-dialog>
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

.profile-hero {
  display: flex;
  justify-content: space-between;
  gap: 24px;
  align-items: flex-start;
}

.profile-identity {
  display: flex;
  gap: 20px;
  align-items: center;
}

.avatar {
  width: 108px;
  height: 108px;
  border-radius: 32px;
  display: grid;
  place-items: center;
  overflow: hidden;
  flex-shrink: 0;
  background: linear-gradient(145deg, #6d5a4b, #b89567);
  color: #fff;
  font-size: 40px;
  font-weight: 700;
  box-shadow: 0 18px 36px rgba(88, 68, 47, 0.16);
}

.avatar-large {
  width: 116px;
  height: 116px;
}

.avatar-image {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}

.identity-copy {
  min-width: 0;
}

.identity-copy h1 {
  margin: 16px 0 0;
  font-size: clamp(30px, 4vw, 44px);
}

.identity-meta {
  margin-top: 14px;
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.identity-meta span {
  padding: 8px 12px;
  border-radius: 999px;
  background: rgba(109, 90, 75, 0.08);
  color: var(--text-secondary);
  font-size: 12px;
}

.hero-actions {
  display: flex;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: 12px;
  max-width: 360px;
}

.page-alert {
  margin-top: 22px;
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
  padding: 18px 20px;
  border-radius: 20px;
}

.stat-card {
  position: relative;
  overflow: hidden;
  border: 1px solid rgba(109, 90, 75, 0.08);
  background: rgba(255, 255, 255, 0.78);
}

.stat-card::before {
  content: '';
  position: absolute;
  left: 0;
  right: 0;
  top: 0;
  height: 3px;
  background: linear-gradient(90deg, #6d5a4b, #b89567);
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
  background: rgba(255, 255, 255, 0.68);
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

.editor-panel {
  display: grid;
  grid-template-columns: 168px 1fr;
  gap: 24px;
  align-items: start;
}

.editor-avatar {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
}

.avatar-upload {
  width: 100%;
}

.upload-trigger {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  width: 100%;
  min-height: 42px;
  padding: 0 16px;
  border: 1px solid rgba(109, 90, 75, 0.12);
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.9);
  color: var(--brand-primary);
  font: inherit;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  transition: border-color 0.2s ease, box-shadow 0.2s ease, transform 0.2s ease;
}

.upload-trigger:hover {
  transform: translateY(-1px);
  border-color: rgba(184, 149, 103, 0.38);
  box-shadow: 0 12px 24px rgba(88, 68, 47, 0.08);
}

.upload-trigger:disabled {
  cursor: wait;
  opacity: 0.7;
}

.editor-avatar small {
  color: var(--text-muted);
  font-size: 12px;
}

.editor-label {
  display: block;
  margin-bottom: 8px;
  font-size: 12px;
  color: var(--text-muted);
}

.readonly-grid {
  margin-top: 18px;
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.readonly-card {
  padding: 14px 16px;
  border-radius: 16px;
  background: rgba(109, 90, 75, 0.06);
}

.readonly-card span,
.readonly-card strong {
  display: block;
}

.readonly-card span {
  font-size: 12px;
  color: var(--text-muted);
}

.readonly-card strong {
  margin-top: 8px;
  line-height: 1.6;
}

.primary-btn {
  --el-button-bg-color: var(--brand-primary);
  --el-button-border-color: var(--brand-primary);
  --el-button-hover-bg-color: #7b6857;
  --el-button-hover-border-color: #7b6857;
}

@media (max-width: 860px) {
  .profile-hero,
  .profile-identity,
  .editor-panel {
    grid-template-columns: 1fr;
    flex-direction: column;
    align-items: flex-start;
  }

  .hero-actions {
    max-width: none;
    justify-content: flex-start;
  }
}

@media (max-width: 720px) {
  .stats-grid,
  .info-grid,
  .readonly-grid {
    grid-template-columns: 1fr;
  }

  .profile-actions,
  .hero-actions {
    flex-direction: column;
    width: 100%;
  }
}
</style>
