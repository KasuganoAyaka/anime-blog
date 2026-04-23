<template>
  <div class="user-menu-wrapper" ref="wrapperRef">
    <button class="avatar-trigger" @click.stop="toggleMenu">
      <img
        v-if="userStore.isLoggedIn && userStore.user?.avatar"
        :src="userStore.user.avatar"
        class="avatar-img"
      />
      <div v-else class="avatar-placeholder">
        <svg viewBox="0 0 24 24" fill="none">
          <circle cx="12" cy="8" r="4" stroke="currentColor" stroke-width="1.5" />
          <path d="M4 20c0-4 4-6 8-6s8 2 8 6" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" />
        </svg>
      </div>
    </button>

    <transition name="menu-pop">
      <div v-if="menuVisible" class="user-menu" @click.stop>
        <div class="menu-user-info">
          <div class="menu-avatar">
            <img
              v-if="userStore.isLoggedIn && userStore.user?.avatar"
              :src="userStore.user.avatar"
              class="menu-avatar-img"
            />
            <div v-else class="menu-avatar-placeholder">
              <svg viewBox="0 0 24 24" fill="none">
                <circle cx="12" cy="8" r="4" stroke="currentColor" stroke-width="1.5" />
                <path d="M4 20c0-4 4-6 8-6s8 2 8 6" stroke="currentColor" stroke-width="1.5" />
              </svg>
            </div>
          </div>
          <div class="menu-user-text">
            <div class="menu-username">
              {{ userStore.isLoggedIn ? (userStore.user?.nickname || userStore.user?.username) : t('common.guest') }}
            </div>
            <div :class="['menu-status', userStore.isLoggedIn ? 'online' : 'offline']">
              <span class="status-dot"></span>
              {{ userStore.isLoggedIn ? t('common.loggedIn') : t('common.guest') }}
            </div>
          </div>
        </div>

        <div class="menu-divider"></div>

        <div class="menu-row">
          <span class="row-icon">🌐</span>
          <span class="row-label">{{ t('common.language') }}</span>
          <div class="lang-group">
            <button
              v-for="option in langStore.languages"
              :key="option.code"
              :class="['lang-btn', { active: langStore.lang === option.code }]"
              @click="langStore.setLang(option.code)"
            >
              {{ t(`common.languages.${option.code}`) }}
            </button>
          </div>
        </div>

        <div class="menu-divider"></div>

        <div class="menu-row clickable" @click="themeStore.cycleTheme()">
          <span class="row-icon">{{ themeStore.themeIcon }}</span>
          <span class="row-label">{{ t(`theme.${themeStore.theme}`) }}</span>
          <div class="theme-group">
            <span :class="['theme-dot', { active: themeStore.theme === 'light' }]">☀️</span>
            <span :class="['theme-dot', { active: themeStore.theme === 'dark' }]">🌙</span>
            <span :class="['theme-dot', { active: themeStore.theme === 'system' }]">💻</span>
          </div>
        </div>

        <div class="menu-divider"></div>

        <div class="menu-row clickable auth-row" @click="handleAuth">
          <span class="row-icon">{{ userStore.isLoggedIn ? '🔐' : '🔑' }}</span>
          <span class="row-label" :class="userStore.isLoggedIn ? 'logout-text' : 'login-text'">
            {{ userStore.isLoggedIn ? t('userMenu.logout') : t('userMenu.login') }}
          </span>
        </div>

        <div v-if="userStore.isLoggedIn" class="menu-row clickable admin-row" @click="goToAdmin">
          <span class="row-icon">🔨</span>
          <span class="row-label">
            {{ isAdminUser ? t('userMenu.enterAdmin') : t('userMenu.enterWorkspace') }}
          </span>
        </div>
      </div>
    </transition>
  </div>
</template>

<script setup>
import { computed, ref, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useUserStore, useThemeStore, useLangStore } from '@/stores'

const router = useRouter()
const userStore = useUserStore()
const themeStore = useThemeStore()
const langStore = useLangStore()
const { t } = useI18n()

const menuVisible = ref(false)
const wrapperRef = ref(null)
const isAdminUser = computed(() => ['admin', 'manager'].includes(userStore.user?.role))

const toggleMenu = () => {
  menuVisible.value = !menuVisible.value
}

const handleAuth = async () => {
  menuVisible.value = false
  if (userStore.isLoggedIn) {
    try {
      await ElMessageBox.confirm(t('userMenu.confirmLogout'), t('userMenu.logoutTitle'), {
        confirmButtonText: t('common.submit'),
        cancelButtonText: t('common.cancel'),
        type: 'warning'
      })
      await userStore.logout()
      ElMessage.success(t('userMenu.logoutSuccess'))
    } catch {}
    return
  }

  router.push('/login')
}

const goToAdmin = () => {
  menuVisible.value = false
  router.push('/admin')
}

const handleClickOutside = (event) => {
  if (wrapperRef.value && !wrapperRef.value.contains(event.target)) {
    menuVisible.value = false
  }
}

onMounted(() => document.addEventListener('click', handleClickOutside))
onUnmounted(() => document.removeEventListener('click', handleClickOutside))
</script>

<style scoped>
.user-menu-wrapper {
  position: relative;
}

.avatar-trigger {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  border: 2px solid var(--border-color);
  background: var(--bg-tertiary);
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
  transition: all 0.3s ease;
  padding: 0;
}

.avatar-trigger:hover {
  border-color: var(--accent);
  box-shadow: 0 0 0 4px var(--accent-light);
  transform: scale(1.05);
}

.avatar-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.avatar-placeholder {
  width: 22px;
  height: 22px;
  color: var(--text-tertiary);
}

.avatar-placeholder svg {
  width: 100%;
  height: 100%;
}

.user-menu {
  position: absolute;
  top: calc(100% + 12px);
  right: 0;
  width: 280px;
  background: var(--bg-glass);
  backdrop-filter: blur(20px) saturate(180%);
  -webkit-backdrop-filter: blur(20px) saturate(180%);
  border: 1px solid var(--border-color);
  border-radius: 20px;
  box-shadow: var(--shadow-xl);
  z-index: 9999;
  overflow: hidden;
}

.menu-user-info {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 20px;
}

.menu-avatar {
  width: 48px;
  height: 48px;
  border-radius: 50%;
  background: var(--bg-tertiary);
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
  flex-shrink: 0;
  border: 2px solid var(--accent);
}

.menu-avatar-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.menu-avatar-placeholder {
  width: 26px;
  height: 26px;
  color: var(--text-tertiary);
}

.menu-avatar-placeholder svg {
  width: 100%;
  height: 100%;
}

.menu-username {
  font-weight: 600;
  color: var(--text-primary);
  font-size: 1rem;
}

.menu-status {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 0.8rem;
  margin-top: 4px;
}

.status-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
}

.menu-status.online {
  color: var(--accent);
}

.menu-status.online .status-dot {
  background: var(--accent);
}

.menu-status.offline {
  color: var(--text-tertiary);
}

.menu-status.offline .status-dot {
  background: var(--text-tertiary);
}

.menu-divider {
  height: 1px;
  background: var(--border-light);
}

.menu-row {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 14px 20px;
}

.menu-row.clickable {
  cursor: pointer;
  transition: background 0.2s ease;
}

.menu-row.clickable:hover {
  background: var(--accent-light);
}

.row-icon {
  font-size: 0.85rem;
  min-width: 24px;
  text-align: center;
  letter-spacing: 0.04em;
}

.row-label {
  flex: 1;
  color: var(--text-primary);
  font-size: 0.9rem;
}

.lang-group {
  display: flex;
  gap: 6px;
}

.lang-btn {
  padding: 4px 10px;
  border: 1px solid var(--border-color);
  border-radius: 8px;
  background: transparent;
  color: var(--text-secondary);
  font-size: 0.75rem;
  cursor: pointer;
  transition: all 0.2s ease;
}

.lang-btn.active {
  background: var(--accent);
  color: #fff;
  border-color: var(--accent);
}

.lang-btn:hover:not(.active) {
  border-color: var(--accent);
  color: var(--accent);
}

.theme-group {
  display: flex;
  gap: 6px;
}

.theme-dot {
  font-size: 0.9rem;
  opacity: 0.3;
  transition: all 0.25s ease;
}

.theme-dot.active {
  opacity: 1;
  transform: scale(1.15);
}

.login-text {
  color: var(--accent);
  font-weight: 500;
}

.logout-text {
  color: #ff6b6b;
  font-weight: 500;
}

.admin-row {
  border-top: 1px solid var(--border-light);
  background: var(--accent-light);
}

.admin-row .row-label {
  color: var(--accent);
  font-weight: 500;
}

.menu-pop-enter-active {
  animation: menuPop 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

.menu-pop-leave-active {
  animation: menuPop 0.2s ease-out reverse;
}

@keyframes menuPop {
  from {
    opacity: 0;
    transform: translateY(-10px) scale(0.95);
  }
  to {
    opacity: 1;
    transform: translateY(0) scale(1);
  }
}
</style>
