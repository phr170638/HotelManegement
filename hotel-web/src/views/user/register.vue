<script setup>
import { computed, onBeforeUnmount, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ArrowLeft, HomeFilled, Lock, Message, Phone, Tickets, UserFilled } from '@element-plus/icons-vue'
import { sendCode } from '@/api/user'
import { useUserStore } from '@/store/user'
import { validatePassword, validatePhone } from '@/utils/validate'
import { authVisualImage } from '@/utils/hotel'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()
const formRef = ref()
const loading = ref(false)
const sendingCode = ref(false)
const countdown = ref(0)
let countdownTimer = null
const canGoBack = computed(() => window.history.length > 1)
const fromPath = computed(() => String(route.query.from || ''))
const loginLink = computed(() => ({
  path: '/login',
  query: {
    redirect: String(route.query.redirect || fromPath.value || '')
  }
}))

const form = reactive({
  phone: '',
  password: '',
  code: ''
})

const rules = {
  phone: [{ validator: validatePhone, trigger: 'blur' }],
  password: [{ validator: validatePassword, trigger: 'blur' }],
  code: [{ required: true, message: '请输入验证码', trigger: 'blur' }]
}

function startCountdown(seconds = 60) {
  countdown.value = seconds
  window.clearInterval(countdownTimer)
  countdownTimer = window.setInterval(() => {
    if (countdown.value <= 1) {
      countdown.value = 0
      window.clearInterval(countdownTimer)
      countdownTimer = null
      return
    }
    countdown.value -= 1
  }, 1000)
}

async function handleSendCode() {
  form.phone = form.phone.trim()
  try {
    await formRef.value?.validateField('phone')
  } catch {
    return
  }

  if (sendingCode.value || countdown.value > 0) return

  sendingCode.value = true
  try {
    const result = await sendCode(form.phone, 'register', { silent: true })
    if (result?.debugCode) {
      form.code = result.debugCode
    }
    startCountdown(result?.resendIntervalInSeconds || 60)
    ElMessage.success(result?.debugCode ? '验证码已发送，当前联调环境已自动填入验证码' : '验证码已发送，请注意查收')
  } catch (error) {
    ElMessage.error(error.response?.data?.message || error.message || '验证码发送失败')
  } finally {
    sendingCode.value = false
  }
}

async function handleRegister() {
  form.phone = form.phone.trim()
  form.password = form.password.trim()
  form.code = form.code.trim()
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return

  loading.value = true
  try {
    await userStore.register({ phone: form.phone, password: form.password, code: form.code })
    ElMessage.success('注册成功，请登录')
    router.push(loginLink.value)
  } catch (error) {
    ElMessage.error(error.message || '注册失败')
  } finally {
    loading.value = false
  }
}

onBeforeUnmount(() => {
  window.clearInterval(countdownTimer)
})

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
    <div class="auth-panel soft-card register-panel">
      <section class="form-side">
        <div class="auth-nav">
          <el-button text :icon="HomeFilled" @click="goHome">返回首页</el-button>
          <el-button text :icon="ArrowLeft" @click="goBack">返回上一页</el-button>
        </div>

        <div class="form-top">
          <div class="gold-chip">Create Account</div>
          <h2>创建你的酒店系统账号</h2>
          <p>注册后即可体验搜索、下单、订单管理等完整流程。</p>
          <div class="feature-list">
            <span class="feature-tag"><el-icon><UserFilled /></el-icon> 会员账号</span>
            <span class="feature-tag"><el-icon><Tickets /></el-icon> 订单跟踪</span>
            <span class="feature-tag"><el-icon><Message /></el-icon> 验证注册</span>
          </div>
          <el-alert
            v-if="fromPath"
            class="redirect-tip"
            type="info"
            :closable="false"
            title="完成注册后，你可以继续回到刚才的浏览流程"
          />
        </div>

        <el-form ref="formRef" :model="form" :rules="rules" label-width="0" class="auth-form" @submit.prevent="handleRegister">
          <el-form-item prop="phone">
            <el-input v-model="form.phone" size="large" placeholder="请输入手机号">
              <template #prefix>
                <el-icon class="input-prefix"><Phone /></el-icon>
              </template>
            </el-input>
          </el-form-item>
          <el-form-item prop="password">
            <el-input v-model="form.password" size="large" type="password" show-password placeholder="密码至少 6 位">
              <template #prefix>
                <el-icon class="input-prefix"><Lock /></el-icon>
              </template>
            </el-input>
          </el-form-item>
          <el-form-item prop="code">
            <div class="code-row">
              <el-input v-model="form.code" size="large" placeholder="请输入验证码">
                <template #prefix>
                  <el-icon class="input-prefix"><Message /></el-icon>
                </template>
              </el-input>
              <el-button
                class="code-btn"
                native-type="button"
                :disabled="countdown > 0"
                :loading="sendingCode"
                @click="handleSendCode"
              >
                {{ countdown > 0 ? `${countdown}s 后重试` : '发送验证码' }}
              </el-button>
            </div>
          </el-form-item>
          <el-button type="primary" native-type="submit" class="submit-btn" :loading="loading">完成注册</el-button>
        </el-form>

        <div class="entry-hint">
          <span class="entry-hint__title">注册说明</span>
          <p>完成注册后即可下单、查看订单与入住信息；验证码发送后会按倒计时控制重发频率。</p>
        </div>

        <div class="tips">
          <span>已经有账号？</span>
          <router-link :to="loginLink">立即登录</router-link>
        </div>
      </section>

      <section class="visual-side">
        <img :src="authVisualImage" alt="酒店注册视觉图" class="image-cover" />
        <div class="hero-overlay" />
        <div class="visual-copy">
          <div class="gold-chip">Member Access</div>
          <h1>注册后即可开启更完整的入住服务</h1>
          <p>完成注册后，可管理订单、查看入住信息，并享受更便捷的预订流程。</p>
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
  grid-template-columns: 0.9fr 1.1fr;
  overflow: hidden;
  background: rgba(255, 252, 247, 0.92);
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
  flex-wrap: wrap;
}

.form-top h2,
.visual-copy h1 {
  margin: 18px 0 12px;
  font-size: clamp(28px, 4vw, 44px);
}

.form-top p,
.visual-copy p {
  margin: 0;
  color: var(--text-secondary);
  line-height: 1.8;
}

.feature-list {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-top: 18px;
}

.feature-tag {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 9px 12px;
  border-radius: 999px;
  background: rgba(109, 90, 75, 0.07);
  color: var(--text-secondary);
  font-size: 13px;
}

.auth-form {
  margin-top: 28px;
}

.input-prefix {
  color: var(--text-muted);
}

.redirect-tip {
  margin-top: 16px;
}

.code-row {
  display: grid;
  grid-template-columns: 1fr 132px;
  gap: 12px;
  width: 100%;
}

.code-btn {
  --el-button-text-color: var(--brand-primary);
  --el-button-border-color: rgba(109, 90, 75, 0.24);
  --el-button-bg-color: rgba(255, 252, 247, 0.92);
  --el-button-hover-text-color: var(--brand-primary);
  --el-button-hover-border-color: rgba(109, 90, 75, 0.36);
  --el-button-hover-bg-color: rgba(255, 248, 240, 0.96);
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

.entry-hint {
  margin-top: 18px;
  padding: 14px 16px;
  border-radius: 16px;
  background: rgba(109, 90, 75, 0.06);
  border: 1px solid rgba(109, 90, 75, 0.08);
}

.entry-hint__title {
  display: block;
  margin-bottom: 6px;
  color: var(--brand-primary);
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.06em;
  text-transform: uppercase;
}

.entry-hint p {
  margin: 0;
  color: var(--text-secondary);
  line-height: 1.7;
  font-size: 13px;
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

.visual-copy p {
  color: rgba(247, 243, 238, 0.88);
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
    justify-content: flex-start;
  }

  .auth-nav :deep(.el-button) {
    margin-left: 0;
  }

  .code-row {
    grid-template-columns: 1fr;
  }
}
</style>
