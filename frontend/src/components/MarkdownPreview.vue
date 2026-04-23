<template>
  <div class="markdown-preview-shell">
    <MdPreview
      :id="previewId"
      :model-value="source"
      :theme="previewTheme"
      :language="previewLanguage"
      :sanitize="sanitizeRichHtml"
      class="markdown-preview"
    />
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { MdPreview } from 'md-editor-v3'
import 'md-editor-v3/lib/preview.css'
import { useLangStore, useThemeStore } from '@/stores'
import { sanitizeRichHtml } from '@/utils/content'

defineProps({
  source: { type: String, default: '' },
  previewId: { type: String, default: 'markdown-preview' }
})

const themeStore = useThemeStore()
const langStore = useLangStore()

const previewTheme = computed(() => (themeStore.isDark ? 'dark' : 'light'))
const previewLanguage = computed(() => (langStore.lang === 'en' ? 'en-US' : 'zh-CN'))
</script>

<style scoped>
.markdown-preview-shell {
  width: 100%;
  min-width: 0;
}

.markdown-preview-shell :deep(.markdown-preview) {
  border: 0;
  height: auto;
  background: transparent;
  color: var(--text-primary);
  --md-color: var(--text-primary);
  --md-bk-color: transparent;
  --md-bk-color-outstand: var(--bg-tertiary);
  --md-theme-bg-color: transparent;
  --md-theme-bg-color-inset: var(--bg-tertiary);
  --md-theme-border-color: var(--border-color);
  --md-theme-link-color: var(--accent);
  --md-theme-link-hover-color: color-mix(in srgb, var(--accent) 82%, white);
}

.markdown-preview-shell :deep(.md-editor-previewOnly) {
  background: transparent;
}

.markdown-preview-shell :deep(.md-editor-preview) {
  padding: 0;
  font-size: 1rem;
  line-height: 1.9;
  color: var(--text-primary);
  word-break: break-word;
}

.markdown-preview-shell :deep(.md-editor-preview > *:first-child) {
  margin-top: 0;
}

.markdown-preview-shell :deep(.md-editor-preview > *:last-child) {
  margin-bottom: 0;
}

.markdown-preview-shell :deep(a) {
  color: var(--accent);
}

.markdown-preview-shell :deep(img) {
  max-width: 100%;
  border-radius: 12px;
}

.markdown-preview-shell :deep(blockquote) {
  color: var(--text-secondary);
}
</style>
