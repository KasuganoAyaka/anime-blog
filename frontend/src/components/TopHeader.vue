<template>
  <header
    ref="headerRef"
    class="top-header"
    :class="{ dark: themeStore.isDark, compact: isCompact, elevated: isScrolled, 'post-detail': isPostDetailRoute }"
  >
    <div class="header-inner">
      <div class="header-left">
        <router-link to="/" class="logo" @click="handleLogoClick">
          <img class="logo-icon" src="/logo.png" :alt="t('common.appName')" />
          <span class="logo-text">{{ t('common.appName') }}</span>
        </router-link>
      </div>

      <div class="header-center">
        <transition name="center-switch" mode="out-in">
          <button
            v-if="showPostTitleNav"
            key="post-title-nav"
            type="button"
            class="post-title-nav"
            :class="{ compact: isCompact }"
            :title="postTitleNavText"
            @click="scrollToPostHeader"
          >
            <span class="post-title-nav-text">{{ postTitleNavText }}</span>
            <span v-if="isCompact" class="post-title-helper">{{ t('topHeader.backToTopHint') }}</span>
          </button>
          <nav
            v-else
            key="main-nav"
            class="nav"
            :class="{ compact: isCompact }"
            :aria-label="t('topHeader.home')"
          >
            <button
              v-for="item in navItems"
              :key="item.path"
              type="button"
              class="nav-item"
              :class="{
                active: activeNav.path === item.path,
                compact: isCompact && activeNav.path === item.path,
                'is-collapsed': isCompact && activeNav.path !== item.path
              }"
              :aria-hidden="isCompact && activeNav.path !== item.path ? 'true' : undefined"
              :tabindex="isCompact && activeNav.path !== item.path ? -1 : undefined"
              @click="handleNavClick(item)"
            >
              <span class="nav-label">{{ item.label }}</span>
              <span v-if="isCompact && activeNav.path === item.path" class="nav-helper">{{ t('topHeader.backToTopHint') }}</span>
            </button>
          </nav>
        </transition>
      </div>

      <div class="header-right">
        <button
          class="icon-btn search-btn"
          :class="{ active: searchVisible }"
          type="button"
          :title="searchButtonTitle"
          @click="searchVisible = true"
        >
          <svg viewBox="0 0 24 24" aria-hidden="true">
            <circle cx="11" cy="11" r="7" />
            <path d="M20 20l-3.5-3.5" />
          </svg>
        </button>
        <button class="icon-btn music-btn" type="button" :title="t('topHeader.music')" @click="$emit('toggle-music')">
          <span class="btn-icon">🎵</span>
        </button>
        <button
          class="icon-btn top-btn"
          :class="{ active: isScrolled }"
          type="button"
          :title="t('topHeader.backToTop')"
          @click="scrollToTop"
        >
          <svg viewBox="0 0 24 24" aria-hidden="true">
            <path d="M12 5l-7 7m7-7l7 7M12 5v14" />
          </svg>
        </button>
        <UserMenu />
      </div>
    </div>
  </header>
  <SearchOverlay v-model="searchVisible" />
</template>

<script setup>
import { computed, nextTick, onMounted, onUnmounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { useThemeStore } from '@/stores'
import SearchOverlay from './SearchOverlay.vue'
import UserMenu from './UserMenu.vue'

const route = useRoute()
const router = useRouter()
const themeStore = useThemeStore()
const { t, locale } = useI18n()

const navItems = computed(() => ([
  { path: '/', label: t('topHeader.home') },
  { path: '/tags', label: t('topHeader.tags') },
  { path: '/categories', label: t('topHeader.categories') },
  { path: '/about', label: t('topHeader.about') }
]))

const isScrolled = ref(false)
const isCompact = ref(false)
const searchVisible = ref(false)
const postTitleNavText = ref('')
const showPostTitleNav = ref(false)
const headerRef = ref(null)

const searchButtonTitleMap = {
  'zh-CN': '搜索',
  'zh-TW': '搜尋',
  en: 'Search'
}

const searchButtonTitle = computed(() => searchButtonTitleMap[locale.value] || searchButtonTitleMap['zh-CN'])

const activeNav = computed(() => {
  if (route.path.startsWith('/tags')) return navItems.value[1]
  if (route.path.startsWith('/categories')) return navItems.value[2]
  if (route.path.startsWith('/about')) return navItems.value[3]
  return navItems.value[0]
})

const isPostDetailRoute = computed(() => route.path.startsWith('/post/'))

let scrollSyncFrame = 0
let routeSyncTimers = []
let cachedFirstArticle = null
let cachedPostHeader = null
let cachedPostTitle = null

const getHeaderHeight = () => Math.round(headerRef.value?.getBoundingClientRect().height || 72)

const refreshTrackedElements = () => {
  cachedFirstArticle = document.querySelector('.article-card, .post-card')
  cachedPostHeader = document.querySelector('.post-header')
  cachedPostTitle = cachedPostHeader?.querySelector('h1') || null
}

const ensureTrackedElements = () => {
  if (!cachedFirstArticle || (isPostDetailRoute.value && (!cachedPostHeader || !cachedPostTitle))) {
    refreshTrackedElements()
  }
}

const measureHeaderState = () => {
  ensureTrackedElements()

  const scrollTop = window.scrollY || document.documentElement.scrollTop || 0
  const headerHeight = getHeaderHeight()
  const nextScrolled = scrollTop > 12
  let nextPostTitle = ''
  let showPostTitle = false
  let nextCompact = scrollTop > (isCompact.value ? 132 : 168)

  if (isPostDetailRoute.value && cachedPostTitle) {
    const titleRect = cachedPostTitle.getBoundingClientRect()
    nextPostTitle = cachedPostTitle.textContent?.trim() || ''
    showPostTitle = titleRect.bottom <= headerHeight + 12 && Boolean(nextPostTitle)
  }

  if (showPostTitle) {
    nextCompact = true
  } else if (cachedFirstArticle) {
    const rect = cachedFirstArticle.getBoundingClientRect()
    const midpoint = rect.top + rect.height / 2
    const triggerOffset = isCompact.value ? 54 : 24
    nextCompact = midpoint <= headerHeight + triggerOffset
  }

  return {
    nextScrolled,
    nextCompact,
    nextPostTitle,
    showPostTitle
  }
}

const syncHeaderState = () => {
  const { nextScrolled, nextCompact, nextPostTitle, showPostTitle } = measureHeaderState()
  isScrolled.value = nextScrolled
  isCompact.value = nextCompact
  postTitleNavText.value = nextPostTitle
  showPostTitleNav.value = showPostTitle
}

const queueHeaderStateSync = () => {
  if (scrollSyncFrame) {
    return
  }

  scrollSyncFrame = window.requestAnimationFrame(() => {
    scrollSyncFrame = 0
    syncHeaderState()
  })
}

const clearRouteSyncTimers = () => {
  routeSyncTimers.forEach((timer) => window.clearTimeout(timer))
  routeSyncTimers = []
}

const scheduleRouteStateSync = async () => {
  await nextTick()
  refreshTrackedElements()
  queueHeaderStateSync()

  ;[120, 360].forEach((delay) => {
    const timer = window.setTimeout(() => {
      refreshTrackedElements()
      queueHeaderStateSync()
    }, delay)
    routeSyncTimers.push(timer)
  })
}

const scrollToTop = () => {
  window.scrollTo({ top: 0, behavior: 'smooth' })
}

const scrollToPostHeader = () => {
  if (!cachedPostHeader) {
    refreshTrackedElements()
  }

  const titleEl = cachedPostHeader

  if (!titleEl) {
    scrollToTop()
    return
  }

  const headerHeight = getHeaderHeight()
  const targetTop = window.scrollY + titleEl.getBoundingClientRect().top - headerHeight - 14

  window.scrollTo({
    top: Math.max(0, targetTop),
    behavior: 'smooth'
  })
}

const handleNavClick = async (item) => {
  if (route.path === item.path) {
    scrollToTop()
    return
  }

  await router.push(item.path)
  requestAnimationFrame(scrollToTop)
}

const handleLogoClick = (event) => {
  if (route.path === '/') {
    event.preventDefault()
    scrollToTop()
  }
}

watch(
  () => route.fullPath,
  () => {
    searchVisible.value = false
    clearRouteSyncTimers()
    void scheduleRouteStateSync()
  }
)

onMounted(() => {
  refreshTrackedElements()
  syncHeaderState()
  window.addEventListener('scroll', queueHeaderStateSync, { passive: true })
  window.addEventListener('resize', queueHeaderStateSync, { passive: true })
})

onUnmounted(() => {
  clearRouteSyncTimers()
  window.removeEventListener('scroll', queueHeaderStateSync)
  window.removeEventListener('resize', queueHeaderStateSync)
  if (scrollSyncFrame) {
    window.cancelAnimationFrame(scrollSyncFrame)
    scrollSyncFrame = 0
  }
})
</script>

<style scoped>
.top-header {
  position: sticky;
  top: 0;
  z-index: 100;
  background: color-mix(in srgb, var(--bg-glass) 88%, transparent);
  backdrop-filter: blur(20px) saturate(180%);
  -webkit-backdrop-filter: blur(20px) saturate(180%);
  border-bottom: 1px solid transparent;
  transition: background 0.3s ease, border-color 0.3s ease, box-shadow 0.3s ease;
}

.top-header.elevated {
  border-bottom-color: var(--border-color);
  box-shadow: 0 12px 32px rgba(15, 23, 42, 0.08);
}

.top-header.compact {
  background: transparent;
  backdrop-filter: none;
  -webkit-backdrop-filter: none;
  border-bottom-color: transparent;
  box-shadow: none;
}

.header-inner {
  max-width: 1200px;
  margin: 0 auto;
  padding: 0 24px;
  min-height: 72px;
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto minmax(0, 1fr);
  align-items: center;
  gap: 18px;
}

.top-header.post-detail.compact .header-inner {
  max-width: 1280px;
  padding: 0 16px;
}

.header-left {
  display: flex;
  justify-content: flex-start;
  min-width: 0;
  transition: transform 0.32s cubic-bezier(0.22, 1, 0.36, 1), opacity 0.24s ease;
}

.header-center {
  display: flex;
  justify-content: center;
  min-width: 0;
}

.header-center > * {
  min-width: 0;
}

.header-right {
  --header-right-shift-x: 0px;
  --header-right-shift-y: 0px;
  display: flex;
  justify-content: flex-end;
  align-items: center;
  gap: 10px;
  transition:
    transform 0.32s cubic-bezier(0.22, 1, 0.36, 1),
    opacity 0.24s ease,
    gap 0.28s ease;
}

.top-header.compact .header-left,
.top-header.compact .header-right {
  transform: translateY(1px);
}

.top-header.post-detail.compact .header-left {
  transform: translate(-85px, 1px);
}

.top-header.post-detail.compact .header-right {
  --header-right-shift-x: 22px;
}

.top-header.compact .header-right {
  --header-right-shift-y: 1px;
  gap: 8px;
  padding: 5px 8px;
  width: fit-content;
  justify-self: end;
  justify-content: flex-start;
  border-radius: 999px;
  background: color-mix(in srgb, var(--bg-card) 84%, transparent);
  border: 1px solid transparent;
  box-shadow: 0 8px 18px rgba(15, 23, 42, 0.06);
  backdrop-filter: blur(18px) saturate(180%);
  -webkit-backdrop-filter: blur(18px) saturate(180%);
  transform: translate(var(--header-right-shift-x), var(--header-right-shift-y));
  transition:
    transform 0.24s ease,
    box-shadow 0.24s ease,
    background 0.24s ease,
    border-color 0.24s ease,
    padding 0.28s cubic-bezier(0.22, 1, 0.36, 1),
    gap 0.28s ease;
}

.top-header.compact .header-right:hover {
  transform: translate(var(--header-right-shift-x), calc(var(--header-right-shift-y) - 2px));
  box-shadow: 0 12px 26px rgba(15, 23, 42, 0.1);
}

.top-header.compact .icon-btn {
  width: 38px;
  height: 38px;
  border-radius: 13px;
}

.top-header.compact .icon-btn svg {
  width: 17px;
  height: 17px;
}

.top-header.compact .btn-icon {
  font-size: 1.04rem;
}

.top-header.compact :deep(.avatar-trigger) {
  width: 38px;
  height: 38px;
  border-width: 1.5px;
}

.top-header.compact :deep(.avatar-placeholder) {
  width: 21px;
  height: 21px;
}

.logo {
  display: inline-flex;
  align-items: center;
  gap: 12px;
  font-size: 1.34rem;
  font-weight: 700;
  color: var(--text-primary);
  text-decoration: none;
  transition:
    transform 0.3s cubic-bezier(0.22, 1, 0.36, 1),
    gap 0.3s cubic-bezier(0.22, 1, 0.36, 1),
    padding 0.3s cubic-bezier(0.22, 1, 0.36, 1),
    border-radius 0.3s cubic-bezier(0.22, 1, 0.36, 1),
    background 0.24s ease,
    border-color 0.24s ease,
    box-shadow 0.28s ease;
}

.top-header.compact .logo {
  gap: 0;
  min-width: 0;
  padding: 4px;
  border-radius: 18px;
  background: color-mix(in srgb, var(--bg-card) 84%, transparent);
  border: 1px solid transparent;
  box-shadow: 0 10px 24px rgba(15, 23, 42, 0.06);
  backdrop-filter: blur(18px) saturate(180%);
  -webkit-backdrop-filter: blur(18px) saturate(180%);
  overflow: hidden;
  transform-origin: left center;
}

.top-header.compact .logo:hover,
.top-header.compact .logo:focus-visible {
  gap: 10px;
  padding: 7px 16px 7px 11px;
  border-radius: 999px;
}

.logo:hover {
  transform: translateY(-1px);
  gap: 15px;
}

.logo-icon {
  width: 46px;
  height: 46px;
  border-radius: 16px;
  object-fit: cover;
  border: 2px solid var(--accent);
  box-shadow: 0 10px 24px rgba(57, 197, 187, 0.18);
  transition: transform 0.28s cubic-bezier(0.22, 1, 0.36, 1), box-shadow 0.28s ease;
}

.logo-text {
  background: var(--gradient-primary);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
  white-space: nowrap;
  transition:
    transform 0.28s cubic-bezier(0.22, 1, 0.36, 1),
    opacity 0.22s ease,
    max-width 0.28s cubic-bezier(0.22, 1, 0.36, 1),
    margin 0.28s cubic-bezier(0.22, 1, 0.36, 1);
}

.logo:hover .logo-icon {
  transform: translateX(-2px) rotate(-3deg) scale(1.03);
  box-shadow: 0 13px 26px rgba(57, 197, 187, 0.24);
}

.logo:hover .logo-text {
  transform: translateX(2px);
}

.top-header.compact .logo-icon {
  transform: translateX(-3px);
}

.top-header.compact .logo-text {
  max-width: 0;
  margin-left: -4px;
  opacity: 0;
  overflow: hidden;
  transform: translateX(-8px);
  pointer-events: none;
}

.top-header.compact .logo:hover .logo-text,
.top-header.compact .logo:focus-visible .logo-text {
  max-width: 220px;
  margin-left: 0;
  opacity: 1;
  transform: translateX(0);
}

.top-header.compact .logo:hover .logo-icon,
.top-header.compact .logo:focus-visible .logo-icon {
  transform: translateX(-4px) rotate(-3deg) scale(1.03);
}

.nav {
  display: inline-flex;
  align-items: center;
  position: relative;
  gap: 10px;
  padding: 8px;
  border-radius: 999px;
  background: color-mix(in srgb, var(--bg-card) 82%, transparent);
  border: 1px solid color-mix(in srgb, var(--border-color) 80%, transparent);
  box-shadow: 0 10px 24px rgba(15, 23, 42, 0.06);
  overflow: hidden;
  transition:
    padding 0.18s cubic-bezier(0.22, 1, 0.36, 1),
    gap 0.18s cubic-bezier(0.22, 1, 0.36, 1),
    box-shadow 0.12s ease,
    border-color 0.12s ease;
}

.post-title-nav {
  max-width: min(520px, 100%);
  border: 1px solid color-mix(in srgb, var(--border-color) 80%, transparent);
  background: color-mix(in srgb, var(--bg-card) 82%, transparent);
  color: var(--text-primary);
  font: inherit;
  font-weight: 700;
  padding: 10px 20px;
  border-radius: 999px;
  cursor: pointer;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 12px;
  min-width: 0;
  box-shadow: 0 10px 24px rgba(15, 23, 42, 0.06);
  transition:
    transform 0.26s cubic-bezier(0.22, 1, 0.36, 1),
    padding 0.26s cubic-bezier(0.22, 1, 0.36, 1),
    background 0.24s ease,
    border-color 0.24s ease,
    box-shadow 0.24s ease;
}

.post-title-nav:hover {
  transform: translateY(-1px);
  background: color-mix(in srgb, var(--accent-light) 72%, transparent);
  border-color: color-mix(in srgb, var(--accent) 26%, var(--border-color));
  box-shadow: 0 12px 26px rgba(15, 23, 42, 0.08);
}

.post-title-nav.compact {
  padding: 9px 16px;
}

.post-title-nav-text {
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.post-title-helper {
  flex-shrink: 0;
  font-size: 0.78rem;
  opacity: 0.82;
  padding-left: 10px;
  border-left: 1px solid color-mix(in srgb, var(--text-secondary) 26%, transparent);
}

.center-switch-enter-active,
.center-switch-leave-active {
  transition:
    opacity 0.22s ease,
    transform 0.3s cubic-bezier(0.22, 1, 0.36, 1),
    filter 0.24s ease;
}

.center-switch-enter-from {
  opacity: 0;
  transform: translateY(8px) scale(0.985);
  filter: blur(4px);
}

.center-switch-leave-to {
  opacity: 0;
  transform: translateY(-8px) scale(0.985);
  filter: blur(4px);
}

.nav.compact {
  gap: 0;
  padding: 6px;
}

.nav-item {
  border: none;
  background: transparent;
  color: var(--text-secondary);
  font: inherit;
  font-weight: 600;
  padding: 11px 18px;
  border-radius: 999px;
  cursor: pointer;
  display: inline-flex;
  flex: 0 0 auto;
  align-items: center;
  gap: 10px;
  min-width: 0;
  max-width: 140px;
  overflow: hidden;
  transform-origin: center;
  transition:
    color 0.16s ease,
    background 0.16s ease,
    transform 0.2s cubic-bezier(0.22, 1, 0.36, 1),
    padding 0.2s cubic-bezier(0.22, 1, 0.36, 1),
    max-width 0.2s cubic-bezier(0.22, 1, 0.36, 1),
    opacity 0.16s ease,
    margin 0.2s cubic-bezier(0.22, 1, 0.36, 1),
    filter 0.16s ease;
  white-space: nowrap;
}

.nav-label {
  transition:
    opacity 0.14s ease,
    transform 0.18s cubic-bezier(0.22, 1, 0.36, 1);
}

.nav-item:hover {
  color: var(--text-primary);
  background: color-mix(in srgb, var(--accent-light) 78%, transparent);
  transform: translateY(-1px);
}

.nav-item.active {
  color: #fff;
  background: linear-gradient(135deg, var(--accent), color-mix(in srgb, var(--accent) 72%, #ffffff));
  box-shadow: 0 10px 22px rgba(57, 197, 187, 0.24);
}

.nav-item.compact {
  padding: 10px 16px;
  max-width: 260px;
  overflow: hidden;
  justify-content: center;
}

.nav.compact .nav-item.is-collapsed {
  padding: 0;
  margin: 0;
  max-width: 0;
  opacity: 0;
  transform: translateX(-12px) scale(0.92);
  pointer-events: none;
}

.nav.compact .nav-item.is-collapsed .nav-label {
  opacity: 0;
  transform: translateX(-10px);
}

.nav-helper {
  font-size: 0.78rem;
  opacity: 0.82;
  padding-left: 10px;
  border-left: 1px solid rgba(255, 255, 255, 0.22);
  animation: navHelperFade 0.16s cubic-bezier(0.22, 1, 0.36, 1);
}

@keyframes navHelperFade {
  from {
    opacity: 0;
    transform: translateX(-6px);
  }

  to {
    opacity: 0.82;
    transform: translateX(0);
  }
}

.icon-btn {
  width: 42px;
  height: 42px;
  border-radius: 14px;
  border: 1px solid color-mix(in srgb, var(--border-color) 82%, transparent);
  background: color-mix(in srgb, var(--bg-card) 88%, transparent);
  color: var(--text-secondary);
  cursor: pointer;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  transition: transform 0.22s ease, background 0.22s ease, color 0.22s ease, border-color 0.22s ease;
}

.icon-btn svg {
  width: 18px;
  height: 18px;
  fill: none;
  stroke: currentColor;
  stroke-width: 1.8;
  stroke-linecap: round;
  stroke-linejoin: round;
}

.icon-btn:hover {
  transform: translateY(-1px);
  background: var(--accent-light);
  color: var(--accent);
  border-color: color-mix(in srgb, var(--accent) 36%, var(--border-color));
}

.search-btn.active {
  color: var(--accent);
  background: var(--accent-light);
  border-color: color-mix(in srgb, var(--accent) 34%, var(--border-color));
}

.btn-icon {
  font-size: 1.1rem;
}

.top-btn svg {
  width: 18px;
  height: 18px;
}

.top-btn.active {
  color: var(--accent);
  background: var(--accent-light);
  border-color: color-mix(in srgb, var(--accent) 34%, var(--border-color));
}

@media (max-width: 960px) {
  .header-inner {
    grid-template-columns: 1fr auto;
    grid-template-areas:
      'left right'
      'center center';
    gap: 12px 16px;
    padding: 14px 18px;
  }

  .top-header.post-detail.compact .header-inner {
    max-width: 1200px;
    padding: 14px 18px;
  }

  .header-left {
    grid-area: left;
  }

  .header-center {
    grid-area: center;
    justify-content: flex-start;
  }

  .header-right {
    grid-area: right;
  }

  .top-header.post-detail.compact .header-left,
  .top-header.post-detail.compact .header-right {
    transform: translateY(1px);
  }
}

@media (max-width: 768px) {
  .logo-text {
    font-size: 1.1rem;
  }

  .nav {
    width: 100%;
    justify-content: flex-start;
    overflow-x: auto;
    scrollbar-width: none;
  }

  .nav.compact {
    width: auto;
    max-width: 100%;
    overflow: hidden;
  }

  .post-title-nav {
    width: 100%;
    justify-content: flex-start;
  }

  .nav::-webkit-scrollbar {
    display: none;
  }

  .nav-item {
    padding: 10px 14px;
    font-size: 0.92rem;
  }
}

@media (max-width: 560px) {
  .header-inner {
    padding: 12px 14px;
  }

  .logo-text {
    display: none;
  }

  .header-right {
    gap: 8px;
  }

  .post-title-helper {
    display: none;
  }
}
</style>
