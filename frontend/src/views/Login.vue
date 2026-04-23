<template>
  <div class="auth-page">
    <div class="auth-card animate-scale-in">
      <router-link to="/" class="back-btn">
        <svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="2">
          <path d="M19 12H5M12 19l-7-7 7-7" />
        </svg>
        <span>{{ t('common.backHome') }}</span>
      </router-link>

      <div class="auth-header">
        <div class="logo-wrap">
          <img class="logo-icon" src="/logo.png" alt="Logo" />
        </div>
        <h1 class="logo-title">{{ t('login.title') }}</h1>
        <p class="auth-desc">{{ t('login.desc') }}</p>
      </div>

      <el-form class="auth-form" @submit.prevent>
        <el-form-item>
          <el-input
            v-model="loginForm.username"
            size="large"
            :placeholder="t('login.usernamePlaceholder')"
            @keyup.enter="handleLogin"
          >
            <template #prefix>
              <svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="2">
                <circle cx="12" cy="8" r="4" />
                <path d="M4 20c0-4 4-6 8-6s8 2 8 6" />
              </svg>
            </template>
          </el-input>
        </el-form-item>

        <el-form-item>
          <el-input
            v-model="loginForm.password"
            size="large"
            type="password"
            show-password
            :placeholder="t('login.passwordPlaceholder')"
            @keyup.enter="handleLogin"
          >
            <template #prefix>
              <svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="2">
                <rect x="3" y="11" width="18" height="11" rx="2" />
                <circle cx="12" cy="16" r="1" />
                <path d="M7 11V7a5 5 0 0110 0v4" />
              </svg>
            </template>
          </el-input>
        </el-form-item>

        <div v-if="shouldShowTurnstile" class="turnstile-row">
          <TurnstileWidget
            ref="turnstileWidgetRef"
            v-model="turnstileToken"
            :enabled="shouldShowTurnstile"
            :site-key="turnstileConfig.siteKey"
            action="login"
            :helper-text="t('login.turnstileHint')"
          />
        </div>

        <div class="remember-row">
          <label class="remember-label" @click="loginForm.rememberMe = !loginForm.rememberMe">
            <div class="checkbox" :class="{ checked: loginForm.rememberMe }">
              <svg v-if="loginForm.rememberMe" viewBox="0 0 24 24" width="12" height="12" fill="none" stroke="currentColor" stroke-width="3">
                <path d="M20 6L9 17l-5-5" />
              </svg>
            </div>
            <span class="remember-text">{{ t('login.rememberMe') }}</span>
          </label>
        </div>

        <el-button
          type="primary"
          size="large"
          class="submit-btn"
          :loading="loginLoading"
          @click="handleLogin"
        >
          {{ loginLoading ? t('login.submitting') : t('login.submit') }}
        </el-button>
      </el-form>

      <div class="auth-links">
        <router-link to="/register">{{ t('login.register') }}</router-link>
        <span class="divider">·</span>
        <router-link to="/forgot-password">{{ t('login.forgotPassword') }}</router-link>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { ElMessage } from 'element-plus'
import { publicApi } from '@/api'
import TurnstileWidget from '@/components/TurnstileWidget.vue'
import { useUserStore } from '@/stores'
import { sanitizePasswordInput } from '@/utils/passwordRules'
import { getDefaultTurnstileConfig, getTurnstileConfig } from '@/utils/turnstile'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const { t } = useI18n()
const loginLoading = ref(false)
const turnstileConfig = ref(getDefaultTurnstileConfig())
const turnstileRequired = ref(false)
const turnstileToken = ref('')
const turnstileWidgetRef = ref(null)

const loginForm = reactive({
  username: '',
  password: '',
  rememberMe: false
})

const shouldShowTurnstile = computed(() => turnstileRequired.value && turnstileConfig.value.login.enabled)

const resetTurnstile = () => {
  turnstileToken.value = ''
  turnstileWidgetRef.value?.reset?.()
}

watch(() => loginForm.password, (value) => {
  const sanitized = sanitizePasswordInput(value)
  if (sanitized !== value) {
    loginForm.password = sanitized
  }
})

const handleLogin = async () => {
  if (!loginForm.username || !loginForm.password) {
    ElMessage.warning(t('login.fillCredentials'))
    return
  }

  if (shouldShowTurnstile.value && !turnstileToken.value) {
    ElMessage.warning(t('login.turnstileRequired'))
    return
  }

  loginLoading.value = true
  try {
    const res = await publicApi.login({
      username: loginForm.username,
      password: loginForm.password,
      rememberMe: loginForm.rememberMe,
      turnstileToken: shouldShowTurnstile.value ? turnstileToken.value : ''
    })
    userStore.login(res.data.user, {
      accessToken: res.data.accessToken,
      refreshToken: res.data.refreshToken
    }, loginForm.rememberMe)
    turnstileRequired.value = false
    resetTurnstile()
    ElMessage.success(loginForm.rememberMe ? t('login.rememberSuccess') : t('login.success'))
    router.push(route.query.redirect || '/')
  } catch (error) {
    if (error?.data?.requireTurnstile || error?.code === 429) {
      turnstileRequired.value = true
      resetTurnstile()
    }
    ElMessage.error(error.message || 'Login failed')
  } finally {
    loginLoading.value = false
  }
}

onMounted(async () => {
  turnstileConfig.value = await getTurnstileConfig()
})
</script>

<style scoped>
.auth-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--gradient-bg);
  padding: 40px 20px;
}

.auth-card {
  width: 100%;
  max-width: 420px;
  background: var(--bg-glass);
  backdrop-filter: blur(20px) saturate(180%);
  -webkit-backdrop-filter: blur(20px) saturate(180%);
  border: 1px solid var(--border-color);
  border-radius: 28px;
  padding: 40px;
  box-shadow: var(--shadow-xl);
}

.back-btn {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  color: var(--text-secondary);
  text-decoration: none;
  font-size: 0.9rem;
  margin-bottom: 32px;
  transition: all 0.25s ease;
}

.back-btn:hover {
  color: var(--accent);
  transform: translateX(-4px);
}

.auth-header {
  text-align: center;
  margin-bottom: 36px;
}

.logo-wrap {
  margin-bottom: 20px;
}

.logo-icon {
  width: 72px;
  height: 72px;
  border-radius: 50%;
  border: 3px solid var(--accent);
  box-shadow: var(--shadow-md);
  transition: transform 0.3s ease;
}

.logo-icon:hover {
  transform: rotate(15deg) scale(1.05);
}

.logo-title {
  font-size: 1.8rem;
  font-weight: 700;
  color: var(--text-primary);
  margin-bottom: 8px;
}

.auth-desc {
  color: var(--text-secondary);
  font-size: 0.95rem;
}

.auth-form {
  margin-bottom: 24px;
}

.turnstile-row {
  margin-bottom: 24px;
}

.auth-form :deep(.el-input__wrapper) {
  background: var(--bg-tertiary);
  border-radius: 14px;
  box-shadow: none;
  border: 1px solid var(--border-color);
  padding: 4px 16px;
  transition: all 0.25s ease;
}

.auth-form :deep(.el-input__wrapper:hover) {
  border-color: var(--accent);
}

.auth-form :deep(.el-input__wrapper.is-focus) {
  border-color: var(--accent);
  box-shadow: 0 0 0 3px var(--accent-light);
}

.auth-form :deep(.el-input__inner) {
  color: var(--text-primary);
}

.auth-form :deep(.el-input__inner::placeholder) {
  color: var(--text-tertiary);
}

.auth-form :deep(.el-input__prefix) {
  color: var(--text-tertiary);
}

.submit-btn {
  width: 100%;
  height: 48px;
  border-radius: 14px;
  font-size: 1rem;
  font-weight: 600;
  background: var(--gradient-primary);
  border: none;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

.submit-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 24px rgba(57, 197, 187, 0.35);
}

.submit-btn:active {
  transform: translateY(0) scale(0.98);
}

.auth-links {
  text-align: center;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 16px;
}

.remember-row {
  margin-bottom: 24px;
}

.remember-label {
  display: inline-flex;
  align-items: center;
  gap: 10px;
  cursor: pointer;
  user-select: none;
}

.checkbox {
  width: 20px;
  height: 20px;
  border: 2px solid var(--border-color);
  border-radius: 6px;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.25s ease;
  background: var(--bg-tertiary);
}

.checkbox.checked {
  background: var(--accent);
  border-color: var(--accent);
}

.checkbox svg {
  color: #fff;
}

.remember-text {
  color: var(--text-secondary);
  font-size: 0.9rem;
}

.remember-label:hover .checkbox:not(.checked) {
  border-color: var(--accent);
}

.auth-links a {
  color: var(--text-secondary);
  text-decoration: none;
  font-size: 0.9rem;
  transition: color 0.25s ease;
}

.auth-links a:hover {
  color: var(--accent);
}

.divider {
  color: var(--border-color);
}

@media (max-width: 480px) {
  .auth-card {
    padding: 28px;
  }

  .logo-icon {
    width: 56px;
    height: 56px;
  }

  .logo-title {
    font-size: 1.5rem;
  }
}
</style>
