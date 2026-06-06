<script setup lang="ts">
import { onMounted } from 'vue';
import { useAuthStore } from './stores/auth';
import { useMatchmakingStore } from './stores/matchmaking';
import Board from './components/Board.vue';
import Matchmaking from './components/Matchmaking.vue';

const authStore = useAuthStore();
const matchmakingStore = useMatchmakingStore();

onMounted(async () => {
    try {
        if (!authStore.isAuthenticated) {
            await authStore.loginAsGuest();
        }
        matchmakingStore.connect();
    } catch (e) {
        console.error('App initialization failed', e);
    }
});
</script>

<template>
    <div class="p-8 min-h-screen bg-white text-black">
        <h1 class="text-3xl font-bold mb-6">Chess Game</h1>
        
        <div class="flex flex-col gap-6">
            <div class="flex gap-6 items-center border-b pb-4">
                <div class="flex flex-col">
                    <span class="font-semibold">User: {{ authStore.nickname || 'Logging in...' }}</span>
                    <span class="text-sm text-gray-500">ID: {{ authStore.guestId }}</span>
                </div>
                <div class="flex items-center gap-2">
                    <div :class="['w-3 h-3 rounded-full', matchmakingStore.isConnected ? 'bg-green-500' : 'bg-red-500']"></div>
                    <span>{{ matchmakingStore.isConnected ? 'Connected' : 'Disconnected' }}</span>
                </div>
            </div>

            <div class="flex flex-wrap gap-12">
                <div class="border p-2 shadow-sm bg-gray-50">
                    <Board />
                </div>
                
                <div class="flex flex-col gap-4 w-80">
                    <Matchmaking />
                    
                    <div v-if="matchmakingStore.matchFound" class="p-4 border-2 border-green-500 bg-green-50 rounded">
                        <h4 class="font-bold text-green-700">Game Active</h4>
                        <p class="text-sm">ID: {{ matchmakingStore.matchFound.gameId }}</p>
                        <div class="mt-2 text-xs">
                            <div>White: {{ matchmakingStore.matchFound.whitePlayer.nickname }}</div>
                            <div>Black: {{ matchmakingStore.matchFound.blackPlayer.nickname }}</div>
                        </div>
                    </div>
                </div>
            </div>
        </div>
    </div>
</template>

<style>
body { 
    margin: 0; 
    font-family: sans-serif;
}
</style>
