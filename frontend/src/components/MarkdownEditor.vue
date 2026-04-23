<template>
  <div class="markdown-editor-shell">
    <div class="editor-toolbar" role="toolbar" aria-label="文章编辑工具栏">
      <div class="editor-toolbar-scroll" @wheel="handleToolbarWheel">
        <section class="toolbar-group">
          <span class="toolbar-group-title">文本</span>
          <div class="toolbar-group-actions">
            <el-dropdown trigger="click" @command="handleHeadingCommand">
              <button type="button" class="toolbar-button toolbar-button-wide">
                <span class="toolbar-button-icon toolbar-button-icon-text">H</span>
                <span>标题</span>
              </button>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item command="h1">一级标题</el-dropdown-item>
                  <el-dropdown-item command="h2">二级标题</el-dropdown-item>
                  <el-dropdown-item command="h3">三级标题</el-dropdown-item>
                  <el-dropdown-item command="h4">四级标题</el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>

            <button
              v-for="action in textActions"
              :key="action.command"
              type="button"
              class="toolbar-button"
              :title="action.label"
              @click="runEditorCommand(action.command)"
            >
              <span v-if="action.iconText" class="toolbar-button-icon toolbar-button-icon-text">{{ action.iconText }}</span>
              <el-icon v-else class="toolbar-button-icon">
                <component :is="action.icon" />
              </el-icon>
              {{ action.label }}
            </button>
          </div>
        </section>

        <section class="toolbar-group">
          <span class="toolbar-group-title">段落</span>
          <div class="toolbar-group-actions">
            <button
              v-for="action in blockActions"
              :key="action.command"
              type="button"
              class="toolbar-button"
              :title="action.label"
              @click="runEditorCommand(action.command)"
            >
              <el-icon class="toolbar-button-icon">
                <component :is="action.icon" />
              </el-icon>
              {{ action.label }}
            </button>
          </div>
        </section>

        <section class="toolbar-group">
          <span class="toolbar-group-title">插入</span>
          <div class="toolbar-group-actions">
            <button type="button" class="toolbar-button" title="插入链接" @click="insertLink">
              <el-icon class="toolbar-button-icon"><Link /></el-icon>
              <span>链接</span>
            </button>

            <el-dropdown trigger="click" @command="handleImageCommand">
              <button type="button" class="toolbar-button">
                <el-icon class="toolbar-button-icon"><Picture /></el-icon>
                <span>图片</span>
              </button>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item command="upload">上传图片</el-dropdown-item>
                  <el-dropdown-item command="link">图片链接</el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>

            <button type="button" class="toolbar-button" title="插入表格" @click="insertTable">
              <el-icon class="toolbar-button-icon"><Grid /></el-icon>
              <span>表格</span>
            </button>

            <el-dropdown trigger="click" @command="handleMermaidCommand">
              <button type="button" class="toolbar-button toolbar-button-wide">
                <el-icon class="toolbar-button-icon"><Share /></el-icon>
                <span>Mermaid</span>
              </button>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item command="flow">流程图</el-dropdown-item>
                  <el-dropdown-item command="sequence">时序图</el-dropdown-item>
                  <el-dropdown-item command="gantt">甘特图</el-dropdown-item>
                  <el-dropdown-item command="class">类图</el-dropdown-item>
                  <el-dropdown-item command="state">状态图</el-dropdown-item>
                  <el-dropdown-item command="pie">饼图</el-dropdown-item>
                  <el-dropdown-item command="relationship">关系图</el-dropdown-item>
                  <el-dropdown-item command="journey">旅程图</el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>

            <el-dropdown trigger="click" @command="handleKatexCommand">
              <button type="button" class="toolbar-button">
                <el-icon class="toolbar-button-icon"><DataAnalysis /></el-icon>
                <span>公式</span>
              </button>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item command="katexInline">行内公式</el-dropdown-item>
                  <el-dropdown-item command="katexBlock">块级公式</el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </div>
        </section>

        <section class="toolbar-group">
          <span class="toolbar-group-title">编辑</span>
          <div class="toolbar-group-actions">
            <button type="button" class="toolbar-button" title="撤销" @click="undoChange">
              <el-icon class="toolbar-button-icon"><RefreshLeft /></el-icon>
              <span>撤销</span>
            </button>
            <button type="button" class="toolbar-button" title="重做" @click="redoChange">
              <el-icon class="toolbar-button-icon"><RefreshRight /></el-icon>
              <span>重做</span>
            </button>
            <button type="button" class="toolbar-button" title="格式化 Markdown" @click="runEditorCommand('prettier')">
              <el-icon class="toolbar-button-icon"><MagicStick /></el-icon>
              <span>美化</span>
            </button>
          </div>
        </section>

        <section class="toolbar-group">
          <span class="toolbar-group-title">视图</span>
          <div class="toolbar-group-actions">
            <button
              type="button"
              class="toolbar-button"
              :class="{ active: viewState.preview }"
              title="切换预览"
              @click="togglePreview"
            >
              <el-icon class="toolbar-button-icon"><View /></el-icon>
              <span>预览</span>
            </button>
            <button
              type="button"
              class="toolbar-button"
              :class="{ active: viewState.previewOnly }"
              title="仅预览"
              @click="togglePreviewOnly"
            >
              <el-icon class="toolbar-button-icon"><Document /></el-icon>
              <span>仅预览</span>
            </button>
            <button
              type="button"
              class="toolbar-button"
              :class="{ active: viewState.htmlPreview }"
              title="HTML 预览"
              @click="toggleHtmlPreview"
            >
              <span class="toolbar-button-icon toolbar-button-icon-text toolbar-button-icon-code">&lt;/&gt;</span>
              <span>HTML</span>
            </button>
            <button
              type="button"
              class="toolbar-button"
              :class="{ active: viewState.catalog }"
              title="切换目录"
              @click="toggleCatalog"
            >
              <el-icon class="toolbar-button-icon"><Menu /></el-icon>
              <span>目录</span>
            </button>
          </div>
        </section>
      </div>
    </div>

    <input
      ref="imageInputRef"
      type="file"
      accept="image/png,image/jpeg,image/webp,image/gif"
      multiple
      class="toolbar-file-input"
      @change="handleToolbarImageUpload"
    />

    <MdEditor
      ref="editorRef"
      editor-id="admin-post-editor"
      :model-value="props.modelValue"
      :theme="editorTheme"
      :preview-theme="previewTheme"
      :code-theme="codeTheme"
      :language="editorLanguage"
      :toolbars="emptyToolbars"
      :floating-toolbars="emptyToolbars"
      :sanitize="sanitizeRichHtml"
      :catalog-layout="'fixed'"
      :show-code-row-number="true"
      :code-foldable="true"
      :auto-fold-threshold="18"
      :scroll-auto="true"
      :auto-detect-code="true"
      :on-upload-img="handleUploadImages"
      placeholder="使用 Markdown 编写文章内容..."
      class="markdown-editor"
      @update:model-value="handleChange"
    />
  </div>
</template>

<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { undo, redo } from '@codemirror/commands'
import {
  ChatDotRound,
  CollectionTag,
  DataAnalysis,
  Document,
  EditPen,
  Finished,
  Grid,
  Link,
  List,
  MagicStick,
  Menu,
  Picture,
  RefreshLeft,
  RefreshRight,
  Share,
  Tickets,
  View
} from '@element-plus/icons-vue'
import { MdEditor } from 'md-editor-v3'
import 'md-editor-v3/lib/style.css'
import { adminApi } from '@/api'
import { useLangStore, useThemeStore } from '@/stores'
import { sanitizeRichHtml } from '@/utils/content'
import { getRequestErrorMessage } from '@/utils/request'

const props = defineProps({
  modelValue: { type: String, default: '' }
})

const emit = defineEmits(['update:modelValue'])

const editorRef = ref(null)
const imageInputRef = ref(null)
const themeStore = useThemeStore()
const langStore = useLangStore()

const emptyToolbars = Object.freeze([])
const textActions = Object.freeze([
  { label: '粗体', command: 'bold', iconText: 'B' },
  { label: '斜体', command: 'italic', iconText: 'I' },
  { label: '下划线', command: 'underline', iconText: 'U' },
  { label: '删除线', command: 'strikeThrough', iconText: 'S' },
  { label: '下标', command: 'sub', iconText: 'X₂' },
  { label: '上标', command: 'sup', iconText: 'X²' }
])
const blockActions = Object.freeze([
  { label: '引用', command: 'quote', icon: ChatDotRound },
  { label: '无序列表', command: 'unorderedList', icon: List },
  { label: '有序列表', command: 'orderedList', icon: Tickets },
  { label: '任务列表', command: 'task', icon: Finished },
  { label: '行内代码', command: 'codeRow', icon: EditPen },
  { label: '代码块', command: 'code', icon: CollectionTag }
])

const viewState = reactive({
  preview: true,
  previewOnly: false,
  htmlPreview: false,
  catalog: false
})

const editorTheme = computed(() => (themeStore.isDark ? 'dark' : 'light'))
const editorLanguage = computed(() => (langStore.lang === 'en' ? 'en-US' : 'zh-CN'))
const previewTheme = computed(() => 'github')
const codeTheme = computed(() => (themeStore.isDark ? 'atom' : 'github'))
let toolbarWheelFrame = 0
let toolbarWheelPending = 0
let toolbarWheelTarget = null

const flushToolbarWheel = () => {
  if (toolbarWheelTarget && toolbarWheelPending !== 0) {
    toolbarWheelTarget.scrollLeft += toolbarWheelPending
  }

  toolbarWheelFrame = 0
  toolbarWheelPending = 0
  toolbarWheelTarget = null
}

const handleChange = (value) => {
  emit('update:modelValue', value)
}

const handleToolbarWheel = (event) => {
  const target = event.currentTarget
  if (!(target instanceof HTMLElement)) {
    return
  }

  const hasHorizontalOverflow = target.scrollWidth > target.clientWidth + 4
  const usingVerticalWheel = Math.abs(event.deltaY) > Math.abs(event.deltaX)
  if (!hasHorizontalOverflow || !usingVerticalWheel) {
    return
  }

  const deltaUnit = event.deltaMode === 1 ? 28 : event.deltaMode === 2 ? target.clientWidth * 0.9 : 1
  const rawDelta = event.deltaY * deltaUnit
  const minimumStep = Math.min(196, Math.max(116, target.clientWidth * 0.22))
  const scrollDelta = Math.sign(rawDelta) * Math.max(Math.abs(rawDelta) * 2.8, minimumStep)

  event.preventDefault()

  if (toolbarWheelTarget && toolbarWheelTarget !== target) {
    if (toolbarWheelFrame) {
      cancelAnimationFrame(toolbarWheelFrame)
    }
    flushToolbarWheel()
  }

  toolbarWheelTarget = target
  toolbarWheelPending += scrollDelta

  if (!toolbarWheelFrame) {
    toolbarWheelFrame = requestAnimationFrame(() => {
      flushToolbarWheel()
    })
  }
}

const withEditor = (callback) => {
  const editor = editorRef.value
  if (!editor) {
    return null
  }

  return callback(editor)
}

const runEditorCommand = (command) => {
  withEditor((editor) => {
    editor.execCommand(command)
  })
}

const insertByGenerator = (generator) => {
  withEditor((editor) => {
    editor.insert(generator)
  })
}

const undoChange = () => {
  withEditor((editor) => {
    const view = editor.getEditorView?.()
    if (!view) {
      return
    }

    undo(view)
    view.focus()
  })
}

const redoChange = () => {
  withEditor((editor) => {
    const view = editor.getEditorView?.()
    if (!view) {
      return
    }

    redo(view)
    view.focus()
  })
}

const togglePreview = () => {
  withEditor((editor) => {
    editor.togglePreview()
  })
}

const togglePreviewOnly = () => {
  withEditor((editor) => {
    editor.togglePreviewOnly()
  })
}

const toggleHtmlPreview = () => {
  withEditor((editor) => {
    editor.toggleHtmlPreview()
  })
}

const toggleCatalog = () => {
  withEditor((editor) => {
    editor.toggleCatalog()
  })
}

const handleHeadingCommand = (command) => {
  runEditorCommand(command)
}

const handleMermaidCommand = (command) => {
  runEditorCommand(command)
}

const handleKatexCommand = (command) => {
  runEditorCommand(command)
}

const buildTableMarkdown = (rows = 3, columns = 3) => {
  const headerCells = Array.from({ length: columns }, (_, index) => `Column ${index + 1}`)
  const alignRow = Array.from({ length: columns }, () => '---')
  const bodyRows = Array.from({ length: rows }, (_, rowIndex) => {
    const cells = Array.from({ length: columns }, (_, columnIndex) => `Cell ${rowIndex + 1}-${columnIndex + 1}`)
    return `| ${cells.join(' | ')} |`
  })

  return [
    `| ${headerCells.join(' | ')} |`,
    `| ${alignRow.join(' | ')} |`,
    ...bodyRows,
    ''
  ].join('\n')
}

const insertTable = () => {
  insertByGenerator(() => ({
    targetValue: buildTableMarkdown(),
    select: false
  }))
}

const insertLink = async () => {
  try {
    const { value } = await ElMessageBox.prompt('请输入链接地址', '插入链接', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      inputPlaceholder: 'https://example.com'
    })

    if (!value) {
      return
    }

    insertByGenerator((selectedText) => ({
      targetValue: `[${selectedText || '链接文本'}](${value})`,
      select: false
    }))
  } catch {
    // ignore cancel
  }
}

const insertImageByUrl = async () => {
  try {
    const { value } = await ElMessageBox.prompt('请输入图片地址', '插入图片', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      inputPlaceholder: 'https://example.com/image.png'
    })

    if (!value) {
      return
    }

    insertByGenerator((selectedText) => ({
      targetValue: `![${selectedText || '图片'}](${value})\n`,
      select: false
    }))
  } catch {
    // ignore cancel
  }
}

const triggerImageUpload = () => {
  imageInputRef.value?.click()
}

const handleImageCommand = (command) => {
  if (command === 'upload') {
    triggerImageUpload()
    return
  }

  if (command === 'link') {
    insertImageByUrl()
  }
}

const uploadImages = async (files) => {
  const fileList = Array.isArray(files) ? files : Array.from(files || [])
  if (!fileList.length) {
    return []
  }

  const formData = new FormData()
  fileList.forEach((file) => formData.append('images', file))

  const res = await adminApi.uploadPostImages(formData)
  const urls = Array.isArray(res.data?.urls) ? res.data.urls : []
  if (!urls.length) {
    throw new Error('未获取到图片地址')
  }

  return urls
}

const handleUploadImages = async (files, callback) => {
  try {
    const urls = await uploadImages(files)
    callback(urls)
    ElMessage.success(`已上传 ${urls.length} 张图片`)
  } catch (error) {
    ElMessage.error(getRequestErrorMessage(error, '图片上传失败'))
  }
}

const handleToolbarImageUpload = async (event) => {
  const files = Array.from(event.target.files || [])
  if (!files.length) {
    return
  }

  try {
    const urls = await uploadImages(files)
    insertByGenerator(() => ({
      targetValue: `${urls.map((url) => `![](${url})`).join('\n')}\n`,
      select: false
    }))
    ElMessage.success(`已上传 ${urls.length} 张图片`)
  } catch (error) {
    ElMessage.error(getRequestErrorMessage(error, '图片上传失败'))
  } finally {
    event.target.value = ''
  }
}

const bindEditorEvents = () => {
  const editor = editorRef.value
  if (!editor?.on) {
    return
  }

  editor.on('preview', (status) => {
    viewState.preview = status
  })
  editor.on('previewOnly', (status) => {
    viewState.previewOnly = status
  })
  editor.on('htmlPreview', (status) => {
    viewState.htmlPreview = status
  })
  editor.on('catalog', (status) => {
    viewState.catalog = status
  })
}

onMounted(async () => {
  await nextTick()
  bindEditorEvents()
})

onBeforeUnmount(() => {
  if (toolbarWheelFrame) {
    cancelAnimationFrame(toolbarWheelFrame)
  }
})
</script>

<style scoped>
.markdown-editor-shell {
  height: 100%;
  min-height: 0;
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.editor-toolbar {
  border: 1px solid color-mix(in srgb, var(--border-color) 88%, transparent);
  border-radius: 20px;
  background:
    linear-gradient(180deg, color-mix(in srgb, var(--accent-light) 70%, var(--bg-card)), var(--bg-card));
  box-shadow: 0 12px 28px rgba(15, 23, 42, 0.08);
}

.editor-toolbar-scroll {
  display: flex;
  gap: 12px;
  padding: 14px 14px 10px;
  overflow-x: auto;
  overflow-y: hidden;
  scroll-behavior: auto;
  scrollbar-width: thin;
  scrollbar-color: transparent transparent;
  scrollbar-gutter: stable;
}

.editor-toolbar:hover .editor-toolbar-scroll,
.editor-toolbar:focus-within .editor-toolbar-scroll {
  scrollbar-color:
    color-mix(in srgb, var(--accent) 52%, var(--border-color))
    color-mix(in srgb, var(--bg-card) 92%, var(--accent-light));
}

.editor-toolbar-scroll::-webkit-scrollbar {
  height: 10px;
}

.editor-toolbar-scroll::-webkit-scrollbar-track {
  background: transparent;
  border-radius: 999px;
  transition: background-color 0.24s ease;
}

.editor-toolbar:hover .editor-toolbar-scroll::-webkit-scrollbar-track,
.editor-toolbar:focus-within .editor-toolbar-scroll::-webkit-scrollbar-track {
  background: color-mix(in srgb, var(--bg-card) 92%, var(--accent-light));
}

.editor-toolbar-scroll::-webkit-scrollbar-thumb {
  background: transparent;
  border-radius: 999px;
  border: 2px solid transparent;
  transition:
    background-color 0.24s ease,
    border-color 0.24s ease;
}

.editor-toolbar:hover .editor-toolbar-scroll::-webkit-scrollbar-thumb,
.editor-toolbar:focus-within .editor-toolbar-scroll::-webkit-scrollbar-thumb {
  background: color-mix(in srgb, var(--accent) 52%, var(--border-color));
  border: 2px solid color-mix(in srgb, var(--bg-card) 92%, var(--accent-light));
}

.editor-toolbar:hover .editor-toolbar-scroll::-webkit-scrollbar-thumb:hover,
.editor-toolbar:focus-within .editor-toolbar-scroll::-webkit-scrollbar-thumb:hover {
  background: color-mix(in srgb, var(--accent) 68%, var(--text-secondary));
}

.toolbar-group {
  flex: 0 0 auto;
  min-width: max-content;
  display: grid;
  gap: 8px;
  padding: 12px;
  border: 1px solid color-mix(in srgb, var(--border-color) 84%, transparent);
  border-radius: 16px;
  background: color-mix(in srgb, var(--bg-card) 92%, transparent);
}

.toolbar-group-title {
  font-size: 0.76rem;
  font-weight: 700;
  letter-spacing: 0.04em;
  color: var(--text-secondary);
}

.toolbar-group-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.toolbar-button {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  flex: 0 0 auto;
  min-height: 36px;
  padding: 0 12px;
  border: 1px solid color-mix(in srgb, var(--border-color) 88%, transparent);
  border-radius: 12px;
  background: color-mix(in srgb, var(--bg-card) 96%, transparent);
  color: var(--text-primary);
  font-size: 0.9rem;
  line-height: 1;
  white-space: nowrap;
  cursor: pointer;
  transition:
    border-color 0.2s ease,
    background-color 0.2s ease,
    color 0.2s ease,
    box-shadow 0.2s ease;
  transform: none !important;
}

.toolbar-button:hover,
.toolbar-button:active {
  transform: none !important;
}

.toolbar-button:hover {
  border-color: color-mix(in srgb, var(--accent) 42%, var(--border-color));
  background: color-mix(in srgb, var(--accent-light) 88%, var(--bg-card));
  color: var(--accent);
}

.toolbar-button.active {
  border-color: color-mix(in srgb, var(--accent) 48%, var(--border-color));
  background: color-mix(in srgb, var(--accent-light) 92%, var(--bg-card));
  color: var(--accent);
  box-shadow: 0 0 0 1px color-mix(in srgb, var(--accent) 24%, transparent) inset;
}

.toolbar-button-wide {
  min-width: 68px;
}

.toolbar-button-icon {
  flex: 0 0 auto;
  font-size: 1rem;
}

.toolbar-button-icon-text {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 1.1em;
  font-size: 0.95em;
  font-weight: 700;
}

.toolbar-button-icon-code {
  font-family: 'JetBrains Mono', 'Fira Code', Consolas, monospace;
  font-size: 0.84em;
  letter-spacing: -0.04em;
}

.toolbar-file-input {
  display: none;
}

.markdown-editor-shell :deep(.markdown-editor) {
  flex: 1 1 auto;
  min-height: 0;
  border-radius: 20px;
  border-color: var(--border-color);
  background: var(--bg-card);
  box-shadow: var(--shadow-sm);
  --md-color: var(--text-primary);
  --md-hover-color: var(--text-primary);
  --md-bk-color: var(--bg-card);
  --md-bk-color-outstand: var(--bg-tertiary);
  --md-bk-hover-color: var(--accent-light);
  --md-border-color: var(--border-color);
  --md-border-hover-color: var(--accent);
  --md-border-active-color: var(--accent);
  --md-scrollbar-bg-color: var(--bg-tertiary);
  --md-scrollbar-thumb-color: color-mix(in srgb, var(--text-secondary) 30%, transparent);
  --md-scrollbar-thumb-hover-color: color-mix(in srgb, var(--text-secondary) 45%, transparent);
  --md-scrollbar-thumb-active-color: color-mix(in srgb, var(--text-secondary) 60%, transparent);
  --editor-selection-bg: color-mix(in srgb, var(--accent) 42%, transparent);
  --editor-selection-bg-strong: color-mix(in srgb, var(--accent) 58%, var(--bg-card));
  --editor-selection-text: var(--text-primary);
  --editor-selection-outline: color-mix(in srgb, var(--accent) 72%, var(--text-primary) 28%);
}

.markdown-editor-shell :deep(.md-editor-content),
.markdown-editor-shell :deep(.md-editor-content-wrapper),
.markdown-editor-shell :deep(.md-editor-input-wrapper),
.markdown-editor-shell :deep(.md-editor-preview-wrapper) {
  min-height: 0;
  background: var(--bg-card);
}

.markdown-editor-shell :deep(.cm-editor),
.markdown-editor-shell :deep(.cm-scroller),
.markdown-editor-shell :deep(.cm-content),
.markdown-editor-shell :deep(.md-editor-preview) {
  background: var(--bg-card);
  color: var(--text-primary);
}

.markdown-editor-shell :deep(.cm-content) {
  caret-color: var(--accent);
  font-size: 0.98rem;
  line-height: 1.8;
}

.markdown-editor-shell :deep(.cm-editor ::selection),
.markdown-editor-shell :deep(.cm-scroller ::selection),
.markdown-editor-shell :deep(.cm-content ::selection),
.markdown-editor-shell :deep(.cm-line::selection),
.markdown-editor-shell :deep(.cm-line *::selection) {
  background: var(--editor-selection-bg-strong) !important;
  color: var(--editor-selection-text) !important;
}

.markdown-editor-shell :deep(.md-editor-preview::selection) {
  background: var(--editor-selection-bg-strong) !important;
  color: var(--text-primary) !important;
}

.markdown-editor-shell :deep(.md-editor-preview *::selection) {
  background: var(--editor-selection-bg-strong) !important;
  color: var(--text-primary) !important;
}

.markdown-editor-shell :deep(.cm-editor .cm-selectionLayer .cm-selectionBackground),
.markdown-editor-shell :deep(.cm-editor .cm-selectionBackground) {
  background: var(--editor-selection-bg) !important;
  opacity: 1 !important;
  border-radius: 3px;
}

.markdown-editor-shell :deep(.cm-editor.cm-focused .cm-selectionLayer .cm-selectionBackground),
.markdown-editor-shell :deep(.cm-editor.cm-focused .cm-selectionBackground) {
  background: var(--editor-selection-bg-strong) !important;
  opacity: 1 !important;
  box-shadow: 0 0 0 1px var(--editor-selection-outline) inset;
}

.markdown-editor-shell :deep(.cm-selectedLine) {
  background-color: var(--editor-selection-bg) !important;
}

.markdown-editor-shell :deep(.cm-selectionMatch) {
  background: color-mix(in srgb, var(--accent) 24%, transparent) !important;
  outline: 1px solid color-mix(in srgb, var(--accent) 46%, transparent);
}

.markdown-editor-shell :deep(.md-editor-modal) {
  z-index: 40000 !important;
}

.markdown-editor-shell :deep(.md-editor-modal-mask) {
  z-index: 39999 !important;
}

.markdown-editor-shell :deep(.md-editor-preview) {
  padding: 20px 24px;
}

.markdown-editor-shell :deep(.md-editor-preview-wrapper) {
  border-left: 1px solid color-mix(in srgb, var(--border-color) 85%, transparent);
}

.markdown-editor-shell :deep(.md-editor-footer) {
  background: color-mix(in srgb, var(--bg-card) 92%, var(--accent-light));
  border-top-color: var(--border-color);
  color: var(--text-secondary);
}

.markdown-editor-shell :deep(.md-editor-catalog-editor) {
  width: 220px;
  background: color-mix(in srgb, var(--bg-card) 95%, var(--accent-light));
  border-left: 1px solid var(--border-color);
}

.markdown-editor-shell :deep(.md-editor-catalog-link span) {
  color: var(--text-secondary);
}

.markdown-editor-shell :deep(.md-editor-catalog-active > span),
.markdown-editor-shell :deep(.md-editor-catalog-link span:hover) {
  color: var(--accent);
}

.markdown-editor-shell :deep(.md-editor-catalog-indicator) {
  background-color: var(--accent);
}

.markdown-editor-shell :deep(.md-editor-fullscreen) {
  z-index: 4000;
}

.markdown-editor-shell :deep(.cm-gutters) {
  background: color-mix(in srgb, var(--bg-tertiary) 84%, var(--bg-card));
  border-right: 1px solid var(--border-color);
}

.markdown-editor-shell :deep(.md-editor-code pre code) {
  border-radius: 0 0 14px 14px;
}

@media (max-width: 900px) {
  .editor-toolbar-scroll {
    padding: 12px;
  }

  .toolbar-group {
    padding: 10px;
  }
}

@media (max-width: 768px) {
  .markdown-editor-shell :deep(.markdown-editor) {
    min-height: 420px;
  }

  .toolbar-button {
    min-height: 34px;
    padding: 0 10px;
    font-size: 0.84rem;
  }

  .toolbar-group-title {
    font-size: 0.72rem;
  }

  .markdown-editor-shell :deep(.md-editor-catalog-editor) {
    width: 180px;
  }
}
</style>
