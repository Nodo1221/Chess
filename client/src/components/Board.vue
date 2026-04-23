<script setup lang="ts">
import { computed, ref } from 'vue';
import { Chess } from 'chess.js';

const props = defineProps<{ size?: number }>();
const boardSize = ref(props.size ?? 704);

function colour(square: number) {
    const row = Math.floor((square - 1) / 8);
    const col = (square - 1) % 8;
    return (row + col) % 2 === 0 ? 'bg-red-700' : 'bg-black';
}

let startX = 0;
let startSize = 0;

function onMouseDown(e: MouseEvent) {
    // document.body.style.cursor = 'se-resize !important';
    document.body.style.setProperty('cursor', 'se-resize', 'important');

    startX = e.clientX;
    startSize = boardSize.value;

    window.addEventListener('mousemove', onMouseMove);
    window.addEventListener('mouseup', onMouseUp);
}

function onMouseMove(e: MouseEvent) {
    boardSize.value = Math.max(160, startSize + (e.clientX - startX) / 1.2);
}

function onMouseUp() {
    // document.body.style.cursor = '';
    document.body.style.setProperty('cursor', '', '');

    window.removeEventListener('mousemove', onMouseMove);
    window.removeEventListener('mouseup', onMouseUp);
}

const chess = new Chess();
const board = computed(() => chess.board());

function piece(square: number) {
    const row = Math.floor((square - 1) / 8);
    const col = (square - 1) % 8;

    return board.value[row]?.[col]?.type;
}

</script>

<template>
    <div class="relative select-none" :style="{ width: `${boardSize}px` }">
        <div class="grid grid-cols-8" style="aspect-ratio: 1 / 1">
            <div
  v-for="square in 64"
  :key="square"
  class="relative aspect-square flex items-center justify-center text-xl"
  :class="colour(square)"
>{{ piece(square) }}</div>
        </div>

        <!-- custom resize handle -->
        <div class="absolute bottom-0 right-0 w-4 h-4 cursor-se-resize" @mousedown.prevent="onMouseDown"></div>
    </div>
</template>
