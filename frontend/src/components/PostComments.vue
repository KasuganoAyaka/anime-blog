<template>
  <section class="comments-card">
    <header class="comments-head">
      <div class="comments-title">
        <div class="comments-icon">&#9998;</div>
        <div>
          <h2>{{ copy.title }}</h2>
          <p>{{ copy.subtitle(totalCommentCount) }}</p>
        </div>
      </div>
      <span class="comments-count">{{ totalCommentCount }}</span>
    </header>

    <Transition
      @enter="handleComposerEnter"
      @leave="handleComposerLeave"
    >
    <div v-if="!replyTarget" :ref="setComposerCardRef" class="composer-card" :class="{ focused: composerFocused }">
      <div class="composer-top">
        <div class="mode-switch">
          <button
            v-if="!userStore.isLoggedIn"
            type="button"
            class="chip login-chip"
            @click="goLogin"
          >
            {{ copy.loginAction }}
          </button>
          <button
            v-if="!userStore.isLoggedIn"
            type="button"
            class="chip"
            :class="{ active: guestMode && !anonymousMode }"
            @click="toggleGuestMode"
          >
            {{ copy.guestMode }}
          </button>
          <button
            v-if="!userStore.isLoggedIn"
            type="button"
            class="chip"
            :class="{ active: anonymousMode }"
            @click="toggleAnonymous"
          >
            {{ anonymousMode ? copy.anonymousOn : copy.anonymousOff }}
          </button>
          <div v-else class="identity-card">
            <div class="identity-avatar" :class="{ image: !!currentUserAvatar }">
              <img v-if="currentUserAvatar" :src="currentUserAvatar" :alt="currentUserDisplayName" />
              <span v-else>{{ currentUserInitial }}</span>
            </div>
            <div class="identity-info">
              <strong>{{ currentUserDisplayName }}</strong>
              <span>{{ currentUserRoleLabel }}</span>
            </div>
          </div>
        </div>
      </div>

      <div v-if="anonymousMode" class="anonymous-alias-row">
        <div class="anonymous-alias-card">
          <span class="anonymous-alias-label">{{ copy.anonymousAliasLabel }}</span>
          <strong>{{ anonymousAlias }}</strong>
        </div>
        <button type="button" class="chip" @click="refreshAnonymousAlias">{{ copy.refreshAlias }}</button>
      </div>

      <div v-if="guestMode && !anonymousMode" class="guest-grid">
        <input
          v-model="guestForm.authorName"
          class="field-input"
          :placeholder="copy.nicknamePlaceholder"
          maxlength="100"
        />
        <input
          v-model="guestForm.authorEmail"
          class="field-input"
          :placeholder="copy.emailPlaceholder"
          maxlength="100"
        />
        <input
          v-model="guestForm.authorWebsite"
          class="field-input"
          :placeholder="copy.websitePlaceholder"
          maxlength="255"
        />
      </div>

      <textarea
        :ref="setComposerRef"
        v-model="draft"
        class="composer-input"
        :maxlength="commentLimit"
        :placeholder="replyTarget ? copy.replyPlaceholder : copy.placeholder"
        @paste="handleComposerPaste"
        @focus="composerFocused = true"
        @blur="composerFocused = false"
      />

      <div v-if="draftImages.length" class="image-list">
        <div v-for="(image, index) in draftImages" :key="`${image}-${index}`" class="image-chip">
          <img :src="image" alt="comment attachment" />
          <button type="button" class="image-remove" @click="removeDraftImage(index)">&times;</button>
        </div>
      </div>

      <div v-if="commentTurnstileEnabled" class="turnstile-row">
        <TurnstileWidget
          ref="commentTurnstileRef"
          v-model="commentTurnstileToken"
          :enabled="commentTurnstileEnabled"
          :site-key="turnstileConfig.siteKey"
          action="guest_comment"
          :helper-text="copy.turnstileHint"
        />
      </div>

      <div class="composer-actions">
        <div class="toolbar-shell">
          <div v-if="showEmojiPanel" :ref="setEmojiPanelRef" class="emoji-panel" @mousedown.stop>
            <button v-for="emoji in emojiOptions" :key="emoji" type="button" class="emoji-btn" @click="appendEmoji(emoji)">
              {{ emoji }}
            </button>
          </div>

          <div class="tool-row">
            <div class="tool-buttons">
              <button
                :ref="setEmojiToggleRef"
                type="button"
                class="tool-btn"
                :class="{ active: showEmojiPanel }"
                :title="copy.emojiAction"
                @click="toggleEmojiPanel"
              >
                <svg viewBox="0 0 24 24" aria-hidden="true">
                  <circle cx="12" cy="12" r="8" />
                  <path d="M9.5 10.2h.01" />
                  <path d="M14.5 10.2h.01" />
                  <path d="M8.8 14.1c.9 1.3 2.2 1.9 3.2 1.9s2.3-.6 3.2-1.9" />
                </svg>
                <span class="sr-only">{{ copy.emojiAction }}</span>
              </button>

              <button
                type="button"
                class="tool-btn"
                :title="copy.imageAction"
                :disabled="uploadingImages || draftImages.length >= imageLimit"
                @click="openImagePicker"
              >
                <svg viewBox="0 0 24 24" aria-hidden="true">
                  <path d="M4.5 7.5a2 2 0 0 1 2-2h11a2 2 0 0 1 2 2v9a2 2 0 0 1-2 2h-11a2 2 0 0 1-2-2z" />
                  <path d="M8.5 13.5 10.8 11l2.3 2.3 2.1-1.8 3.3 3" />
                  <circle cx="9" cy="9.2" r="1.1" />
                </svg>
                <span class="sr-only">{{ copy.imageAction }}</span>
              </button>

              <input
                :ref="setImageInputRef"
                class="sr-only"
                type="file"
                accept="image/jpeg,image/png,image/webp,image/gif"
                multiple
                @change="handleLocalImageSelect"
              />
            </div>

            <div class="tool-meta">
              <span v-if="uploadingImages" class="upload-state">{{ copy.uploadingImages }}</span>
              <span class="counter">{{ draft.length }}/{{ commentLimit }}</span>
            </div>
          </div>
        </div>

        <button type="button" class="submit-btn" :disabled="submitting" @click="submitComment">
          {{ submitting ? copy.submitting : (replyTarget ? copy.replyAction : copy.submitAction) }}
        </button>
      </div>

    </div>
    </Transition>

    <div v-if="loading" class="state-block">{{ copy.loading }}</div>

    <div v-else-if="comments.length" class="comments-list">
      <article v-for="comment in comments" :key="comment.id" class="comment-item">
        <div class="avatar" :class="{ image: !!comment.authorAvatar }">
          <img v-if="comment.authorAvatar" :src="comment.authorAvatar" :alt="comment.authorName" />
          <span v-else>{{ comment.authorName.slice(0, 1).toUpperCase() }}</span>
        </div>

        <div class="comment-main">
          <div class="comment-card-head">
            <div class="comment-meta-main">
              <div class="author-row">
                <strong>{{ comment.authorName }}</strong>
                <a v-if="comment.authorWebsite" :href="comment.authorWebsite" target="_blank" rel="noreferrer" class="author-link">
                  {{ copy.websiteLabel }}
                </a>
                <span v-if="comment.authorBadge" class="badge">{{ comment.authorBadge }}</span>
                <span v-if="comment.anonymous" class="badge ghost">{{ copy.anonymousBadge }}</span>
              </div>
              <span class="comment-date">{{ formatCommentDate(comment.createdAt) }}</span>
            </div>
            <button type="button" class="reply-bubble" @click="setReplyTarget(comment)">
              <svg viewBox="0 0 24 24" aria-hidden="true">
                <path d="M19 15a3 3 0 0 1-3 3H9l-4 3v-3H5a3 3 0 0 1-3-3V8a3 3 0 0 1 3-3h11a3 3 0 0 1 3 3Z" />
              </svg>
              <span v-if="replyCount(comment)">{{ replyCount(comment) }}</span>
            </button>
          </div>

          <div class="comment-content">
            <p v-for="(paragraph, index) in formatCommentParagraphs(comment.content)" :key="`${comment.id}-${index}`">
              {{ paragraph }}
            </p>
          </div>

          <div v-if="comment.images.length" class="comment-images">
            <a v-for="(image, index) in comment.images" :key="`${comment.id}-${index}`" :href="image" target="_blank" rel="noreferrer" class="image-link">
              <img :src="image" alt="comment attachment" />
            </a>
          </div>

          <div class="comment-foot" :class="{ compact: !comment.images.length }">
            <div v-if="getCommentClientTags(comment).length" class="comment-client-tags">
              <span v-for="tag in getCommentClientTags(comment)" :key="`${comment.id}-${tag.kind}-${tag.key}`" class="client-tag">
                <svg viewBox="0 0 24 24" aria-hidden="true">
                  <component :is="node.tag" v-for="(node, index) in getClientTagIcon(tag)" :key="`${tag.key}-${index}`" v-bind="node.attrs" />
                </svg>
                <span>{{ tag.label }}</span>
              </span>
            </div>
            <div class="comment-foot-actions">
              <button v-if="canManageCommentActions" type="button" class="meta-action" @click="deleteComment(comment)">{{ copy.deleteAction }}</button>
              <button type="button" class="meta-action" @click="openReportDialog(comment)">{{ copy.reportAction }}</button>
              <span class="meta-chip status-chip">{{ comment.images.length ? copy.imageCount(comment.images.length) : copy.postedLabel }}</span>
            </div>
          </div>

          <Transition
            @enter="handleComposerEnter"
            @leave="handleComposerLeave"
          >
          <div v-if="isReplyingTo(comment)" :ref="setComposerCardRef" class="composer-card reply-composer-card" :class="{ focused: composerFocused, inline: true }">
            <div class="reply-banner inline">
              <div>
                <strong>{{ copy.replyingTo(comment.authorName) }}</strong>
                <p>{{ replyPreview }}</p>
              </div>
              <button type="button" class="chip" @click="clearReplyTarget({ focus: true })">{{ copy.cancelReply }}</button>
            </div>

            <div class="composer-top">
              <div class="mode-switch">
                <button
                  v-if="!userStore.isLoggedIn"
                  type="button"
                  class="chip login-chip"
                  @click="goLogin"
                >
                  {{ copy.loginAction }}
                </button>
                <button
                  v-if="!userStore.isLoggedIn"
                  type="button"
                  class="chip"
                  :class="{ active: guestMode && !anonymousMode }"
                  @click="toggleGuestMode"
                >
                  {{ copy.guestMode }}
                </button>
                <button
                  v-if="!userStore.isLoggedIn"
                  type="button"
                  class="chip"
                  :class="{ active: anonymousMode }"
                  @click="toggleAnonymous"
                >
                  {{ anonymousMode ? copy.anonymousOn : copy.anonymousOff }}
                </button>
                <div v-else class="identity-card">
                  <div class="identity-avatar" :class="{ image: !!currentUserAvatar }">
                    <img v-if="currentUserAvatar" :src="currentUserAvatar" :alt="currentUserDisplayName" />
                    <span v-else>{{ currentUserInitial }}</span>
                  </div>
                  <div class="identity-info">
                    <strong>{{ currentUserDisplayName }}</strong>
                    <span>{{ currentUserRoleLabel }}</span>
                  </div>
                </div>
              </div>
            </div>

            <div v-if="anonymousMode" class="anonymous-alias-row">
              <div class="anonymous-alias-card">
                <span class="anonymous-alias-label">{{ copy.anonymousAliasLabel }}</span>
                <strong>{{ anonymousAlias }}</strong>
              </div>
              <button type="button" class="chip" @click="refreshAnonymousAlias">{{ copy.refreshAlias }}</button>
            </div>

            <div v-if="guestMode && !anonymousMode" class="guest-grid">
              <input
                v-model="guestForm.authorName"
                class="field-input"
                :placeholder="copy.nicknamePlaceholder"
                maxlength="100"
              />
              <input
                v-model="guestForm.authorEmail"
                class="field-input"
                :placeholder="copy.emailPlaceholder"
                maxlength="100"
              />
              <input
                v-model="guestForm.authorWebsite"
                class="field-input"
                :placeholder="copy.websitePlaceholder"
                maxlength="255"
              />
            </div>

            <textarea
              :ref="setComposerRef"
              v-model="draft"
              class="composer-input"
              :maxlength="commentLimit"
              :placeholder="copy.replyPlaceholder"
              @paste="handleComposerPaste"
              @focus="composerFocused = true"
              @blur="composerFocused = false"
            />

            <div v-if="draftImages.length" class="image-list">
              <div v-for="(image, index) in draftImages" :key="`${image}-${index}`" class="image-chip">
                <img :src="image" alt="comment attachment" />
                <button type="button" class="image-remove" @click="removeDraftImage(index)">&times;</button>
              </div>
            </div>

            <div v-if="commentTurnstileEnabled" class="turnstile-row">
              <TurnstileWidget
                ref="commentTurnstileRef"
                v-model="commentTurnstileToken"
                :enabled="commentTurnstileEnabled"
                :site-key="turnstileConfig.siteKey"
                action="guest_comment"
                :helper-text="copy.turnstileHint"
              />
            </div>

            <div class="composer-actions">
              <div class="toolbar-shell">
                <div v-if="showEmojiPanel" :ref="setEmojiPanelRef" class="emoji-panel" @mousedown.stop>
                  <button v-for="emoji in emojiOptions" :key="emoji" type="button" class="emoji-btn" @click="appendEmoji(emoji)">
                    {{ emoji }}
                  </button>
                </div>

                <div class="tool-row">
                  <div class="tool-buttons">
                    <button
                      :ref="setEmojiToggleRef"
                      type="button"
                      class="tool-btn"
                      :class="{ active: showEmojiPanel }"
                      :title="copy.emojiAction"
                      @click="toggleEmojiPanel"
                    >
                      <svg viewBox="0 0 24 24" aria-hidden="true">
                        <circle cx="12" cy="12" r="8" />
                        <path d="M9.5 10.2h.01" />
                        <path d="M14.5 10.2h.01" />
                        <path d="M8.8 14.1c.9 1.3 2.2 1.9 3.2 1.9s2.3-.6 3.2-1.9" />
                      </svg>
                      <span class="sr-only">{{ copy.emojiAction }}</span>
                    </button>

                    <button
                      type="button"
                      class="tool-btn"
                      :title="copy.imageAction"
                      :disabled="uploadingImages || draftImages.length >= imageLimit"
                      @click="openImagePicker"
                    >
                      <svg viewBox="0 0 24 24" aria-hidden="true">
                        <path d="M4.5 7.5a2 2 0 0 1 2-2h11a2 2 0 0 1 2 2v9a2 2 0 0 1-2 2h-11a2 2 0 0 1-2-2z" />
                        <path d="M8.5 13.5 10.8 11l2.3 2.3 2.1-1.8 3.3 3" />
                        <circle cx="9" cy="9.2" r="1.1" />
                      </svg>
                      <span class="sr-only">{{ copy.imageAction }}</span>
                    </button>

                    <input
                      :ref="setImageInputRef"
                      class="sr-only"
                      type="file"
                      accept="image/jpeg,image/png,image/webp,image/gif"
                      multiple
                      @change="handleLocalImageSelect"
                    />
                  </div>

                  <div class="tool-meta">
                    <span v-if="uploadingImages" class="upload-state">{{ copy.uploadingImages }}</span>
                    <span class="counter">{{ draft.length }}/{{ commentLimit }}</span>
                  </div>
                </div>
              </div>

              <button type="button" class="submit-btn" :disabled="submitting" @click="submitComment">
                {{ submitting ? copy.submitting : copy.replyAction }}
              </button>
            </div>

          </div>
          </Transition>

          <div v-if="flattenReplies(comment.replies).length" class="reply-list">
            <article
              v-for="reply in flattenReplies(comment.replies)"
              :key="reply.id"
              class="reply-item"
            >
              <div class="avatar reply-avatar" :class="{ image: !!reply.authorAvatar }">
                <img v-if="reply.authorAvatar" :src="reply.authorAvatar" :alt="reply.authorName" />
                <span v-else>{{ reply.authorName.slice(0, 1).toUpperCase() }}</span>
              </div>

              <div class="reply-main">
              <div class="comment-card-head reply-card-head">
                <div class="comment-meta-main">
                  <div class="author-row">
                    <strong>{{ reply.authorName }}</strong>
                    <a v-if="reply.authorWebsite" :href="reply.authorWebsite" target="_blank" rel="noreferrer" class="author-link">
                      {{ copy.websiteLabel }}
                    </a>
                    <span v-if="reply.authorBadge" class="badge">{{ reply.authorBadge }}</span>
                    <span v-if="reply.anonymous" class="badge ghost">{{ copy.anonymousBadge }}</span>
                  </div>
                  <div class="meta-inline-row">
                    <span class="comment-date">{{ formatCommentDate(reply.createdAt) }}</span>
                    <span v-if="reply.replyToAuthorName" class="reply-target">{{ copy.replyTarget(reply.replyToAuthorName) }}</span>
                  </div>
                </div>
                <button type="button" class="reply-bubble compact" @click="setReplyTarget(reply)">
                  <svg viewBox="0 0 24 24" aria-hidden="true">
                    <path d="M19 15a3 3 0 0 1-3 3H9l-4 3v-3H5a3 3 0 0 1-3-3V8a3 3 0 0 1 3-3h11a3 3 0 0 1 3 3Z" />
                  </svg>
                </button>
              </div>

              <div class="comment-content">
                <p v-for="(paragraph, index) in formatCommentParagraphs(reply.content)" :key="`${reply.id}-${index}`">
                  {{ paragraph }}
                </p>
              </div>

              <div v-if="reply.images.length" class="comment-images compact">
                <a v-for="(image, index) in reply.images" :key="`${reply.id}-${index}`" :href="image" target="_blank" rel="noreferrer" class="image-link">
                  <img :src="image" alt="reply attachment" />
                </a>
              </div>

              <div class="comment-foot compact" :class="{ dense: !reply.images.length }">
                <div v-if="getCommentClientTags(reply).length" class="comment-client-tags compact">
                  <span v-for="tag in getCommentClientTags(reply)" :key="`${reply.id}-${tag.kind}-${tag.key}`" class="client-tag">
                    <svg viewBox="0 0 24 24" aria-hidden="true">
                      <component :is="node.tag" v-for="(node, index) in getClientTagIcon(tag)" :key="`${tag.key}-${index}`" v-bind="node.attrs" />
                    </svg>
                    <span>{{ tag.label }}</span>
                  </span>
                </div>
                <div class="comment-foot-actions">
                  <button v-if="canManageCommentActions" type="button" class="meta-action" @click="deleteComment(reply)">{{ copy.deleteAction }}</button>
                  <button type="button" class="meta-action" @click="openReportDialog(reply)">{{ copy.reportAction }}</button>
                  <span class="meta-chip status-chip">{{ reply.images.length ? copy.imageCount(reply.images.length) : copy.postedLabel }}</span>
                </div>
              </div>

              <Transition
                @enter="handleComposerEnter"
                @leave="handleComposerLeave"
              >
              <div v-if="isReplyingTo(reply)" :ref="setComposerCardRef" class="composer-card reply-composer-card" :class="{ focused: composerFocused, inline: true }">
                <div class="reply-banner inline">
                  <div>
                    <strong>{{ copy.replyingTo(reply.authorName) }}</strong>
                    <p>{{ replyPreview }}</p>
                  </div>
                  <button type="button" class="chip" @click="clearReplyTarget({ focus: true })">{{ copy.cancelReply }}</button>
                </div>

                <div class="composer-top">
                  <div class="mode-switch">
                    <button
                      v-if="!userStore.isLoggedIn"
                      type="button"
                      class="chip login-chip"
                      @click="goLogin"
                    >
                      {{ copy.loginAction }}
                    </button>
                    <button
                      v-if="!userStore.isLoggedIn"
                      type="button"
                      class="chip"
                      :class="{ active: guestMode && !anonymousMode }"
                      @click="toggleGuestMode"
                    >
                      {{ copy.guestMode }}
                    </button>
                    <button
                      v-if="!userStore.isLoggedIn"
                      type="button"
                      class="chip"
                      :class="{ active: anonymousMode }"
                      @click="toggleAnonymous"
                    >
                      {{ anonymousMode ? copy.anonymousOn : copy.anonymousOff }}
                    </button>
                    <div v-else class="identity-card">
                      <div class="identity-avatar" :class="{ image: !!currentUserAvatar }">
                        <img v-if="currentUserAvatar" :src="currentUserAvatar" :alt="currentUserDisplayName" />
                        <span v-else>{{ currentUserInitial }}</span>
                      </div>
                      <div class="identity-info">
                        <strong>{{ currentUserDisplayName }}</strong>
                        <span>{{ currentUserRoleLabel }}</span>
                      </div>
                    </div>
                  </div>
                </div>

                <div v-if="anonymousMode" class="anonymous-alias-row">
                  <div class="anonymous-alias-card">
                    <span class="anonymous-alias-label">{{ copy.anonymousAliasLabel }}</span>
                    <strong>{{ anonymousAlias }}</strong>
                  </div>
                  <button type="button" class="chip" @click="refreshAnonymousAlias">{{ copy.refreshAlias }}</button>
                </div>

                <div v-if="guestMode && !anonymousMode" class="guest-grid">
                  <input
                    v-model="guestForm.authorName"
                    class="field-input"
                    :placeholder="copy.nicknamePlaceholder"
                    maxlength="100"
                  />
                  <input
                    v-model="guestForm.authorEmail"
                    class="field-input"
                    :placeholder="copy.emailPlaceholder"
                    maxlength="100"
                  />
                  <input
                    v-model="guestForm.authorWebsite"
                    class="field-input"
                    :placeholder="copy.websitePlaceholder"
                    maxlength="255"
                  />
                </div>

                <textarea
                  :ref="setComposerRef"
                  v-model="draft"
                  class="composer-input"
                  :maxlength="commentLimit"
                  :placeholder="copy.replyPlaceholder"
                  @paste="handleComposerPaste"
                  @focus="composerFocused = true"
                  @blur="composerFocused = false"
                />

                <div v-if="draftImages.length" class="image-list">
                  <div v-for="(image, index) in draftImages" :key="`${image}-${index}`" class="image-chip">
                    <img :src="image" alt="comment attachment" />
                    <button type="button" class="image-remove" @click="removeDraftImage(index)">&times;</button>
                  </div>
                </div>

                <div v-if="commentTurnstileEnabled" class="turnstile-row">
                  <TurnstileWidget
                    ref="commentTurnstileRef"
                    v-model="commentTurnstileToken"
                    :enabled="commentTurnstileEnabled"
                    :site-key="turnstileConfig.siteKey"
                    action="guest_comment"
                    :helper-text="copy.turnstileHint"
                  />
                </div>

                <div class="composer-actions">
                  <div class="toolbar-shell">
                  <div v-if="showEmojiPanel" :ref="setEmojiPanelRef" class="emoji-panel" @mousedown.stop>
                      <button v-for="emoji in emojiOptions" :key="emoji" type="button" class="emoji-btn" @click="appendEmoji(emoji)">
                        {{ emoji }}
                      </button>
                    </div>

                    <div class="tool-row">
                      <div class="tool-buttons">
                      <button
                        :ref="setEmojiToggleRef"
                          type="button"
                          class="tool-btn"
                          :class="{ active: showEmojiPanel }"
                          :title="copy.emojiAction"
                          @click="toggleEmojiPanel"
                        >
                          <svg viewBox="0 0 24 24" aria-hidden="true">
                            <circle cx="12" cy="12" r="8" />
                            <path d="M9.5 10.2h.01" />
                            <path d="M14.5 10.2h.01" />
                            <path d="M8.8 14.1c.9 1.3 2.2 1.9 3.2 1.9s2.3-.6 3.2-1.9" />
                          </svg>
                          <span class="sr-only">{{ copy.emojiAction }}</span>
                        </button>

                        <button
                          type="button"
                          class="tool-btn"
                          :title="copy.imageAction"
                          :disabled="uploadingImages || draftImages.length >= imageLimit"
                          @click="openImagePicker"
                        >
                          <svg viewBox="0 0 24 24" aria-hidden="true">
                            <path d="M4.5 7.5a2 2 0 0 1 2-2h11a2 2 0 0 1 2 2v9a2 2 0 0 1-2 2h-11a2 2 0 0 1-2-2z" />
                            <path d="M8.5 13.5 10.8 11l2.3 2.3 2.1-1.8 3.3 3" />
                            <circle cx="9" cy="9.2" r="1.1" />
                          </svg>
                          <span class="sr-only">{{ copy.imageAction }}</span>
                        </button>

                      <input
                        :ref="setImageInputRef"
                          class="sr-only"
                          type="file"
                          accept="image/jpeg,image/png,image/webp,image/gif"
                          multiple
                          @change="handleLocalImageSelect"
                        />
                      </div>

                      <div class="tool-meta">
                        <span v-if="uploadingImages" class="upload-state">{{ copy.uploadingImages }}</span>
                        <span class="counter">{{ draft.length }}/{{ commentLimit }}</span>
                      </div>
                    </div>
                  </div>

                  <button type="button" class="submit-btn" :disabled="submitting" @click="submitComment">
                    {{ submitting ? copy.submitting : copy.replyAction }}
                  </button>
                </div>
              </div>
              </Transition>
              </div>
            </article>
          </div>
        </div>
      </article>
    </div>

    <div v-else class="state-block empty">
      <h3>{{ copy.emptyTitle }}</h3>
      <p>{{ copy.emptyDescription }}</p>
    </div>

    <el-dialog
      v-model="showReportDialog"
      :title="copy.reportDialogTitle"
      width="560px"
      destroy-on-close
      class="comment-report-dialog"
      @closed="resetReportState"
    >
      <div class="report-dialog-body">
        <div v-if="reportTarget" class="report-target-card">
          <div class="report-target-label">{{ copy.reportTargetLabel }}</div>
          <div class="report-target-author">{{ reportTarget.authorName }}</div>
          <div class="report-target-content">{{ reportPreview }}</div>
        </div>

        <el-form label-position="top" class="report-form">
          <el-form-item :label="copy.reportReasonLabel" required>
            <el-select
              v-model="reportForm.reasonCode"
              class="report-select"
              :placeholder="copy.reportReasonPlaceholder"
            >
              <el-option
                v-for="option in reportReasonOptions"
                :key="option.value"
                :label="option.label"
                :value="option.value"
              />
            </el-select>
          </el-form-item>

          <el-form-item v-if="reportForm.reasonCode === 'other'" :label="copy.reportOtherLabel" required>
            <el-input
              v-model="reportForm.otherReason"
              :maxlength="100"
              :placeholder="copy.reportOtherPlaceholder"
            />
          </el-form-item>

          <el-form-item :label="copy.reportDescriptionLabel" required>
            <el-input
              v-model="reportForm.description"
              type="textarea"
              :rows="5"
              :maxlength="1000"
              resize="vertical"
              :placeholder="copy.reportDescriptionPlaceholder"
            />
          </el-form-item>
        </el-form>
      </div>

      <template #footer>
        <div class="report-dialog-footer">
          <el-button @click="showReportDialog = false">{{ copy.reportCancel }}</el-button>
          <el-button type="danger" :loading="reportSubmitting" @click="submitReport">
            {{ reportSubmitting ? copy.reportSubmitting : copy.reportSubmit }}
          </el-button>
        </div>
      </template>
    </el-dialog>
  </section>
</template>

<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useI18n } from 'vue-i18n'
import { adminApi, publicApi } from '@/api'
import TurnstileWidget from '@/components/TurnstileWidget.vue'
import { usePostCommentPresentation } from '@/composables/usePostCommentPresentation'
import { useUserStore } from '@/stores'
import { getRequestErrorMessage } from '@/utils/request'
import { getDefaultTurnstileConfig, getTurnstileConfig } from '@/utils/turnstile'

const props = defineProps({
  postId: {
    type: [String, Number],
    required: true
  }
})

const router = useRouter()
const { locale } = useI18n()
const userStore = useUserStore()
userStore.hydrateSession()

const commentLimit = 100000
const imageLimit = 9
const {
  createAnonymousAlias,
  formatCommentDate,
  formatCommentParagraphs,
  getClientTagIcon,
  getCommentClientTags
} = usePostCommentPresentation({ locale })
const emojiOptions = [
  '😀', '😁', '😂', '😃', '😄', '😅', '😆', '😊',
  '😉', '😍', '😘', '😎', '😏', '😇', '😌', '😋',
  '😜', '😝', '😛', '😡', '🤔', '🤗', '🤭', '😳',
  '😶', '😴', '😫', '😷', '😺', '🙈', '😮', '🤩',
  '😕', '😢', '😭', '🥰', '😤', '😥', '😧', '🙌',
  '👍', '👏', '✌️', '✨', '🌟', '🍀', '🎉', '🎈',
  '☀️', '🌙', '💖', '🎀', '🎵', '🎧', '📚', '📎',
  '☕', '🍃', '🍪', '🍜', '🍁', '💅', '💆', '💡',
  '🔆', '🫶', '🤍', '💜', '✍️', '📷', '📝', '🎀'
]
const composerCardRef = ref(null)
const composerRef = ref(null)
const imageInputRef = ref(null)
const emojiPanelRef = ref(null)
const emojiToggleRef = ref(null)
const commentTurnstileRef = ref(null)
const draft = ref('')
const draftImages = ref([])
const comments = ref([])
const guestMode = ref(false)
const anonymousMode = ref(false)
const anonymousAlias = ref('')
const showEmojiPanel = ref(false)
const composerFocused = ref(false)
const loading = ref(false)
const submitting = ref(false)
const uploadingImages = ref(false)
const turnstileConfig = ref(getDefaultTurnstileConfig())
const commentTurnstileToken = ref('')
const replyTarget = ref(null)
const pendingComposerFocusMode = ref('')
const guestForm = ref({
  authorName: '',
  authorEmail: '',
  authorWebsite: ''
})
const showReportDialog = ref(false)
const reportSubmitting = ref(false)
const reportTarget = ref(null)
const reportForm = ref({
  reasonCode: 'spam',
  otherReason: '',
  description: ''
})

const copyMap = {
  'zh-CN': {
    title: '评论区',
    subtitle: (count) => count ? `当前展示 ${count} 条评论` : '登录或填写昵称与邮箱，都可以参与评论',
    placeholder: '写下你的观点、补充、疑问或回应...',
    replyPlaceholder: '正在回复，写下你想说的话...',
    accountMode: '账号身份',
    accountReady: '当前已登录，将以账号身份发表评论',
    guestMode: '其他方式',
    loginAction: '去登录',
    nicknamePlaceholder: '昵称（必填）',
    emailPlaceholder: '邮箱（必填）',
    websitePlaceholder: '网站（选填）',
    emojiAction: '插入表情',
    imageAction: '上传图片',
    anonymousOn: '匿名审核',
    anonymousOff: '匿名评论',
    anonymousAliasLabel: '匿名名称',
    refreshAlias: '换一个',
    reviewHint: '匿名评论或匿名回复提交后，需要管理员审核通过才会显示',
    accountHint: '使用当前登录账号直接发表评论',
    guestHint: '填写昵称和邮箱后即可发表，网站为选填项',
    loginHint: '可先登录，或切换到其他方式 / 匿名发表评论',
    submitAction: '发表评论',
    replyAction: '提交回复',
    submitting: '提交中...',
    loading: '正在加载评论...',
    emptyTitle: '还没有评论',
    emptyDescription: '你可以成为第一个留下想法的人。',
    uploadingImages: '图片上传中...',
    imageUploadSuccess: (count) => count > 1 ? `已上传 ${count} 张图片` : '图片已上传',
    imageUploadFailed: '图片上传失败，请稍后重试',
    imageLimitReached: '评论最多只能添加 9 张图片',
    pasteImageSuccess: (count) => count > 1 ? `已粘贴并上传 ${count} 张图片` : '图片已粘贴并上传',
    emptyDraft: '请先填写评论内容',
    guestRequired: '请先填写昵称和邮箱',
    turnstileHint: '游客评论前请先完成人机验证。',
    turnstileRequired: '请先完成人机验证',
    submitSuccess: '评论已发布',
    submitPending: '已提交，等待管理员审核',
    anonymousBadge: '匿名',
    guestBadge: '游客',
    postedLabel: '已发布',
    imageCount: (count) => `附带 ${count} 张图片`,
    adminBadge: '站长',
    managerBadge: '管理员',
    websiteLabel: '网站',
    replyButton: '回复',
    replyTarget: (name) => `回复 @${name}`,
    replyingTo: (name) => `正在回复 ${name}`,
    cancelReply: '取消回复',
    deleteAction: '删除',
    reportAction: '举报',
    deleteConfirmTitle: '删除评论',
    deleteConfirmMessage: '确定要删除这条评论吗？如果它下面还有回复，也会一并删除。',
    deleteSuccess: '评论已删除',
    deleteFailed: '删除评论失败',
    reportDialogTitle: '举报评论',
    reportTargetLabel: '举报对象',
    reportReasonLabel: '举报原因',
    reportReasonPlaceholder: '请选择举报原因',
    reportOtherLabel: '补充原因',
    reportOtherPlaceholder: '请输入具体原因',
    reportDescriptionLabel: '举报说明',
    reportDescriptionPlaceholder: '请详细说明这条评论的问题，管理员会根据你的说明处理',
    reportCancel: '取消',
    reportSubmit: '提交举报',
    reportSubmitting: '提交中...',
    reportMissingReason: '请选择举报原因',
    reportMissingOtherReason: '请补充具体原因',
    reportMissingDescription: '请填写举报说明',
    reportSubmitSuccess: '举报已提交，感谢反馈',
    reportSubmitFailed: '举报提交失败，请稍后重试',
    reportReasonSpam: '垃圾信息',
    reportReasonAbuse: '辱骂攻击',
    reportReasonAds: '广告引流',
    reportReasonIllegal: '违法违规',
    reportReasonPorn: '色情低俗',
    reportReasonPrivacy: '隐私泄露',
    reportReasonOther: '其他'
  },
  en: {
    title: 'Comments',
    subtitle: (count) => count ? `${count} visible comments` : 'Sign in or fill in a nickname and email to comment',
    placeholder: 'Share a thought, question, or reply...',
    replyPlaceholder: 'Write your reply here...',
    accountMode: 'Account',
    accountReady: 'Signed in and ready to post',
    guestMode: 'Guest form',
    loginAction: 'Log in',
    nicknamePlaceholder: 'Nickname (required)',
    emailPlaceholder: 'Email (required)',
    websitePlaceholder: 'Website (optional)',
    emojiAction: 'Insert emoji',
    imageAction: 'Upload image',
    anonymousOn: 'Anonymous review',
    anonymousOff: 'Anonymous',
    anonymousAliasLabel: 'Anonymous name',
    refreshAlias: 'Refresh',
    reviewHint: 'Anonymous comments and replies must be approved before they appear',
    accountHint: 'Post with your signed-in account',
    guestHint: 'Fill in nickname and email to post as a guest',
    loginHint: 'Log in first, or switch to guest / anonymous posting',
    submitAction: 'Post comment',
    replyAction: 'Post reply',
    submitting: 'Posting...',
    loading: 'Loading comments...',
    emptyTitle: 'No comments yet',
    emptyDescription: 'Be the first one to leave a note here.',
    uploadingImages: 'Uploading images...',
    imageUploadSuccess: (count) => count > 1 ? `${count} images uploaded` : 'Image uploaded',
    imageUploadFailed: 'Image upload failed, please try again',
    imageLimitReached: 'You can attach up to 9 images',
    pasteImageSuccess: (count) => count > 1 ? `${count} pasted images uploaded` : 'Pasted image uploaded',
    emptyDraft: 'Write something before posting',
    guestRequired: 'Please fill in nickname and email first',
    turnstileHint: 'Please complete the human verification before posting as a guest.',
    turnstileRequired: 'Please complete the human verification first',
    submitSuccess: 'Comment posted',
    submitPending: 'Submitted for review',
    anonymousBadge: 'Anonymous',
    guestBadge: 'Guest',
    postedLabel: 'Posted',
    imageCount: (count) => `${count} image${count === 1 ? '' : 's'}`,
    adminBadge: 'Author',
    managerBadge: 'Moderator',
    websiteLabel: 'Site',
    replyButton: 'Reply',
    replyTarget: (name) => `Reply to @${name}`,
    replyingTo: (name) => `Replying to ${name}`,
    cancelReply: 'Cancel',
    deleteAction: 'Delete',
    reportAction: 'Report',
    deleteConfirmTitle: 'Delete Comment',
    deleteConfirmMessage: 'Delete this comment? Any replies under it will also be removed.',
    deleteSuccess: 'Comment deleted',
    deleteFailed: 'Failed to delete comment',
    reportDialogTitle: 'Report Comment',
    reportTargetLabel: 'Reported comment',
    reportReasonLabel: 'Reason',
    reportReasonPlaceholder: 'Select a reason',
    reportOtherLabel: 'Custom reason',
    reportOtherPlaceholder: 'Describe the specific reason',
    reportDescriptionLabel: 'Details',
    reportDescriptionPlaceholder: 'Explain why this comment should be reviewed',
    reportCancel: 'Cancel',
    reportSubmit: 'Submit report',
    reportSubmitting: 'Submitting...',
    reportMissingReason: 'Please select a reason',
    reportMissingOtherReason: 'Please enter the custom reason',
    reportMissingDescription: 'Please explain the issue',
    reportSubmitSuccess: 'Report submitted, thank you for your feedback',
    reportSubmitFailed: 'Report submission failed, please try again later',
    reportReasonSpam: 'Spam',
    reportReasonAbuse: 'Harassment',
    reportReasonAds: 'Advertising',
    reportReasonIllegal: 'Illegal content',
    reportReasonPorn: 'Sexual content',
    reportReasonPrivacy: 'Privacy leak',
    reportReasonOther: 'Other'
  }
}

const copy = computed(() => copyMap[locale.value] || copyMap['zh-CN'])
const totalCommentCount = computed(() => countComments(comments.value))
const commentTurnstileEnabled = computed(() => !userStore.isLoggedIn && turnstileConfig.value.comment.guestCheckEnabled)
const reportReasonOptions = computed(() => ([
  { value: 'spam', label: copy.value.reportReasonSpam },
  { value: 'abuse', label: copy.value.reportReasonAbuse },
  { value: 'ads', label: copy.value.reportReasonAds },
  { value: 'illegal', label: copy.value.reportReasonIllegal },
  { value: 'porn', label: copy.value.reportReasonPorn },
  { value: 'privacy', label: copy.value.reportReasonPrivacy },
  { value: 'other', label: copy.value.reportReasonOther }
]))
const currentUserDisplayName = computed(() => {
  const user = userStore.user || {}
  return String(user.nickname || user.username || '已登录用户')
})
const currentUserAvatar = computed(() => String(userStore.user?.avatar || ''))
const currentUserInitial = computed(() => currentUserDisplayName.value.slice(0, 1).toUpperCase())
const canManageCommentActions = computed(() => ['admin', 'manager'].includes(String(userStore.user?.role || '')))
const currentUserRoleLabel = computed(() => {
  const role = String(userStore.user?.role || 'user')
  const username = String(userStore.user?.username || '').toLowerCase()

  if (role === 'admin' && username === 'admin') {
    return '站长'
  }
  if (role === 'admin' || role === 'manager') {
    return '管理员'
  }
  return '用户'
})

const resetCommentTurnstile = () => {
  commentTurnstileToken.value = ''
  commentTurnstileRef.value?.reset?.()
}
const replyPreview = computed(() => {
  const content = String(replyTarget.value?.content || '').replace(/\s+/g, ' ').trim()
  return content.length > 60 ? `${content.slice(0, 60)}...` : content
})
const reportPreview = computed(() => {
  const content = String(reportTarget.value?.content || '').replace(/\s+/g, ' ').trim()
  return content.length > 120 ? `${content.slice(0, 120)}...` : content
})

const setComposerCardRef = (element) => {
  composerCardRef.value = element || null
}

const setComposerRef = (element) => {
  composerRef.value = element || null
}

const setImageInputRef = (element) => {
  imageInputRef.value = element || null
}

const setEmojiPanelRef = (element) => {
  emojiPanelRef.value = element || null
}

const setEmojiToggleRef = (element) => {
  emojiToggleRef.value = element || null
}

const countComments = (items) => Array.isArray(items)
  ? items.reduce((sum, item) => sum + 1 + countComments(item.replies), 0)
  : 0

const normalizeComments = (items) => Array.isArray(items)
  ? items
    .filter((item) => item && typeof item === 'object' && item.id)
    .map((item) => ({
      id: String(item.id),
      parentId: item.parentId ? String(item.parentId) : '',
      authorName: String(item.authorName || copy.value.guestMode),
      authorAvatar: String(item.authorAvatar || ''),
      authorWebsite: String(item.authorWebsite || ''),
      authorBadge: resolveCommentAuthorBadge(item),
      anonymous: Boolean(item.anonymous),
      status: String(item.status || 'visible'),
      clientRegion: String(item.clientRegion || ''),
      clientOs: String(item.clientOs || ''),
      clientBrowser: String(item.clientBrowser || ''),
      content: String(item.content || ''),
      images: Array.isArray(item.images) ? item.images.filter(Boolean).map(String) : [],
      createdAt: String(item.createdAt || new Date().toISOString()),
      replyToAuthorName: String(item.replyToAuthorName || ''),
      replies: normalizeComments(item.replies || [])
    }))
  : []

const flattenReplies = (items, depth = 1) => Array.isArray(items)
  ? items.flatMap((item) => [{ ...item, depth }, ...flattenReplies(item.replies || [], depth + 1)])
  : []

const resolveCommentAuthorBadge = (item) => {
  const role = String(item?.authorRole || '').toLowerCase()
  if (role === 'admin') {
    return copy.value.adminBadge
  }
  if (role === 'manager') {
    return copy.value.managerBadge
  }
  if (!role || role === 'guest') {
    return copy.value.guestBadge
  }
  return ''
}

const replyCount = (item) => countComments(item?.replies || [])
const isReplyingTo = (item) => Boolean(item?.id && replyTarget.value && String(replyTarget.value.id) === String(item.id))

const resetReportForm = () => {
  reportForm.value = {
    reasonCode: 'spam',
    otherReason: '',
    description: ''
  }
}

const resetReportState = () => {
  reportSubmitting.value = false
  reportTarget.value = null
  resetReportForm()
}

const openReportDialog = (comment) => {
  if (!comment?.id) {
    return
  }
  reportTarget.value = comment
  resetReportForm()
  showReportDialog.value = true
}

const appendVisibleComment = (list, incoming) => {
  if (!incoming.parentId) {
    return [incoming, ...list]
  }

  const attach = (items) => items.map((item) => {
    if (String(item.id) === String(incoming.parentId)) {
      return { ...item, replies: [...(item.replies || []), incoming] }
    }
    return { ...item, replies: attach(item.replies || []) }
  })

  return attach(list)
}

const goLogin = () => router.push('/login')

const toggleGuestMode = () => {
  if (userStore.isLoggedIn) {
    guestMode.value = false
    return
  }

  if (!guestMode.value || anonymousMode.value) {
    guestMode.value = true
    anonymousMode.value = false
    anonymousAlias.value = ''
    return
  }

  guestMode.value = false
}

const toggleEmojiPanel = () => {
  showEmojiPanel.value = !showEmojiPanel.value
}

const refreshAnonymousAlias = () => {
  anonymousAlias.value = createAnonymousAlias()
}

const toggleAnonymous = () => {
  if (userStore.isLoggedIn) {
    anonymousMode.value = false
    return
  }

  anonymousMode.value = !anonymousMode.value
  if (anonymousMode.value) {
    guestMode.value = true
    refreshAnonymousAlias()
  } else {
    guestMode.value = false
    anonymousAlias.value = ''
  }
}

const removeDraftImage = (index) => {
  draftImages.value.splice(index, 1)
}

const composerTransitionDuration = 150

const cleanupComposerTransition = (element) => {
  element.style.transition = ''
  element.style.height = ''
  element.style.opacity = ''
  element.style.transform = ''
  element.style.transformOrigin = ''
  element.style.overflow = ''
  element.style.pointerEvents = ''
  element.style.willChange = ''
}

const prefersReducedMotion = () => typeof window !== 'undefined'
  && typeof window.matchMedia === 'function'
  && window.matchMedia('(prefers-reduced-motion: reduce)').matches

const focusComposer = (options = null) => {
  const block = options?.block || 'nearest'
  requestAnimationFrame(() => {
    composerCardRef.value?.scrollIntoView({ behavior: 'smooth', block })
    composerRef.value?.focus()
  })
}

const scheduleComposerFocus = (mode) => {
  pendingComposerFocusMode.value = mode
}

const flushComposerFocus = () => {
  if (!pendingComposerFocusMode.value) {
    return
  }

  const mode = pendingComposerFocusMode.value
  pendingComposerFocusMode.value = ''
  focusComposer({ block: mode === 'top' ? 'start' : 'nearest' })
}

const runComposerTransition = (element, direction, done) => {
  if (!element) {
    done()
    return
  }

  if (prefersReducedMotion()) {
    cleanupComposerTransition(element)
    done()
    if (direction === 'enter') {
      flushComposerFocus()
    }
    return
  }

  const initialHeight = `${element.scrollHeight}px`
  const leaveTransform = 'translateY(-8px)'
  let finished = false

  const finish = () => {
    if (finished) {
      return
    }
    finished = true
    cleanupComposerTransition(element)
    done()
    if (direction === 'enter') {
      flushComposerFocus()
    }
  }

  element.style.overflow = 'hidden'
  element.style.pointerEvents = 'none'
  element.style.willChange = 'height, opacity, transform'
  element.style.transformOrigin = 'top center'

  if (direction === 'enter') {
    element.style.height = '0px'
    element.style.opacity = '0'
    element.style.transform = leaveTransform
  } else {
    element.style.height = initialHeight
    element.style.opacity = '1'
    element.style.transform = 'translateY(0)'
  }

  void element.offsetHeight

  requestAnimationFrame(() => {
    element.style.transition = [
      `height ${composerTransitionDuration}ms cubic-bezier(0.22, 1, 0.36, 1)`,
      `opacity ${Math.round(composerTransitionDuration * 0.72)}ms ease`,
      `transform ${composerTransitionDuration}ms cubic-bezier(0.22, 1, 0.36, 1)`
    ].join(', ')
    element.style.height = direction === 'enter' ? initialHeight : '0px'
    element.style.opacity = direction === 'enter' ? '1' : '0'
    element.style.transform = direction === 'enter' ? 'translateY(0)' : leaveTransform
  })

  window.setTimeout(finish, composerTransitionDuration + 40)
}

const handleComposerEnter = (element, done) => {
  runComposerTransition(element, 'enter', done)
}

const handleComposerLeave = (element, done) => {
  runComposerTransition(element, 'leave', done)
}

const clearReplyTarget = async (options = null) => {
  const shouldFocus = Boolean(options && typeof options === 'object' && options.focus)
  const hadReplyTarget = Boolean(replyTarget.value)
  if (shouldFocus && hadReplyTarget) {
    scheduleComposerFocus('top')
  } else {
    pendingComposerFocusMode.value = ''
  }
  replyTarget.value = null
  showEmojiPanel.value = false
  if (!shouldFocus || !hadReplyTarget) {
    return
  }
  await nextTick()
}

const setReplyTarget = async (comment) => {
  if (!comment?.id) {
    return
  }

  if (isReplyingTo(comment)) {
    pendingComposerFocusMode.value = ''
    showEmojiPanel.value = false
    await nextTick()
    focusComposer()
    return
  }

  scheduleComposerFocus('inline')
  replyTarget.value = comment
  showEmojiPanel.value = false
  await nextTick()
}

const appendEmoji = (emoji) => {
  const input = composerRef.value
  if (!input) {
    draft.value += emoji
    showEmojiPanel.value = false
    return
  }

  const start = input.selectionStart ?? draft.value.length
  const end = input.selectionEnd ?? draft.value.length
  draft.value = `${draft.value.slice(0, start)}${emoji}${draft.value.slice(end)}`
  showEmojiPanel.value = false

  requestAnimationFrame(() => {
    input.focus()
    const cursor = start + emoji.length
    input.setSelectionRange(cursor, cursor)
  })
}

const openImagePicker = () => {
  if (draftImages.value.length >= imageLimit) {
    ElMessage.warning(copy.value.imageLimitReached)
    return
  }

  showEmojiPanel.value = false
  if (imageInputRef.value) {
    imageInputRef.value.value = ''
    imageInputRef.value.click()
  }
}

const uploadDraftImages = async (files, successMessageFactory = copy.value.imageUploadSuccess) => {
  const remaining = imageLimit - draftImages.value.length
  if (remaining <= 0) {
    ElMessage.warning(copy.value.imageLimitReached)
    return
  }

  const selectedFiles = files.slice(0, remaining)
  if (files.length > selectedFiles.length) {
    ElMessage.warning(copy.value.imageLimitReached)
  }

  const formData = new FormData()
  selectedFiles.forEach((file) => formData.append('images', file))

  uploadingImages.value = true
  try {
    const res = await publicApi.uploadCommentImages(formData)
    const urls = Array.isArray(res.data?.urls) ? res.data.urls.filter(Boolean) : []
    if (!urls.length) {
      throw new Error(copy.value.imageUploadFailed)
    }

    draftImages.value = [...draftImages.value, ...urls].slice(0, imageLimit)
    ElMessage.success(successMessageFactory(urls.length))
  } catch (error) {
    ElMessage.error(getRequestErrorMessage(error, copy.value.reportSubmitFailed))
  } finally {
    uploadingImages.value = false
  }
}

const handleLocalImageSelect = async (event) => {
  const input = event?.target
  const files = Array.from(input?.files || []).filter(Boolean)
  if (!files.length) {
    return
  }

  try {
    await uploadDraftImages(files, copy.value.imageUploadSuccess)
  } finally {
    input.value = ''
  }
}

const handleComposerPaste = async (event) => {
  const clipboardItems = Array.from(event?.clipboardData?.items || [])
  const imageFiles = clipboardItems
    .filter((item) => item?.kind === 'file' && String(item.type || '').startsWith('image/'))
    .map((item) => item.getAsFile())
    .filter(Boolean)

  if (!imageFiles.length) {
    return
  }

  event.preventDefault()
  showEmojiPanel.value = false
  await uploadDraftImages(imageFiles, copy.value.pasteImageSuccess)
}

const loadComments = async () => {
  loading.value = true
  try {
    const res = await publicApi.getPostComments(props.postId)
    comments.value = normalizeComments(res.data)
  } catch (error) {
    comments.value = []
    ElMessage.error(getRequestErrorMessage(error, '加载评论失败'))
  } finally {
    loading.value = false
  }
}

const submitComment = async () => {
  const content = draft.value.trim()
  if (!content && !draftImages.value.length) {
    ElMessage.warning(copy.value.emptyDraft)
    return
  }

  if (guestMode.value && !anonymousMode.value && (!guestForm.value.authorName.trim() || !guestForm.value.authorEmail.trim())) {
    ElMessage.warning(copy.value.guestRequired)
    return
  }

  if (!guestMode.value && !userStore.isLoggedIn) {
    ElMessage.warning(copy.value.loginHint)
    return
  }

  if (commentTurnstileEnabled.value && !commentTurnstileToken.value) {
    ElMessage.warning(copy.value.turnstileRequired)
    return
  }

  submitting.value = true
  try {
    const res = await publicApi.createPostComment(props.postId, {
      parentId: replyTarget.value?.id || null,
      guestMode: guestMode.value,
      authorName: anonymousMode.value
        ? anonymousAlias.value
        : (guestMode.value ? guestForm.value.authorName.trim() : ''),
      authorEmail: guestMode.value && !anonymousMode.value ? guestForm.value.authorEmail.trim() : '',
      authorWebsite: guestMode.value && !anonymousMode.value ? guestForm.value.authorWebsite.trim() : '',
      content,
      images: [...draftImages.value],
      anonymous: anonymousMode.value,
      turnstileToken: commentTurnstileEnabled.value ? commentTurnstileToken.value : ''
    })

    const result = res.data || {}
    const visibleComment = result.comment ? normalizeComments([result.comment])[0] : null
    if (result.status === 'visible' && visibleComment) {
      comments.value = appendVisibleComment(comments.value, visibleComment)
      ElMessage.success(copy.value.submitSuccess)
    } else {
      ElMessage.success(result.message || copy.value.submitPending)
    }

    draft.value = ''
    draftImages.value = []
    guestMode.value = false
    anonymousMode.value = false
    anonymousAlias.value = ''
    showEmojiPanel.value = false
    resetCommentTurnstile()
    await clearReplyTarget({ focus: true })
  } catch (error) {
    resetCommentTurnstile()
    ElMessage.error(getRequestErrorMessage(error, '发表评论失败'))
  } finally {
    submitting.value = false
  }
}

const submitReport = async () => {
  if (!reportTarget.value?.id) {
    return
  }

  if (!reportForm.value.reasonCode) {
    ElMessage.warning(copy.value.reportMissingReason)
    return
  }

  if (reportForm.value.reasonCode === 'other' && !reportForm.value.otherReason.trim()) {
    ElMessage.warning(copy.value.reportMissingOtherReason)
    return
  }

  if (!reportForm.value.description.trim()) {
    ElMessage.warning(copy.value.reportMissingDescription)
    return
  }

  reportSubmitting.value = true
  try {
    await publicApi.createCommentReport(props.postId, reportTarget.value.id, {
      reasonCode: reportForm.value.reasonCode,
      otherReason: reportForm.value.reasonCode === 'other' ? reportForm.value.otherReason.trim() : '',
      description: reportForm.value.description.trim()
    })
    ElMessage.success(copy.value.reportSubmitSuccess)
    showReportDialog.value = false
  } catch (error) {
    ElMessage.error(getRequestErrorMessage(error, copy.value.imageUploadFailed))
  } finally {
    reportSubmitting.value = false
  }
}

const deleteComment = async (comment) => {
  if (!canManageCommentActions.value || !comment?.id) {
    return
  }

  try {
    await ElMessageBox.confirm(copy.value.deleteConfirmMessage, copy.value.deleteConfirmTitle, {
      type: 'warning',
      confirmButtonText: locale.value === 'en' ? 'Delete' : '删除',
      cancelButtonText: locale.value === 'en' ? 'Cancel' : '取消'
    })
    await adminApi.deleteComment(comment.id)
    if (replyTarget.value?.id && String(replyTarget.value.id) === String(comment.id)) {
      await clearReplyTarget()
    }
    if (reportTarget.value?.id && String(reportTarget.value.id) === String(comment.id)) {
      showReportDialog.value = false
    }
    await loadComments()
    ElMessage.success(copy.value.deleteSuccess)
  } catch (error) {
    if (error === 'cancel' || error === 'close') {
      return
    }
    ElMessage.error(getRequestErrorMessage(error, copy.value.deleteFailed))
  }
}

const handleDocumentPointerDown = (event) => {
  if (!showEmojiPanel.value) {
    return
  }
  if (emojiPanelRef.value?.contains(event.target) || emojiToggleRef.value?.contains(event.target)) {
    return
  }
  showEmojiPanel.value = false
}

onMounted(() => {
  document.addEventListener('mousedown', handleDocumentPointerDown)
  void getTurnstileConfig().then((config) => {
    turnstileConfig.value = config
  })
})

onBeforeUnmount(() => {
  document.removeEventListener('mousedown', handleDocumentPointerDown)
})

watch(() => props.postId, () => {
  clearReplyTarget()
  showEmojiPanel.value = false
  resetCommentTurnstile()
  loadComments()
}, { immediate: true })

watch(() => userStore.isLoggedIn, () => {
  guestMode.value = false
  anonymousMode.value = false
  anonymousAlias.value = ''
  showEmojiPanel.value = false
  resetCommentTurnstile()
})

watch(anonymousMode, (enabled) => {
  if (enabled && !anonymousAlias.value) {
    refreshAnonymousAlias()
  }
})

watch(showReportDialog, (visible) => {
  if (!visible) {
    resetReportState()
  }
})
</script>

<style scoped>
.comments-card { margin-top: 22px; padding: 24px 26px 26px; border-radius: 28px; border: 1px solid color-mix(in srgb, var(--border-color) 78%, transparent); background: linear-gradient(180deg, color-mix(in srgb, var(--accent-light) 42%, var(--bg-card)), color-mix(in srgb, var(--bg-card) 96%, transparent)); box-shadow: 0 18px 40px rgba(15, 23, 42, 0.08); }
.comments-head, .comment-head { display: flex; align-items: center; justify-content: space-between; gap: 12px; }
.comments-head { margin-bottom: 20px; }
.comments-title { display: flex; align-items: center; gap: 12px; }
.comments-title h2, .comments-title p, .state-block h3, .state-block p { margin: 0; }
.comments-title h2 { color: var(--text-primary); font-size: clamp(1.26rem, 2vw, 1.5rem); }
.comments-title p, .counter, .comment-head, .upload-state { color: var(--text-secondary); }
.comments-icon, .comments-count, .avatar { display: inline-flex; align-items: center; justify-content: center; }
.comments-icon { width: 40px; height: 40px; border-radius: 14px; background: color-mix(in srgb, var(--accent-light) 90%, var(--bg-card)); color: var(--accent); }
.comments-count { min-width: 34px; height: 34px; padding: 0 12px; border-radius: 999px; background: linear-gradient(135deg, var(--accent), color-mix(in srgb, var(--accent-secondary) 84%, var(--accent))); color: #fff; font-weight: 700; }
.composer-card, .state-block, .comment-item, .reply-item { border-radius: 24px; border: 1px solid color-mix(in srgb, var(--border-color) 72%, transparent); background: color-mix(in srgb, var(--bg-card) 94%, transparent); }
.composer-card { position: relative; display: grid; gap: 14px; padding: 18px; overflow: visible; scroll-margin-top: 88px; transition: border-color 0.24s ease, box-shadow 0.24s ease; }
.composer-card.focused { border-color: color-mix(in srgb, var(--accent) 36%, var(--border-color)); box-shadow: 0 0 0 5px color-mix(in srgb, var(--accent-light) 88%, transparent); }
.composer-top, .author-row { display: flex; align-items: center; gap: 10px; flex-wrap: wrap; }
.composer-top { justify-content: flex-start; }
.mode-switch, .tool-buttons { display: flex; flex-wrap: wrap; gap: 10px; align-items: center; }
.identity-card { display: inline-flex; align-items: center; gap: 12px; min-height: 52px; padding: 8px 14px 8px 10px; border-radius: 18px; border: 1px solid color-mix(in srgb, var(--accent) 18%, var(--border-color)); background: linear-gradient(135deg, color-mix(in srgb, var(--accent-light) 82%, var(--bg-card)), color-mix(in srgb, var(--bg-card) 98%, transparent)); box-shadow: 0 10px 24px color-mix(in srgb, var(--accent) 10%, transparent); }
.identity-avatar { display: inline-flex; align-items: center; justify-content: center; width: 34px; height: 34px; overflow: hidden; flex: 0 0 auto; border: 1.5px solid color-mix(in srgb, var(--accent) 38%, var(--border-color)); border-radius: 12px; background: linear-gradient(135deg, var(--accent), color-mix(in srgb, var(--accent-secondary) 82%, var(--accent))); color: #fff; font-size: 0.92rem; font-weight: 700; box-shadow: 0 6px 18px color-mix(in srgb, var(--accent) 14%, transparent); }
.identity-avatar.image { background: color-mix(in srgb, var(--accent-light) 58%, var(--bg-card)); }
.identity-avatar img { width: 100%; height: 100%; object-fit: cover; }
.identity-info { display: grid; gap: 2px; min-width: 0; }
.identity-info strong, .identity-info span { display: block; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.identity-info strong { color: var(--text-primary); font-size: 0.96rem; line-height: 1.2; }
.identity-info span { color: var(--accent); font-size: 0.8rem; font-weight: 700; line-height: 1.2; }
.guest-grid { display: grid; grid-template-columns: repeat(3, minmax(0, 1fr)); gap: 12px; }
.guest-grid.compact { grid-template-columns: repeat(2, minmax(0, 1fr)); }
.field-input, .composer-input { width: 100%; border: 1px solid color-mix(in srgb, var(--border-color) 76%, transparent); border-radius: 16px; background: color-mix(in srgb, var(--bg-card) 96%, transparent); color: var(--text-primary); font: inherit; }
.field-input { min-height: 48px; padding: 0 14px; }
.field-input:focus, .composer-input:focus { outline: none; border-color: color-mix(in srgb, var(--accent) 28%, var(--border-color)); box-shadow: 0 0 0 4px color-mix(in srgb, var(--accent-light) 84%, transparent); }
.composer-input { min-height: 150px; padding: 14px 16px; resize: vertical; line-height: 1.8; }
.chip, .submit-btn, .emoji-btn, .image-remove, .tool-btn { font: inherit; }
.chip, .emoji-btn { min-height: 38px; padding: 0 14px; border-radius: 999px; border: 1px solid color-mix(in srgb, var(--border-color) 76%, transparent); background: color-mix(in srgb, var(--bg-card) 98%, transparent); color: var(--text-secondary); cursor: pointer; transition: color 0.2s ease, border-color 0.2s ease, background 0.2s ease, transform 0.2s ease; }
.chip.active, .chip:hover, .emoji-btn:hover, .tool-btn:hover { color: var(--accent); border-color: color-mix(in srgb, var(--accent) 28%, var(--border-color)); background: color-mix(in srgb, var(--accent-light) 88%, var(--bg-card)); }
.login-chip { color: #fff; border-color: transparent; background: linear-gradient(135deg, var(--accent), color-mix(in srgb, var(--accent-secondary) 82%, var(--accent))); }
.login-chip:hover, .login-chip:focus-visible { color: #fff; border-color: transparent; background: linear-gradient(135deg, var(--accent), color-mix(in srgb, var(--accent-secondary) 82%, var(--accent))); transform: translateY(-1px); }
.anonymous-alias-row { display: flex; align-items: center; gap: 12px; flex-wrap: wrap; }
.anonymous-alias-card { display: flex; align-items: center; gap: 12px; min-height: 48px; padding: 0 16px; border-radius: 16px; border: 1px dashed color-mix(in srgb, var(--accent) 26%, var(--border-color)); background: color-mix(in srgb, var(--accent-light) 58%, var(--bg-card)); color: var(--text-primary); }
.anonymous-alias-label { color: var(--text-secondary); font-size: 0.86rem; }
.reply-banner { padding: 12px 14px; border-radius: 16px; background: color-mix(in srgb, var(--bg-tertiary) 82%, transparent); }
.reply-banner p { margin: 4px 0 0; color: var(--text-secondary); }
.reply-banner.inline { display: flex; align-items: flex-start; justify-content: space-between; gap: 14px; }
.reply-banner.inline strong { color: var(--text-primary); }
.reply-banner.inline > div { min-width: 0; }
.image-list, .comment-images { display: flex; flex-wrap: wrap; gap: 12px; }
.image-chip { position: relative; width: 88px; height: 88px; border-radius: 18px; overflow: hidden; }
.image-chip img, .image-link img, .avatar img { width: 100%; height: 100%; object-fit: cover; }
.image-remove { position: absolute; top: 8px; right: 8px; width: 24px; height: 24px; border: 0; border-radius: 999px; background: rgba(15, 23, 42, 0.72); color: #fff; cursor: pointer; }
.turnstile-row { margin-top: -2px; }
.composer-actions { display: flex; align-items: flex-end; justify-content: space-between; gap: 14px; }
.toolbar-shell { position: relative; flex: 1 1 auto; min-width: 0; }
.tool-row { display: flex; align-items: center; justify-content: space-between; gap: 14px; min-height: 44px; }
.tool-meta { display: flex; align-items: center; justify-content: flex-end; gap: 12px; margin-left: auto; }
.tool-btn { display: inline-flex; align-items: center; justify-content: center; width: 44px; height: 44px; padding: 0; border-radius: 16px; border: 1px solid color-mix(in srgb, var(--border-color) 76%, transparent); background: color-mix(in srgb, var(--bg-card) 98%, transparent); color: var(--text-secondary); cursor: pointer; transition: color 0.2s ease, border-color 0.2s ease, background 0.2s ease, transform 0.2s ease; }
.tool-btn.active { color: var(--accent); border-color: color-mix(in srgb, var(--accent) 28%, var(--border-color)); background: color-mix(in srgb, var(--accent-light) 88%, var(--bg-card)); }
.tool-btn:disabled { opacity: 0.56; cursor: not-allowed; transform: none; }
.tool-btn svg { width: 20px; height: 20px; fill: none; stroke: currentColor; stroke-width: 1.8; stroke-linecap: round; stroke-linejoin: round; }
.emoji-panel { position: absolute; left: 0; bottom: calc(100% + 12px); z-index: 16; box-sizing: border-box; display: grid; grid-template-columns: repeat(8, minmax(0, 1fr)); gap: 10px; width: min(420px, calc(100vw - 96px)); max-height: 248px; padding: 14px; border-radius: 20px; border: 1px solid color-mix(in srgb, var(--border-color) 78%, transparent); background: color-mix(in srgb, var(--bg-card) 98%, transparent); box-shadow: 0 18px 40px rgba(15, 23, 42, 0.16); backdrop-filter: blur(16px); overflow-x: hidden; overflow-y: auto; overscroll-behavior: contain; scrollbar-width: thin; scrollbar-color: color-mix(in srgb, var(--accent) 68%, transparent) color-mix(in srgb, var(--bg-tertiary) 74%, transparent); }
.emoji-panel::-webkit-scrollbar { width: 8px; }
.emoji-panel::-webkit-scrollbar-track { background: color-mix(in srgb, var(--bg-tertiary) 74%, transparent); border-radius: 999px; }
.emoji-panel::-webkit-scrollbar-thumb { background: color-mix(in srgb, var(--accent) 62%, transparent); border-radius: 999px; }
.emoji-panel::-webkit-scrollbar-thumb:hover { background: color-mix(in srgb, var(--accent) 78%, transparent); }
.emoji-btn { display: inline-flex; align-items: center; justify-content: center; min-height: 42px; padding: 0; font-size: 1.15rem; }
.sr-only { position: absolute; width: 1px; height: 1px; padding: 0; margin: -1px; overflow: hidden; clip: rect(0, 0, 0, 0); white-space: nowrap; border: 0; }
.submit-btn { min-height: 42px; padding: 0 18px; border: 0; border-radius: 999px; color: #fff; cursor: pointer; white-space: nowrap; background: linear-gradient(135deg, var(--accent), color-mix(in srgb, var(--accent-secondary) 82%, var(--accent))); }
.submit-btn:disabled { opacity: 0.72; cursor: not-allowed; }
.state-block { margin-top: 22px; padding: 22px; }
.state-block.empty { text-align: center; }
.comments-list { display: grid; gap: 18px; margin-top: 22px; }
.comment-item { display: grid; grid-template-columns: 50px minmax(0, 1fr); gap: 14px; padding: 18px; border-top: 1px solid color-mix(in srgb, var(--border-color) 55%, transparent); }
.avatar { width: 50px; height: 50px; overflow: hidden; border: 2px solid color-mix(in srgb, var(--accent) 34%, var(--border-color)); border-radius: 17px; color: #fff; font-weight: 700; background: linear-gradient(135deg, var(--accent), color-mix(in srgb, var(--accent-secondary) 82%, var(--accent))); box-shadow: 0 6px 18px color-mix(in srgb, var(--accent) 10%, transparent); }
.avatar.image { background: color-mix(in srgb, var(--accent-light) 58%, var(--bg-card)); }
.comment-main { display: grid; gap: 12px; min-width: 0; }
.reply-composer-card { margin-top: 4px; padding: 16px; border-radius: 22px; background: color-mix(in srgb, var(--accent-light) 18%, var(--bg-card)); box-shadow: inset 0 0 0 1px color-mix(in srgb, var(--accent) 10%, var(--border-color)); }
.reply-composer-card.inline { gap: 12px; }
.reply-composer-card .composer-input { min-height: 132px; }
.reply-item > .reply-composer-card { margin-top: 8px; }
.comment-card-head { display: flex; align-items: flex-start; justify-content: space-between; gap: 16px; }
.comment-meta-main { display: grid; gap: 4px; }
.meta-inline-row { display: flex; align-items: center; gap: 10px; flex-wrap: wrap; }
.comment-date { color: var(--text-secondary); font-size: 0.92rem; }
.author-row strong { color: var(--text-primary); }
.author-link, .reply-target { color: var(--accent); text-decoration: none; font-size: 0.85rem; }
.author-link:hover { text-decoration: underline; }
.badge, .meta-chip { display: inline-flex; align-items: center; min-height: 26px; padding: 0 10px; border-radius: 999px; font-size: 0.8rem; font-weight: 700; }
.badge { background: color-mix(in srgb, var(--accent-light) 88%, var(--bg-card)); color: var(--accent); }
.badge.ghost, .meta-chip { background: color-mix(in srgb, var(--bg-tertiary) 88%, transparent); color: var(--text-secondary); }
.comment-foot { display: flex; align-items: center; justify-content: space-between; gap: 10px; flex-wrap: wrap; margin-top: 4px; }
.comment-foot.compact { gap: 8px; }
.comment-foot.dense { margin-top: 2px; }
.comment-foot-actions { display: inline-flex; align-items: center; gap: 8px; margin-left: auto; }
.comment-client-tags { display: flex; flex: 1 1 auto; min-width: 0; flex-wrap: wrap; justify-content: flex-start; gap: 8px; }
.comment-client-tags.compact { gap: 7px; }
.client-tag { display: inline-flex; align-items: center; gap: 6px; min-height: 28px; padding: 0 11px; border-radius: 999px; border: 1px solid color-mix(in srgb, var(--border-color) 72%, transparent); background: color-mix(in srgb, var(--bg-tertiary) 82%, transparent); color: var(--text-secondary); font-size: 0.78rem; letter-spacing: 0.01em; }
.client-tag svg { width: 14px; height: 14px; flex: 0 0 auto; fill: none; stroke: currentColor; stroke-width: 1.7; stroke-linecap: round; stroke-linejoin: round; }
.client-tag svg circle,
.client-tag svg rect,
.client-tag svg path { vector-effect: non-scaling-stroke; }
.status-chip { margin-left: auto; white-space: nowrap; }
.meta-action { min-height: 28px; padding: 0 12px; border: 1px solid color-mix(in srgb, var(--border-color) 72%, transparent); border-radius: 999px; background: color-mix(in srgb, var(--bg-card) 96%, transparent); color: var(--text-secondary); cursor: pointer; transition: color 0.2s ease, border-color 0.2s ease, background 0.2s ease; }
.meta-action:hover,
.meta-action:focus-visible { color: #d14f4f; border-color: color-mix(in srgb, #d14f4f 42%, var(--border-color)); background: color-mix(in srgb, #ffe7e7 74%, var(--bg-card)); outline: none; }
.comment-content { margin: 4px 0 2px; color: var(--text-primary); line-height: 1.9; }
.comment-content p { margin: 0; }
.comment-content p + p { margin-top: 10px; }
.comment-images { display: grid; grid-template-columns: repeat(auto-fit, minmax(180px, 1fr)); }
.comment-images.compact { grid-template-columns: repeat(auto-fit, minmax(120px, 1fr)); }
.image-link { overflow: hidden; border-radius: 18px; background: color-mix(in srgb, var(--bg-tertiary) 86%, transparent); }
.image-link img { height: 180px; }
.comment-images.compact .image-link img { height: 120px; }
.reply-bubble { flex: 0 0 auto; min-width: 54px; height: 54px; padding: 0 14px; border-radius: 18px; border: 1px solid color-mix(in srgb, var(--accent) 20%, var(--border-color)); background: color-mix(in srgb, var(--accent-light) 34%, var(--bg-card)); color: color-mix(in srgb, var(--accent) 88%, var(--text-primary)); display: inline-flex; align-items: center; justify-content: center; gap: 6px; cursor: pointer; transition: color 0.2s ease, border-color 0.2s ease, background 0.2s ease, transform 0.2s ease, box-shadow 0.2s ease; }
.reply-bubble svg { width: 18px; height: 18px; fill: currentColor; }
.reply-bubble.compact { min-width: 46px; height: 46px; border-radius: 16px; }
.reply-bubble:hover,
.reply-bubble:focus-visible { color: #fff; border-color: color-mix(in srgb, var(--accent) 48%, var(--border-color)); background: linear-gradient(135deg, var(--accent), color-mix(in srgb, var(--accent-secondary) 84%, var(--accent))); box-shadow: 0 10px 22px color-mix(in srgb, var(--accent) 18%, transparent); transform: translateY(-1px); outline: none; }
.reply-list { display: grid; gap: 12px; padding: 14px 0 0 10px; border-left: 1px dashed color-mix(in srgb, var(--border-color) 72%, transparent); }
.reply-item { display: grid; grid-template-columns: 40px minmax(0, 1fr); align-items: flex-start; gap: 12px; width: 100%; margin-left: 0; padding: 16px 18px; background: color-mix(in srgb, var(--bg-tertiary) 26%, var(--bg-card)); box-shadow: inset 0 0 0 1px color-mix(in srgb, var(--border-color) 58%, transparent); }
.reply-avatar { width: 40px; height: 40px; border-radius: 14px; }
.reply-main { display: grid; gap: 10px; min-width: 0; }
.reply-card-head { margin-bottom: 2px; }
.report-dialog-body { display: grid; gap: 16px; }
.report-target-card { padding: 14px 16px; border: 1px solid color-mix(in srgb, var(--border-color) 72%, transparent); border-radius: 18px; background: color-mix(in srgb, var(--bg-tertiary) 42%, var(--bg-card)); }
.report-target-label { color: var(--text-secondary); font-size: 0.84rem; margin-bottom: 6px; }
.report-target-author { color: var(--text-primary); font-weight: 700; margin-bottom: 6px; }
.report-target-content { color: var(--text-secondary); line-height: 1.7; white-space: pre-wrap; word-break: break-word; }
.report-form :deep(.el-form-item__label) { color: var(--text-secondary); font-weight: 600; }
.report-form :deep(.el-input__wrapper),
.report-form :deep(.el-textarea__inner),
.report-form :deep(.el-select__wrapper) { background: color-mix(in srgb, var(--bg-card) 96%, transparent); box-shadow: 0 0 0 1px color-mix(in srgb, var(--border-color) 76%, transparent) inset !important; }
.report-dialog-footer { display: flex; justify-content: flex-end; gap: 12px; }
@media (max-width: 768px) {
  .comments-card { padding: 18px; border-radius: 22px; }
  .comments-head, .comment-item, .comment-card-head, .composer-actions { display: grid; }
  .guest-grid, .guest-grid.compact { grid-template-columns: 1fr; }
  .comment-item { grid-template-columns: 1fr; }
  .avatar { width: 44px; height: 44px; border-radius: 15px; }
  .identity-card { width: 100%; }
  .tool-row { flex-direction: column; align-items: stretch; }
  .tool-meta { justify-content: space-between; margin-left: 0; }
  .emoji-panel { width: min(100%, calc(100vw - 48px)); grid-template-columns: repeat(6, minmax(0, 1fr)); }
  .submit-btn { width: 100%; }
  .comment-foot, .comment-client-tags { justify-content: flex-start; }
  .status-chip, .comment-foot-actions { margin-left: 0; }
  .reply-item { grid-template-columns: 1fr; }
  .reply-avatar { width: 38px; height: 38px; border-radius: 13px; }
  .reply-banner.inline { flex-direction: column; }
}
</style>
