<template>
  <Teleport to="body">
    <transition name="search-fade">
      <div v-if="modelValue" class="search-overlay" @click="close">
        <div class="search-panel" @click.stop>
          <div class="search-toolbar">
            <div class="mode-switch" role="tablist" :aria-label="copy.modeLabel">
              <button
                type="button"
                class="mode-btn"
                :class="{ active: mode === 'title' }"
                :aria-selected="mode === 'title'"
                @click="mode = 'title'"
              >
                {{ copy.modeTitle }}
              </button>
              <span class="mode-separator" aria-hidden="true">/</span>
              <button
                type="button"
                class="mode-btn"
                :class="{ active: mode === 'content' }"
                :aria-selected="mode === 'content'"
                @click="mode = 'content'"
              >
                {{ copy.modeContent }}
              </button>
            </div>

            <button type="button" class="close-btn" :title="copy.close" @click="close">
              <svg viewBox="0 0 24 24" aria-hidden="true">
                <path d="M6 6l12 12M18 6L6 18" />
              </svg>
            </button>
          </div>

          <label class="search-input-wrap">
            <span class="search-icon" aria-hidden="true">
              <svg viewBox="0 0 24 24">
                <circle cx="11" cy="11" r="7" />
                <path d="M20 20l-3.5-3.5" />
              </svg>
            </span>
            <input
              ref="inputRef"
              v-model.trim="keyword"
              type="text"
              class="search-input"
              :placeholder="mode === 'title' ? copy.placeholderTitle : copy.placeholderContent"
              @keydown.enter.prevent="openFirstResult"
            />
          </label>

          <div class="search-meta">
            <span>{{ trimmedKeyword ? copy.resultHint : copy.initialHint }}</span>
            <span v-if="trimmedKeyword && results.length">{{ copy.resultCount(results.length) }}</span>
          </div>

          <div class="search-results">
            <div v-if="loading" class="search-state">
              {{ copy.loading }}
            </div>

            <div v-else-if="results.length" class="results-list">
              <button
                v-for="item in results"
                :key="item.id"
                type="button"
                class="result-card"
                @click="openPost(item)"
              >
                <div class="result-head">
                  <span v-if="item.category" class="result-category">{{ item.category }}</span>
                  <span class="result-date">{{ formatDate(item.createTime) }}</span>
                </div>
                <h3 class="result-title">{{ item.title || copy.untitled }}</h3>
                <p class="result-snippet">{{ buildSnippet(item) }}</p>
              </button>
            </div>

            <div v-else class="search-state">
              <div>
                <strong>{{ trimmedKeyword ? copy.emptyTitle : copy.idleTitle }}</strong>
                <p>{{ trimmedKeyword ? copy.emptyDesc : copy.idleDesc }}</p>
              </div>
            </div>
          </div>
        </div>
      </div>
    </transition>
  </Teleport>
</template>

<script setup>
import Fuse from 'fuse.js'
import { computed, nextTick, onBeforeUnmount, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { publicApi } from '@/api'

const props = defineProps({
  modelValue: {
    type: Boolean,
    default: false
  }
})

const emit = defineEmits(['update:modelValue'])

const router = useRouter()
const { locale } = useI18n()

const inputRef = ref(null)
const loading = ref(false)
const posts = ref([])
const keyword = ref('')
const mode = ref('title')
const hasLoaded = ref(false)

const copyMap = {
  'zh-CN': {
    modeLabel: '搜索方式',
    close: '关闭搜索',
    placeholderTitle: '搜索文章标题',
    placeholderContent: '搜索文章内容',
    modeTitle: '标题',
    modeContent: '内容',
    initialHint: '输入关键词开始搜索，按 Enter 可打开第一条结果，Esc 关闭',
    resultHint: '模糊匹配已开启，可直接搜索相关内容',
    loading: '正在整理文章索引...',
    idleTitle: '输入关键词开始搜索',
    idleDesc: '可直接按标题或正文内容进行搜索',
    emptyTitle: '没有找到相关结果',
    emptyDesc: '试试更短的关键词、不同写法，或者切换到另一种搜索方式',
    latestTitle: '最新文章',
    untitled: '未命名文章',
    resultCount: (count) => `找到 ${count} 条结果`
  },
  'zh-TW': {
    modeLabel: '搜尋方式',
    close: '關閉搜尋',
    placeholderTitle: '搜尋文章標題',
    placeholderContent: '搜尋文章內容',
    modeTitle: '標題',
    modeContent: '內容',
    initialHint: '輸入關鍵字開始搜尋，按 Enter 可開啟第一條結果，Esc 關閉',
    resultHint: '已啟用模糊匹配，可直接搜尋相關內容',
    loading: '正在整理文章索引...',
    idleTitle: '輸入關鍵字開始搜尋',
    idleDesc: '未輸入內容時不顯示文章結果，可直接搜尋標題或正文',
    emptyTitle: '沒有找到相關結果',
    emptyDesc: '試試更短的關鍵字、不同寫法，或切換另一種搜尋方式',
    latestTitle: '最新文章',
    untitled: '未命名文章',
    resultCount: (count) => `找到 ${count} 筆結果`
  },
  en: {
    modeLabel: 'Search mode',
    close: 'Close search',
    placeholderTitle: 'Search post titles',
    placeholderContent: 'Search post content',
    modeTitle: 'Title',
    modeContent: 'Content',
    initialHint: 'Type to search. Press Enter to open the first result, or Esc to close',
    resultHint: 'Fuzzy matching is on, so related wording can still be found',
    loading: 'Building the post index...',
    idleTitle: 'Start typing to search',
    idleDesc: 'No posts are shown until you enter a keyword for title or content search',
    emptyTitle: 'No matching results',
    emptyDesc: 'Try a shorter keyword, a different phrase, or switch the search mode',
    latestTitle: 'Latest posts',
    untitled: 'Untitled post',
    resultCount: (count) => `${count} result${count === 1 ? '' : 's'}`
  }
}

const copy = computed(() => copyMap[locale.value] || copyMap['zh-CN'])

const normalizeWhitespace = (value = '') => value.replace(/\s+/g, ' ').trim()

const stripMarkdown = (value = '') => {
  if (!value) return ''

  let text = String(value)
    .replace(/```[\s\S]*?```/g, ' ')
    .replace(/`[^`]*`/g, ' ')
    .replace(/!\[[^\]]*]\([^)]*\)/g, ' ')
    .replace(/\[([^\]]+)]\([^)]*\)/g, '$1')
    .replace(/<\/?[^>]+(>|$)/g, ' ')
    .replace(/(^|\s)#{1,6}\s+/gm, ' ')
    .replace(/^>\s?/gm, ' ')
    .replace(/[*_~\-|]/g, ' ')
    .replace(/\r?\n+/g, ' ')

  if (typeof document !== 'undefined') {
    const helper = document.createElement('textarea')
    helper.innerHTML = text
    text = helper.value
  }

  return normalizeWhitespace(text)
}

const normalizedPosts = computed(() => {
  const list = Array.isArray(posts.value) ? posts.value : []

  return [...list]
    .map((post) => ({
      ...post,
      title: normalizeWhitespace(post.title || ''),
      excerptText: stripMarkdown(post.excerpt || ''),
      summaryText: stripMarkdown(post.summary || ''),
      contentText: stripMarkdown(post.content || '')
    }))
    .sort((a, b) => new Date(b.createTime || 0).getTime() - new Date(a.createTime || 0).getTime())
})

const titleFuse = computed(() => new Fuse(normalizedPosts.value, {
  keys: [{ name: 'title', weight: 1 }],
  threshold: 0.35,
  ignoreLocation: true,
  minMatchCharLength: 1
}))

const contentFuse = computed(() => new Fuse(normalizedPosts.value, {
  keys: [
    { name: 'contentText', weight: 0.6 },
    { name: 'summaryText', weight: 0.2 },
    { name: 'excerptText', weight: 0.15 },
    { name: 'title', weight: 0.05 }
  ],
  threshold: 0.42,
  ignoreLocation: true,
  minMatchCharLength: 1
}))

const trimmedKeyword = computed(() => keyword.value.trim())

const results = computed(() => {
  if (!trimmedKeyword.value) {
    return []
  }

  const fuse = mode.value === 'title' ? titleFuse.value : contentFuse.value
  return fuse.search(trimmedKeyword.value, { limit: 12 }).map((entry) => entry.item)
})

const formatDate = (value) => {
  if (!value) return ''

  const date = new Date(value)
  if (Number.isNaN(date.getTime())) {
    return value
  }

  const localeCode = locale.value === 'en' ? 'en-US' : locale.value
  return new Intl.DateTimeFormat(localeCode, {
    year: 'numeric',
    month: 'short',
    day: 'numeric'
  }).format(date)
}

const truncateText = (value, maxLength = 110) => {
  if (!value) return ''
  if (value.length <= maxLength) return value
  return `${value.slice(0, maxLength).trim()}...`
}

const buildSnippet = (item) => {
  const fallback = item.summaryText || item.excerptText || item.contentText
  const baseText = mode.value === 'content'
    ? (item.contentText || fallback)
    : fallback

  if (!baseText) {
    return copy.value.latestTitle
  }

  if (!trimmedKeyword.value) {
    return truncateText(baseText)
  }

  const loweredText = baseText.toLowerCase()
  const candidates = [trimmedKeyword.value, ...trimmedKeyword.value.split(/\s+/)].filter(Boolean)
  const match = candidates.find((entry) => loweredText.includes(entry.toLowerCase()))

  if (!match) {
    return truncateText(baseText)
  }

  const keywordIndex = loweredText.indexOf(match.toLowerCase())
  const start = Math.max(0, keywordIndex - 24)
  const end = Math.min(baseText.length, keywordIndex + 88)
  const snippet = baseText.slice(start, end).trim()

  return `${start > 0 ? '...' : ''}${snippet}${end < baseText.length ? '...' : ''}`
}

const syncBodyLock = (locked) => {
  if (typeof document === 'undefined') return
  document.body.style.overflow = locked ? 'hidden' : ''
}

const ensurePostsLoaded = async () => {
  if (hasLoaded.value || loading.value) return

  loading.value = true
  try {
    const res = await publicApi.getPostSearchIndex()
    posts.value = res.data?.records || res.data || []
    hasLoaded.value = true
  } catch (error) {
    posts.value = []
  } finally {
    loading.value = false
  }
}

const close = () => {
  emit('update:modelValue', false)
}

const resetState = () => {
  keyword.value = ''
  mode.value = 'title'
}

const openPost = async (item) => {
  close()
  await router.push(`/post/${item.id}`)
}

const openFirstResult = () => {
  if (!results.value.length) return
  openPost(results.value[0])
}

const handleKeydown = (event) => {
  if (event.key === 'Escape' && props.modelValue) {
    close()
  }
}

watch(
  () => props.modelValue,
  async (visible) => {
    syncBodyLock(visible)

    if (visible) {
      await ensurePostsLoaded()
      await nextTick()
      inputRef.value?.focus()
      return
    }

    resetState()
  }
)

watch(
  () => props.modelValue,
  (visible) => {
    if (typeof window === 'undefined') return

    if (visible) {
      window.addEventListener('keydown', handleKeydown)
      return
    }

    window.removeEventListener('keydown', handleKeydown)
  },
  { immediate: true }
)

onBeforeUnmount(() => {
  syncBodyLock(false)
  if (typeof window !== 'undefined') {
    window.removeEventListener('keydown', handleKeydown)
  }
})
</script>

<style scoped>
.search-overlay {
  position: fixed;
  inset: 0;
  z-index: 260;
  padding: 6vh 20px 28px;
  background:
    radial-gradient(circle at top center, color-mix(in srgb, var(--accent-light) 90%, transparent), transparent 38%),
    color-mix(in srgb, var(--bg-glass) 42%, rgba(10, 14, 24, 0.6));
  backdrop-filter: blur(24px) saturate(180%);
  -webkit-backdrop-filter: blur(24px) saturate(180%);
  overflow-y: auto;
}

.search-panel {
  width: min(920px, 100%);
  margin: 0 auto;
  padding: 22px;
  border-radius: 28px;
  border: 1px solid color-mix(in srgb, var(--border-color) 72%, transparent);
  background:
    linear-gradient(180deg, color-mix(in srgb, var(--bg-card) 96%, var(--accent-light)), color-mix(in srgb, var(--bg-card) 98%, transparent));
  box-shadow: 0 26px 70px rgba(15, 23, 42, 0.2);
}

.search-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 18px;
}

.mode-switch {
  display: inline-flex;
  align-items: center;
  padding: 6px;
  border-radius: 999px;
  background: color-mix(in srgb, var(--bg-card) 88%, transparent);
  border: 1px solid color-mix(in srgb, var(--border-color) 80%, transparent);
}

.mode-btn {
  border: none;
  background: transparent;
  color: var(--text-secondary);
  font: inherit;
  font-weight: 700;
  padding: 9px 16px;
  border-radius: 999px;
  cursor: pointer;
  transition: color 0.2s ease, background 0.2s ease, transform 0.2s ease;
}

.mode-btn:hover {
  color: var(--text-primary);
  transform: translateY(-1px);
}

.mode-btn.active {
  color: var(--accent);
  background: color-mix(in srgb, var(--accent-light) 88%, var(--bg-card));
}

.mode-separator {
  color: color-mix(in srgb, var(--text-secondary) 72%, transparent);
  padding: 0 6px;
  font-weight: 700;
}

.close-btn {
  width: 44px;
  height: 44px;
  border-radius: 16px;
  border: 1px solid color-mix(in srgb, var(--border-color) 82%, transparent);
  background: color-mix(in srgb, var(--bg-card) 88%, transparent);
  color: var(--text-secondary);
  display: inline-flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: background 0.2s ease, color 0.2s ease, transform 0.2s ease;
}

.close-btn:hover {
  color: var(--accent);
  background: var(--accent-light);
  transform: translateY(-1px);
}

.close-btn svg,
.search-icon svg {
  width: 20px;
  height: 20px;
  fill: none;
  stroke: currentColor;
  stroke-width: 1.8;
  stroke-linecap: round;
  stroke-linejoin: round;
}

.search-input-wrap {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 18px 20px;
  border-radius: 24px;
  background: color-mix(in srgb, var(--bg-card) 94%, transparent);
  border: 1px solid color-mix(in srgb, var(--border-color) 82%, transparent);
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.08);
}

.search-input-wrap:focus-within {
  border-color: color-mix(in srgb, var(--accent) 42%, var(--border-color));
  box-shadow:
    0 0 0 5px color-mix(in srgb, var(--accent-light) 88%, transparent),
    inset 0 1px 0 rgba(255, 255, 255, 0.08);
}

.search-icon {
  color: var(--accent);
  display: inline-flex;
  align-items: center;
  justify-content: center;
}

.search-input {
  flex: 1;
  min-width: 0;
  border: none;
  outline: none;
  background: transparent;
  color: var(--text-primary);
  font-size: clamp(1.1rem, 2vw, 1.5rem);
  font-weight: 700;
  letter-spacing: 0.01em;
}

.search-input::placeholder {
  color: color-mix(in srgb, var(--text-secondary) 72%, transparent);
  font-weight: 600;
}

.search-meta {
  margin: 14px 2px 0;
  display: flex;
  flex-wrap: wrap;
  justify-content: space-between;
  gap: 8px 16px;
  color: var(--text-secondary);
  font-size: 0.94rem;
}

.search-results {
  margin-top: 18px;
  min-height: 240px;
}

.results-list {
  display: grid;
  gap: 14px;
}

.result-card {
  width: 100%;
  text-align: left;
  padding: 18px 20px;
  border-radius: 22px;
  border: 1px solid color-mix(in srgb, var(--border-color) 78%, transparent);
  background:
    linear-gradient(135deg, color-mix(in srgb, var(--bg-card) 96%, var(--accent-light)), color-mix(in srgb, var(--bg-card) 92%, transparent));
  cursor: pointer;
  transition: transform 0.22s ease, border-color 0.22s ease, box-shadow 0.22s ease;
}

.result-card:hover {
  transform: translateY(-2px);
  border-color: color-mix(in srgb, var(--accent) 28%, var(--border-color));
  box-shadow: 0 16px 30px rgba(15, 23, 42, 0.08);
}

.result-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  margin-bottom: 10px;
  font-size: 0.86rem;
}

.result-category {
  display: inline-flex;
  align-items: center;
  min-height: 28px;
  padding: 0 12px;
  border-radius: 999px;
  color: var(--accent);
  background: color-mix(in srgb, var(--accent-light) 88%, var(--bg-card));
  font-weight: 700;
}

.result-date {
  color: var(--text-secondary);
}

.result-title {
  margin: 0;
  color: var(--text-primary);
  font-size: 1.08rem;
  line-height: 1.45;
}

.result-snippet {
  margin: 10px 0 0;
  color: var(--text-secondary);
  line-height: 1.75;
}

.search-state {
  min-height: 240px;
  display: grid;
  place-items: center;
  text-align: center;
  color: var(--text-secondary);
  border-radius: 24px;
  border: 1px dashed color-mix(in srgb, var(--border-color) 78%, transparent);
  background: color-mix(in srgb, var(--bg-card) 92%, transparent);
  padding: 28px;
}

.search-state strong {
  display: block;
  margin-bottom: 8px;
  color: var(--text-primary);
  font-size: 1.06rem;
}

.search-state p {
  margin: 0;
  max-width: 420px;
  line-height: 1.75;
}

.search-fade-enter-active,
.search-fade-leave-active {
  transition: opacity 0.22s ease;
}

.search-fade-enter-from,
.search-fade-leave-to {
  opacity: 0;
}

@media (max-width: 768px) {
  .search-overlay {
    padding: 16px 14px 24px;
  }

  .search-panel {
    padding: 16px;
    border-radius: 24px;
  }

  .search-toolbar {
    flex-direction: column;
    align-items: stretch;
  }

  .mode-switch {
    justify-content: center;
  }

  .close-btn {
    align-self: flex-end;
  }

  .search-input-wrap {
    padding: 16px;
    border-radius: 20px;
  }

  .result-card {
    padding: 16px;
  }
}

@media (max-width: 520px) {
  .search-input {
    font-size: 1rem;
  }

  .result-head {
    flex-direction: column;
    align-items: flex-start;
  }
}
</style>
