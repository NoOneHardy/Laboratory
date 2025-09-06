<script setup lang="ts">
import {Pawn} from '../model/pieces/pawn'

defineProps<{
  piece: Pawn
}>()

</script>

<template>
  <div v-if="piece">
    <h1 @click="piece.die()">P</h1>
  </div>
</template>
