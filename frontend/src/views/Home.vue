<template>
  <div class="blog-home">
    <TopHeader @toggle-music="uiStore.toggleMusicPlayer()" />

    <main class="main-content">
      <div class="container">
        <div class="content-area">
          <div v-if="loading" class="loading-state">
            <div class="loading-spinner"></div>
            <span>{{ t('common.loading') }}</span>
          </div>

          <div v-else-if="posts.length === 0" class="empty-state animate-scale-in">
            <div class="empty-icon">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.85" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
                <path d="M7 4.5h8l4 4V19a1.5 1.5 0 0 1-1.5 1.5h-10A1.5 1.5 0 0 1 6 19V6A1.5 1.5 0 0 1 7.5 4.5Z" />
                <path d="M15 4.5V9h4" />
                <path d="M9 12h6" />
              </svg>
            </div>
            <div class="empty-text">{{ t('home.emptyTitle') }}</div>
            <div class="empty-desc">{{ t('home.emptyDesc') }}</div>
          </div>

          <div v-else class="article-list">
            <article
              v-for="(post, index) in visiblePosts"
              :key="post.id"
              :class="['article-card', 'animate-fade-in-up', { 'has-cover': !!post.coverImage }]"
              :style="{ animationDelay: `${index * 0.08}s` }"
              @click="goToPost(post)"
            >
              <div v-if="post.coverImage" class="article-card-layout">
                <div class="article-main">
                  <div class="article-meta">
                    <span class="date">{{ formatDate(post.createTime) }}</span>
                    <span class="dot">•</span>
                    <span class="views">{{ t('home.views', { count: post.viewCount || 0 }) }}</span>
                  </div>
                  <h2 class="article-title">
                    <a href="#">{{ post.title }}</a>
                  </h2>
                  <p v-if="post.excerpt || post.summary" class="article-excerpt">{{ post.excerpt || post.summary }}</p>
                  <div class="article-footer with-cover">
                    <div class="tags">
                      <span
                        v-if="post.category"
                        class="tag clickable-tag"
                        @click.stop="goToCategory(post.category)"
                      >{{ post.category }}</span>
                      <span
                        v-for="tag in getTags(post.tags)"
                        :key="tag"
                        class="tag clickable-tag"
                        @click.stop="goToTag(tag)"
                      >{{ tag }}</span>
                    </div>
                  </div>
                </div>

                <div class="article-side">
                  <div class="article-cover">
                    <img :src="post.coverImage" :alt="post.title" loading="lazy" />
                  </div>
                  <a href="#" class="read-more read-more-side" @click.prevent="goToPost(post)">
                    {{ t('common.readMore') }}
                    <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="2">
                      <path d="M5 12h14M12 5l7 7-7 7" />
                    </svg>
                  </a>
                </div>
              </div>

              <div v-else>
                <div class="article-meta">
                  <span class="date">{{ formatDate(post.createTime) }}</span>
                  <span class="dot">•</span>
                  <span class="views">{{ t('home.views', { count: post.viewCount || 0 }) }}</span>
                </div>
                <h2 class="article-title">
                  <a href="#">{{ post.title }}</a>
                </h2>
                <p v-if="post.excerpt || post.summary" class="article-excerpt">{{ post.excerpt || post.summary }}</p>
                <div class="article-footer">
                  <div class="tags">
                    <span
                      v-if="post.category"
                      class="tag clickable-tag"
                      @click.stop="goToCategory(post.category)"
                    >{{ post.category }}</span>
                    <span
                      v-for="tag in getTags(post.tags)"
                      :key="tag"
                      class="tag clickable-tag"
                      @click.stop="goToTag(tag)"
                    >{{ tag }}</span>
                  </div>
                  <a href="#" class="read-more" @click.prevent="goToPost(post)">
                    {{ t('common.readMore') }}
                    <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="2">
                      <path d="M5 12h14M12 5l7 7-7 7" />
                    </svg>
                  </a>
                </div>
              </div>
            </article>

            <div v-if="hasMorePosts || loadingMorePosts" ref="postsSentinelRef" class="waterfall-sentinel" aria-hidden="true">
              <div class="loading-spinner loading-spinner-inline"></div>
            </div>
          </div>
        </div>

        <aside class="sidebar" :class="{ floating: sidebarFloating }">
          <div ref="sidebarTriggerRef" class="sidebar-trigger" aria-hidden="true"></div>
          <div class="sidebar-shell" :style="sidebarShellStyle">
            <div class="sidebar-primary-stack">
              <div class="widget author-widget animate-fade-in-up">
                <div class="widget-title">
                  <span class="title-icon">
                    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
                      <circle cx="12" cy="8" r="4" />
                      <path d="M4 20c0-4 4-6 8-6s8 2 8 6" />
                    </svg>
                  </span>
                  <span>{{ t('home.author') }}</span>
                </div>
                <div class="author-card">
                  <button
                    type="button"
                    class="author-avatar-button"
                    :class="{ expanded: authorBioExpanded, interactive: hasSecretAuthorBio }"
                    :aria-expanded="authorBioExpanded ? 'true' : 'false'"
                    :aria-label="t('home.author')"
                    @click="toggleAuthorBio"
                  >
                    <template v-if="userStore.isLoggedIn && userStore.user">
                      <img
                        v-if="userStore.user.avatar"
                        :src="userStore.user.avatar"
                        class="author-avatar"
                        :alt="t('home.author')"
                      />
                      <div v-else class="author-avatar-default">
                        {{ (userStore.user.nickname || userStore.user.username || '?')[0].toUpperCase() }}
                      </div>
                    </template>
                    <template v-else>
                      <div class="author-avatar-guest">
                        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" width="36" height="36">
                          <circle cx="12" cy="8" r="4" />
                          <path d="M4 20c0-4 4-6 8-6s8 2 8 6" />
                        </svg>
                      </div>
                    </template>
                  </button>
                  <div class="author-name" :class="{ guest: !userStore.isLoggedIn || !userStore.user }">
                    {{ userStore.isLoggedIn && userStore.user ? (userStore.user.nickname || userStore.user.username) : t('common.guest') }}
                  </div>
                  <div class="author-bio">{{ t('home.authorBio') }}</div>
                  <Transition name="author-bio-reveal">
                    <div v-if="authorBioExpanded && hasSecretAuthorBio" class="author-secret-bio">
                      {{ displayedAuthorBio }}
                    </div>
                  </Transition>
                </div>
              </div>

              <div class="widget tags-widget animate-fade-in-up" style="animation-delay: 0.1s">
                <div class="widget-title">
                  <span class="title-icon">
                    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.85" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
                      <path d="M20 13l-7 7-9-9V4h7z" />
                      <circle cx="7.5" cy="7.5" r="1" />
                    </svg>
                  </span>
                  <span>{{ t('home.hotTags') }}</span>
                </div>
                <div class="tags-cloud">
                  <span
                    v-for="(tag, index) in tagList"
                    :key="tag"
                    class="tag-item animate-scale-in"
                    :style="{ animationDelay: `${index * 0.03}s` }"
                    @click="goToTag(tag)"
                  >{{ tag }}</span>
                  <span v-if="tagList.length === 0" class="empty-text">{{ t('home.noTags') }}</span>
                </div>
              </div>
            </div>

            <Transition name="friend-links-toggle">
              <div
                ref="friendLinksPanelRef"
                v-show="showFriendLinks"
                class="friend-links-panel"
              >
                <div class="widget links-widget">
                  <div class="widget-title">
                    <span class="title-icon">
                      <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.85" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
                        <path d="M10 14 21 3" />
                        <path d="M15 3h6v6" />
                        <path d="M14 10 8 16a3 3 0 1 1-4-4l6-6" />
                      </svg>
                    </span>
                    <span>{{ t('home.friendLinks') }}</span>
                  </div>
                  <ul class="friend-links">
                    <li v-for="link in friendLinks" :key="link.id" class="link-item">
                      <a :href="link.url" target="_blank" rel="noopener">
                        <span class="link-name">{{ link.name }}</span>
                        <svg viewBox="0 0 24 24" width="14" height="14" fill="none" stroke="currentColor" stroke-width="2">
                          <path d="M7 17L17 7M17 7H7M17 7V17" />
                        </svg>
                      </a>
                    </li>
                    <li v-if="friendLinks.length === 0" class="empty-text">{{ t('home.noLinks') }}</li>
                  </ul>
                </div>
              </div>
            </Transition>
          </div>
        </aside>
      </div>
    </main>
    <footer class="footer">
      <p>{{ t('common.copyright', { year: 2026 }) }}</p>
    </footer>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onBeforeUnmount, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { publicApi } from '@/api'
import TopHeader from '@/components/TopHeader.vue'
import { useLoadMoreTrigger } from '@/composables/useLoadMoreTrigger'
import { useUiStore, useUserStore } from '@/stores'
import { parseTagList } from '@/utils/tags'

const POST_PAGE_SIZE = 6

const router = useRouter()
const userStore = useUserStore()
const uiStore = useUiStore()
const { t, locale } = useI18n()
const loading = ref(true)
const loadingMorePosts = ref(false)
const authorBioExpanded = ref(false)
const posts = ref([])
const tagList = ref([])
const friendLinks = ref([])
const sidebarTriggerRef = ref(null)
const friendLinksPanelRef = ref(null)
const sidebarFloating = ref(false)
const sidebarStickyTop = ref(90)
const showFriendLinks = ref(true)
const currentPage = ref(1)
const totalPosts = ref(0)
const hasMorePosts = computed(() => posts.value.length < totalPosts.value)
const visiblePosts = computed(() => posts.value)
const sidebarShellStyle = computed(() => ({
  '--home-sidebar-sticky-top': `${sidebarStickyTop.value}px`
}))
const hasSecretAuthorBio = computed(() => {
  const profileBio = String(userStore.user?.bio || '').trim()
  return Boolean(profileBio)
})
const displayedAuthorBio = computed(() => {
  const profileBio = String(userStore.user?.bio || '').trim()
  return profileBio || t('home.authorBio')
})
const { sentinelRef: postsSentinelRef } = useLoadMoreTrigger(() => {
  void loadMorePosts()
}, {
  enabled: computed(() => !loading.value && !loadingMorePosts.value && hasMorePosts.value)
})

const dateLocale = computed(() => (locale.value === 'en' ? 'en-US' : locale.value))
const linksStorageKey = 'admin_links_updated'
let sidebarMotionFrame = 0
let sidebarMotionStart = 0

const getTags = (tags) => parseTagList(tags, { limit: 3 })

const mergePosts = (incomingPosts, reset = false) => {
  if (reset) {
    posts.value = incomingPosts
    return
  }

  const existingIds = new Set(posts.value.map((post) => post.id))
  posts.value = [...posts.value, ...incomingPosts.filter((post) => !existingIds.has(post.id))]
}

const fetchPosts = async ({ reset = false } = {}) => {
  const targetPage = reset ? 1 : currentPage.value + 1
  if (reset) {
    loading.value = true
  } else {
    loadingMorePosts.value = true
  }

  try {
    const res = await publicApi.getPosts({
      page: targetPage,
      size: POST_PAGE_SIZE
    })
    const pageData = res.data || {}
    const records = pageData.records || []

    mergePosts(records, reset)
    currentPage.value = Number(pageData.current || targetPage)
    totalPosts.value = Number(pageData.total || posts.value.length)
  } catch (error) {
    console.error('加载文章失败:', error)
  } finally {
    if (reset) {
      loading.value = false
    } else {
      loadingMorePosts.value = false
    }
  }
}

const fetchFriendLinks = async () => {
  try {
    const res = await publicApi.getFriendLinks()
    friendLinks.value = res.data || []
  } catch (error) {
    console.error('加载友情链接失败:', error)
  }
}

const fetchHotTags = async () => {
  try {
    const res = await publicApi.getPostTags()
    tagList.value = (res.data || []).map((item) => item.name).slice(0, 15)
  } catch (error) {
    console.error('加载热门标签失败:', error)
    tagList.value = []
  }
}

const loadMorePosts = async () => {
  if (loading.value || loadingMorePosts.value || !hasMorePosts.value) {
    return
  }

  await fetchPosts()
}

const refreshFriendLinks = async () => {
  await fetchFriendLinks()
}

const syncSidebarMotionState = () => {
  const header = document.querySelector('.top-header')
  const headerHeight = Math.round(header?.getBoundingClientRect().height || 72)
  const stickyTop = headerHeight + 18
  sidebarStickyTop.value = stickyTop

  if (window.innerWidth <= 900) {
    sidebarFloating.value = false
    showFriendLinks.value = true
    return
  }

  const triggerRectTop = sidebarTriggerRef.value?.getBoundingClientRect().top ?? Number.POSITIVE_INFINITY
  const triggerPageTop = triggerRectTop + window.scrollY
  sidebarMotionStart = Math.max(0, Math.round(triggerPageTop - stickyTop))

  const shouldFloat = window.scrollY >= sidebarMotionStart
  const friendLinksPanelHeight = friendLinksPanelRef.value?.offsetHeight || 0
  const maxScrollableDistance = Math.max(
    0,
    (document.documentElement?.scrollHeight || 0) - window.innerHeight
  )
  const canCollapseFriendLinks = friendLinksPanelHeight > 0
    && maxScrollableDistance > Math.max(160, friendLinksPanelHeight + 40)

  sidebarFloating.value = shouldFloat
  showFriendLinks.value = canCollapseFriendLinks ? !shouldFloat : true
}

const queueSidebarMotionState = () => {
  if (sidebarMotionFrame) {
    return
  }

  sidebarMotionFrame = window.requestAnimationFrame(() => {
    sidebarMotionFrame = 0
    syncSidebarMotionState()
  })
}

const handleLinksStorage = (event) => {
  if (event.key !== linksStorageKey) {
    return
  }

  void refreshFriendLinks()
  localStorage.removeItem(linksStorageKey)
}

const handleLinksUpdated = () => {
  void refreshFriendLinks()
}

const toggleAuthorBio = () => {
  if (!hasSecretAuthorBio.value) {
    return
  }
  authorBioExpanded.value = !authorBioExpanded.value
}

const formatDate = (date) => {
  if (!date) return ''
  const value = new Date(date)
  return value.toLocaleDateString(dateLocale.value, {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit'
  })
}

const goToPost = (post) => {
  router.push(`/post/${post.id}`)
}

const goToTag = (tag) => {
  if (!tag) return
  router.push({ path: '/tags', query: { tag } })
}

const goToCategory = (category) => {
  if (!category) return
  router.push({ path: '/categories', query: { category } })
}

defineExpose({ refreshFriendLinks })

onMounted(async () => {
  window.addEventListener('scroll', queueSidebarMotionState, { passive: true })
  window.addEventListener('resize', queueSidebarMotionState, { passive: true })
  window.addEventListener('storage', handleLinksStorage)
  window.addEventListener('links-updated', handleLinksUpdated)

  await Promise.all([
    fetchPosts({ reset: true }),
    fetchFriendLinks(),
    fetchHotTags()
  ])

  await nextTick()
  syncSidebarMotionState()
})

onBeforeUnmount(() => {
  window.removeEventListener('scroll', queueSidebarMotionState)
  window.removeEventListener('resize', queueSidebarMotionState)
  window.removeEventListener('storage', handleLinksStorage)
  window.removeEventListener('links-updated', handleLinksUpdated)

  if (sidebarMotionFrame) {
    window.cancelAnimationFrame(sidebarMotionFrame)
    sidebarMotionFrame = 0
  }
})
</script>

<style scoped>
.blog-home {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  background: var(--gradient-bg);
}

.main-content {
  flex: 1;
  padding: 40px 0;
}

.container {
  max-width: 1200px;
  margin: 0 auto;
  padding: 0 24px;
  display: flex;
  gap: 32px;
}

.content-area {
  flex: 1;
  min-width: 0;
}

.loading-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 100px 20px;
  color: var(--text-secondary);
  gap: 16px;
}

.loading-spinner {
  width: 40px;
  height: 40px;
  border: 3px solid var(--border-color);
  border-top-color: var(--accent);
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 80px 20px;
  background: var(--bg-card);
  border-radius: 24px;
  border: 1px solid var(--border-color);
}

.empty-icon {
  width: 72px;
  height: 72px;
  margin-bottom: 16px;
  color: var(--accent);
}

.empty-icon svg {
  width: 100%;
  height: 100%;
}

.empty-text {
  font-size: 1.25rem;
  font-weight: 600;
  color: var(--text-primary);
  margin-bottom: 8px;
}

.empty-desc {
  color: var(--text-secondary);
}

.article-list {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.waterfall-sentinel {
  display: flex;
  justify-content: center;
  padding: 8px 0 20px;
}

.loading-spinner-inline {
  width: 30px;
  height: 30px;
  border-width: 2px;
}

.article-card {
  background: var(--bg-card);
  border: 1px solid var(--border-color);
  border-radius: 20px;
  padding: 28px;
  cursor: pointer;
  transition: all 0.35s cubic-bezier(0.4, 0, 0.2, 1);
  opacity: 0;
  animation-fill-mode: forwards;
}

.article-card:hover {
  transform: translateY(-6px);
  box-shadow: var(--shadow-lg);
  border-color: var(--accent);
}

.article-card-layout {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 220px;
  gap: 20px;
  align-items: stretch;
}

.article-main {
  min-width: 0;
  display: flex;
  flex-direction: column;
}

.article-side {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.article-cover {
  position: relative;
  overflow: hidden;
  border-radius: 18px;
  min-height: 142px;
  background: color-mix(in srgb, var(--accent-light) 75%, var(--bg-card));
  border: 1px solid color-mix(in srgb, var(--border-color) 85%, transparent);
}

.article-cover img {
  width: 100%;
  height: 100%;
  display: block;
  object-fit: cover;
}

.article-meta {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 0.85rem;
  color: var(--text-tertiary);
  margin-bottom: 14px;
}

.dot {
  color: var(--border-color);
}

.article-title {
  font-size: 1.35rem;
  font-weight: 600;
  margin-bottom: 12px;
  line-height: 1.4;
}

.article-title a {
  color: var(--text-primary);
  text-decoration: none;
  transition: color 0.25s ease;
}

.article-card:hover .article-title a {
  color: var(--accent);
}

.article-excerpt {
  color: var(--text-secondary);
  line-height: 1.8;
  margin-bottom: 18px;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.article-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 16px;
}

.article-footer.with-cover {
  margin-top: auto;
}

.tags {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.tag {
  background: var(--accent-light);
  color: var(--accent);
  padding: 5px 14px;
  border-radius: 14px;
  font-size: 0.85rem;
  font-weight: 500;
  transition: all 0.2s ease;
}

.tag:hover {
  background: var(--accent);
  color: #fff;
  transform: scale(1.05);
}

.clickable-tag {
  cursor: pointer;
  position: relative;
  padding-right: 24px;
}

.clickable-tag::after {
  content: '>';
  position: absolute;
  right: 10px;
  top: 50%;
  transform: translateY(-50%);
  font-size: 0.72rem;
  opacity: 0.75;
}

.read-more {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  color: var(--accent);
  text-decoration: none;
  font-weight: 500;
  transition: all 0.25s ease;
}

.read-more:hover {
  gap: 10px;
}

.read-more svg {
  transition: transform 0.25s ease;
}

.read-more:hover svg {
  transform: translateX(3px);
}

.read-more-side {
  width: 100%;
  justify-content: center;
  padding: 12px 14px;
  border-radius: 14px;
  background: color-mix(in srgb, var(--accent-light) 72%, transparent);
}

.read-more-side:hover {
  background: color-mix(in srgb, var(--accent-light) 92%, transparent);
}

.sidebar {
  width: 300px;
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
  gap: 0;
}

.sidebar-trigger {
  width: 100%;
  height: 1px;
  margin-top: -1px;
  pointer-events: none;
}

.sidebar-shell {
  position: sticky;
  top: var(--home-sidebar-sticky-top, 90px);
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.sidebar-primary-stack {
  display: flex;
  flex-direction: column;
  gap: 20px;
  transition: transform 0.28s cubic-bezier(0.22, 1, 0.36, 1), filter 0.24s ease;
  will-change: transform;
}

.sidebar.floating .sidebar-primary-stack {
  transform: translate3d(0, -4px, 0);
  filter: drop-shadow(0 18px 28px color-mix(in srgb, var(--accent) 10%, transparent));
}

.widget {
  background: var(--bg-card);
  border: 1px solid var(--border-color);
  border-radius: 20px;
  padding: 24px;
  opacity: 0;
  animation-fill-mode: forwards;
}

.widget-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 1rem;
  font-weight: 600;
  color: var(--text-primary);
  margin-bottom: 18px;
  padding-bottom: 14px;
  border-bottom: 1px solid var(--border-light);
}

.title-icon {
  width: 18px;
  height: 18px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  color: var(--accent);
}

.title-icon svg {
  width: 100%;
  height: 100%;
}

.author-card {
  display: flex;
  flex-direction: column;
  align-items: center;
  text-align: center;
}

.author-avatar-button {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  padding: 0;
  background: transparent;
  border: 0;
  border-radius: 999px;
  cursor: default;
}

.author-avatar-button.interactive {
  cursor: pointer;
}

.author-avatar-button:focus-visible {
  outline: none;
  box-shadow: 0 0 0 5px color-mix(in srgb, var(--accent-light) 90%, transparent);
}

.author-avatar {
  width: 80px;
  height: 80px;
  border-radius: 50%;
  object-fit: cover;
  border: 3px solid var(--accent);
  transition: transform 0.3s ease, box-shadow 0.3s ease, border-color 0.3s ease;
}

.author-avatar-button:hover .author-avatar,
.author-avatar-button:focus-visible .author-avatar {
  transform: scale(1.08);
  box-shadow: 0 16px 34px color-mix(in srgb, var(--accent) 20%, transparent);
}

.author-avatar-button.expanded .author-avatar {
  box-shadow: 0 18px 36px color-mix(in srgb, var(--accent) 24%, transparent);
}

.author-avatar-default {
  width: 80px;
  height: 80px;
  border-radius: 50%;
  background: var(--gradient-primary);
  color: #fff;
  font-size: 2rem;
  font-weight: 700;
  display: flex;
  align-items: center;
  justify-content: center;
  border: 3px solid var(--accent);
  transition: transform 0.3s ease, box-shadow 0.3s ease;
}

.author-avatar-guest {
  width: 80px;
  height: 80px;
  border-radius: 50%;
  background: var(--bg-tertiary);
  color: var(--text-tertiary);
  display: flex;
  align-items: center;
  justify-content: center;
  border: 2px dashed var(--border-color);
  transition: transform 0.3s ease, border-color 0.3s ease, color 0.3s ease, box-shadow 0.3s ease;
}

.author-avatar-button:hover .author-avatar-default,
.author-avatar-button:focus-visible .author-avatar-default,
.author-avatar-button:hover .author-avatar-guest,
.author-avatar-button:focus-visible .author-avatar-guest {
  transform: scale(1.08);
  box-shadow: 0 16px 34px color-mix(in srgb, var(--accent) 18%, transparent);
}

.author-avatar-button.expanded .author-avatar-default,
.author-avatar-button.expanded .author-avatar-guest {
  transform: scale(1.04);
  box-shadow: 0 16px 34px color-mix(in srgb, var(--accent) 18%, transparent);
}

.author-name {
  font-size: 1.1rem;
  font-weight: 600;
  color: var(--text-primary);
  margin-top: 14px;
}

.author-name.guest {
  color: var(--text-secondary);
  font-weight: 400;
}

.author-bio {
  color: var(--text-secondary);
  font-size: 0.9rem;
  margin-top: 12px;
  max-width: 100%;
  word-break: break-word;
}

.author-secret-bio {
  color: var(--text-secondary);
  font-size: 0.9rem;
  margin-top: 12px;
  padding: 12px 14px;
  border-radius: 18px;
  background: color-mix(in srgb, var(--accent-light) 56%, var(--bg-tertiary));
  line-height: 1.8;
  word-break: break-word;
}

.author-bio-reveal-enter-active,
.author-bio-reveal-leave-active {
  transition: opacity 0.22s ease, transform 0.28s cubic-bezier(0.22, 1, 0.36, 1), max-height 0.28s cubic-bezier(0.22, 1, 0.36, 1);
  overflow: hidden;
}

.author-bio-reveal-enter-from,
.author-bio-reveal-leave-to {
  opacity: 0;
  transform: translateY(-8px);
  max-height: 0;
}

.author-bio-reveal-enter-to,
.author-bio-reveal-leave-from {
  opacity: 1;
  transform: translateY(0);
  max-height: 180px;
}

.tags-cloud {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.tag-item {
  background: var(--bg-tertiary);
  color: var(--text-secondary);
  padding: 7px 16px;
  border-radius: 16px;
  font-size: 0.85rem;
  cursor: pointer;
  transition: all 0.25s cubic-bezier(0.4, 0, 0.2, 1);
}

.tag-item:hover {
  background: var(--accent);
  color: #fff;
  transform: translateY(-2px) scale(1.05);
}

.friend-links {
  list-style: none;
  padding: 0;
  margin: 0;
}

.friend-links-panel {
  overflow: hidden;
  transform-origin: top center;
  will-change: opacity, transform, max-height;
}

.links-widget {
  opacity: 1;
  animation: none;
}

.friend-links-toggle-enter-active,
.friend-links-toggle-leave-active {
  overflow: hidden;
  transition: opacity 0.24s ease, transform 0.28s cubic-bezier(0.22, 1, 0.36, 1), max-height 0.28s cubic-bezier(0.22, 1, 0.36, 1), margin-top 0.28s ease;
}

.friend-links-toggle-enter-from,
.friend-links-toggle-leave-to {
  opacity: 0;
  transform: translate3d(0, -18px, 0) scale(0.96);
  max-height: 0 !important;
  margin-top: -8px;
}

.friend-links-toggle-enter-to,
.friend-links-toggle-leave-from {
  opacity: 1;
  transform: translate3d(0, 0, 0) scale(1);
  max-height: 420px;
  margin-top: 0;
}

.link-item {
  border-bottom: 1px solid var(--border-light);
}

.link-item:last-child {
  border-bottom: none;
}

.link-item a {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 0;
  color: var(--text-secondary);
  text-decoration: none;
  transition: all 0.25s ease;
}

.link-item a:hover {
  color: var(--accent);
}

.link-item a:hover svg {
  transform: translate(3px, -3px);
}

.link-item a svg {
  color: var(--text-tertiary);
  transition: transform 0.25s ease;
}

.link-name {
  font-size: 0.95rem;
}

.empty-text {
  color: var(--text-tertiary);
  font-size: 0.9rem;
}

.footer {
  background: var(--bg-card);
  padding: 28px;
  text-align: center;
  color: var(--text-tertiary);
  font-size: 0.9rem;
  border-top: 1px solid var(--border-color);
  margin-top: 40px;
}

@media (max-width: 900px) {
  .container {
    flex-direction: column;
  }

  .sidebar {
    width: 100%;
  }

  .sidebar-trigger {
    display: none;
  }

  .sidebar-shell {
    position: static;
    top: auto;
  }

  .sidebar-primary-stack {
    transform: none;
    filter: none;
  }

  .article-card-layout {
    grid-template-columns: 1fr;
  }

  .article-side {
    order: -1;
  }

  .article-cover {
    min-height: 190px;
  }
}

@media (max-width: 768px) {
  .main-content {
    padding: 24px 0;
  }

  .container {
    padding: 0 16px;
    gap: 24px;
  }

  .article-card {
    padding: 20px;
  }

  .article-title {
    font-size: 1.15rem;
  }

  .article-footer {
    flex-wrap: wrap;
  }
}
</style>
