import { computed, nextTick, onBeforeUnmount, ref, unref, watch } from 'vue'

const OBSERVER_ROOT_MARGIN = '280px 0px'

export function useLoadMoreTrigger(onLoadMore, options = {}) {
  const enabledSource = options.enabled ?? true
  const sentinelRef = ref(null)
  const enabled = computed(() => Boolean(unref(enabledSource)))

  let observer = null

  const stopObserving = () => {
    observer?.disconnect()
    observer = null
  }

  const syncObserver = () => {
    stopObserving()

    if (
      typeof window === 'undefined' ||
      typeof IntersectionObserver === 'undefined' ||
      !enabled.value ||
      !sentinelRef.value
    ) {
      return
    }

    observer = new IntersectionObserver((entries) => {
      if (entries.some((entry) => entry.isIntersecting)) {
        onLoadMore()
      }
    }, {
      rootMargin: OBSERVER_ROOT_MARGIN
    })

    observer.observe(sentinelRef.value)
  }

  watch([sentinelRef, enabled], async () => {
    await nextTick()
    syncObserver()
  }, {
    immediate: true
  })

  onBeforeUnmount(() => {
    stopObserving()
  })

  return {
    sentinelRef
  }
}
