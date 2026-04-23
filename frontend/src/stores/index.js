import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { LANG_STORAGE_KEY, defaultLocale, setI18nLanguage } from '@/i18n'
import { authApi } from '@/api'
import {
  clearAuthSession,
  readAuthSession,
  saveAuthSession,
  updateAuthSession
} from '@/utils/session'

export const useLangStore = defineStore('lang', () => {
  const lang = ref(localStorage.getItem(LANG_STORAGE_KEY) || defaultLocale)
  const languages = [
    { code: 'zh-CN', label: '简中' },
    { code: 'zh-TW', label: '繁中' },
    { code: 'en', label: 'EN' }
  ]

  const setLang = (newLang) => {
    lang.value = setI18nLanguage(newLang)
    window.dispatchEvent(new Event('lang-changed'))
  }

  const initLang = () => {
    lang.value = setI18nLanguage(lang.value)
  }

  return { lang, languages, setLang, initLang }
})

export const useUserStore = defineStore('user', () => {
  const initialSession = readAuthSession()
  const rememberMe = ref(initialSession.rememberMe)
  const user = ref(initialSession.user)
  const token = ref(initialSession.token || '')
  const refreshToken = ref(initialSession.refreshToken || '')

  const isLoggedIn = computed(() => !!user.value && (!!token.value || !!refreshToken.value))

  const hydrateSession = () => {
    const session = readAuthSession()
    rememberMe.value = session.rememberMe
    user.value = session.user
    token.value = session.token || ''
    refreshToken.value = session.refreshToken || ''
  }

  const persistSession = (remember = rememberMe.value) => {
    rememberMe.value = remember
    saveAuthSession({
      token: token.value,
      refreshToken: refreshToken.value,
      user: user.value,
      rememberMe: remember
    })
  }

  const login = (userData, authData = {}, remember = false) => {
    user.value = userData
    token.value = authData.accessToken || ''
    refreshToken.value = authData.refreshToken || ''
    persistSession(remember)
  }

  const updateUser = (userData) => {
    user.value = userData
    updateAuthSession({ user: user.value })
  }

  const updateAccessToken = (nextToken) => {
    token.value = nextToken || ''
    updateAuthSession({ token: token.value })
  }

  const updateRefreshToken = (nextRefreshToken) => {
    refreshToken.value = nextRefreshToken || ''
    updateAuthSession({ refreshToken: refreshToken.value })
  }

  const logout = async ({ remote = true } = {}) => {
    const currentRefreshToken = refreshToken.value

    if (remote && (token.value || currentRefreshToken)) {
      try {
        await authApi.logout(currentRefreshToken)
      } catch {
      }
    }

    user.value = null
    token.value = ''
    refreshToken.value = ''
    rememberMe.value = false
    clearAuthSession()
  }

  const checkSession = () => {
    hydrateSession()

    if (!user.value || (!token.value && !refreshToken.value)) {
      user.value = null
      token.value = ''
      refreshToken.value = ''
      rememberMe.value = false
      clearAuthSession()
      return false
    }

    return true
  }

  return {
    user,
    token,
    refreshToken,
    isLoggedIn,
    rememberMe,
    hydrateSession,
    login,
    updateUser,
    updateAccessToken,
    updateRefreshToken,
    logout,
    checkSession
  }
})

export const useThemeStore = defineStore('theme', () => {
  const theme = ref(localStorage.getItem('theme') || 'system')

  const systemIsDark = () => window.matchMedia('(prefers-color-scheme: dark)').matches

  const isDark = computed(() => {
    if (theme.value === 'dark') return true
    if (theme.value === 'light') return false
    return systemIsDark()
  })

  const themeIcon = computed(() => {
    if (theme.value === 'dark') return '🌙'
    if (theme.value === 'light') return '☀️'
    return '💻'
  })

  const themeLabel = computed(() => {
    if (theme.value === 'dark') return '深色主题'
    if (theme.value === 'light') return '浅色主题'
    return '系统主题'
  })

  const cycleTheme = () => {
    if (theme.value === 'system') theme.value = 'dark'
    else if (theme.value === 'dark') theme.value = 'light'
    else theme.value = 'system'
    localStorage.setItem('theme', theme.value)
    applyTheme()
  }

  const applyTheme = () => {
    if (isDark.value) {
      document.documentElement.classList.add('dark')
    } else {
      document.documentElement.classList.remove('dark')
    }
    window.dispatchEvent(new Event('theme-changed'))
  }

  const initTheme = () => {
    applyTheme()
    window.matchMedia('(prefers-color-scheme: dark)').addEventListener('change', () => {
      if (theme.value === 'system') applyTheme()
    })
  }

  return { theme, isDark, themeIcon, themeLabel, cycleTheme, initTheme, applyTheme }
})

export const useUiStore = defineStore('ui', () => {
  const musicPlayerVisible = ref(false)

  const openMusicPlayer = () => {
    musicPlayerVisible.value = true
  }

  const closeMusicPlayer = () => {
    musicPlayerVisible.value = false
  }

  const toggleMusicPlayer = () => {
    musicPlayerVisible.value = !musicPlayerVisible.value
  }

  return {
    musicPlayerVisible,
    openMusicPlayer,
    closeMusicPlayer,
    toggleMusicPlayer
  }
})
