import { authApi } from '@/api'

const defaultTurnstileConfig = Object.freeze({
  enabled: false,
  siteKey: '',
  login: {
    enabled: false,
    failureThreshold: 0
  },
  register: {
    emailCheckEnabled: false
  },
  comment: {
    guestCheckEnabled: false
  }
})

let turnstileConfigCache = null
let turnstileConfigPromise = null

const normalizeTurnstileConfig = (data = {}) => ({
  enabled: Boolean(data?.enabled && data?.siteKey),
  siteKey: data?.siteKey || '',
  login: {
    enabled: Boolean(data?.login?.enabled && data?.siteKey),
    failureThreshold: Number(data?.login?.failureThreshold || 0)
  },
  register: {
    emailCheckEnabled: Boolean(data?.register?.emailCheckEnabled && data?.siteKey)
  },
  comment: {
    guestCheckEnabled: Boolean(data?.comment?.guestCheckEnabled && data?.siteKey)
  }
})

export const getDefaultTurnstileConfig = () => ({
  ...defaultTurnstileConfig,
  login: { ...defaultTurnstileConfig.login },
  register: { ...defaultTurnstileConfig.register },
  comment: { ...defaultTurnstileConfig.comment }
})

export const getTurnstileConfig = async ({ force = false } = {}) => {
  if (!force && turnstileConfigCache) {
    return turnstileConfigCache
  }

  if (!turnstileConfigPromise || force) {
    turnstileConfigPromise = authApi.getTurnstileConfig()
      .then((res) => normalizeTurnstileConfig(res.data || {}))
      .catch(() => getDefaultTurnstileConfig())
      .finally(() => {
        turnstileConfigPromise = null
      })
  }

  turnstileConfigCache = await turnstileConfigPromise
  return turnstileConfigCache
}
