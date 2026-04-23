import { computed, nextTick, onBeforeUnmount, ref, unref, watch } from 'vue'

const DEFAULT_BATCH_SIZE = 6
const OBSERVER_ROOT_MARGIN = '280px 0px'

export function useWaterfallList(sourceItems, options = {}) {
  const batchSize = options.batchSize ?? DEFAULT_BATCH_SIZE
  const enabledSource = options.enabled ?? true
  const sentinelRef = ref(null)
  const visibleCount = ref(0)

  let observer = null

  const items = computed(() => {
    const value = unref(sourceItems)
    return Array.isArray(value) ? value : []
  })

  const enabled = computed(() => Boolean(unref(enabledSource)))
  const visibleItems = computed(() => items.value.slice(0, visibleCount.value))
  const hasMore = computed(() => visibleCount.value < items.value.length)

  const stopObserving = () => {
    observer?.disconnect()
    observer = null
  }

  const loadMore = () => {
    if (!hasMore.value) {
      return
    }

    visibleCount.value = Math.min(visibleCount.value + batchSize, items.value.length)
  }

  const syncObserver = () => {
    stopObserving()

    if (
      typeof window === 'undefined' ||
      !enabled.value ||
      !hasMore.value ||
      !sentinelRef.value ||
      typeof IntersectionObserver === 'undefined'
    ) {
      return
    }

    observer = new IntersectionObserver((entries) => {
      if (entries.some((entry) => entry.isIntersecting)) {
        loadMore()
      }
    }, {
      rootMargin: OBSERVER_ROOT_MARGIN
    })

    observer.observe(sentinelRef.value)
  }

  const reset = async () => {
    visibleCount.value = Math.min(batchSize, items.value.length)
    await nextTick()
    syncObserver()
  }

  watch([items, enabled], reset, {
    immediate: true
  })

  watch([sentinelRef, hasMore], () => {
    syncObserver()
  })

  watch(visibleCount, async () => {
    await nextTick()
    syncObserver()
  })

  onBeforeUnmount(() => {
    stopObserving()
  })

  return {
    hasMore,
    loadMore,
    reset,
    sentinelRef,
    visibleItems
  }
}
