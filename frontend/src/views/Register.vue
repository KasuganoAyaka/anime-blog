<template>
  <div class="auth-page">
    <div class="auth-card animate-scale-in">
      <div class="auth-header">
        <div class="logo-wrap">
          <img class="logo-icon" src="/logo.png" alt="Logo" />
        </div>
        <div class="logo-title">{{ t('register.title') }}</div>
        <p class="auth-desc">{{ t('register.desc') }}</p>
      </div>

      <el-form ref="formRef" class="auth-form" :model="form" :rules="rules" @submit.prevent>
        <el-form-item prop="username">
          <el-input
            v-model="form.username"
            size="large"
            :placeholder="t('register.usernamePlaceholder')"
            :maxlength="USERNAME_MAX_LENGTH"
            show-word-limit
          />
          <div class="field-tip">{{ t('register.usernameTip', { min: USERNAME_MIN_LENGTH, max: USERNAME_MAX_LENGTH }) }}</div>
        </el-form-item>

        <el-form-item prop="email">
          <el-input v-model="form.email" size="large" :placeholder="t('register.emailPlaceholder')">
            <template #suffix>
              <el-button link :disabled="cooldown > 0" @click="sendEmailCode">
                {{ cooldown > 0 ? `${cooldown}s` : t('register.sendCode') }}
              </el-button>
            </template>
          </el-input>
        </el-form-item>

        <el-form-item prop="emailCode">
          <el-input v-model="form.emailCode" size="large" :placeholder="t('register.emailCodePlaceholder')" />
        </el-form-item>

        <el-form-item prop="password">
          <el-input
            v-model="form.password"
            size="large"
            type="password"
            show-password
            :placeholder="t('register.passwordPlaceholder')"
          />
          <div class="password-rules">
            <div
              v-for="item in passwordRuleItems"
              :key="item.key"
              :class="['password-rule', { met: item.met }]"
            >
              {{ item.met ? '[OK]' : '[ ]' }} {{ item.label }}
            </div>
          </div>
        </el-form-item>

        <el-form-item prop="confirmPassword">
          <el-input
            v-model="form.confirmPassword"
            size="large"
            type="password"
            show-password
            :placeholder="t('register.confirmPasswordPlaceholder')"
          />
        </el-form-item>

        <div v-if="registerEmailTurnstileEnabled" class="turnstile-row">
          <TurnstileWidget
            ref="turnstileWidgetRef"
            v-model="turnstileToken"
            :enabled="registerEmailTurnstileEnabled"
            :site-key="turnstileConfig.siteKey"
            action="register_email"
            :helper-text="t('register.turnstileHint')"
          />
        </div>

        <el-button
          type="primary"
          size="large"
          class="submit-btn"
          :loading="loading"
          @click="handleRegister"
        >
          {{ t('register.submit') }}
        </el-button>
      </el-form>

      <div class="auth-links">
        <router-link to="/login">{{ t('register.backLogin') }}</router-link>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, onUnmounted, reactive, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { ElMessage } from 'element-plus'
import { publicApi } from '@/api'
import TurnstileWidget from '@/components/TurnstileWidget.vue'
import {
  USERNAME_MAX_LENGTH,
  USERNAME_MIN_LENGTH,
  getPasswordRuleItems,
  getPasswordValidationMessage,
  sanitizePasswordInput
} from '@/utils/passwordRules'
import { getDefaultTurnstileConfig, getTurnstileConfig } from '@/utils/turnstile'

const router = useRouter()
const { t } = useI18n()
const formRef = ref(null)
const loading = ref(false)
const cooldown = ref(0)
const turnstileConfig = ref(getDefaultTurnstileConfig())
const turnstileToken = ref('')
const turnstileWidgetRef = ref(null)
let cooldownTimer = null

const emailRegex = /^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$/

const form = reactive({
  username: '',
  email: '',
  emailCode: '',
  password: '',
  confirmPassword: ''
})

const passwordRuleItems = computed(() => (
  getPasswordRuleItems(form.password, t).filter(item => item.key !== 'charset')
))
const registerEmailTurnstileEnabled = computed(() => turnstileConfig.value.register.emailCheckEnabled)

const resetTurnstile = () => {
  turnstileToken.value = ''
  turnstileWidgetRef.value?.reset?.()
}

const validatePassword = (rule, value, callback) => {
  const message = getPasswordValidationMessage(value, t)
  if (message) {
    callback(new Error(message))
    return
  }
  callback()
}

const validateConfirmPassword = (rule, value, callback) => {
  if (!value) {
    callback(new Error(t('register.messages.confirmPasswordRequired')))
    return
  }
  if (value !== form.password) {
    callback(new Error(t('register.messages.confirmPasswordMismatch')))
    return
  }
  callback()
}

const rules = computed(() => ({
  username: [
    { required: true, message: t('register.messages.usernameRequired'), trigger: 'blur' },
    {
      min: USERNAME_MIN_LENGTH,
      max: USERNAME_MAX_LENGTH,
      message: t('register.messages.usernameLength', { min: USERNAME_MIN_LENGTH, max: USERNAME_MAX_LENGTH }),
      trigger: 'blur'
    }
  ],
  email: [
    { required: true, message: t('register.messages.emailRequired'), trigger: 'blur' },
    { type: 'email', message: t('register.messages.emailInvalid'), trigger: 'blur' }
  ],
  emailCode: [
    { required: true, message: t('register.messages.emailCodeRequired'), trigger: 'blur' },
    { len: 6, message: t('register.messages.emailCodeLength'), trigger: 'blur' }
  ],
  password: [{ validator: validatePassword, trigger: 'change' }],
  confirmPassword: [{ validator: validateConfirmPassword, trigger: 'change' }]
}))

const startCooldown = () => {
  if (cooldownTimer) {
    clearInterval(cooldownTimer)
  }

  cooldown.value = 60
  cooldownTimer = setInterval(() => {
    cooldown.value -= 1
    if (cooldown.value <= 0) {
      clearInterval(cooldownTimer)
      cooldownTimer = null
    }
  }, 1000)
}

const stopCooldown = () => {
  if (cooldownTimer) {
    clearInterval(cooldownTimer)
    cooldownTimer = null
  }
  cooldown.value = 0
}

const sendEmailCode = () => {
  if (!emailRegex.test(form.email || '')) {
    ElMessage.warning(t('register.messages.invalidEmailFirst'))
    return
  }

  if (registerEmailTurnstileEnabled.value && !turnstileToken.value) {
    ElMessage.warning(t('register.messages.turnstileRequired'))
    return
  }

  startCooldown()
  ElMessage.success(t('register.messages.codeSending'))

  void publicApi.sendEmailCode(form.email.trim(), 'register', turnstileToken.value)
    .then(() => {
      resetTurnstile()
    })
    .catch((error) => {
      stopCooldown()
      resetTurnstile()
      ElMessage.error(error.message || t('register.messages.codeSendFailed'))
    })
}

const handleRegister = async () => {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  loading.value = true
  try {
    await publicApi.register({
      username: form.username.trim(),
      email: form.email.trim(),
      emailCode: form.emailCode,
      password: form.password,
      confirmPassword: form.confirmPassword
    })
    ElMessage.success(t('register.messages.success'))
    router.push('/login')
  } catch (error) {
    ElMessage.error(error.message || t('register.messages.failed'))
  } finally {
    loading.value = false
  }
}

watch(() => form.password, (value) => {
  const sanitized = sanitizePasswordInput(value)
  if (sanitized !== value) {
    form.password = sanitized
  }
})

watch(() => form.confirmPassword, (value) => {
  const sanitized = sanitizePasswordInput(value)
  if (sanitized !== value) {
    form.confirmPassword = sanitized
  }
})

onMounted(() => {
  void getTurnstileConfig().then((config) => {
    turnstileConfig.value = config
  })
})

onUnmounted(() => {
  stopCooldown()
})
</script>

<style scoped>
.auth-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 40px 20px;
  background: var(--gradient-bg);
}

.auth-card {
  width: 100%;
  max-width: 460px;
  padding: 40px;
  border-radius: 28px;
  background: var(--bg-glass);
  backdrop-filter: blur(20px) saturate(180%);
  -webkit-backdrop-filter: blur(20px) saturate(180%);
  border: 1px solid var(--border-color);
  box-shadow: var(--shadow-xl);
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
  margin: 0 0 8px;
  font-size: 1.8rem;
  font-weight: 700;
  color: var(--text-primary);
}

.auth-desc {
  margin: 0;
  color: var(--text-secondary);
  font-size: 0.95rem;
}

.auth-form {
  margin-bottom: 24px;
}

.auth-form :deep(.el-form-item) {
  margin-bottom: 22px;
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

.auth-form :deep(.el-input__suffix) {
  color: var(--text-secondary);
}

.auth-form :deep(.el-input__count) {
  color: var(--text-tertiary);
}

.field-tip {
  margin-top: 8px;
  font-size: 0.86rem;
  color: var(--text-secondary);
}

.password-rules {
  width: 100%;
  margin-top: 10px;
  display: grid;
  gap: 6px;
  padding: 12px 14px;
  border-radius: 14px;
  background: rgba(255, 255, 255, 0.04);
  border: 1px solid color-mix(in srgb, var(--border-color) 88%, transparent);
}

.password-rule {
  font-size: 0.84rem;
  color: var(--text-secondary);
  transition: color 0.2s ease, transform 0.2s ease;
}

.password-rule.met {
  color: var(--accent);
  transform: translateX(2px);
}

.auth-card :deep(.el-form-item__error) {
  position: static;
  margin-top: 8px;
  white-space: normal;
  line-height: 1.5;
  word-break: break-word;
}

.turnstile-row {
  margin-bottom: 22px;
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
