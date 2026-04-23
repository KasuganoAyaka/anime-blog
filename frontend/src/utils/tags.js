const TAG_SEPARATOR_REGEX = /[,\uFF0C]/

export const parseTagList = (tagsValue = '', { limit } = {}) => {
  if (!tagsValue) return []

  const tags = String(tagsValue)
    .split(TAG_SEPARATOR_REGEX)
    .map(tag => tag.trim())
    .filter(Boolean)

  return Number.isInteger(limit) && limit >= 0 ? tags.slice(0, limit) : tags
}

export const normalizeTagInput = (tagsValue = '') => parseTagList(tagsValue).join(', ')
