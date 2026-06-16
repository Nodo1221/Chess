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

function sendChat() {
    if (!chatInput.value.trim()) return;
    matchmakingStore.sendChatMessage(gameId, chatInput.value);
    chatInput.value = '';
}

// Derive display data from either player match or spectator game
const activeGame = computed(() => matchmakingStore.matchFound ?? matchmakingStore.spectatorGame);
</script>

<template>
    <div v-if="notFound" class="p-8 text-center text-gray-500">
        Game not found.
        <button @click="router.push('/')" class="ml-2 underline">Go home</button>
    </div>

    <div v-else-if="activeGame" class="p-4 flex flex-col gap-4 max-w-6xl mx-auto">
        <div v-if="matchmakingStore.isSpectating" class="text-xs text-gray-400 uppercase tracking-wide">
            Spectating
        </div>

        <div v-if="matchmakingStore.gameOver || activeGame?.isGameOver" class="text-sm">
            <span class="font-bold">Game Over!</span>
            {{
                (matchmakingStore.gameOver?.winner ?? activeGame?.winner ?? '?').toUpperCase()
            }} won by {{ matchmakingStore.gameOver?.reason ?? activeGame?.resultReason ?? '?' }}
        </div>

        <div class="flex gap-8 items-start">
            <!-- Board (includes nicknames and clocks now) -->
            <div class="flex-shrink-0">
                <Board :initial-moves="matchmakingStore.spectatorGame?.moves" />
            </div>

            <!-- Sidebar: chat -->
            <div class="flex flex-col flex-1 min-w-[300px] self-stretch border rounded bg-gray-50 overflow-hidden">
                <div class="bg-gray-100 px-3 py-2 text-xs font-bold uppercase tracking-wider border-b">
                    Game Chat
                </div>
                
                <!-- Chat Messages -->
                <div class="flex-1 overflow-y-auto p-3 space-y-2 text-sm">
                    <div
                        v-for="(msg, i) in matchmakingStore.chatMessages"
                        :key="i"
                        class="break-words"
                    >
                        <span class="font-bold text-blue-700">{{ msg.senderNickname }}:</span>
                        <span class="ml-1 text-gray-800">{{ msg.content }}</span>
                    </div>
                    <div v-if="matchmakingStore.chatMessages.length === 0" class="text-gray-400 italic text-center py-4">
                        No messages yet
                    </div>
                </div>

                <!-- Chat Input -->
                <div class="border-t bg-white flex">
                    <input
                        v-model="chatInput"
                        @keydown.enter="sendChat"
                        class="flex-1 px-3 py-1.5 text-xs outline-none"
                        placeholder="Type a message..."
                        maxlength="500"
                    />
                    <button 
                        @click="sendChat" 
                        class="px-4 text-xs font-semibold border-l hover:bg-gray-100 transition-colors"
                    >
                        Send
                    </button>
                </div>

                <div class="p-2 border-t bg-white" v-if="matchmakingStore.isInGame">
                    <button
                        @click="matchmakingStore.resetMatch(); router.push('/')"
                        class="w-full py-1 text-red-500 hover:bg-red-50 rounded text-xs transition-colors"
                    >
                        Resign / Leave Game
                    </button>
                </div>
            </div>
        </div>
    </div>

    <div v-else class="p-8 text-center text-gray-400">Loading game...</div>
</template>
