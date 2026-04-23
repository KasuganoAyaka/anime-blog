<template>
  <div class="post-page">
    <TopHeader @toggle-music="uiStore.toggleMusicPlayer()" />

    <main class="post-main">
      <div v-if="loading" class="state-card animate-fade-in">
        <div class="loading-spinner"></div>
        <span>{{ t('common.loading') }}</span>
      </div>

      <div v-else-if="!post" class="state-card animate-scale-in">
        <div class="state-icon">📄</div>
        <div class="state-text">{{ t('postDetail.notFound') }}</div>
      </div>

      <div v-else class="post-shell">
        <header class="post-header hero-card animate-fade-in-down">
          <div class="hero-cover" :class="{ empty: !post.coverImage }">
            <img
              v-if="post.coverImage"
              :src="post.coverImage"
              :alt="post.title"
              class="hero-cover-image"
              loading="eager"
            />
            <div class="hero-cover-fallback" aria-hidden="true"></div>
            <div class="hero-cover-overlay"></div>
          </div>

          <div class="hero-body">
            <button class="back-link" @click="goBack">
              <svg viewBox="0 0 24 24" aria-hidden="true">
                <path d="M19 12H5M12 19l-7-7 7-7" />
              </svg>
              <span>{{ t('postDetail.back') }}</span>
            </button>

            <div class="hero-badges">
              <span class="hero-badge">{{ formattedDate }}</span>
              <span class="hero-badge">{{ t('home.views', { count: post.viewCount || 0 }) }}</span>
              <span class="hero-badge">{{ copy.readingTime(readingMinutes) }}</span>
            </div>

            <h1>{{ post.title }}</h1>

            <div v-if="post.category || tagList.length" class="hero-tags">
              <button
                v-if="post.category"
                type="button"
                class="hero-tag hero-tag-category"
                @click="goToCategory(post.category)"
              >
                {{ post.category }}
              </button>
              <span v-else class="hero-tag hero-tag-category is-static">{{ copy.uncategorized }}</span>
              <button
                v-for="tag in tagList"
                :key="tag"
                type="button"
                class="hero-tag"
                @click="goToTag(tag)"
              >
                # {{ tag }}
              </button>
            </div>
          </div>
        </header>

        <div class="post-layout">
          <div class="post-primary">
            <article class="content-card animate-fade-in-up">
              <div class="content-card-head">
                <div class="content-card-title">
                  <span class="section-chip">{{ copy.articleLabel }}</span>
                  <span>{{ copy.articleSubline(headingCount) }}</span>
                </div>
                <div class="content-card-meta">
                  <span>{{ formattedDate }}</span>
                  <span>{{ copy.readingTime(readingMinutes) }}</span>
                </div>
              </div>

              <div class="post-content">
                <MarkdownPreview :source="post.content || ''" :preview-id="previewId" />
              </div>
            </article>

            <PostComments
              class="animate-fade-in-up"
              :post-id="post.id || route.params.id"
            />
          </div>

          <aside class="post-sidebar animate-fade-in-up">
            <section class="sidebar-card toc-card">
              <div class="sidebar-title">{{ copy.catalogLabel }}</div>
              <div v-if="catalogItems.length > 0" ref="catalogWrapRef" class="catalog-wrap">
                <button
                  v-for="item in catalogItems"
                  :key="item.id"
                  type="button"
                  class="catalog-link"
                  :class="[`level-${Math.min(item.level, 4)}`, { active: activeCatalogId === item.id }]"
                  :style="{ '--catalog-level': item.level - 1 }"
                  :aria-current="activeCatalogId === item.id ? 'true' : undefined"
                  @click="scrollToHeading(item.id)"
                >
                  <span class="catalog-level-badge">H{{ item.level }}</span>
                  <span>{{ item.text }}</span>
                </button>
              </div>
              <div v-else class="catalog-empty">
                {{ copy.catalogEmpty }}
              </div>
            </section>

            <section class="sidebar-card meta-card">
              <div class="sidebar-title">{{ copy.articleInfo }}</div>
              <div class="meta-list">
                <div class="meta-row">
                  <span>{{ copy.publishLabel }}</span>
                  <strong>{{ formattedDate }}</strong>
                </div>
                <div class="meta-row">
                  <span>{{ copy.viewLabel }}</span>
                  <strong>{{ post.viewCount || 0 }}</strong>
                </div>
                <div class="meta-row">
                  <span>{{ copy.readingLabel }}</span>
                  <strong>{{ copy.readingTime(readingMinutes) }}</strong>
                </div>
                <div class="meta-row">
                  <span>{{ copy.headingLabel }}</span>
                  <strong>{{ headingCount }}</strong>
                </div>
              </div>
            </section>

            <section class="sidebar-card tags-card">
              <div class="sidebar-title">{{ copy.topicLabel }}</div>
              <div class="sidebar-tags">
                <button
                  v-if="post.category"
                  type="button"
                  class="sidebar-tag sidebar-tag-category"
                  @click="goToCategory(post.category)"
                >
                  {{ post.category }}
                </button>
                <span v-else class="sidebar-tag sidebar-tag-category is-static">{{ copy.uncategorized }}</span>
                <button
                  v-for="tag in tagList"
                  :key="tag"
                  type="button"
                  class="sidebar-tag"
                  @click="goToTag(tag)"
                >
                  # {{ tag }}
                </button>
              </div>
            </section>
          </aside>
        </div>
      </div>
    </main>
  </div>
</template>

<script setup>
import { computed, nextTick, onMounted, onUnmounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import 'md-editor-v3/lib/style.css'
import { publicApi } from '@/api'
import { useUiStore } from '@/stores'
import { parseTagList } from '@/utils/tags'
import TopHeader from '@/components/TopHeader.vue'
import MarkdownPreview from '@/components/MarkdownPreview.vue'
import PostComments from '@/components/PostComments.vue'

const route = useRoute()
const router = useRouter()
const uiStore = useUiStore()
const { locale, t } = useI18n()

const loading = ref(true)
const post = ref(null)
const catalogItems = ref([])
const activeCatalogId = ref('')
const catalogWrapRef = ref(null)

let catalogScrollLockId = ''
let catalogScrollLockTimer = null

const previewId = 'post-detail-preview'

const copyMap = {
  'zh-CN': {
    uncategorized: '未分类',
    articleLabel: '正文内容',
    articleSubline: (count) => `共 ${count} 个正文标题节点`,
    articleInfo: '文章信息',
    publishLabel: '发布时间',
    viewLabel: '浏览次数',
    readingLabel: '阅读时长',
    headingLabel: '正文标题',
    topicLabel: '话题标签',
    catalogLabel: '文章目录',
    catalogEmpty: '这篇文章暂时没有可生成的目录',
    readingTime: (minutes) => `${minutes} 分钟`
  },
  'zh-TW': {
    uncategorized: '未分類',
    articleLabel: '正文內容',
    articleSubline: (count) => `共 ${count} 個正文標題節點`,
    articleInfo: '文章資訊',
    publishLabel: '發布時間',
    viewLabel: '瀏覽次數',
    readingLabel: '閱讀時長',
    headingLabel: '正文標題',
    topicLabel: '話題標籤',
    catalogLabel: '文章目錄',
    catalogEmpty: '這篇文章暫時沒有可生成的目錄',
    readingTime: (minutes) => `${minutes} 分鐘`
  },
  en: {
    uncategorized: 'Uncategorized',
    articleLabel: 'Article',
    articleSubline: (count) => `${count} content heading${count === 1 ? '' : 's'}`,
    articleInfo: 'Article Info',
    publishLabel: 'Published',
    viewLabel: 'Views',
    readingLabel: 'Reading time',
    headingLabel: 'Headings',
    topicLabel: 'Topics',
    catalogLabel: 'Table of contents',
    catalogEmpty: 'No catalog is available for this article yet',
    readingTime: (minutes) => `${minutes} min`
  }
}

const copy = computed(() => copyMap[locale.value] || copyMap['zh-CN'])

const dateLocale = computed(() => (locale.value === 'en' ? 'en-US' : locale.value))
const formattedDate = computed(() => formatDate(post.value?.createTime))
const tagList = computed(() => parseTagList(post.value?.tags || ''))

const stripMarkdown = (value = '') => String(value)
  .replace(/```[\s\S]*?```/g, ' ')
  .replace(/`[^`]*`/g, ' ')
  .replace(/!\[[^\]]*]\([^)]*\)/g, ' ')
  .replace(/\[([^\]]+)]\([^)]*\)/g, '$1')
  .replace(/<\/?[^>]+(>|$)/g, ' ')
  .replace(/^>\s?/gm, ' ')
  .replace(/(^|\s)#{1,6}\s+/gm, ' ')
  .replace(/[*_~>-]/g, ' ')
  .replace(/\r?\n+/g, ' ')
  .replace(/\s+/g, ' ')
  .trim()

const estimateReadingMinutes = (content = '') => {
  const text = stripMarkdown(content)
  if (!text) return 1

  const chineseChars = (text.match(/[\u4e00-\u9fa5]/g) || []).length
  const latinWords = text.replace(/[\u4e00-\u9fa5]/g, ' ').trim().split(/\s+/).filter(Boolean).length
  const totalMinutes = chineseChars / 320 + latinWords / 220

  return Math.max(1, Math.round(totalMinutes))
}

const readingMinutes = computed(() => estimateReadingMinutes(post.value?.content || ''))
const headingCount = computed(() => (post.value?.content?.match(/^#{1,6}\s+/gm) || []).length)

const formatDate = (dateStr) => {
  if (!dateStr) return ''

  const date = new Date(dateStr)
  if (Number.isNaN(date.getTime())) {
    return dateStr
  }

  return date.toLocaleDateString(dateLocale.value, {
    year: 'numeric',
    month: locale.value === 'en' ? 'short' : '2-digit',
    day: '2-digit'
  })
}

const collectCatalogItems = () => {
  const previewRoot = document.getElementById(previewId)
  const headings = previewRoot?.querySelectorAll('h1, h2, h3, h4, h5, h6') || []

  catalogItems.value = Array.from(headings).map((heading, index) => {
    // Repeated heading text can produce duplicate ids in the rendered preview.
    // Force a stable unique id per heading so TOC navigation always targets
    // the correct section.
    heading.id = `${previewId}-heading-${index + 1}`

    return {
      id: heading.id,
      text: heading.textContent?.trim() || '',
      level: Number(heading.tagName.slice(1)) || 1
    }
  }).filter((item) => item.text)

  updateActiveCatalog()
}

const keepActiveCatalogVisible = (behavior = 'auto') => {
  const wrapEl = catalogWrapRef.value
  if (!wrapEl || !activeCatalogId.value) {
    return
  }

  const activeEl = wrapEl.querySelector(`[aria-current="true"]`)
  if (!(activeEl instanceof HTMLElement)) {
    return
  }

  const wrapTop = wrapEl.scrollTop
  const wrapBottom = wrapTop + wrapEl.clientHeight
  const itemTop = activeEl.offsetTop - 10
  const itemBottom = activeEl.offsetTop + activeEl.offsetHeight + 10

  if (itemTop < wrapTop) {
    wrapEl.scrollTo({ top: itemTop, behavior })
    return
  }

  if (itemBottom > wrapBottom) {
    wrapEl.scrollTo({ top: itemBottom - wrapEl.clientHeight, behavior })
  }
}

const clearCatalogScrollLock = () => {
  catalogScrollLockId = ''
  if (catalogScrollLockTimer) {
    window.clearTimeout(catalogScrollLockTimer)
    catalogScrollLockTimer = null
  }
}

const updateActiveCatalog = () => {
  if (!catalogItems.value.length) {
    activeCatalogId.value = ''
    return
  }

  if (catalogScrollLockId) {
    activeCatalogId.value = catalogScrollLockId
    keepActiveCatalogVisible()
    return
  }

  const header = document.querySelector('.top-header')
  const headerHeight = header?.getBoundingClientRect().height || 72
  const anchorLine = headerHeight + 28
  let currentId = catalogItems.value[0].id

  for (const item of catalogItems.value) {
    const target = document.getElementById(item.id)
    if (!target) continue

    if (target.getBoundingClientRect().top <= anchorLine) {
      currentId = item.id
    } else {
      break
    }
  }

  activeCatalogId.value = currentId
  keepActiveCatalogVisible()
}

const scrollToHeading = (headingId) => {
  const target = document.getElementById(headingId)
  if (!target) return

  clearCatalogScrollLock()
  catalogScrollLockId = headingId

  const header = document.querySelector('.top-header')
  const headerHeight = header?.getBoundingClientRect().height || 72
  const targetTop = window.scrollY + target.getBoundingClientRect().top - headerHeight - 18

  window.scrollTo({
    top: Math.max(0, targetTop),
    behavior: 'smooth'
  })

  activeCatalogId.value = headingId
  nextTick(() => {
    keepActiveCatalogVisible('smooth')
  })

  catalogScrollLockTimer = window.setTimeout(() => {
    clearCatalogScrollLock()
    updateActiveCatalog()
  }, 900)
}

const loadPost = async () => {
  loading.value = true
  window.scrollTo({ top: 0, behavior: 'auto' })

  try {
    const postRes = await publicApi.getPost(route.params.id)
    post.value = postRes.data
  } catch (error) {
    post.value = null
    catalogItems.value = []
    activeCatalogId.value = ''
  } finally {
    loading.value = false
    await nextTick()
    requestAnimationFrame(() => {
      collectCatalogItems()
    })
  }
}

const goBack = () => {
  const tag = route.query.tag
  const category = route.query.category

  if (tag) {
    router.push({ path: '/tags', query: { tag } })
  } else if (category) {
    router.push({ path: '/categories', query: { category } })
  } else if (window.history.length > 1) {
    router.back()
  } else {
    router.push('/')
  }
}

const goToTag = (tag) => {
  if (!tag) return
  router.push({ path: '/tags', query: { tag } })
}

const goToCategory = (category) => {
  if (!category) return
  router.push({ path: '/categories', query: { category } })
}

onMounted(() => {
  loadPost()
  window.addEventListener('scroll', updateActiveCatalog, { passive: true })
})

onUnmounted(() => {
  window.removeEventListener('scroll', updateActiveCatalog)
  clearCatalogScrollLock()
})
</script>

<style scoped>
.post-page {
  min-height: 100vh;
  background:
    radial-gradient(circle at top center, color-mix(in srgb, var(--accent-light) 90%, transparent), transparent 32%),
    radial-gradient(circle at left bottom, color-mix(in srgb, var(--accent-secondary) 12%, transparent), transparent 28%),
    var(--gradient-bg);
}

.post-main {
  padding: 36px 18px 80px;
}

.post-shell {
  max-width: 1320px;
  margin: 0 auto;
}

.hero-card {
  position: relative;
  overflow: hidden;
  border-radius: 34px;
  min-height: 420px;
  border: 1px solid color-mix(in srgb, var(--border-color) 78%, transparent);
  box-shadow: 0 28px 56px rgba(15, 23, 42, 0.12);
  background: color-mix(in srgb, var(--bg-card) 82%, transparent);
  margin-bottom: 28px;
}

.hero-cover {
  position: absolute;
  inset: 0;
}

.hero-cover-image,
.hero-cover-fallback,
.hero-cover-overlay {
  position: absolute;
  inset: 0;
}

.hero-cover-image {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.hero-cover-fallback {
  background:
    linear-gradient(135deg, color-mix(in srgb, var(--accent) 26%, #ffffff), color-mix(in srgb, var(--accent-secondary) 22%, #ffffff));
}

.hero-cover.empty .hero-cover-fallback {
  opacity: 1;
}

.hero-cover:not(.empty) .hero-cover-fallback {
  opacity: 0;
}

.hero-cover-overlay {
  background:
    linear-gradient(180deg, rgba(15, 23, 42, 0.08), rgba(15, 23, 42, 0.68)),
    linear-gradient(135deg, color-mix(in srgb, var(--accent) 26%, transparent), transparent 45%);
}

.hero-body {
  position: relative;
  z-index: 1;
  min-height: 420px;
  display: flex;
  flex-direction: column;
  justify-content: flex-end;
  padding: 26px 28px 30px;
  color: #fff;
}

.back-link {
  align-self: flex-start;
  margin-bottom: auto;
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 10px 16px;
  border: 1px solid rgba(255, 255, 255, 0.18);
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.12);
  color: #fff;
  cursor: pointer;
  backdrop-filter: blur(16px);
  -webkit-backdrop-filter: blur(16px);
}

.back-link svg {
  width: 16px;
  height: 16px;
  fill: none;
  stroke: currentColor;
  stroke-width: 2;
  stroke-linecap: round;
  stroke-linejoin: round;
}

.hero-badges,
.hero-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.hero-badges {
  margin-bottom: 18px;
}

.hero-badge,
.hero-tag {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-height: 34px;
  padding: 0 14px;
  border-radius: 999px;
  font-size: 0.92rem;
  font-weight: 600;
  backdrop-filter: blur(14px);
  -webkit-backdrop-filter: blur(14px);
}

.hero-badge {
  background: rgba(255, 255, 255, 0.12);
  border: 1px solid rgba(255, 255, 255, 0.14);
}

.hero-badge.accent,
.hero-tag {
  background: rgba(57, 197, 187, 0.18);
  border: 1px solid rgba(79, 212, 204, 0.28);
}

.hero-tag {
  color: #fff;
  cursor: pointer;
  transition: transform 0.22s ease, background-color 0.22s ease, border-color 0.22s ease;
}

.hero-tag:hover {
  transform: translateY(-1px);
  background: rgba(57, 197, 187, 0.26);
  border-color: rgba(123, 238, 231, 0.4);
}

.hero-tag-category {
  background: rgba(255, 255, 255, 0.14);
  border-color: rgba(255, 255, 255, 0.22);
}

.hero-tag-category:hover {
  background: rgba(255, 255, 255, 0.2);
  border-color: rgba(255, 255, 255, 0.3);
}

.hero-tag.is-static {
  cursor: default;
}

.hero-tag.is-static:hover {
  transform: none;
}

.post-header h1 {
  display: block;
  width: 100%;
  max-width: none;
  margin: 0;
  font-size: clamp(2rem, 4vw, 3.3rem);
  line-height: 1.16;
  letter-spacing: -0.02em;
  word-break: normal;
  overflow-wrap: break-word;
}

.hero-tags {
  margin-top: 18px;
}

.post-layout {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 320px;
  gap: 22px;
  align-items: start;
}

.post-primary,
.post-sidebar {
  min-width: 0;
}

.content-card,
.sidebar-card,
.state-card {
  border-radius: 28px;
  border: 1px solid color-mix(in srgb, var(--border-color) 78%, transparent);
  background: color-mix(in srgb, var(--bg-card) 94%, transparent);
  box-shadow: 0 16px 36px rgba(15, 23, 42, 0.06);
}

.content-card {
  padding: 24px 26px;
}

.section-chip {
  display: inline-flex;
  align-items: center;
  min-height: 32px;
  padding: 0 14px;
  border-radius: 999px;
  color: var(--accent);
  background: color-mix(in srgb, var(--accent-light) 88%, var(--bg-card));
  font-size: 0.9rem;
  font-weight: 700;
}

.content-card {
  padding-top: 20px;
}

.content-card-head {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: space-between;
  gap: 14px;
  margin-bottom: 24px;
}

.content-card-title,
.content-card-meta {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 10px 14px;
}

.content-card-meta {
  color: var(--text-secondary);
  font-size: 0.92rem;
}

.post-content {
  color: var(--text-primary);
  line-height: 1.9;
  font-size: 1.05rem;
}

.post-content :deep(.markdown-preview-shell) {
  width: 100%;
}

.post-content :deep(.md-editor-preview) {
  font-size: 1.06rem;
  line-height: 1.95;
}

.post-content :deep(h1),
.post-content :deep(h2),
.post-content :deep(h3),
.post-content :deep(h4) {
  scroll-margin-top: 104px;
}

.post-content :deep(img) {
  display: block;
  margin: 1.5rem auto;
  border-radius: 20px;
  box-shadow: 0 18px 34px rgba(15, 23, 42, 0.12);
}

.post-content :deep(code) {
  border-radius: 10px;
}

.post-content :deep(blockquote) {
  border-left: 4px solid var(--accent);
  padding: 10px 0 10px 18px;
  color: var(--text-secondary);
  background: color-mix(in srgb, var(--accent-light) 50%, transparent);
  border-radius: 0 18px 18px 0;
}

.post-sidebar {
  position: sticky;
  top: 96px;
  display: grid;
  gap: 16px;
  align-content: start;
}

.sidebar-card {
  padding: 20px;
}

.sidebar-title {
  font-size: 0.98rem;
  font-weight: 700;
  color: var(--text-primary);
  margin-bottom: 14px;
}

.meta-list {
  display: grid;
  gap: 12px;
}

.meta-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  color: var(--text-secondary);
  font-size: 0.92rem;
}

.meta-row strong {
  color: var(--text-primary);
  text-align: right;
}

.sidebar-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.sidebar-tag {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-height: 32px;
  padding: 0 12px;
  border-radius: 999px;
  background: color-mix(in srgb, var(--accent-light) 72%, var(--bg-card));
  color: color-mix(in srgb, var(--text-primary) 86%, var(--accent) 14%);
  font-size: 0.9rem;
  border: 1px solid transparent;
  cursor: pointer;
  transition: transform 0.2s ease, border-color 0.2s ease, background-color 0.2s ease, color 0.2s ease;
}

.sidebar-tag:hover {
  transform: translateY(-1px);
  border-color: color-mix(in srgb, var(--accent) 26%, transparent);
  background: color-mix(in srgb, var(--accent-light) 88%, var(--bg-card));
  color: var(--accent);
}

.sidebar-tag-category {
  background: color-mix(in srgb, var(--accent) 12%, var(--bg-card));
}

.sidebar-tag.is-static {
  cursor: default;
}

.sidebar-tag.is-static:hover {
  transform: none;
  border-color: transparent;
  color: color-mix(in srgb, var(--text-primary) 86%, var(--accent) 14%);
}

.catalog-wrap {
  display: grid;
  gap: 8px;
  max-height: clamp(180px, 24vh, 260px);
  overflow: auto;
  padding-right: 6px;
  scrollbar-width: thin;
  scrollbar-color: color-mix(in srgb, var(--accent) 34%, transparent) transparent;
}

.catalog-wrap::-webkit-scrollbar {
  width: 8px;
}

.catalog-wrap::-webkit-scrollbar-track {
  background: transparent;
}

.catalog-wrap::-webkit-scrollbar-thumb {
  border-radius: 999px;
  background: color-mix(in srgb, var(--accent) 24%, transparent);
}

.catalog-wrap::-webkit-scrollbar-thumb:hover {
  background: color-mix(in srgb, var(--accent) 40%, transparent);
}

.catalog-link {
  position: relative;
  display: flex;
  width: 100%;
  min-width: 0;
  align-items: flex-start;
  gap: 10px;
  padding: 10px 12px;
  padding-left: calc(12px + var(--catalog-level, 0) * 14px);
  border: 1px solid color-mix(in srgb, var(--border-color) 62%, transparent);
  border-radius: 16px;
  background: color-mix(in srgb, var(--bg-tertiary) 82%, transparent);
  color: var(--text-secondary);
  text-align: left;
  font: inherit;
  font-size: 0.94rem;
  line-height: 1.45;
  cursor: pointer;
  transition:
    transform 0.2s ease,
    color 0.2s ease,
    background-color 0.2s ease,
    border-color 0.2s ease,
    box-shadow 0.2s ease,
    opacity 0.2s ease;
}

.catalog-link::before {
  content: '';
  position: absolute;
  inset: 10px auto 10px 0;
  width: 3px;
  border-radius: 999px;
  background: transparent;
  transition: background-color 0.2s ease, opacity 0.2s ease;
}

.catalog-level-badge {
  flex: 0 0 auto;
  min-width: 28px;
  padding: 3px 6px;
  border-radius: 999px;
  background: color-mix(in srgb, var(--bg-card) 86%, transparent);
  color: color-mix(in srgb, var(--text-secondary) 88%, transparent);
  font-size: 0.72rem;
  font-weight: 700;
  line-height: 1;
  letter-spacing: 0.04em;
}

.catalog-link span:last-child {
  display: -webkit-box;
  min-width: 0;
  overflow: hidden;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}

.catalog-link:hover {
  transform: translateY(-1px);
  color: var(--text-primary);
  background: color-mix(in srgb, var(--accent-light) 68%, var(--bg-card));
  border-color: color-mix(in srgb, var(--accent) 20%, transparent);
  box-shadow: 0 8px 14px rgba(15, 23, 42, 0.05);
}

.catalog-link.active {
  color: color-mix(in srgb, var(--text-primary) 82%, var(--accent) 18%);
  background:
    linear-gradient(135deg, color-mix(in srgb, var(--accent-light) 92%, var(--bg-card)), color-mix(in srgb, var(--accent) 8%, var(--bg-card)));
  border-color: color-mix(in srgb, var(--accent) 32%, transparent);
  box-shadow: 0 10px 22px rgba(57, 197, 187, 0.12);
}

.catalog-link.active::before {
  background: linear-gradient(180deg, var(--accent), color-mix(in srgb, var(--accent-secondary) 52%, var(--accent)));
}

.catalog-link.active .catalog-level-badge {
  background: color-mix(in srgb, var(--accent) 14%, var(--bg-card));
  color: var(--accent);
}

.catalog-link.level-1 {
  font-size: 0.98rem;
  font-weight: 700;
  color: var(--text-primary);
}

.catalog-link.level-2 {
  font-size: 0.94rem;
  font-weight: 650;
}

.catalog-link.level-3 {
  font-size: 0.9rem;
  opacity: 0.96;
}

.catalog-link.level-4 {
  font-size: 0.86rem;
  opacity: 0.84;
}

.catalog-empty {
  color: var(--text-secondary);
  line-height: 1.75;
}

.state-card {
  max-width: 880px;
  margin: 24px auto 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 16px;
  padding: 80px 20px;
}

.loading-spinner {
  width: 42px;
  height: 42px;
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

.state-icon {
  font-size: 3.8rem;
}

.state-text {
  color: var(--text-secondary);
  font-size: 1.08rem;
}

@media (max-width: 1120px) {
  .post-layout {
    grid-template-columns: minmax(0, 1fr);
  }

  .post-sidebar {
    position: static;
    grid-template-columns: repeat(3, minmax(0, 1fr));
    max-height: none;
    overflow: visible;
    padding-right: 0;
  }
}

@media (max-width: 768px) {
  .post-main {
    padding: 24px 14px 60px;
  }

  .hero-card {
    min-height: 360px;
    border-radius: 26px;
  }

  .hero-body {
    min-height: 360px;
    padding: 20px 18px 22px;
  }

  .post-header h1 {
    font-size: 1.74rem;
  }

  .content-card {
    padding: 18px;
    border-radius: 22px;
  }

  .post-sidebar {
    grid-template-columns: 1fr;
  }

  .catalog-wrap {
    max-height: 220px;
  }
}

@media (max-width: 560px) {
  .hero-badge,
  .hero-tag {
    min-height: 30px;
    padding: 0 12px;
    font-size: 0.84rem;
  }

  .back-link {
    padding: 9px 14px;
  }

}
</style>
