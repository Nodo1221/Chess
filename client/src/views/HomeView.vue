<script setup lang="ts">
import { onMounted } from 'vue';
import { useRouter } from 'vue-router';
import { useAuthStore } from '@/stores/auth';
import { useMatchmakingStore } from '@/stores/matchmaking';
import Matchmaking from '@/components/Matchmaking.vue';

const authStore = useAuthStore();
const matchmakingStore = useMatchmakingStore();
const router = useRouter();

onMounted(async () => {
    if (matchmakingStore.isInGame) {
        const gameId = matchmakingStore.matchFound.gameId;
        const resp = await fetch(`/api/game/${gameId}`);
        if (resp.ok) {
            const game = await resp.json();
            if (game.status === 'active') {
                router.replace(`/game/${gameId}`);
                return;
            }
        }
        // Game no longer exists or is finished — stale localStorage
        matchmakingStore.resetMatch();
    }
});
</script>

<template>
    <div class="p-4 max-w-xl mx-auto">
        <h2 class="text-2xl font-bold mb-4">Quick pairing</h2>
        
        <div v-if="!authStore.isAuthenticated" class="border p-4 mb-4">
            <p class="mb-2">Not logged in.</p>
            <button @click="authStore.loginAsGuest" class="border px-4 py-1 hover:bg-gray-100">
                Play as Guest
            </button>
        </div>

        <div v-if="authStore.isAuthenticated">
            <Matchmaking />
        </div>
    </div>
</template>
