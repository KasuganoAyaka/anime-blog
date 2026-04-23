<template>
  <div class="about-page">
    <TopHeader @toggle-music="uiStore.toggleMusicPlayer()" />

    <main class="main-content">
      <div class="container">
        <div class="about-nav">
          <button
            v-for="tab in tabs"
            :key="tab.id"
            :class="['tab-btn', { active: activeTab === tab.id }]"
            @click="activeTab = tab.id"
          >
            {{ tab.icon }} {{ tab.name }}
          </button>
        </div>

        <div v-if="activeTab === 'contact'" class="tab-content animate-fade-in-up">
          <div class="content-card">
            <h2 class="section-title">📬 {{ t('about.contact.title') }}</h2>
            <p class="section-desc">{{ t('about.contact.desc') }}</p>

            <div class="contact-wrapper">
              <div class="contact-form">
                <el-form ref="contactFormRef" :model="contactForm" :rules="contactRules" label-position="top">
                  <el-form-item :label="t('about.contact.name')" prop="name">
                    <el-input v-model="contactForm.name" :placeholder="t('about.contact.namePlaceholder')" size="large" />
                  </el-form-item>
                  <el-form-item :label="t('about.contact.email')" prop="email">
                    <el-input v-model="contactForm.email" :placeholder="t('about.contact.emailPlaceholder')" size="large" />
                  </el-form-item>
                  <el-form-item :label="t('about.contact.subject')" prop="subject">
                    <el-input v-model="contactForm.subject" :placeholder="t('about.contact.subjectPlaceholder')" size="large" />
                  </el-form-item>
                  <el-form-item :label="t('about.contact.message')" prop="message">
                    <el-input
                      v-model="contactForm.message"
                      type="textarea"
                      rows="5"
                      :placeholder="t('about.contact.messagePlaceholder')"
                      size="large"
                    />
                  </el-form-item>
                  <el-button type="primary" size="large" class="submit-btn" @click="handleSubmitContact">
                    <span>📨 {{ t('about.contact.submit') }}</span>
                  </el-button>
                </el-form>
              </div>

              <div class="contact-info">
                <h3>{{ t('about.contact.otherMethods') }}</h3>
                <ul class="contact-list">
                  <li>
                    <span class="contact-icon">📬</span>
                    <span>blog@ayakacloud.cn</span>
                  </li>
                  <li>
                    <span class="contact-icon">📺</span>
                    <span>Bilibili: @KasuganoAyaka</span>
                  </li>
                  <li>
                    <span class="contact-icon">💻</span>
                    <span>GitHub: github.com/KasuganoAyaka</span>
                  </li>
                </ul>
              </div>
            </div>
          </div>
        </div>

        <div v-if="activeTab === 'sites'" class="tab-content animate-fade-in-up">
          <div class="content-card">
            <h2 class="section-title">🌐 {{ t('about.sites.title') }}</h2>
            <p class="section-desc">{{ t('about.sites.desc') }}</p>

            <div class="sites-grid">
              <a href="https://pan.ayakacloud.cn" target="_blank" class="site-card">
                <div class="site-icon">☁️</div>
                <div class="site-info">
                  <h3 class="site-name">Ayaka Cloud</h3>
                  <p class="site-desc">{{ t('about.sites.ayakaCloud') }}</p>
                </div>
                <span class="site-link">{{ t('about.sites.visit') }}</span>
              </a>
              <a href="https://space.bilibili.com/24419347" target="_blank" class="site-card">
                <div class="site-icon">📺</div>
                <div class="site-info">
                  <h3 class="site-name">Bilibili</h3>
                  <p class="site-desc">{{ t('about.sites.bilibili') }}</p>
                </div>
                <span class="site-link">{{ t('about.sites.visit') }}</span>
              </a>
              <a href="https://music.163.com/#/my/m/music/playlist?id=14389818985" target="_blank" class="site-card">
                <div class="site-icon">🎵</div>
                <div class="site-info">
                  <h3 class="site-name">Music</h3>
                  <p class="site-desc">{{ t('about.sites.music') }}</p>
                </div>
                <span class="site-link">{{ t('about.sites.visit') }}</span>
              </a>
              <a href="https://ima.qq.com" target="_blank" class="site-card">
                <div class="site-icon">📚</div>
                <div class="site-info">
                  <h3 class="site-name">Knowledge</h3>
                  <p class="site-desc">{{ t('about.sites.knowledge') }}</p>
                </div>
                <span class="site-link">{{ t('about.sites.visit') }}</span>
              </a>
            </div>
          </div>
        </div>

        <div v-if="activeTab === 'links'" class="tab-content animate-fade-in-up">
          <div class="content-card">
            <h2 class="section-title">🔗 {{ t('about.links.title') }}</h2>
            <p class="section-desc">{{ t('about.links.desc') }}</p>

            <div class="links-section">
              <h3 class="links-category">💻 {{ t('about.links.techPartners') }}</h3>
              <div class="links-grid">
                <a v-for="link in friendLinks" :key="link.id" :href="link.url" target="_blank" class="link-card">
                  <div class="link-name">{{ link.name }}</div>
                  <div class="link-desc">{{ link.description }}</div>
                </a>
              </div>
              <div v-if="friendLinks.length === 0" class="empty-links">
                <p>{{ t('about.links.empty') }}</p>
              </div>
            </div>

            <div class="submit-link-section">
              <h3>📝 {{ t('about.links.submitTitle') }}</h3>
              <p>{{ t('about.links.submitDesc') }}</p>

              <div class="link-form-wrapper">
                <el-form ref="linkFormRef" :model="linkForm" :rules="linkRules" label-position="top">
                  <el-form-item :label="t('about.links.siteName')" prop="siteName">
                    <el-input v-model="linkForm.siteName" :placeholder="t('about.links.siteNamePlaceholder')" size="large" />
                  </el-form-item>
                  <el-form-item :label="t('about.links.siteUrl')" prop="siteUrl">
                    <el-input v-model="linkForm.siteUrl" placeholder="https://example.com" size="large" />
                  </el-form-item>
                  <el-form-item :label="t('about.links.siteDesc')" prop="siteDesc">
                    <el-input v-model="linkForm.siteDesc" :placeholder="t('about.links.siteDescPlaceholder')" size="large" />
                  </el-form-item>
                  <el-form-item :label="t('about.links.contactEmail')" prop="contactEmail">
                    <el-input v-model="linkForm.contactEmail" :placeholder="t('about.links.contactEmailPlaceholder')" size="large" />
                  </el-form-item>
                  <el-button type="primary" size="large" class="submit-btn" @click="handleSubmitLink">
                    <span>✨ {{ t('about.links.submit') }}</span>
                  </el-button>
                </el-form>
              </div>
            </div>
          </div>
        </div>
      </div>
    </main>
    <footer class="footer">
      <p>{{ t('common.copyright', { year: 2026 }) }}</p>
    </footer>
  </div>
</template>

<script setup>
import { computed, ref, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage } from 'element-plus'
import TopHeader from '@/components/TopHeader.vue'
import { publicApi } from '@/api'
import { useUiStore } from '@/stores'
import { getRequestErrorMessage } from '@/utils/request'

const { t } = useI18n()
const uiStore = useUiStore()
const activeTab = ref('contact')

const tabs = computed(() => ([
  { id: 'contact', name: t('about.tabs.contact'), icon: '📬' },
  { id: 'sites', name: t('about.tabs.sites'), icon: '🌐' },
  { id: 'links', name: t('about.tabs.links'), icon: '🔗' }
]))

const contactForm = ref({
  name: '',
  email: '',
  subject: '',
  message: ''
})

const contactRules = computed(() => ({
  name: [{ required: true, message: t('about.rules.nameRequired'), trigger: 'blur' }],
  email: [
    { required: true, message: t('about.rules.emailRequired'), trigger: 'blur' },
    { type: 'email', message: t('about.rules.emailInvalid'), trigger: 'blur' }
  ],
  subject: [{ required: true, message: t('about.rules.subjectRequired'), trigger: 'blur' }],
  message: [{ required: true, message: t('about.rules.messageRequired'), trigger: 'blur' }]
}))

const linkForm = ref({
  siteName: '',
  siteUrl: '',
  siteDesc: '',
  contactEmail: ''
})

const linkRules = computed(() => ({
  siteName: [{ required: true, message: t('about.rules.siteNameRequired'), trigger: 'blur' }],
  siteUrl: [
    { required: true, message: t('about.rules.siteUrlRequired'), trigger: 'blur' },
    { type: 'url', message: t('about.rules.urlInvalid'), trigger: 'blur' }
  ],
  siteDesc: [{ required: true, message: t('about.rules.siteDescRequired'), trigger: 'blur' }],
  contactEmail: [
    { required: true, message: t('about.rules.emailRequired'), trigger: 'blur' },
    { type: 'email', message: t('about.rules.emailInvalid'), trigger: 'blur' }
  ]
}))

const friendLinks = ref([])

const fetchFriendLinks = async () => {
  try {
    const res = await publicApi.getFriendLinks()
    friendLinks.value = res.data || []
  } catch (error) {
    console.error('Failed to fetch friend links:', error)
  }
}

const contactFormRef = ref(null)
const linkFormRef = ref(null)

const handleSubmitContact = async () => {
  const valid = await contactFormRef.value.validate().catch(() => false)
  if (!valid) return

  try {
    await publicApi.submitContact(contactForm.value)
    ElMessage.success(t('about.contact.success'))
    contactForm.value = { name: '', email: '', subject: '', message: '' }
    contactFormRef.value?.clearValidate?.()
  } catch (error) {
    ElMessage.error(getRequestErrorMessage(error, t('about.contact.failed')))
  }
}

const handleSubmitLink = async () => {
  const valid = await linkFormRef.value.validate().catch(() => false)
  if (!valid) return

  try {
    await publicApi.submitFriendLink(linkForm.value)
    ElMessage.success(t('about.links.success'))
    linkForm.value = { siteName: '', siteUrl: '', siteDesc: '', contactEmail: '' }
    linkFormRef.value?.clearValidate?.()
  } catch (error) {
    ElMessage.error(getRequestErrorMessage(error, t('about.links.failed')))
  }
}

onMounted(() => {
  fetchFriendLinks()
})
</script>

<style scoped>
.about-page {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  background: var(--gradient-bg);
}

.main-content {
  flex: 1;
  padding: 40px 0;
}

.container {
  max-width: 800px;
  margin: 0 auto;
  padding: 0 24px;
}

.about-nav {
  display: flex;
  gap: 8px;
  margin-bottom: 32px;
  padding: 10px;
  background: var(--bg-card);
  border: 1px solid var(--border-color);
  border-radius: 18px;
  justify-content: center;
}

.tab-btn {
  padding: 12px 24px;
  background: transparent;
  border: none;
  color: var(--text-secondary);
  font-size: 1rem;
  font-weight: 500;
  cursor: pointer;
  border-radius: 12px;
  position: relative;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

.tab-btn:hover {
  color: var(--accent);
  background: var(--accent-light);
}

.tab-btn.active {
  color: #fff;
  background: var(--gradient-primary);
  box-shadow: 0 4px 16px rgba(57, 197, 187, 0.35);
  transform: translateY(-2px);
}

.content-card {
  background: var(--bg-card);
  border: 1px solid var(--border-color);
  border-radius: 24px;
  padding: 40px;
  box-shadow: var(--shadow-md);
}

.section-title {
  font-size: 1.6rem;
  color: var(--text-primary);
  margin-bottom: 12px;
  text-align: center;
}

.section-desc {
  color: var(--text-secondary);
  line-height: 1.8;
  text-align: center;
  margin-bottom: 32px;
  max-width: 600px;
  margin-left: auto;
  margin-right: auto;
}

.contact-wrapper {
  display: grid;
  grid-template-columns: 1fr 280px;
  gap: 32px;
}

.contact-form,
.link-form-wrapper {
  background: var(--bg-tertiary);
  border-radius: 18px;
  padding: 28px;
  border: 1px solid var(--border-light);
}

.contact-form :deep(.el-form-item__label),
.link-form-wrapper :deep(.el-form-item__label) {
  font-weight: 500;
  color: var(--text-primary);
  padding-bottom: 8px !important;
}

.contact-form :deep(.el-input__wrapper),
.link-form-wrapper :deep(.el-input__wrapper),
.contact-form :deep(.el-textarea__inner),
.link-form-wrapper :deep(.el-textarea__inner) {
  background: var(--bg-card) !important;
  border-radius: 12px !important;
  border: 1px solid var(--border-color) !important;
  box-shadow: none !important;
  transition: all 0.25s ease !important;
}

.contact-form :deep(.el-input__inner),
.link-form-wrapper :deep(.el-input__inner),
.contact-form :deep(.el-textarea__inner),
.link-form-wrapper :deep(.el-textarea__inner) {
  color: var(--text-primary) !important;
}

.contact-form :deep(.el-input__inner::placeholder),
.link-form-wrapper :deep(.el-input__inner::placeholder),
.contact-form :deep(.el-textarea__inner::placeholder),
.link-form-wrapper :deep(.el-textarea__inner::placeholder) {
  color: var(--text-tertiary) !important;
}

.contact-form :deep(.el-input__wrapper:hover),
.link-form-wrapper :deep(.el-input__wrapper:hover),
.contact-form :deep(.el-textarea__inner:hover),
.link-form-wrapper :deep(.el-textarea__inner:hover) {
  border-color: var(--accent) !important;
}

.contact-form :deep(.el-input__wrapper.is-focus),
.link-form-wrapper :deep(.el-input__wrapper.is-focus),
.contact-form :deep(.el-textarea__inner:focus),
.link-form-wrapper :deep(.el-textarea__inner:focus) {
  border-color: var(--accent) !important;
  box-shadow: 0 0 0 3px var(--accent-light) !important;
}

.contact-form :deep(.el-form-item),
.link-form-wrapper :deep(.el-form-item) {
  margin-bottom: 20px;
}

.submit-btn {
  width: 100%;
  height: 48px;
  border-radius: 14px !important;
  font-size: 1rem;
  background: var(--gradient-primary) !important;
  border: none !important;
  margin-top: 8px;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

.submit-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 24px rgba(57, 197, 187, 0.4);
}

.contact-info {
  background: var(--gradient-primary);
  border-radius: 18px;
  padding: 28px;
  color: #fff;
}

.contact-info h3 {
  font-size: 1.1rem;
  margin-bottom: 20px;
  opacity: 0.95;
}

.contact-list {
  list-style: none;
  padding: 0;
  margin: 0;
}

.contact-list li {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 0;
  border-bottom: 1px solid rgba(255, 255, 255, 0.2);
  font-size: 0.95rem;
}

.contact-list li:last-child {
  border-bottom: none;
}

.contact-icon {
  font-size: 1.2rem;
  width: 28px;
  text-align: center;
}

.sites-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(220px, 1fr));
  gap: 16px;
}

.site-card {
  background: var(--bg-tertiary);
  border: 1px solid var(--border-color);
  border-radius: 18px;
  padding: 24px;
  display: flex;
  flex-direction: column;
  align-items: center;
  text-align: center;
  text-decoration: none;
  transition: all 0.35s cubic-bezier(0.4, 0, 0.2, 1);
}

.site-card:hover {
  transform: translateY(-6px);
  box-shadow: var(--shadow-lg);
  border-color: var(--accent);
}

.site-icon {
  font-size: 3rem;
  margin-bottom: 16px;
}

.site-info {
  flex: 1;
}

.site-name {
  color: var(--text-primary);
  font-size: 1.1rem;
  margin-bottom: 6px;
}

.site-desc {
  color: var(--text-tertiary);
  font-size: 0.85rem;
}

.site-link {
  color: var(--accent);
  font-weight: 500;
  margin-top: 16px;
  transition: transform 0.25s ease;
}

.site-card:hover .site-link {
  transform: translateX(4px);
  display: inline-block;
}

.links-section {
  margin-bottom: 40px;
}

.links-category {
  color: var(--text-primary);
  font-size: 1.1rem;
  margin-bottom: 20px;
  padding-left: 4px;
}

.links-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(200px, 1fr));
  gap: 14px;
}

.empty-links {
  text-align: center;
  padding: 40px 20px;
  color: var(--text-tertiary);
}

.link-card {
  background: var(--bg-tertiary);
  border: 1px solid var(--border-color);
  border-radius: 14px;
  padding: 18px;
  text-decoration: none;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

.link-card:hover {
  transform: translateY(-4px);
  box-shadow: var(--shadow-md);
  border-color: var(--accent);
}

.link-name {
  color: var(--text-primary);
  font-weight: 600;
  margin-bottom: 4px;
}

.link-desc {
  color: var(--text-tertiary);
  font-size: 0.85rem;
  line-height: 1.5;
}

.submit-link-section {
  background: var(--bg-tertiary);
  border: 1px solid var(--border-light);
  border-radius: 18px;
  padding: 28px;
}

.submit-link-section h3 {
  color: var(--text-primary);
  font-size: 1.1rem;
  margin-bottom: 8px;
}

.submit-link-section > p {
  color: var(--text-secondary);
  margin-bottom: 24px;
}

.footer {
  background: var(--bg-card);
  padding: 28px;
  text-align: center;
  color: var(--text-tertiary);
  font-size: 0.9rem;
  border-top: 1px solid var(--border-color);
  margin-top: 40px;
}

@keyframes fadeInUp {
  from {
    opacity: 0;
    transform: translateY(20px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.animate-fade-in-up {
  animation: fadeInUp 0.4s ease-out;
}

@media (max-width: 768px) {
  .contact-wrapper {
    grid-template-columns: 1fr;
  }

  .content-card {
    padding: 24px;
  }

  .about-nav {
    flex-wrap: wrap;
  }

  .tab-btn {
    padding: 10px 16px;
    font-size: 0.9rem;
  }
}
</style>
