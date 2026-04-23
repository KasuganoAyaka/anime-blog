<template>
  <div class="auth-page">
    <div class="auth-card">
      <div v-if="step !== 4" class="auth-header">
        <h1>{{ t('forgotPassword.title') }}</h1>
        <p>{{ t('forgotPassword.desc') }}</p>
      </div>

      <div v-if="step === 1" class="step-block">
        <el-form @submit.prevent>
          <el-form-item>
            <el-input
              v-model="step1Form.email"
              size="large"
              :placeholder="t('forgotPassword.emailPlaceholder')"
              @keyup.enter="handleSendCode"
            />
          </el-form-item>
          <el-button type="primary" size="large" class="submit-btn" :loading="loading" @click="handleSendCode">
            {{ t('forgotPassword.sendCode') }}
          </el-button>
        </el-form>
      </div>

      <div v-else-if="step === 2" class="step-block">
        <el-form @submit.prevent>
          <el-form-item>
            <el-input
              v-model="step2Form.code"
              size="large"
              :placeholder="t('forgotPassword.codePlaceholder')"
              @keyup.enter="handleVerifyCode"
            />
          </el-form-item>
          <el-button type="primary" size="large" class="submit-btn" :loading="loading" @click="handleVerifyCode">
            {{ t('forgotPassword.verifyCode') }}
          </el-button>
        </el-form>
        <div class="helper-row">
          <button type="button" class="text-btn" :disabled="cooldown > 0" @click="resendCode">
            {{ cooldown > 0 ? t('forgotPassword.resendCountdown', { seconds: cooldown }) : t('forgotPassword.resendCode') }}
          </button>
          <button type="button" class="text-btn" @click="step = 1">{{ t('forgotPassword.changeEmail') }}</button>
        </div>
      </div>

      <div v-else-if="step === 3" class="step-block">
        <el-form @submit.prevent>
          <el-form-item>
            <el-input
              v-model="step3Form.newPassword"
              size="large"
              type="password"
              show-password
              :placeholder="t('forgotPassword.newPasswordPlaceholder')"
            />
            <div class="password-rules">
              <div
                v-for="item in passwordRuleItems"
                :key="item.key"
                :class="['password-rule', { met: item.met }]"
              >
                {{ item.met ? '•' : '·' }} {{ item.label }}
              </div>
            </div>
          </el-form-item>
          <el-form-item>
            <el-input
              v-model="step3Form.confirmPassword"
              size="large"
              type="password"
              show-password
              :placeholder="t('forgotPassword.confirmNewPasswordPlaceholder')"
              @keyup.enter="handleResetPassword"
            />
          </el-form-item>
          <el-button type="primary" size="large" class="submit-btn" :loading="loading" @click="handleResetPassword">
            {{ t('forgotPassword.resetPassword') }}
          </el-button>
        </el-form>
      </div>

      <div v-else class="step-block success-block">
        <h2>{{ t('forgotPassword.successTitle') }}</h2>
        <p>{{ t('forgotPassword.successDesc') }}</p>
        <router-link to="/login" class="link-btn">{{ t('forgotPassword.backLogin') }}</router-link>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, onUnmounted, reactive, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage } from 'element-plus'
import { publicApi } from '@/api'
import { getPasswordRuleItems, getPasswordValidationMessage, sanitizePasswordInput } from '@/utils/passwordRules'

const { t } = useI18n()
const step = ref(1)
const loading = ref(false)
const cooldown = ref(0)
let cooldownTimer = null

const step1Form = reactive({ email: '' })
const step2Form = reactive({ code: '' })
const step3Form = reactive({ newPassword: '', confirmPassword: '' })

const emailRegex = /^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$/
const passwordRuleItems = computed(() => getPasswordRuleItems(step3Form.newPassword, t))

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

const queueSendCode = ({ nextStep }) => {
  const previousStep = step.value

  startCooldown()
  step.value = nextStep
  ElMessage.success(t('forgotPassword.messages.codeSending'))

  void publicApi.sendForgetCode(step1Form.email.trim())
    .catch((error) => {
      stopCooldown()
      step.value = previousStep
      ElMessage.error(error.message || t('forgotPassword.messages.codeSendFailed'))
    })
}

const handleSendCode = () => {
  if (!emailRegex.test(step1Form.email || '')) {
    ElMessage.warning(t('forgotPassword.messages.invalidEmail'))
    return
  }

  queueSendCode({ nextStep: 2 })
}

const resendCode = () => {
  if (cooldown.value > 0) return
  queueSendCode({ nextStep: 2 })
}

const handleVerifyCode = async () => {
  if (!step2Form.code || step2Form.code.length !== 6) {
    ElMessage.warning(t('forgotPassword.messages.codeRequired'))
    return
  }

  loading.value = true
  try {
    await publicApi.verifyEmailCode({
      email: step1Form.email.trim(),
      code: step2Form.code.trim(),
      type: 'forget'
    })
    ElMessage.success(t('forgotPassword.messages.codeVerified'))
    step.value = 3
  } catch (error) {
    ElMessage.error(error.message || t('forgotPassword.messages.codeInvalid'))
  } finally {
    loading.value = false
  }
}

const handleResetPassword = async () => {
  if (!step3Form.newPassword || !step3Form.confirmPassword) {
    ElMessage.warning(t('forgotPassword.messages.passwordIncomplete'))
    return
  }
  if (step3Form.newPassword !== step3Form.confirmPassword) {
    ElMessage.error(t('forgotPassword.messages.passwordMismatch'))
    return
  }

  const passwordMessage = getPasswordValidationMessage(step3Form.newPassword, t)
  if (passwordMessage) {
    ElMessage.error(passwordMessage)
    return
  }

  loading.value = true
  try {
    await publicApi.resetPassword({
      email: step1Form.email.trim(),
      code: step2Form.code.trim(),
      newPassword: step3Form.newPassword,
      confirmPassword: step3Form.confirmPassword
    })
    ElMessage.success(t('forgotPassword.messages.resetSuccess'))
    step.value = 4
  } catch (error) {
    ElMessage.error(error.message || t('forgotPassword.messages.resetFailed'))
  } finally {
    loading.value = false
  }
}

watch(() => step3Form.newPassword, (value) => {
  const sanitized = sanitizePasswordInput(value)
  if (sanitized !== value) {
    step3Form.newPassword = sanitized
  }
})

watch(() => step3Form.confirmPassword, (value) => {
  const sanitized = sanitizePasswordInput(value)
  if (sanitized !== value) {
    step3Form.confirmPassword = sanitized
  }
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
  padding: 24px;
  background: var(--gradient-bg);
}

.auth-card {
  width: min(420px, 100%);
  padding: 32px;
  border-radius: 24px;
  background: var(--bg-glass);
  box-shadow: var(--shadow-xl);
  border: 1px solid var(--border-color);
}

.auth-header {
  text-align: center;
  margin-bottom: 24px;
}

.auth-header h1 {
  margin: 0 0 8px;
  color: var(--text-primary);
}

.auth-header p {
  margin: 0;
  color: var(--text-secondary);
}

.password-rules {
  width: 100%;
  margin-top: 10px;
  display: grid;
  gap: 6px;
}

.password-rule {
  font-size: 0.84rem;
  color: var(--text-secondary);
  transition: color 0.2s ease;
}

.password-rule.met {
  color: var(--accent);
}

.submit-btn {
  width: 100%;
}

.helper-row {
  display: flex;
  justify-content: space-between;
  margin-top: 16px;
}

.text-btn,
.link-btn {
  color: var(--accent);
  background: none;
  border: 0;
  text-decoration: none;
  cursor: pointer;
  padding: 0;
  transition: color 0.25s ease;
}

.text-btn:hover,
.link-btn:hover {
  color: var(--accent-secondary);
}

.success-block {
  text-align: center;
}

.success-block h2 {
  margin-bottom: 8px;
  color: var(--text-primary);
}

.success-block p {
  color: var(--text-secondary);
}
</style>
