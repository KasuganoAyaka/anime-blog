import { createI18n } from 'vue-i18n'
import en from 'element-plus/es/locale/lang/en'
import zhCn from 'element-plus/es/locale/lang/zh-cn'
import zhTw from 'element-plus/es/locale/lang/zh-tw'
import { defaultLocale, fallbackLocale, messages, supportedLocales } from './messages'

export { defaultLocale, fallbackLocale, supportedLocales } from './messages'

export const LANG_STORAGE_KEY = 'lang'

const resolveInitialLocale = () => {
  const saved = localStorage.getItem(LANG_STORAGE_KEY)
  return supportedLocales.includes(saved) ? saved : defaultLocale
}

export const i18n = createI18n({
  legacy: false,
  globalInjection: true,
  locale: resolveInitialLocale(),
  fallbackLocale,
  messages
})

export const elementLocales = {
  'zh-CN': zhCn,
  'zh-TW': zhTw,
  en
}

export const setI18nLanguage = (locale) => {
  const targetLocale = supportedLocales.includes(locale) ? locale : defaultLocale

  i18n.global.locale.value = targetLocale
  document.documentElement.lang = targetLocale === 'en' ? 'en' : targetLocale
  localStorage.setItem(LANG_STORAGE_KEY, targetLocale)

  return targetLocale
}

setI18nLanguage(i18n.global.locale.valueOf)
