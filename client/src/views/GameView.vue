<script setup lang="ts">
import { onMounted, onUnmounted, ref, watch, computed } from 'vue';
import { useMatchmakingStore } from '@/stores/matchmaking';
import { useAuthStore } from '@/stores/auth';
import Board from '@/components/Board.vue';
import { useRouter, useRoute } from 'vue-router';

const matchmakingStore = useMatchmakingStore();
const authStore = useAuthStore();
const router = useRouter();
const route = useRoute();
const gameId = route.params.id as string;

let timerInterval: number | null = null;
const notFound = ref(false);
const chatInput = ref('');

function startTimer() {
    if (timerInterval) return;
    timerInterval = window.setInterval(() => {
        if (matchmakingStore.gameOver) return;
        if (matchmakingStore.currentTurn === 'w') {
            matchmakingStore.whiteTimeLeftMs = Math.max(0, matchmakingStore.whiteTimeLeftMs - 100);
        } else {
            matchmakingStore.blackTimeLeftMs = Math.max(0, matchmakingStore.blackTimeLeftMs - 100);
        }
    }, 100);
}

onMounted(async () => {
    // Ensure the user has an identity (needed for WebSocket auth and chat)
    if (!authStore.isAuthenticated) {
        await authStore.loginAsGuest();
    }

    // Player already in this game — just start the timer and subscribe to chat
    if (matchmakingStore.isInGame && matchmakingStore.matchFound?.gameId === gameId) {
        startTimer();
        return;
    }

    // Connect if not already (spectator visiting directly)
    await matchmakingStore.connect();

    const ok = await matchmakingStore.joinAsSpectator(gameId);
    if (!ok) {
        notFound.value = true;
    }
});

onUnmounted(() => {
    if (timerInterval) clearInterval(timerInterval);
});

// Start the timer once the player-side match is confirmed (handles race on page load)
watch(() => matchmakingStore.isInGame, (inGame) => {
    if (inGame && matchmakingStore.matchFound?.gameId === gameId) startTimer();
});

function formatTime(ms: number) {
    const totalSeconds = Math.ceil(ms / 1000);
    const m = Math.floor(totalSeconds / 60);
    const s = totalSeconds % 60;
    return `${m}:${s.toString().padStart(2, '0')}`;
}

function sendChat() {
    if (!chatInput.value.trim()) return;
    matchmakingStore.sendChatMessage(gameId, chatInput.value);
    chatInput.value = '';
}

// Derive display data from either player match or spectator game
const activeGame = computed(() => matchmakingStore.matchFound ?? matchmakingStore.spectatorGame);
const whiteMs = computed(() => matchmakingStore.isInGame
    ? matchmakingStore.whiteTimeLeftMs
    : (matchmakingStore.spectatorGame?.whiteTimeLeftMs ?? 0));
const blackMs = computed(() => matchmakingStore.isInGame
    ? matchmakingStore.blackTimeLeftMs
    : (matchmakingStore.spectatorGame?.blackTimeLeftMs ?? 0));
</script>

<template>
    <div v-if="notFound" class="p-8 text-center text-gray-500">
        Game not found.
        <button @click="router.push('/')" class="ml-2 underline">Go home</button>
    </div>

    <div v-else-if="activeGame" class="p-4 flex flex-col gap-4 max-w-5xl mx-auto">
        <div v-if="matchmakingStore.isSpectating" class="text-xs text-gray-400 uppercase tracking-wide">
            Spectating
        </div>

        <div v-if="matchmakingStore.gameOver || activeGame?.isGameOver" class="text-sm">
            <span class="font-bold">Game Over!</span>
            {{
                (matchmakingStore.gameOver?.winner ?? activeGame?.winner ?? '?').toUpperCase()
            }} won by {{ matchmakingStore.gameOver?.reason ?? activeGame?.resultReason ?? '?' }}
        </div>

        <div class="flex gap-4">
            <!-- Board -->
            <div class="flex-shrink-0">
                <Board :initial-moves="matchmakingStore.spectatorGame?.moves" />
            </div>

            <!-- Sidebar: clocks + chat -->
            <div class="flex flex-col gap-4 flex-1 min-w-0">
                <!-- Clocks -->
                <div class="flex flex-col gap-2 text-sm">
                    <div class="flex justify-between">
                        <span class="font-bold truncate">{{ activeGame?.blackPlayer?.nickname ?? activeGame?.blackNickname }}</span>
                        <span class="font-mono text-lg">{{ formatTime(blackMs) }}</span>
                    </div>
                    <div class="flex justify-between">
                        <span class="font-bold truncate">{{ activeGame?.whitePlayer?.nickname ?? activeGame?.whiteNickname }}</span>
                        <span class="font-mono text-lg">{{ formatTime(whiteMs) }}</span>
                    </div>
                </div>

                <!-- Chat -->
                <div class="flex flex-col flex-1 border min-h-0">
                    <div class="flex-1 overflow-y-auto p-2 space-y-1 text-sm max-h-64">
                        <div
                            v-for="(msg, i) in matchmakingStore.chatMessages"
                            :key="i"
                            class="break-words"
                        >
                            <span class="font-semibold">{{ msg.senderNickname }}:</span>
                            {{ msg.content }}
                        </div>
                        <div v-if="matchmakingStore.chatMessages.length === 0" class="text-gray-400 italic">
                            No messages yet
                        </div>
                    </div>
                    <div class="border-t flex">
                        <input
                            v-model="chatInput"
                            @keydown.enter="sendChat"
                            class="flex-1 px-2 py-1 text-sm outline-none"
                            placeholder="Message..."
                            maxlength="500"
                        />
                        <button @click="sendChat" class="px-3 text-sm border-l hover:bg-gray-100">
                            Send
                        </button>
                    </div>
                </div>

                <div class="pt-2" v-if="matchmakingStore.isInGame">
                    <button
                        @click="matchmakingStore.resetMatch(); router.push('/')"
                        class="text-red-500 underline text-sm"
                    >
                        Resign / Leave Game
                    </button>
                </div>
            </div>
        </div>
    </div>

    <div v-else class="p-8 text-center text-gray-400">Loading game...</div>
</template>
