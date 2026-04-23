import DOMPurify from 'dompurify'
import { marked } from 'marked'

marked.setOptions({
  breaks: true,
  gfm: true
})

const sanitizeOptions = {
  USE_PROFILES: { html: true },
  ADD_TAGS: ['iframe', 'video', 'source'],
  ADD_ATTR: ['allow', 'allowfullscreen', 'controls', 'frameborder', 'scrolling']
}

export const sanitizeRichHtml = (html = '') => DOMPurify.sanitize(html, sanitizeOptions)

export const renderRichContent = (content = '') => {
  if (!content) return ''
  return sanitizeRichHtml(marked.parse(content))
}
