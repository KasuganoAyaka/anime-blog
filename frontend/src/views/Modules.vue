<template>
  <div class="modules">
    <div class="modules-container">
      <h1 class="page-title">🌀 {{ t('modules.title') }}</h1>
      <div class="modules-grid">
        <div
          v-for="module in modules"
          :key="module.id"
          class="module-card"
          @click="handleModuleClick(module)"
        >
          <div class="module-icon">{{ module.icon }}</div>
          <div class="module-content">
            <h3 class="module-title">{{ module.title }}</h3>
            <p class="module-desc">{{ module.description }}</p>
          </div>
        </div>
      </div>

      <div class="friends-section">
        <h2 class="section-title">🔗 {{ t('modules.linksTitle') }}</h2>
        <div class="friends-grid">
          <a
            v-for="friend in friends"
            :key="friend.id"
            :href="friend.url"
            target="_blank"
            class="friend-card"
          >
            <span class="friend-icon">🌐</span>
            <div class="friend-info">
              <span class="friend-name">{{ friend.name }}</span>
              <span class="friend-desc">{{ friend.description }}</span>
            </div>
          </a>
          <div v-if="friends.length === 0" class="no-friends">
            {{ t('modules.noFriends') }}
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { publicApi } from '@/api'
import { ElMessage } from 'element-plus'

const router = useRouter()
const { t } = useI18n()
const friends = ref([])

const modules = computed(() => ([
  {
    id: 1,
    title: t('modules.mainSite'),
    icon: '🏠',
    description: t('modules.mainSiteDesc'),
    action: () => router.push('/')
  },
  {
    id: 2,
    title: t('modules.drive'),
    icon: '☁️',
    description: t('modules.driveDesc'),
    action: () => ElMessage.info(t('modules.developing'))
  },
  {
    id: 3,
    title: t('modules.more'),
    icon: '✨',
    description: t('modules.moreDesc'),
    action: () => ElMessage.info(t('modules.developing'))
  }
]))

const handleModuleClick = (module) => {
  module.action()
}

const fetchFriends = async () => {
  try {
    const res = await publicApi.getFriendLinks()
    if (res.code === 200 && res.data) {
      friends.value = res.data
    }
  } catch (error) {
    console.error('Failed to fetch friend links:', error)
  }
}

onMounted(() => {
  fetchFriends()
})
</script>

<style scoped>
.modules {
  padding: 100px 2rem 4rem;
  min-height: 100vh;
  background: var(--gradient-bg);
}

.modules-container {
  max-width: 1200px;
  margin: 0 auto;
}

.page-title {
  text-align: center;
  font-size: 2.5rem;
  color: var(--text-primary);
  margin-bottom: 3rem;
}

.modules-grid {
  display: grid;
  gap: 1.5rem;
  margin-bottom: 4rem;
}

.module-card {
  background: var(--bg-card);
  backdrop-filter: blur(20px);
  border-radius: 20px;
  padding: 2rem;
  display: flex;
  align-items: center;
  gap: 1.5rem;
  border: 1px solid var(--border-color);
  cursor: pointer;
  transition: all 0.4s ease;
}

.module-card:hover {
  transform: translateX(20px);
  box-shadow: var(--shadow-lg);
  border-color: var(--accent);
}

.module-icon {
  font-size: 3rem;
}

.module-title {
  font-size: 1.5rem;
  color: var(--text-primary);
  margin-bottom: 0.5rem;
}

.module-desc {
  color: var(--text-secondary);
}

.friends-section {
  margin-top: 4rem;
}

.section-title {
  text-align: center;
  font-size: 2rem;
  color: var(--text-primary);
  margin-bottom: 2rem;
}

.friends-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 1rem;
}

.friend-card {
  display: flex;
  align-items: center;
  gap: 1rem;
  padding: 1.5rem;
  background: var(--bg-card);
  backdrop-filter: blur(10px);
  border-radius: 16px;
  border: 1px solid var(--border-color);
  text-decoration: none;
  transition: all 0.3s ease;
}

.friend-card:hover {
  transform: translateY(-5px);
  box-shadow: var(--shadow-lg);
  border-color: var(--accent);
}

.friend-icon {
  font-size: 2rem;
}

.friend-info {
  flex: 1;
  display: flex;
  flex-direction: column;
}

.friend-name {
  color: var(--text-primary);
  font-weight: 600;
}

.friend-desc {
  color: var(--text-secondary);
  font-size: 0.85rem;
}

.no-friends {
  grid-column: 1 / -1;
  text-align: center;
  padding: 3rem;
  color: var(--text-tertiary);
  font-size: 1.1rem;
}
</style>
