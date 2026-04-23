import { computed, onUnmounted, ref, watch } from 'vue'
import { authApi, profileApi } from '@/api'
import { getRequestErrorMessage } from '@/utils/request'
import { getPasswordRuleItems, getPasswordValidationMessage, sanitizePasswordInput } from '@/utils/passwordRules'

const MAX_AVATAR_SOURCE_SIZE = 5 * 1024 * 1024
const SUPPORTED_AVATAR_TYPES = ['image/png', 'image/jpeg', 'image/webp', 'image/gif']

export const useAdminProfile = ({ activeMenu, userStore, showAvatarDialog, ElMessage }) => {
  const savingProfile = ref(false)
  const savingPassword = ref(false)
  const savingEmail = ref(false)
  const sendingCode = ref(false)
  const profileForm = ref({ nickname: '', bio: '', avatar: '' })
  const passwordForm = ref({ oldPassword: '', newPassword: '', confirmPassword: '' })
  const emailForm = ref({ newEmail: '', code: '' })
  const emailCooldown = ref(0)
  const avatarUrl = ref('')
  const pendingAvatarFile = ref(null)
  const passwordRuleItems = computed(() => getPasswordRuleItems(passwordForm.value.newPassword))
  let emailCooldownTimer = null
  let avatarPreviewObjectUrl = ''

  const updateCurrentUser = (patch) => {
    if (!userStore.user) return

    userStore.updateUser({
      ...userStore.user,
      ...patch
    })
  }

  const revokeAvatarPreview = () => {
    if (avatarPreviewObjectUrl) {
      URL.revokeObjectURL(avatarPreviewObjectUrl)
      avatarPreviewObjectUrl = ''
    }
  }

  const initProfileForm = () => {
    revokeAvatarPreview()
    pendingAvatarFile.value = null
    profileForm.value = {
      nickname: userStore.user?.nickname || '',
      bio: userStore.user?.bio || '',
      avatar: userStore.user?.avatar || ''
    }
    avatarUrl.value = userStore.user?.avatar || ''
    emailForm.value = { newEmail: '', code: '' }
  }

  watch(activeMenu, (value) => {
    if (value === 'profile') {
      initProfileForm()
    }
  })

  watch(() => passwordForm.value.oldPassword, (value) => {
    const sanitized = sanitizePasswordInput(value)
    if (sanitized !== value) {
      passwordForm.value.oldPassword = sanitized
    }
  })

  watch(() => passwordForm.value.newPassword, (value) => {
    const sanitized = sanitizePasswordInput(value)
    if (sanitized !== value) {
      passwordForm.value.newPassword = sanitized
    }
  })

  watch(() => passwordForm.value.confirmPassword, (value) => {
    const sanitized = sanitizePasswordInput(value)
    if (sanitized !== value) {
      passwordForm.value.confirmPassword = sanitized
    }
  })

  const startEmailCooldown = () => {
    if (emailCooldownTimer) {
      clearInterval(emailCooldownTimer)
    }

    emailCooldown.value = 60
    emailCooldownTimer = setInterval(() => {
      emailCooldown.value -= 1
      if (emailCooldown.value <= 0) {
        clearInterval(emailCooldownTimer)
        emailCooldownTimer = null
      }
    }, 1000)
  }

  const stopEmailCooldown = () => {
    if (emailCooldownTimer) {
      clearInterval(emailCooldownTimer)
      emailCooldownTimer = null
    }

    emailCooldown.value = 0
  }

  const saveProfile = async () => {
    savingProfile.value = true
    try {
      await profileApi.updateBasic(profileForm.value)
      updateCurrentUser(profileForm.value)
      ElMessage.success('个人资料保存成功')
    } catch (error) {
      ElMessage.error(getRequestErrorMessage(error, '保存个人资料失败'))
    } finally {
      savingProfile.value = false
    }
  }

  const sendEmailCode = () => {
    const email = emailForm.value.newEmail?.trim()
    if (!email || !email.match(/^[A-Za-z0-9+_.-]+@(.+)$/)) {
      ElMessage.warning('请输入正确的邮箱地址')
      return
    }

    if (email === userStore.user?.email) {
      ElMessage.warning('新邮箱不能与当前邮箱相同')
      return
    }

    startEmailCooldown()
    ElMessage.success('验证码发送中，请留意邮箱')

    void authApi.sendEmailCode(email, 'change_email')
      .catch((error) => {
        stopEmailCooldown()
        ElMessage.error(getRequestErrorMessage(error, '发送验证码失败'))
      })
  }

  const changeEmail = async () => {
    if (!emailForm.value.newEmail) {
      ElMessage.warning('请输入新邮箱')
      return
    }
    if (!emailForm.value.code || emailForm.value.code.length !== 6) {
      ElMessage.warning('请输入 6 位验证码')
      return
    }

    savingEmail.value = true
    try {
      await profileApi.updateEmail({
        newEmail: emailForm.value.newEmail,
        code: emailForm.value.code
      })
      updateCurrentUser({
        email: emailForm.value.newEmail,
        emailVerified: 1
      })
      ElMessage.success('邮箱更新成功')
      emailForm.value = { newEmail: '', code: '' }
    } catch (error) {
      ElMessage.error(getRequestErrorMessage(error, '更新邮箱失败'))
    } finally {
      savingEmail.value = false
    }
  }

  const saveAvatar = async () => {
    if (!avatarUrl.value) {
      ElMessage.warning('请填写头像地址或上传本地图片')
      return
    }

    savingProfile.value = true
    try {
      let nextAvatarUrl = avatarUrl.value

      if (pendingAvatarFile.value) {
        const formData = new FormData()
        formData.append('avatar', pendingAvatarFile.value)
        const res = await profileApi.uploadAvatar(formData)
        nextAvatarUrl = res.data?.url || ''
        if (!nextAvatarUrl) {
          throw new Error('未能获取头像地址')
        }
      } else {
        await profileApi.updateAvatar(avatarUrl.value)
      }

      revokeAvatarPreview()
      pendingAvatarFile.value = null
      avatarUrl.value = nextAvatarUrl
      profileForm.value.avatar = nextAvatarUrl
      updateCurrentUser({ avatar: nextAvatarUrl })
      showAvatarDialog.value = false
      ElMessage.success('头像更新成功')
    } catch (error) {
      ElMessage.error(getRequestErrorMessage(error, '保存头像失败'))
    } finally {
      savingProfile.value = false
    }
  }

  const loadAvatarFile = async (file) => {
    if (!file) return

    if (!SUPPORTED_AVATAR_TYPES.includes(file.type)) {
      ElMessage.warning('仅支持 PNG、JPG、WebP 和 GIF 图片')
      return
    }

    if (file.size > MAX_AVATAR_SOURCE_SIZE) {
      ElMessage.warning('请上传小于 5MB 的图片')
      return
    }

    revokeAvatarPreview()
    pendingAvatarFile.value = file
    avatarPreviewObjectUrl = URL.createObjectURL(file)
    avatarUrl.value = avatarPreviewObjectUrl
    ElMessage.success('本地图片已载入，保存后即可生效')
  }

  const changePassword = async () => {
    if (!passwordForm.value.oldPassword || !passwordForm.value.newPassword) {
      ElMessage.warning('请完整填写密码表单')
      return
    }
    if (passwordForm.value.newPassword !== passwordForm.value.confirmPassword) {
      ElMessage.warning('两次输入的密码不一致')
      return
    }

    const passwordMessage = getPasswordValidationMessage(passwordForm.value.newPassword)
    if (passwordMessage) {
      ElMessage.warning(passwordMessage)
      return
    }

    savingPassword.value = true
    try {
      await profileApi.updatePassword({
        oldPassword: passwordForm.value.oldPassword,
        newPassword: passwordForm.value.newPassword
      })
      ElMessage.success('密码更新成功')
      passwordForm.value = { oldPassword: '', newPassword: '', confirmPassword: '' }
    } catch (error) {
      ElMessage.error(getRequestErrorMessage(error, '更新密码失败'))
    } finally {
      savingPassword.value = false
    }
  }

  onUnmounted(() => {
    stopEmailCooldown()
    revokeAvatarPreview()
  })

  return {
    savingProfile,
    savingPassword,
    savingEmail,
    sendingCode,
    profileForm,
    passwordForm,
    emailForm,
    emailCooldown,
    avatarUrl,
    passwordRuleItems,
    initProfileForm,
    saveProfile,
    sendEmailCode,
    changeEmail,
    saveAvatar,
    loadAvatarFile,
    changePassword
  }
}
