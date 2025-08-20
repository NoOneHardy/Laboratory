<script setup lang="ts">
import TodoItem from '@/components/TodoItem.vue'

defineProps<{
  msg: string
}>()
</script>

<template>
  <TodoItem/>
</template>
