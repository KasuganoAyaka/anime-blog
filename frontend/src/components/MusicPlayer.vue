<template>
  <Teleport to="body">
    <Transition name="slide-up">
      <div
        v-if="props.visible"
        class="music-player-overlay"
        :class="{ dark: themeStore.isDark }"
        @click.self="$emit('update:visible', false)"
      >
        <div class="music-player" :class="{ dark: themeStore.isDark }">
          <button class="close-btn" @click="$emit('update:visible', false)">x</button>

          <div class="player-shell">
            <section class="player-main">
              <div class="disc-section">
                <div class="disc" :class="{ spinning: isPlaying }">
                  <div class="disc-cover">
                    <img v-if="currentMusic?.coverUrl" :src="currentMusic.coverUrl" alt="cover" />
                    <div v-else class="disc-placeholder">
                      <span>M</span>
                    </div>
                  </div>
                  <div class="disc-center"></div>
                </div>
                <div class="needle" :class="{ playing: isPlaying }"></div>
              </div>

              <div class="song-info">
                <h3 class="song-title">{{ currentMusic?.title || '暂无播放' }}</h3>
                <p class="song-artist">{{ currentMusic?.artist || '未知歌手' }}</p>
              </div>

              <div class="progress-section">
                <span class="time">{{ formatTime(currentTime) }}</span>
                <div class="progress-track" @click="seekTo">
                  <div class="progress-fill" :style="{ width: progressPercent + '%' }">
                    <div class="progress-dot"></div>
                  </div>
                </div>
                <span class="time">{{ formatTime(duration) }}</span>
              </div>

              <div class="controls">
                <button class="ctrl-btn mode-btn" @click="toggleMode" :title="modeText">
                  <svg v-if="playMode === 'loop'" viewBox="0 0 24 24" width="25" height="25" aria-hidden="true">
                    <path fill="currentColor" d="M7 7h10.59L15.3 4.71L16.71 3.3L21.41 8l-4.7 4.7l-1.41-1.41L17.59 9H9v4H7zm10 10H6.41l2.29 2.29l-1.41 1.41L2.59 16l4.7-4.7l1.41 1.41L6.41 15H15v-4h2z"/>
                  </svg>
                  <svg v-else-if="playMode === 'single'" viewBox="0 0 24 24" width="25" height="25" aria-hidden="true">
                    <path fill="currentColor" d="M7 7h10.59L15.3 4.71L16.71 3.3L21.41 8l-4.7 4.7l-1.41-1.41L17.59 9H9v4H7zm10 10H6.41l2.29 2.29l-1.41 1.41L2.59 16l4.7-4.7l1.41 1.41L6.41 15H15v-4h2z"/>
                    <path fill="currentColor" d="M10.3 10.4V9h2v8h-1.6v-5.3l-1.3.8l-.8-1.3z"/>
                  </svg>
                  <svg v-else viewBox="0 0 24 24" width="25" height="25" aria-hidden="true">
                    <path fill="currentColor" d="M18.59 5H21v2.41h-2V6.7l-4.88 4.88l-1.41-1.41zm-13.18 0h4.17l9 9H21v2h-3.24l-2.31-2.31L10.7 18.4a2 2 0 0 1-1.41.6H5v-2h4.29l4.75-4.75l-2-2l-4.63 4.63a2 2 0 0 1-1.41.59H3v-2h2.17l4.46-4.46L7.59 7H5.41z"/>
                  </svg>
                </button>

                <button class="ctrl-btn prev" @click="prevMusic">
                  <svg viewBox="0 0 24 24" width="24" height="24" aria-hidden="true">
                    <path fill="currentColor" d="M6 6h2v12H6zm3.5 6l8.5 6V6z"/>
                  </svg>
                </button>

                <button class="ctrl-btn play" @click="togglePlay">
                  <svg v-if="!isPlaying" viewBox="0 0 24 24" width="32" height="32" aria-hidden="true">
                    <path fill="currentColor" d="M8 5v14l11-7z"/>
                  </svg>
                  <svg v-else viewBox="0 0 24 24" width="32" height="32" aria-hidden="true">
                    <path fill="currentColor" d="M6 19h4V5H6v14zm8-14v14h4V5h-4z"/>
                  </svg>
                </button>

                <button class="ctrl-btn next" @click="nextMusic">
                  <svg viewBox="0 0 24 24" width="24" height="24" aria-hidden="true">
                    <path fill="currentColor" d="M6 18l8.5-6L6 6v12zM16 6v12h2V6h-2z"/>
                  </svg>
                </button>

                <button class="ctrl-btn mode-btn" @click="toggleMute" :title="`音量 ${volumeDisplay}`">
                  <svg v-if="volumeIcon === 'mute'" viewBox="0 0 24 24" width="23" height="23" aria-hidden="true">
                    <path fill="currentColor" d="M14 4.83V19.17L9.41 14.59H5V9.41h4.41zM16.5 12l2.5-2.5l1.5 1.5l-2.5 2.5l2.5 2.5l-1.5 1.5l-2.5-2.5l-2.5 2.5l-1.5-1.5l2.5-2.5l-2.5-2.5l1.5-1.5z"/>
                  </svg>
                  <svg v-else-if="volumeIcon === 'low'" viewBox="0 0 24 24" width="23" height="23" aria-hidden="true">
                    <path fill="currentColor" d="M14 4.83V19.17L9.41 14.59H5V9.41h4.41zM16.5 8.5a4.5 4.5 0 0 1 0 7z"/>
                  </svg>
                  <svg v-else-if="volumeIcon === 'medium'" viewBox="0 0 24 24" width="23" height="23" aria-hidden="true">
                    <path fill="currentColor" d="M14 4.83V19.17L9.41 14.59H5V9.41h4.41zM16.5 8.5a4.5 4.5 0 0 1 0 7zM18.6 6.2a7.5 7.5 0 0 1 0 11.6z"/>
                  </svg>
                  <svg v-else viewBox="0 0 24 24" width="23" height="23" aria-hidden="true">
                    <path fill="currentColor" d="M14 4.83V19.17L9.41 14.59H5V9.41h4.41zM16.5 8.5a4.5 4.5 0 0 1 0 7zM18.6 6.2a7.5 7.5 0 0 1 0 11.6zM20.8 4a10.5 10.5 0 0 1 0 16z"/>
                  </svg>
                </button>
              </div>

              <div class="volume-section">
                <div class="volume-row">
                  <div class="volume-icon" :title="`音量 ${volumeDisplay}`">
                    <svg v-if="volumeIcon === 'mute'" viewBox="0 0 24 24" width="22" height="22" aria-hidden="true">
                      <path fill="currentColor" d="M14 4.83V19.17L9.41 14.59H5V9.41h4.41zM16.5 12l2.5-2.5l1.5 1.5l-2.5 2.5l2.5 2.5l-1.5 1.5l-2.5-2.5l-2.5 2.5l-1.5-1.5l2.5-2.5l-2.5-2.5l1.5-1.5z"/>
                    </svg>
                    <svg v-else-if="volumeIcon === 'low'" viewBox="0 0 24 24" width="22" height="22" aria-hidden="true">
                      <path fill="currentColor" d="M14 4.83V19.17L9.41 14.59H5V9.41h4.41zM16.5 8.5a4.5 4.5 0 0 1 0 7z"/>
                    </svg>
                    <svg v-else-if="volumeIcon === 'medium'" viewBox="0 0 24 24" width="22" height="22" aria-hidden="true">
                      <path fill="currentColor" d="M14 4.83V19.17L9.41 14.59H5V9.41h4.41zM16.5 8.5a4.5 4.5 0 0 1 0 7zM18.6 6.2a7.5 7.5 0 0 1 0 11.6z"/>
                    </svg>
                    <svg v-else viewBox="0 0 24 24" width="22" height="22" aria-hidden="true">
                      <path fill="currentColor" d="M14 4.83V19.17L9.41 14.59H5V9.41h4.41zM16.5 8.5a4.5 4.5 0 0 1 0 7zM18.6 6.2a7.5 7.5 0 0 1 0 11.6zM20.8 4a10.5 10.5 0 0 1 0 16z"/>
                    </svg>
                  </div>

                  <input
                    v-model="volume"
                    type="range"
                    min="0"
                    max="100"
                    class="volume-slider"
                    @input="changeVolume"
                  />

                  <span class="volume-value">{{ volumeDisplay }}</span>
                </div>
              </div>
            </section>

            <aside class="playlist-panel">
              <div class="playlist-header">
                <span>播放列表</span>
                <span class="count">{{ musicList.length }} 首</span>
              </div>

              <div class="playlist">
                <div
                  v-for="(music, index) in musicList"
                  :key="music.id"
                  class="playlist-item"
                  :class="{ active: currentIndex === index }"
                  @click="playMusic(index)"
                >
                  <div class="item-left">
                    <span class="item-num">{{ String(index + 1).padStart(2, '0') }}</span>
                    <div class="item-info">
                      <span class="item-title">{{ music.title }}</span>
                      <span class="item-artist">{{ music.artist }}</span>
                    </div>
                  </div>
                  <span class="item-duration">{{ formatTime(music.duration) }}</span>
                </div>

                <div v-if="musicList.length === 0" class="empty-list">
                  暂无音乐，去后台添加吧
                </div>
              </div>
            </aside>
          </div>
        </div>
      </div>
    </Transition>
  </Teleport>
</template>

<script setup>
import { computed, onMounted, onUnmounted, ref, watch } from 'vue'
import { publicApi } from '@/api'
import { useThemeStore } from '@/stores'

const props = defineProps({
  visible: { type: Boolean, default: false }
})

defineEmits(['update:visible'])

const themeStore = useThemeStore()
const musicList = ref([])
const currentIndex = ref(0)
const isPlaying = ref(false)
const currentTime = ref(0)
const duration = ref(0)
const volume = ref(70)
const isMuted = ref(false)
const playMode = ref('loop')

const MUSIC_LIST_UPDATED_EVENT = 'music-list-updated'
const MUSIC_LIST_UPDATED_STORAGE_KEY = 'music_list_updated'

const currentMusic = computed(() => musicList.value[currentIndex.value] || null)
const progressPercent = computed(() => duration.value ? (currentTime.value / duration.value) * 100 : 0)
const numericVolume = computed(() => {
  const nextVolume = Number(volume.value)
  if (!Number.isFinite(nextVolume)) return 0
  return Math.max(0, Math.min(100, Math.round(nextVolume)))
})
const volumeDisplay = computed(() => String(numericVolume.value))
const volumeIcon = computed(() => {
  if (numericVolume.value <= 0 || isMuted.value) return 'mute'
  if (numericVolume.value <= 35) return 'low'
  if (numericVolume.value <= 70) return 'medium'
  return 'high'
})
const modeText = computed(() => {
  const modes = {
    loop: '列表循环',
    single: '单曲循环',
    random: '随机播放'
  }
  return modes[playMode.value]
})

let audio = null

const handleTimeUpdate = () => {
  if (!audio) return
  currentTime.value = audio.currentTime
}

const handleLoadedMetadata = () => {
  if (!audio) return
  duration.value = Number.isFinite(audio.duration) ? audio.duration : Number(currentMusic.value?.duration || 0)
}

const handleAudioEnded = () => {
  handleEnded()
}

const handleAudioError = () => {
  isPlaying.value = false
}

const removeAudioListeners = (target) => {
  if (!target) return
  target.removeEventListener('timeupdate', handleTimeUpdate)
  target.removeEventListener('loadedmetadata', handleLoadedMetadata)
  target.removeEventListener('ended', handleAudioEnded)
  target.removeEventListener('error', handleAudioError)
}

const destroyAudio = () => {
  if (!audio) return
  audio.pause()
  removeAudioListeners(audio)
  audio.src = ''
  audio = null
}

const formatTime = (seconds) => {
  if (!seconds || isNaN(seconds)) return '00:00'
  const mins = Math.floor(seconds / 60)
  const secs = Math.floor(seconds % 60)
  return `${String(mins).padStart(2, '0')}:${String(secs).padStart(2, '0')}`
}

const initAudio = () => {
  destroyAudio()

  if (!currentMusic.value?.url) {
    currentTime.value = 0
    duration.value = 0
    return
  }

  audio = new Audio()
  audio.src = currentMusic.value.url
  audio.volume = numericVolume.value / 100
  audio.muted = isMuted.value || numericVolume.value === 0
  currentTime.value = 0
  duration.value = Number(currentMusic.value?.duration || 0)

  audio.addEventListener('timeupdate', handleTimeUpdate)
  audio.addEventListener('loadedmetadata', handleLoadedMetadata)
  audio.addEventListener('ended', handleAudioEnded)
  audio.addEventListener('error', handleAudioError)
}

const fetchMusicList = async () => {
  try {
    const previousMusicId = currentMusic.value?.id ?? null
    const previousMusicUrl = currentMusic.value?.url ?? ''
    const res = await publicApi.getMusicList()
    const nextList = Array.isArray(res.data) ? res.data : []
    musicList.value = nextList

    if (nextList.length === 0) {
      destroyAudio()
      currentIndex.value = 0
      currentTime.value = 0
      duration.value = 0
      isPlaying.value = false
      return
    }

    if (previousMusicId != null) {
      const matchedIndex = nextList.findIndex((music) => music.id === previousMusicId)
      currentIndex.value = matchedIndex >= 0 ? matchedIndex : Math.min(currentIndex.value, nextList.length - 1)
    } else {
      currentIndex.value = Math.min(currentIndex.value, nextList.length - 1)
    }

    const nextMusicUrl = currentMusic.value?.url ?? ''
    if (!audio || nextMusicUrl !== previousMusicUrl) {
      initAudio()
    } else {
      duration.value = Number(currentMusic.value?.duration || audio.duration || 0)
    }
  } catch (error) {
    console.error('获取音乐失败:', error)
  }
}

const safePlayCurrent = async () => {
  if (!audio || !currentMusic.value) return
  try {
    await audio.play()
    isPlaying.value = true
  } catch {
    isPlaying.value = false
  }
}

const handleEnded = () => {
  if (!audio || musicList.value.length === 0) return

  if (playMode.value === 'single') {
    audio.currentTime = 0
    safePlayCurrent()
    return
  }

  if (playMode.value === 'random') {
    currentIndex.value = Math.floor(Math.random() * musicList.value.length)
    initAudio()
    safePlayCurrent()
    return
  }

  nextMusic()
}

const togglePlay = () => {
  if (!audio || !currentMusic.value) return

  if (isPlaying.value) {
    audio.pause()
    isPlaying.value = false
    return
  }

  safePlayCurrent()
}

const playMusic = (index) => {
  if (index < 0 || index >= musicList.value.length) return
  currentIndex.value = index
  initAudio()
  safePlayCurrent()
}

const prevMusic = () => {
  if (musicList.value.length === 0) return
  const shouldResume = isPlaying.value
  currentIndex.value = (currentIndex.value - 1 + musicList.value.length) % musicList.value.length
  initAudio()
  if (shouldResume) safePlayCurrent()
}

const nextMusic = () => {
  if (musicList.value.length === 0) return
  const shouldResume = isPlaying.value
  currentIndex.value = (currentIndex.value + 1) % musicList.value.length
  initAudio()
  if (shouldResume) safePlayCurrent()
}

const seekTo = (event) => {
  if (!audio || !duration.value) return
  const rect = event.currentTarget.getBoundingClientRect()
  const percent = (event.clientX - rect.left) / rect.width
  audio.currentTime = Math.max(0, Math.min(duration.value, percent * duration.value))
}

const changeVolume = () => {
  volume.value = numericVolume.value
  if (audio) {
    audio.volume = numericVolume.value / 100
    audio.muted = numericVolume.value === 0
  }
  isMuted.value = numericVolume.value === 0
}

const toggleMute = () => {
  if (numericVolume.value <= 0 || isMuted.value) {
    volume.value = 70
    isMuted.value = false
    if (audio) {
      audio.volume = 0.7
      audio.muted = false
    }
    return
  }

  volume.value = 0
  isMuted.value = true
  if (audio) {
    audio.volume = 0
    audio.muted = true
  }
}

const toggleMode = () => {
  const modes = ['loop', 'single', 'random']
  const currentModeIndex = modes.indexOf(playMode.value)
  playMode.value = modes[(currentModeIndex + 1) % modes.length]
}

const handleMusicListUpdated = () => {
  fetchMusicList()
}

const handleStorageChange = (event) => {
  if (event.key === MUSIC_LIST_UPDATED_STORAGE_KEY) {
    fetchMusicList()
  }
}

watch(() => props.visible, (nextVisible) => {
  if (nextVisible) {
    fetchMusicList()
  }
})

onMounted(() => {
  if (props.visible) {
    fetchMusicList()
  }
  window.addEventListener(MUSIC_LIST_UPDATED_EVENT, handleMusicListUpdated)
  window.addEventListener('storage', handleStorageChange)
})

onUnmounted(() => {
  window.removeEventListener(MUSIC_LIST_UPDATED_EVENT, handleMusicListUpdated)
  window.removeEventListener('storage', handleStorageChange)
  destroyAudio()
})
</script>

<style scoped>
.music-player-overlay {
  position: fixed;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(255, 255, 255, 0.7);
  backdrop-filter: blur(12px);
  z-index: 9999;
}

.music-player-overlay.dark {
  background: rgba(0, 0, 0, 0.6);
}

.music-player {
  width: min(920px, calc(100vw - 32px));
  height: min(720px, calc(100vh - 40px));
  max-height: calc(100vh - 40px);
  padding: 24px;
  border-radius: 28px;
  position: relative;
  overflow: hidden;
  box-shadow:
    0 25px 50px rgba(0, 0, 0, 0.15),
    0 0 0 1px rgba(255, 255, 255, 0.1) inset;
  background: linear-gradient(135deg, rgba(255, 255, 255, 0.92), rgba(248, 250, 252, 0.88));
  backdrop-filter: blur(20px) saturate(180%);
  -webkit-backdrop-filter: blur(20px) saturate(180%);
  border: 1px solid rgba(255, 255, 255, 0.3);
}

.music-player.dark {
  background: linear-gradient(135deg, rgba(26, 26, 46, 0.92), rgba(22, 33, 62, 0.88));
  border: 1px solid rgba(255, 255, 255, 0.08);
  box-shadow:
    0 25px 50px rgba(0, 0, 0, 0.4),
    0 0 0 1px rgba(255, 255, 255, 0.05) inset;
}

.close-btn {
  position: absolute;
  top: 12px;
  right: 12px;
  width: 34px;
  height: 34px;
  border: none;
  border-radius: 999px;
  background: rgba(0, 0, 0, 0.06);
  color: #556070;
  font-size: 18px;
  cursor: pointer;
  z-index: 4;
  transition: all 0.2s ease;
}

.close-btn:hover {
  transform: scale(1.08) rotate(90deg);
  background: rgba(0, 0, 0, 0.12);
}

.music-player.dark .close-btn {
  background: rgba(255, 255, 255, 0.1);
  color: #fff;
}

.music-player.dark .close-btn:hover {
  background: rgba(255, 255, 255, 0.2);
  color: #39c5bb;
}

.player-shell {
  height: 100%;
  display: grid;
  grid-template-columns: minmax(0, 1fr) 320px;
  gap: 24px;
}

.player-main {
  min-width: 0;
  min-height: 0;
  display: flex;
  flex-direction: column;
  justify-content: center;
  padding: 8px 8px 8px 4px;
}

.playlist-panel {
  min-width: 0;
  min-height: 0;
  display: flex;
  flex-direction: column;
  padding: 12px 10px 12px 18px;
  border-left: 1px solid rgba(0, 0, 0, 0.08);
}

.music-player.dark .playlist-panel {
  border-left-color: rgba(255, 255, 255, 0.08);
}

.disc-section {
  position: relative;
  display: flex;
  justify-content: center;
  align-items: center;
  margin-bottom: 24px;
  padding-top: 16px;
  flex-shrink: 0;
}

.disc {
  width: 200px;
  height: 200px;
  border-radius: 50%;
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(145deg, #e8e8e8, #f5f5f5);
  box-shadow: 0 0 0 10px rgba(0, 0, 0, 0.03), 0 20px 40px rgba(0, 0, 0, 0.1);
}

.music-player.dark .disc {
  background: linear-gradient(145deg, #2d2d44, #1e1e30);
  box-shadow: 0 0 0 10px rgba(255, 255, 255, 0.05), 0 20px 40px rgba(0, 0, 0, 0.3);
}

.disc.spinning {
  animation: spin 8s linear infinite;
}

.disc-cover {
  width: 132px;
  height: 132px;
  border-radius: 50%;
  overflow: hidden;
  border: 3px solid rgba(0, 0, 0, 0.05);
}

.music-player.dark .disc-cover {
  border-color: rgba(255, 255, 255, 0.1);
}

.disc-cover img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.disc-placeholder {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #39c5bb, #667eea);
  color: #fff;
  font-size: 2rem;
  font-weight: 700;
}

.disc-center {
  position: absolute;
  width: 20px;
  height: 20px;
  border-radius: 50%;
  background: #fff;
  border: 3px solid #ddd;
}

.music-player.dark .disc-center {
  background: #1a1a2e;
  border-color: #333;
}

.needle {
  position: absolute;
  top: -6px;
  right: calc(50% - 116px);
  width: 8px;
  height: 66px;
  border-radius: 4px;
  transform-origin: top center;
  transform: rotate(-30deg);
  transition: transform 0.3s ease;
  background: linear-gradient(to bottom, #aaa, #888);
}

.needle::after {
  content: '';
  position: absolute;
  bottom: -8px;
  left: -4px;
  width: 16px;
  height: 16px;
  border-radius: 50%;
  background: #39c5bb;
}

.needle.playing {
  transform: rotate(0deg);
}

.music-player.dark .needle {
  background: linear-gradient(to bottom, #666, #333);
}

@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}

.song-info {
  text-align: center;
  margin-bottom: 22px;
  flex-shrink: 0;
}

.song-title {
  margin: 0 0 8px;
  color: #1a1a2e;
  font-size: 1.35rem;
  font-weight: 700;
}

.song-artist {
  margin: 0;
  color: #728095;
  font-size: 0.95rem;
}

.music-player.dark .song-title {
  color: #fff;
}

.music-player.dark .song-artist {
  color: rgba(255, 255, 255, 0.65);
}

.progress-section {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 20px;
  padding: 0 8px;
  flex-shrink: 0;
}

.time {
  min-width: 40px;
  color: #8a96a8;
  font-size: 0.75rem;
}

.music-player.dark .time {
  color: rgba(255, 255, 255, 0.55);
}

.progress-track {
  flex: 1;
  height: 5px;
  border-radius: 999px;
  background: rgba(0, 0, 0, 0.08);
  cursor: pointer;
  position: relative;
}

.music-player.dark .progress-track {
  background: rgba(255, 255, 255, 0.1);
}

.progress-fill {
  height: 100%;
  border-radius: inherit;
  position: relative;
  background: linear-gradient(90deg, #39c5bb, #667eea);
}

.progress-dot {
  position: absolute;
  right: -5px;
  top: -3px;
  width: 11px;
  height: 11px;
  border-radius: 50%;
  background: #fff;
  box-shadow: 0 2px 6px rgba(0, 0, 0, 0.22);
}

.controls {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 16px;
  margin-bottom: 18px;
  flex-shrink: 0;
}

.ctrl-btn {
  border: none;
  border-radius: 999px;
  background: transparent;
  color: #7e8a9d;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s ease;
}

.ctrl-btn svg,
.volume-icon svg {
  display: block;
}

.ctrl-btn:hover {
  color: #39c5bb;
  background: rgba(0, 0, 0, 0.05);
}

.music-player.dark .ctrl-btn {
  color: rgba(255, 255, 255, 0.82);
}

.music-player.dark .ctrl-btn:hover {
  background: rgba(255, 255, 255, 0.1);
}

.ctrl-btn:not(.play) {
  width: 42px;
  height: 42px;
}

.ctrl-btn.play {
  width: 58px;
  height: 58px;
  color: #fff;
  background: linear-gradient(135deg, #39c5bb, #667eea);
  box-shadow: 0 6px 18px rgba(57, 197, 187, 0.28);
}

.ctrl-btn.play:hover {
  transform: scale(1.05);
  background: linear-gradient(135deg, #4fd4cc, #7789f0);
}

.volume-section {
  padding: 0 18px;
  flex-shrink: 0;
}

.volume-row {
  display: flex;
  align-items: center;
  gap: 12px;
}

.volume-icon {
  width: 28px;
  height: 28px;
  border-radius: 999px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #7e8a9d;
  background: rgba(0, 0, 0, 0.04);
  flex-shrink: 0;
}

.music-player.dark .volume-icon {
  color: rgba(255, 255, 255, 0.82);
  background: rgba(255, 255, 255, 0.08);
}

.volume-slider {
  flex: 1;
  width: 100%;
  height: 4px;
  -webkit-appearance: none;
  appearance: none;
  outline: none;
  border-radius: 999px;
  background: rgba(0, 0, 0, 0.08);
}

.music-player.dark .volume-slider {
  background: rgba(255, 255, 255, 0.1);
}

.volume-slider::-webkit-slider-thumb {
  -webkit-appearance: none;
  width: 12px;
  height: 12px;
  border-radius: 50%;
  background: #39c5bb;
  cursor: pointer;
}

.volume-value {
  min-width: 34px;
  text-align: right;
  color: #6f7c90;
  font-size: 0.85rem;
  font-weight: 700;
  font-variant-numeric: tabular-nums;
  flex-shrink: 0;
}

.music-player.dark .volume-value {
  color: rgba(255, 255, 255, 0.72);
}

.playlist-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 14px;
  padding-right: 23px;
  color: #6d788b;
  font-size: 0.92rem;
  font-weight: 600;
  flex-shrink: 0;
}

.music-player.dark .playlist-header {
  color: rgba(255, 255, 255, 0.7);
}

.playlist {
  flex: 1;
  min-height: 0;
  overflow-y: auto;
  padding-right: 6px;
  overscroll-behavior: contain;
  scrollbar-gutter: stable;
}

.playlist::-webkit-scrollbar {
  width: 6px;
}

.playlist::-webkit-scrollbar-track {
  background: rgba(0, 0, 0, 0.04);
  border-radius: 999px;
}

.playlist::-webkit-scrollbar-thumb {
  background: rgba(0, 0, 0, 0.22);
  border-radius: 999px;
}

.music-player.dark .playlist::-webkit-scrollbar-track {
  background: rgba(255, 255, 255, 0.06);
}

.music-player.dark .playlist::-webkit-scrollbar-thumb {
  background: rgba(255, 255, 255, 0.24);
}

.playlist-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  min-width: 0;
  padding: 12px;
  border-radius: 12px;
  cursor: pointer;
  transition: all 0.2s ease;
}

.playlist-item + .playlist-item {
  margin-top: 8px;
}

.playlist-item:hover {
  background: rgba(0, 0, 0, 0.035);
}

.music-player.dark .playlist-item:hover {
  background: rgba(255, 255, 255, 0.06);
}

.playlist-item.active {
  background: rgba(57, 197, 187, 0.12);
  box-shadow: inset 0 0 0 1px rgba(57, 197, 187, 0.22);
}

.playlist-item.active .item-title,
.playlist-item.active .item-num {
  color: #39c5bb;
}

.item-left {
  min-width: 0;
  flex: 1;
  display: flex;
  align-items: center;
  gap: 12px;
  overflow: hidden;
}

.item-num {
  min-width: 22px;
  color: #a0aabc;
  font-size: 0.8rem;
  font-weight: 700;
  flex-shrink: 0;
}

.music-player.dark .item-num {
  color: rgba(255, 255, 255, 0.34);
}

.item-info {
  min-width: 0;
  overflow: hidden;
}

.item-title {
  display: block;
  color: #2d3645;
  font-size: 0.92rem;
  font-weight: 600;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.item-artist {
  display: block;
  margin-top: 3px;
  color: #8e98a9;
  font-size: 0.75rem;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.music-player.dark .item-title {
  color: #fff;
}

.music-player.dark .item-artist,
.music-player.dark .item-duration {
  color: rgba(255, 255, 255, 0.45);
}

.item-duration {
  color: #8e98a9;
  font-size: 0.75rem;
  flex-shrink: 0;
}

.empty-list {
  text-align: center;
  padding: 28px 16px;
  color: #97a3b5;
  font-size: 0.92rem;
}

.music-player.dark .empty-list {
  color: rgba(255, 255, 255, 0.45);
}

.slide-up-enter-active {
  animation: fadeInScale 0.35s cubic-bezier(0.34, 1.56, 0.64, 1);
}

.slide-up-leave-active {
  animation: fadeOutScale 0.25s ease-out;
}

@keyframes fadeInScale {
  0% {
    opacity: 0;
    transform: scale(0.92);
  }
  100% {
    opacity: 1;
    transform: scale(1);
  }
}

@keyframes fadeOutScale {
  0% {
    opacity: 1;
    transform: scale(1);
  }
  100% {
    opacity: 0;
    transform: scale(0.96);
  }
}

@media (max-width: 840px) {
  .music-player {
    width: min(560px, calc(100vw - 24px));
    height: min(760px, calc(100vh - 24px));
    max-height: calc(100vh - 24px);
    padding: 18px;
  }

  .player-shell {
    grid-template-columns: 1fr;
    grid-template-rows: auto minmax(220px, 1fr);
    gap: 18px;
  }

  .player-main {
    padding: 4px;
  }

  .playlist-panel {
    padding: 14px 4px 0;
    border-left: none;
    border-top: 1px solid rgba(0, 0, 0, 0.08);
  }

  .music-player.dark .playlist-panel {
    border-top-color: rgba(255, 255, 255, 0.08);
  }

  .disc {
    width: 172px;
    height: 172px;
  }

  .disc-cover {
    width: 114px;
    height: 114px;
  }

  .needle {
    right: calc(50% - 100px);
  }
}
</style>
