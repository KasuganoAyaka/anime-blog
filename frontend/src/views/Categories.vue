<template>
  <div class="categories-page">
    <TopHeader @toggle-music="uiStore.toggleMusicPlayer()" />

    <main class="main-content">
      <div class="container">
        <div v-if="!selectedCategory" class="categories-view animate-fade-in">
          <div class="page-header">
            <h1 class="page-title">
              <span class="title-icon">
                <svg class="title-icon-svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.85" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
                  <component :is="node.tag" v-for="(node, index) in getCategoryIconNodes('library')" :key="`title-${index}`" v-bind="node.attrs" />
                </svg>
              </span>
              {{ t('categories.title') }}
            </h1>
            <p class="page-desc">{{ t('categories.desc') }}</p>
          </div>

          <div v-if="loading" class="loading-state">
            <div class="loading-spinner"></div>
          </div>

          <div v-else-if="categories.length === 0" class="empty-state animate-scale-in">
            <div class="empty-icon">
              <svg class="empty-icon-svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.85" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
                <component :is="node.tag" v-for="(node, index) in getCategoryIconNodes('library')" :key="`empty-library-${index}`" v-bind="node.attrs" />
              </svg>
            </div>
            <div class="empty-text">{{ t('categories.emptyTitle') }}</div>
            <div class="empty-desc">{{ t('categories.emptyDesc') }}</div>
          </div>

          <div v-else class="categories-grid">
            <div
              v-for="(category, index) in categories"
              :key="category.name"
              class="category-card animate-fade-in-up"
              :style="{ animationDelay: `${index * 0.05}s` }"
              @click="selectCategory(category.name)"
            >
              <div class="category-icon">
                <svg class="category-icon-svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.85" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
                  <component :is="node.tag" v-for="(node, iconIndex) in getCategoryIconNodes(category.name)" :key="`${category.name}-${iconIndex}`" v-bind="node.attrs" />
                </svg>
              </div>
              <div class="category-info">
                <h3 class="category-name">{{ category.name }}</h3>
                <span class="article-count">{{ t('categories.articleCount', { count: category.count }) }}</span>
              </div>
              <div class="category-arrow">
                <svg viewBox="0 0 24 24" width="20" height="20" fill="none" stroke="currentColor" stroke-width="2">
                  <path d="M9 18l6-6-6-6" />
                </svg>
              </div>
            </div>
          </div>
        </div>

        <transition name="slide-in">
          <div v-if="selectedCategory" class="category-articles-view">
            <div class="page-header">
              <button class="back-btn" @click="clearSelection">
                <svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="2">
                  <path d="M19 12H5M12 19l-7-7 7-7" />
                </svg>
                <span>{{ t('categories.backToList') }}</span>
              </button>
              <h1 class="page-title">
                <span class="title-icon">
                  <svg class="title-icon-svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.85" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
                    <component :is="node.tag" v-for="(node, index) in getCategoryIconNodes(selectedCategory)" :key="`selected-${index}`" v-bind="node.attrs" />
                  </svg>
                </span>
                {{ selectedCategory }}
                <span class="article-count">{{ t('categories.articleCount', { count: totalArticles }) }}</span>
              </h1>
            </div>

            <div v-if="articlesLoading" class="loading-state">
              <div class="loading-spinner"></div>
            </div>

            <div v-else-if="articles.length === 0" class="empty-state animate-scale-in">
              <div class="empty-icon">
                <svg class="empty-icon-svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.85" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
                  <component :is="node.tag" v-for="(node, index) in getCategoryIconNodes('empty')" :key="`empty-post-${index}`" v-bind="node.attrs" />
                </svg>
              </div>
              <div class="empty-text">{{ t('categories.noArticles') }}</div>
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
                    <span class="tag clickable-tag" v-if="article.category" @click.stop="selectCategory(article.category)">{{ article.category }}</span>
                    <span class="tag clickable-tag" v-for="tag in getTags(article.tags)" :key="tag" @click.stop="goToTag(tag)">{{ tag }}</span>
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
import { POST_CATEGORIES } from '@/constants/postEditor'
import { parseTagList } from '@/utils/tags'

const ARTICLE_PAGE_SIZE = 6

const router = useRouter()
const route = useRoute()
const uiStore = useUiStore()
const { locale, t } = useI18n()
const loading = ref(true)
const articlesLoading = ref(false)
const loadingMoreArticles = ref(false)
const selectedCategory = ref('')
const categories = ref([])
const articles = ref([])
const currentPage = ref(1)
const totalArticles = ref(0)
const hasMoreArticles = computed(() => articles.value.length < totalArticles.value)
const { sentinelRef: articlesSentinelRef } = useLoadMoreTrigger(() => {
  void loadMoreArticles()
}, {
  enabled: computed(() => !loading.value && !articlesLoading.value && !loadingMoreArticles.value && Boolean(selectedCategory.value) && hasMoreArticles.value)
})
const predefinedCategories = POST_CATEGORIES

const dateLocale = computed(() => (locale.value === 'en' ? 'en-US' : locale.value))
const normalizeRouteValue = (value) => String(value || '').trim()

const categoryIcons = {
  library: [
    { tag: 'path', attrs: { d: 'M5.5 6.5A2.5 2.5 0 0 1 8 4h10.5v15.5H8a2.5 2.5 0 0 0-2.5 2.5' } },
    { tag: 'path', attrs: { d: 'M5.5 6.5v13A2.5 2.5 0 0 1 8 17h10.5' } }
  ],
  闲聊: [
    { tag: 'path', attrs: { d: 'M6 17.5 4.5 20v-4A7.5 7.5 0 1 1 7 18.7' } },
    { tag: 'path', attrs: { d: 'M8 10h8' } },
    { tag: 'path', attrs: { d: 'M8 13.5h5' } }
  ],
  软件: [
    { tag: 'rect', attrs: { x: '4', y: '4', width: '7', height: '7', rx: '2' } },
    { tag: 'rect', attrs: { x: '13', y: '4', width: '7', height: '7', rx: '2' } },
    { tag: 'rect', attrs: { x: '8.5', y: '13', width: '7', height: '7', rx: '2' } }
  ],
  经验分享: [
    { tag: 'path', attrs: { d: 'M7 4.5h8l4 4V19a1.5 1.5 0 0 1-1.5 1.5h-10A1.5 1.5 0 0 1 6 19V6A1.5 1.5 0 0 1 7.5 4.5Z' } },
    { tag: 'path', attrs: { d: 'M15 4.5V9h4' } },
    { tag: 'path', attrs: { d: 'M9 12h6' } },
    { tag: 'path', attrs: { d: 'M9 15.5h4.5' } }
  ],
  项目分享: [
    { tag: 'path', attrs: { d: 'M4.5 8.5h15v9a2 2 0 0 1-2 2h-11a2 2 0 0 1-2-2v-9Z' } },
    { tag: 'path', attrs: { d: 'M9 8.5 10.3 6h3.4L15 8.5' } }
  ],
  数码前线: [
    { tag: 'rect', attrs: { x: '7', y: '3.5', width: '10', height: '17', rx: '2.5' } },
    { tag: 'path', attrs: { d: 'M10 6.5h4' } },
    { tag: 'circle', attrs: { cx: '12', cy: '17', r: '0.8' } }
  ],
  前端: [
    { tag: 'rect', attrs: { x: '3.5', y: '5', width: '17', height: '13.5', rx: '2.5' } },
    { tag: 'path', attrs: { d: 'M3.5 9h17' } },
    { tag: 'path', attrs: { d: 'm10 12-2.5 2.5L10 17' } },
    { tag: 'path', attrs: { d: 'm14 12 2.5 2.5L14 17' } }
  ],
  后端: [
    { tag: 'rect', attrs: { x: '4', y: '4.5', width: '16', height: '6', rx: '2' } },
    { tag: 'rect', attrs: { x: '4', y: '13.5', width: '16', height: '6', rx: '2' } },
    { tag: 'path', attrs: { d: 'M8 7.5h.01' } },
    { tag: 'path', attrs: { d: 'M8 16.5h.01' } },
    { tag: 'path', attrs: { d: 'M12 7.5h4' } },
    { tag: 'path', attrs: { d: 'M12 16.5h4' } }
  ],
  生活: [
    { tag: 'path', attrs: { d: 'M18.5 5.5c-5 0-9 4-9 9 0 1.9.6 3.6 1.6 5 4.8-.4 8.4-4.5 8.4-9.3 0-1.7-.4-3.3-1-4.7Z' } },
    { tag: 'path', attrs: { d: 'M9.5 14.5c1.2.1 2.4-.1 3.5-.7' } }
  ],
  技术: [
    { tag: 'path', attrs: { d: 'M14.5 5.5 18.5 9.5' } },
    { tag: 'path', attrs: { d: 'm9 20-5-5 9.5-9.5 5 5Z' } },
    { tag: 'path', attrs: { d: 'M12.5 7.5 16.5 11.5' } }
  ],
  资源优选: [
    { tag: 'path', attrs: { d: 'M12 3.8 19 7.5v9L12 20.2 5 16.5v-9L12 3.8Z' } },
    { tag: 'path', attrs: { d: 'M5 7.5 12 11.2l7-3.7' } },
    { tag: 'path', attrs: { d: 'M12 11.2v9' } }
  ],
  游戏: [
    { tag: 'path', attrs: { d: 'M7.5 10h9a3 3 0 0 1 2.9 3.7l-1 3.4a2 2 0 0 1-3.5.7l-1.4-1.8h-3L9 17.8a2 2 0 0 1-3.5-.7l-1-3.4A3 3 0 0 1 7.5 10Z' } },
    { tag: 'path', attrs: { d: 'M8 13.5h3' } },
    { tag: 'path', attrs: { d: 'M9.5 12v3' } },
    { tag: 'circle', attrs: { cx: '15.5', cy: '13', r: '0.7' } },
    { tag: 'circle', attrs: { cx: '17.5', cy: '15', r: '0.7' } }
  ],
  职场与成长: [
    { tag: 'path', attrs: { d: 'M5 18.5h14' } },
    { tag: 'path', attrs: { d: 'M7.5 15.5V11' } },
    { tag: 'path', attrs: { d: 'M12 15.5V8' } },
    { tag: 'path', attrs: { d: 'M16.5 15.5V5.5' } },
    { tag: 'path', attrs: { d: 'm6 9.5 3-3 3 2.5 4.5-4.5' } }
  ],
  empty: [
    { tag: 'path', attrs: { d: 'M7 4.5h8l4 4V19a1.5 1.5 0 0 1-1.5 1.5h-10A1.5 1.5 0 0 1 6 19V6A1.5 1.5 0 0 1 7.5 4.5Z' } },
    { tag: 'path', attrs: { d: 'M15 4.5V9h4' } },
    { tag: 'path', attrs: { d: 'M9 12h6' } },
    { tag: 'path', attrs: { d: 'm10 16 4-4' } }
  ],
  default: [
    { tag: 'path', attrs: { d: 'M5.5 6.5A2.5 2.5 0 0 1 8 4h10.5v15.5H8a2.5 2.5 0 0 0-2.5 2.5' } },
    { tag: 'path', attrs: { d: 'M5.5 6.5v13A2.5 2.5 0 0 1 8 17h10.5' } }
  ]
}

const getCategoryIconNodes = (name) => categoryIcons[name] || categoryIcons.default

const getTags = (tagsValue) => parseTagList(tagsValue, { limit: 3 })

const fetchData = async ({ silent = false } = {}) => {
  if (!silent) {
    loading.value = true
  }
  try {
    const res = await publicApi.getPostCategories()
    const fetchedCategories = res.data || []
    const fetchedMap = new Map(fetchedCategories.map((item) => [item.name, item.count]))
    const categoryOrder = new Map(predefinedCategories.map((name, index) => [name, index]))

    const mergedCategories = predefinedCategories.map((name) => ({
      name,
      count: fetchedMap.get(name) || 0
    }))

    fetchedCategories.forEach((item) => {
      if (!categoryOrder.has(item.name)) {
        mergedCategories.push(item)
      }
    })

    categories.value = mergedCategories.sort((left, right) => {
      const leftOrder = categoryOrder.has(left.name) ? categoryOrder.get(left.name) : Number.MAX_SAFE_INTEGER
      const rightOrder = categoryOrder.has(right.name) ? categoryOrder.get(right.name) : Number.MAX_SAFE_INTEGER

      if (leftOrder !== rightOrder) {
        return leftOrder - rightOrder
      }
      if (leftOrder !== Number.MAX_SAFE_INTEGER) {
        return 0
      }
      if (left.count !== right.count) {
        return right.count - left.count
      }
      return left.name.localeCompare(right.name, 'zh-CN')
    })

    if (route.query.category && route.query.category !== selectedCategory.value) {
      await selectCategory(route.query.category)
    }
  } catch (error) {
    console.error('加载分类失败:', error)
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

const requestCategoryArticles = async (categoryName, page) => {
  const res = await publicApi.getPosts({
    page,
    size: ARTICLE_PAGE_SIZE,
    category: categoryName
  })
  return res.data || {}
}

const applyArticlesPage = (pageData, targetPage, reset = false) => {
  const records = pageData.records || []
  mergeArticles(records, reset)
  currentPage.value = Number(pageData.current || targetPage)
  totalArticles.value = Number(pageData.total || articles.value.length)
}

const fetchCategoryArticles = async ({ reset = false } = {}) => {
  if (!selectedCategory.value) {
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
    const pageData = await requestCategoryArticles(selectedCategory.value, targetPage)
    applyArticlesPage(pageData, targetPage, reset)
  } catch (error) {
    console.error('加载分类文章失败:', error)
  } finally {
    if (reset) {
      articlesLoading.value = false
    } else {
      loadingMoreArticles.value = false
    }
  }
}

const resetSelectionState = () => {
  selectedCategory.value = ''
  articles.value = []
  totalArticles.value = 0
  currentPage.value = 1
}

const selectCategory = async (categoryName) => {
  if (!categoryName) return
  if (articlesLoading.value || loadingMoreArticles.value) return

  const nextCategory = normalizeRouteValue(categoryName)
  if (!nextCategory || nextCategory === selectedCategory.value) return

  articlesLoading.value = true
  try {
    const pageData = await requestCategoryArticles(nextCategory, 1)
    selectedCategory.value = nextCategory
    articles.value = []
    totalArticles.value = 0
    currentPage.value = 1
    applyArticlesPage(pageData, 1, true)

    if (route.query.category !== nextCategory) {
      await router.replace({ query: { category: nextCategory } })
    }
    window.scrollTo({ top: 0, behavior: 'smooth' })
  } catch (error) {
    console.error('加载分类文章失败:', error)
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

  await fetchCategoryArticles()
}

const goToTag = (tag) => {
  if (!tag) return
  router.push({ path: '/tags', query: { tag } })
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
    query: { category: selectedCategory.value }
  })
}

onMounted(() => {
  const initialCategory = normalizeRouteValue(route.query.category)
  if (initialCategory) {
    selectedCategory.value = initialCategory
    void Promise.all([
      fetchCategoryArticles({ reset: true }),
      fetchData({ silent: true })
    ]).finally(() => {
      loading.value = false
    })
    return
  }

  void fetchData()
})

watch(() => route.query.category, async (category) => {
  if (loading.value) return

  const nextCategory = normalizeRouteValue(category)

  if (nextCategory && nextCategory !== selectedCategory.value) {
    await selectCategory(nextCategory)
  } else if (!category && selectedCategory.value) {
    resetSelectionState()
  }
})
</script>

<style scoped>
.categories-page {
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
  width: 34px;
  height: 34px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  color: var(--accent);
}

.title-icon-svg {
  width: 28px;
  height: 28px;
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

.categories-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(240px, 1fr));
  gap: 18px;
}

.category-card {
  background: var(--bg-card);
  border: 1px solid var(--border-color);
  border-radius: 20px;
  padding: 24px;
  display: flex;
  align-items: center;
  gap: 16px;
  cursor: pointer;
  transition: all 0.35s cubic-bezier(0.4, 0, 0.2, 1);
  opacity: 0;
  animation-fill-mode: forwards;
}

.category-card:hover {
  transform: translateY(-6px) scale(1.02);
  box-shadow: var(--shadow-lg);
  border-color: var(--accent);
}

.category-icon {
  flex-shrink: 0;
  width: 50px;
  height: 50px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--bg-tertiary);
  border-radius: 14px;
  color: var(--accent);
}

.category-icon-svg {
  width: 26px;
  height: 26px;
}

.category-info {
  flex: 1;
  min-width: 0;
}

.category-name {
  font-size: 1.1rem;
  font-weight: 600;
  color: var(--text-primary);
  margin-bottom: 4px;
}

.category-arrow {
  color: var(--text-tertiary);
  transition: transform 0.25s ease;
}

.category-card:hover .category-arrow {
  color: var(--accent);
  transform: translateX(4px);
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
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 20px;
  background: var(--bg-tertiary);
  color: var(--accent);
}

.empty-icon-svg {
  width: 36px;
  height: 36px;
}

.empty-text { font-size: 1.25rem; font-weight: 600; color: var(--text-primary); margin-bottom: 8px; }
.empty-desc { color: var(--text-secondary); }

.category-articles-view {
  min-height: calc(100vh - 200px);
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

.dot { color: var(--border-color); }

.article-title { font-size: 1.35rem; font-weight: 600; margin-bottom: 12px; }
.article-title a { color: var(--text-primary); text-decoration: none; transition: color 0.25s; }
.article-card:hover .article-title a { color: var(--accent); }

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

.tags { display: flex; gap: 8px; flex-wrap: wrap; }
.tag {
  background: var(--accent-light);
  color: var(--accent);
  padding: 5px 14px;
  border-radius: 14px;
  font-size: 0.85rem;
}

.clickable-tag {
  cursor: pointer;
  position: relative;
  padding-right: 24px;
  transition: transform 0.2s ease, background 0.2s ease, color 0.2s ease;
}

.clickable-tag:hover {
  background: var(--accent);
  color: #fff;
  transform: translateY(-1px);
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
  transition: gap 0.25s ease;
}

.read-more:hover { gap: 10px; }

.slide-in-enter-active {
  animation: slideIn 0.4s cubic-bezier(0.4, 0, 0.2, 1);
}

.slide-in-leave-active {
  animation: slideOut 0.3s ease-out;
}

@keyframes slideIn {
  from { opacity: 0; transform: translateX(40px); }
  to { opacity: 1; transform: translateX(0); }
}

@keyframes slideOut {
  from { opacity: 1; transform: translateX(0); }
  to { opacity: 0; transform: translateX(-30px); }
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
  .categories-grid {
    grid-template-columns: 1fr;
  }

  .page-title { font-size: 1.5rem; }
  .article-card { padding: 20px; }
}
</style>
