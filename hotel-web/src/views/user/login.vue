<script setup>
import { computed, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/store/user'
import { validatePassword, validatePhone } from '@/utils/validate'
import { authVisualImage } from '@/utils/hotel'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()
const formRef = ref()
const loading = ref(false)
const canGoBack = computed(() => window.history.length > 1)
const redirectPath = computed(() => String(route.query.redirect || ''))
const registerLink = computed(() => ({
  path: '/register',
  query: {
    from: redirectPath.value || route.fullPath,
    redirect: redirectPath.value || undefined
  }
}))

const form = reactive({
  phone: '',
  password: ''
})

const rules = {
  phone: [{ validator: validatePhone, trigger: 'blur' }],
  password: [{ validator: validatePassword, trigger: 'blur' }]
}

async function handleLogin() {
  form.phone = form.phone.trim()
  form.password = form.password.trim()
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return

  loading.value = true
  try {
    await userStore.login({ phone: form.phone, password: form.password })
    ElMessage.success('登录成功')
    router.push(String(route.query.redirect || '/'))
  } catch (error) {
    ElMessage.error(error.message || '登录失败')
  } finally {
    loading.value = false
  }
}

function goHome() {
  router.push('/')
}

function goBack() {
  if (canGoBack.value) {
    router.back()
    return
  }
  goHome()
}
</script>

<template>
  <div class="auth-page">
    <div class="auth-panel soft-card">
      <section class="visual-side">
        <img :src="authVisualImage" alt="酒店登录视觉图" class="image-cover" />
        <div class="hero-overlay" />
        <div class="visual-copy">
          <div class="gold-chip">Welcome Back</div>
          <h1>登录后继续管理你的预订与后台操作</h1>
          <p>支持查看订单、管理入住信息，并为管理人员提供完整的后台入口。</p>
        </div>
      </section>

      <section class="form-side">
        <div class="auth-nav">
          <el-button text @click="goHome">返回首页</el-button>
          <el-button text @click="goBack">返回上一页</el-button>
        </div>

        <div class="form-top">
          <div class="gold-chip">Sign In</div>
          <h2>欢迎回来</h2>
          <p>请输入你的手机号与密码，继续当前的预订与管理操作。</p>
          <el-alert
            v-if="redirectPath"
            class="redirect-tip"
            type="info"
            :closable="false"
            title="登录后将继续回到你刚才访问的页面"
          />
        </div>

        <el-form ref="formRef" :model="form" :rules="rules" label-width="0" class="auth-form" @submit.prevent="handleLogin">
          <el-form-item prop="phone">
            <el-input v-model="form.phone" size="large" placeholder="请输入手机号" />
          </el-form-item>
          <el-form-item prop="password">
            <el-input v-model="form.password" size="large" type="password" show-password placeholder="请输入密码" />
          </el-form-item>
          <el-button type="primary" native-type="submit" class="submit-btn" :loading="loading">立即登录</el-button>
        </el-form>

        <div class="tips">
          <span>还没有账号？</span>
          <router-link :to="registerLink">去注册</router-link>
        </div>
      </section>
    </div>
  </div>
</template>

<style scoped>
.auth-page {
  min-height: 100vh;
  display: grid;
  place-items: center;
  padding: 24px;
}

.auth-panel {
  width: min(1080px, 100%);
  display: grid;
  grid-template-columns: 1.1fr 0.9fr;
  overflow: hidden;
  background: rgba(255, 252, 247, 0.92);
}

.visual-side {
  position: relative;
  min-height: 680px;
}

.visual-copy {
  position: absolute;
  left: 40px;
  right: 40px;
  bottom: 40px;
  z-index: 1;
  color: #fff;
}

.visual-copy h1,
.form-top h2 {
  margin: 18px 0 12px;
  font-size: clamp(28px, 4vw, 44px);
}

.visual-copy p,
.form-top p {
  margin: 0;
  line-height: 1.8;
}

.form-side {
  padding: 42px;
  display: flex;
  flex-direction: column;
  justify-content: center;
}

.auth-nav {
  display: flex;
  justify-content: space-between;
  gap: 12px;
}

.form-top p {
  color: var(--text-secondary);
}

.auth-form {
  margin-top: 28px;
}

.redirect-tip {
  margin-top: 16px;
}

.submit-btn {
  width: 100%;
  --el-button-bg-color: var(--brand-primary);
  --el-button-border-color: var(--brand-primary);
  --el-button-text-color: #fff8f0;
  --el-button-hover-bg-color: #7b6857;
  --el-button-hover-border-color: #7b6857;
  --el-button-hover-text-color: #fffdfa;
}

.tips {
  margin-top: 20px;
  color: var(--text-secondary);
}

.tips a {
  margin-left: 6px;
  color: var(--brand-primary);
  font-weight: 700;
}

@media (max-width: 900px) {
  .auth-panel {
    grid-template-columns: 1fr;
  }

  .visual-side {
    min-height: 280px;
  }

  .form-side {
    padding: 28px 20px;
  }

  .auth-nav {
    flex-direction: column;
    align-items: flex-start;
  }
}
</style>
