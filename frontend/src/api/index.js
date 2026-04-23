import axios from 'axios'
import {
  clearAuthSession,
  getAccessToken,
  getRefreshToken,
  readAuthSession,
  updateAuthSession
} from '@/utils/session'

const EMAIL_REQUEST_TIMEOUT = 30000
const REFRESH_REQUEST_TIMEOUT = 15000

const api = axios.create({
  baseURL: '/api',
  timeout: 10000
})

let refreshPromise = null

const createApiError = (message, payload = {}, status = 200) => {
  const error = new Error(message || 'Request failed')
  error.name = 'ApiError'
  error.code = payload?.code || status
  error.data = payload?.data
  error.response = {
    status,
    data: payload
  }
  return error
}

const decodeBase64Url = (value) => {
  const normalized = value.replace(/-/g, '+').replace(/_/g, '/')
  const padded = normalized.padEnd(normalized.length + ((4 - normalized.length % 4) % 4), '=')
  return atob(padded)
}

export const isTokenValid = (token) => {
  if (!token) {
    return false
  }

  try {
    const parts = token.split('.')
    if (parts.length !== 3) {
      return false
    }

    const payload = JSON.parse(decodeBase64Url(parts[1]))
    return !payload.exp || Date.now() < payload.exp * 1000
  } catch {
    return false
  }
}

const redirectToLogin = () => {
  if (window.location.pathname !== '/login') {
    window.location.href = '/login'
  }
}

const applyAuthPayload = (payload) => {
  const currentSession = readAuthSession()
  const accessToken = payload?.accessToken || ''
  if (!accessToken) {
    throw new Error('Missing access token')
  }

  updateAuthSession({
    token: accessToken,
    refreshToken: payload?.refreshToken || currentSession.refreshToken,
    user: payload?.user || currentSession.user
  })

  return accessToken
}

const handleAuthFailure = (message = 'Authentication expired') => {
  clearAuthSession()
  redirectToLogin()
  return Promise.reject(new Error(message))
}

const requestTokenRefresh = async () => {
  const refreshToken = getRefreshToken()
  if (!refreshToken) {
    throw new Error('Missing refresh token')
  }

  if (!refreshPromise) {
    refreshPromise = api.post(
      '/auth/tokens/refresh',
      { refreshToken },
      {
        skipAuthRefresh: true,
        timeout: REFRESH_REQUEST_TIMEOUT
      }
    )
      .then((payload) => applyAuthPayload(payload.data))
      .finally(() => {
        refreshPromise = null
      })
  }

  return refreshPromise
}

const retryWithRefresh = async (requestConfig, fallbackMessage) => {
  const session = readAuthSession()
  if (!session.refreshToken) {
    return handleAuthFailure(fallbackMessage)
  }

  try {
    const nextAccessToken = await requestTokenRefresh()
    requestConfig._retry = true
    requestConfig.headers = {
      ...(requestConfig.headers || {}),
      Authorization: `Bearer ${nextAccessToken}`
    }
    return api.request(requestConfig)
  } catch (error) {
    return handleAuthFailure(error?.message || fallbackMessage)
  }
}

export const ensureValidAccessToken = async () => {
  const session = readAuthSession()
  if (!session.user || (!session.token && !session.refreshToken)) {
    return false
  }

  if (isTokenValid(session.token)) {
    return true
  }

  if (!session.refreshToken) {
    return false
  }

  try {
    await requestTokenRefresh()
    return true
  } catch {
    clearAuthSession()
    return false
  }
}

api.interceptors.request.use((config) => {
  const token = getAccessToken()
  config.headers = config.headers || {}

  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }

  return config
})

api.interceptors.response.use(
  (response) => {
    const payload = response.data

    if (payload?.code === 401) {
      if (response.config?.skipAuthRefresh || response.config?._retry) {
        return handleAuthFailure(payload.message || 'Authentication expired')
      }
      return retryWithRefresh(response.config, payload.message || 'Authentication expired')
    }

    if (payload?.code === 403) {
      return Promise.reject(new Error(payload.message || 'Forbidden'))
    }

    if (payload?.code && payload.code !== 200) {
      return Promise.reject(createApiError(payload.message || 'Request failed', payload, response.status))
    }

    return payload
  },
  async (error) => {
    if (error?.response?.status === 401 && error.config && !error.config.skipAuthRefresh && !error.config._retry) {
      return retryWithRefresh(error.config, error.response?.data?.message || error.message)
    }

    return Promise.reject(error)
  }
)

export const authApi = {
  getCaptcha: (type = 'login', identifier = '') =>
    api.get('/auth/captcha/base64', { params: { type, identifier } }),
  getTurnstileConfig: () => api.get('/auth/turnstile/config'),
  login: (data) => api.post('/auth/login', data),
  refreshToken: (refreshToken) => api.post('/auth/tokens/refresh', { refreshToken }, { skipAuthRefresh: true }),
  logout: (refreshToken) => api.post('/auth/tokens/logout', {}, {
    headers: refreshToken ? { 'X-Refresh-Token': refreshToken } : undefined
  }),
  logoutAll: () => api.post('/auth/tokens/logout-all'),
  register: (data) => api.post('/auth/register', data),
  sendEmailCode: (email, type = 'register', turnstileToken = '') =>
    api.post('/auth/send-email-code', { email, type, turnstileToken }, { timeout: EMAIL_REQUEST_TIMEOUT }),
  sendForgetCode: (email) =>
    api.post('/auth/send-forget-code', { email }, { timeout: EMAIL_REQUEST_TIMEOUT }),
  verifyEmailCode: (data) => api.post('/auth/verify-email-code', data),
  resetPassword: (data) => api.post('/auth/reset-password', data)
}

export const contentApi = {
  getPosts: (params) => api.get('/posts', { params }),
  getPostTags: () => api.get('/posts/tags'),
  getPostCategories: () => api.get('/posts/categories'),
  getPostSearchIndex: () => api.get('/posts/search-index'),
  getPost: (id) => api.get(`/posts/${id}`),
  getPostComments: (id) => api.get(`/posts/${id}/comments`),
  createPostComment: (id, data) => api.post(`/posts/${id}/comments`, data),
  createCommentReport: (postId, commentId, data) => api.post(`/posts/${postId}/comments/${commentId}/reports`, data),
  uploadCommentImages: (formData) =>
    api.post('/public/comments/images', formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
      timeout: 120000
    }),
  getMusicList: () => api.get('/public/music/list'),
  getNextMusic: (id) => api.get(`/public/music/next/${id}`),
  getPrevMusic: (id) => api.get(`/public/music/prev/${id}`),
  getStats: () => api.get('/public/stats'),
  getFriendLinks: () => api.get('/public/friend-links'),
  submitContact: (data) => api.post('/public/contact', data),
  submitFriendLink: (data) => api.post('/public/friend-links/apply', data)
}

export const profileApi = {
  getProfile: () => api.get('/admin/profile'),
  updateBasic: (data) => api.put('/admin/profile/basic', data),
  updateEmail: (data) => api.put('/admin/profile/email', data),
  uploadAvatar: (formData) =>
    api.post('/admin/profile/avatar/upload', formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
      timeout: 120000
    }),
  updateAvatar: (avatar) => api.put('/admin/profile/avatar', { avatar }),
  updatePassword: (data) => api.put('/admin/profile/password', data),
  deleteAccount: () => api.delete('/admin/profile')
}

export const adminApi = {
  getOverview: () => api.get('/admin/overview'),
  getPosts: (params) => api.get('/admin/posts', { params }),
  getPostDetail: (id) => api.get(`/admin/posts/${id}`),
  getPostWorkspace: (params) => api.get('/admin/post-workspace', { params }),
  createPost: (data) => api.post('/admin/posts', data),
  updatePost: (id, data) => api.put(`/admin/posts/${id}`, data),
  deletePost: (id) => api.delete(`/admin/posts/${id}`),
  uploadPostImages: (formData) =>
    api.post('/admin/posts/images', formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
      timeout: 120000
    }),
  submitPostReview: (id, data) => api.post(`/admin/posts/${id}/review`, data),
  savePostDraft: (id, data) => api.put(`/admin/posts/${id}/draft`, data),
  offlinePost: (id) => api.put(`/admin/posts/${id}/offline`),
  publishPost: (id) => api.put(`/admin/posts/${id}/publish`),
  createPostReview: (data) => api.post('/admin/post-reviews', data),
  updatePostReview: (id, data) => api.put(`/admin/post-reviews/${id}`, data),
  submitSavedPostReview: (id) => api.put(`/admin/post-reviews/${id}/submit`),
  getPostReviews: (params) => api.get('/admin/post-reviews', { params }),
  getPostReviewDetail: (id) => api.get(`/admin/post-reviews/${id}`),
  approvePostReview: (id) => api.put(`/admin/post-reviews/${id}/approve`),
  rejectPostReview: (id, data) => api.put(`/admin/post-reviews/${id}/reject`, data),
  submitPostReviewBatch: (operation, data) => api.post(`/admin/post-reviews/batch/${operation}`, data),
  deletePostReview: (id) => api.delete(`/admin/post-reviews/${id}`),
  getMusic: (params) => api.get('/admin/music', { params }),
  uploadMusicAssets: (formData) =>
    api.post('/admin/music/upload', formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
      timeout: 120000
    }),
  createMusic: (data) => api.post('/admin/music', data),
  updateMusic: (id, data) => api.put(`/admin/music/${id}`, data),
  deleteMusic: (id) => api.delete(`/admin/music/${id}`),
  getUsers: (params) => api.get('/admin/users', { params }),
  createUser: (data) => api.post('/admin/users', data),
  updateUser: (id, data) => api.put(`/admin/users/${id}`, data),
  deleteUser: (id) => api.delete(`/admin/users/${id}`),
  getFriendLinks: (params) => api.get('/admin/friend-links', { params }),
  createFriendLink: (data) => api.post('/admin/friend-links', data),
  updateFriendLink: (id, data) => api.put(`/admin/friend-links/${id}`, data),
  deleteFriendLink: (id) => api.delete(`/admin/friend-links/${id}`),
  approveFriendLink: (id) => api.put(`/admin/friend-links/${id}/approve`),
  getComments: (params) => api.get('/admin/comments', { params }),
  approveComment: (id) => api.put(`/admin/comments/${id}/approve`),
  rejectComment: (id, data) => api.put(`/admin/comments/${id}/reject`, data),
  submitCommentBatch: (operation, data) => api.post(`/admin/comments/batch/${operation}`, data),
  deleteComment: (id) => api.delete(`/admin/comments/${id}`),
  getCommentReports: (params) => api.get('/admin/comments/reports', { params }),
  resolveCommentReport: (id, data) => api.put(`/admin/comments/reports/${id}/resolve`, data),
  getModerationTask: (taskId) => api.get(`/admin/moderation-tasks/${taskId}`),
  getContactMessages: (params) => api.get('/admin/contact-messages', { params }),
  replyContactMessage: (id, replyContent) => api.post(`/admin/contact-messages/${id}/reply`, { replyContent }),
  deleteContactMessage: (id) => api.delete(`/admin/contact-messages/${id}`)
}

export const chatApi = {
  getBootstrap: () => api.get('/chat/bootstrap'),
  getGlobalMessages: (params) => api.get('/chat/global/messages', { params }),
  getPrivateMessages: (targetUserId, params) => api.get(`/chat/private/${targetUserId}/messages`, { params }),
  requestFriend: (targetUserId) => api.post(`/chat/friend-requests/${targetUserId}`),
  addFriend: (targetUserId) => api.post(`/chat/friend-requests/${targetUserId}`),
  acceptFriendRequest: (requestId) => api.post(`/chat/friend-requests/${requestId}/accept`),
  rejectFriendRequest: (requestId) => api.post(`/chat/friend-requests/${requestId}/reject`),
  cancelFriendRequest: (requestId) => api.delete(`/chat/friend-requests/${requestId}`),
  removeFriend: (targetUserId) => api.delete(`/chat/friends/${targetUserId}`),
  blockUser: (targetUserId) => api.post(`/chat/blocks/${targetUserId}`),
  unblockUser: (targetUserId) => api.delete(`/chat/blocks/${targetUserId}`),
  markRoomRead: (data) => api.post('/chat/read', data),
  searchContacts: (params) => api.get('/chat/search/contacts', { params }),
  searchMessages: (params) => api.get('/chat/search/messages', { params }),
  sendMessage: (data) => api.post('/chat/messages', data)
}

export const publicApi = {
  ...authApi,
  ...contentApi
}

export default api
