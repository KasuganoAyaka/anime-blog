<template>
  <div class="chat-widget">
    <transition name="chat-backdrop">
      <div
        v-if="visible"
        class="chat-backdrop"
        @click="closePanel"
      ></div>
    </transition>

    <div class="chat-anchor">
    <transition name="chat-fab">
      <button
        v-if="!visible"
        type="button"
        class="chat-fab"
        @click="openPanel"
      >
        <span class="chat-fab-icon">💬</span>
        <span class="chat-fab-label">聊天室</span>
        <span v-if="totalUnread > 0" class="chat-fab-badge">{{ formatBadge(totalUnread) }}</span>
      </button>
    </transition>

    <transition name="chat-panel">
      <section v-if="visible" class="chat-panel" :class="{ 'chat-panel-ready': panelContentReady }">
        <header class="chat-panel-header">
          <div>
            <div class="chat-panel-title">站内聊天</div>
            <div class="chat-panel-subtitle">
              <span :class="['chat-connection-dot', { online: socketConnected }]"></span>
              {{ socketConnected ? '实时连接中' : '连接中断，正在重连' }}
            </div>
          </div>
          <div class="chat-panel-actions">
            <button type="button" class="chat-icon-btn" title="刷新" @click="refreshBootstrap()">
              ↻
            </button>
            <button type="button" class="chat-icon-btn" title="关闭" @click="closePanel">
              ×
            </button>
          </div>
        </header>

        <div class="chat-panel-body">
          <aside class="chat-sidebar">
            <button
              type="button"
              :class="['chat-room-item', { active: activeRoom.type === 'global' }]"
              @click="openGlobalRoom"
            >
              <div class="chat-room-avatar global">全</div>
              <div class="chat-room-main">
                <div class="chat-room-name">{{ globalRoomName }}</div>
                <div class="chat-room-meta">所有登录用户默认加入</div>
              </div>
              <span v-if="globalUnreadCount > 0" class="chat-room-badge">{{ formatBadge(globalUnreadCount) }}</span>
            </button>

            <section class="chat-sidebar-section">
              <div class="chat-section-title">搜索联系人</div>
              <div class="chat-tool-grid">
                <label class="chat-tool-field">
                  <span>会话排序</span>
                  <select v-model="conversationSort" class="chat-select">
                    <option value="smart">未读优先</option>
                    <option value="latest">按最新消息</option>
                    <option value="name">按名称</option>
                  </select>
                </label>
                <div class="chat-tool-field">
                  <span>按昵称 / 用户名 / 邮箱搜索</span>
                  <div class="chat-search-bar">
                    <input
                      v-model="contactSearchKeyword"
                      class="chat-search-input"
                      type="text"
                      placeholder="输入昵称、用户名或邮箱"
                    />
                    <button
                      type="button"
                      class="chat-mini-btn primary"
                      :disabled="contactSearchLoading || !contactSearchKeyword.trim()"
                      @click="handleContactSearch"
                    >
                      {{ contactSearchLoading ? '搜索中...' : '搜索' }}
                    </button>
                    <button
                      type="button"
                      class="chat-mini-btn"
                      :disabled="contactSearchLoading || (!contactSearchKeyword.trim() && !appliedContactSearchKeyword)"
                      @click="clearContactSearch"
                    >
                      清空
                    </button>
                  </div>
                </div>
              </div>
              <div v-if="appliedContactSearchKeyword && searchedContacts.length === 0" class="chat-section-empty">没有找到匹配的联系人</div>
              <div v-else-if="searchedContacts.length > 0" class="chat-search-results">
                <div
                  v-for="contact in searchedContacts"
                  :key="contact.id"
                  class="chat-card"
                >
                  <div class="chat-card-top">
                    <div class="chat-card-title">
                      {{ displayName(contact) }}
                      <span v-if="isElevatedRole(contact.role)" class="chat-role-chip">{{ getRoleLabel(contact) }}</span>
                      <span v-else-if="contact.friend" class="chat-role-chip friend">好友</span>
                    </div>
                    <div class="chat-card-meta">@{{ contact.username }}</div>
                  </div>
                  <div class="chat-card-meta">{{ contactStatusText(contact) }}</div>
                  <div class="chat-card-actions">
                    <button
                      v-if="contact.canChatDirectly"
                      type="button"
                      class="chat-mini-btn primary"
                      @click="openConversationByContact(contact)"
                    >
                      {{ isAdmin ? '直接联系' : '私聊' }}
                    </button>
                    <button
                      v-else-if="contact.canAddFriend"
                      type="button"
                      class="chat-mini-btn primary"
                      @click="handleRequestFriend(contact)"
                    >
                      添加好友
                    </button>
                    <button
                      v-else-if="contact.outgoingRequestPending"
                      type="button"
                      class="chat-mini-btn"
                      disabled
                    >
                      已申请
                    </button>
                    <button
                      v-if="contact.canRemoveFriend"
                      type="button"
                      class="chat-mini-btn"
                      @click="handleRemoveFriend(contact)"
                    >
                      删除好友
                    </button>
                    <button
                      v-if="contact.canBlock"
                      type="button"
                      class="chat-mini-btn danger"
                      @click="handleBlockUser(contact)"
                    >
                      拉黑
                    </button>
                    <button
                      v-if="contact.canUnblock"
                      type="button"
                      class="chat-mini-btn"
                      @click="handleUnblockUser(contact)"
                    >
                      解除拉黑
                    </button>
                  </div>
                </div>
              </div>
            </section>

            <section class="chat-sidebar-section">
              <div class="chat-section-title">会话列表</div>
              <div v-if="filteredConversations.length === 0" class="chat-section-empty">暂无可展示会话</div>
              <button
                v-for="conversation in filteredConversations"
                :key="conversation.peerUserId"
                type="button"
                :class="['chat-room-item', { active: activeRoom.type === 'private' && activeRoom.targetUserId === conversation.peerUserId }]"
                @click="openConversation(conversation)"
              >
                <img
                  v-if="conversation.avatar"
                  :src="conversation.avatar"
                  :alt="conversation.title"
                  class="chat-room-avatar"
                />
                <div v-else class="chat-room-avatar">{{ conversation.title?.slice(0, 1) || '?' }}</div>
                <div class="chat-room-main">
                  <div class="chat-room-name">
                    {{ conversation.title }}
                    <span v-if="isElevatedRole(conversation.role)" class="chat-role-chip">{{ getRoleLabel(conversation) }}</span>
                    <span v-else-if="conversation.friend" class="chat-role-chip friend">好友</span>
                    <span v-if="conversation.blocked" class="chat-role-chip blocked">已拉黑</span>
                  </div>
                  <div class="chat-room-meta">
                    {{ conversation.lastMessagePreview || conversation.username || '点击查看会话' }}
                  </div>
                </div>
                <div class="chat-room-side">
                  <span class="chat-room-time">{{ formatTime(conversation.lastMessageTime) }}</span>
                  <span v-if="conversation.unreadCount > 0" class="chat-room-badge">{{ formatBadge(conversation.unreadCount) }}</span>
                </div>
              </button>
            </section>

            <section v-if="incomingRequests.length > 0" class="chat-sidebar-section">
              <div class="chat-section-title">收到的好友申请</div>
              <div
                v-for="request in incomingRequests"
                :key="request.id"
                class="chat-card"
              >
                <div class="chat-card-title">{{ request.requesterName }}</div>
                <div class="chat-card-meta">申请时间：{{ formatTime(request.createTime) }}</div>
                <div class="chat-card-actions">
                  <button type="button" class="chat-mini-btn primary" @click="handleAcceptRequest(request)">同意</button>
                  <button type="button" class="chat-mini-btn danger" @click="handleRejectRequest(request)">拒绝</button>
                </div>
              </div>
            </section>

            <section v-if="outgoingRequests.length > 0" class="chat-sidebar-section">
              <div class="chat-section-title">发出的好友申请</div>
              <div
                v-for="request in outgoingRequests"
                :key="request.id"
                class="chat-card"
              >
                <div class="chat-card-title">{{ request.targetUserName }}</div>
                <div class="chat-card-meta">等待对方处理</div>
                <div class="chat-card-actions">
                  <button type="button" class="chat-mini-btn" @click="handleCancelRequest(request)">撤回</button>
                </div>
              </div>
            </section>

          </aside>

          <main class="chat-main">
            <header class="chat-conversation-header">
              <div>
                <div class="chat-conversation-title">{{ activeRoomTitle }}</div>
                <div class="chat-conversation-meta">{{ activeRoomDescription }}</div>
              </div>
              <div v-if="activeRoom.type === 'private' && activeContact" class="chat-conversation-actions">
                <button
                  v-if="activeContact.canAddFriend"
                  type="button"
                  class="chat-mini-btn primary"
                  @click="handleRequestFriend(activeContact)"
                >
                  申请好友
                </button>
                <button
                  v-if="activeContact.canRemoveFriend"
                  type="button"
                  class="chat-mini-btn"
                  @click="handleRemoveFriend(activeContact)"
                >
                  删除好友
                </button>
                <button
                  v-if="activeContact.canBlock"
                  type="button"
                  class="chat-mini-btn danger"
                  @click="handleBlockUser(activeContact)"
                >
                  拉黑
                </button>
                <button
                  v-if="activeContact.canUnblock"
                  type="button"
                  class="chat-mini-btn"
                  @click="handleUnblockUser(activeContact)"
                >
                  解除拉黑
                </button>
              </div>
            </header>

            <div ref="messageListRef" class="chat-message-list" @scroll="handleMessageScroll">
              <div v-if="historyLoading" class="chat-history-tip">正在加载更早消息...</div>
              <div v-if="loading" class="chat-state">聊天数据加载中...</div>
              <div v-else-if="activeRoom.type === 'private' && !canSendMessage" class="chat-state">
                {{ blockedStateText }}
              </div>
              <div v-else-if="currentMessages.length === 0" class="chat-state">
                {{ emptyMessageText }}
              </div>
              <div
                v-for="message in currentMessages"
                :key="message.id"
                :class="['chat-message', { mine: isMine(message) }]"
              >
                <img
                  v-if="!isMine(message) && message.senderAvatar"
                  :src="message.senderAvatar"
                  :alt="message.senderName"
                  class="chat-message-avatar"
                />
                <div v-else-if="!isMine(message)" class="chat-message-avatar">{{ message.senderName?.slice(0, 1) || '?' }}</div>
                <div class="chat-message-body">
                  <div class="chat-message-author">
                    {{ isMine(message) ? '我' : message.senderName }}
                    <span v-if="isElevatedRole(message.senderRole)" class="chat-role-chip">{{ getRoleLabel({ role: message.senderRole }) }}</span>
                  </div>
                  <div class="chat-message-bubble">
                    <div class="chat-message-content">{{ message.content }}</div>
                  </div>
                  <div class="chat-message-time">{{ formatTime(message.createTime) }}</div>
                </div>
              </div>
            </div>

            <div v-if="showJumpToLatest" class="chat-scroll-actions">
              <button type="button" class="chat-jump-btn" @click="scrollToLatest">
                最新消息
              </button>
            </div>

            <footer class="chat-composer">
              <textarea
                v-model="draft"
                class="chat-input"
                :placeholder="canSendMessage ? '输入消息，Ctrl/Cmd + Enter 发送' : '当前会话不可发送消息'"
                rows="3"
                :disabled="!canSendMessage"
                @keydown="handleComposerKeydown"
              />
              <div class="chat-composer-actions">
                <div class="chat-composer-hint">{{ composerHint }}</div>
                <button
                  type="button"
                  class="chat-send-btn"
                  :disabled="sending || !draft.trim() || !canSendMessage"
                  @click="sendCurrentMessage"
                >
                  {{ sending ? '发送中...' : '发送' }}
                </button>
              </div>
            </footer>
          </main>
        </div>
        </section>
      </transition>
    </div>
  </div>
</template>

<script setup>
import { computed, nextTick, onMounted, onUnmounted, ref, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { chatApi, ensureValidAccessToken } from '@/api'
import { useUserStore } from '@/stores'
import { getRequestErrorMessage } from '@/utils/request'

const userStore = useUserStore()

const visible = ref(false)
const bootstrapping = ref(false)
const messageLoading = ref(false)
const historyLoading = ref(false)
const sending = ref(false)
const socketConnected = ref(false)

const globalRoomName = ref('全站聊天室')
const globalUnreadCount = ref(0)
const contacts = ref([])
const conversations = ref([])
const incomingRequests = ref([])
const outgoingRequests = ref([])
const globalMessages = ref([])
const privateMessages = ref({})
const privateHasMore = ref({})
const activeRoom = ref({ type: 'global', targetUserId: null })
const draft = ref('')
const conversationSort = ref('smart')
const contactSearchKeyword = ref('')
const appliedContactSearchKeyword = ref('')
const searchedContacts = ref([])
const contactSearchLoading = ref(false)
const messageListRef = ref(null)
const globalHasMore = ref(true)
const showJumpToLatest = ref(false)
const shouldStickToBottom = ref(true)
const autoScrolling = ref(false)
const panelContentReady = ref(false)

const INITIAL_MESSAGE_LIMIT = 80
const HISTORY_BATCH_SIZE = 30

let socket = null
let reconnectTimer = null
let socketConnecting = false

const loading = computed(() => bootstrapping.value || messageLoading.value)
const currentUserId = computed(() => userStore.user?.id)
const isElevatedRole = (role) => role === 'admin' || role === 'manager'
const getRoleLabel = (user) => (user?.role === 'admin' ? '站长' : '管理员')
const isAdmin = computed(() => isElevatedRole(userStore.user?.role))
const contactMap = computed(() => new Map(contacts.value.map(item => [item.id, item])))
const conversationMap = computed(() => new Map(conversations.value.map(item => [item.peerUserId, item])))
const activeContact = computed(() => contactMap.value.get(activeRoom.value.targetUserId) || null)
const totalUnread = computed(() => (
  Number(globalUnreadCount.value || 0)
  + conversations.value.reduce((sum, item) => sum + Number(item.unreadCount || 0), 0)
))
const currentMessages = computed(() => {
  if (activeRoom.value.type === 'global') {
    return globalMessages.value
  }
  return privateMessages.value[activeRoom.value.targetUserId] || []
})
const filteredConversations = computed(() => {
  const list = [...conversations.value]
  return list.sort((left, right) => compareConversation(left, right, conversationSort.value))
})
const canSendMessage = computed(() => {
  if (activeRoom.value.type === 'global') {
    return true
  }
  return Boolean(activeContact.value?.canChatDirectly)
})
const activeRoomTitle = computed(() => (
  activeRoom.value.type === 'global'
    ? globalRoomName.value
    : displayName(activeContact.value)
))
const activeRoomDescription = computed(() => {
  if (activeRoom.value.type === 'global') {
    return '在这里和全站用户实时聊天'
  }
  if (!activeContact.value) {
    return '请选择一个会话'
  }
  if (activeContact.value.blockedByMe) {
    return '你已拉黑对方，解除拉黑后可继续私聊'
  }
  if (activeContact.value.blockedByOther) {
    return '对方已将你拉黑，当前无法继续私聊'
  }
  if (isElevatedRole(activeContact.value.role)) {
    return `${getRoleLabel(activeContact.value)}会话，可直接私聊`
  }
  if (activeContact.value.friend) {
    return '站内好友，消息仅双方可见'
  }
  if (activeContact.value.outgoingRequestPending) {
    return '好友申请已发出，等待对方处理'
  }
  if (activeContact.value.incomingRequestPending) {
    return '对方向你发来了好友申请，可先在左侧处理'
  }
  return '当前暂不可直接私聊'
})
const blockedStateText = computed(() => {
  if (!activeContact.value) {
    return '请选择一个联系人开始聊天'
  }
  if (activeContact.value.blockedByMe) {
    return '你已拉黑对方，解除拉黑后可继续发送消息。'
  }
  if (activeContact.value.blockedByOther) {
    return '对方已将你拉黑，当前无法继续发送消息。'
  }
  if (activeContact.value.outgoingRequestPending) {
    return '好友申请已发出，等对方同意后即可私聊。'
  }
  if (activeContact.value.incomingRequestPending) {
    return '对方向你发来了好友申请，同意后即可开始私聊。'
  }
  return '当前会话不可发送消息。'
})
const emptyMessageText = computed(() => (
  activeRoom.value.type === 'global'
    ? '全站聊天室还没有消息，发一条试试吧。'
    : '你们的私聊还没有消息，打个招呼吧。'
))
const composerHint = computed(() => (
  activeRoom.value.type === 'global'
    ? '消息会广播到全站聊天室'
    : canSendMessage.value
      ? '只有你和对方能看到这条消息'
      : '请先处理好友关系或拉黑状态'
))
const canLoadMoreHistory = computed(() => {
  if (activeRoom.value.type === 'global') {
    return globalHasMore.value
  }
  return Boolean(privateHasMore.value[activeRoom.value.targetUserId])
})

const displayName = (contact) => {
  if (!contact) return '聊天'
  return contact.nickname || contact.username || '未命名用户'
}

const formatBadge = (value) => (value > 99 ? '99+' : String(value))

const formatTime = (value) => {
  if (!value) return ''
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return ''
  return date.toLocaleString('zh-CN', {
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
    hour12: false
  })
}

const buildPreview = (value, limit = 32) => {
  const text = String(value || '').replace(/\s+/g, ' ').trim()
  if (!text) return ''
  return text.length > limit ? `${text.slice(0, limit)}...` : text
}

const isMine = (message) => message.senderId === currentUserId.value

const compareConversation = (left, right, mode) => {
  if (mode === 'name') {
    return (left.title || '').localeCompare(right.title || '', 'zh-CN')
  }

  const leftTime = toTimeValue(left.lastMessageTime)
  const rightTime = toTimeValue(right.lastMessageTime)
  if (mode === 'latest') {
    if (rightTime !== leftTime) return rightTime - leftTime
    return (left.title || '').localeCompare(right.title || '', 'zh-CN')
  }

  const unreadGap = Number(right.unreadCount || 0) - Number(left.unreadCount || 0)
  if (unreadGap !== 0) return unreadGap
  if (rightTime !== leftTime) return rightTime - leftTime
  return (left.title || '').localeCompare(right.title || '', 'zh-CN')
}

const toTimeValue = (value) => {
  if (!value) return 0
  const date = new Date(value)
  return Number.isNaN(date.getTime()) ? 0 : date.getTime()
}

const isAtBottom = (element) => {
  if (!element) return true
  return element.scrollHeight - element.scrollTop - element.clientHeight < 56
}

const setPrivateMessages = (targetUserId, messages) => {
  privateMessages.value = {
    ...privateMessages.value,
    [targetUserId]: messages
  }
}

const setPrivateHasMore = (targetUserId, value) => {
  privateHasMore.value = {
    ...privateHasMore.value,
    [targetUserId]: value
  }
}

const appendUniqueMessage = (list, message) => {
  if (list.some(item => item.id === message.id)) {
    return list
  }
  return [...list, message]
}

const ensureConversationRecord = (peerUserId) => {
  const existing = conversationMap.value.get(peerUserId)
  if (existing) {
    return existing
  }
  const contact = contactMap.value.get(peerUserId)
  if (!contact) {
    return null
  }

  const created = {
    roomType: 'private',
    roomKey: '',
    peerUserId,
    title: displayName(contact),
    username: contact.username,
    nickname: contact.nickname,
    avatar: contact.avatar,
    role: contact.role,
    friend: Boolean(contact.friend),
    blocked: Boolean(contact.blockedByMe || contact.blockedByOther),
    canChatDirectly: Boolean(contact.canChatDirectly),
    unreadCount: 0,
    lastMessagePreview: '',
    lastMessageTime: null,
    hasMessages: false
  }
  conversations.value = [created, ...conversations.value]
  return created
}

const updateConversation = (peerUserId, updater) => {
  const existing = ensureConversationRecord(peerUserId)
  if (!existing) {
    return
  }
  conversations.value = conversations.value.map(item => {
    if (item.peerUserId !== peerUserId) {
      return item
    }
    const nextValue = updater({ ...item })
    return {
      ...item,
      ...nextValue
    }
  })
}

const waitForPaint = () => new Promise(resolve => requestAnimationFrame(() => requestAnimationFrame(resolve)))

const scrollToBottom = async () => {
  const el = messageListRef.value
  if (!el) return
  autoScrolling.value = true
  await nextTick()
  el.scrollTop = el.scrollHeight
  await waitForPaint()
  el.scrollTop = el.scrollHeight
  shouldStickToBottom.value = true
  showJumpToLatest.value = false
  window.setTimeout(() => {
    autoScrolling.value = false
  }, 80)
}

const scrollToLatest = async () => {
  shouldStickToBottom.value = true
  showJumpToLatest.value = false
  await scrollToBottom()
  markActiveRoomRead()
}

const prependHistoryMessages = async (targetUserId, messages) => {
  const el = messageListRef.value
  const previousHeight = el?.scrollHeight || 0
  const previousTop = el?.scrollTop || 0

  if (targetUserId == null) {
    globalMessages.value = [...messages, ...globalMessages.value]
  } else {
    const current = privateMessages.value[targetUserId] || []
    setPrivateMessages(targetUserId, [...messages, ...current])
  }

  await nextTick()
  if (el) {
    el.scrollTop = el.scrollHeight - previousHeight + previousTop
  }
}

const handleMessageScroll = async () => {
  const el = messageListRef.value
  if (!el) return
  if (autoScrolling.value) return

  const atBottom = isAtBottom(el)
  shouldStickToBottom.value = atBottom
  showJumpToLatest.value = !atBottom && currentMessages.value.length > 0

  if (el.scrollTop > 80 || historyLoading.value || loading.value || !canLoadMoreHistory.value) {
    return
  }
  await loadOlderMessages()
}

const replaceBootstrap = (data) => {
  globalRoomName.value = data.globalRoomName || '全站聊天室'
  globalUnreadCount.value = Number(data.globalUnreadCount || 0)
  contacts.value = data.contacts || []
  conversations.value = data.conversations || []
  incomingRequests.value = data.incomingRequests || []
  outgoingRequests.value = data.outgoingRequests || []
  globalMessages.value = data.globalMessages || []
  globalHasMore.value = globalMessages.value.length >= INITIAL_MESSAGE_LIMIT

  if (activeRoom.value.type === 'private' && !contactMap.value.has(activeRoom.value.targetUserId)) {
    activeRoom.value = { type: 'global', targetUserId: null }
  }
}

const ensureBootstrap = async () => {
  if (!userStore.token || bootstrapping.value) return
  bootstrapping.value = true
  try {
    const res = await chatApi.getBootstrap()
    replaceBootstrap(res.data || {})
  } catch (error) {
    ElMessage.error(getRequestErrorMessage(error, '聊天数据加载失败'))
  } finally {
    bootstrapping.value = false
  }
}

const fetchContactSearchResults = async (keyword, silent = false) => {
  const normalizedKeyword = keyword.trim()
  if (!normalizedKeyword) {
    return []
  }

  try {
    const res = await chatApi.searchContacts({ keyword: normalizedKeyword })
    return res.data || []
  } catch (error) {
    if (!silent) {
      ElMessage.error(getRequestErrorMessage(error, '联系人搜索失败'))
    }
    throw error
  }
}

const refreshContactSearchResults = async () => {
  if (!appliedContactSearchKeyword.value) {
    return
  }

  try {
    searchedContacts.value = await fetchContactSearchResults(appliedContactSearchKeyword.value, true)
  } catch (error) {}
}

const refreshBootstrap = async (showToast = true) => {
  await ensureBootstrap()
  await refreshContactSearchResults()
  if (showToast) {
    ElMessage.success('聊天列表已刷新')
  }
}

const connectSocket = async () => {
  if (!userStore.isLoggedIn || socket || reconnectTimer || socketConnecting) return

  socketConnecting = true
  let shouldRetry = false

  try {
    const tokenReady = await ensureValidAccessToken()
    userStore.hydrateSession()
    if (!tokenReady || !userStore.token || socket) {
      socketConnected.value = false
      return
    }

    const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:'
    const nextSocket = new WebSocket(`${protocol}//${window.location.host}/ws/chat?token=${encodeURIComponent(userStore.token)}`)
    socket = nextSocket

    nextSocket.addEventListener('open', () => {
      if (socket !== nextSocket) return
      socketConnected.value = true
    })

    nextSocket.addEventListener('message', async (event) => {
      if (socket !== nextSocket) return

      let payload = null
      try {
        payload = JSON.parse(event.data)
      } catch (error) {
        return
      }

      if (payload?.type === 'chat.message' && payload.message) {
        applyIncomingMessage(payload.message)
        return
      }

      if (payload?.type === 'chat.sync') {
        await refreshBootstrap(false)
      }
    })

    nextSocket.addEventListener('close', () => {
      if (socket !== nextSocket) return
      socket = null
      socketConnected.value = false
      scheduleReconnect()
    })

    nextSocket.addEventListener('error', () => {
      if (socket !== nextSocket) return
      socketConnected.value = false
    })
  } catch (error) {
    socketConnected.value = false
    shouldRetry = true
  } finally {
    socketConnecting = false
    if (shouldRetry) {
      scheduleReconnect()
    }
  }
}

const closeSocket = () => {
  socketConnecting = false
  if (reconnectTimer) {
    clearTimeout(reconnectTimer)
    reconnectTimer = null
  }

  const activeSocket = socket
  socket = null
  if (activeSocket) {
    activeSocket.close()
  }
  socketConnected.value = false
}

const scheduleReconnect = () => {
  if (!userStore.isLoggedIn || reconnectTimer || socketConnecting) return
  reconnectTimer = window.setTimeout(() => {
    reconnectTimer = null
    void connectSocket()
  }, 3000)
}

const markGlobalRead = async (silent = true) => {
  if (!userStore.token) return
  try {
    await chatApi.markRoomRead({ roomType: 'global' })
    globalUnreadCount.value = 0
  } catch (error) {
    if (!silent) {
      ElMessage.error(getRequestErrorMessage(error, '标记全站消息已读失败'))
    }
  }
}

const markPrivateRead = async (targetUserId, silent = true) => {
  if (!userStore.token || !targetUserId) return
  try {
    await chatApi.markRoomRead({ roomType: 'private', targetUserId })
    updateConversation(targetUserId, item => ({ ...item, unreadCount: 0 }))
  } catch (error) {
    if (!silent) {
      ElMessage.error(getRequestErrorMessage(error, '标记私聊已读失败'))
    }
  }
}

const openPanel = async () => {
  panelContentReady.value = false
  visible.value = true
  await ensureBootstrap()
  await connectSocket()
  await markActiveRoomRead()
  await scrollToBottom()
  await waitForPaint()
  panelContentReady.value = true
}

const openGlobalRoom = async () => {
  activeRoom.value = { type: 'global', targetUserId: null }
  await markGlobalRead(false)
  await scrollToBottom()
}

const openConversation = async (conversation) => {
  activeRoom.value = {
    type: 'private',
    targetUserId: conversation.peerUserId
  }
  if (conversation.canChatDirectly || conversation.hasMessages) {
    await loadPrivateMessages(conversation.peerUserId, true)
    if (conversation.canChatDirectly) {
      await markPrivateRead(conversation.peerUserId, false)
    }
  }
  await scrollToBottom()
}

const openConversationByContact = async (contact) => {
  await openConversation({
    peerUserId: contact.id,
    canChatDirectly: contact.canChatDirectly
  })
}

const loadPrivateMessages = async (targetUserId, reset = false) => {
  if (reset) {
    messageLoading.value = true
  }
  try {
    const params = { limit: reset ? INITIAL_MESSAGE_LIMIT : HISTORY_BATCH_SIZE }
    const current = privateMessages.value[targetUserId] || []
    if (!reset && current.length > 0) {
      params.beforeMessageId = current[0]?.id
    }
    const res = await chatApi.getPrivateMessages(targetUserId, params)
    const messages = res.data || []
    if (reset) {
      setPrivateMessages(targetUserId, messages)
      setPrivateHasMore(targetUserId, messages.length >= INITIAL_MESSAGE_LIMIT)
    } else {
      await prependHistoryMessages(targetUserId, messages)
      setPrivateHasMore(targetUserId, messages.length >= HISTORY_BATCH_SIZE)
    }
  } catch (error) {
    ElMessage.error(getRequestErrorMessage(error, '私聊记录加载失败'))
  } finally {
    if (reset) {
      messageLoading.value = false
    }
  }
}

const loadGlobalMessages = async (reset = false) => {
  if (reset) {
    messageLoading.value = true
  }
  try {
    const params = { limit: reset ? INITIAL_MESSAGE_LIMIT : HISTORY_BATCH_SIZE }
    if (!reset && globalMessages.value.length > 0) {
      params.beforeMessageId = globalMessages.value[0]?.id
    }
    const res = await chatApi.getGlobalMessages(params)
    const messages = res.data || []
    if (reset) {
      globalMessages.value = messages
      globalHasMore.value = messages.length >= INITIAL_MESSAGE_LIMIT
    } else {
      await prependHistoryMessages(null, messages)
      globalHasMore.value = messages.length >= HISTORY_BATCH_SIZE
    }
  } catch (error) {
    ElMessage.error(getRequestErrorMessage(error, '全站消息加载失败'))
  } finally {
    if (reset) {
      messageLoading.value = false
    }
  }
}

const loadOlderMessages = async () => {
  if (historyLoading.value || !canLoadMoreHistory.value) return
  historyLoading.value = true
  try {
    if (activeRoom.value.type === 'global') {
      await loadGlobalMessages(false)
    } else if (activeRoom.value.targetUserId) {
      await loadPrivateMessages(activeRoom.value.targetUserId, false)
    }
  } finally {
    historyLoading.value = false
  }
}

const markActiveRoomRead = async () => {
  if (activeRoom.value.type === 'global') {
    await markGlobalRead(true)
    return
  }
  if (activeRoom.value.targetUserId && activeContact.value?.canChatDirectly) {
    await markPrivateRead(activeRoom.value.targetUserId, true)
  }
}

const syncConversationFromMessage = (message) => {
  if (message.roomType !== 'private') {
    return
  }

  const peerUserId = message.senderId === currentUserId.value ? message.receiverId : message.senderId
  if (!peerUserId) {
    return
  }

  updateConversation(peerUserId, item => {
    const isActive = visible.value
      && activeRoom.value.type === 'private'
      && activeRoom.value.targetUserId === peerUserId
      && activeContact.value?.canChatDirectly
    const isOutgoing = message.senderId === currentUserId.value
    return {
      ...item,
      lastMessagePreview: buildPreview(message.content),
      lastMessageTime: message.createTime,
      hasMessages: true,
      unreadCount: isOutgoing || isActive ? 0 : Number(item.unreadCount || 0) + 1
    }
  })
}

const applyIncomingMessage = (message) => {
  const activeGlobal = visible.value && activeRoom.value.type === 'global'
  if (message.roomType === 'global') {
    globalMessages.value = appendUniqueMessage(globalMessages.value, message)
    if (!activeGlobal) {
      globalUnreadCount.value += 1
    } else if (message.senderId !== currentUserId.value) {
      markGlobalRead(true)
    }
  } else if (message.roomType === 'private') {
    const peerUserId = message.senderId === currentUserId.value ? message.receiverId : message.senderId
    const list = privateMessages.value[peerUserId] || []
    setPrivateMessages(peerUserId, appendUniqueMessage(list, message))
    syncConversationFromMessage(message)
    if (
      message.senderId !== currentUserId.value
      && visible.value
      && activeRoom.value.type === 'private'
      && activeRoom.value.targetUserId === peerUserId
      && activeContact.value?.canChatDirectly
    ) {
      markPrivateRead(peerUserId, true)
    }
  }

  nextTick(async () => {
    if (visible.value && shouldStickToBottom.value) {
      await scrollToBottom()
    } else if (visible.value) {
      showJumpToLatest.value = true
    }
  })
}

const handleRequestFriend = async (contact) => {
  try {
    await chatApi.requestFriend(contact.id)
    await refreshBootstrap(false)
    ElMessage.success(`已向 ${displayName(contact)} 发出好友申请`)
  } catch (error) {
    ElMessage.error(getRequestErrorMessage(error, '好友申请发送失败'))
  }
}

const handleAcceptRequest = async (request) => {
  try {
    await chatApi.acceptFriendRequest(request.id)
    await refreshBootstrap(false)
    const latest = contactMap.value.get(request.requesterId)
    if (latest?.canChatDirectly) {
      await openConversationByContact(latest)
    }
    ElMessage.success('已同意好友申请')
  } catch (error) {
    ElMessage.error(getRequestErrorMessage(error, '处理好友申请失败'))
  }
}

const handleRejectRequest = async (request) => {
  try {
    await chatApi.rejectFriendRequest(request.id)
    await refreshBootstrap(false)
    ElMessage.success('已拒绝好友申请')
  } catch (error) {
    ElMessage.error(getRequestErrorMessage(error, '拒绝好友申请失败'))
  }
}

const handleCancelRequest = async (request) => {
  try {
    await chatApi.cancelFriendRequest(request.id)
    await refreshBootstrap(false)
    ElMessage.success('好友申请已撤回')
  } catch (error) {
    ElMessage.error(getRequestErrorMessage(error, '撤回好友申请失败'))
  }
}

const handleRemoveFriend = async (contact) => {
  try {
    await ElMessageBox.confirm(`确定删除与 ${displayName(contact)} 的好友关系吗？`, '提示', {
      type: 'warning'
    })
    await chatApi.removeFriend(contact.id)
    await refreshBootstrap(false)
    ElMessage.success('好友已删除')
  } catch (error) {
    if (error === 'cancel') return
    ElMessage.error(getRequestErrorMessage(error, '删除好友失败'))
  }
}

const handleBlockUser = async (contact) => {
  try {
    await ElMessageBox.confirm(`确定拉黑 ${displayName(contact)} 吗？拉黑后将无法继续私聊。`, '提示', {
      type: 'warning'
    })
    await chatApi.blockUser(contact.id)
    await refreshBootstrap(false)
    ElMessage.success('已拉黑该用户')
  } catch (error) {
    if (error === 'cancel') return
    ElMessage.error(getRequestErrorMessage(error, '拉黑用户失败'))
  }
}

const handleUnblockUser = async (contact) => {
  try {
    await chatApi.unblockUser(contact.id)
    await refreshBootstrap(false)
    ElMessage.success('已解除拉黑')
  } catch (error) {
    ElMessage.error(getRequestErrorMessage(error, '解除拉黑失败'))
  }
}

const clearContactSearch = () => {
  contactSearchKeyword.value = ''
  appliedContactSearchKeyword.value = ''
  searchedContacts.value = []
}

const handleContactSearch = async () => {
  const keyword = contactSearchKeyword.value.trim()
  if (!keyword || contactSearchLoading.value) {
    return
  }

  contactSearchLoading.value = true
  try {
    searchedContacts.value = await fetchContactSearchResults(keyword)
    appliedContactSearchKeyword.value = keyword
  } finally {
    contactSearchLoading.value = false
  }
}

const closePanel = () => {
  panelContentReady.value = false
  visible.value = false
}

const sendCurrentMessage = async () => {
  const content = draft.value.trim()
  if (!content || !canSendMessage.value) return

  sending.value = true
  try {
    const payload = activeRoom.value.type === 'global'
      ? { roomType: 'global', content }
      : { roomType: 'private', targetUserId: activeRoom.value.targetUserId, content }
    const res = await chatApi.sendMessage(payload)
    if (res.data) {
      shouldStickToBottom.value = true
      showJumpToLatest.value = false
      applyIncomingMessage(res.data)
    }
    draft.value = ''
  } catch (error) {
    ElMessage.error(getRequestErrorMessage(error, '消息发送失败'))
  } finally {
    sending.value = false
  }
}

const handleComposerKeydown = (event) => {
  if (event.key !== 'Enter') return
  if (!(event.ctrlKey || event.metaKey)) return
  event.preventDefault()
  sendCurrentMessage()
}

const contactStatusText = (contact) => {
  if (contact.blockedByMe) return '你已拉黑对方'
  if (contact.blockedByOther) return '对方已将你拉黑'
  if (contact.friend) return '你们已是好友'
  if (contact.outgoingRequestPending) return '好友申请已发出'
  if (contact.incomingRequestPending) return '等待你处理好友申请'
  if (isElevatedRole(contact.role)) return `${getRoleLabel(contact)}可直接私聊`
  return '可发起好友申请'
}

watch(
  () => userStore.token,
  async (token) => {
    if (!token) {
      closeSocket()
      globalRoomName.value = '全站聊天室'
      globalUnreadCount.value = 0
      contacts.value = []
      conversations.value = []
      incomingRequests.value = []
      outgoingRequests.value = []
      globalMessages.value = []
      privateMessages.value = {}
      privateHasMore.value = {}
      globalHasMore.value = true
      activeRoom.value = { type: 'global', targetUserId: null }
      contactSearchKeyword.value = ''
      appliedContactSearchKeyword.value = ''
      searchedContacts.value = []
      contactSearchLoading.value = false
      showJumpToLatest.value = false
      shouldStickToBottom.value = true
      panelContentReady.value = false
      visible.value = false
      return
    }

    await ensureBootstrap()
    await connectSocket()
  },
  { immediate: true }
)

watch(
  () => currentMessages.value.length,
  async () => {
    if (visible.value && shouldStickToBottom.value) {
      await scrollToBottom()
    }
  }
)

onMounted(async () => {
  if (!userStore.isLoggedIn) return
  await ensureBootstrap()
  await connectSocket()
})

onUnmounted(() => {
  closeSocket()
})
</script>

<style scoped>
.chat-widget {
  position: fixed;
  inset: 0;
  z-index: 1200;
  pointer-events: none;
}

.chat-backdrop {
  position: absolute;
  inset: 0;
  background:
    radial-gradient(circle at top right, rgba(57, 197, 187, 0.12), transparent 32%),
    rgba(15, 23, 42, 0.12);
  backdrop-filter: blur(8px);
  -webkit-backdrop-filter: blur(8px);
  pointer-events: auto;
}

.chat-anchor {
  position: absolute;
  right: 22px;
  bottom: 22px;
  left: auto;
  display: flex;
  align-items: flex-end;
  justify-content: flex-end;
  pointer-events: none;
}

.chat-fab {
  position: absolute;
  right: 0;
  bottom: 0;
  display: inline-flex;
  align-items: center;
  gap: 10px;
  width: max-content;
  max-width: calc(100vw - 24px);
  padding: 14px 18px;
  border: none;
  border-radius: 999px;
  background: linear-gradient(135deg, var(--accent), color-mix(in srgb, var(--accent-secondary) 78%, #ffffff));
  color: #fff;
  box-shadow: 0 18px 40px rgba(57, 197, 187, 0.28);
  cursor: pointer;
  pointer-events: auto;
}

.chat-fab-icon {
  font-size: 1.1rem;
}

.chat-fab-label {
  font-weight: 700;
  letter-spacing: 0.02em;
  white-space: nowrap;
}

.chat-fab-badge,
.chat-room-badge {
  min-width: 22px;
  height: 22px;
  padding: 0 6px;
  border-radius: 999px;
  background: #ef4444;
  color: #fff;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-size: 0.74rem;
  font-weight: 700;
}

.chat-panel {
  position: relative;
  width: min(1100px, calc(100vw - 24px));
  height: min(760px, calc(100vh - 32px));
  background: color-mix(in srgb, var(--bg-card) 90%, transparent);
  border: 1px solid var(--border-color);
  border-radius: 28px;
  backdrop-filter: blur(24px) saturate(160%);
  -webkit-backdrop-filter: blur(24px) saturate(160%);
  box-shadow: 0 26px 64px rgba(15, 23, 42, 0.22);
  overflow: hidden;
  transform-origin: right bottom;
  pointer-events: auto;
}

.chat-panel:not(.chat-panel-ready) .chat-main {
  opacity: 0;
  transform: translateY(12px) scale(0.992);
}

.chat-panel .chat-main {
  transition: opacity 0.22s ease, transform 0.22s ease;
}

.chat-panel-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  padding: 20px 22px 16px;
  border-bottom: 1px solid var(--border-color);
  background: linear-gradient(180deg, color-mix(in srgb, var(--accent-light) 72%, transparent), transparent);
}

.chat-panel-title {
  font-size: 1.1rem;
  font-weight: 800;
  color: var(--text-primary);
}

.chat-panel-subtitle {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  margin-top: 6px;
  color: var(--text-secondary);
  font-size: 0.88rem;
}

.chat-connection-dot {
  width: 9px;
  height: 9px;
  border-radius: 999px;
  background: #f59e0b;
  box-shadow: 0 0 0 6px rgba(245, 158, 11, 0.14);
}

.chat-connection-dot.online {
  background: #22c55e;
  box-shadow: 0 0 0 6px rgba(34, 197, 94, 0.14);
}

.chat-panel-actions {
  display: flex;
  gap: 8px;
}

.chat-icon-btn {
  width: 38px;
  height: 38px;
  border: 1px solid var(--border-color);
  border-radius: 12px;
  background: color-mix(in srgb, var(--bg-card) 84%, transparent);
  color: var(--text-secondary);
  font-size: 1.1rem;
  cursor: pointer;
}

.chat-panel-body {
  display: grid;
  grid-template-columns: 360px minmax(0, 1fr);
  height: calc(100% - 78px);
  min-height: 0;
}

.chat-sidebar {
  padding: 18px;
  border-right: 1px solid var(--border-color);
  overflow: auto;
  background: color-mix(in srgb, var(--bg-tertiary) 60%, transparent);
  min-height: 0;
}

.chat-sidebar-section {
  margin-top: 18px;
}

.chat-section-title {
  margin-bottom: 10px;
  color: var(--text-secondary);
  font-size: 0.8rem;
  font-weight: 700;
  letter-spacing: 0.08em;
}

.chat-section-empty {
  padding: 12px 14px;
  border: 1px dashed var(--border-color);
  border-radius: 16px;
  color: var(--text-tertiary);
  font-size: 0.84rem;
  background: color-mix(in srgb, var(--bg-card) 72%, transparent);
}

.chat-tool-grid {
  display: grid;
  gap: 12px;
}

.chat-tool-field {
  display: grid;
  gap: 8px;
  color: var(--text-secondary);
  font-size: 0.82rem;
}

.chat-select,
.chat-search-input,
.chat-input {
  width: 100%;
  border: 1px solid var(--border-color);
  border-radius: 14px;
  background: color-mix(in srgb, var(--bg-card) 92%, transparent);
  color: var(--text-primary);
  font: inherit;
  outline: none;
}

.chat-select,
.chat-search-input {
  padding: 10px 12px;
}

.chat-search-bar {
  display: flex;
  gap: 8px;
}

.chat-search-results {
  margin-top: 12px;
  display: grid;
  gap: 8px;
}

.chat-search-result,
.chat-room-item {
  width: 100%;
  border: 1px solid transparent;
  border-radius: 18px;
  background: color-mix(in srgb, var(--bg-card) 80%, transparent);
  color: var(--text-primary);
  cursor: pointer;
}

.chat-search-result {
  padding: 12px;
  text-align: left;
}

.chat-search-title {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  font-weight: 700;
  font-size: 0.88rem;
}

.chat-search-time,
.chat-room-time {
  color: var(--text-tertiary);
  font-size: 0.74rem;
}

.chat-search-meta {
  margin-top: 4px;
  color: var(--text-secondary);
  font-size: 0.8rem;
}

.chat-room-item {
  display: grid;
  grid-template-columns: 44px minmax(0, 1fr) auto;
  gap: 12px;
  align-items: center;
  padding: 12px;
  margin-bottom: 10px;
  text-align: left;
}

.chat-room-item.active {
  border-color: color-mix(in srgb, var(--accent) 30%, transparent);
  background: color-mix(in srgb, var(--accent-light) 78%, var(--bg-card));
  box-shadow: 0 14px 28px rgba(57, 197, 187, 0.12);
}

.chat-room-avatar {
  width: 44px;
  height: 44px;
  border-radius: 16px;
  object-fit: cover;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  background: color-mix(in srgb, var(--accent-light) 86%, var(--bg-card));
  color: var(--accent);
  font-weight: 800;
  border: 1px solid color-mix(in srgb, var(--border-color) 90%, transparent);
}

.chat-room-avatar.global {
  background: linear-gradient(135deg, var(--accent), color-mix(in srgb, var(--accent-secondary) 82%, #ffffff));
  color: #fff;
}

.chat-room-main {
  min-width: 0;
}

.chat-room-name {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
  font-size: 0.92rem;
  font-weight: 700;
  color: var(--text-primary);
}

.chat-room-meta {
  margin-top: 4px;
  color: var(--text-secondary);
  font-size: 0.8rem;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.chat-room-side {
  display: grid;
  justify-items: end;
  gap: 6px;
}

.chat-role-chip {
  display: inline-flex;
  align-items: center;
  padding: 2px 8px;
  border-radius: 999px;
  background: rgba(59, 130, 246, 0.12);
  color: #2563eb;
  font-size: 0.72rem;
  font-weight: 700;
}

.chat-role-chip.friend {
  background: rgba(34, 197, 94, 0.12);
  color: #16a34a;
}

.chat-role-chip.blocked {
  background: rgba(239, 68, 68, 0.12);
  color: #dc2626;
}

.chat-card {
  padding: 12px;
  margin-bottom: 10px;
  border-radius: 18px;
  border: 1px solid var(--border-color);
  background: color-mix(in srgb, var(--bg-card) 82%, transparent);
}

.chat-card-top {
  margin-bottom: 6px;
}

.chat-card-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 0.92rem;
  font-weight: 700;
  color: var(--text-primary);
}

.chat-card-meta {
  color: var(--text-secondary);
  font-size: 0.8rem;
  line-height: 1.5;
}

.chat-card-actions,
.chat-conversation-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 10px;
}

.chat-mini-btn,
.chat-send-btn,
.chat-link-btn {
  border: none;
  border-radius: 12px;
  cursor: pointer;
  font-weight: 700;
  font-size: 0.8rem;
}

.chat-mini-btn,
.chat-link-btn {
  padding: 9px 12px;
  background: color-mix(in srgb, var(--bg-card) 84%, var(--accent-light));
  color: var(--text-primary);
  border: 1px solid var(--border-color);
}

.chat-mini-btn.primary,
.chat-send-btn {
  background: linear-gradient(135deg, var(--accent), color-mix(in srgb, var(--accent-secondary) 82%, #ffffff));
  color: #fff;
  border-color: transparent;
}

.chat-mini-btn.danger {
  background: rgba(239, 68, 68, 0.12);
  color: #dc2626;
  border-color: rgba(239, 68, 68, 0.22);
}

.chat-mini-btn:disabled,
.chat-send-btn:disabled {
  opacity: 0.55;
  cursor: not-allowed;
}

.chat-main {
  display: grid;
  grid-template-rows: auto minmax(0, 1fr) auto;
  min-width: 0;
  min-height: 0;
  position: relative;
}

.chat-conversation-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  padding: 18px 22px 14px;
  border-bottom: 1px solid var(--border-color);
}

.chat-conversation-title {
  font-size: 1rem;
  font-weight: 800;
  color: var(--text-primary);
}

.chat-conversation-meta {
  margin-top: 6px;
  color: var(--text-secondary);
  font-size: 0.86rem;
}

.chat-message-list {
  padding: 18px 22px 8px;
  overflow-y: auto;
  overflow-x: hidden;
  min-height: 0;
}

.chat-message-list::-webkit-scrollbar,
.chat-sidebar::-webkit-scrollbar {
  width: 8px;
}

.chat-message-list::-webkit-scrollbar-thumb,
.chat-sidebar::-webkit-scrollbar-thumb {
  background: color-mix(in srgb, var(--accent) 28%, transparent);
  border-radius: 999px;
}

.chat-message-list::-webkit-scrollbar-track,
.chat-sidebar::-webkit-scrollbar-track {
  background: transparent;
}

.chat-history-tip {
  position: sticky;
  top: 0;
  z-index: 2;
  margin: 0 auto 12px;
  width: fit-content;
  padding: 8px 12px;
  border-radius: 999px;
  background: color-mix(in srgb, var(--bg-card) 94%, transparent);
  border: 1px solid var(--border-color);
  color: var(--text-secondary);
  font-size: 0.78rem;
}

.chat-scroll-actions {
  position: absolute;
  right: 22px;
  bottom: 108px;
  z-index: 3;
}

.chat-jump-btn {
  padding: 10px 14px;
  border: none;
  border-radius: 999px;
  background: color-mix(in srgb, var(--bg-card) 96%, transparent);
  color: var(--text-primary);
  border: 1px solid var(--border-color);
  box-shadow: 0 12px 24px rgba(15, 23, 42, 0.12);
  cursor: pointer;
  font-size: 0.8rem;
  font-weight: 700;
}

.chat-state {
  min-height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  text-align: center;
  color: var(--text-tertiary);
  font-size: 0.92rem;
  line-height: 1.7;
}

.chat-message {
  display: flex;
  align-items: flex-end;
  gap: 10px;
  margin-bottom: 14px;
}

.chat-message.mine {
  justify-content: flex-end;
}

.chat-message-avatar {
  width: 34px;
  height: 34px;
  border-radius: 12px;
  object-fit: cover;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  background: color-mix(in srgb, var(--bg-tertiary) 88%, transparent);
  color: var(--text-primary);
  font-size: 0.85rem;
  font-weight: 700;
}

.chat-message-body {
  display: flex;
  flex-direction: column;
  max-width: min(78%, 560px);
}

.chat-message.mine .chat-message-body {
  align-items: flex-end;
}

.chat-message-bubble {
  width: 100%;
  padding: 12px 14px 10px;
  border-radius: 18px 18px 18px 8px;
  background: color-mix(in srgb, var(--bg-card) 92%, transparent);
  border: 1px solid var(--border-color);
  box-shadow: 0 10px 26px rgba(15, 23, 42, 0.06);
}

.chat-message.mine .chat-message-bubble {
  border-radius: 18px 18px 8px 18px;
  background: linear-gradient(135deg, var(--accent), color-mix(in srgb, var(--accent-secondary) 84%, #ffffff));
  color: #fff;
  border-color: transparent;
}

.chat-message-author {
  display: inline-flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 6px;
  font-size: 0.8rem;
  font-weight: 700;
  color: color-mix(in srgb, var(--text-primary) 82%, var(--accent) 18%);
}

.chat-message.mine .chat-message-author {
  justify-content: flex-end;
}

.chat-message.mine .chat-role-chip {
  background: color-mix(in srgb, var(--accent-light) 78%, var(--bg-card));
  color: color-mix(in srgb, var(--accent) 78%, var(--text-primary) 22%);
  border: 1px solid color-mix(in srgb, var(--accent) 24%, var(--border-color));
}

.chat-message-content {
  white-space: pre-wrap;
  word-break: break-word;
  line-height: 1.65;
  font-size: 0.94rem;
}

.chat-message-time {
  margin-top: 6px;
  display: inline-flex;
  align-items: center;
  padding: 4px 10px;
  border-radius: 999px;
  background: color-mix(in srgb, var(--bg-card) 94%, var(--accent-light));
  border: 1px solid color-mix(in srgb, var(--border-color) 76%, var(--accent) 24%);
  color: color-mix(in srgb, var(--text-primary) 82%, var(--accent) 18%);
  font-size: 0.74rem;
  font-weight: 600;
  line-height: 1;
  box-shadow: 0 6px 18px rgba(15, 23, 42, 0.06);
}

.chat-message.mine .chat-message-time {
  background: color-mix(in srgb, var(--accent) 18%, var(--bg-card));
  border-color: color-mix(in srgb, var(--accent) 28%, var(--border-color));
  color: color-mix(in srgb, #ffffff 68%, var(--text-primary) 32%);
}

.chat-composer {
  padding: 14px 18px 18px;
  border-top: 1px solid var(--border-color);
  background: color-mix(in srgb, var(--bg-card) 84%, transparent);
}

.chat-input {
  resize: none;
  padding: 14px 16px;
}

.chat-input:focus,
.chat-select:focus,
.chat-search-input:focus {
  border-color: color-mix(in srgb, var(--accent) 55%, transparent);
  box-shadow: 0 0 0 4px color-mix(in srgb, var(--accent-light) 90%, transparent);
}

.chat-composer-actions {
  margin-top: 12px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 14px;
}

.chat-composer-hint {
  color: var(--text-secondary);
  font-size: 0.82rem;
}

.chat-send-btn {
  min-width: 88px;
  padding: 11px 16px;
}

.chat-backdrop-enter-active {
  transition: opacity 0.2s ease, backdrop-filter 0.2s ease;
}

.chat-backdrop-leave-active {
  transition: opacity 0.01s linear, backdrop-filter 0.01s linear;
}

.chat-backdrop-enter-from,
.chat-backdrop-leave-to {
  opacity: 0;
  backdrop-filter: blur(0);
  -webkit-backdrop-filter: blur(0);
}

.chat-backdrop-enter-to,
.chat-backdrop-leave-from {
  opacity: 1;
  backdrop-filter: blur(8px);
  -webkit-backdrop-filter: blur(8px);
}

.chat-fab-enter-active,
.chat-fab-leave-active,
.chat-panel-enter-active,
.chat-panel-leave-active {
  transition:
    opacity 0.32s cubic-bezier(0.22, 1, 0.36, 1),
    transform 0.32s cubic-bezier(0.22, 1, 0.36, 1),
    filter 0.32s cubic-bezier(0.22, 1, 0.36, 1);
}

.chat-fab-enter-from,
.chat-fab-leave-to,
.chat-panel-enter-from,
.chat-panel-leave-to {
  opacity: 0;
  transform: translateY(28px) scale(0.94);
  filter: blur(10px);
}

.chat-panel-enter-to,
.chat-panel-leave-from {
  opacity: 1;
  transform: translateY(0) scale(1);
  filter: blur(0);
}

@media (max-width: 900px) {
  .chat-anchor {
    right: 12px;
    bottom: 12px;
    left: 12px;
  }

  .chat-panel {
    width: 100%;
    height: min(88vh, 760px);
    border-radius: 24px;
  }

  .chat-panel-body {
    grid-template-columns: 1fr;
  }

  .chat-sidebar {
    max-height: 300px;
    border-right: none;
    border-bottom: 1px solid var(--border-color);
  }

  .chat-conversation-header,
  .chat-composer-actions,
  .chat-search-bar {
    flex-direction: column;
    align-items: stretch;
  }

  .chat-room-side {
    justify-items: start;
  }
}
</style>
