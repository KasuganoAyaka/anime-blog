<template>
  <div v-if="enabled && siteKey" class="turnstile-widget">
    <div ref="containerRef" class="turnstile-widget__container"></div>
    <p v-if="helperText" class="turnstile-widget__helper">{{ helperText }}</p>
  </div>
</template>

<script setup>
import { nextTick, onBeforeUnmount, ref, watch } from 'vue'

const props = defineProps({
  enabled: {
    type: Boolean,
    default: false
  },
  siteKey: {
    type: String,
    default: ''
  },
  action: {
    type: String,
    default: ''
  },
  helperText: {
    type: String,
    default: ''
  },
  theme: {
    type: String,
    default: 'auto'
  }
})

const emit = defineEmits(['update:modelValue', 'error'])

const TURNSTILE_SCRIPT_SRC = 'https://challenges.cloudflare.com/turnstile/v0/api.js?render=explicit'

const containerRef = ref(null)
let widgetId = null

const loadTurnstileScript = () => {
  if (window.turnstile) {
    return Promise.resolve(window.turnstile)
  }

  if (window.__turnstileLoadPromise) {
    return window.__turnstileLoadPromise
  }

  window.__turnstileLoadPromise = new Promise((resolve, reject) => {
    const existingScript = document.querySelector(`script[src="${TURNSTILE_SCRIPT_SRC}"]`)
    if (existingScript) {
      existingScript.addEventListener('load', () => resolve(window.turnstile), { once: true })
      existingScript.addEventListener('error', reject, { once: true })
      return
    }

    const script = document.createElement('script')
    script.src = TURNSTILE_SCRIPT_SRC
    script.async = true
    script.defer = true
    script.onload = () => resolve(window.turnstile)
    script.onerror = reject
    document.head.appendChild(script)
  })

  return window.__turnstileLoadPromise
}

const clearToken = () => {
  emit('update:modelValue', '')
}

const removeWidget = () => {
  clearToken()
  if (widgetId !== null && window.turnstile) {
    window.turnstile.remove(widgetId)
    widgetId = null
  }
  if (containerRef.value) {
    containerRef.value.innerHTML = ''
  }
}

const renderWidget = async () => {
  if (!props.enabled || !props.siteKey || !containerRef.value || widgetId !== null) {
    return
  }

  try {
    const turnstile = await loadTurnstileScript()
    if (!turnstile || !containerRef.value || widgetId !== null) {
      return
    }

    widgetId = turnstile.render(containerRef.value, {
      sitekey: props.siteKey,
      action: props.action || undefined,
      theme: props.theme,
      callback: (token) => emit('update:modelValue', token || ''),
      'expired-callback': () => clearToken(),
      'timeout-callback': () => clearToken(),
      'error-callback': () => {
        clearToken()
        emit('error')
      }
    })
  } catch (error) {
    clearToken()
    emit('error')
  }
}

const reset = () => {
  clearToken()
  if (widgetId !== null && window.turnstile) {
    window.turnstile.reset(widgetId)
  }
}

defineExpose({ reset })

watch(
  () => [props.enabled, props.siteKey, props.action].join('|'),
  async () => {
    removeWidget()
    await nextTick()
    await renderWidget()
  },
  { immediate: true }
)

onBeforeUnmount(() => {
  removeWidget()
})
</script>

<style scoped>
.turnstile-widget {
  display: grid;
  gap: 10px;
}

.turnstile-widget__container {
  min-height: 65px;
}

.turnstile-widget__helper {
  margin: 0;
  font-size: 0.84rem;
  line-height: 1.5;
  color: var(--text-secondary);
}
</style>
