import { adminApi } from '@/api'

const normalizePageData = (payload, fallbackSize = 10) => {
  if (Array.isArray(payload)) {
    return {
      records: payload,
      total: payload.length,
      current: 1,
      size: fallbackSize,
      summary: {},
      summaryProvided: false
    }
  }

  const data = payload && typeof payload === 'object' ? payload : {}
  const records = Array.isArray(data.records) ? data.records : []
  const current = Number(data.current || 1)
  const size = Number(data.size || fallbackSize || records.length || 10)
  const total = Number(data.total ?? records.length)

  return {
    records,
    total: Number.isFinite(total) ? total : records.length,
    current: Number.isFinite(current) && current > 0 ? current : 1,
    size: Number.isFinite(size) && size > 0 ? size : fallbackSize,
    summary: data.summary && typeof data.summary === 'object' ? data.summary : {},
    summaryProvided: Object.prototype.hasOwnProperty.call(data, 'summary')
      && data.summary
      && typeof data.summary === 'object'
  }
}

const resetPageMeta = (meta, fallbackSize = 10) => {
  meta.total = 0
  meta.page = 1
  meta.size = meta.size || fallbackSize
  meta.summary = {}
}

const applyPageData = (target, meta, payload, fallbackSize = 10) => {
  const pageData = normalizePageData(payload, meta?.size || fallbackSize)
  target.value = pageData.records
  meta.total = pageData.total
  meta.page = pageData.current
  meta.size = pageData.size
  if (pageData.summaryProvided) {
    meta.summary = pageData.summary || {}
  }
  return pageData
}

const deriveSummaryByKey = (records, field, values, total = records.length) => {
  const source = Array.isArray(records) ? records : []
  const summary = {
    total: Number(total || 0)
  }

  values.forEach((value) => {
    summary[value] = source.filter((item) => String(item?.[field] || '').toLowerCase() === String(value).toLowerCase()).length
  })

  return summary
}

const mergeSummary = (currentSummary, nextSummary) => ({
  ...(currentSummary && typeof currentSummary === 'object' ? currentSummary : {}),
  ...(nextSummary && typeof nextSummary === 'object' ? nextSummary : {})
})

export const useAdminData = ({
  isAdmin,
  canManageMusic,
  posts,
  postReviews,
  musicList,
  users,
  links,
  adminComments,
  commentReports,
  contactMessages,
  stats,
  listMeta,
  getPostParams = () => ({}),
  getReviewParams = () => ({}),
  getCommentParams = () => ({}),
  getReportParams = () => ({})
}) => {
  const requestState = {
    overview: 0,
    article: 0,
    users: 0,
    links: 0,
    contacts: 0,
    comments: 0,
    reports: 0,
    music: 0
  }

  const beginRequest = (key) => {
    requestState[key] += 1
    return requestState[key]
  }

  const isLatestRequest = (key, requestId) => requestState[key] === requestId

  const fetchOverviewData = async () => {
    const requestId = beginRequest('overview')
    try {
      const res = await adminApi.getOverview()
      const overview = res.data || {}
      if (!isLatestRequest('overview', requestId) || Object.keys(overview).length === 0) {
        return
      }

      if (isAdmin.value) {
        stats.value = {
          ...stats.value,
          posts: Number(overview.posts || 0),
          music: Number(overview.music || 0),
          users: Number(overview.users || 0),
          links: Number(overview.links || 0),
          pendingReviews: Number(overview.pendingReviews || 0),
          pendingComments: Number(overview.pendingComments || 0),
          pendingContacts: Number(overview.pendingContacts || 0)
        }
        return
      }

      stats.value = {
        ...stats.value,
        posts: Number(overview.total || 0),
        pending: Number(overview.pending || 0),
        rejected: Number(overview.rejected || 0),
        published: Number(overview.published || 0)
      }
    } catch (error) {
      console.error(error)
    }
  }

  const fetchArticleData = async () => {
    const requestId = beginRequest('article')
    try {
      if (isAdmin.value) {
        const postParams = {
          page: listMeta.posts.page,
          size: listMeta.posts.size,
          ...(getPostParams() || {})
        }
        const reviewParams = {
          page: listMeta.reviews.page,
          size: listMeta.reviews.size,
          ...(getReviewParams() || {})
        }
        const [postRes, reviewRes] = await Promise.all([
          adminApi.getPosts(postParams),
          adminApi.getPostReviews(reviewParams)
        ])
        if (!isLatestRequest('article', requestId)) {
          return
        }

        const postPage = applyPageData(posts, listMeta.posts, postRes.data, 10)
        const reviewPage = applyPageData(postReviews, listMeta.reviews, reviewRes.data, 10)
        if (!reviewPage.summaryProvided && reviewPage.records.length > 0) {
          listMeta.reviews.summary = mergeSummary(
            listMeta.reviews.summary,
            deriveSummaryByKey(reviewPage.records, 'reviewStatus', ['pending', 'approved', 'rejected'], reviewPage.total)
          )
        }
        stats.value = {
          ...stats.value,
          posts: postPage.total,
          pendingReviews: Number((listMeta.reviews.summary || {}).pending || 0)
        }
        return
      }

      const workspaceParams = {
        page: listMeta.posts.page,
        size: listMeta.posts.size
      }
      const workspace = await adminApi.getPostWorkspace(workspaceParams)
      if (!isLatestRequest('article', requestId)) {
        return
      }
      const workspacePage = applyPageData(posts, listMeta.posts, workspace.data, 10)
      postReviews.value = []
      resetPageMeta(listMeta.reviews, 10)
      stats.value = {
        ...stats.value,
        posts: Number(workspacePage.summary?.total || workspacePage.total || 0),
        pending: Number(workspacePage.summary?.pending || 0),
        rejected: Number(workspacePage.summary?.rejected || 0),
        published: Number(workspacePage.summary?.published || 0)
      }
    } catch (error) {
      console.error(error)
    }
  }

  const fetchUsersData = async () => {
    const requestId = beginRequest('users')
    try {
      if (!isAdmin.value) {
        users.value = []
        resetPageMeta(listMeta.users, 10)
        stats.value = {
          ...stats.value,
          users: 0
        }
        return
      }

      const res = await adminApi.getUsers({
        page: listMeta.users.page,
        size: listMeta.users.size
      })
      if (!isLatestRequest('users', requestId)) {
        return
      }
      const pageData = applyPageData(users, listMeta.users, res.data, 10)
      stats.value = {
        ...stats.value,
        users: pageData.total
      }
    } catch (error) {
      console.error(error)
    }
  }

  const fetchLinkData = async () => {
    const requestId = beginRequest('links')
    try {
      if (!isAdmin.value) {
        links.value = []
        resetPageMeta(listMeta.links, 10)
        stats.value = {
          ...stats.value,
          links: 0
        }
        return
      }

      const res = await adminApi.getFriendLinks({
        page: listMeta.links.page,
        size: listMeta.links.size
      })
      if (!isLatestRequest('links', requestId)) {
        return
      }
      const pageData = applyPageData(links, listMeta.links, res.data, 10)
      stats.value = {
        ...stats.value,
        links: pageData.total
      }
    } catch (error) {
      console.error(error)
    }
  }

  const fetchContactMessageData = async () => {
    const requestId = beginRequest('contacts')
    try {
      if (!isAdmin.value) {
        contactMessages.value = []
        resetPageMeta(listMeta.contacts, 10)
        stats.value = {
          ...stats.value,
          contacts: 0,
          pendingContacts: 0
        }
        return
      }

      const res = await adminApi.getContactMessages({
        page: listMeta.contacts.page,
        size: listMeta.contacts.size
      })
      if (!isLatestRequest('contacts', requestId)) {
        return
      }
      const pageData = applyPageData(contactMessages, listMeta.contacts, res.data, 10)
      stats.value = {
        ...stats.value,
        contacts: pageData.total,
        pendingContacts: Number(pageData.summary?.pending || 0)
      }
    } catch (error) {
      console.error(error)
    }
  }

  const fetchCommentData = async () => {
    const requestId = beginRequest('comments')
    try {
      if (!isAdmin.value) {
        adminComments.value = []
        resetPageMeta(listMeta.comments, 10)
        stats.value = {
          ...stats.value,
          comments: 0,
          pendingComments: 0
        }
        return
      }

      const params = {
        page: listMeta.comments.page,
        size: listMeta.comments.size,
        ...(getCommentParams() || {})
      }
      const res = await adminApi.getComments(params)
      if (!isLatestRequest('comments', requestId)) {
        return
      }
      const pageData = applyPageData(adminComments, listMeta.comments, res.data, 10)
      if (!pageData.summaryProvided && pageData.records.length > 0) {
        listMeta.comments.summary = mergeSummary(
          listMeta.comments.summary,
          deriveSummaryByKey(pageData.records, 'status', ['pending', 'visible', 'rejected'], pageData.total)
        )
      }
      stats.value = {
        ...stats.value,
        comments: pageData.total,
        pendingComments: Number((listMeta.comments.summary || {}).pending || 0)
      }
    } catch (error) {
      console.error(error)
    }
  }

  const fetchCommentReportData = async () => {
    const requestId = beginRequest('reports')
    try {
      if (!isAdmin.value) {
        commentReports.value = []
        resetPageMeta(listMeta.reports, 10)
        return
      }

      const params = {
        page: listMeta.reports.page,
        size: listMeta.reports.size,
        ...(getReportParams() || {})
      }
      const res = await adminApi.getCommentReports(params)
      if (!isLatestRequest('reports', requestId)) {
        return
      }
      const pageData = applyPageData(commentReports, listMeta.reports, res.data, 10)
      if (!pageData.summaryProvided && pageData.records.length > 0) {
        listMeta.reports.summary = mergeSummary(
          listMeta.reports.summary,
          deriveSummaryByKey(pageData.records, 'status', ['pending', 'resolved'], pageData.total)
        )
      }
    } catch (error) {
      console.error(error)
    }
  }

  const fetchMusicData = async () => {
    const requestId = beginRequest('music')
    try {
      if (!canManageMusic.value) {
        musicList.value = []
        resetPageMeta(listMeta.music, 10)
        stats.value = {
          ...stats.value,
          music: 0
        }
        return
      }

      const res = await adminApi.getMusic({
        page: listMeta.music.page,
        size: listMeta.music.size
      })
      if (!isLatestRequest('music', requestId)) {
        return
      }
      const pageData = applyPageData(musicList, listMeta.music, res.data, 10)
      stats.value = {
        ...stats.value,
        music: pageData.total
      }
    } catch (error) {
      console.error(error)
    }
  }

  return {
    fetchOverviewData,
    fetchArticleData,
    fetchCommentData,
    fetchCommentReportData,
    fetchUsersData,
    fetchLinkData,
    fetchContactMessageData,
    fetchMusicData
  }
}
