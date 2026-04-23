import { createRouter, createWebHistory } from 'vue-router'
import { ensureValidAccessToken } from '@/api'
import { clearAuthSession, readAuthSession } from '@/utils/session'

const routes = [
  { path: '/', name: 'Home', component: () => import('../views/Home.vue') },
  { path: '/tags', name: 'Tags', component: () => import('../views/Tags.vue') },
  { path: '/categories', name: 'Categories', component: () => import('../views/Categories.vue') },
  { path: '/about', name: 'About', component: () => import('../views/About.vue') },
  { path: '/modules', name: 'Modules', component: () => import('../views/Modules.vue') },
  { path: '/login', name: 'Login', component: () => import('../views/Login.vue') },
  { path: '/register', name: 'Register', component: () => import('../views/Register.vue') },
  { path: '/forgot-password', name: 'ForgotPassword', component: () => import('../views/ForgotPassword.vue') },
  { path: '/admin', name: 'Admin', component: () => import('../views/Admin.vue'), meta: { requiresAuth: true } },
  { path: '/admin/post-editor', name: 'AdminPostEditor', component: () => import('../views/AdminPostEditor.vue'), meta: { requiresAuth: true } },
  { path: '/post/:id', name: 'PostDetail', component: () => import('../views/PostDetail.vue') }
]

const router = createRouter({
  history: createWebHistory(),
  routes,
  scrollBehavior(to, from, savedPosition) {
    if (savedPosition) {
      return savedPosition
    }

    return {
      top: 0,
      left: 0,
      behavior: 'auto'
    }
  }
})

router.beforeEach(async (to) => {
  if (!to.meta.requiresAuth) {
    return true
  }

  const session = readAuthSession()
  if (!session.user || (!session.token && !session.refreshToken)) {
    clearAuthSession()
    return { path: '/login', query: { redirect: to.fullPath } }
  }

  const isAuthorized = await ensureValidAccessToken()
  if (!isAuthorized) {
    clearAuthSession()
    return { path: '/login', query: { redirect: to.fullPath } }
  }

  return true
})

export default router
