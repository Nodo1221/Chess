<script setup lang="ts">
import { ref, onMounted, onUnmounted, computed, watch } from 'vue';
import { Chess } from 'chess.js';
import { useMatchmakingStore } from '@/stores/matchmaking';
import { useAuthStore } from '@/stores/auth';

const matchmakingStore = useMatchmakingStore();
const authStore = useAuthStore();

const props = defineProps<{ size?: number; initialMoves?: any[] }>();
const boardSize = ref(props.size ?? 704);

function colour(square: number) {
    const row = Math.floor((square - 1) / 8);
    const col = (square - 1) % 8;
    return (row + col) % 2 === 0 ? 'bg-cyan-300' : 'bg-cyan-700';
}

// ── pieces ────────────────────────────────────────────────────────────────────

const chess = new Chess();
const board = ref(chess.board());
const gameHistory = ref<string[]>([]);
const viewIndex = ref(0);
const isFlipped = ref(false);

const playerSide = computed(() => {
    if (!matchmakingStore.matchFound) return null;
    // CRITICAL: Check both, but if IDs are identical, warn user
    const isWhite = matchmakingStore.matchFound.whitePlayer.id === authStore.guestId;
    const isBlack = matchmakingStore.matchFound.blackPlayer.id === authStore.guestId;
    
    if (isWhite && isBlack) return 'w'; // Default to white if self-playing
    if (isWhite) return 'w';
    if (isBlack) return 'b';
    return null;
});

const isSelfPlay = computed(() => {
    return matchmakingStore.matchFound?.whitePlayer.id === matchmakingStore.matchFound?.blackPlayer.id;
});

// Auto-flip when match starts
watch(playerSide, (side) => {
    if (side === 'b') isFlipped.value = true;
    else isFlipped.value = false;
}, { immediate: true });

// Sync moves from other players
watch(() => matchmakingStore.lastMoveReceived, (moveData) => {
    if (!moveData) return;
    
    // We only apply if:
    // 1. It's from a different player ID
    // 2. OR if it's a self-play match (for debugging), we apply it to the "other" side of the board
    const isFromOpponent = moveData.playerId !== authStore.guestId;
    
    if (isFromOpponent) {
        console.log('DEBUG: Opponent move received:', moveData.move);
        try {
            const result = chess.move(moveData.move);
            if (result) {
                gameHistory.value = [...chess.history()];
                viewIndex.value = gameHistory.value.length;
                syncBoard();
            }
        } catch (e) {
            console.error('DEBUG: Failed to apply move', e);
        }
    }
});

function syncBoard() {
    const c = new Chess();
    for (const m of gameHistory.value.slice(0, viewIndex.value)) {
        c.move(m);
    }
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

function formatTime(ms: number) {
    const totalSeconds = Math.ceil(ms / 1000);
    const m = Math.floor(totalSeconds / 60);
    const s = totalSeconds % 60;
    return `${m}:${s.toString().padStart(2, '0')}`;
}

const whiteMs = computed(() => matchmakingStore.isInGame
    ? matchmakingStore.whiteTimeLeftMs
    : (matchmakingStore.spectatorGame?.whiteTimeLeftMs ?? 0));
const blackMs = computed(() => matchmakingStore.isInGame
    ? matchmakingStore.blackTimeLeftMs
    : (matchmakingStore.spectatorGame?.blackTimeLeftMs ?? 0));

const topPlayer = computed(() => {
    const game = matchmakingStore.matchFound ?? matchmakingStore.spectatorGame;
    if (!game) return null;
    if (isFlipped.value) {
        return {
            nickname: game.whitePlayer?.nickname ?? game.whiteNickname ?? 'White',
            ms: whiteMs.value
        };
    }
    return {
        nickname: game.blackPlayer?.nickname ?? game.blackNickname ?? 'Black',
        ms: blackMs.value
    };
});

const bottomPlayer = computed(() => {
    const game = matchmakingStore.matchFound ?? matchmakingStore.spectatorGame;
    if (!game) return null;
    if (isFlipped.value) {
        return {
            nickname: game.blackPlayer?.nickname ?? game.blackNickname ?? 'Black',
            ms: blackMs.value
        };
    }
    return {
        nickname: game.whitePlayer?.nickname ?? game.whiteNickname ?? 'White',
        ms: whiteMs.value
    };
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
    
    let col = Math.floor((relX / rect.width) * 8);
    let row = Math.floor((relY / rect.height) * 8);
    
    if (isFlipped.value) {
        col = 7 - col;
        row = 7 - row;
    }
    
    return row * 8 + col + 1;
}

const boardEl = ref<HTMLElement | null>(null);

function onPiecePointerDown(e: PointerEvent, square: number) {
    if (viewIndex.value !== gameHistory.value.length) return;
    
    // Only allow movement if it's an active match and we are a player
    if (!matchmakingStore.matchFound || !playerSide.value) return;

    // Stop move if not our turn
    if (chess.turn() !== playerSide.value) return;
    
    const row = Math.floor((square - 1) / 8);
    const col = (square - 1) % 8;
    const p = board.value[row]?.[col];

    // Stop move if not our piece
    if (!p || p.color !== playerSide.value) return;

    const asset = pieceAsset(square);
    if (!asset) return;
    e.preventDefault();
    e.stopPropagation();
    dragging.value = { fromSquare: square, asset, x: e.clientX, y: e.clientY };
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
                gameHistory.value = [...chess.history()];
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
            // illegal
        }
    }
    dragging.value = null;
    window.removeEventListener('pointermove', onDragMove);
    window.removeEventListener('pointerup', onDragUp);
}

function onKeyDown(e: KeyboardEvent) {
    if (e.target instanceof HTMLInputElement || e.target instanceof HTMLTextAreaElement) return;
    if (e.key === 'ArrowLeft')  { viewIndex.value = Math.max(0, viewIndex.value - 1); syncBoard(); }
    if (e.key === 'ArrowRight') { viewIndex.value = Math.min(gameHistory.value.length, viewIndex.value + 1); syncBoard(); }
    if (e.key.toLowerCase() === 'f') { isFlipped.value = !isFlipped.value; }
}

onMounted(() => {
    window.addEventListener('keydown', onKeyDown);
    if (props.initialMoves?.length) {
        for (const m of props.initialMoves) {
            try { chess.move(m); } catch { break; }
        }
        gameHistory.value = [...chess.history()];
        viewIndex.value = gameHistory.value.length;
        syncBoard();
    }
});
onUnmounted(() => window.removeEventListener('keydown', onKeyDown));
</script>

<template>
    <div class="flex gap-4 items-start">
        <!-- Move History (Left) -->
        <div class="text-sm w-32 max-h-[704px] overflow-y-auto shrink-0 pr-4 self-stretch pt-10">
            <div v-for="(_, i) in Math.ceil(gameHistory.length / 2)" :key="i" class="flex gap-2">
                <span class="text-gray-500 w-5">{{ i + 1 }}.</span>
                <span :class="{ 'bg-yellow-200 px-1': viewIndex === i * 2 + 1 }">{{ gameHistory[i * 2] }}</span>
                <span :class="{ 'bg-yellow-200 px-1': viewIndex === i * 2 + 2 }">{{ gameHistory[i * 2 + 1] ?? '' }}</span>
            </div>
        </div>

        <!-- Board & Players (Center) -->
        <div class="flex flex-col gap-2">
            <!-- Top Player -->
            <div v-if="topPlayer" class="flex justify-between items-center text-sm font-bold h-8">
                <span class="truncate max-w-[200px]">{{ topPlayer.nickname }} (1000)</span>
                <span class="text-lg px-2">{{ formatTime(topPlayer.ms) }}</span>
            </div>

            <div class="relative select-none shrink-0" ref="boardEl" :style="{ width: `${boardSize}px` }">
                <div class="grid grid-cols-8 border-2 border-gray-800" style="aspect-ratio: 1 / 1">
                    <div
                        v-for="i in 64"
                        :key="i"
                        class="relative aspect-square flex items-center justify-center"
                        :class="colour(isFlipped ? 65 - i : i)"
                    >
                        <img
                            v-if="pieceAsset(isFlipped ? 65 - i : i)"
                            :src="pieceAsset(isFlipped ? 65 - i : i)!"
                            class="w-full h-full"
                            :class="{ 
                                'opacity-0': dragging?.fromSquare === (isFlipped ? 65 - i : i),
                                'cursor-grab': matchmakingStore.matchFound && playerSide
                            }"
                            draggable="false"
                            @pointerdown="(e) => onPiecePointerDown(e, isFlipped ? 65 - i : i)"
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
            </div>

            <!-- Bottom Player -->
            <div v-if="bottomPlayer" class="flex justify-between items-center text-sm font-bold h-8">
                <div class="flex items-center gap-2">
                    <span class="truncate max-w-[200px]">{{ bottomPlayer.nickname }} (1000)</span>
                    <span v-if="matchmakingStore.matchFound" class="text-[10px] text-gray-500 font-normal uppercase">
                        ({{ playerSide === 'w' ? 'White' : 'Black' }})
                    </span>
                </div>
                <span class="text-lg px-2">{{ formatTime(bottomPlayer.ms) }}</span>
            </div>
        </div>
    </div>
</template>
