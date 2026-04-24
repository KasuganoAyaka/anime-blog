<template>
  <div class="admin-layout">
    <!-- 侧边栏 -->
    <aside class="sidebar">
      <div class="sidebar-header">
        <img class="logo-icon" src="/logo.png" alt="Logo" />
        <span class="logo-text">AyakaのBlog</span>
      </div>
      <nav class="sidebar-nav">
        <div
          v-for="item in navItems"
          :key="item.key"
          :class="['nav-item', { active: activeMenu === item.key }]"
          @click="handleMenuClick(item.key)"
        >
          <span class="nav-icon">
            <svg class="nav-icon-svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.85" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
              <component :is="node.tag" v-for="(node, index) in getAdminIconNodes(item.icon)" :key="`${item.key}-${index}`" v-bind="node.attrs" />
            </svg>
          </span>
          <span class="nav-label">{{ item.label }}</span>
        </div>
      </nav>
      <div class="sidebar-footer">
        <div class="nav-item" @click="goHome">
          <span class="nav-icon">
            <svg class="nav-icon-svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.85" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
              <component :is="node.tag" v-for="(node, index) in getAdminIconNodes('home')" :key="`home-${index}`" v-bind="node.attrs" />
            </svg>
          </span>
          <span class="nav-label">返回前台</span>
        </div>
        <div class="nav-item logout" @click="handleLogout">
          <span class="nav-icon">
            <svg class="nav-icon-svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.85" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
              <component :is="node.tag" v-for="(node, index) in getAdminIconNodes('logout')" :key="`logout-${index}`" v-bind="node.attrs" />
            </svg>
          </span>
          <span class="nav-label">退出登录</span>
        </div>
      </div>
    </aside>

    <!-- 主内容 -->
    <div class="main-wrap">
      <header class="topbar">
        <h1 class="page-title">{{ currentPageTitle }}</h1>
        <div class="topbar-actions">
          <!-- 主题切换 -->
          <div class="theme-toggle" @click="themeStore.cycleTheme()">
            <span class="theme-icon">{{ themeStore.themeIcon }}</span>
            <span class="theme-label">{{ themeStore.themeLabel }}</span>
          </div>
          <div class="user-info">
            <span :class="['admin-badge', isAdmin ? 'admin' : 'user']">{{ currentUserRoleLabel }}</span>
            <span class="username">{{ userStore.user?.nickname || userStore.user?.username }}</span>
          </div>
        </div>
      </header>

      <div class="content-area">
        <!-- 控制台 -->
        <div v-if="activeMenu === 'dashboard'" class="animate-fade-in">
          <div class="stats-grid">
            <div class="stat-card" v-for="s in statCards" :key="s.label">
              <div class="stat-icon">
                <svg class="stat-icon-svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.85" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
                  <component :is="node.tag" v-for="(node, index) in getAdminIconNodes(s.icon)" :key="`${s.label}-${index}`" v-bind="node.attrs" />
                </svg>
              </div>
              <div>
                <div class="stat-value">{{ s.value }}</div>
                <div class="stat-label">{{ s.label }}</div>
              </div>
            </div>
          </div>
          <div class="welcome-card">
            <h2>👋 欢迎回来，{{ userStore.user?.nickname || userStore.user?.username }}！</h2>
            <p>使用左侧菜单管理你的博客内容。</p>
          </div>
        </div>

        <!-- 文章管理 -->
        <div v-if="activeMenu === 'posts'" class="animate-fade-in">
          <div class="toolbar">
            <div v-if="isAdmin" class="toolbar-filters">
              <span class="toolbar-filter-label">创建人</span>
              <el-select
                v-model="postFilters.creatorId"
                clearable
                placeholder="全部创建人"
                class="toolbar-select"
                @change="handlePostCreatorChange"
              >
                <el-option
                  v-for="option in postCreatorOptions"
                  :key="option.id"
                  :label="option.label"
                  :value="option.id"
                />
              </el-select>
            </div>
            <el-button type="primary" @click="openPostDialog()">{{ isAdmin ? '➕ 新增文章' : '📝 提交文章' }}</el-button>
          </div>
          <div v-if="postListLoading" class="module-loading-card">
            <div class="module-loading-spinner"></div>
            <div class="module-loading-title">正在加载文章列表...</div>
            <div class="module-loading-desc">刚保存的文章会在列表刷新完成后显示。</div>
          </div>
          <div v-else-if="posts.length === 0" class="post-empty-card">
            <div class="post-empty-title">{{ isAdmin ? '暂无文章' : '暂无投稿记录' }}</div>
            <div class="post-empty-desc">新布局已经准备好，创建一篇文章后这里会以卡片流形式展示。</div>
          </div>
          <div v-else class="post-card-grid">
            <article
              v-for="row in posts"
              :key="row.id || row.reviewId || row.postId"
              :class="['post-manage-card', { published: shouldHidePublishedActions(row) }]"
            >
              <div class="post-manage-main">
                <div class="post-card-topline">
                  <span class="post-card-category">{{ row.category || '未分类' }}</span>
                  <span class="post-card-time">{{ formatDate(getPostDisplayTime(row)) }}</span>
                </div>

                <h3 class="post-card-title">{{ row.title }}</h3>

                <p class="post-card-summary">{{ row.excerpt || row.summary || '暂未填写摘要，建议补充一句简介，列表展示会更清晰。' }}</p>

                <div class="post-card-statuses">
                  <span :class="['badge', getPostStatusBadgeClass(row)]">
                    {{ getPostDisplayStatusLabel(row) }}
                  </span>
                  <span v-if="!isAdmin" :class="['badge', getReviewStatusBadgeClass(row)]">
                    {{ getReviewStatusLabel(row) }}
                  </span>
                </div>

                <div v-if="!isAdmin && row.reviewNote" class="post-card-note">
                  <span class="post-card-note-label">审核备注</span>
                  <span class="post-card-note-text">{{ row.reviewNote }}</span>
                </div>

                <div class="post-card-actions">
                  <button class="btn-edit" @click="handleEditPost(row)">编辑</button>
                  <button
                    v-if="canPublishPost(row)"
                    class="btn-approve"
                    @click="publishPost(row)"
                  >{{ getPublishActionLabel(row) }}</button>
                  <button
                    v-if="canOfflinePost(row)"
                    class="btn-reject"
                    @click="offlinePost(row)"
                  >下线</button>
                  <button
                    v-if="isAdmin || canDeleteOwnPost(row)"
                    class="btn-del"
                    @click="deletePost(isAdmin ? row.id : row.postId)"
                  >删除</button>
                  <button
                    v-else-if="canWithdrawReview(row)"
                    class="btn-del"
                    @click="deletePostDraft(row)"
                  >撤回</button>
                </div>
              </div>

              <div class="post-manage-cover" :class="{ empty: !row.coverImage }">
                <img v-if="row.coverImage" :src="row.coverImage" alt="封面图" />
                <div v-else class="post-manage-cover-placeholder">
                  <span>{{ row.title || '未设置封面' }}</span>
                </div>
              </div>
            </article>
          </div>
          <AdminListPagination
            :current-page="listMeta.posts.page"
            :page-size="listMeta.posts.size"
            :total="listMeta.posts.total"
            @update:current-page="handleModulePageChange('posts', $event)"
            @update:page-size="handleModulePageSizeChange('posts', $event)"
          />
        </div>

        <div v-if="activeMenu === 'reviews'" class="animate-fade-in">
          <div class="toolbar review-toolbar">
            <div class="review-toolbar-copy">
              <div class="review-toolbar-title">文章审核</div>
              <div class="review-toolbar-desc">审核普通用户提交的新文章和文章修改申请</div>
            </div>
            <div class="review-toolbar-stats">
              <button
                v-for="option in reviewListOptions"
                :key="option.key"
                type="button"
                :class="['review-filter-chip', 'badge', option.badgeClass, { active: activeReviewList === option.key }]"
                @click="activeReviewList = option.key"
              >
                {{ option.label }} {{ option.count }}
              </button>
            </div>
          </div>
          <div v-if="selectedReviewIds.length" class="moderation-bulk-bar">
            <div class="moderation-bulk-copy">已选中 {{ selectedReviewIds.length }} 条文章审核记录</div>
            <div class="moderation-bulk-actions">
              <button v-if="activeReviewList === 'pending'" class="btn-approve" :disabled="isModerationBusy('reviews')" @click="submitReviewBatch('approve')">批量通过</button>
              <button v-if="activeReviewList === 'pending'" class="btn-reject" :disabled="isModerationBusy('reviews')" @click="submitReviewBatch('reject')">批量驳回</button>
              <button class="btn-del" :disabled="isModerationBusy('reviews')" @click="submitReviewBatch('delete')">批量删除</button>
            </div>
          </div>
          <div v-if="moderationTask?.menuKey === 'reviews'" class="moderation-task-card">
            <div class="moderation-task-head">
              <strong>{{ moderationTaskTitle }}</strong>
              <span :class="['badge', moderationTaskBadgeClass]">{{ moderationTaskStatusLabel }}</span>
            </div>
            <div class="moderation-task-desc">{{ moderationTask.message }}</div>
            <div class="moderation-task-progress">
              <div class="moderation-task-progress-bar" :style="{ width: `${moderationTaskProgress}%` }" />
            </div>
            <div class="moderation-task-meta">
              <span>已处理 {{ moderationTask.processedCount || 0 }} / {{ moderationTask.totalCount || 0 }}</span>
              <span>成功 {{ moderationTask.successCount || 0 }}</span>
              <span v-if="moderationTask.failureCount">失败 {{ moderationTask.failureCount }}</span>
            </div>
          </div>
          <div v-if="reviewListLoading" class="module-loading-card table-loading-card">
            <div class="module-loading-spinner"></div>
            <div class="module-loading-title">正在加载审核列表...</div>
          </div>
          <Transition v-else name="review-switch" mode="out-in">
            <div :key="activeReviewList" class="table-container review-table-stage">
              <table class="data-table">
                <thead>
                  <tr>
                    <th class="col-select">
                      <el-checkbox
                        :model-value="areAllVisibleReviewsSelected"
                        :indeterminate="isReviewSelectionIndeterminate"
                        @change="toggleSelectAllReviews"
                      />
                    </th>
                    <th class="col-title">标题</th>
                    <th class="col-name">作者</th>
                    <th class="col-status">类型</th>
                    <th class="col-status">文章状态</th>
                    <th class="col-status">审核状态</th>
                    <th class="col-time">提交时间</th>
                    <th class="col-desc">审核备注</th>
                    <th class="col-actions">操作</th>
                  </tr>
                </thead>
                <tbody>
                  <tr v-if="filteredPostReviews.length === 0" class="review-empty-row">
                    <td colspan="9" class="empty-row">暂无{{ currentReviewListLabel }}记录</td>
                  </tr>
                  <tr v-if="postReviews.length === 0"><td colspan="9" class="empty-row">暂无审核记录</td></tr>
                  <tr v-for="row in filteredPostReviews" :key="row.id">
                    <td class="col-select">
                      <el-checkbox :model-value="selectedReviewIds.includes(String(row.id))" @change="toggleReviewSelection(row.id, $event)" />
                    </td>
                    <td class="col-title">{{ row.title }}</td>
                    <td class="col-name">{{ row.nickname || row.username || '-' }}</td>
                    <td class="col-status">
                      <span :class="['badge', row.action === 'create' ? 'badge-info' : 'badge-default']">
                        {{ row.action === 'create' ? '新文章' : '修改文章' }}
                      </span>
                    </td>
                    <td class="col-status">
                      <span :class="['badge', row.postStatus === 'published' ? 'badge-success' : 'badge-default']">
                        {{ row.postStatus === 'published' ? '已发布' : '未发布' }}
                      </span>
                    </td>
                    <td class="col-status">
                      <span :class="['badge', getReviewStatusBadgeClass(row.reviewStatus)]">
                        {{ getReviewStatusLabel(row.reviewStatus) }}
                      </span>
                    </td>
                    <td class="col-time">{{ formatDate(row.updateTime || row.createTime) }}</td>
                    <td class="col-desc">{{ row.reviewNote || '-' }}</td>
                    <td class="col-actions">
                      <button class="btn-view" @click="openReviewDialog(row)">查看</button>
                      <button v-if="row.reviewStatus === 'pending'" class="btn-approve" @click="approvePostReview(row.id)">通过</button>
                      <button v-if="row.reviewStatus === 'pending'" class="btn-reject" @click="rejectPostReview(row)">驳回</button>
                    </td>
                  </tr>
                </tbody>
              </table>
            </div>
          </Transition>
          <AdminListPagination
            :current-page="listMeta.reviews.page"
            :page-size="listMeta.reviews.size"
            :total="listMeta.reviews.total"
            @update:current-page="handleModulePageChange('reviews', $event)"
            @update:page-size="handleModulePageSizeChange('reviews', $event)"
          />
        </div>

        <div v-if="activeMenu === 'comments'" class="animate-fade-in">
          <div class="toolbar review-toolbar admin-comment-toolbar">
            <div class="review-toolbar-copy">
              <div class="review-toolbar-title">评论审核</div>
              <div class="review-toolbar-desc">查看评论、回复上下文，审核匿名内容，并管理删除</div>
            </div>
            <div class="comment-toolbar-filters">
              <el-input
                v-model="commentFilters.keyword"
                clearable
                placeholder="搜索作者 / 邮箱 / 内容"
                class="toolbar-search"
              />
              <el-select
                v-model="commentFilters.postId"
                clearable
                placeholder="全部文章"
                class="toolbar-select"
              >
                <el-option
                  v-for="option in commentPostOptions"
                  :key="option.value"
                  :label="option.label"
                  :value="option.value"
                />
              </el-select>
            </div>
            <div class="review-toolbar-stats">
              <button
                v-for="option in commentListOptions"
                :key="option.key"
                type="button"
                :class="['review-filter-chip', 'badge', option.badgeClass, { active: activeCommentList === option.key }]"
                @click="activeCommentList = option.key"
              >
                {{ option.label }} {{ option.count }}
              </button>
            </div>
          </div>
          <div v-if="selectedCommentIds.length" class="moderation-bulk-bar">
            <div class="moderation-bulk-copy">已选中 {{ selectedCommentIds.length }} 条评论记录</div>
            <div class="moderation-bulk-actions">
              <button v-if="activeCommentList === 'pending'" class="btn-approve" :disabled="isModerationBusy('comments')" @click="submitCommentBatch('approve')">批量通过</button>
              <button v-if="activeCommentList === 'pending'" class="btn-reject" :disabled="isModerationBusy('comments')" @click="submitCommentBatch('reject')">批量驳回</button>
              <button class="btn-del" :disabled="isModerationBusy('comments')" @click="submitCommentBatch('delete')">批量删除</button>
            </div>
          </div>
          <div v-if="moderationTask?.menuKey === 'comments'" class="moderation-task-card">
            <div class="moderation-task-head">
              <strong>{{ moderationTaskTitle }}</strong>
              <span :class="['badge', moderationTaskBadgeClass]">{{ moderationTaskStatusLabel }}</span>
            </div>
            <div class="moderation-task-desc">{{ moderationTask.message }}</div>
            <div class="moderation-task-progress">
              <div class="moderation-task-progress-bar" :style="{ width: `${moderationTaskProgress}%` }" />
            </div>
            <div class="moderation-task-meta">
              <span>已处理 {{ moderationTask.processedCount || 0 }} / {{ moderationTask.totalCount || 0 }}</span>
              <span>成功 {{ moderationTask.successCount || 0 }}</span>
              <span v-if="moderationTask.failureCount">失败 {{ moderationTask.failureCount }}</span>
            </div>
          </div>
          <Transition name="review-switch" mode="out-in">
            <div :key="activeCommentList" class="table-container review-table-stage">
              <table class="data-table">
                <thead>
                  <tr>
                    <th class="col-select">
                      <el-checkbox
                        :model-value="areAllVisibleCommentsSelected"
                        :indeterminate="isCommentSelectionIndeterminate"
                        @change="toggleSelectAllComments"
                      />
                    </th>
                    <th class="col-title">文章</th>
                    <th class="col-name">作者</th>
                    <th class="col-status">类型</th>
                    <th class="col-status">状态</th>
                    <th class="col-desc">内容</th>
                    <th class="col-time">提交时间</th>
                    <th class="col-actions">操作</th>
                  </tr>
                </thead>
                <tbody>
                  <tr v-if="filteredAdminComments.length === 0" class="review-empty-row">
                    <td colspan="8" class="empty-row">暂无{{ currentCommentListLabel }}记录</td>
                  </tr>
                  <tr v-if="adminComments.length === 0"><td colspan="8" class="empty-row">暂无评论记录</td></tr>
                  <tr v-for="row in filteredAdminComments" :key="row.id">
                    <td class="col-select">
                      <el-checkbox :model-value="selectedCommentIds.includes(String(row.id))" @change="toggleCommentSelection(row.id, $event)" />
                    </td>
                    <td class="col-title">{{ row.postTitle || '-' }}</td>
                    <td class="col-name">{{ row.nickname || row.username || row.authorName || '-' }}</td>
                    <td class="col-status">
                      <span :class="['badge', row.parentId ? 'badge-info' : 'badge-default']">
                        {{ row.parentId ? '回复' : '评论' }}
                      </span>
                    </td>
                    <td class="col-status">
                      <span :class="['badge', row.status === 'visible' ? 'badge-success' : (row.status === 'rejected' ? 'badge-danger' : 'badge-warning')]">
                        {{ row.status === 'visible' ? '已通过' : (row.status === 'rejected' ? '已驳回' : '待审核') }}
                      </span>
                    </td>
                    <td class="col-desc">{{ (row.content || '').slice(0, 40) || '-' }}</td>
                    <td class="col-time">{{ formatDateTime(row.createdAt) }}</td>
                    <td class="col-actions">
                      <button class="btn-view" @click="openCommentDialog(row)">查看</button>
                      <button v-if="row.status === 'pending'" class="btn-approve" @click="approveComment(row.id)">通过</button>
                      <button v-if="row.status === 'pending'" class="btn-reject" @click="rejectComment(row)">驳回</button>
                      <button class="btn-del" @click="deleteComment(row.id)">删除</button>
                    </td>
                  </tr>
                </tbody>
              </table>
            </div>
          </Transition>
          <AdminListPagination
            :current-page="listMeta.comments.page"
            :page-size="listMeta.comments.size"
            :total="listMeta.comments.total"
            @update:current-page="handleModulePageChange('comments', $event)"
            @update:page-size="handleModulePageSizeChange('comments', $event)"
          />
        </div>

        <div v-if="activeMenu === 'reports'" class="animate-fade-in">
          <div class="toolbar review-toolbar admin-comment-toolbar">
            <div class="review-toolbar-copy">
              <div class="review-toolbar-title">举报管理</div>
              <div class="review-toolbar-desc">查看评论举报、核对举报说明，并决定保留或删除被举报评论</div>
            </div>
            <div class="comment-toolbar-filters">
              <el-input
                v-model="reportFilters.keyword"
                clearable
                placeholder="搜索文章 / 举报原因 / 评论内容 / 举报人"
                class="toolbar-search"
              />
            </div>
            <div class="review-toolbar-stats">
              <button
                v-for="option in reportListOptions"
                :key="option.key"
                type="button"
                :class="['review-filter-chip', 'badge', option.badgeClass, { active: activeReportList === option.key }]"
                @click="activeReportList = option.key"
              >
                {{ option.label }} {{ option.count }}
              </button>
            </div>
          </div>
          <div class="table-container review-table-stage">
            <table class="data-table">
              <thead>
                <tr>
                  <th class="col-title">文章</th>
                  <th class="col-name">举报原因</th>
                  <th class="col-desc">举报说明</th>
                  <th class="col-status">评论状态</th>
                  <th class="col-name">举报人</th>
                  <th class="col-time">提交时间</th>
                  <th class="col-actions">操作</th>
                </tr>
              </thead>
              <tbody>
                <tr v-if="filteredCommentReports.length === 0" class="review-empty-row">
                  <td colspan="7" class="empty-row">暂无{{ currentReportListLabel }}举报记录</td>
                </tr>
                <tr v-if="commentReports.length === 0">
                  <td colspan="7" class="empty-row">暂无举报记录</td>
                </tr>
                <tr v-for="row in filteredCommentReports" :key="row.id">
                  <td class="col-title">{{ row.postTitle || '-' }}</td>
                  <td class="col-name">{{ getReportReasonText(row) }}</td>
                  <td class="col-desc">{{ (row.description || '').slice(0, 52) || '-' }}</td>
                  <td class="col-status">
                    <span :class="['badge', getReportStatusBadgeClass(row)]">
                      {{ getReportStatusLabel(row) }}
                    </span>
                  </td>
                  <td class="col-name">{{ row.reporterName || '访客' }}</td>
                  <td class="col-time">{{ formatDateTime(row.createdAt) }}</td>
                  <td class="col-actions">
                    <button class="btn-view" @click="openReportDialog(row)">查看</button>
                    <button
                      v-if="row.status === 'pending' && !row.commentDeleted"
                      class="btn-approve"
                      @click="resolveCommentReport(row, 'kept')"
                    >保留评论</button>
                    <button
                      v-if="row.status === 'pending'"
                      class="btn-del"
                      @click="resolveCommentReport(row, 'deleted')"
                    >删除评论</button>
                  </td>
                </tr>
              </tbody>
            </table>
          </div>
          <AdminListPagination
            :current-page="listMeta.reports.page"
            :page-size="listMeta.reports.size"
            :total="listMeta.reports.total"
            @update:current-page="handleModulePageChange('reports', $event)"
            @update:page-size="handleModulePageSizeChange('reports', $event)"
          />
        </div>

        <!-- 音乐管理 -->
        <div v-if="activeMenu === 'music' && canManageMusic" class="animate-fade-in">
          <div class="toolbar">
            <el-button type="primary" @click="openMusicDialog()">➕ 新增音乐</el-button>
          </div>
          <div class="table-container">
            <table class="data-table">
              <thead>
                <tr>
                  <th class="col-id">序号</th>
                  <th class="col-title">标题</th>
                  <th class="col-artist">艺术家</th>
                  <th class="col-album">专辑</th>
                  <th>时长</th>
                  <th class="col-actions">操作</th>
                </tr>
              </thead>
              <tbody>
                <tr v-if="musicList.length === 0"><td colspan="6" class="empty-row">暂无音乐</td></tr>
                <tr v-for="(row, index) in musicList" :key="row.id">
                  <td class="col-id">{{ getPageRowIndex('music', index) }}</td>
                  <td class="col-title">{{ row.title }}</td>
                  <td class="col-artist">{{ row.artist || '-' }}</td>
                  <td class="col-album">{{ row.album || '-' }}</td>
                  <td>{{ formatMusicDuration(row.duration) }}</td>
                  <td class="col-actions">
                    <button class="btn-edit" @click="openMusicDialog(row)">编辑</button>
                    <button class="btn-del" @click="deleteMusic(row.id)">删除</button>
                  </td>
                </tr>
              </tbody>
            </table>
          </div>
          <AdminListPagination
            :current-page="listMeta.music.page"
            :page-size="listMeta.music.size"
            :total="listMeta.music.total"
            @update:current-page="handleModulePageChange('music', $event)"
            @update:page-size="handleModulePageSizeChange('music', $event)"
          />
        </div>

        <!-- 用户管理 -->
        <div v-if="activeMenu === 'users'" class="animate-fade-in">
          <div class="toolbar">
            <el-button type="primary" @click="openUserDialog()">➕ 添加用户</el-button>
          </div>
          <div class="table-container">
            <table class="data-table">
              <thead>
                <tr>
                  <th class="col-id">序号</th>
                  <th class="col-username">用户名</th>
                  <th class="col-nickname">昵称</th>
                  <th class="col-email">邮箱</th>
                  <th class="col-role">角色</th>
                  <th class="col-actions">操作</th>
                </tr>
              </thead>
              <tbody>
                <tr v-if="users.length === 0"><td colspan="6" class="empty-row">暂无用户</td></tr>
                <tr v-for="(row, index) in users" :key="row.id">
                  <td class="col-id">{{ getPageRowIndex('users', index) }}</td>
                  <td class="col-username">{{ row.username }}</td>
                  <td class="col-nickname">{{ row.nickname || '-' }}</td>
                  <td class="col-email">{{ row.email }}</td>
                  <td class="col-role">
                    <span :class="['badge', getUserRoleBadgeClass(row)]">
                      {{ getUserRoleLabel(row) }}
                    </span>
                  </td>
                  <td class="col-actions">
                    <button class="btn-edit" @click="openUserDialog(row)" :disabled="!canEditUserAccount(row)">编辑</button>
                    <button class="btn-del" @click="deleteUser(row.id)" :disabled="!canDeleteUserAccount(row)">删除</button>
                  </td>
                </tr>
              </tbody>
            </table>
          </div>
          <AdminListPagination
            :current-page="listMeta.users.page"
            :page-size="listMeta.users.size"
            :total="listMeta.users.total"
            @update:current-page="handleModulePageChange('users', $event)"
            @update:page-size="handleModulePageSizeChange('users', $event)"
          />
        </div>

        <!-- 友链管理 -->
        <div v-if="activeMenu === 'links'" class="animate-fade-in">
          <div class="toolbar">
            <el-button type="primary" @click="openLinkDialog()">➕ 添加友链</el-button>
          </div>
          <div class="table-container">
            <table class="data-table">
              <thead>
                <tr>
                  <th class="col-id">序号</th>
                  <th class="col-name">名称</th>
                  <th class="col-url">链接</th>
                  <th class="col-desc">描述</th>
                  <th class="col-status">状态</th>
                  <th class="col-actions">操作</th>
                </tr>
              </thead>
              <tbody>
                <tr v-if="links.length === 0"><td colspan="6" class="empty-row">暂无友链</td></tr>
                <tr v-for="(row, index) in links" :key="row.id">
                  <td class="col-id">{{ getPageRowIndex('links', index) }}</td>
                  <td class="col-name">{{ row.name }}</td>
                  <td class="col-url"><a :href="row.url" target="_blank" class="link-url">{{ row.url }}</a></td>
                  <td class="col-desc">{{ row.description || '-' }}</td>
                  <td class="col-status">
                    <span :class="['badge', row.status === 1 ? 'badge-success' : 'badge-warning']">
                      {{ row.status === 1 ? '已审核' : '待审核' }}
                    </span>
                  </td>
                  <td class="col-actions">
                    <button v-if="row.status !== 1" class="btn-approve" @click="approveLink(row.id)">审核</button>
                    <button class="btn-edit" @click="openLinkDialog(row)">编辑</button>
                    <button class="btn-del" @click="deleteLink(row.id)">删除</button>
                  </td>
                </tr>
              </tbody>
            </table>
          </div>
          <AdminListPagination
            :current-page="listMeta.links.page"
            :page-size="listMeta.links.size"
            :total="listMeta.links.total"
            @update:current-page="handleModulePageChange('links', $event)"
            @update:page-size="handleModulePageSizeChange('links', $event)"
          />
        </div>

        <!-- 个人信息管理 -->
        <div v-if="activeMenu === 'contacts'" class="animate-fade-in">
          <div class="toolbar review-toolbar">
            <div class="review-toolbar-copy">
              <div class="review-toolbar-title">联系消息</div>
              <div class="review-toolbar-desc">管理访客通过联系我们表单提交的留言</div>
            </div>
            <div class="review-toolbar-stats">
              <span class="badge badge-warning">待处理 {{ pendingContactCount }}</span>
            </div>
          </div>
          <div class="table-container">
            <table class="data-table">
              <thead>
                <tr>
                  <th class="col-id">序号</th>
                  <th class="col-name">姓名</th>
                  <th class="col-email">邮箱</th>
                  <th class="col-subject">主题</th>
                  <th class="col-message">留言内容</th>
                  <th class="col-status">状态</th>
                  <th class="col-time">提交时间</th>
                  <th class="col-actions">操作</th>
                </tr>
              </thead>
              <tbody>
                <tr v-if="contactMessages.length === 0"><td colspan="8" class="empty-row">暂无联系消息</td></tr>
                <tr v-for="(row, index) in contactMessages" :key="row.id">
                  <td class="col-id">{{ getPageRowIndex('contacts', index) }}</td>
                  <td class="col-name">{{ row.name }}</td>
                  <td class="col-email">
                    <a :href="`mailto:${row.email}`" class="link-url">{{ row.email }}</a>
                  </td>
                  <td class="col-subject">{{ row.subject || '-' }}</td>
                  <td class="col-message">
                    <div class="message-cell">{{ row.message || '-' }}</div>
                  </td>
                  <td class="col-status">
                    <span :class="['badge', getContactStatusBadgeClass(row.status)]">
                      {{ getContactStatusLabel(row.status) }}
                    </span>
                  </td>
                  <td class="col-time">{{ formatDateTime(row.createTime) }}</td>
                  <td class="col-actions">
                    <button class="btn-view" @click="openContactDialog(row)">查看</button>
                    <button class="btn-del" @click="deleteContactMessage(row.id)">删除</button>
                  </td>
                </tr>
              </tbody>
            </table>
          </div>
          <AdminListPagination
            :current-page="listMeta.contacts.page"
            :page-size="listMeta.contacts.size"
            :total="listMeta.contacts.total"
            @update:current-page="handleModulePageChange('contacts', $event)"
            @update:page-size="handleModulePageSizeChange('contacts', $event)"
          />
        </div>

        <div v-if="activeMenu === 'profile'" class="animate-fade-in">
          <div class="profile-container">
            <!-- 头像区域 -->
            <div class="profile-avatar-section">
              <div class="avatar-upload" @click="showAvatarDialog = true">
                <img v-if="profileForm.avatar" :src="profileForm.avatar" class="avatar-img" />
                <div v-else class="avatar-placeholder">
                  <svg class="avatar-placeholder-icon" viewBox="0 0 24 24" fill="none" aria-hidden="true">
                    <component :is="node.tag" v-for="(node, index) in avatarPlaceholderNodes" :key="`profile-avatar-${index}`" v-bind="node.attrs" />
                  </svg>
                  <span class="placeholder-text">点击上传头像</span>
                </div>
                <div class="avatar-hover-mask">
                  <span>更换头像</span>
                </div>
              </div>
              <div class="profile-basic-info">
                <div class="profile-username">@{{ userStore.user?.username }}</div>
                <div class="profile-role">
                  <span :class="['role-badge', userStore.user?.role]">
                    {{ currentUserRoleLabel }}
                  </span>
                </div>
                <div class="profile-join-time">
                  加入时间：{{ formatDate(userStore.user?.createTime) }}
                </div>
              </div>
            </div>

            <!-- 信息表单 -->
            <div class="profile-forms">
              <!-- 基本信息 -->
              <div class="form-section">
                <h3 class="section-title">📝 基本信息</h3>
                <div class="form-grid">
                  <div class="form-item">
                    <label>昵称</label>
                    <input v-model="profileForm.nickname" class="form-input" placeholder="输入昵称" />
                  </div>
                  <div class="form-item">
                    <label>个人简介</label>
                    <input v-model="profileForm.bio" class="form-input" placeholder="介绍一下自己..." />
                  </div>
                </div>
                <div class="form-actions">
                  <el-button type="primary" @click="saveProfile" :loading="savingProfile">保存基本信息</el-button>
                </div>
              </div>

              <!-- 邮箱管理 -->
              <div class="form-section">
                <h3 class="section-title">📧 邮箱管理</h3>
                <div class="email-current">
                  <span class="label">当前邮箱：</span>
                  <span class="value">{{ userStore.user?.email || '未设置' }}</span>
                  <span v-if="userStore.user?.emailVerified" class="verified-badge">✓ 已验证</span>
                  <span v-else class="unverified-badge">未验证</span>
                </div>
                <div class="email-change-form">
                  <div class="form-grid">
                    <div class="form-item">
                      <label>新邮箱</label>
                      <input v-model="emailForm.newEmail" type="email" class="form-input" placeholder="输入新邮箱地址" />
                    </div>
                    <div class="form-item">
                      <label>验证码</label>
                      <div class="code-input-group">
                        <input v-model="emailForm.code" class="form-input code-input" placeholder="6位验证码" maxlength="6" />
                        <el-button 
                          class="email-code-btn"
                          :disabled="emailCooldown > 0 || !emailForm.newEmail" 
                          @click="sendEmailCode"
                          :loading="sendingCode"
                        >
                          {{ emailCooldown > 0 ? `${emailCooldown}s` : '获取验证码' }}
                        </el-button>
                      </div>
                    </div>
                  </div>
                  <div class="form-actions">
                    <el-button type="primary" @click="changeEmail" :loading="savingEmail">更换邮箱</el-button>
                  </div>
                </div>
              </div>

              <!-- 修改密码 -->
              <div class="form-section">
                <h3 class="section-title">🔐 修改密码</h3>
                <div class="form-grid">
                  <div class="form-item full-width">
                    <label>当前密码</label>
                    <input v-model="passwordForm.oldPassword" type="password" class="form-input" placeholder="输入当前密码" />
                  </div>
                  <div class="form-item">
                    <label>新密码</label>
                    <input v-model="passwordForm.newPassword" type="password" class="form-input" placeholder="输入新密码" />
                    <div class="password-rules-inline">
                      <div
                        v-for="item in passwordRuleItems"
                        :key="item.key"
                        :class="['password-rule-inline', { met: item.met }]"
                      >
                        {{ item.met ? '√' : '·' }} {{ item.label }}
                      </div>
                    </div>
                  </div>
                  <div class="form-item">
                    <label>确认密码</label>
                    <input v-model="passwordForm.confirmPassword" type="password" class="form-input" placeholder="再次输入新密码" />
                  </div>
                </div>
                <div class="form-actions">
                  <el-button type="primary" @click="changePassword" :loading="savingPassword">修改密码</el-button>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 文章对话框 -->
    <el-dialog
      v-model="showPostDialog"
      :title="editingId.post ? '编辑文章' : '新增文章'"
      width="95%"
      class="post-dialog fullscreen-dialog"
      :show-close="false"
      destroy-on-close
    >
      <div class="post-dialog-header">
        <div class="header-left">
          <el-button @click="showPostDialog = false" size="large">取消</el-button>
        </div>
        <div class="header-center">
          <span class="dialog-title">{{ isAdmin ? (editingId.post ? '编辑文章' : '新增文章') : (editingId.post || editingId.review ? '编辑投稿' : '提交文章') }}</span>
        </div>
        <div class="header-right">
          <el-button type="primary" @click="savePost" :loading="saving" size="large">保存</el-button>
        </div>
      </div>

      <div class="post-dialog-body">
        <div class="post-form-main">
          <el-form ref="postFormRef" :model="postForm" :rules="postRules" label-position="top">
            <el-form-item label="标题" prop="title">
              <el-input v-model="postForm.title" placeholder="文章标题" size="large" class="title-input" />
            </el-form-item>

            <el-form-item label="分类" prop="category">
              <el-select v-model="postForm.category" placeholder="选择分类" size="large" class="post-category-select" style="width: 200px;">
                <el-option v-for="c in categories" :key="c" :label="c" :value="c" />
              </el-select>
            </el-form-item>

            <el-form-item label="标签" prop="tags">
              <el-input v-model="postForm.tags" placeholder="逗号分隔" size="large" />
            </el-form-item>

            <el-form-item label="摘要">
              <el-input v-model="postForm.excerpt" type="textarea" rows="3" placeholder="文章摘要" />
            </el-form-item>

          </el-form>
        </div>

        <div class="post-editor-section">
          <div class="editor-label">文章内容 (Markdown)</div>
          <MarkdownEditor v-model="postForm.content" />
        </div>
      </div>
    </el-dialog>

    <el-dialog v-model="showReviewDialog" title="文章审核详情" width="900px" class="admin-dialog review-detail-dialog">
      <div v-if="selectedReview" class="review-detail">
        <div class="review-meta-grid">
          <div class="review-meta-card">
            <div class="review-meta-label">投稿作者</div>
            <div class="review-meta-value">{{ selectedReview.nickname || selectedReview.username || '-' }}</div>
          </div>
          <div class="review-meta-card">
            <div class="review-meta-label">审核类型</div>
            <div class="review-meta-value">{{ selectedReview.action === 'create' ? '新文章' : '修改文章' }}</div>
          </div>
          <div class="review-meta-card">
            <div class="review-meta-label">投稿状态</div>
            <div class="review-meta-value">{{ selectedReview.postStatus === 'published' ? '已发布' : '未发布' }}</div>
          </div>
          <div class="review-meta-card">
            <div class="review-meta-label">审核状态</div>
            <div class="review-meta-value">{{ getReviewStatusLabel(selectedReview.reviewStatus) }}</div>
          </div>
        </div>

        <div class="review-section">
          <div class="review-section-title">标题</div>
          <div class="review-section-content">{{ selectedReview.title || '-' }}</div>
        </div>

        <div class="review-inline-grid">
          <div class="review-section">
            <div class="review-section-title">分类</div>
            <div class="review-section-content">{{ selectedReview.category || '-' }}</div>
          </div>
          <div class="review-section">
            <div class="review-section-title">标签</div>
            <div class="review-section-content">{{ selectedReview.tags || '-' }}</div>
          </div>
        </div>

        <div class="review-section">
          <div class="review-section-title">摘要</div>
          <div class="review-section-content">{{ selectedReview.excerpt || selectedReview.summary || '暂无摘要' }}</div>
        </div>

        <div class="review-section">
          <div class="review-section-title">审核备注</div>
          <div class="review-section-content">{{ selectedReview.reviewNote || '暂无备注' }}</div>
        </div>

        <div class="review-section">
          <div class="review-section-title">正文预览</div>
          <div class="review-preview">
            <MarkdownPreview :source="selectedReview.content || '暂无正文内容'" preview-id="admin-review-preview" />
          </div>
        </div>
      </div>
      <template #footer>
        <el-button @click="showReviewDialog = false">关闭</el-button>
        <el-button v-if="selectedReview?.reviewStatus === 'pending'" type="danger" plain @click="rejectPostReview(selectedReview)">驳回</el-button>
        <el-button v-if="selectedReview?.reviewStatus === 'pending'" type="primary" @click="approvePostReview(selectedReview.id)">审核通过</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="showCommentDialog" title="评论详情" width="820px" class="admin-dialog review-detail-dialog">
      <div v-if="selectedComment" class="review-detail">
        <div class="review-meta-grid">
          <div class="review-meta-card">
            <div class="review-meta-label">所属文章</div>
            <div class="review-meta-value">{{ selectedComment.postTitle || '-' }}</div>
          </div>
          <div class="review-meta-card">
            <div class="review-meta-label">评论类型</div>
            <div class="review-meta-value">{{ selectedComment.parentId ? '回复' : '评论' }}</div>
          </div>
          <div class="review-meta-card">
            <div class="review-meta-label">发布身份</div>
            <div class="review-meta-value">{{ selectedComment.nickname || selectedComment.username || selectedComment.authorName || '-' }}</div>
          </div>
          <div class="review-meta-card">
            <div class="review-meta-label">审核状态</div>
            <div class="review-meta-value">{{ getCommentStatusLabel(selectedComment.status) }}</div>
          </div>
        </div>

        <div class="review-inline-grid">
          <div class="review-section">
            <div class="review-section-title">昵称</div>
            <div class="review-section-content">{{ selectedComment.authorName || '-' }}</div>
          </div>
          <div class="review-section">
            <div class="review-section-title">邮箱</div>
            <div class="review-section-content">{{ selectedComment.authorEmail || '-' }}</div>
          </div>
        </div>

        <div class="review-section">
          <div class="review-section-title">网站</div>
          <div class="review-section-content">
            <a v-if="selectedComment.authorWebsite" :href="selectedComment.authorWebsite" target="_blank" rel="noreferrer" class="link-url">
              {{ selectedComment.authorWebsite }}
            </a>
            <span v-else>-</span>
          </div>
        </div>

        <div class="review-section">
          <div class="review-section-title">地区 / 系统 / 浏览器</div>
          <div class="comment-admin-tags">
            <span
              v-for="tag in getAdminCommentClientTags(selectedComment)"
              :key="`comment-client-${tag}`"
              class="comment-admin-tag"
            >
              {{ tag }}
            </span>
            <span v-if="!getAdminCommentClientTags(selectedComment).length">-</span>
          </div>
        </div>

        <div v-if="selectedComment.parentId" class="review-section">
          <div class="review-section-title">回复上下文</div>
          <div class="review-section-content review-message-content">
            <div>回复对象：{{ selectedComment.parentAuthorName || '-' }}</div>
            <div>上级状态：{{ getCommentStatusLabel(selectedComment.parentStatus) }}</div>
            <div class="comment-context-body">{{ selectedComment.parentContent || '暂无上级内容' }}</div>
          </div>
        </div>

        <div class="review-section">
          <div class="review-section-title">评论内容</div>
          <div class="review-section-content review-message-content">{{ selectedComment.content || '-' }}</div>
        </div>

        <div v-if="selectedComment.images?.length" class="review-section">
          <div class="review-section-title">附带图片</div>
          <div class="comment-review-images">
            <a
              v-for="(image, index) in selectedComment.images"
              :key="`${selectedComment.id}-${index}`"
              :href="image"
              target="_blank"
              rel="noreferrer"
              class="comment-review-image"
            >
              <img :src="image" alt="comment attachment" />
            </a>
          </div>
        </div>

        <div class="review-inline-grid">
          <div class="review-section">
            <div class="review-section-title">提交时间</div>
            <div class="review-section-content">{{ formatDateTime(selectedComment.createdAt) }}</div>
          </div>
          <div class="review-section">
            <div class="review-section-title">审核记录</div>
            <div class="review-section-content">
              {{ selectedComment.reviewerName || '暂无' }}
              <span v-if="selectedComment.reviewedAt"> · {{ formatDateTime(selectedComment.reviewedAt) }}</span>
            </div>
          </div>
        </div>

        <div class="review-section">
          <div class="review-section-title">审核备注</div>
          <div class="review-section-content">{{ selectedComment.reviewNote || '暂无备注' }}</div>
        </div>
      </div>
      <template #footer>
        <el-button @click="showCommentDialog = false">关闭</el-button>
        <el-button type="danger" plain @click="deleteComment(selectedComment?.id)">删除</el-button>
        <el-button v-if="selectedComment?.status === 'pending'" type="danger" plain @click="rejectComment(selectedComment)">驳回</el-button>
        <el-button v-if="selectedComment?.status === 'pending'" type="primary" @click="approveComment(selectedComment?.id)">审核通过</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="showReportDialog" title="举报详情" width="820px" class="admin-dialog review-detail-dialog">
      <div v-if="selectedReport" class="review-detail">
        <div class="review-meta-grid">
          <div class="review-meta-card">
            <div class="review-meta-label">所属文章</div>
            <div class="review-meta-value">{{ selectedReport.postTitle || '-' }}</div>
          </div>
          <div class="review-meta-card">
            <div class="review-meta-label">举报人</div>
            <div class="review-meta-value">{{ selectedReport.reporterName || '访客' }}</div>
          </div>
          <div class="review-meta-card">
            <div class="review-meta-label">举报状态</div>
            <div class="review-meta-value">{{ getReportStatusLabel(selectedReport) }}</div>
          </div>
          <div class="review-meta-card">
            <div class="review-meta-label">提交时间</div>
            <div class="review-meta-value">{{ formatDateTime(selectedReport.createdAt) }}</div>
          </div>
        </div>

        <div class="review-inline-grid">
          <div class="review-section">
            <div class="review-section-title">举报原因</div>
            <div class="review-section-content">{{ getReportReasonText(selectedReport) }}</div>
          </div>
          <div class="review-section">
            <div class="review-section-title">评论状态</div>
            <div class="review-section-content">
              {{ selectedReport.commentDeleted ? '评论已删除' : getCommentStatusLabel(selectedReport.commentStatus) }}
            </div>
          </div>
        </div>

        <div class="review-section">
          <div class="review-section-title">举报说明</div>
          <div class="review-section-content review-message-content">{{ selectedReport.description || '-' }}</div>
        </div>

        <div class="review-section">
          <div class="review-section-title">被举报评论</div>
          <div class="review-section-content review-message-content">{{ selectedReport.commentContent || '-' }}</div>
        </div>

        <div v-if="selectedReport.parentId" class="review-section">
          <div class="review-section-title">回复上下文</div>
          <div class="review-section-content review-message-content">
            回复对象：{{ selectedReport.parentAuthorName || '-' }}
          </div>
        </div>

        <div class="review-section">
          <div class="review-section-title">处理记录</div>
          <div class="review-section-content">
            <div>处理动作：{{ getReportResolutionLabel(selectedReport.resolutionAction) }}</div>
            <div>处理人：{{ selectedReport.resolverName || '暂无' }}</div>
            <div v-if="selectedReport.resolvedAt">处理时间：{{ formatDateTime(selectedReport.resolvedAt) }}</div>
            <div v-if="selectedReport.resolutionNote" class="report-resolution-note">处理备注：{{ selectedReport.resolutionNote }}</div>
          </div>
        </div>
      </div>
      <template #footer>
        <el-button @click="showReportDialog = false">关闭</el-button>
        <el-button
          v-if="selectedReport?.status === 'pending' && !selectedReport?.commentDeleted"
          type="primary"
          @click="resolveCommentReport(selectedReport, 'kept')"
        >保留评论</el-button>
        <el-button
          v-if="selectedReport?.status === 'pending'"
          type="danger"
          plain
          @click="resolveCommentReport(selectedReport, 'deleted')"
        >删除评论</el-button>
      </template>
    </el-dialog>

    <!-- 联系消息对话框 -->
    <el-dialog v-model="showContactDialog" title="联系消息详情" width="760px" class="admin-dialog review-detail-dialog">
      <div v-if="selectedContact" class="review-detail">
        <div class="review-meta-grid">
          <div class="review-meta-card">
            <div class="review-meta-label">姓名</div>
            <div class="review-meta-value">{{ selectedContact.name || '-' }}</div>
          </div>
          <div class="review-meta-card">
            <div class="review-meta-label">邮箱</div>
            <div class="review-meta-value">{{ selectedContact.email || '-' }}</div>
          </div>
          <div class="review-meta-card">
            <div class="review-meta-label">提交时间</div>
            <div class="review-meta-value">{{ formatDateTime(selectedContact.createTime) }}</div>
          </div>
          <div class="review-meta-card">
            <div class="review-meta-label">当前状态</div>
            <div class="review-meta-value">{{ getContactStatusLabel(selectedContact.status) }}</div>
          </div>
        </div>

        <div class="review-section">
          <div class="review-section-title">主题</div>
          <div class="review-section-content">{{ selectedContact.subject || '-' }}</div>
        </div>

        <div class="review-section">
          <div class="review-section-title">留言内容</div>
          <div class="review-section-content review-message-content">{{ selectedContact.message || '-' }}</div>
        </div>

        <div v-if="selectedContact.repliedTime" class="review-section">
          <div class="review-section-title">上次回复时间</div>
          <div class="review-section-content">{{ formatDateTime(selectedContact.repliedTime) }}</div>
        </div>

        <div class="review-section">
          <div class="review-section-title">邮件回复</div>
          <el-input
            v-model="contactReplyForm.replyContent"
            type="textarea"
            :rows="8"
            resize="vertical"
            placeholder="输入回复内容后，将通过邮件发送到对方邮箱"
          />
        </div>
      </div>
      <template #footer>
        <el-button @click="showContactDialog = false">关闭</el-button>
        <el-button type="primary" :loading="sendingContactReply" @click="sendContactReply">
          {{ selectedContact?.repliedTime ? '再次回复' : '发送回复' }}
        </el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="showMusicDialog" :title="editingId.music ? '编辑音乐' : '新增音乐'" width="620px" class="admin-dialog">
      <div class="music-upload-panel">
        <input
          ref="musicAudioInputRef"
          type="file"
          accept=".mp3,.wav,.flac,.ogg,.m4a,.aac,audio/*"
          class="avatar-file-input"
          @change="handleMusicAudioChange"
        />
        <input
          ref="musicCoverInputRef"
          type="file"
          accept="image/png,image/jpeg,image/webp,image/gif"
          class="avatar-file-input"
          @change="handleMusicCoverChange"
        />
        <div class="music-upload-actions">
          <el-button type="primary" plain @click="triggerMusicAudioSelect" :loading="musicUploading">上传音频文件</el-button>
          <el-button plain @click="triggerMusicCoverSelect" :loading="musicUploading">上传封面图片</el-button>
        </div>
        <div class="upload-hint">上传音频后会自动读取歌曲名、时长、歌手和专辑信息，你也可以再手动修改。</div>
      </div>
      <el-form :model="musicForm" label-width="88px">
        <el-form-item label="标题"><el-input v-model="musicForm.title" placeholder="自动识别后可手动调整" /></el-form-item>
        <el-form-item label="艺术家"><el-input v-model="musicForm.artist" placeholder="例如：YOASOBI" /></el-form-item>
        <el-form-item label="专辑"><el-input v-model="musicForm.album" placeholder="可留空" /></el-form-item>
        <el-form-item label="时长"><el-input :model-value="formatMusicDuration(musicForm.duration)" readonly /></el-form-item>
        <el-form-item label="音频地址"><el-input v-model="musicForm.url" readonly placeholder="请先上传音频文件" /></el-form-item>
        <el-form-item label="封面地址">
          <el-input v-model="musicForm.coverUrl" placeholder="可上传封面，也可直接填写外部图片链接" />
        </el-form-item>
        <div class="upload-hint">封面支持两种方式：上传图片自动填充，或直接粘贴外部图片链接。</div>
        <div v-if="musicForm.coverUrl" class="music-cover-preview" :class="{ failed: musicCoverPreviewFailed }">
          <img
            v-if="!musicCoverPreviewFailed"
            :src="musicForm.coverUrl"
            alt="音乐封面预览"
            referrerpolicy="no-referrer"
            @load="handleMusicCoverPreviewLoad"
            @error="handleMusicCoverPreviewError"
          />
          <div v-else class="music-cover-preview-fallback">
            <div class="music-cover-preview-title">封面预览加载失败</div>
            <div class="music-cover-preview-desc">请更换可直接访问的图片链接，或改用本地上传。</div>
          </div>
        </div>
      </el-form>
      <template #footer>
        <el-button @click="showMusicDialog = false">取消</el-button>
        <el-button type="primary" @click="saveMusic" :loading="saving">保存</el-button>
      </template>
    </el-dialog>

    <!-- 用户对话框 -->
    <el-dialog v-model="showUserDialog" :title="editingId.user ? '编辑用户' : '添加用户'" width="500px" class="admin-dialog">
      <el-form :model="userForm" label-width="80px">
        <el-form-item label="用户名"><el-input v-model="userForm.username" :disabled="!!editingId.user" /></el-form-item>
        <el-form-item label="昵称"><el-input v-model="userForm.nickname" /></el-form-item>
        <el-form-item label="邮箱"><el-input v-model="userForm.email" /></el-form-item>
        <el-form-item label="密码" v-if="!editingId.user">
          <el-input v-model="userForm.password" type="password" show-password />
          <div class="password-rules-inline">
            <div
              v-for="item in userPasswordRuleItems"
              :key="item.key"
              :class="['password-rule-inline', { met: item.met }]"
            >
              {{ item.met ? '[OK]' : '[ ]' }} {{ item.label }}
            </div>
          </div>
        </el-form-item>
        <el-form-item label="角色">
          <span v-if="isProtectedStationMasterAccount(userForm)" class="badge badge-danger">站长（唯一）</span>
          <el-radio-group v-else v-model="userForm.role">
            <el-radio value="user">用户</el-radio>
            <el-radio v-if="isStationMaster" value="manager">管理员</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showUserDialog = false">取消</el-button>
        <el-button type="primary" @click="saveUser" :loading="saving">保存</el-button>
      </template>
    </el-dialog>

    <!-- 友链对话框 -->
    <el-dialog v-model="showLinkDialog" :title="editingId.link ? '编辑友链' : '添加友链'" width="500px" class="admin-dialog">
      <el-form :model="linkForm" label-width="80px">
        <el-form-item label="名称"><el-input v-model="linkForm.name" /></el-form-item>
        <el-form-item label="链接"><el-input v-model="linkForm.url" placeholder="https://..." /></el-form-item>
        <el-form-item label="描述"><el-input v-model="linkForm.description" type="textarea" rows="2" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showLinkDialog = false">取消</el-button>
        <el-button type="primary" @click="saveLink" :loading="saving">保存</el-button>
      </template>
    </el-dialog>

    <!-- 头像上传对话框 -->
    <el-dialog v-model="showAvatarDialog" title="设置头像" width="400px" class="admin-dialog">
      <div class="avatar-dialog-content">
        <div class="avatar-upload-actions">
          <input
            ref="avatarFileInputRef"
            type="file"
            accept="image/png,image/jpeg,image/webp,image/gif"
            class="avatar-file-input"
            @change="handleAvatarFileChange"
          />
          <el-button @click="triggerAvatarFileSelect">本地上传</el-button>
          <span class="upload-hint">支持 PNG/JPG/WebP/GIF，上传后会自动压缩</span>
        </div>
        <div class="form-item">
          <label>头像地址</label>
          <input v-model="avatarUrl" class="form-input" placeholder="https://example.com/avatar.jpg 或使用上方本地上传" />
        </div>
        <div class="avatar-preview" v-if="avatarUrl">
          <img :src="avatarUrl" alt="预览" @error="avatarUrl = ''" />
        </div>
        <div v-else class="avatar-preview empty">
          <div class="avatar-preview-placeholder">
            <svg class="avatar-placeholder-icon" viewBox="0 0 24 24" fill="none" aria-hidden="true">
              <component :is="node.tag" v-for="(node, index) in avatarPlaceholderNodes" :key="`dialog-avatar-${index}`" v-bind="node.attrs" />
            </svg>
          </div>
        </div>
      </div>
      <template #footer>
        <el-button @click="showAvatarDialog = false">取消</el-button>
        <el-button type="primary" @click="saveAvatar" :loading="savingProfile">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, onBeforeUnmount, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useUserStore, useThemeStore } from '@/stores'
import AdminListPagination from '@/components/admin/AdminListPagination.vue'
import MarkdownEditor from '@/components/MarkdownEditor.vue'
import MarkdownPreview from '@/components/MarkdownPreview.vue'
import { useAdminData } from '@/composables/useAdminData'
import { useAdminMenuState } from '@/composables/useAdminMenuState'
import { useAdminProfile } from '@/composables/useAdminProfile'
import { POST_CATEGORIES, createEmptyPostForm } from '@/constants/postEditor'
import { syncCurrentProfile } from '@/utils/profile'
import { getPasswordRuleItems, getPasswordValidationMessage, sanitizePasswordInput } from '@/utils/passwordRules'
import { getRequestErrorMessage } from '@/utils/request'
import { normalizeTagInput } from '@/utils/tags'
import { adminApi } from '@/api'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const themeStore = useThemeStore()
const resolveInitialActiveMenu = () => {
  const menu = typeof route.query.menu === 'string' ? route.query.menu.trim() : ''
  return menu || 'dashboard'
}

const menuReady = ref(false)
const activeMenu = ref(resolveInitialActiveMenu())
const saving = ref(false)
const showPostDialog = ref(false)
const showMusicDialog = ref(false)
const showUserDialog = ref(false)
const showLinkDialog = ref(false)
const showAvatarDialog = ref(false)
const showReviewDialog = ref(false)
const showCommentDialog = ref(false)
const showReportDialog = ref(false)
const showContactDialog = ref(false)
const editingId = ref({ post: null, review: null, music: null, user: null, link: null })
const avatarFileInputRef = ref(null)
const musicAudioInputRef = ref(null)
const musicCoverInputRef = ref(null)
const musicUploading = ref(false)
const postFilters = ref({ creatorId: '' })
const commentFilters = ref({ keyword: '', postId: '' })
const reportFilters = ref({ keyword: '' })
const activeReviewList = ref('pending')
const activeCommentList = ref('pending')
const activeReportList = ref('pending')

const posts = ref([])
const postReviews = ref([])
const adminComments = ref([])
const commentReports = ref([])
const selectedReview = ref(null)
const selectedComment = ref(null)
const selectedReport = ref(null)
const selectedContact = ref(null)
const musicList = ref([])
const users = ref([])
const links = ref([])
const contactMessages = ref([])
const stats = ref({
  posts: 0,
  music: 0,
  users: 0,
  links: 0,
  comments: 0,
  contacts: 0,
  pendingReviews: 0,
  pendingComments: 0,
  pendingContacts: 0,
  pending: 0,
  rejected: 0,
  published: 0
})
const createListMeta = (page = 1, size = 10) => ({
  page,
  size,
  total: 0,
  summary: {}
})
const listMeta = reactive({
  posts: createListMeta(1, 10),
  reviews: createListMeta(1, 10),
  comments: createListMeta(1, 10),
  reports: createListMeta(1, 10),
  music: createListMeta(1, 10),
  users: createListMeta(1, 10),
  links: createListMeta(1, 10),
  contacts: createListMeta(1, 10)
})
const selectedReviewIds = ref([])
const selectedCommentIds = ref([])
const moderationTask = ref(null)
let moderationTaskTimer = null
let moderationTaskCloseTimer = null
const commentClientRegionLabels = {
  local: '',
  'ipv6-network': '',
  'public-network': ''
}
const commentClientOsLabels = {
  windows: 'Windows',
  macos: 'macOS',
  ios: 'iOS',
  ipados: 'iPadOS',
  android: 'Android',
  harmonyos: 'HarmonyOS',
  linux: 'Linux',
  chromeos: 'ChromeOS',
  unix: 'Unix'
}
const commentClientBrowserLabels = {
  edge: 'Edge',
  chrome: 'Chrome',
  firefox: 'Firefox',
  safari: 'Safari',
  opera: 'Opera',
  'samsung-internet': 'Samsung Internet',
  'qq-browser': 'QQ Browser',
  wechat: 'WeChat',
  ie: 'Internet Explorer'
}

const categories = POST_CATEGORIES
const avatarPlaceholderNodes = [
  { tag: 'circle', attrs: { cx: '12', cy: '8', r: '4', stroke: 'currentColor', 'stroke-width': '1.5' } },
  { tag: 'path', attrs: { d: 'M4 20c0-4 4-6 8-6s8 2 8 6', stroke: 'currentColor', 'stroke-width': '1.5', 'stroke-linecap': 'round' } }
]
const adminIconMap = {
  dashboard: [
    { tag: 'rect', attrs: { x: '3', y: '4', width: '7', height: '7', rx: '2' } },
    { tag: 'rect', attrs: { x: '14', y: '4', width: '7', height: '4', rx: '2' } },
    { tag: 'rect', attrs: { x: '14', y: '11', width: '7', height: '9', rx: '2' } },
    { tag: 'rect', attrs: { x: '3', y: '14', width: '7', height: '6', rx: '2' } }
  ],
  posts: [
    { tag: 'path', attrs: { d: 'M7 4.5h8l4 4V19a1.5 1.5 0 0 1-1.5 1.5h-10A1.5 1.5 0 0 1 6 19V6A1.5 1.5 0 0 1 7.5 4.5Z' } },
    { tag: 'path', attrs: { d: 'M15 4.5V9h4' } },
    { tag: 'path', attrs: { d: 'M9 12h6' } },
    { tag: 'path', attrs: { d: 'M9 15.5h6' } }
  ],
  reviews: [
    { tag: 'circle', attrs: { cx: '12', cy: '12', r: '8.5' } },
    { tag: 'path', attrs: { d: 'm8.5 12 2.2 2.2L15.8 9' } }
  ],
  comments: [
    { tag: 'path', attrs: { d: 'M20 15.2a3 3 0 0 1-3 3H9.8L5 21v-2.8H7a3 3 0 0 1-3-3V7a3 3 0 0 1 3-3h10a3 3 0 0 1 3 3Z' } }
  ],
  music: [
    { tag: 'path', attrs: { d: 'M15 5v9.5' } },
    { tag: 'path', attrs: { d: 'M15 5 9 6.5v9' } },
    { tag: 'circle', attrs: { cx: '9', cy: '17', r: '2.5' } },
    { tag: 'circle', attrs: { cx: '15', cy: '15', r: '2.5' } }
  ],
  users: [
    { tag: 'circle', attrs: { cx: '9', cy: '9', r: '3.2' } },
    { tag: 'path', attrs: { d: 'M4.5 19c0-2.7 2.1-4.8 4.5-4.8s4.5 2.1 4.5 4.8' } },
    { tag: 'circle', attrs: { cx: '17', cy: '10', r: '2.5' } },
    { tag: 'path', attrs: { d: 'M14.5 19c.3-1.9 1.8-3.4 3.8-3.8' } }
  ],
  links: [
    { tag: 'path', attrs: { d: 'M10.5 13.5 13.5 10.5' } },
    { tag: 'path', attrs: { d: 'M7.8 15.8 5.9 17.7a3 3 0 1 1-4.2-4.2l1.9-1.9a3 3 0 0 1 4.2 0' } },
    { tag: 'path', attrs: { d: 'M16.2 8.2 18.1 6.3a3 3 0 1 1 4.2 4.2l-1.9 1.9a3 3 0 0 1-4.2 0' } }
  ],
  contacts: [
    { tag: 'rect', attrs: { x: '3', y: '5', width: '18', height: '14', rx: '2.5' } },
    { tag: 'path', attrs: { d: 'm5.5 7.5 6.5 5 6.5-5' } }
  ],
  profile: [
    { tag: 'circle', attrs: { cx: '12', cy: '8', r: '3.5' } },
    { tag: 'path', attrs: { d: 'M5 19c0-3.1 3-5.5 7-5.5s7 2.4 7 5.5' } }
  ],
  home: [
    { tag: 'path', attrs: { d: 'M4 10.5 12 4l8 6.5' } },
    { tag: 'path', attrs: { d: 'M6.5 9.5V20h11V9.5' } }
  ],
  logout: [
    { tag: 'path', attrs: { d: 'M9 5H5.8A1.8 1.8 0 0 0 4 6.8v10.4A1.8 1.8 0 0 0 5.8 19H9' } },
    { tag: 'path', attrs: { d: 'M13 8 18 12 13 16' } },
    { tag: 'path', attrs: { d: 'M10 12h8' } }
  ],
  pending: [
    { tag: 'circle', attrs: { cx: '12', cy: '12', r: '8.5' } },
    { tag: 'path', attrs: { d: 'M12 7.8v4.6l2.8 1.8' } }
  ],
  rejected: [
    { tag: 'circle', attrs: { cx: '12', cy: '12', r: '8.5' } },
    { tag: 'path', attrs: { d: 'm9 9 6 6' } },
    { tag: 'path', attrs: { d: 'm15 9-6 6' } }
  ],
  published: [
    { tag: 'path', attrs: { d: 'M5 12.5 12 5l7 7.5' } },
    { tag: 'path', attrs: { d: 'M12 5v14' } },
    { tag: 'path', attrs: { d: 'M8.5 15.5 12 19l3.5-3.5' } }
  ],
  default: [
    { tag: 'circle', attrs: { cx: '12', cy: '12', r: '8.5' } }
  ]
}
const getAdminIconNodes = (key) => adminIconMap[key] || adminIconMap.default

const postFormRef = ref(null)
const postForm = ref(createEmptyPostForm())
const createEmptyMusicForm = () => ({ title: '', artist: '', album: '', url: '', coverUrl: '', duration: 0, sort: 0, status: 1 })
const musicForm = ref(createEmptyMusicForm())
const musicCoverPreviewFailed = ref(false)
const MUSIC_LIST_UPDATED_EVENT = 'music-list-updated'
const MUSIC_LIST_UPDATED_STORAGE_KEY = 'music_list_updated'
const userForm = ref({ username: '', nickname: '', email: '', password: '', role: 'user' })
const linkForm = ref({ name: '', url: '', description: '' })
const contactReplyForm = ref({ replyContent: '' })
const sendingContactReply = ref(false)

// 文章表单验证规则
const postRules = {
  title: [{ required: true, message: '请输入文章标题', trigger: 'blur' }],
  category: [{ required: true, message: '请选择文章分类', trigger: 'change' }],
  tags: [{ required: true, message: '请输入文章标签', trigger: 'blur' }]
}

// 个人信息
const isElevatedRole = (role) => role === 'admin' || role === 'manager'
const normalizeManagedUserRole = (role) => (['admin', 'manager', 'user'].includes(role) ? role : 'user')
const isAdmin = computed(() => isElevatedRole(userStore.user?.role))
const isStationMaster = computed(() => userStore.user?.role === 'admin' && userStore.user?.username === 'admin')
const canManageMusic = computed(() => isStationMaster.value)
const isProtectedStationMasterAccount = (user) => user?.role === 'admin' && user?.username === 'admin'
const isManagerAccount = (user) => user?.role === 'manager'
const getUserRoleLabel = (user) => {
  if (isProtectedStationMasterAccount(user)) return '站长'
  if (isManagerAccount(user)) return '管理员'
  return '用户'
}
const getUserRoleBadgeClass = (user) => {
  if (isProtectedStationMasterAccount(user)) return 'badge-danger'
  if (isManagerAccount(user)) return 'badge-warning'
  return 'badge-success'
}
const currentUserRoleLabel = computed(() => {
  if (isStationMaster.value) return '站长'
  if (isAdmin.value) return '管理员'
  return '普通用户'
})
const isCurrentUserAccount = (user) => user?.id && userStore.user?.id && user.id === userStore.user.id
const canEditUserAccount = (user) => {
  if (isCurrentUserAccount(user) && !isStationMaster.value) return false
  if (isProtectedStationMasterAccount(user)) return isStationMaster.value
  if (isManagerAccount(user)) return isStationMaster.value
  return true
}
const canDeleteUserAccount = (user) => {
  if (isProtectedStationMasterAccount(user)) return false
  if (isManagerAccount(user)) return isStationMaster.value
  return true
}
const {
  savingProfile,
  savingPassword,
  savingEmail,
  sendingCode,
  profileForm,
  passwordForm,
  emailForm,
  emailCooldown,
  avatarUrl,
  passwordRuleItems,
  initProfileForm,
  saveProfile,
  sendEmailCode,
  changeEmail,
  saveAvatar,
  loadAvatarFile,
  changePassword
} = useAdminProfile({
  activeMenu,
  userStore,
  showAvatarDialog,
  ElMessage
})

const userPasswordRuleItems = computed(() => getPasswordRuleItems(userForm.value.password))

watch(() => userForm.value.password, (value) => {
  const sanitized = sanitizePasswordInput(value)
  if (sanitized !== value) {
    userForm.value.password = sanitized
  }
})

watch(() => musicForm.value.coverUrl, () => {
  musicCoverPreviewFailed.value = false
})

const {
  dataLoading,
  fetchOverviewData,
  fetchArticleData,
  fetchCommentData,
  fetchCommentReportData,
  fetchUsersData,
  fetchLinkData,
  fetchContactMessageData,
  fetchMusicData
} = useAdminData({
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
  getPostParams: () => getPostQueryParams(),
  getReviewParams: () => ({ status: activeReviewList.value }),
  getCommentParams: () => getCommentQueryParams(),
  getReportParams: () => getReportQueryParams()
})

const postListLoading = computed(() => dataLoading.posts)
const reviewListLoading = computed(() => dataLoading.reviews)

const navItems = computed(() => (
  isAdmin.value
    ? [
        { key: 'dashboard', label: '控制台', icon: 'dashboard' },
        { key: 'posts', label: '文章管理', icon: 'posts' },
        { key: 'reviews', label: '文章审核', icon: 'reviews' },
        { key: 'comments', label: '评论管理', icon: 'comments' },
        { key: 'reports', label: '举报管理', icon: 'rejected' },
        ...(canManageMusic.value ? [{ key: 'music', label: '音乐管理', icon: 'music' }] : []),
        { key: 'users', label: '用户管理', icon: 'users' },
        { key: 'links', label: '友链管理', icon: 'links' },
        { key: 'contacts', label: '联系消息', icon: 'contacts' },
        { key: 'profile', label: '个人信息', icon: 'profile' }
      ]
    : [
        { key: 'dashboard', label: '工作台', icon: 'dashboard' },
        { key: 'posts', label: '我的文章', icon: 'posts' },
        { key: 'profile', label: '个人信息', icon: 'profile' }
      ]
))

const {
  handleMenuClick,
  handleModulePageChange,
  handleModulePageSizeChange,
  forceRefreshMenu,
  markMenuDirty,
  refreshMenuData,
  resetMenuPage,
  initializeMenuState,
  syncMenuFromRoute
} = useAdminMenuState({
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
})

const currentPageTitle = computed(() => navItems.value.find(i => i.key === activeMenu.value)?.label || '控制台')

const hasSummaryValue = (summary, key) => Object.prototype.hasOwnProperty.call(summary || {}, key)
const shouldUseReviewSummary = computed(() => {
  const summary = listMeta.reviews.summary || {}
  if (hasSummaryValue(summary, 'pending') || hasSummaryValue(summary, 'approved') || hasSummaryValue(summary, 'rejected')) {
    return true
  }
  return postReviews.value.length === 0
})
const shouldUseCommentSummary = computed(() => {
  const summary = listMeta.comments.summary || {}
  if (hasSummaryValue(summary, 'pending') || hasSummaryValue(summary, 'visible') || hasSummaryValue(summary, 'rejected')) {
    return true
  }
  return adminComments.value.length === 0
})
const shouldUseReportSummary = computed(() => {
  const summary = listMeta.reports.summary || {}
  if (hasSummaryValue(summary, 'pending') || hasSummaryValue(summary, 'resolved')) {
    return true
  }
  return commentReports.value.length === 0
})
const pendingReviewCount = computed(() => (
  shouldUseReviewSummary.value
    ? Number(listMeta.reviews.summary?.pending || 0)
    : postReviews.value.filter(item => item?.reviewStatus === 'pending').length
))
const approvedReviewCount = computed(() => (
  shouldUseReviewSummary.value
    ? Number(listMeta.reviews.summary?.approved || 0)
    : postReviews.value.filter(item => item?.reviewStatus === 'approved').length
))
const rejectedReviewCount = computed(() => (
  shouldUseReviewSummary.value
    ? Number(listMeta.reviews.summary?.rejected || 0)
    : postReviews.value.filter(item => item?.reviewStatus === 'rejected').length
))
const reviewListOptions = computed(() => ([
  { key: 'pending', label: '待审核', count: pendingReviewCount.value, badgeClass: 'badge-warning' },
  { key: 'approved', label: '已审核', count: approvedReviewCount.value, badgeClass: 'badge-success' },
  { key: 'rejected', label: '驳回', count: rejectedReviewCount.value, badgeClass: 'badge-danger' }
]))
const filteredPostReviews = computed(() => postReviews.value.filter((item) => {
  if (!item) return false
  if (!activeReviewList.value || activeReviewList.value === 'all') return true
  return String(item.reviewStatus || '').toLowerCase() === String(activeReviewList.value).toLowerCase()
}))
const currentReviewListLabel = computed(() => (
  reviewListOptions.value.find(item => item.key === activeReviewList.value)?.label || '待审核'
))
const pendingCommentCount = computed(() => (
  shouldUseCommentSummary.value
    ? Number(listMeta.comments.summary?.pending || 0)
    : adminComments.value.filter(item => item?.status === 'pending').length
))
const approvedCommentCount = computed(() => (
  shouldUseCommentSummary.value
    ? Number(listMeta.comments.summary?.visible || 0)
    : adminComments.value.filter(item => item?.status === 'visible').length
))
const rejectedCommentCount = computed(() => (
  shouldUseCommentSummary.value
    ? Number(listMeta.comments.summary?.rejected || 0)
    : adminComments.value.filter(item => item?.status === 'rejected').length
))
const commentListOptions = computed(() => ([
  { key: 'pending', label: '待审核', count: pendingCommentCount.value, badgeClass: 'badge-warning' },
  { key: 'visible', label: '已通过', count: approvedCommentCount.value, badgeClass: 'badge-success' },
  { key: 'rejected', label: '已驳回', count: rejectedCommentCount.value, badgeClass: 'badge-danger' }
]))
const commentPostOptions = computed(() => {
  const options = new Map()
  adminComments.value.forEach((item) => {
    if (!item?.postId) return
    const key = String(item.postId)
    if (!options.has(key)) {
      options.set(key, {
        value: key,
        label: item.postTitle || `文章 #${key}`
      })
    }
  })
  return Array.from(options.values()).sort((a, b) => a.label.localeCompare(b.label, 'zh-CN'))
})
watch(commentPostOptions, (options) => {
  const selectedPostId = String(commentFilters.value.postId || '').trim()
  if (selectedPostId && !options.some(option => option.value === selectedPostId)) {
    commentFilters.value.postId = ''
  }
})
const filteredAdminComments = computed(() => adminComments.value.filter((item) => {
  if (!item) return false
  if (!activeCommentList.value || activeCommentList.value === 'all') return true
  return String(item.status || '').toLowerCase() === String(activeCommentList.value).toLowerCase()
}))
const currentCommentListLabel = computed(() => (
  commentListOptions.value.find(item => item.key === activeCommentList.value)?.label || '待审核'
))
const pendingReportCount = computed(() => (
  shouldUseReportSummary.value
    ? Number(listMeta.reports.summary?.pending || 0)
    : commentReports.value.filter(item => item?.status === 'pending').length
))
const resolvedReportCount = computed(() => (
  shouldUseReportSummary.value
    ? Number(listMeta.reports.summary?.resolved || 0)
    : commentReports.value.filter(item => item?.status === 'resolved').length
))
const reportListOptions = computed(() => ([
  { key: 'pending', label: '待处理', count: pendingReportCount.value, badgeClass: 'badge-warning' },
  { key: 'resolved', label: '已处理', count: resolvedReportCount.value, badgeClass: 'badge-success' }
]))
const filteredCommentReports = computed(() => commentReports.value.filter((item) => {
  if (!item) return false
  if (!activeReportList.value || activeReportList.value === 'all') return true
  return String(item.status || '').toLowerCase() === String(activeReportList.value).toLowerCase()
}))
const currentReportListLabel = computed(() => (
  reportListOptions.value.find(item => item.key === activeReportList.value)?.label || '待处理'
))
const visibleReviewIds = computed(() => filteredPostReviews.value.map(item => String(item.id)))
const visibleCommentIds = computed(() => filteredAdminComments.value.map(item => String(item.id)))
const areAllVisibleReviewsSelected = computed(() => visibleReviewIds.value.length > 0 && visibleReviewIds.value.every(id => selectedReviewIds.value.includes(id)))
const areAllVisibleCommentsSelected = computed(() => visibleCommentIds.value.length > 0 && visibleCommentIds.value.every(id => selectedCommentIds.value.includes(id)))
const isReviewSelectionIndeterminate = computed(() => selectedReviewIds.value.length > 0 && !areAllVisibleReviewsSelected.value)
const isCommentSelectionIndeterminate = computed(() => selectedCommentIds.value.length > 0 && !areAllVisibleCommentsSelected.value)
watch(visibleReviewIds, (ids) => {
  const idSet = new Set(ids)
  selectedReviewIds.value = selectedReviewIds.value.filter(id => idSet.has(id))
})
watch(visibleCommentIds, (ids) => {
  const idSet = new Set(ids)
  selectedCommentIds.value = selectedCommentIds.value.filter(id => idSet.has(id))
})
const moderationTaskProgress = computed(() => {
  const total = Number(moderationTask.value?.totalCount || 0)
  const processed = Number(moderationTask.value?.processedCount || 0)
  if (!total) {
    return 0
  }
  return Math.min(100, Math.round((processed / total) * 100))
})
const moderationTaskStatusLabel = computed(() => {
  const status = moderationTask.value?.status
  if (status === 'queued') return '排队中'
  if (status === 'running') return '处理中'
  if (status === 'completed') return '已完成'
  if (status === 'failed') return '执行失败'
  return '未知状态'
})
const moderationTaskBadgeClass = computed(() => {
  const status = moderationTask.value?.status
  if (status === 'completed') return 'badge-success'
  if (status === 'failed') return 'badge-danger'
  return 'badge-warning'
})
const moderationTaskTitle = computed(() => {
  if (!moderationTask.value) {
    return '批量任务'
  }
  const targetLabel = moderationTask.value.menuKey === 'reviews' ? '文章审核' : '评论管理'
  const operationMap = {
    approve: '批量通过',
    reject: '批量驳回',
    delete: '批量删除'
  }
  return `${targetLabel} · ${operationMap[moderationTask.value.operation] || '批量处理'}`
})
const getAdminCommentClientTags = (comment) => {
  if (!comment || typeof comment !== 'object') {
    return []
  }

  const tags = []
  const regionKey = String(comment.clientRegion || '')
  const region = regionKey && regionKey !== 'unknown'
    ? (Object.prototype.hasOwnProperty.call(commentClientRegionLabels, regionKey) ? commentClientRegionLabels[regionKey] : regionKey)
    : ''
  const os = comment.clientOs && comment.clientOs !== 'unknown'
    ? (commentClientOsLabels[comment.clientOs] || comment.clientOs)
    : ''
  const browser = comment.clientBrowser && comment.clientBrowser !== 'unknown'
    ? (commentClientBrowserLabels[comment.clientBrowser] || comment.clientBrowser)
    : ''

  if (region) tags.push(region)
  if (os) tags.push(os)
  if (browser) tags.push(browser)
  return tags
}
const getReportReasonText = (report) => {
  if (!report) return '-'
  const customReason = String(report.otherReason || '').trim()
  return customReason ? `${report.reasonLabel || '其他'}：${customReason}` : (report.reasonLabel || '-')
}
const getReportStatusLabel = (report) => {
  if (!report) return '未知'
  if (report.status === 'resolved') {
    return report.resolutionAction === 'deleted' ? '已处理 · 已删评' : '已处理 · 已保留'
  }
  return '待处理'
}
const getReportStatusBadgeClass = (report) => {
  if (!report) return 'badge-default'
  if (report.status === 'resolved') {
    return report.resolutionAction === 'deleted' ? 'badge-danger' : 'badge-success'
  }
  return 'badge-warning'
}
const getReportResolutionLabel = (action) => {
  if (action === 'deleted') return '删除评论'
  if (action === 'kept') return '保留评论'
  return '尚未处理'
}
const pendingContactCount = computed(() => Number(stats.value.pendingContacts || listMeta.contacts.summary?.pending || 0))
const myPendingCount = computed(() => Number(stats.value.pending || listMeta.posts.summary?.pending || 0))
const myRejectedCount = computed(() => Number(stats.value.rejected || listMeta.posts.summary?.rejected || 0))
const myPublishedCount = computed(() => Number(stats.value.published || listMeta.posts.summary?.published || 0))
const postCreatorOptions = computed(() => users.value
  .map(user => ({
    id: user.id,
    label: user.nickname ? `${user.nickname} (${user.username})` : user.username
  }))
  .sort((a, b) => a.label.localeCompare(b.label, 'zh-CN'))
)

const statCards = computed(() => (
  isAdmin.value
    ? [
        { icon: 'posts', label: '文章总数', value: stats.value.posts },
        { icon: 'reviews', label: '待审核数', value: stats.value.pendingReviews || pendingReviewCount.value },
        { icon: 'comments', label: '待审评论', value: stats.value.pendingComments || pendingCommentCount.value },
        { icon: 'users', label: '用户总数', value: stats.value.users },
        { icon: 'links', label: '友链总数', value: stats.value.links },
        { icon: 'contacts', label: '待处理留言', value: stats.value.pendingContacts || pendingContactCount.value }
      ]
    : [
        { icon: 'posts', label: '我的条目', value: stats.value.posts || listMeta.posts.summary?.total || posts.value.length },
        { icon: 'pending', label: '待审核', value: myPendingCount.value },
        { icon: 'rejected', label: '未通过', value: myRejectedCount.value },
        { icon: 'published', label: '已发布', value: myPublishedCount.value }
      ]
))

const formatDate = (d) => {
  if (!d) return '-'
  return new Date(d).toLocaleDateString('zh-CN')
}

const formatDateTime = (d) => {
  if (!d) return '-'
  return new Date(d).toLocaleString('zh-CN', { hour12: false })
}

const formatMusicDuration = (seconds) => {
  if (!seconds || Number.isNaN(Number(seconds))) return '--:--'
  const totalSeconds = Number(seconds)
  const minutes = Math.floor(totalSeconds / 60)
  const remainSeconds = Math.floor(totalSeconds % 60)
  return `${String(minutes).padStart(2, '0')}:${String(remainSeconds).padStart(2, '0')}`
}

const getPageRowIndex = (menuKey, index) => {
  const meta = listMeta[menuKey]
  if (!meta) return index + 1
  return (Math.max(1, meta.page) - 1) * Math.max(1, meta.size) + index + 1
}

const stripFileExtension = (filename = '') => filename.replace(/\.[^.]+$/, '')

const detectAudioDurationFromFile = (file) => new Promise((resolve) => {
  const objectUrl = URL.createObjectURL(file)
  const audio = new Audio()
  let settled = false

  const done = (value = 0) => {
    if (settled) return
    settled = true
    audio.pause()
    audio.removeAttribute('src')
    audio.load()
    URL.revokeObjectURL(objectUrl)
    resolve(value)
  }

  audio.preload = 'metadata'
  audio.onloadedmetadata = () => {
    const duration = Number(audio.duration)
    done(Number.isFinite(duration) && duration > 0 ? Math.round(duration) : 0)
  }
  audio.onerror = () => done(0)

  window.setTimeout(() => done(0), 10000)
  audio.src = objectUrl
})

const getPostQueryParams = () => {
  if (!isAdmin.value) return {}

  const params = {}
  if (postFilters.value.creatorId !== '' && postFilters.value.creatorId !== null && postFilters.value.creatorId !== undefined) {
    params.creatorId = postFilters.value.creatorId
  }
  return params
}

const getCommentQueryParams = () => {
  const params = {
    status: activeCommentList.value
  }
  const keyword = String(commentFilters.value.keyword || '').trim()
  const postId = String(commentFilters.value.postId || '').trim()
  if (keyword) {
    params.keyword = keyword
  }
  if (postId) {
    params.postId = Number(postId)
  }
  return params
}

const getReportQueryParams = () => {
  const params = {
    status: activeReportList.value
  }
  const keyword = String(reportFilters.value.keyword || '').trim()
  if (keyword) {
    params.keyword = keyword
  }
  return params
}

const getPostDisplayTime = (row) => row.displayTime || row.updateTime || row.createTime

const getPostStatusLabel = (row) => {
  const status = isAdmin.value ? row.status : row.postStatus
  return status === 'published' ? '已发布' : '未发布'
}

const getPostStatusBadgeClass = (row) => {
  const status = isAdmin.value ? row.status : row.postStatus
  if (!isAdmin.value && row?.reviewStatus === 'pending') return 'badge-warning'
  return status === 'published' ? 'badge-success' : 'badge-default'
}

const getManagedPostId = (row) => (isAdmin.value ? row.id : row.postId)

const buildPostPayload = (row, status) => ({
  title: row.title || '',
  slug: row.slug || '',
  content: row.content || '',
  summary: row.summary || row.excerpt || '',
  excerpt: row.excerpt || row.summary || '',
  category: row.category || '',
  tags: normalizeTagInput(row.tags || ''),
  coverImage: row.coverImage || '',
  status
})

const canPublishPost = (row) => {
  const status = isAdmin.value ? row.status : row.postStatus
  if (status !== 'draft') return false
  if (!isAdmin.value && row.itemType !== 'post' && !(row.itemType === 'review' && !row.postId)) return false
  return row.reviewStatus !== 'pending'
}

const canOfflinePost = (row) => {
  const status = isAdmin.value ? row.status : row.postStatus
  if (status !== 'published') return false
  if (!isAdmin.value && row.itemType !== 'post') return false
  return row.reviewStatus !== 'pending'
}

const canWithdrawReview = (row) => (
  !isAdmin.value
  && !!row?.reviewId
  && (row?.reviewStatus === 'pending' || row?.reviewStatus === 'rejected')
)

const canDeleteOwnPost = (row) => (
  !isAdmin.value
  && row?.itemType === 'post'
  && row?.postStatus === 'draft'
  && !row?.reviewStatus
  && !!row?.postId
)

const isPublishedPost = (row) => {
  const status = isAdmin.value ? row.status : row.postStatus
  return status === 'published'
}

const shouldHidePublishedActions = (row) => {
  if (isAdmin.value) return row?.status === 'published'
  return row?.itemType === 'post' && row?.postStatus === 'published' && !row?.reviewStatus
}

const isApprovedWorkspacePost = (row) => !isAdmin.value && row?.itemType === 'post' && !row?.reviewStatus

const needsEditConfirmation = (row) => !isAdmin.value && row?.itemType === 'post' && row?.postStatus === 'draft' && !row?.reviewStatus

const getPublishActionLabel = (row) => {
  if (isAdmin.value) return '发布'
  return isApprovedWorkspacePost(row) ? '发布' : '提审'
}

const getPostDisplayStatusLabel = (row) => {
  if (!isAdmin.value && row?.reviewStatus === 'pending' && row?.postStatus === 'published') {
    return '待发布'
  }
  if (!isAdmin.value && row?.reviewStatus === 'pending' && row?.postStatus === 'draft') {
    return '待下线'
  }
  return isPublishedPost(row) ? '已发布' : '未发布'
}

const getReviewStatusLabel = (value) => {
  const status = typeof value === 'object' && value !== null ? value.reviewStatus : value
  if (typeof value === 'object' && value !== null && isApprovedWorkspacePost(value)) return '已审核'
  if (status === 'withdrawn') return '未审核'
  if (status === 'draft') return '未提审'
  if (status === 'pending') return '待审核'
  if (status === 'rejected') return '未通过'
  if (status === 'approved') return '已通过'
  return '无需审核'
}

const getReviewStatusBadgeClass = (value) => {
  const status = typeof value === 'object' && value !== null ? value.reviewStatus : value
  if (typeof value === 'object' && value !== null && isApprovedWorkspacePost(value)) return 'badge-success'
  if (status === 'withdrawn') return 'badge-default'
  if (status === 'draft') return 'badge-default'
  if (status === 'pending') return 'badge-warning'
  if (status === 'rejected') return 'badge-danger'
  if (status === 'approved') return 'badge-success'
  return 'badge-info'
}

const getCommentStatusLabel = (status) => {
  if (status === 'visible') return '已通过'
  if (status === 'rejected') return '已驳回'
  return '待审核'
}

const getContactStatusLabel = (status) => (status === 1 ? '已处理' : '待处理')

const getContactStatusBadgeClass = (status) => (status === 1 ? 'badge-success' : 'badge-warning')

const goHome = () => router.push('/')
const handleLogout = async () => {
  await userStore.logout()
  router.push('/')
}

const notifyLinksUpdated = () => {
  localStorage.setItem('admin_links_updated', Date.now().toString())
  window.dispatchEvent(new CustomEvent('links-updated'))
}

const syncProfile = () => syncCurrentProfile(userStore)

const triggerAvatarFileSelect = () => {
  avatarFileInputRef.value?.click()
}

const handleAvatarFileChange = async (event) => {
  const file = event.target.files?.[0]
  await loadAvatarFile(file)
  event.target.value = ''
}

const triggerMusicAudioSelect = () => {
  if (!canManageMusic.value) {
    ElMessage.warning('只有站长可以管理音乐')
    return
  }
  musicAudioInputRef.value?.click()
}

const triggerMusicCoverSelect = () => {
  if (!canManageMusic.value) {
    ElMessage.warning('只有站长可以管理音乐')
    return
  }
  musicCoverInputRef.value?.click()
}

const uploadMusicAssets = async ({ audioFile = null, coverFile = null } = {}) => {
  if (!audioFile && !coverFile) return
  if (!canManageMusic.value) {
    ElMessage.warning('只有站长可以管理音乐')
    return
  }

  const formData = new FormData()
  if (audioFile) formData.append('audio', audioFile)
  if (coverFile) formData.append('cover', coverFile)

  musicUploading.value = true
  try {
    const res = await adminApi.uploadMusicAssets(formData)
    const uploaded = res.data || {}

    musicForm.value = {
      ...musicForm.value,
      ...(uploaded.title ? { title: uploaded.title } : (audioFile && !musicForm.value.title ? { title: stripFileExtension(audioFile.name) } : {})),
      ...(uploaded.artist ? { artist: uploaded.artist } : {}),
      ...(uploaded.album ? { album: uploaded.album } : {}),
      ...(uploaded.url ? { url: uploaded.url } : {}),
      ...(uploaded.coverUrl ? { coverUrl: uploaded.coverUrl } : {}),
      ...(typeof uploaded.duration === 'number' ? { duration: uploaded.duration } : {})
    }

    if (audioFile && (!uploaded.duration || Number(uploaded.duration) <= 0)) {
      const detectedDuration = await detectAudioDurationFromFile(audioFile)
      if (detectedDuration > 0) {
        musicForm.value.duration = detectedDuration
      }
    }

    if (audioFile) {
      ElMessage.success('音频上传成功，歌曲信息已自动读取')
    } else {
      ElMessage.success('封面上传成功')
    }
  } catch (error) {
    ElMessage.error(getRequestErrorMessage(error, audioFile ? '音频上传失败' : '封面上传失败'))
  } finally {
    musicUploading.value = false
  }
}

const handleMusicAudioChange = async (event) => {
  const file = event.target.files?.[0]
  if (file) {
    await uploadMusicAssets({ audioFile: file })
  }
  event.target.value = ''
}

const handleMusicCoverChange = async (event) => {
  const file = event.target.files?.[0]
  if (file) {
    await uploadMusicAssets({ coverFile: file })
  }
  event.target.value = ''
}

const handleMusicCoverPreviewLoad = () => {
  musicCoverPreviewFailed.value = false
}

const handleMusicCoverPreviewError = () => {
  musicCoverPreviewFailed.value = true
  ElMessage.warning('封面图片链接无法加载，请检查地址是否可访问')
}

const ensureEditableDraft = async (row) => {
  const payload = buildPostPayload(row, 'draft')
  const res = await adminApi.savePostDraft(row.postId, payload)
  const review = res.data || {}

  row.reviewId = review.id || row.reviewId || null
  row.reviewStatus = 'draft'
  row.reviewAction = review.action || 'update'
  row.postStatus = 'draft'
  row.reviewNote = review.reviewNote || null
  row.displayTime = review.updateTime || row.displayTime
}

const handleEditPost = async (row) => {
  if (!needsEditConfirmation(row)) {
    openPostDialog(row)
    return
  }

  try {
    await ElMessageBox.confirm('编辑后需要重新提交审核，是否继续？', '提示', {
      type: 'warning',
      confirmButtonText: '确定',
      cancelButtonText: '取消'
    })
    await ensureEditableDraft(row)
    openPostDialog(row)
  } catch (e) {
    if (e !== 'cancel' && e !== 'close') {
      ElMessage.error(getRequestErrorMessage(e, '创建编辑草稿失败'))
    }
  }
}

const openPostDialog = (row = null) => {
  const query = {}

  if (isAdmin.value) {
    if (row?.id) query.postId = row.id
    router.push({ name: 'AdminPostEditor', query })
    return
  }

  if (row?.itemType === 'review' && row?.reviewId) {
    query.reviewId = row.reviewId
  } else if (row?.reviewId) {
    query.reviewId = row.reviewId
  } else if (row?.postId) {
    query.postId = row.postId
  }

  router.push({ name: 'AdminPostEditor', query })
}

const savePost = async () => {
  if (!postFormRef.value) return
  
  let valid = false
  await postFormRef.value.validate(v => { valid = v }).catch(() => { valid = false })
  
  if (!valid) return
  
  if (!postForm.value.content) {
    ElMessage.warning('请输入文章内容')
    return
  }
  
  saving.value = true
  try {
    const draftPayload = {
      ...postForm.value,
      tags: normalizeTagInput(postForm.value.tags || ''),
      status: 'draft'
    }

    if (isAdmin.value) {
      if (editingId.value.post) await adminApi.updatePost(editingId.value.post, draftPayload)
      else await adminApi.createPost(draftPayload)
      ElMessage.success('保存成功')
    } else if (editingId.value.post) {
      await adminApi.savePostDraft(editingId.value.post, draftPayload)
      ElMessage.success('保存成功')
    } else if (editingId.value.review) {
      await adminApi.updatePostReview(editingId.value.review, draftPayload)
      ElMessage.success('保存成功')
    } else {
      await adminApi.createPostReview(draftPayload)
      ElMessage.success('保存成功')
    }
    showPostDialog.value = false
    notifyMusicListUpdated()
    await forceRefreshMenu('posts')
  } catch (e) {
    ElMessage.error(getRequestErrorMessage(e, '保存失败'))
  } finally {
    saving.value = false
  }
}
const deletePost = async (id) => {
  try {
    await ElMessageBox.confirm('确定删除？', '提示', { type: 'warning' })
    await adminApi.deletePost(id)
    ElMessage.success('删除成功')
    await forceRefreshMenu('posts')
  } catch (e) {}
}

const deletePostDraft = async (row) => {
  try {
    await ElMessageBox.confirm('确定撤回这条投稿记录吗？', '提示', { type: 'warning' })
    await adminApi.deletePostReview(row.reviewId)
    ElMessage.success('已撤回投稿')
    await forceRefreshMenu('posts')
  } catch (e) {}
}

const changePostStatus = async (row, nextStatus) => {
  const isPublish = nextStatus === 'published'
  const actionText = isAdmin.value
    ? (isPublish ? '发布' : '下线')
    : (isPublish ? getPublishActionLabel(row) : '下线')

  try {
    await ElMessageBox.confirm(`确定要${actionText}这篇文章吗？`, '提示', { type: 'warning' })

    const postId = getManagedPostId(row)
    const payload = buildPostPayload(row, nextStatus)

    if (isAdmin.value) {
      await adminApi.updatePost(postId, payload)
      ElMessage.success(isPublish ? '文章已发布' : '文章已下线')
    } else {
      if (!isPublish) {
        await adminApi.offlinePost(postId)
        row.postStatus = 'draft'
        row.reviewStatus = null
        row.reviewId = null
        row.reviewAction = null
        row.reviewNote = null
        ElMessage.success('文章已下线')
      } else if (isApprovedWorkspacePost(row)) {
        await adminApi.publishPost(postId)
        row.postStatus = 'published'
        row.reviewStatus = null
        row.reviewId = null
        row.reviewAction = null
        row.reviewNote = null
        ElMessage.success('文章已发布')
      } else if (row.itemType === 'review' && !row.postId) {
        await adminApi.submitSavedPostReview(row.reviewId)
        row.reviewStatus = 'pending'
        row.postStatus = 'published'
        ElMessage.success('已提交审核')
      } else {
        await adminApi.submitPostReview(postId, payload)
        row.reviewStatus = 'pending'
        row.postStatus = 'published'
        ElMessage.success('已提交发布审核')
      }
    }

    await forceRefreshMenu('posts')
  } catch (e) {
    if (e !== 'cancel' && e !== 'close') {
      ElMessage.error(getRequestErrorMessage(e, `${actionText}失败`))
    }
  }
}

const publishPost = async (row) => {
  await changePostStatus(row, 'published')
}

const offlinePost = async (row) => {
  await changePostStatus(row, 'draft')
}

const handlePostCreatorChange = async () => {
  resetMenuPage('posts')
  await forceRefreshMenu('posts')
}

const toggleReviewSelection = (id, checked) => {
  const nextId = String(id)
  selectedReviewIds.value = checked
    ? Array.from(new Set([...selectedReviewIds.value, nextId]))
    : selectedReviewIds.value.filter(item => item !== nextId)
}

const toggleCommentSelection = (id, checked) => {
  const nextId = String(id)
  selectedCommentIds.value = checked
    ? Array.from(new Set([...selectedCommentIds.value, nextId]))
    : selectedCommentIds.value.filter(item => item !== nextId)
}

const toggleSelectAllReviews = (checked) => {
  selectedReviewIds.value = checked ? [...visibleReviewIds.value] : []
}

const toggleSelectAllComments = (checked) => {
  selectedCommentIds.value = checked ? [...visibleCommentIds.value] : []
}

function stopModerationTaskPolling() {
  if (moderationTaskTimer) {
    clearTimeout(moderationTaskTimer)
    moderationTaskTimer = null
  }
}

function stopModerationTaskAutoClose() {
  if (moderationTaskCloseTimer) {
    clearTimeout(moderationTaskCloseTimer)
    moderationTaskCloseTimer = null
  }
}

function clearModerationTaskDisplay() {
  stopModerationTaskPolling()
  stopModerationTaskAutoClose()
  moderationTask.value = null
}

function scheduleModerationTaskAutoClose() {
  stopModerationTaskAutoClose()
  moderationTaskCloseTimer = setTimeout(() => {
    moderationTask.value = null
    moderationTaskCloseTimer = null
  }, 3000)
}

const isModerationBusy = (menuKey) => (
  moderationTask.value?.menuKey === menuKey
  && ['queued', 'running'].includes(String(moderationTask.value?.status || ''))
)

const refreshAfterModerationTask = async (menuKey) => {
  if (menuKey === 'reviews') {
    selectedReviewIds.value = []
    await forceRefreshMenu('reviews')
    return
  }

  if (menuKey === 'comments') {
    selectedCommentIds.value = []
    await Promise.all([forceRefreshMenu('comments'), forceRefreshMenu('reports')])
  }
}

const startModerationTaskPolling = (task, menuKey) => {
  if (!task?.taskId) {
    return
  }

  clearModerationTaskDisplay()
  moderationTask.value = {
    ...task,
    menuKey
  }

  const poll = async () => {
    try {
      const res = await adminApi.getModerationTask(task.taskId)
      moderationTask.value = {
        ...(res.data || task),
        menuKey
      }

      if (['completed', 'failed'].includes(String(moderationTask.value?.status || ''))) {
        if (moderationTask.value.failureCount) {
          ElMessage.warning(moderationTask.value.message || '批量任务已完成，但有部分记录处理失败')
        } else {
          ElMessage.success(moderationTask.value.message || '批量任务已完成')
        }
        await refreshAfterModerationTask(menuKey)
        scheduleModerationTaskAutoClose()
        return
      }
    } catch (error) {
      ElMessage.error(getRequestErrorMessage(error, '批量任务状态获取失败'))
      return
    }

    moderationTaskTimer = setTimeout(poll, 1200)
  }

  moderationTaskTimer = setTimeout(poll, 1200)
}

const requestBatchReviewNote = async (title, defaultValue = '') => {
  const { value } = await ElMessageBox.prompt('请填写驳回原因，处理结果会回写到对应记录中。', title, {
    confirmButtonText: '确认',
    cancelButtonText: '取消',
    inputType: 'textarea',
    inputValue: defaultValue,
    inputValidator: (val) => !!val?.trim() || '请填写驳回原因'
  })
  return value.trim()
}

const submitReviewBatch = async (operation) => {
  if (!selectedReviewIds.value.length) return
  try {
    let reviewNote = ''
    if (operation === 'reject') {
      reviewNote = await requestBatchReviewNote('批量驳回文章')
    } else {
      const actionText = operation === 'approve' ? '批量通过' : '批量删除'
      await ElMessageBox.confirm(`确定要${actionText}选中的 ${selectedReviewIds.value.length} 条文章审核记录吗？`, '提示', { type: 'warning' })
    }

    const res = await adminApi.submitPostReviewBatch(operation, {
      ids: selectedReviewIds.value.map(id => Number(id)),
      reviewNote
    })
    startModerationTaskPolling(res.data || {}, 'reviews')
    ElMessage.success('批量任务已提交，正在后台处理')
  } catch (error) {
    if (error !== 'cancel' && error !== 'close') {
      ElMessage.error(getRequestErrorMessage(error, '批量操作失败'))
    }
  }
}

const submitCommentBatch = async (operation) => {
  if (!selectedCommentIds.value.length) return
  try {
    let reviewNote = ''
    if (operation === 'reject') {
      reviewNote = await requestBatchReviewNote('批量驳回评论')
    } else if (operation === 'delete') {
      await ElMessageBox.confirm(`确定要批量删除选中的 ${selectedCommentIds.value.length} 条评论吗？若存在回复将一并删除。`, '提示', { type: 'warning' })
    } else {
      await ElMessageBox.confirm(`确定要批量通过选中的 ${selectedCommentIds.value.length} 条评论吗？`, '提示', { type: 'warning' })
    }

    const res = await adminApi.submitCommentBatch(operation, {
      ids: selectedCommentIds.value.map(id => Number(id)),
      reviewNote
    })
    startModerationTaskPolling(res.data || {}, 'comments')
    ElMessage.success('批量任务已提交，正在后台处理')
  } catch (error) {
    if (error !== 'cancel' && error !== 'close') {
      ElMessage.error(getRequestErrorMessage(error, '批量操作失败'))
    }
  }
}

const openReviewDialog = (row) => {
  selectedReview.value = row
  showReviewDialog.value = true
}

const openCommentDialog = (row) => {
  selectedComment.value = { ...row }
  showCommentDialog.value = true
}

const openReportDialog = (row) => {
  selectedReport.value = { ...row }
  showReportDialog.value = true
}

const openContactDialog = (row) => {
  selectedContact.value = { ...row }
  contactReplyForm.value = { replyContent: row?.replyContent || '' }
  showContactDialog.value = true
}

const approvePostReview = async (id) => {
  try {
    await ElMessageBox.confirm('确认通过这条文章审核吗？', '提示', { type: 'warning' })
    await adminApi.approvePostReview(id)
    ElMessage.success('审核已通过')
    showReviewDialog.value = false
    await forceRefreshMenu('reviews')
  } catch (e) {}
}

const rejectPostReview = async (row) => {
  try {
    const { value } = await ElMessageBox.prompt('请填写驳回原因，作者会在文章审核备注中看到它。', '驳回文章', {
      confirmButtonText: '确认驳回',
      cancelButtonText: '取消',
      inputType: 'textarea',
      inputValue: row?.reviewNote || '',
      inputValidator: (val) => !!val?.trim() || '请填写驳回原因'
    })
    await adminApi.rejectPostReview(row.id, { reviewNote: value.trim() })
    ElMessage.success('已驳回该投稿')
    if (selectedReview.value?.id === row.id) {
      showReviewDialog.value = false
    }
    await forceRefreshMenu('reviews')
  } catch (e) {}
}

const approveComment = async (id) => {
  if (!id) return
  try {
    await ElMessageBox.confirm('确认通过这条评论审核吗？', '提示', { type: 'warning' })
    const res = await adminApi.approveComment(id)
    const updated = res.data || null
    if (updated) {
      adminComments.value = adminComments.value.map(item => (item.id === id ? updated : item))
      if (selectedComment.value?.id === id) {
        selectedComment.value = updated
      }
    }
    ElMessage.success('评论已审核通过')
    await forceRefreshMenu('comments')
  } catch (e) {}
}

const rejectComment = async (row) => {
  if (!row?.id) return
  try {
    const { value } = await ElMessageBox.prompt('请填写驳回原因，评论作者后续可据此调整内容。', '驳回评论', {
      confirmButtonText: '确认驳回',
      cancelButtonText: '取消',
      inputType: 'textarea',
      inputValue: row?.reviewNote || '',
      inputValidator: (val) => !!val?.trim() || '请填写驳回原因'
    })
    const res = await adminApi.rejectComment(row.id, { reviewNote: value.trim() })
    const updated = res.data || null
    if (updated) {
      adminComments.value = adminComments.value.map(item => (item.id === row.id ? updated : item))
      if (selectedComment.value?.id === row.id) {
        selectedComment.value = updated
      }
    }
    ElMessage.success('已驳回该评论')
    await forceRefreshMenu('comments')
  } catch (e) {}
}

const deleteComment = async (id) => {
  if (!id) return
  try {
    await ElMessageBox.confirm('确定删除这条评论吗？如果它下面有回复，会一并删除。', '提示', { type: 'warning' })
    await adminApi.deleteComment(id)
    adminComments.value = adminComments.value.filter(item => item.id !== id && item.parentId !== id)
    if (selectedComment.value?.id === id) {
      showCommentDialog.value = false
      selectedComment.value = null
    }
    ElMessage.success('评论已删除')
    await Promise.all([forceRefreshMenu('comments'), forceRefreshMenu('reports')])
  } catch (e) {}
}

const resolveCommentReport = async (row, action) => {
  if (!row?.id) return

  try {
    let resolutionNote = ''
    if (action === 'kept') {
      const prompt = await ElMessageBox.prompt('可以补充说明为什么保留该评论，留空则直接处理。', '保留评论', {
        confirmButtonText: '确认保留',
        cancelButtonText: '取消',
        inputType: 'textarea',
        inputValue: row?.resolutionNote || '',
        inputValidator: () => true
      })
      resolutionNote = String(prompt.value || '').trim()
    } else {
      const prompt = await ElMessageBox.prompt('可以补充删除说明，留空则使用默认备注。', '删除评论', {
        confirmButtonText: '确认删除',
        cancelButtonText: '取消',
        inputType: 'textarea',
        inputValue: row?.resolutionNote || '',
        inputValidator: () => true
      })
      resolutionNote = String(prompt.value || '').trim()
    }

    const res = await adminApi.resolveCommentReport(row.id, {
      action,
      resolutionNote
    })
    selectedReport.value = res.data || selectedReport.value
    ElMessage.success(action === 'kept' ? '该举报已标记为保留评论' : '该举报已按删除评论处理')
    await Promise.all([forceRefreshMenu('comments'), forceRefreshMenu('reports')])
    if (selectedComment.value?.id && action === 'deleted') {
      const stillExists = adminComments.value.find(item => String(item.id) === String(selectedComment.value.id))
      if (!stillExists) {
        showCommentDialog.value = false
        selectedComment.value = null
      }
    }
  } catch (error) {
    if (error !== 'cancel' && error !== 'close') {
      ElMessage.error(getRequestErrorMessage(error, '举报处理失败'))
    }
  }
}

// 音乐
const openMusicDialog = (row = null) => {
  if (!canManageMusic.value) {
    ElMessage.warning('只有站长可以管理音乐')
    return
  }
  editingId.value.music = row?.id || null
  musicForm.value = row ? { ...createEmptyMusicForm(), ...row } : createEmptyMusicForm()
  showMusicDialog.value = true
}
const notifyMusicListUpdated = () => {
  window.dispatchEvent(new Event(MUSIC_LIST_UPDATED_EVENT))
  localStorage.setItem(MUSIC_LIST_UPDATED_STORAGE_KEY, String(Date.now()))
}
const saveMusic = async () => {
  if (!canManageMusic.value) { ElMessage.warning('只有站长可以管理音乐'); return }
  if (!musicForm.value.url) { ElMessage.warning('请先上传音频文件'); return }
  if (!musicForm.value.title) { ElMessage.warning('请填写歌曲标题'); return }
  saving.value = true
  try {
    if (editingId.value.music) await adminApi.updateMusic(editingId.value.music, musicForm.value)
    else await adminApi.createMusic(musicForm.value)
    ElMessage.success('保存成功')
    notifyMusicListUpdated()
    showMusicDialog.value = false
    await forceRefreshMenu('music')
  } catch (e) { ElMessage.error(getRequestErrorMessage(e, '保存失败')) }
  finally { saving.value = false }
}
const deleteMusic = async (id) => {
  if (!canManageMusic.value) { ElMessage.warning('只有站长可以管理音乐'); return }
  try {
    await ElMessageBox.confirm('确定删除？', '提示', { type: 'warning' })
    await adminApi.deleteMusic(id)
    notifyMusicListUpdated()
    ElMessage.success('删除成功')
    await forceRefreshMenu('music')
  } catch (e) {}
}

// 用户
const openUserDialog = (row = null) => {
  if (row && isProtectedStationMasterAccount(row) && !isStationMaster.value) {
    ElMessage.warning('管理员不可编辑站长账号')
    return
  }
  if (row && isManagerAccount(row) && !isStationMaster.value) {
    ElMessage.warning('只有站长可以编辑管理员账号')
    return
  }
  editingId.value.user = row?.id || null
  userForm.value = row
    ? { ...row, password: '', role: normalizeManagedUserRole(row.role) }
    : { username: '', nickname: '', email: '', password: '', role: 'user' }
  showUserDialog.value = true
}
const saveUser = async () => {
  if (!editingId.value.user) {
    const passwordMessage = getPasswordValidationMessage(userForm.value.password)
    if (passwordMessage) { ElMessage.warning(passwordMessage); return }
  }
  if (!userForm.value.username || !userForm.value.email) { ElMessage.warning('请填写用户名和邮箱'); return }
  if (!editingId.value.user && !userForm.value.password) { ElMessage.warning('请填写密码'); return }
  const isProtectedUser = isProtectedStationMasterAccount(userForm.value)
  const targetRole = isProtectedUser ? 'admin' : normalizeManagedUserRole(userForm.value.role)
  if (targetRole === 'admin' && !isProtectedUser) {
    ElMessage.warning('站长角色唯一，不可分配给其他账号')
    return
  }
  if (targetRole === 'manager' && !isStationMaster.value) {
    ElMessage.warning('只有站长可以设置管理员')
    return
  }
  saving.value = true
  try {
    const payload = { ...userForm.value, role: targetRole }
    if (editingId.value.user) await adminApi.updateUser(editingId.value.user, payload)
    else await adminApi.createUser(payload)
    ElMessage.success('保存成功')
    showUserDialog.value = false
    await forceRefreshMenu('users')
  } catch (e) { ElMessage.error(getRequestErrorMessage(e, '保存失败')) }
  finally { saving.value = false }
}
const deleteUser = async (id) => {
  try {
    await ElMessageBox.confirm('确定删除？', '提示', { type: 'warning' })
    await adminApi.deleteUser(id)
    ElMessage.success('删除成功')
    await forceRefreshMenu('users')
  } catch (e) {}
}

// 友链
const openLinkDialog = (row = null) => {
  editingId.value.link = row?.id || null
  linkForm.value = row ? { ...row } : { name: '', url: '', description: '' }
  showLinkDialog.value = true
}
const saveLink = async () => {
  if (!linkForm.value.name || !linkForm.value.url) { ElMessage.warning('请填写名称和链接'); return }
  saving.value = true
  try {
    if (editingId.value.link) await adminApi.updateFriendLink(editingId.value.link, linkForm.value)
    else await adminApi.createFriendLink(linkForm.value)
    ElMessage.success('保存成功')
    showLinkDialog.value = false
    await forceRefreshMenu('links')
    notifyLinksUpdated()
  } catch (e) { ElMessage.error(getRequestErrorMessage(e, '保存失败')) }
  finally { saving.value = false }
}
const deleteLink = async (id) => {
  try {
    await ElMessageBox.confirm('确定删除？', '提示', { type: 'warning' })
    await adminApi.deleteFriendLink(id)
    ElMessage.success('删除成功')
    await forceRefreshMenu('links')
    notifyLinksUpdated()
  } catch (e) {}
}
const approveLink = async (id) => {
  try {
    await adminApi.approveFriendLink(id)
    ElMessage.success('审核通过')
    await forceRefreshMenu('links')
    notifyLinksUpdated()
  } catch (e) { ElMessage.error(getRequestErrorMessage(e, '审核失败')) }
}

const sendContactReply = async () => {
  if (!selectedContact.value?.id) return

  const replyContent = contactReplyForm.value.replyContent?.trim()
  if (!replyContent) {
    ElMessage.warning('请输入回复内容')
    return
  }

  sendingContactReply.value = true
  try {
    const res = await adminApi.replyContactMessage(selectedContact.value.id, replyContent)
    selectedContact.value = res.data || {
      ...selectedContact.value,
      replyContent,
      status: 1,
      repliedTime: new Date().toISOString()
    }
    contactReplyForm.value.replyContent = selectedContact.value.replyContent || replyContent
    ElMessage.success('回复邮件已发送')
    await forceRefreshMenu('contacts')
  } catch (e) {
    ElMessage.error(getRequestErrorMessage(e, '回复发送失败'))
  } finally {
    sendingContactReply.value = false
  }
}

const deleteContactMessage = async (id) => {
  try {
    await ElMessageBox.confirm('确定删除这条联系消息吗？', '提示', { type: 'warning' })
    await adminApi.deleteContactMessage(id)
    ElMessage.success('删除成功')
    await forceRefreshMenu('contacts')
  } catch (e) {}
}

onBeforeUnmount(() => {
  clearModerationTaskDisplay()
})

onMounted(async () => {
  // 初始化主题
  themeStore.initTheme()
  
  if (!userStore.checkSession()) {
    ElMessage.error('请先登录')
    router.replace('/login')
    return
  }
  await syncProfile()
  initProfileForm()
  await initializeMenuState()
})
</script>

<style scoped>
/* ===== 布局基础 ===== */
.admin-layout {
  display: flex;
  min-height: 100vh;
  background: var(--bg-primary);
}

/* ===== 侧边栏 ===== */
.sidebar {
  width: 240px;
  background: var(--bg-card);
  border-right: 1px solid var(--border-color);
  display: flex;
  flex-direction: column;
  position: fixed;
  left: 0;
  top: 0;
  bottom: 0;
  z-index: 100;
}

.sidebar-header {
  padding: 20px;
  display: flex;
  align-items: center;
  gap: 12px;
  border-bottom: 1px solid var(--border-color);
}

.logo-icon {
  width: 40px;
  height: 40px;
  border-radius: 12px;
  object-fit: cover;
}

.logo-text {
  font-size: 1.15rem;
  font-weight: 700;
  color: transparent;
  background: var(--gradient-primary);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.sidebar-nav {
  flex: 1;
  padding: 16px 12px;
  overflow-y: auto;
}

.nav-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 14px 16px;
  border-radius: 12px;
  cursor: pointer;
  color: var(--text-secondary);
  transition: all 0.2s ease;
  margin-bottom: 4px;
  user-select: none;
}

.nav-item:hover {
  background: var(--bg-tertiary);
  color: var(--text-primary);
}

.nav-item.active {
  background: var(--accent);
  color: #fff;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
}

.nav-item.logout:hover {
  background: rgba(255, 77, 79, 0.1);
  color: #ff4d4f;
}

.nav-icon {
  width: 24px;
  height: 24px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
}

.nav-icon-svg {
  width: 18px;
  height: 18px;
}

.nav-label {
  font-size: 0.95rem;
  font-weight: 500;
}

.sidebar-footer {
  padding: 12px;
  border-top: 1px solid var(--border-color);
}

/* ===== 主内容区域 ===== */
.main-wrap {
  flex: 1;
  margin-left: 240px;
  display: flex;
  flex-direction: column;
  min-height: 100vh;
}

.topbar {
  height: 64px;
  background: var(--bg-card);
  border-bottom: 1px solid var(--border-color);
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 24px;
  position: sticky;
  top: 0;
  z-index: 50;
}

.page-title {
  font-size: 1.3rem;
  font-weight: 600;
  color: var(--text-primary);
}

.topbar-actions {
  display: flex;
  align-items: center;
  gap: 16px;
}

/* 主题切换按钮 */
.theme-toggle {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 16px;
  background: var(--bg-tertiary);
  border-radius: 20px;
  cursor: pointer;
  transition: all 0.2s;
}

.theme-toggle:hover {
  background: var(--accent-light);
}

.theme-icon {
  font-size: 1.1rem;
}

.theme-label {
  font-size: 0.85rem;
  color: var(--text-secondary);
  font-weight: 500;
}

.user-info {
  display: flex;
  align-items: center;
  gap: 12px;
}

.admin-badge {
  background: var(--gradient-primary);
  color: #fff;
  padding: 6px 14px;
  border-radius: 14px;
  font-size: 0.8rem;
  font-weight: 600;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.admin-badge.user {
  background: linear-gradient(135deg, #36cfc9, #13c2c2);
}

.username {
  color: var(--text-secondary);
  font-weight: 500;
}

.content-area {
  flex: 1;
  padding: 24px;
  overflow-y: auto;
}

/* ===== 统计卡片 ===== */
.stats-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
  gap: 20px;
  margin-bottom: 24px;
}

.stat-card {
  background: var(--bg-card);
  border: 1px solid var(--border-color);
  border-radius: 16px;
  padding: 24px;
  display: flex;
  align-items: center;
  gap: 16px;
  transition: all 0.3s ease;
}

.stat-card:hover {
  transform: translateY(-2px);
  box-shadow: var(--shadow-md);
}

.stat-icon {
  width: 60px;
  height: 60px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--accent-light);
  border-radius: 16px;
  color: var(--accent);
}

.stat-icon-svg {
  width: 28px;
  height: 28px;
}

.stat-value {
  font-size: 1.8rem;
  font-weight: 700;
  color: var(--text-primary);
  line-height: 1.2;
}

.stat-label {
  color: var(--text-tertiary);
  font-size: 0.9rem;
  margin-top: 4px;
}

.welcome-card {
  background: var(--bg-card);
  border: 1px solid var(--border-color);
  border-radius: 16px;
  padding: 40px;
  text-align: center;
}

.welcome-card h2 {
  color: var(--text-primary);
  margin-bottom: 12px;
  font-size: 1.5rem;
}

.welcome-card p {
  color: var(--text-secondary);
}

/* ===== 工具栏 ===== */
.toolbar {
  margin-bottom: 20px;
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
  justify-content: flex-end;
}

.toolbar-filters {
  margin-right: auto;
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}

.toolbar-filter-label {
  color: var(--text-secondary);
  font-size: 0.9rem;
  font-weight: 500;
}

.toolbar-select {
  width: 220px;
}

.review-toolbar {
  justify-content: space-between;
  align-items: center;
  gap: 16px;
}

.admin-comment-toolbar {
  align-items: flex-start;
}

.comment-toolbar-filters {
  display: flex;
  flex: 1;
  align-items: center;
  justify-content: center;
  gap: 12px;
  flex-wrap: wrap;
}

.review-toolbar-stats {
  display: flex;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: 10px;
}

.toolbar-search {
  width: min(360px, 100%);
}

.review-filter-chip {
  border: 1px solid transparent;
  cursor: pointer;
  transition: all 0.2s ease;
}

.review-filter-chip.active {
  transform: translateY(-1px);
  box-shadow: 0 8px 20px rgba(0, 0, 0, 0.12);
  border-color: currentColor;
}

.moderation-bulk-bar {
  margin-bottom: 16px;
  padding: 14px 16px;
  border: 1px solid var(--border-color);
  border-radius: 16px;
  background: var(--bg-card);
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  flex-wrap: wrap;
}

.moderation-bulk-copy {
  color: var(--text-secondary);
  font-weight: 600;
}

.moderation-bulk-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.moderation-task-card {
  margin-bottom: 16px;
  padding: 16px 18px;
  border-radius: 16px;
  border: 1px solid var(--border-color);
  background: var(--bg-card);
  display: grid;
  gap: 10px;
}

.moderation-task-head,
.moderation-task-meta {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  flex-wrap: wrap;
}

.moderation-task-desc,
.moderation-task-meta {
  color: var(--text-secondary);
  font-size: 0.9rem;
}

.moderation-task-progress {
  height: 10px;
  border-radius: 999px;
  overflow: hidden;
  background: var(--bg-tertiary);
}

.moderation-task-progress-bar {
  height: 100%;
  border-radius: inherit;
  background: linear-gradient(90deg, #36cfc9, #1677ff);
  transition: width 0.2s ease;
}

.review-empty-row + tr {
  display: none;
}

.review-table-stage {
  transform-origin: top center;
}

.review-switch-enter-active,
.review-switch-leave-active {
  transition: opacity 0.24s ease, transform 0.24s ease, filter 0.24s ease;
}

.review-switch-enter-from,
.review-switch-leave-to {
  opacity: 0;
  transform: translateY(10px) scale(0.985);
  filter: blur(3px);
}

.review-toolbar-copy {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.review-toolbar-title {
  font-size: 1.05rem;
  font-weight: 700;
  color: var(--text-primary);
}

.review-toolbar-desc {
  color: var(--text-secondary);
  font-size: 0.9rem;
}

/* ===== 表格容器 ===== */
.table-container {
  background: var(--bg-card);
  border: 1px solid var(--border-color);
  border-radius: 16px;
  overflow: auto;
  max-height: calc(100vh - 200px);
}

.post-card-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(420px, 1fr));
  gap: 18px;
}

.module-loading-card {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 12px;
  min-height: 240px;
  padding: 36px 24px;
  border-radius: 24px;
  border: 1px dashed color-mix(in srgb, var(--accent) 26%, var(--border-color));
  background: linear-gradient(180deg, color-mix(in srgb, var(--accent-light) 68%, var(--bg-card)), var(--bg-card));
  text-align: center;
}

.table-loading-card {
  min-height: 320px;
}

.module-loading-spinner {
  width: 38px;
  height: 38px;
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

.module-loading-title {
  color: var(--text-primary);
  font-size: 1.05rem;
  font-weight: 700;
}

.module-loading-desc {
  color: var(--text-secondary);
  line-height: 1.75;
}

.post-empty-card {
  border: 1px dashed color-mix(in srgb, var(--accent) 28%, var(--border-color));
  border-radius: 24px;
  background: linear-gradient(180deg, color-mix(in srgb, var(--accent-light) 72%, var(--bg-card)), var(--bg-card));
  padding: 54px 24px;
  text-align: center;
}

.post-empty-title {
  color: var(--text-primary);
  font-size: 1.2rem;
  font-weight: 700;
  margin-bottom: 10px;
}

.post-empty-desc {
  color: var(--text-secondary);
  line-height: 1.8;
}

.post-manage-card {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 168px;
  gap: 18px;
  padding: 18px;
  border: 1px solid var(--border-color);
  border-radius: 24px;
  background: color-mix(in srgb, var(--bg-card) 96%, transparent);
  box-shadow: var(--shadow-sm);
  transition: transform 0.22s ease, box-shadow 0.22s ease, border-color 0.22s ease;
}

.post-manage-card:hover {
  transform: translateY(-3px);
  box-shadow: var(--shadow-md);
  border-color: color-mix(in srgb, var(--accent) 32%, var(--border-color));
}

.post-manage-card.published {
  background: color-mix(in srgb, var(--accent-light) 24%, var(--bg-card));
}

.post-manage-main {
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.post-card-topline {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 12px;
}

.post-card-category {
  display: inline-flex;
  align-items: center;
  max-width: fit-content;
  padding: 6px 10px;
  border-radius: 999px;
  background: var(--accent-light);
  color: var(--accent);
  font-size: 0.82rem;
  font-weight: 700;
}

.post-card-time {
  color: var(--text-tertiary);
  font-size: 0.86rem;
  white-space: nowrap;
}

.post-card-title {
  margin: 0 0 10px;
  color: var(--text-primary);
  font-size: 1.12rem;
  line-height: 1.5;
}

.post-card-summary {
  margin: 0;
  color: var(--text-secondary);
  line-height: 1.75;
  display: -webkit-box;
  -webkit-line-clamp: 3;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.post-card-statuses {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-top: 16px;
}

.post-card-note {
  margin-top: 14px;
  padding: 12px 14px;
  border-radius: 16px;
  background: color-mix(in srgb, var(--bg-tertiary) 75%, transparent);
  display: grid;
  gap: 6px;
}

.post-card-note-label {
  color: var(--text-tertiary);
  font-size: 0.8rem;
  font-weight: 700;
}

.post-card-note-text {
  color: var(--text-secondary);
  line-height: 1.65;
  word-break: break-word;
}

.post-card-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: auto;
  padding-top: 18px;
}

.post-card-actions .btn-edit,
.post-card-actions .btn-del,
.post-card-actions .btn-approve,
.post-card-actions .btn-view,
.post-card-actions .btn-reject {
  margin-right: 0;
}

.post-manage-cover {
  position: relative;
  min-height: 188px;
  border-radius: 20px;
  overflow: hidden;
  border: 1px solid var(--border-color);
  background: linear-gradient(135deg, color-mix(in srgb, var(--accent-light) 90%, var(--bg-card)), var(--bg-card));
}

.post-manage-cover img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}

.post-manage-cover.empty::after {
  content: '';
  position: absolute;
  inset: 0;
  background: linear-gradient(180deg, transparent, rgba(0, 0, 0, 0.18));
}

.post-manage-cover-placeholder {
  position: absolute;
  inset: auto 0 0 0;
  z-index: 1;
  padding: 18px 14px 14px;
  color: var(--text-primary);
  font-weight: 700;
  line-height: 1.5;
}

/* ===== 数据表格 ===== */
.data-table {
  width: 100%;
  border-collapse: collapse;
  min-width: 800px;
}

.data-table thead {
  position: sticky;
  top: 0;
  z-index: 10;
}

.data-table th {
  background: var(--bg-tertiary);
  padding: 16px;
  text-align: left;
  font-weight: 600;
  color: var(--text-secondary);
  font-size: 0.85rem;
  white-space: nowrap;
  border-bottom: 1px solid var(--border-color);
}

.data-table td {
  padding: 16px;
  border-bottom: 1px solid var(--border-light);
  color: var(--text-primary);
  vertical-align: middle;
}

.data-table th.col-select,
.data-table td.col-select {
  text-align: center;
}

.data-table tbody tr {
  transition: background 0.2s;
}

.data-table tbody tr:hover {
  background: var(--bg-tertiary);
}

/* 列宽度定义 */
.col-id { width: 60px; text-align: center; }
.col-select { width: 56px; text-align: center; }
.col-title { min-width: 200px; }
.col-category { width: 100px; }
.col-status { width: 90px; text-align: center; }
.col-role { width: 90px; text-align: center; }
.col-time { width: 120px; }
.col-cover { width: 124px; }
.col-actions { width: 180px; }
.col-username { width: 120px; }
.col-nickname { width: 120px; }
.col-email { min-width: 180px; }
.col-artist { width: 130px; }
.col-album { width: 150px; }
.col-name { width: 150px; }
.col-url { min-width: 200px; }
.col-subject { min-width: 180px; }
.col-message { min-width: 280px; }
.col-desc { min-width: 150px; }

.message-cell {
  max-width: 420px;
  white-space: pre-wrap;
  word-break: break-word;
  line-height: 1.6;
}

.empty-row {
  text-align: center;
  color: var(--text-tertiary);
  padding: 60px 20px;
  font-size: 1rem;
}

.post-title-cell {
  display: grid;
  gap: 6px;
}

.post-title-main {
  color: var(--text-primary);
  font-weight: 700;
  line-height: 1.5;
}

.post-title-sub {
  color: var(--text-secondary);
  font-size: 0.88rem;
  line-height: 1.6;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.post-cover-cell {
  width: 92px;
  height: 64px;
  border-radius: 14px;
  overflow: hidden;
  border: 1px solid var(--border-color);
  background: color-mix(in srgb, var(--accent-light) 60%, var(--bg-card));
  display: grid;
  place-items: center;
  color: var(--text-tertiary);
  font-size: 0.82rem;
}

.post-cover-cell img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}

/* ===== 徽章 ===== */
.badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  padding: 5px 12px;
  border-radius: 12px;
  font-size: 0.8rem;
  font-weight: 600;
  white-space: nowrap;
}

.badge-success {
  background: rgba(82, 196, 26, 0.15);
  color: #52c41a;
}

.badge-warning {
  background: rgba(250, 173, 20, 0.15);
  color: #faad14;
}

.badge-danger {
  background: rgba(255, 77, 79, 0.15);
  color: #ff4d4f;
}

.badge-default {
  background: var(--bg-tertiary);
  color: var(--text-tertiary);
}

.badge-info {
  background: rgba(54, 207, 201, 0.15);
  color: #13c2c2;
}

/* ===== 操作按钮 ===== */
.col-actions {
  white-space: nowrap;
}

.btn-edit,
.btn-del,
.btn-approve,
.btn-view,
.btn-reject {
  padding: 8px 14px;
  border: none;
  border-radius: 8px;
  font-size: 0.85rem;
  font-weight: 500;
  cursor: pointer;
  margin-right: 6px;
  transition: all 0.2s ease;
}

.btn-edit {
  background: var(--accent-light);
  color: var(--accent);
}

.btn-edit:hover {
  background: var(--accent);
  color: #fff;
}

.btn-edit:disabled {
  opacity: 0.5;
  cursor: not-allowed;
  background: var(--accent-light);
  color: var(--accent);
}

.btn-edit:disabled:hover {
  background: var(--accent-light);
  color: var(--accent);
}

.btn-del {
  background: rgba(255, 77, 79, 0.1);
  color: #ff4d4f;
}

.btn-del:hover {
  background: #ff4d4f;
  color: #fff;
}

.btn-approve {
  background: rgba(82, 196, 26, 0.15);
  color: #52c41a;
}

.btn-approve:hover {
  background: #52c41a;
  color: #fff;
}

.btn-view {
  background: rgba(24, 144, 255, 0.12);
  color: #1677ff;
}

.btn-view:hover {
  background: #1677ff;
  color: #fff;
}

.btn-reject {
  background: rgba(255, 77, 79, 0.1);
  color: #ff4d4f;
}

.btn-reject:hover {
  background: #ff4d4f;
  color: #fff;
}

.btn-approve:disabled,
.btn-reject:disabled,
.btn-view:disabled,
.btn-del:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.btn-approve:disabled:hover {
  background: rgba(82, 196, 26, 0.15);
  color: #52c41a;
}

.btn-reject:disabled:hover,
.btn-del:disabled:hover {
  background: rgba(255, 77, 79, 0.1);
  color: #ff4d4f;
}

.btn-view:disabled:hover {
  background: rgba(24, 144, 255, 0.12);
  color: #1677ff;
}

.post-row-published .btn-edit,
.post-row-published .btn-del {
  display: none;
}

.post-manage-card.published .btn-edit,
.post-manage-card.published .btn-del {
  display: none;
}

.link-url {
  color: var(--accent);
  text-decoration: none;
  word-break: break-all;
}

.link-url:hover {
  text-decoration: underline;
}

/* ===== 个人信息页面 ===== */
.profile-container {
  max-width: 900px;
}

.profile-avatar-section {
  display: flex;
  align-items: center;
  gap: 24px;
  margin-bottom: 32px;
  padding: 24px;
  background: var(--bg-card);
  border: 1px solid var(--border-color);
  border-radius: 16px;
}

.avatar-upload {
  width: 100px;
  height: 100px;
  border-radius: 50%;
  position: relative;
  cursor: pointer;
  overflow: hidden;
  background: var(--bg-tertiary);
}

.avatar-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.avatar-placeholder {
  width: 100%;
  height: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  color: var(--text-tertiary);
}

.avatar-placeholder-icon {
  width: 36px;
  height: 36px;
  color: currentColor;
}

.placeholder-text {
  font-size: 0.7rem;
  margin-top: 4px;
}

.avatar-hover-mask {
  position: absolute;
  inset: 0;
  background: rgba(0, 0, 0, 0.6);
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-size: 0.85rem;
  opacity: 0;
  transition: opacity 0.2s;
}

.avatar-upload:hover .avatar-hover-mask {
  opacity: 1;
}

.profile-basic-info {
  flex: 1;
}

.profile-username {
  font-size: 1.5rem;
  font-weight: 700;
  color: var(--text-primary);
  margin-bottom: 8px;
}

.profile-role {
  margin-bottom: 8px;
}

.role-badge {
  padding: 4px 12px;
  border-radius: 12px;
  font-size: 0.8rem;
  font-weight: 600;
}

.role-badge.admin {
  background: rgba(255, 77, 79, 0.1);
  color: #ff4d4f;
}

.role-badge.user {
  background: rgba(79, 212, 204, 0.1);
  color: #39c5bb;
}

.profile-join-time {
  color: var(--text-tertiary);
  font-size: 0.9rem;
}

.profile-forms {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.form-section {
  background: var(--bg-card);
  border: 1px solid var(--border-color);
  border-radius: 16px;
  padding: 24px;
}

.section-title {
  font-size: 1.1rem;
  font-weight: 600;
  color: var(--text-primary);
  margin-bottom: 20px;
  padding-bottom: 12px;
  border-bottom: 1px solid var(--border-light);
}

.form-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 16px;
}

.form-item {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.form-item.full-width {
  grid-column: 1 / -1;
}

.form-item label {
  font-size: 0.9rem;
  font-weight: 500;
  color: var(--text-secondary);
}

.form-input,
.form-textarea {
  padding: 12px 16px;
  border: 1px solid var(--border-color);
  border-radius: 10px;
  background: var(--bg-primary);
  color: var(--text-primary);
  font-size: 0.95rem;
  transition: all 0.2s;
}

.form-input:focus,
.form-textarea:focus {
  outline: none;
  border-color: var(--accent);
  box-shadow: 0 0 0 3px var(--accent-light);
}

.form-textarea {
  resize: vertical;
  min-height: 80px;
}

.form-actions {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
}

/* 邮箱管理 */
.email-current {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 16px;
  background: var(--bg-tertiary);
  border-radius: 12px;
  margin-bottom: 20px;
}

.email-current .label {
  color: var(--text-secondary);
  font-size: 0.9rem;
}

.email-current .value {
  color: var(--text-primary);
  font-weight: 600;
}

.verified-badge {
  padding: 4px 10px;
  background: rgba(82, 196, 26, 0.15);
  color: #52c41a;
  border-radius: 10px;
  font-size: 0.8rem;
  font-weight: 500;
}

.unverified-badge {
  padding: 4px 10px;
  background: rgba(250, 173, 20, 0.15);
  color: #faad14;
  border-radius: 10px;
  font-size: 0.8rem;
  font-weight: 500;
}

.email-change-form {
  margin-top: 16px;
}

.code-input-group {
  display: flex;
  align-items: center;
  gap: 12px;
}

.code-input {
  flex: 1;
  min-height: 46px;
}

.code-input-group .el-button {
  white-space: nowrap;
  min-width: 100px;
  height: 46px;
  align-self: center;
}

:global(.email-code-btn.el-button) {
  --el-button-bg-color: #fff3e2;
  --el-button-border-color: #f3c894;
  --el-button-text-color: #9a5b13;
  --el-button-hover-bg-color: #ffe7c7;
  --el-button-hover-border-color: #e8ae63;
  --el-button-hover-text-color: #7c4306;
  --el-button-disabled-bg-color: #f9efe2;
  --el-button-disabled-border-color: #ead3b3;
  --el-button-disabled-text-color: #b48a55;
  background-color: #fff3e2 !important;
  border-color: #f3c894 !important;
  color: #9a5b13 !important;
  box-shadow: none !important;
}

:global(.email-code-btn.el-button:hover:not(:disabled)) {
  background-color: #ffe7c7 !important;
  border-color: #e8ae63 !important;
  color: #7c4306 !important;
}

:global(.email-code-btn.el-button.is-disabled),
:global(.email-code-btn.el-button.is-disabled:hover),
:global(.email-code-btn.el-button:disabled),
:global(.email-code-btn.el-button:disabled:hover) {
  background-color: #f9efe2 !important;
  border-color: #ead3b3 !important;
  color: #b48a55 !important;
  box-shadow: none !important;
  opacity: 1 !important;
}

:global(html.dark .email-code-btn.el-button) {
  --el-button-bg-color: var(--bg-tertiary);
  --el-button-border-color: var(--border-color);
  --el-button-text-color: var(--text-primary);
  --el-button-hover-bg-color: var(--accent-light);
  --el-button-hover-border-color: var(--accent);
  --el-button-hover-text-color: var(--accent);
  background-color: var(--bg-tertiary) !important;
  border-color: var(--border-color) !important;
  color: var(--text-primary) !important;
  box-shadow: none !important;
}

:global(html.dark .email-code-btn.el-button:hover:not(:disabled)) {
  background-color: var(--accent-light) !important;
  border-color: var(--accent) !important;
  color: var(--accent) !important;
}

:global(html.dark .email-code-btn.el-button.is-disabled),
:global(html.dark .email-code-btn.el-button.is-disabled:hover),
:global(html.dark .email-code-btn.el-button:disabled),
:global(html.dark .email-code-btn.el-button:disabled:hover) {
  --el-button-disabled-bg-color: color-mix(in srgb, var(--bg-tertiary) 92%, transparent);
  --el-button-disabled-border-color: var(--border-color);
  --el-button-disabled-text-color: var(--text-secondary);
  background-color: color-mix(in srgb, var(--bg-tertiary) 92%, transparent) !important;
  border-color: var(--border-color) !important;
  color: var(--text-secondary) !important;
  box-shadow: none !important;
  opacity: 1 !important;
}

:global(.dark) .code-input-group .el-button {
  --el-button-bg-color: var(--bg-tertiary);
  --el-button-border-color: var(--border-color);
  --el-button-text-color: var(--text-primary);
  --el-button-hover-bg-color: var(--accent-light);
  --el-button-hover-border-color: var(--accent);
  --el-button-hover-text-color: var(--accent);
  background: var(--bg-tertiary) !important;
  border-color: var(--border-color) !important;
  color: var(--text-primary) !important;
  box-shadow: none !important;
}

:global(.dark) .code-input-group .el-button:hover:not(:disabled) {
  background: var(--accent-light);
  border-color: var(--accent);
  color: var(--accent);
}

:global(.dark) .code-input-group .el-button.is-disabled,
:global(.dark) .code-input-group .el-button.is-disabled:hover,
:global(.dark) .code-input-group .el-button:disabled,
:global(.dark) .code-input-group .el-button:disabled:hover {
  --el-button-disabled-bg-color: color-mix(in srgb, var(--bg-tertiary) 92%, transparent);
  --el-button-disabled-border-color: var(--border-color);
  --el-button-disabled-text-color: var(--text-secondary);
  background: color-mix(in srgb, var(--bg-tertiary) 92%, transparent) !important;
  border-color: var(--border-color) !important;
  color: var(--text-secondary) !important;
  opacity: 1 !important;
}

/* 头像对话框 */
.avatar-dialog-content {
  padding: 8px 0;
}

.avatar-upload-actions {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 10px;
  margin-bottom: 16px;
}

.avatar-file-input {
  display: none;
}

.music-upload-panel {
  padding: 4px 0 12px;
}

.music-upload-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-bottom: 10px;
}

.music-cover-preview {
  margin-top: 8px;
  display: flex;
  justify-content: center;
}

.music-cover-preview.failed {
  align-items: stretch;
}

.music-cover-preview img {
  width: 160px;
  height: 160px;
  border-radius: 18px;
  object-fit: cover;
  border: 1px solid var(--border-color);
  box-shadow: 0 10px 24px rgba(15, 23, 42, 0.12);
}

.music-cover-preview-fallback {
  width: 160px;
  min-height: 160px;
  padding: 18px 16px;
  border-radius: 18px;
  border: 1px dashed var(--border-color);
  background: var(--bg-tertiary);
  color: var(--text-secondary);
  display: grid;
  align-content: center;
  gap: 8px;
  text-align: center;
}

.music-cover-preview-title {
  color: var(--text-primary);
  font-weight: 700;
}

.music-cover-preview-desc {
  font-size: 0.84rem;
  line-height: 1.55;
}

.upload-hint {
  color: var(--text-tertiary);
  font-size: 0.8rem;
  line-height: 1.5;
}

.avatar-preview {
  margin-top: 16px;
  text-align: center;
}

.avatar-preview.empty {
  display: flex;
  justify-content: center;
}

.avatar-preview img {
  max-width: 150px;
  max-height: 150px;
  border-radius: 50%;
  object-fit: cover;
}

.avatar-preview-placeholder {
  width: 150px;
  height: 150px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--bg-tertiary);
  border: 1px dashed var(--border-color);
  color: var(--text-tertiary);
}

.avatar-preview-placeholder .avatar-placeholder-icon {
  width: 54px;
  height: 54px;
}

/* ===== 动画 ===== */
.animate-fade-in {
  animation: fadeIn 0.3s ease;
}

@keyframes fadeIn {
  from {
    opacity: 0;
    transform: translateY(10px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

/* ===== 深色模式适配 ===== */
:global(.dark) .stat-card:hover {
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.5);
}

:global(.dark) .badge-success {
  background: rgba(82, 196, 26, 0.25);
}

:global(.dark) .badge-warning {
  background: rgba(250, 173, 20, 0.25);
}

:global(.dark) .badge-danger {
  background: rgba(255, 77, 79, 0.25);
}

:global(.dark) .badge-info {
  background: rgba(54, 207, 201, 0.22);
}

/* ===== 响应式 ===== */
@media (max-width: 1200px) {
  .stats-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}

@media (max-width: 768px) {
  .moderation-bulk-bar,
  .moderation-task-head,
  .moderation-task-meta {
    align-items: stretch;
  }

  .moderation-bulk-actions {
    width: 100%;
  }

  .comment-toolbar-filters {
    width: 100%;
    justify-content: stretch;
  }

  .toolbar-search,
  .comment-toolbar-filters .toolbar-select {
    width: 100%;
  }

  .post-card-grid {
    grid-template-columns: 1fr;
  }

  .post-manage-card {
    grid-template-columns: 1fr;
  }

  .post-manage-cover {
    min-height: 170px;
    order: -1;
  }

  .post-card-topline {
    flex-wrap: wrap;
  }

  .sidebar {
    width: 60px;
  }

  .logo-text,
  .nav-label,
  .theme-label {
    display: none;
  }

  .sidebar-header {
    justify-content: center;
    padding: 16px 10px;
  }

  .nav-item {
    justify-content: center;
    padding: 14px 10px;
  }

  .main-wrap {
    margin-left: 60px;
  }

  .stats-grid {
    grid-template-columns: 1fr;
  }

  .topbar {
    padding: 0 16px;
  }

  .content-area {
    padding: 16px;
  }

  .theme-toggle {
    padding: 8px 10px;
  }

  .user-info {
    gap: 8px;
  }

  .username {
    display: none;
  }

  .profile-avatar-section {
    flex-direction: column;
    text-align: center;
  }

  .form-grid {
    grid-template-columns: 1fr;
  }
}

/* ===== 文章对话框 ===== */
:global(.post-dialog) {
  margin: 20px !important;
  max-width: 95vw !important;
}

:global(.post-dialog .el-dialog__header) {
  display: none;
}

:global(.post-dialog .el-dialog__body) {
  padding: 0;
  max-height: calc(100vh - 80px);
  overflow: hidden;
}

/* 全屏对话框样式 */
:global(.post-dialog.fullscreen-dialog) {
  width: 100% !important;
  max-width: calc(100vw - 40px) !important;
  max-height: calc(100vh - 40px) !important;
  margin: 20px auto;
}

:global(.post-dialog.fullscreen-dialog .el-dialog__body) {
  padding: 0;
  height: calc(100vh - 140px);
  overflow: hidden;
}

.post-dialog-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 24px;
  background: var(--bg-card);
  border-bottom: 1px solid var(--border-color);
}

.header-left,
.header-right {
  flex: 1;
  display: flex;
  align-items: center;
}

.header-left {
  justify-content: flex-start;
}

.header-right {
  justify-content: flex-end;
}

.header-center {
  flex: 2;
  text-align: center;
}

.dialog-title {
  font-size: 1.2rem;
  font-weight: 600;
  color: var(--text-primary);
}

.post-dialog-body {
  display: grid;
  grid-template-columns: 350px 1fr;
  height: calc(100vh - 80px);
  background: var(--bg-primary);
}

.post-form-main {
  padding: 24px;
  background: var(--bg-card);
  border-right: 1px solid var(--border-color);
  overflow-y: auto;
}

.post-form-main :deep(.el-form-item__label) {
  color: var(--text-secondary);
  font-weight: 500;
}

.post-form-main :deep(.el-input),
.post-form-main :deep(.el-textarea),
.post-form-main :deep(.el-select) {
  --el-input-bg-color: var(--bg-tertiary);
  --el-fill-color-blank: var(--bg-tertiary);
  --el-fill-color-light: var(--bg-tertiary);
  --el-text-color-regular: var(--text-primary);
  --el-text-color-placeholder: var(--text-tertiary);
  --el-border-color: var(--border-color);
  --el-border-color-hover: var(--accent);
  --el-input-focus-border-color: var(--accent);
}

.post-form-main :deep(.el-input__wrapper),
.post-form-main :deep(.el-textarea__inner),
.post-form-main :deep(.el-select__wrapper) {
  background: var(--bg-tertiary);
  border-color: var(--border-color);
  color: var(--text-primary);
  box-shadow: 0 0 0 1px var(--border-color) inset !important;
}

.post-form-main :deep(.el-input__wrapper:hover),
.post-form-main :deep(.el-select__wrapper:hover) {
  box-shadow: 0 0 0 1px var(--accent) inset !important;
}

.post-form-main :deep(.el-input__wrapper.is-focus),
.post-form-main :deep(.el-select__wrapper.is-focused) {
  box-shadow: 0 0 0 1px var(--accent) inset !important;
}

.post-form-main :deep(.el-input__inner::placeholder),
.post-form-main :deep(.el-textarea__inner::placeholder),
.post-form-main :deep(.el-select__placeholder) {
  color: var(--text-tertiary);
}

/* 分类选择框深色模式 */
.post-form-main :deep(.el-select) {
  width: 200px;
}

.post-form-main :deep(.el-select .el-input__wrapper) {
  background: var(--bg-tertiary) !important;
  border-color: var(--border-color);
  color: var(--text-primary);
  box-shadow: none !important;
}

.post-form-main :deep(.post-category-select .el-select__wrapper) {
  background: var(--bg-tertiary) !important;
  color: var(--text-primary) !important;
}

.post-form-main :deep(.el-select .el-input__inner) {
  background: var(--bg-tertiary) !important;
  color: var(--text-primary) !important;
}

.post-form-main :deep(.post-category-select .el-select__selected-item),
.post-form-main :deep(.post-category-select .el-select__placeholder),
.post-form-main :deep(.post-category-select .el-select__caret) {
  color: var(--text-primary) !important;
}

:global(.dark) .el-select-dropdown {
  background: var(--bg-card) !important;
  border-color: var(--border-color) !important;
}

:global(.dark) .el-select__popper.el-popper,
:global(.dark) .el-popper.is-light.el-select__popper {
  background: var(--bg-card) !important;
  border-color: var(--border-color) !important;
}

:global(.dark) .el-select__popper.el-popper .el-popper__arrow::before,
:global(.dark) .el-popper.is-light.el-select__popper .el-popper__arrow::before {
  background: var(--bg-card) !important;
  border-color: var(--border-color) !important;
}

:global(.dark) .el-select-dropdown__item {
  color: var(--text-primary);
}

:global(.dark) .el-select-dropdown__item:hover {
  background: var(--bg-tertiary) !important;
}

:global(.dark) .el-select-dropdown__item.selected {
  color: var(--accent);
  font-weight: 600;
}

.title-input :deep(.el-input__inner) {
  font-size: 1.3rem;
  font-weight: 600;
}

.post-editor-section {
  display: flex;
  flex-direction: column;
  padding: 16px;
  overflow: hidden;
}

.editor-label {
  font-size: 0.9rem;
  color: var(--text-secondary);
  margin-bottom: 12px;
  font-weight: 500;
}

.review-detail {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.review-meta-grid,
.review-inline-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16px;
}

.review-meta-card,
.review-section {
  background: var(--bg-card);
  border: 1px solid var(--border-color);
  border-radius: 16px;
  padding: 16px;
}

.review-meta-label,
.review-section-title {
  color: var(--text-secondary);
  font-size: 0.85rem;
  margin-bottom: 8px;
  font-weight: 600;
}

.review-meta-value,
.review-section-content {
  color: var(--text-primary);
  line-height: 1.7;
  word-break: break-word;
}

.review-message-content {
  white-space: pre-wrap;
}

.comment-context-body {
  margin-top: 10px;
  padding: 12px 14px;
  border-radius: 12px;
  background: var(--bg-tertiary);
  color: var(--text-primary);
}

.comment-admin-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.comment-admin-tag {
  display: inline-flex;
  align-items: center;
  min-height: 30px;
  padding: 0 12px;
  border-radius: 999px;
  border: 1px solid var(--border-color);
  background: color-mix(in srgb, var(--bg-tertiary) 86%, transparent);
  color: var(--text-secondary);
  font-size: 0.82rem;
}

.comment-review-images {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(140px, 1fr));
  gap: 12px;
}

.comment-review-image {
  display: block;
  overflow: hidden;
  border-radius: 14px;
  border: 1px solid var(--border-color);
  background: var(--bg-tertiary);
}

.comment-review-image img {
  display: block;
  width: 100%;
  height: 140px;
  object-fit: cover;
}

.report-resolution-note {
  margin-top: 8px;
}

.review-preview {
  min-height: 220px;
  max-height: 420px;
  overflow: auto;
  padding: 16px;
  border-radius: 14px;
  background: var(--bg-tertiary);
  color: var(--text-primary);
}

.review-preview :deep(pre) {
  padding: 16px;
  border-radius: 14px;
  overflow: auto;
  background: #1e2937;
  color: #f8fafc;
}

.review-preview :deep(p:first-child) {
  margin-top: 0;
}

.review-preview :deep(p:last-child) {
  margin-bottom: 0;
}

.password-rules-inline {
  margin-top: 10px;
  display: grid;
  gap: 6px;
}

.password-rule-inline {
  font-size: 0.84rem;
  color: var(--text-secondary);
  transition: color 0.2s ease;
}

.password-rule-inline.met {
  color: var(--accent);
}

@media (max-width: 1200px) {
  .post-dialog-body {
    grid-template-columns: 1fr;
    grid-template-rows: auto 1fr;
  }
  
  .post-form-main {
    border-right: none;
    border-bottom: 1px solid var(--border-color);
    max-height: 300px;
  }

  .review-meta-grid,
  .review-inline-grid {
    grid-template-columns: 1fr;
  }
}
</style>
