<template>
  <div class="app-container" :class="{ dark: themeStore.isDark }">
    <el-config-provider :locale="elementLocale">
      <router-view v-slot="{ Component }">
        <div class="page-wrapper">
          <transition name="page-slide" mode="out-in">
            <component :is="Component" :key="routeViewKey" />
          </transition>
        </div>
      </router-view>
      <ChatWidget v-if="sessionReady && userStore.isLoggedIn" />
      <MusicPlayer v-model:visible="uiStore.musicPlayerVisible" />
    </el-config-provider>
  </div>
</template>

<script setup>
import { computed, defineAsyncComponent, onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import MusicPlayer from './components/MusicPlayer.vue'
import { ensureValidAccessToken } from './api'
import { elementLocales } from './i18n'
import { useThemeStore, useUserStore, useLangStore, useUiStore } from './stores'
import { syncCurrentProfile } from './utils/profile'

const ChatWidget = defineAsyncComponent(() => import('./components/ChatWidget.vue'))

const sessionReady = ref(false)
const route = useRoute()
const themeStore = useThemeStore()
const userStore = useUserStore()
const langStore = useLangStore()
const uiStore = useUiStore()
const elementLocale = computed(() => elementLocales[langStore.lang] || elementLocales['zh-CN'])
const routeViewKey = computed(() => {
  const params = route.params && typeof route.params === 'object' ? route.params : {}
  const serializedParams = Object.keys(params)
    .sort()
    .map((key) => `${key}:${Array.isArray(params[key]) ? params[key].join(',') : params[key]}`)
    .join('|')

  return serializedParams ? `${route.path}?${serializedParams}` : route.path
})

onMounted(async () => {
  langStore.initLang()
  themeStore.initTheme()

  if (!userStore.checkSession()) {
    sessionReady.value = true
    return
  }

  const isAuthorized = await ensureValidAccessToken()
  if (!isAuthorized) {
    await userStore.logout({ remote: false })
    sessionReady.value = true
    return
  }

  await syncCurrentProfile(userStore)
  sessionReady.value = true
})
</script>

<style>
* {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
}

html, body {
  min-height: 100vh;
}

body {
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Noto Sans SC', sans-serif;
  transition: background-color 0.4s ease, color 0.4s ease;
}

/* ===== 浅色主题（默认） ===== */
:root {
  --bg-primary: #f8fafc;
  --bg-secondary: #ffffff;
  --bg-tertiary: #f1f5f9;
  --bg-card: #ffffff;
  --bg-glass: rgba(255, 255, 255, 0.8);
  --text-primary: #1e293b;
  --text-secondary: #64748b;
  --text-tertiary: #94a3b8;
  --border-color: rgba(0, 0, 0, 0.06);
  --border-light: rgba(0, 0, 0, 0.04);
  --shadow-sm: 0 1px 3px rgba(0, 0, 0, 0.05);
  --shadow-md: 0 4px 12px rgba(0, 0, 0, 0.08);
  --shadow-lg: 0 12px 32px rgba(0, 0, 0, 0.12);
  --shadow-xl: 0 20px 50px rgba(0, 0, 0, 0.15);
  --accent: #39c5bb;
  --accent-light: rgba(57, 197, 187, 0.1);
  --accent-secondary: #667eea;
  --overlay-backdrop: rgba(148, 163, 184, 0.16);
  --message-box-bg: linear-gradient(180deg, rgba(255, 255, 255, 0.98), rgba(244, 248, 252, 0.96));
  --message-box-border: rgba(57, 197, 187, 0.18);
  --message-box-shadow: 0 30px 70px rgba(15, 23, 42, 0.18);
  --message-box-muted-bg: rgba(255, 255, 255, 0.7);
  --message-box-muted-border: rgba(148, 163, 184, 0.2);
  --message-box-danger: #d05d5d;
  --message-box-danger-soft: rgba(208, 93, 93, 0.12);
  --message-box-warning-bg: linear-gradient(180deg, rgba(255, 248, 244, 0.98), rgba(255, 240, 235, 0.97));
  --message-box-warning-border: rgba(208, 93, 93, 0.22);
  --message-box-warning-shadow: 0 30px 70px rgba(208, 93, 93, 0.14);
  --message-box-warning-muted-bg: rgba(255, 245, 240, 0.82);
  --message-box-warning-muted-border: rgba(208, 93, 93, 0.16);
  --gradient-primary: linear-gradient(135deg, #39c5bb, #667eea);
  --gradient-bg: linear-gradient(135deg, #f8fafc 0%, #e2e8f0 100%);
}

/* ===== 深色主题 ===== */
.dark {
  --bg-primary: #0f0f1a;
  --bg-secondary: #1a1a2e;
  --bg-tertiary: #252542;
  --bg-card: #1e1e36;
  --bg-glass: rgba(26, 26, 46, 0.85);
  --text-primary: #f1f5f9;
  --text-secondary: #a1a1aa;
  --text-tertiary: #71717a;
  --border-color: rgba(255, 255, 255, 0.08);
  --border-light: rgba(255, 255, 255, 0.04);
  --shadow-sm: 0 1px 3px rgba(0, 0, 0, 0.2);
  --shadow-md: 0 4px 12px rgba(0, 0, 0, 0.3);
  --shadow-lg: 0 12px 32px rgba(0, 0, 0, 0.4);
  --shadow-xl: 0 20px 50px rgba(0, 0, 0, 0.5);
  --accent: #4fd4cc;
  --accent-light: rgba(79, 212, 204, 0.15);
  --overlay-backdrop: rgba(15, 23, 42, 0.42);
  --message-box-bg: linear-gradient(180deg, rgba(31, 41, 55, 0.96), rgba(18, 24, 38, 0.98));
  --message-box-border: rgba(79, 212, 204, 0.2);
  --message-box-shadow: 0 34px 80px rgba(2, 6, 23, 0.48);
  --message-box-muted-bg: rgba(30, 41, 59, 0.72);
  --message-box-muted-border: rgba(148, 163, 184, 0.14);
  --message-box-danger: #f08a8a;
  --message-box-danger-soft: rgba(240, 138, 138, 0.16);
  --message-box-warning-bg: linear-gradient(180deg, rgba(61, 31, 30, 0.96), rgba(43, 23, 24, 0.98));
  --message-box-warning-border: rgba(240, 138, 138, 0.24);
  --message-box-warning-shadow: 0 34px 82px rgba(82, 24, 24, 0.38);
  --message-box-warning-muted-bg: rgba(74, 38, 39, 0.78);
  --message-box-warning-muted-border: rgba(240, 138, 138, 0.18);
  --gradient-primary: linear-gradient(135deg, #4fd4cc, #7789f0);
  --gradient-bg: linear-gradient(135deg, #0f0f1a 0%, #1a1a2e 100%);
}

body {
  background: var(--bg-primary);
  color: var(--text-primary);
}

.app-container {
  min-height: 100vh;
  background: var(--gradient-bg);
}

/* 页面包装器 - 确保动画期间背景色正确 */
.page-wrapper {
  min-height: 100vh;
  background: var(--gradient-bg);
}

/* ===== 页面切换动画 ===== */
.page-slide-enter-active {
  animation: pageSlideIn 0.4s cubic-bezier(0.4, 0, 0.2, 1);
}

.page-slide-leave-active {
  animation: pageSlideOut 0.3s ease-out;
}

@keyframes pageSlideIn {
  from {
    opacity: 0;
    transform: translateX(20px);
  }
  to {
    opacity: 1;
    transform: translateX(0);
  }
}

@keyframes pageSlideOut {
  from {
    opacity: 1;
    transform: translateX(0);
  }
  to {
    opacity: 0;
    transform: translateX(-15px);
  }
}

/* ===== 全局动画 ===== */
@keyframes fadeIn {
  from { opacity: 0; }
  to { opacity: 1; }
}

@keyframes fadeInUp {
  from { opacity: 0; transform: translateY(24px); }
  to { opacity: 1; transform: translateY(0); }
}

@keyframes fadeInDown {
  from { opacity: 0; transform: translateY(-24px); }
  to { opacity: 1; transform: translateY(0); }
}

@keyframes scaleIn {
  from { opacity: 0; transform: scale(0.92); }
  to { opacity: 1; transform: scale(1); }
}

@keyframes slideInRight {
  from { opacity: 0; transform: translateX(30px); }
  to { opacity: 1; transform: translateX(0); }
}

@keyframes slideInLeft {
  from { opacity: 0; transform: translateX(-30px); }
  to { opacity: 1; transform: translateX(0); }
}

@keyframes float {
  0%, 100% { transform: translateY(0); }
  50% { transform: translateY(-6px); }
}

/* ===== 全局按钮样式 ===== */
button {
  transition: all 0.25s cubic-bezier(0.4, 0, 0.2, 1) !important;
}

button:hover {
  transform: translateY(-2px);
}

button:active {
  transform: translateY(0) scale(0.97);
}

/* ===== 全局链接动画 ===== */
a {
  transition: color 0.25s ease;
}

/* ===== 全局输入框动画 ===== */
input, textarea, select {
  transition: all 0.25s cubic-bezier(0.4, 0, 0.2, 1) !important;
}


/* ===== 卡片基础样式 ===== */
.card, .article-card, .stat-card, .category-card, .widget, .tag-card {
  background: var(--bg-card);
  border: 1px solid var(--border-color);
  border-radius: 20px;
  transition: all 0.35s cubic-bezier(0.4, 0, 0.2, 1);
}

.card:hover, .article-card:hover, .category-card:hover {
  transform: translateY(-6px);
  box-shadow: var(--shadow-lg);
  border-color: var(--accent);
}

/* ===== Element Plus 深色主题覆盖 ===== */
.dark .el-button {
  --el-button-bg-color: var(--bg-tertiary);
  --el-button-border-color: var(--border-color);
  --el-button-text-color: var(--text-primary);
  --el-button-hover-bg-color: var(--bg-card);
}

.dark .el-input__wrapper {
  background: var(--bg-tertiary) !important;
  box-shadow: 0 0 0 1px var(--border-color) inset !important;
}

.dark .el-input__wrapper:hover {
  box-shadow: 0 0 0 1px var(--accent) inset !important;
}

.dark .el-input__inner {
  background: var(--bg-tertiary) !important;
  color: var(--text-primary) !important;
}

.dark .el-select .el-input__wrapper {
  background: var(--bg-tertiary) !important;
}

.dark .el-select .el-input__inner {
  background: var(--bg-tertiary) !important;
  color: var(--text-primary) !important;
}

.dark .el-select-dropdown {
  background: var(--bg-card) !important;
  border-color: var(--border-color) !important;
}

.dark .el-select-dropdown__item {
  color: var(--text-primary);
}

.dark .el-select-dropdown__item:hover {
  background: var(--bg-tertiary) !important;
}

.dark .el-select-dropdown__item.selected {
  color: var(--accent);
  font-weight: 600;
}

.dark .el-dialog {
  background: var(--bg-card) !important;
  border: 1px solid var(--border-color);
}

.dark .el-message-box {
  background: var(--bg-card) !important;
  border-color: var(--border-color) !important;
}

.dark .el-dropdown-menu {
  background: var(--bg-card) !important;
  border-color: var(--border-color) !important;
}

.dark .el-dropdown-menu__item {
  color: var(--text-primary);
}

.dark .el-dropdown-menu__item:hover {
  background: var(--bg-tertiary) !important;
  color: var(--accent);
}

.dark .el-tag {
  background: var(--bg-tertiary) !important;
  border-color: var(--border-color) !important;
  color: var(--text-primary);
}

.dark .el-table {
  background: var(--bg-card) !important;
  --el-table-bg-color: var(--bg-card);
  --el-table-header-bg-color: var(--bg-tertiary);
  --el-table-row-hover-bg-color: var(--bg-tertiary);
  --el-table-border-color: var(--border-color);
  --el-table-text-color: var(--text-primary);
}

.dark .el-pagination {
  --el-pagination-bg-color: var(--bg-tertiary);
  --el-pagination-text-color: var(--text-secondary);
  --el-pagination-button-bg-color: var(--bg-tertiary);
}

.dark .el-form-item__label {
  color: var(--text-secondary);
}

.dark .el-textarea__inner {
  background: var(--bg-tertiary) !important;
  border-color: var(--border-color) !important;
  color: var(--text-primary) !important;
}

/* ===== Element Plus 组件动画 ===== */
.el-dialog,
.el-message,
.el-message-box,
.el-overlay-message-box {
  backface-visibility: hidden;
  transform: translateZ(0);
  will-change: transform, opacity;
}

.el-dialog {
  animation: none !important;
}

.dialog-fade-enter-active .el-dialog,
.dialog-fade-leave-active .el-dialog {
  animation: none !important;
  transition: opacity 0.18s ease, transform 0.24s cubic-bezier(0.22, 1, 0.36, 1) !important;
}

.dialog-fade-enter-from .el-dialog,
.dialog-fade-leave-to .el-dialog {
  opacity: 0;
  transform: translateY(10px) scale(0.985);
}

.el-message {
  animation: none !important;
  border-radius: 14px;
  border-color: rgba(148, 163, 184, 0.22);
  box-shadow: 0 14px 36px rgba(15, 23, 42, 0.12);
  backdrop-filter: blur(14px);
  -webkit-backdrop-filter: blur(14px);
  transition:
    opacity 0.18s ease,
    transform 0.22s cubic-bezier(0.22, 1, 0.36, 1),
    top 0.22s cubic-bezier(0.22, 1, 0.36, 1),
    bottom 0.22s cubic-bezier(0.22, 1, 0.36, 1) !important;
}

.el-message__content {
  line-height: 1.45;
}

.el-message-fade-enter-active,
.el-message-fade-leave-active {
  transition:
    opacity 0.18s ease,
    transform 0.22s cubic-bezier(0.22, 1, 0.36, 1),
    top 0.22s cubic-bezier(0.22, 1, 0.36, 1),
    bottom 0.22s cubic-bezier(0.22, 1, 0.36, 1) !important;
}

.el-message-fade-enter-from.is-left,
.el-message-fade-enter-from.is-right,
.el-message-fade-leave-to.is-left,
.el-message-fade-leave-to.is-right {
  opacity: 0;
  transform: translateY(-10px) scale(0.985);
}

.el-message-fade-enter-from.is-left.is-bottom,
.el-message-fade-enter-from.is-right.is-bottom,
.el-message-fade-leave-to.is-left.is-bottom,
.el-message-fade-leave-to.is-right.is-bottom {
  transform: translateY(10px) scale(0.985);
}

.el-message-fade-enter-from.is-center,
.el-message-fade-leave-to.is-center {
  opacity: 0;
  transform: translate(-50%, -10px) scale(0.985);
}

.el-message-fade-enter-from.is-center.is-bottom,
.el-message-fade-leave-to.is-center.is-bottom {
  transform: translate(-50%, 10px) scale(0.985);
}

.el-message-box {
  width: min(92vw, 430px) !important;
  position: relative;
  padding: 0 !important;
  overflow: hidden;
  border-radius: 24px !important;
  border: 1px solid var(--message-box-border);
  background: var(--message-box-bg) !important;
  box-shadow: var(--message-box-shadow);
  backdrop-filter: blur(18px);
  -webkit-backdrop-filter: blur(18px);
}

.el-message-box::before {
  content: '';
  position: absolute;
  top: 12px;
  left: 18px;
  right: 18px;
  height: 3px;
  border-radius: 999px;
  background: linear-gradient(90deg, var(--accent), color-mix(in srgb, var(--accent-secondary) 84%, var(--accent)));
  opacity: 0.9;
  pointer-events: none;
}

.el-overlay-message-box,
.el-overlay-dialog,
.el-overlay {
  background: transparent !important;
  backdrop-filter: none !important;
  -webkit-backdrop-filter: none !important;
}

.el-overlay-message-box {
  position: fixed !important;
  inset: 0 !important;
  display: flex !important;
  align-items: center !important;
  justify-content: center !important;
  padding: 18px !important;
  box-sizing: border-box;
}

.el-message-box {
  margin: 0 !important;
  max-width: min(92vw, 430px) !important;
  max-height: calc(100vh - 36px);
}

.el-message-box__header {
  position: relative;
  padding: 22px 64px 12px 22px !important;
}

.el-message-box__title {
  display: flex;
  align-items: center;
  gap: 10px;
  color: var(--text-primary);
  font-size: 1.04rem;
  font-weight: 700;
  letter-spacing: 0.01em;
}

.el-message-box__headerbtn {
  position: absolute !important;
  top: 20px !important;
  right: 18px !important;
  display: inline-flex !important;
  align-items: center !important;
  justify-content: center !important;
  width: 36px;
  height: 36px;
  padding: 0 !important;
  margin: 0 !important;
  border-radius: 12px;
  border: 1px solid color-mix(in srgb, var(--message-box-muted-border) 92%, transparent);
  color: var(--text-secondary);
  background: color-mix(in srgb, var(--message-box-muted-bg) 84%, transparent);
  box-sizing: border-box;
  cursor: pointer !important;
  transition: color 0.2s ease, background 0.2s ease, transform 0.2s ease;
}

.el-message-box__headerbtn .el-message-box__close,
.el-message-box__headerbtn i,
.el-message-box__headerbtn svg {
  width: 16px !important;
  height: 16px !important;
  margin: 0 !important;
  font-size: 16px !important;
  line-height: 1 !important;
}

.el-message-box__headerbtn:hover {
  color: var(--accent);
  background: color-mix(in srgb, var(--accent-light) 88%, var(--message-box-muted-bg));
  transform: translateY(-1px);
}

.el-message-box__content {
  padding: 4px 22px 0 !important;
}

.el-message-box__container {
  display: flex !important;
  align-items: flex-start;
  gap: 14px;
}

.el-message-box__status {
  position: static !important;
  display: inline-flex !important;
  align-items: center !important;
  justify-content: center !important;
  flex: 0 0 38px;
  width: 38px;
  height: 38px;
  margin-top: 0 !important;
  font-size: 28px !important;
  color: var(--accent) !important;
  border-radius: 50%;
  background: color-mix(in srgb, var(--accent-light) 86%, #fff);
  box-shadow: inset 0 0 0 1px color-mix(in srgb, var(--accent) 12%, transparent);
}

.el-message-box__status.el-icon-warning {
  color: #f2b94b !important;
  background: linear-gradient(180deg, rgba(255, 245, 225, 0.98), rgba(255, 235, 208, 0.94));
  box-shadow: inset 0 0 0 1px rgba(242, 185, 75, 0.2);
}

.el-message-box__status.el-icon-error {
  color: var(--message-box-danger) !important;
  background: linear-gradient(180deg, rgba(255, 240, 238, 0.98), rgba(255, 227, 223, 0.94));
  box-shadow: inset 0 0 0 1px rgba(208, 93, 93, 0.18);
}

.el-message-box__message {
  flex: 1 1 auto;
  min-width: 0;
  padding-top: 4px;
  color: var(--text-secondary);
  font-size: 0.98rem;
  line-height: 1.6;
}

.el-message-box__message p {
  margin: 0;
}

.el-message-box__container:has(.el-message-box__status) {
  align-items: center;
}

.el-message-box__input {
  padding-top: 18px;
}

.el-message-box__input .el-input__wrapper,
.el-message-box__input .el-textarea__inner {
  background: color-mix(in srgb, var(--message-box-muted-bg) 92%, transparent) !important;
  box-shadow: 0 0 0 1px var(--message-box-muted-border) inset !important;
}

.el-message-box__input .el-input__wrapper:hover,
.el-message-box__input .el-textarea__inner:hover {
  box-shadow: 0 0 0 1px color-mix(in srgb, var(--accent) 28%, var(--message-box-muted-border)) inset !important;
}

.el-message-box__btns {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  padding: 20px 22px 22px !important;
  border-top: 0;
  background: linear-gradient(180deg, transparent, color-mix(in srgb, var(--message-box-muted-bg) 68%, transparent));
}

.el-message-box__btns .el-button {
  min-width: 96px;
  min-height: 42px;
  margin-left: 0 !important;
  border-radius: 14px;
  border: 1px solid var(--message-box-muted-border);
  background: color-mix(in srgb, var(--message-box-muted-bg) 88%, transparent);
  color: var(--text-secondary);
  box-shadow: 0 10px 22px rgba(15, 23, 42, 0.06);
  transition: transform 0.2s ease, box-shadow 0.2s ease, border-color 0.2s ease, background 0.2s ease, color 0.2s ease;
}

.el-message-box__btns .el-button:hover {
  color: var(--text-primary);
  border-color: color-mix(in srgb, var(--accent) 20%, var(--message-box-muted-border));
  background: color-mix(in srgb, var(--accent-light) 62%, var(--message-box-muted-bg));
  box-shadow: 0 12px 24px rgba(15, 23, 42, 0.1);
}

.el-message-box__btns .el-button--primary {
  border-color: transparent;
  background: linear-gradient(135deg, var(--accent), color-mix(in srgb, var(--accent-secondary) 84%, var(--accent)));
  color: #fff;
  box-shadow: 0 16px 30px color-mix(in srgb, var(--accent) 24%, transparent);
}

.el-message-box__btns .el-button--primary:hover {
  color: #fff;
  border-color: transparent;
  background: linear-gradient(135deg, color-mix(in srgb, var(--accent) 94%, #fff), color-mix(in srgb, var(--accent-secondary) 92%, var(--accent)));
  box-shadow: 0 18px 34px color-mix(in srgb, var(--accent) 28%, transparent);
}

.el-message-box__btns .el-button--danger {
  border-color: transparent;
  background: linear-gradient(135deg, var(--message-box-danger), color-mix(in srgb, var(--message-box-danger) 78%, #f59e9e));
  color: #fff;
  box-shadow: 0 16px 30px color-mix(in srgb, var(--message-box-danger) 22%, transparent);
}

.el-message-box__btns .el-button--danger:hover {
  color: #fff;
  background: linear-gradient(135deg, color-mix(in srgb, var(--message-box-danger) 94%, #fff), color-mix(in srgb, var(--message-box-danger) 82%, #f5b2b2));
}

.el-message-box__btns .el-button:not(.el-button--primary):not(.el-button--danger).el-button--text,
.el-message-box__btns .el-button:not(.el-button--primary):not(.el-button--danger).is-text {
  background: color-mix(in srgb, var(--message-box-danger-soft) 72%, var(--message-box-muted-bg));
  border-color: color-mix(in srgb, var(--message-box-danger) 22%, var(--message-box-muted-border));
  color: var(--message-box-danger);
}

.el-message-box:has(.el-message-box__status.el-icon-warning),
.el-message-box:has(.el-message-box__status.el-icon-error) {
  border-color: var(--message-box-warning-border);
  background: var(--message-box-warning-bg) !important;
  box-shadow: var(--message-box-warning-shadow);
}

.el-message-box:has(.el-message-box__status.el-icon-warning)::before,
.el-message-box:has(.el-message-box__status.el-icon-error)::before {
  background: linear-gradient(90deg, #f0ad4e, var(--message-box-danger));
}

.el-message-box:has(.el-message-box__status.el-icon-warning) .el-message-box__title,
.el-message-box:has(.el-message-box__status.el-icon-error) .el-message-box__title,
.el-message-box:has(.el-message-box__status.el-icon-warning) .el-message-box__message,
.el-message-box:has(.el-message-box__status.el-icon-error) .el-message-box__message {
  color: var(--text-primary);
}

.el-message-box:has(.el-message-box__status.el-icon-warning) .el-message-box__headerbtn:hover,
.el-message-box:has(.el-message-box__status.el-icon-error) .el-message-box__headerbtn:hover {
  color: var(--message-box-danger);
  background: color-mix(in srgb, var(--message-box-danger-soft) 88%, var(--message-box-warning-muted-bg));
}

.el-message-box:has(.el-message-box__status.el-icon-warning) .el-message-box__headerbtn,
.el-message-box:has(.el-message-box__status.el-icon-error) .el-message-box__headerbtn {
  background: color-mix(in srgb, var(--message-box-warning-muted-bg) 88%, transparent);
}

.el-message-box:has(.el-message-box__status.el-icon-warning) .el-message-box__input .el-input__wrapper,
.el-message-box:has(.el-message-box__status.el-icon-warning) .el-message-box__input .el-textarea__inner,
.el-message-box:has(.el-message-box__status.el-icon-error) .el-message-box__input .el-input__wrapper,
.el-message-box:has(.el-message-box__status.el-icon-error) .el-message-box__input .el-textarea__inner {
  background: color-mix(in srgb, var(--message-box-warning-muted-bg) 90%, transparent) !important;
  box-shadow: 0 0 0 1px var(--message-box-warning-muted-border) inset !important;
}

.el-message-box:has(.el-message-box__status.el-icon-warning) .el-message-box__btns .el-button:not(.el-button--primary),
.el-message-box:has(.el-message-box__status.el-icon-error) .el-message-box__btns .el-button:not(.el-button--primary) {
  background: color-mix(in srgb, var(--message-box-warning-muted-bg) 92%, transparent);
  border-color: var(--message-box-warning-muted-border);
  color: color-mix(in srgb, var(--message-box-danger) 90%, var(--text-primary));
}

.el-message-box:has(.el-message-box__status.el-icon-warning) .el-message-box__btns .el-button:not(.el-button--primary):hover,
.el-message-box:has(.el-message-box__status.el-icon-error) .el-message-box__btns .el-button:not(.el-button--primary):hover {
  color: var(--message-box-danger);
  border-color: color-mix(in srgb, var(--message-box-danger) 30%, var(--message-box-warning-muted-border));
  background: color-mix(in srgb, var(--message-box-danger-soft) 72%, var(--message-box-warning-muted-bg));
}

.el-message-box:has(.el-message-box__status.el-icon-warning) .el-message-box__btns .el-button--primary,
.el-message-box:has(.el-message-box__status.el-icon-error) .el-message-box__btns .el-button--primary {
  background: linear-gradient(135deg, #f0ad4e, var(--message-box-danger));
  box-shadow: 0 16px 30px color-mix(in srgb, var(--message-box-danger) 22%, transparent);
}

.el-message-box:has(.el-message-box__status.el-icon-warning) .el-message-box__btns .el-button--primary:hover,
.el-message-box:has(.el-message-box__status.el-icon-error) .el-message-box__btns .el-button--primary:hover {
  background: linear-gradient(135deg, color-mix(in srgb, #f0ad4e 88%, #fff), color-mix(in srgb, var(--message-box-danger) 88%, #fff));
}

.el-message-box:has(.el-message-box__status.el-icon-warning) .el-message-box__btns,
.el-message-box:has(.el-message-box__status.el-icon-error) .el-message-box__btns {
  background: linear-gradient(180deg, transparent, color-mix(in srgb, var(--message-box-warning-muted-bg) 72%, transparent));
}

.fade-in-linear-enter-active .el-overlay-message-box,
.fade-in-linear-leave-active .el-overlay-message-box {
  animation: none !important;
  transition: opacity 0.18s ease, transform 0.24s cubic-bezier(0.22, 1, 0.36, 1) !important;
}

.fade-in-linear-enter-from .el-overlay-message-box,
.fade-in-linear-leave-to .el-overlay-message-box {
  opacity: 0;
  transform: translateY(-10px) scale(0.985);
}

.el-dropdown-menu {
  animation: fadeInUp 0.22s ease !important;
}

.el-select-dropdown {
  animation: fadeInUp 0.22s ease !important;
}

/* ===== 深色模式全局覆盖 ===== */
:global(html.dark) {
  color-scheme: dark;
}

:global(html.dark body) {
  background: var(--bg-primary);
  color: var(--text-primary);
}

:global(html.dark .app-container) {
  background: var(--gradient-bg);
}

/* ===== 滚动条美化 ===== */
::-webkit-scrollbar {
  width: 6px;
  height: 6px;
}

::-webkit-scrollbar-track {
  background: transparent;
}

::-webkit-scrollbar-thumb {
  background: var(--text-tertiary);
  border-radius: 3px;
}

::-webkit-scrollbar-thumb:hover {
  background: var(--text-secondary);
}

/* ===== 全局类 ===== */
.animate-fade-in {
  animation: fadeIn 0.5s ease;
}

.animate-fade-in-up {
  animation: fadeInUp 0.5s ease;
}

.animate-scale-in {
  animation: scaleIn 0.4s ease;
}

.animate-slide-in-right {
  animation: slideInRight 0.4s ease;
}

.animate-float {
  animation: float 3s ease-in-out infinite;
}
</style>
