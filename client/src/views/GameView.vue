<script setup lang="ts">
import { onMounted, onUnmounted, watch } from 'vue';
import { useMatchmakingStore } from '@/stores/matchmaking';
import { useAuthStore } from '@/stores/auth';
import Board from '@/components/Board.vue';
import { useRouter, useRoute } from 'vue-router';

const matchmakingStore = useMatchmakingStore();
const authStore = useAuthStore();
const router = useRouter();
const route = useRoute();

let timerInterval: number | null = null;

onMounted(() => {
    if (!matchmakingStore.isInGame || matchmakingStore.matchFound?.gameId !== route.params.id) {
        router.push('/');
        return;
    }

    // Start local timer loop
    timerInterval = window.setInterval(() => {
        if (matchmakingStore.gameOver) return;
        
        if (matchmakingStore.currentTurn === 'w') {
            matchmakingStore.whiteTimeLeftMs = Math.max(0, matchmakingStore.whiteTimeLeftMs - 100);
        } else {
            matchmakingStore.blackTimeLeftMs = Math.max(0, matchmakingStore.blackTimeLeftMs - 100);
        }
    }, 100);
});

onUnmounted(() => {
    if (timerInterval) clearInterval(timerInterval);
});

function formatTime(ms: number) {
    const totalSeconds = Math.ceil(ms / 1000);
    const m = Math.floor(totalSeconds / 60);
    const s = totalSeconds % 60;
    return `${m}:${s.toString().padStart(2, '0')}`;
}
</script>

<template>
    <div class="p-4 flex flex-col gap-4 max-w-4xl mx-auto">
        <div class="flex justify-between items-center border-b pb-2">
            <button @click="matchmakingStore.resetMatch(); router.push('/')" class="text-red-500 underline text-sm">
                Resign / Leave Game
            </button>
            
            <div class="text-sm">
                Share URL: <input type="text" readonly :value="`http://localhost:5173/game/${route.params.id}`" class="border px-1 w-64" @click="$event.target?.select()">
            </div>
        </div>

        <div v-if="matchmakingStore.gameOver" class="bg-yellow-100 border-yellow-400 border p-4 text-center">
            <h2 class="font-bold text-xl">Game Over!</h2>
            <p>{{ matchmakingStore.gameOver.winner.toUpperCase() }} won by {{ matchmakingStore.gameOver.reason }}</p>
        </div>

        <div class="flex gap-4">
            <div class="border p-2">
                <Board />
            </div>
            
            <div v-if="matchmakingStore.matchFound" class="w-48 flex flex-col gap-4">
                
                <!-- Opponent Info -->
                <div class="border p-2 bg-gray-50 flex justify-between items-center">
                    <div class="truncate font-bold">
                        {{ matchmakingStore.matchFound.whitePlayer.id === authStore.guestId ? matchmakingStore.matchFound.blackPlayer.nickname : matchmakingStore.matchFound.whitePlayer.nickname }}
                    </div>
                    <div class="font-mono text-xl">
                        {{ formatTime(matchmakingStore.matchFound.whitePlayer.id === authStore.guestId ? matchmakingStore.blackTimeLeftMs : matchmakingStore.whiteTimeLeftMs) }}
                    </div>
                </div>
                
                <div class="flex-grow"></div>

                <!-- You Info -->
                <div class="border p-2 bg-blue-50 flex justify-between items-center">
                    <div class="truncate font-bold">
                        {{ authStore.nickname }}
                    </div>
                    <div class="font-mono text-xl">
                        {{ formatTime(matchmakingStore.matchFound.whitePlayer.id === authStore.guestId ? matchmakingStore.whiteTimeLeftMs : matchmakingStore.blackTimeLeftMs) }}
                    </div>
                </div>

            </div>
        </div>
    </div>
</template>
