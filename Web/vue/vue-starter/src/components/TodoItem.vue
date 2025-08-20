<script setup lang="ts">
import {ref} from 'vue'

const isDone = ref(false)
</script>

<template>
  <div class="item">
    {{ isDone }}
  </div>
  <button v-on:click="isDone = !isDone"><span class="material-symbols-rounded">check</span></button>
</template>

<style scoped>

</style>
