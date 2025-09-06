<script setup lang="ts">

import {Board} from '@/model/board.ts'
import PieceComponent from '@/components/PieceComponent.vue';

const board = new Board()
</script>

<template>
  <main>
    <article
      id="dead-white"
      class="dead">
    </article>
    <section id="board">
      <div
        v-for="row in board.rows"
        :key="row.index"
        class="row">
        <div
          v-for="cell in row.cells"
          :key="cell.id"
          class="cell">
          <PieceComponent
            v-if="cell.piece"
            :piece="cell.piece" />
        </div>
      </div>
    </section>
    <article
      id="dead-black"
      class="dead"></article>
  </main>
</template>

<style scoped>
#board {
  display: grid;
  grid-template-columns: repeat(8, 1fr);
  grid-template-rows: repeat(8, 1fr);
  width: 800px;
  height: 800px;

  .cell {
    display: flex;
    height: 100%;
    width: 100%;
    background-color: #769656;
  }

  .row:nth-child(even) .cell:nth-child(even), .row:nth-child(odd) .cell:nth-child(odd) {
    background-color: #eeeed2;
  }
}
</style>
