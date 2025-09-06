<script setup lang="ts" generic="P extends Piece">

import type {Piece} from '@/model/pieces/piece.ts';

defineProps<{
  piece: P
}>()
</script>

<template>
  <component
    :is="piece.component"
    :piece="piece" />
</template>

