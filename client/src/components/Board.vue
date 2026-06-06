<script setup lang="ts">
import { ref, onMounted, onUnmounted, computed } from 'vue';
import { Chess } from 'chess.js';
import { useMatchmakingStore } from '@/stores/matchmaking';
import { useAuthStore } from '@/stores/auth';

const matchmakingStore = useMatchmakingStore();
const authStore = useAuthStore();

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
const gameHistory = ref<string[]>([]);
const viewIndex = ref(0);

const playerSide = computed(() => {
    if (!matchmakingStore.matchFound) return null;
    if (matchmakingStore.matchFound.whitePlayer.id === authStore.guestId) return 'w';
    if (matchmakingStore.matchFound.blackPlayer.id === authStore.guestId) return 'b';
    return null;
});

function syncBoard() {
    const c = new Chess();
    for (let i = 0; i < viewIndex.value; i++) c.move(gameHistory.value[i]);
    board.value = c.board();
}

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

matchmakingStore.setMoveHandler((move) => {
    if (move.playerId !== authStore.guestId) {
        try {
            chess.move(move.move);
            gameHistory.value = chess.history();
            viewIndex.value = gameHistory.value.length;
            syncBoard();
        } catch (e) {
            console.error('Invalid move received from server', e);
        }
    }
});

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
    if (viewIndex.value !== gameHistory.value.length) return;
    
    if (matchmakingStore.matchFound) {
        if (chess.turn() !== playerSide.value) return;
        
        const row = Math.floor((square - 1) / 8);
        const col = (square - 1) % 8;
        const p = board.value[row]?.[col];
        if (!p || p.color !== playerSide.value) return;
    }

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
        const moveData = {
            from: squareToNotation(dragging.value.fromSquare),
            to: squareToNotation(toSquare),
            promotion: 'q',
        };

        try {
            const move = chess.move(moveData);
            if (move) {
                gameHistory.value = chess.history();
                viewIndex.value = gameHistory.value.length;
                syncBoard();

                if (matchmakingStore.matchFound) {
                    matchmakingStore.sendMove(matchmakingStore.matchFound.gameId, {
                        move: moveData,
                        playerId: authStore.guestId
                    });
                }
            }
        } catch {
            // illegal move
        }
    }
    dragging.value = null;
    setCursor('');
    window.removeEventListener('pointermove', onDragMove);
    window.removeEventListener('pointerup', onDragUp);
}

function onKeyDown(e: KeyboardEvent) {
    if (e.key === 'ArrowLeft')  { viewIndex.value = Math.max(0, viewIndex.value - 1); syncBoard(); }
    if (e.key === 'ArrowRight') { viewIndex.value = Math.min(gameHistory.value.length, viewIndex.value + 1); syncBoard(); }
}

onMounted(() => window.addEventListener('keydown', onKeyDown));
onUnmounted(() => window.removeEventListener('keydown', onKeyDown));
</script>

<template>
    <div class="flex gap-4 items-start">
        <div class="relative select-none shrink-0" ref="boardEl" :style="{ width: `${boardSize}px` }">
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

            <div
                class="absolute bottom-0 right-0 w-4 h-4 cursor-se-resize"
                @mousedown.prevent="onResizeDown"
            ></div>
        </div>

        <div class="font-mono text-sm w-32 max-h-96 overflow-y-auto shrink-0">
            <div v-for="(_, i) in Math.ceil(gameHistory.length / 2)" :key="i" class="flex gap-2">
                <span class="text-gray-500 w-5">{{ i + 1 }}.</span>
                <span :class="{ 'text-yellow-600': viewIndex === i * 2 + 1 }">{{ gameHistory[i * 2] }}</span>
                <span :class="{ 'text-yellow-600': viewIndex === i * 2 + 2 }">{{ gameHistory[i * 2 + 1] ?? '' }}</span>
            </div>
        </div>
    </div>
</template>
