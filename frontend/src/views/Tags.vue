<template>
  <div class="tags-page">
    <TopHeader @toggle-music="uiStore.toggleMusicPlayer()" />

    <main class="main-content">
      <div class="container">
        <div v-if="!selectedTag" class="tags-view animate-fade-in">
          <div class="page-header">
            <h1 class="page-title">
              <span class="title-icon">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.85" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
                  <path d="M20 13l-7 7-9-9V4h7z" />
                  <circle cx="7.5" cy="7.5" r="1" />
                </svg>
              </span>
              {{ t('tags.title') }}
            </h1>
            <p class="page-desc">{{ t('tags.desc') }}</p>
          </div>

          <div v-if="loading" class="loading-state">
            <div class="loading-spinner"></div>
          </div>

          <div v-else-if="tags.length === 0" class="empty-state animate-scale-in">
            <div class="empty-icon">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.85" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
                <path d="M20 13l-7 7-9-9V4h7z" />
                <circle cx="7.5" cy="7.5" r="1" />
              </svg>
            </div>
            <div class="empty-text">{{ t('tags.emptyTitle') }}</div>
            <div class="empty-desc">{{ t('tags.emptyDesc') }}</div>
          </div>

          <div v-else class="tags-container">
            <button
              v-for="(tag, index) in tags"
              :key="tag.name"
              class="tag-item animate-scale-in"
              :style="{ animationDelay: `${index * 0.03}s` }"
              @click="selectTag(tag.name)"
            >
              <span class="tag-name">{{ tag.name }}</span>
              <span class="tag-count">{{ tag.count }}</span>
            </button>
          </div>
        </div>

        <transition name="slide-in">
          <div v-if="selectedTag" class="tag-articles-view">
            <div class="page-header">
              <button class="back-btn" @click="clearSelection">
                <svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="2">
                  <path d="M19 12H5M12 19l-7-7 7-7" />
                </svg>
                <span>{{ t('tags.backToList') }}</span>
              </button>
              <h1 class="page-title">
                <span class="title-icon">
                  <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.85" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
                    <path d="M20 13l-7 7-9-9V4h7z" />
                    <circle cx="7.5" cy="7.5" r="1" />
                  </svg>
                </span>
                {{ selectedTag }}
                <span class="article-count">{{ t('tags.articleCount', { count: totalArticles }) }}</span>
              </h1>
            </div>

            <div v-if="articlesLoading" class="loading-state">
              <div class="loading-spinner"></div>
            </div>

            <div v-else-if="articles.length === 0" class="empty-state animate-scale-in">
              <div class="empty-icon">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.85" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
                  <path d="M9 9h6" />
                  <path d="M8 13h8" />
                  <path d="M6.5 4.5h11A1.5 1.5 0 0 1 19 6v12a1.5 1.5 0 0 1-1.5 1.5h-11A1.5 1.5 0 0 1 5 18V6a1.5 1.5 0 0 1 1.5-1.5Z" />
                </svg>
              </div>
              <div class="empty-text">{{ t('tags.noArticles') }}</div>
            </div>

            <div v-else class="article-list">
              <article
                v-for="(article, index) in articles"
                :key="article.id"
                class="article-card animate-fade-in-up"
                :style="{ animationDelay: `${index * 0.06}s` }"
                @click="goToPost(article)"
              >
                <div class="article-meta">
                  <span class="date">{{ formatDate(article.createTime) }}</span>
                  <span class="dot">•</span>
                  <span class="views">{{ t('home.views', { count: article.viewCount || 0 }) }}</span>
                </div>
                <h2 class="article-title">
                  <a href="#">{{ article.title }}</a>
                </h2>
                <p v-if="article.excerpt || article.summary" class="article-excerpt">{{ article.excerpt || article.summary }}</p>
                <div class="article-footer">
                  <div class="tags">
                    <span
                      v-if="article.category"
                      class="tag clickable-tag"
                      @click.stop="goToCategory(article.category)"
                    >
                      {{ article.category }}
                    </span>
                    <span
                      v-for="currentTag in getTags(article.tags)"
                      :key="currentTag"
                      class="tag clickable-tag"
                      @click.stop="selectTag(currentTag)"
                    >
                      {{ currentTag }}
                    </span>
                  </div>
                  <a href="#" class="read-more" @click.prevent.stop="goToPost(article)">
                    {{ t('common.readMore') }}
                    <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="2">
                      <path d="M5 12h14M12 5l7 7-7 7" />
                    </svg>
                  </a>
                </div>
              </article>

              <div v-if="hasMoreArticles || loadingMoreArticles" ref="articlesSentinelRef" class="waterfall-sentinel" aria-hidden="true">
                <div class="loading-spinner loading-spinner-inline"></div>
              </div>
            </div>
          </div>
        </transition>
      </div>
    </main>
    <footer class="footer">
      <p>{{ t('common.copyright', { year: 2026 }) }}</p>
    </footer>
  </div>
</template>

<script setup>
import { computed, ref, onMounted, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useI18n } from 'vue-i18n'
import TopHeader from '@/components/TopHeader.vue'
import { useLoadMoreTrigger } from '@/composables/useLoadMoreTrigger'
import { useUiStore } from '@/stores'
import { publicApi } from '@/api'
import { parseTagList } from '@/utils/tags'

const ARTICLE_PAGE_SIZE = 6

const router = useRouter()
const route = useRoute()
const uiStore = useUiStore()
const { locale, t } = useI18n()
const loading = ref(true)
const articlesLoading = ref(false)
const loadingMoreArticles = ref(false)
const selectedTag = ref('')
const tags = ref([])
const articles = ref([])
const currentPage = ref(1)
const totalArticles = ref(0)
const hasMoreArticles = computed(() => articles.value.length < totalArticles.value)
const { sentinelRef: articlesSentinelRef } = useLoadMoreTrigger(() => {
  void loadMoreArticles()
}, {
  enabled: computed(() => !loading.value && !articlesLoading.value && !loadingMoreArticles.value && Boolean(selectedTag.value) && hasMoreArticles.value)
})

const dateLocale = computed(() => (locale.value === 'en' ? 'en-US' : locale.value))

const getTags = (tagsValue) => parseTagList(tagsValue, { limit: 3 })
const normalizeRouteValue = (value) => String(value || '').trim()

const fetchTags = async ({ silent = false } = {}) => {
  if (!silent) {
    loading.value = true
  }
  try {
    const res = await publicApi.getPostTags()
    tags.value = res.data || []

    if (route.query.tag && route.query.tag !== selectedTag.value) {
      await selectTag(route.query.tag)
    }
  } catch (error) {
    console.error('加载标签失败:', error)
  } finally {
    if (!silent) {
      loading.value = false
    }
  }
}

const mergeArticles = (incomingArticles, reset = false) => {
  if (reset) {
    articles.value = incomingArticles
    return
  }

  const existingIds = new Set(articles.value.map((article) => article.id))
  articles.value = [...articles.value, ...incomingArticles.filter((article) => !existingIds.has(article.id))]
}

const requestTagArticles = async (tagName, page) => {
  const res = await publicApi.getPosts({
    page,
    size: ARTICLE_PAGE_SIZE,
    tag: tagName
  })
  return res.data || {}
}

const applyArticlesPage = (pageData, targetPage, reset = false) => {
  const records = pageData.records || []
  mergeArticles(records, reset)
  currentPage.value = Number(pageData.current || targetPage)
  totalArticles.value = Number(pageData.total || articles.value.length)
}

const fetchTagArticles = async ({ reset = false } = {}) => {
  if (!selectedTag.value) {
    articles.value = []
    totalArticles.value = 0
    currentPage.value = 1
    return
  }

  const targetPage = reset ? 1 : currentPage.value + 1
  if (reset) {
    articlesLoading.value = true
  } else {
    loadingMoreArticles.value = true
  }

  try {
    const pageData = await requestTagArticles(selectedTag.value, targetPage)
    applyArticlesPage(pageData, targetPage, reset)
  } catch (error) {
    console.error('加载标签文章失败:', error)
  } finally {
    if (reset) {
      articlesLoading.value = false
    } else {
      loadingMoreArticles.value = false
    }
  }
}

const resetSelectionState = () => {
  selectedTag.value = ''
  articles.value = []
  totalArticles.value = 0
  currentPage.value = 1
}

const selectTag = async (tagName) => {
  if (!tagName) return
  if (articlesLoading.value || loadingMoreArticles.value) return

  const nextTag = normalizeRouteValue(tagName)
  if (!nextTag || nextTag === selectedTag.value) return

  articlesLoading.value = true
  try {
    const pageData = await requestTagArticles(nextTag, 1)
    selectedTag.value = nextTag
    articles.value = []
    totalArticles.value = 0
    currentPage.value = 1
    applyArticlesPage(pageData, 1, true)

    if (route.query.tag !== nextTag) {
      await router.replace({ query: { tag: nextTag } })
    }
    window.scrollTo({ top: 0, behavior: 'smooth' })
  } catch (error) {
    console.error('加载标签文章失败:', error)
  } finally {
    articlesLoading.value = false
  }
}

const clearSelection = () => {
  resetSelectionState()
  router.replace({ query: {} })
  window.scrollTo({ top: 0, behavior: 'smooth' })
}

const loadMoreArticles = async () => {
  if (loading.value || articlesLoading.value || loadingMoreArticles.value || !hasMoreArticles.value) {
    return
  }

  await fetchTagArticles()
}

const goToCategory = (category) => {
  if (!category) return
  router.push({ path: '/categories', query: { category } })
}

const formatDate = (date) => {
  if (!date) return '-'
  return new Date(date).toLocaleDateString(dateLocale.value, {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit'
  })
}

const goToPost = (article) => {
  router.push({
    path: `/post/${article.id}`,
    query: { tag: selectedTag.value }
  })
}

onMounted(() => {
  const initialTag = normalizeRouteValue(route.query.tag)
  if (initialTag) {
    selectedTag.value = initialTag
    void Promise.all([
      fetchTagArticles({ reset: true }),
      fetchTags({ silent: true })
    ]).finally(() => {
      loading.value = false
    })
    return
  }

  void fetchTags()
})

watch(
  () => route.query.tag,
  async (tag) => {
    if (loading.value) return

    const nextTag = normalizeRouteValue(tag)

    if (nextTag && nextTag !== selectedTag.value) {
      await selectTag(nextTag)
    } else if (!tag && selectedTag.value) {
      resetSelectionState()
    }
  }
)
</script>

<style scoped>
.tags-page {
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
  max-width: 900px;
  margin: 0 auto;
  padding: 0 24px;
}

.page-header {
  margin-bottom: 36px;
}

.page-title {
  display: flex;
  align-items: center;
  gap: 12px;
  font-size: 2rem;
  font-weight: 700;
  color: var(--text-primary);
  margin: 0 0 12px;
}

.title-icon {
  width: 30px;
  height: 30px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  color: var(--accent);
}

.title-icon svg,
.empty-icon svg {
  width: 100%;
  height: 100%;
}

.article-count {
  font-size: 1rem;
  font-weight: 400;
  color: var(--text-tertiary);
}

.page-desc {
  color: var(--text-secondary);
  line-height: 1.8;
  margin: 0;
}

.tags-container {
  display: flex;
  flex-wrap: wrap;
  gap: 14px;
}

.tag-item {
  display: inline-flex;
  align-items: center;
  gap: 10px;
  padding: 14px 26px;
  background: var(--bg-card);
  border: 1px solid var(--border-color);
  border-radius: 24px;
  cursor: pointer;
  transition: all 0.35s cubic-bezier(0.4, 0, 0.2, 1);
  opacity: 0;
  animation-fill-mode: forwards;
}

.tag-item:hover {
  transform: translateY(-4px) scale(1.05);
  box-shadow: var(--shadow-lg);
  background: var(--accent);
  border-color: var(--accent);
}

.tag-item:hover .tag-name,
.tag-item:hover .tag-count {
  color: #fff;
}

.tag-item:hover .tag-count {
  background: rgba(255, 255, 255, 0.2);
}

.tag-name {
  color: var(--text-primary);
  font-weight: 500;
  font-size: 1rem;
  transition: color 0.25s ease;
}

.tag-count {
  background: var(--bg-tertiary);
  color: var(--text-tertiary);
  padding: 3px 10px;
  border-radius: 12px;
  font-size: 0.85rem;
  transition: all 0.25s ease;
}

.back-btn {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 10px 20px;
  background: var(--bg-tertiary);
  color: var(--text-secondary);
  border: none;
  border-radius: 14px;
  font-size: 0.95rem;
  cursor: pointer;
  transition: all 0.25s ease;
  margin-bottom: 20px;
}

.back-btn:hover {
  background: var(--accent-light);
  color: var(--accent);
  transform: translateX(-4px);
}

.tag-articles-view {
  min-height: calc(100vh - 200px);
}

.loading-state {
  display: flex;
  justify-content: center;
  padding: 80px;
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
  to {
    transform: rotate(360deg);
  }
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
  width: 64px;
  height: 64px;
  margin-bottom: 16px;
  color: var(--accent);
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
}

.article-title a {
  color: var(--text-primary);
  text-decoration: none;
  transition: color 0.25s;
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
}

.clickable-tag {
  cursor: pointer;
  transition: transform 0.2s ease, background 0.2s ease, color 0.2s ease;
}

.clickable-tag:hover {
  background: var(--accent);
  color: #fff;
  transform: translateY(-1px);
}

.read-more {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  color: var(--accent);
  text-decoration: none;
  font-weight: 500;
  transition: gap 0.25s ease;
}

.read-more:hover {
  gap: 10px;
}

.slide-in-enter-active {
  animation: slideIn 0.4s cubic-bezier(0.4, 0, 0.2, 1);
}

.slide-in-leave-active {
  animation: slideOut 0.3s ease-out;
}

@keyframes slideIn {
  from {
    opacity: 0;
    transform: translateX(40px);
  }

  to {
    opacity: 1;
    transform: translateX(0);
  }
}

@keyframes slideOut {
  from {
    opacity: 1;
    transform: translateX(0);
  }

  to {
    opacity: 0;
    transform: translateX(-30px);
  }
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

@media (max-width: 768px) {
  .page-title {
    font-size: 1.5rem;
  }

  .tag-item {
    padding: 10px 18px;
  }

  .article-card {
    padding: 20px;
  }
}
</style>
