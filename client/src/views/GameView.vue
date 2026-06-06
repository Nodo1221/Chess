<script setup lang="ts">
import { onMounted } from 'vue';
import { useMatchmakingStore } from '@/stores/matchmaking';
import Board from '@/components/Board.vue';
import { useRouter, useRoute } from 'vue-router';

const matchmakingStore = useMatchmakingStore();
const router = useRouter();
const route = useRoute();

onMounted(() => {
    // If there's no match found in the store, redirect home.
    // To support spectators later, this logic will need to ask the server for the game state first.
    if (!matchmakingStore.isInGame || matchmakingStore.matchFound?.gameId !== route.params.id) {
        router.push('/');
    }
});
</script>

<template>
    <div class="p-4 max-w-5xl mx-auto flex flex-col gap-4">
        <div class="flex justify-between items-center border-b pb-2">
            <button @click="matchmakingStore.resetMatch(); router.push('/')" class="text-red-500 hover:underline text-sm border px-2 py-1">
                Resign / Leave Game
            </button>
            
            <div class="flex gap-4 items-center">
                <div class="text-sm text-gray-500 flex gap-2 items-center">
                    <span>Share URL to spectate:</span>
                    <input type="text" readonly :value="`http://localhost:5173/game/${route.params.id}`" class="border px-2 py-1 text-xs w-64 bg-gray-50" @click="$event.target?.select()">
                </div>
            </div>
        </div>

        <div class="flex gap-8 justify-center mt-4">
            <div class="border p-2 shadow-sm bg-gray-50">
                <Board />
            </div>
            
            <div v-if="matchmakingStore.matchFound" class="w-64 flex flex-col gap-4">
                <div class="border p-4 bg-gray-50">
                    <h3 class="font-bold border-b pb-2 mb-4 text-center">Opponent</h3>
                    <div class="flex flex-col gap-2 items-center">
                        <span class="font-mono font-bold text-lg truncate">
                            {{ matchmakingStore.matchFound.whitePlayer.id === $authStore?.guestId ? matchmakingStore.matchFound.blackPlayer.nickname : matchmakingStore.matchFound.whitePlayer.nickname }}
                        </span>
                    </div>
                </div>
                
                <div class="border p-4 bg-gray-50 text-center flex flex-col justify-center items-center h-24">
                    <div class="text-xs text-gray-500 mb-1">Time Control</div>
                    <div class="font-bold text-2xl">{{ matchmakingStore.matchFound.timeControlSeconds / 60 }} min</div>
                </div>
            </div>
        </div>
    </div>
</template>
