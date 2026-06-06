<script setup lang="ts">
import { onMounted } from 'vue';
import { useAuthStore } from './stores/auth';
import { useMatchmakingStore } from './stores/matchmaking';
import Board from './components/Board.vue';
import Matchmaking from './components/Matchmaking.vue';

const authStore = useAuthStore();
const matchmakingStore = useMatchmakingStore();

onMounted(async () => {
    if (!authStore.isAuthenticated) {
        await authStore.loginAsGuest();
    }
    matchmakingStore.connect();
});
</script>

<template>
    <div class="min-h-screen bg-slate-900 text-slate-100 p-8">
        <h1 class="mb-4 text-4xl font-bold tracking-tight text-center text-cyan-400">
            🐴 Chess 🐴
        </h1>
        <p class="text-lg text-center mb-8">powered by Java</p>

        <div class="max-w-6xl mx-auto flex flex-col gap-8">
            <div class="flex justify-between items-center bg-slate-800 p-4 rounded-lg shadow-lg">
                <div v-if="authStore.isAuthenticated" class="flex items-center gap-4">
                    <div class="w-10 h-10 bg-cyan-600 rounded-full flex items-center justify-center font-bold">
                        {{ authStore.nickname?.[0] }}
                    </div>
                    <div>
                        <div class="font-bold">{{ authStore.nickname }}</div>
                        <div class="text-xs text-slate-400">{{ authStore.guestId }}</div>
                    </div>
                </div>
                
                <div class="flex items-center gap-2">
                    <div :class="['w-3 h-3 rounded-full', matchmakingStore.isConnected ? 'bg-green-500' : 'bg-red-500']"></div>
                    <span class="text-sm">{{ matchmakingStore.isConnected ? 'Connected' : 'Disconnected' }}</span>
                </div>
            </div>

            <div class="grid grid-cols-1 lg:grid-cols-[1fr_300px] gap-8">
                <div class="flex flex-col items-center">
                    <Board />
                </div>
                
                <div class="flex flex-col gap-4">
                    <Matchmaking />
                    
                    <div v-if="matchmakingStore.matchFound" class="bg-green-900/50 border border-green-500 p-4 rounded-lg animate-bounce">
                        <div class="font-bold text-center mb-2 text-green-400">Match Found!</div>
                        <div class="text-sm text-center">
                            {{ matchmakingStore.matchFound.player1.nickname }} vs {{ matchmakingStore.matchFound.player2.nickname }}
                        </div>
                        <div class="text-xs text-center mt-2 opacity-70">
                            Game ID: {{ matchmakingStore.matchFound.gameId }}
                        </div>
                    </div>
                </div>
            </div>
        </div>
    </div>
</template>

<style>
@reference "./globals.css";

body {
    margin: 0;
    @apply bg-slate-900;
}
</style>
