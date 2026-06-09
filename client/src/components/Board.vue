<script setup lang="ts">
import { ref } from 'vue';
import { Chess } from 'chess.js';

const props = defineProps<{ size?: number }>();
const boardSize = ref(props.size ?? 704);

function colour(square: number) {
    const row = Math.floor((square - 1) / 8);
    const col = (square - 1) % 8;
    return (row + col) % 2 === 0 ? 'bg-cyan-300' : 'bg-cyan-700';
}

// ── resize ────────────────────────────────────────────────────────────────────

let startSize = 0;
let startLeft = 0;
let startTop = 0;
const boardEl = ref<HTMLElement | null>(null);
let styleEl: HTMLStyleElement | null = null;

function setCursor(cursor: string) {
    if (!styleEl) {
        styleEl = document.createElement('style');
        document.head.appendChild(styleEl);
    }
    styleEl.textContent = cursor ? `* { cursor: ${cursor} !important; }` : '';
}

function onResizeDown(e: MouseEvent) {
    setCursor('se-resize');
    const rect = boardEl.value!.getBoundingClientRect();
    startLeft = rect.left;
    startTop = rect.top;
    startSize = boardSize.value;
    window.addEventListener('mousemove', onResizeMove);
    window.addEventListener('mouseup', onResizeUp);
}

function onResizeMove(e: MouseEvent) {
    const dx = e.clientX - startLeft;
    const dy = e.clientY - startTop;
    boardSize.value = Math.max(160, (dx + dy) / 2);
}

function onResizeUp() {
    setCursor('');
    window.removeEventListener('mousemove', onResizeMove);
    window.removeEventListener('mouseup', onResizeUp);
}

// ── pieces ────────────────────────────────────────────────────────────────────

const chess = new Chess();
const board = ref(chess.board());

const pieceAssets = import.meta.glob('../assets/pieces/*.svg', {
    eager: true,
    import: 'default',
}) as Record<string, string>;

function pieceAsset(square: number): string | null {
    const row = Math.floor((square - 1) / 8);
    const col = (square - 1) % 8;
    const p = board.value[row]?.[col];
    if (!p) return null;
    const color = p.color === 'w' ? 'l' : 'd';
    return pieceAssets[`../assets/pieces/Chess_${p.type}${color}t45.svg`] ?? null;
}

// ── drag & drop ───────────────────────────────────────────────────────────────

const dragging = ref<{ fromSquare: number; asset: string; x: number; y: number } | null>(null);

function squareToNotation(square: number): string {
    const col = (square - 1) % 8;
    const row = Math.floor((square - 1) / 8);
    return `${'abcdefgh'[col]}${8 - row}`;
}

function squareFromPoint(x: number, y: number): number | null {
    if (!boardEl.value) return null;
    const rect = boardEl.value.getBoundingClientRect();
    const relX = x - rect.left;
    const relY = y - rect.top;
    if (relX < 0 || relY < 0 || relX > rect.width || relY > rect.height) return null;
    const col = Math.floor((relX / rect.width) * 8);
    const row = Math.floor((relY / rect.height) * 8);
    return row * 8 + col + 1;
}

function onPiecePointerDown(e: PointerEvent, square: number) {
    const asset = pieceAsset(square);
    if (!asset) return;
    e.preventDefault();
    e.stopPropagation();
    dragging.value = { fromSquare: square, asset, x: e.clientX, y: e.clientY };
    setCursor('grabbing');
    window.addEventListener('pointermove', onDragMove);
    window.addEventListener('pointerup', onDragUp);
}

function onDragMove(e: PointerEvent) {
    if (!dragging.value) return;
    dragging.value = { ...dragging.value, x: e.clientX, y: e.clientY };
}

function onDragUp(e: PointerEvent) {
    if (!dragging.value) return;
    const toSquare = squareFromPoint(e.clientX, e.clientY);
    if (toSquare !== null && toSquare !== dragging.value.fromSquare) {
        try {
            chess.move({
                from: squareToNotation(dragging.value.fromSquare),
                to: squareToNotation(toSquare),
                promotion: 'q', // auto-queen for now
            });
            board.value = chess.board();
        } catch {
            // illegal move — piece snaps back automatically since board.value didn't change
        }
    }
    dragging.value = null;
    setCursor('');
    window.removeEventListener('pointermove', onDragMove);
    window.removeEventListener('pointerup', onDragUp);
}

// ── random play ───────────────────────────────────────────────────────────────

const delay = (ms: number) => new Promise((resolve) => setTimeout(resolve, ms));

async function play() {
    while (!chess.isGameOver()) {
        const moves = chess.moves();
        const move = moves[Math.floor(Math.random() * moves.length)];
        if (!move) break;
        chess.move(move);
        board.value = chess.board();
        await delay(1000);
    }
}

// play();
</script>

<template>
    <div class="relative select-none" ref="boardEl" :style="{ width: `${boardSize}px` }">
        <div class="grid grid-cols-8" style="aspect-ratio: 1 / 1">
            <div
                v-for="square in 64"
                :key="square"
                class="relative aspect-square flex items-center justify-center"
                :class="colour(square)"
            >
                <img
                    v-if="pieceAsset(square)"
                    :src="pieceAsset(square)!"
                    class="w-full h-full"
                    :class="{ 'opacity-0': dragging?.fromSquare === square }"
                    draggable="false"
                    @pointerdown="(e) => onPiecePointerDown(e, square)"
                />
            </div>
        </div>

        <!-- floating ghost while dragging -->
        <Teleport to="body">
            <img
                v-if="dragging"
                :src="dragging.asset"
                class="fixed pointer-events-none z-50"
                draggable="false"
                :style="{
                    left: `${dragging.x}px`,
                    top: `${dragging.y}px`,
                    width: `${boardSize / 8}px`,
                    height: `${boardSize / 8}px`,
                    transform: 'translate(-50%, -50%)',
                }"
            />
        </Teleport>

        <!-- resize handle -->
        <div
            class="absolute bottom-0 right-0 w-4 h-4 cursor-se-resize"
            @mousedown.prevent="onResizeDown"
        ></div>
    </div>
</template>