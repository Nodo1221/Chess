<script setup lang="ts">
import { onMounted, watch } from 'vue';
import { useMatchmakingStore } from '@/stores/matchmaking';
import Board from '@/components/Board.vue';
import { useRouter } from 'vue-router';

const matchmakingStore = useMatchmakingStore();
const router = useRouter();

onMounted(() => {
    if (!matchmakingStore.isInGame) {
        router.push('/');
    }
});

// If the match ends or is reset, go back home
watch(() => matchmakingStore.isInGame, (inGame) => {
    if (!inGame) {
        router.push('/');
    }
});
</script>

<template>
    <div class="p-4 max-w-5xl mx-auto flex flex-col gap-4">
        <div class="flex justify-between items-center border-b pb-2">
            <button @click="matchmakingStore.resetMatch" class="text-red-500 hover:underline text-sm border px-2 py-1">
                Resign / Leave Game
            </button>
            
            <div v-if="matchmakingStore.matchFound" class="text-sm">
                Game ID: <span class="font-mono text-gray-500">{{ matchmakingStore.matchFound.gameId }}</span>
            </div>
        </div>

        <div class="flex gap-8 justify-center">
            <div class="border p-2 shadow-sm bg-gray-50">
                <Board />
            </div>
            
            <div v-if="matchmakingStore.matchFound" class="w-64 flex flex-col gap-4">
                <div class="border p-4 bg-gray-50">
                    <h3 class="font-bold border-b pb-2 mb-2">Players</h3>
                    <div class="flex justify-between items-center mb-2">
                        <span class="w-4 h-4 bg-white border inline-block"></span>
                        <span class="font-mono text-sm truncate">{{ matchmakingStore.matchFound.whitePlayer.nickname }}</span>
                    </div>
                    <div class="flex justify-between items-center">
                        <span class="w-4 h-4 bg-black border inline-block"></span>
                        <span class="font-mono text-sm truncate">{{ matchmakingStore.matchFound.blackPlayer.nickname }}</span>
                    </div>
                </div>
                
                <div class="border p-4 bg-gray-50 text-center">
                    <div class="text-xs text-gray-500 mb-1">Time Control</div>
                    <div class="font-bold text-xl">{{ matchmakingStore.matchFound.timeControlSeconds / 60 }} min</div>
                </div>
            </div>
        </div>
    </div>
</template>
