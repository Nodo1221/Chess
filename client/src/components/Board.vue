<script setup lang="ts">
import { ref } from 'vue';
import { Chess } from 'chess.js';

const props = defineProps<{ size?: number }>();
const boardSize = ref(props.size ?? 704);

function colour(square: number) {
    const row = Math.floor((square - 1) / 8);
    const col = (square - 1) % 8;
    return (row + col) % 2 === 0 ? 'bg-red-700' : 'bg-black';
}

let startSize = 0;
const boardEl = ref<HTMLElement | null>(null); // attach with ref="boardEl"

let styleEl: HTMLStyleElement | null = null;

function setCursor(cursor: string) {
    if (!styleEl) {
        styleEl = document.createElement('style');
        document.head.appendChild(styleEl);
    }
    styleEl.textContent = cursor ? `* { cursor: ${cursor} !important; }` : '';
}

let startLeft = 0;
let startTop = 0;

function onMouseDown(e: MouseEvent) {
    setCursor('se-resize');
    const rect = boardEl.value!.getBoundingClientRect();
    startLeft = rect.left;
    startTop = rect.top;
    startSize = boardSize.value;

    window.addEventListener('mousemove', onMouseMove);
    window.addEventListener('mouseup', onMouseUp);
}

function onMouseMove(e: MouseEvent) {
    const dx = e.clientX - startLeft;
    const dy = e.clientY - startTop;
    boardSize.value = Math.max(160, (dx + dy) / 2);
}

function onMouseUp() {
    setCursor('');
    document.body.style.setProperty('cursor', '', '');

    window.removeEventListener('mousemove', onMouseMove);
    window.removeEventListener('mouseup', onMouseUp);
}

const chess = new Chess();
const board = ref(chess.board());

async function play() {
    while (!chess.isGameOver()) {
        const moves = chess.moves();
        const move = moves[Math.floor(Math.random() * moves.length)];
        if (!move) break;

        chess.move(move);
        board.value = chess.board(); // trigger re-render
        await delay(1000);
    }
}

function piece(square: number) {
    const row = Math.floor((square - 1) / 8);
    const col = (square - 1) % 8;
    const piece = board.value[row]?.[col];

    return piece?.color === 'w' ? piece?.type.toUpperCase() : piece?.type;
}

const delay = (ms: number) => new Promise((resolve) => setTimeout(resolve, ms));

// play();
</script>

<template>
    <div class="relative select-none" ref="boardEl" :style="{ width: `${boardSize}px` }">
        <div class="grid grid-cols-8" style="aspect-ratio: 1 / 1">
            <div
                v-for="square in 64"
                :key="square"
                class="relative aspect-square flex items-center justify-center text-xl"
                :class="colour(square)"
            >
                {{ piece(square) }}
            </div>
        </div>

        <!-- custom resize handle -->
        <div
            class="absolute bottom-0 right-0 w-4 h-4 cursor-se-resize"
            @mousedown.prevent="onMouseDown"
        ></div>
    </div>
</template>
