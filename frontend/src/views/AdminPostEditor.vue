<template>
  <div class="editor-page">
    <header class="editor-topbar">
      <div>
        <button class="back-link" @click="goBack">
          <svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M19 12H5M12 19l-7-7 7-7" />
          </svg>
          <span>返回文章管理</span>
        </button>
        <h1 class="editor-title">{{ editorTitle }}</h1>
        <p class="editor-subtitle">{{ editorSubtitle }}</p>
      </div>
      <div class="editor-actions">
        <el-button @click="goBack">取消</el-button>
        <el-button type="primary" plain :loading="saving" @click="savePost()">保存</el-button>
        <el-button type="primary" :loading="saving" @click="savePost({ exitAfterSave: true })">保存并返回</el-button>
      </div>
    </header>

    <div v-if="loading" class="state-card">
      <div class="loading-spinner"></div>
      <span>正在加载文章内容...</span>
    </div>

    <div v-else ref="editorLayoutRef" class="editor-layout">
      <aside ref="editorSidebarRef" class="editor-sidebar">
        <el-form ref="postFormRef" :model="postForm" :rules="postRules" label-position="top" class="editor-form">
          <el-form-item label="文章标题" prop="title">
            <el-input v-model="postForm.title" placeholder="输入文章标题" size="large" class="title-input" />
          </el-form-item>

          <el-form-item label="分类" prop="category">
            <el-select v-model="postForm.category" placeholder="选择分类" size="large" class="field-select">
              <el-option v-for="category in categories" :key="category" :label="category" :value="category" />
            </el-select>
          </el-form-item>

          <el-form-item label="标签" prop="tags">
            <el-input v-model="postForm.tags" placeholder="多个标签用逗号分隔" size="large" />
          </el-form-item>

          <el-form-item label="摘要">
            <el-input v-model="postForm.excerpt" type="textarea" :rows="4" resize="vertical" placeholder="文章摘要会显示在列表与卡片区域" />
          </el-form-item>

          <el-form-item label="标题背景图">
            <input
              ref="coverInputRef"
              type="file"
              accept="image/png,image/jpeg,image/webp,image/gif"
              class="cover-file-input"
              @change="handleCoverFileChange"
            />
            <div class="cover-panel">
              <div class="cover-preview" :class="{ empty: !postForm.coverImage }">
                <img v-if="postForm.coverImage" :src="postForm.coverImage" alt="封面预览" />
                <div v-else class="cover-placeholder">
                  <span class="cover-placeholder-title">标题背景图</span>
                  <span class="cover-placeholder-desc">上传后会在文章列表右侧显示缩略图</span>
                </div>
                <div class="cover-preview-overlay">
                  <span>{{ postForm.title || '文章标题预览' }}</span>
                </div>
              </div>
              <div class="cover-actions-row">
                <el-button :loading="coverUploading" @click="triggerCoverFileSelect">上传图片</el-button>
                <el-button :disabled="!postForm.coverImage" @click="clearCoverImage">清空图片</el-button>
              </div>
              <el-input v-model="postForm.coverImage" placeholder="或直接粘贴图片地址" />
            </div>
          </el-form-item>
        </el-form>
      </aside>

      <section ref="editorMainRef" class="editor-main">
        <div class="editor-main-header">
          <div>
            <div class="section-title">文章内容</div>
            <div class="section-desc">支持完整 Markdown 工具栏、目录、全屏与图片上传</div>
          </div>
          <div class="editor-status-badge">{{ currentStatusLabel }}</div>
        </div>
        <div class="editor-main-body">
          <MarkdownEditor v-model="postForm.content" />
        </div>
      </section>
    </div>
  </div>
</template>

<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { adminApi } from '@/api'
import MarkdownEditor from '@/components/MarkdownEditor.vue'
import { POST_CATEGORIES, createEmptyPostForm } from '@/constants/postEditor'
import { useUserStore } from '@/stores'
import { getRequestErrorMessage } from '@/utils/request'
import { normalizeTagInput } from '@/utils/tags'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const postFormRef = ref(null)
const coverInputRef = ref(null)
const editorLayoutRef = ref(null)
const editorSidebarRef = ref(null)
const editorMainRef = ref(null)
const postForm = ref(createEmptyPostForm())
const loading = ref(true)
const saving = ref(false)
const coverUploading = ref(false)

let sidebarResizeObserver = null
let removeResizeListener = null

const categories = POST_CATEGORIES
const isAdmin = computed(() => ['admin', 'manager'].includes(userStore.user?.role))
const postId = computed(() => {
  const value = Number(route.query.postId)
  return Number.isFinite(value) && value > 0 ? value : null
})
const reviewId = computed(() => {
  const value = Number(route.query.reviewId)
  return Number.isFinite(value) && value > 0 ? value : null
})

const postRules = {
  title: [{ required: true, message: '请输入文章标题', trigger: 'blur' }],
  category: [{ required: true, message: '请选择文章分类', trigger: 'change' }],
  tags: [{ required: true, message: '请输入文章标签', trigger: 'blur' }]
}

const editorTitle = computed(() => {
  if (isAdmin.value) {
    return postId.value ? '编辑文章' : '新增文章'
  }
  return reviewId.value || postId.value ? '编辑投稿' : '提交文章'
})

const editorSubtitle = computed(() => {
  if (isAdmin.value) {
    return '现在改成了独立页面编辑，工具栏和正文区域不会再被弹窗挤压。'
  }
  return '左侧维护分类、标签和标题背景图，右侧专注写正文。'
})

const currentStatusLabel = computed(() => {
  if (reviewId.value) return '投稿草稿'
  if (postId.value && !isAdmin.value) return '文章草稿'
  if (postId.value && isAdmin.value) return '文章编辑中'
  return '新建中'
})

const normalizePostForm = (payload = {}) => ({
  ...createEmptyPostForm(),
  title: payload.title || '',
  category: payload.category || '',
  excerpt: payload.excerpt || payload.summary || '',
  content: payload.content || '',
  tags: normalizeTagInput(payload.tags || ''),
  coverImage: payload.coverImage || '',
  status: payload.status || payload.postStatus || 'draft'
})

const loadPostDetail = async () => {
  loading.value = true
  try {
    if (reviewId.value) {
      const res = await adminApi.getPostReviewDetail(reviewId.value)
      postForm.value = normalizePostForm(res.data)
      return
    }

    if (postId.value) {
      const res = await adminApi.getPostDetail(postId.value)
      postForm.value = normalizePostForm(res.data)
      return
    }

    postForm.value = createEmptyPostForm()
  } catch (error) {
    ElMessage.error(getRequestErrorMessage(error, '加载文章失败'))
    goBack()
  } finally {
    loading.value = false
  }
}

const buildPayload = () => ({
  ...postForm.value,
  excerpt: postForm.value.excerpt || '',
  summary: postForm.value.excerpt || '',
  tags: normalizeTagInput(postForm.value.tags || ''),
  status: 'draft'
})

const syncRouteAfterSave = async (saved) => {
  if (isAdmin.value) {
    const nextPostId = saved?.id || postId.value
    if (nextPostId && nextPostId !== postId.value) {
      await router.replace({ name: 'AdminPostEditor', query: { postId: nextPostId } })
    }
    return
  }

  const nextReviewId = saved?.id || reviewId.value
  const nextPostId = saved?.postId || postId.value
  const nextQuery = nextReviewId ? { reviewId: nextReviewId } : (nextPostId ? { postId: nextPostId } : {})

  if (
    String(nextQuery.reviewId || '') !== String(route.query.reviewId || '')
    || String(nextQuery.postId || '') !== String(route.query.postId || '')
  ) {
    await router.replace({ name: 'AdminPostEditor', query: nextQuery })
  }
}

const savePost = async ({ exitAfterSave = false } = {}) => {
  if (!postFormRef.value) return

  let valid = false
  await postFormRef.value.validate((value) => { valid = value }).catch(() => { valid = false })
  if (!valid) return

  if (!postForm.value.content?.trim()) {
    ElMessage.warning('请输入文章内容')
    return
  }

  saving.value = true
  try {
    const payload = buildPayload()
    let res

    if (isAdmin.value) {
      res = postId.value
        ? await adminApi.updatePost(postId.value, payload)
        : await adminApi.createPost(payload)
    } else if (reviewId.value) {
      res = await adminApi.updatePostReview(reviewId.value, payload)
    } else if (postId.value) {
      res = await adminApi.savePostDraft(postId.value, payload)
    } else {
      res = await adminApi.createPostReview(payload)
    }

    await syncRouteAfterSave(res.data)
    ElMessage.success('保存成功')

    if (exitAfterSave) {
      goBack()
    }
  } catch (error) {
    ElMessage.error(getRequestErrorMessage(error, '保存失败'))
  } finally {
    saving.value = false
  }
}

const triggerCoverFileSelect = () => {
  coverInputRef.value?.click()
}

const handleCoverFileChange = async (event) => {
  const file = event.target.files?.[0]
  if (!file) {
    event.target.value = ''
    return
  }

  coverUploading.value = true
  try {
    const formData = new FormData()
    formData.append('images', file)
    const res = await adminApi.uploadPostImages(formData)
    const nextUrl = res.data?.urls?.[0]
    if (!nextUrl) {
      throw new Error('未获取到图片地址')
    }
    postForm.value.coverImage = nextUrl
    ElMessage.success('标题背景图上传成功')
  } catch (error) {
    ElMessage.error(getRequestErrorMessage(error, '标题背景图上传失败'))
  } finally {
    coverUploading.value = false
    event.target.value = ''
  }
}

const clearCoverImage = () => {
  postForm.value.coverImage = ''
}

const goBack = () => {
  router.push({ name: 'Admin', query: { menu: 'posts' } })
}

const syncEditorPanelHeight = async () => {
  await nextTick()

  const sidebarEl = editorSidebarRef.value
  const mainEl = editorMainRef.value
  if (!sidebarEl || !mainEl) {
    return
  }

  if (window.innerWidth <= 1080) {
    mainEl.style.removeProperty('height')
    mainEl.style.removeProperty('min-height')
    return
  }

  const sidebarHeight = Math.ceil(sidebarEl.getBoundingClientRect().height)
  if (!sidebarHeight) {
    return
  }

  mainEl.style.height = `${sidebarHeight}px`
  mainEl.style.minHeight = `${sidebarHeight}px`
}

watch(
  () => [route.query.postId, route.query.reviewId],
  () => {
    loadPostDetail()
  },
  { immediate: true }
)

watch(
  loading,
  async (value) => {
    if (!value) {
      await syncEditorPanelHeight()
    }
  }
)

onMounted(() => {
  const handleResize = () => {
    syncEditorPanelHeight()
  }

  window.addEventListener('resize', handleResize)
  removeResizeListener = () => {
    window.removeEventListener('resize', handleResize)
  }

  if (typeof ResizeObserver !== 'undefined') {
    sidebarResizeObserver = new ResizeObserver(() => {
      syncEditorPanelHeight()
    })

    if (editorSidebarRef.value) {
      sidebarResizeObserver.observe(editorSidebarRef.value)
    }
  }

  syncEditorPanelHeight()
})

onBeforeUnmount(() => {
  removeResizeListener?.()
  removeResizeListener = null
  sidebarResizeObserver?.disconnect()
  sidebarResizeObserver = null
})
</script>

<style scoped>
.editor-page {
  min-height: 100vh;
  padding: 24px;
  box-sizing: border-box;
  overflow-x: hidden;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
  background:
    radial-gradient(circle at top right, color-mix(in srgb, var(--accent-light) 90%, transparent), transparent 30%),
    linear-gradient(180deg, var(--bg-primary), color-mix(in srgb, var(--bg-card) 70%, var(--bg-primary)));
}

.editor-topbar {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 20px;
  margin-bottom: 20px;
  flex-shrink: 0;
}

.back-link {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 10px 16px;
  border: 1px solid var(--border-color);
  background: color-mix(in srgb, var(--bg-card) 90%, transparent);
  color: var(--text-secondary);
  border-radius: 14px;
  cursor: pointer;
  transition: all 0.22s ease;
}

.back-link:hover {
  color: var(--accent);
  background: var(--accent-light);
}

.editor-title {
  margin: 18px 0 8px;
  color: var(--text-primary);
  font-size: 2rem;
  line-height: 1.2;
}

.editor-subtitle {
  margin: 0;
  color: var(--text-secondary);
  line-height: 1.7;
}

.editor-actions {
  display: flex;
  align-items: center;
  gap: 12px;
}

.state-card {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  min-height: 300px;
  gap: 16px;
  border: 1px solid var(--border-color);
  border-radius: 28px;
  background: var(--bg-card);
}

.loading-spinner {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  border: 3px solid var(--border-color);
  border-top-color: var(--accent);
  animation: spin 0.8s linear infinite;
}

@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}

.editor-layout {
  display: grid;
  grid-template-columns: 340px minmax(0, 1fr);
  gap: 20px;
  flex: 1 0 auto;
  min-height: max-content;
  align-items: start;
}

.editor-sidebar,
.editor-main {
  min-height: 0;
  border: 1px solid var(--border-color);
  border-radius: 28px;
  background: color-mix(in srgb, var(--bg-card) 95%, transparent);
  box-shadow: var(--shadow-md);
}

.editor-sidebar {
  padding: 24px;
  align-self: start;
  overflow: visible;
}

.editor-form :deep(.el-form-item__label) {
  color: var(--text-secondary);
  font-weight: 600;
}

.editor-form :deep(.el-input),
.editor-form :deep(.el-textarea),
.editor-form :deep(.el-select) {
  --el-input-bg-color: var(--bg-tertiary);
  --el-fill-color-blank: var(--bg-tertiary);
  --el-fill-color-light: var(--bg-tertiary);
  --el-border-color: var(--border-color);
  --el-border-color-hover: var(--accent);
  --el-input-focus-border-color: var(--accent);
  --el-text-color-regular: var(--text-primary);
  --el-text-color-placeholder: var(--text-tertiary);
}

.editor-form :deep(.el-input__wrapper),
.editor-form :deep(.el-textarea__inner),
.editor-form :deep(.el-select__wrapper) {
  background: var(--bg-tertiary);
  box-shadow: 0 0 0 1px var(--border-color) inset !important;
}

.title-input :deep(.el-input__inner) {
  font-size: 1.15rem;
  font-weight: 600;
}

.field-select {
  width: 100%;
}

.cover-file-input {
  display: none;
}

.cover-panel {
  display: grid;
  gap: 14px;
}

.cover-preview {
  position: relative;
  min-height: 190px;
  border-radius: 20px;
  overflow: hidden;
  background:
    linear-gradient(135deg, color-mix(in srgb, var(--accent-light) 90%, var(--bg-card)), var(--bg-card));
  border: 1px dashed color-mix(in srgb, var(--accent) 35%, var(--border-color));
}

.cover-preview img {
  width: 100%;
  height: 100%;
  min-height: 190px;
  object-fit: cover;
  display: block;
}

.cover-placeholder {
  min-height: 190px;
  display: flex;
  flex-direction: column;
  justify-content: center;
  padding: 24px;
  color: var(--text-secondary);
  gap: 8px;
}

.cover-placeholder-title {
  font-size: 1.05rem;
  font-weight: 700;
  color: var(--text-primary);
}

.cover-placeholder-desc {
  line-height: 1.7;
}

.cover-preview-overlay {
  position: absolute;
  inset: auto 0 0 0;
  padding: 20px 18px 16px;
  color: #fff;
  font-weight: 700;
  font-size: 1rem;
  background: linear-gradient(180deg, transparent, rgba(0, 0, 0, 0.68));
}

.cover-actions-row {
  display: flex;
  gap: 10px;
}

.cover-actions-row :deep(.el-button:nth-child(2)) {
  --el-button-bg-color: #fff5f1;
  --el-button-border-color: #f0c2b2;
  --el-button-text-color: #a75738;
  --el-button-hover-bg-color: #ffe8df;
  --el-button-hover-border-color: #d9805d;
  --el-button-hover-text-color: #8b4226;
  --el-button-disabled-bg-color: #f9efea;
  --el-button-disabled-border-color: #ecd6cb;
  --el-button-disabled-text-color: #c0927f;
}

:global(html.dark) .cover-actions-row :deep(.el-button:nth-child(2)),
:global(.dark) .cover-actions-row :deep(.el-button:nth-child(2)) {
  --el-button-bg-color: var(--bg-tertiary);
  --el-button-border-color: var(--border-color);
  --el-button-text-color: var(--text-primary);
  --el-button-hover-bg-color: color-mix(in srgb, var(--accent-light) 72%, var(--bg-card));
  --el-button-hover-border-color: var(--accent);
  --el-button-hover-text-color: var(--accent);
  --el-button-disabled-bg-color: color-mix(in srgb, var(--bg-tertiary) 92%, transparent);
  --el-button-disabled-border-color: var(--border-color);
  --el-button-disabled-text-color: var(--text-secondary);
}

.editor-main {
  display: flex;
  flex-direction: column;
  padding: 18px;
  min-height: 760px;
  overflow: visible;
}

.editor-main-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20px;
  padding: 6px 6px 16px;
}

.section-title {
  color: var(--text-primary);
  font-size: 1.1rem;
  font-weight: 700;
}

.section-desc {
  margin-top: 6px;
  color: var(--text-secondary);
}

.editor-status-badge {
  padding: 10px 14px;
  border-radius: 999px;
  background: var(--accent-light);
  color: var(--accent);
  font-size: 0.9rem;
  font-weight: 700;
  white-space: nowrap;
}

.editor-main-body {
  flex: 1;
  min-height: 680px;
  overflow: visible;
}

@media (max-width: 1080px) {
  .editor-layout {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 768px) {
  .editor-page {
    min-height: 100vh;
    padding: 16px;
    overflow-x: hidden;
    overflow-y: auto;
  }

  .editor-topbar {
    flex-direction: column;
  }

  .editor-actions {
    width: 100%;
    flex-wrap: wrap;
  }

  .editor-sidebar,
  .editor-main {
    border-radius: 22px;
  }

  .editor-layout {
    overflow: visible;
  }

  .editor-sidebar,
  .editor-main,
  .editor-main-body {
    overflow: visible;
  }
}
</style>
