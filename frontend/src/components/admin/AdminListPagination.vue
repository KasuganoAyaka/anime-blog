<template>
  <div v-if="total > 0" class="admin-list-pagination">
    <el-pagination
      :current-page="currentPage"
      :page-size="pageSize"
      :page-sizes="pageSizes"
      :total="total"
      layout="total, sizes, prev, pager, next"
      background
      @update:current-page="emit('update:currentPage', $event)"
      @update:page-size="emit('update:pageSize', $event)"
    />
  </div>
</template>

<script setup>
defineProps({
  currentPage: {
    type: Number,
    default: 1
  },
  pageSize: {
    type: Number,
    default: 10
  },
  total: {
    type: Number,
    default: 0
  },
  pageSizes: {
    type: Array,
    default: () => [10, 20, 50]
  }
})

const emit = defineEmits(['update:currentPage', 'update:pageSize'])
</script>

<style scoped>
.admin-list-pagination {
  display: flex;
  justify-content: flex-start;
  margin-top: 18px;
}

.admin-list-pagination :deep(.el-pagination) {
  justify-content: flex-start;
}
</style>
