import { reactive, watch } from 'vue'

export const useAdminMenuState = ({
  menuReady,
  isAdmin,
  activeMenu,
  navItems,
  route,
  listMeta,
  canManageMusic,
  moderationTask,
  clearModerationTaskDisplay,
  activeReviewList,
  activeCommentList,
  activeReportList,
  commentFilters,
  reportFilters,
  fetchOverviewData,
  fetchArticleData,
  fetchCommentData,
  fetchCommentReportData,
  fetchUsersData,
  fetchLinkData,
  fetchContactMessageData,
  fetchMusicData
}) => {
  const loadedMenus = reactive({})

  const ensurePostMenuDependencies = async () => {
    if (!isAdmin?.value) {
      return
    }

    await fetchUsersData()
    loadedMenus.users = true
  }

  const refreshMenuData = async (menuKey, { force = false, allowBeforeReady = false } = {}) => {
    if (!allowBeforeReady && menuReady && !menuReady.value) {
      return
    }

    if (menuKey === 'dashboard') {
      await fetchOverviewData()
      loadedMenus.dashboard = true
      return
    }

    if (!force && loadedMenus[menuKey]) {
      return
    }

    if (menuKey === 'posts' || menuKey === 'reviews') {
      await Promise.all([
        fetchArticleData(),
        ensurePostMenuDependencies()
      ])
      loadedMenus.posts = true
      loadedMenus.reviews = true
      return
    }

    if (menuKey === 'comments') {
      await fetchCommentData()
      loadedMenus.comments = true
      return
    }

    if (menuKey === 'reports') {
      await fetchCommentReportData()
      loadedMenus.reports = true
      return
    }

    if (menuKey === 'users') {
      await fetchUsersData()
      loadedMenus.users = true
      return
    }

    if (menuKey === 'links') {
      await fetchLinkData()
      loadedMenus.links = true
      return
    }

    if (menuKey === 'contacts') {
      await fetchContactMessageData()
      loadedMenus.contacts = true
      return
    }

    if (menuKey === 'music') {
      await fetchMusicData()
      loadedMenus.music = true
    }
  }

  const handleMenuClick = async (menuKey) => {
    if (menuKey === activeMenu.value) {
      await refreshMenuData(menuKey, { force: true })
      return
    }

    activeMenu.value = menuKey
  }

  const syncMenuFromRoute = () => {
    const menu = typeof route.query.menu === 'string' ? route.query.menu : 'dashboard'
    const availableMenus = navItems.value.map(item => item.key)
    activeMenu.value = availableMenus.includes(menu) ? menu : 'dashboard'
  }

  const markMenuDirty = (menuKey) => {
    loadedMenus[menuKey] = false
    if (menuKey === 'posts' || menuKey === 'reviews') {
      loadedMenus.posts = false
      loadedMenus.reviews = false
    }
  }

  const resetMenuPage = (menuKey) => {
    if (listMeta[menuKey]) {
      listMeta[menuKey].page = 1
    }
  }

  const forceRefreshMenu = async (menuKey) => {
    markMenuDirty(menuKey)
    if (menuKey !== 'dashboard') {
      loadedMenus.dashboard = false
    }
    await refreshMenuData(menuKey, { force: true })
  }

  const handleModulePageChange = async (menuKey, page) => {
    if (!listMeta[menuKey]) return
    listMeta[menuKey].page = page
    await forceRefreshMenu(menuKey)
  }

  const handleModulePageSizeChange = async (menuKey, size) => {
    if (!listMeta[menuKey]) return
    listMeta[menuKey].size = size
    listMeta[menuKey].page = 1
    await forceRefreshMenu(menuKey)
  }

  watch(activeMenu, async (menuKey) => {
    if (menuReady && !menuReady.value) {
      return
    }
    if (moderationTask.value && moderationTask.value.menuKey !== menuKey) {
      clearModerationTaskDisplay()
    }
    if (menuKey === 'music' && !canManageMusic.value) {
      activeMenu.value = 'dashboard'
      return
    }
    await refreshMenuData(menuKey)
  })

  watch(() => route.query.menu, () => {
    syncMenuFromRoute()
  })

  watch(activeReviewList, async () => {
    if (activeMenu.value !== 'reviews') return
    resetMenuPage('reviews')
    await forceRefreshMenu('reviews')
  })

  watch([() => commentFilters.value.keyword, () => commentFilters.value.postId, activeCommentList], async () => {
    if (activeMenu.value !== 'comments') return
    resetMenuPage('comments')
    await forceRefreshMenu('comments')
  })

  watch([() => reportFilters.value.keyword, activeReportList], async () => {
    if (activeMenu.value !== 'reports') return
    resetMenuPage('reports')
    await forceRefreshMenu('reports')
  })

  const initializeMenuState = async () => {
    syncMenuFromRoute()
    if (menuReady) {
      menuReady.value = true
    }
    await refreshMenuData('dashboard', { force: true, allowBeforeReady: true })
    if (activeMenu.value !== 'dashboard') {
      await refreshMenuData(activeMenu.value, { force: true, allowBeforeReady: true })
    }
  }

  return {
    handleMenuClick,
    handleModulePageChange,
    handleModulePageSizeChange,
    forceRefreshMenu,
    initializeMenuState,
    markMenuDirty,
    refreshMenuData,
    resetMenuPage,
    syncMenuFromRoute
  }
}
