const STORAGE_KEYS = {
  token: 'token',
  refreshToken: 'refreshToken',
  user: 'user',
  rememberMe: 'rememberMe'
}

const STORAGES = [localStorage, sessionStorage]

const parseUser = (value) => {
  if (!value) {
    return null
  }

  try {
    return JSON.parse(value)
  } catch {
    return null
  }
}

const readFromStorage = (storage) => ({
  storage,
  token: storage.getItem(STORAGE_KEYS.token) || '',
  refreshToken: storage.getItem(STORAGE_KEYS.refreshToken) || '',
  user: parseUser(storage.getItem(STORAGE_KEYS.user)),
  rememberMe: storage.getItem(STORAGE_KEYS.rememberMe) === 'true'
})

export const clearAuthSession = () => {
  STORAGES.forEach((storage) => {
    Object.values(STORAGE_KEYS).forEach((key) => storage.removeItem(key))
  })
}

export const readAuthSession = () => {
  const localSession = readFromStorage(localStorage)
  if (localSession.token || localSession.refreshToken || localSession.user || localSession.rememberMe) {
    return localSession
  }

  const sessionValue = readFromStorage(sessionStorage)
  if (sessionValue.token || sessionValue.refreshToken || sessionValue.user) {
    return sessionValue
  }

  return {
    storage: localStorage,
    token: '',
    refreshToken: '',
    user: null,
    rememberMe: false
  }
}

export const saveAuthSession = ({
  token = '',
  refreshToken = '',
  user = null,
  rememberMe = false
}) => {
  clearAuthSession()

  const storage = rememberMe ? localStorage : sessionStorage
  storage.setItem(STORAGE_KEYS.rememberMe, rememberMe ? 'true' : 'false')

  if (token) {
    storage.setItem(STORAGE_KEYS.token, token)
  }

  if (refreshToken) {
    storage.setItem(STORAGE_KEYS.refreshToken, refreshToken)
  }

  if (user) {
    storage.setItem(STORAGE_KEYS.user, JSON.stringify(user))
  }
}

export const updateAuthSession = (updates = {}) => {
  const currentSession = readAuthSession()

  saveAuthSession({
    token: Object.prototype.hasOwnProperty.call(updates, 'token') ? updates.token : currentSession.token,
    refreshToken: Object.prototype.hasOwnProperty.call(updates, 'refreshToken') ? updates.refreshToken : currentSession.refreshToken,
    user: Object.prototype.hasOwnProperty.call(updates, 'user') ? updates.user : currentSession.user,
    rememberMe: Object.prototype.hasOwnProperty.call(updates, 'rememberMe') ? updates.rememberMe : currentSession.rememberMe
  })
}

export const getAccessToken = () => readAuthSession().token

export const getRefreshToken = () => readAuthSession().refreshToken
