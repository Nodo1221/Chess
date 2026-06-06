<script setup lang="ts">
import { useMatchmakingStore } from '@/stores/matchmaking';
import { useAuthStore } from '@/stores/auth';

const matchmakingStore = useMatchmakingStore();
const authStore = useAuthStore();
</script>

<template>
    <div class="border p-4">
        <h3>Matchmaking</h3>
        <div class="flex flex-col gap-2 mt-2">
            <button 
                @click="matchmakingStore.joinCasualQueue"
                :disabled="!matchmakingStore.isConnected || matchmakingStore.isInQueue || matchmakingStore.isInGame"
                class="border px-4 py-2 disabled:opacity-50"
            >
                {{ matchmakingStore.isInQueue ? 'Searching...' : 'Play Casual' }}
            </button>
            <button 
                @click="matchmakingStore.joinRatedQueue"
                :disabled="!matchmakingStore.isConnected || matchmakingStore.isInQueue || matchmakingStore.isInGame || authStore.guestId?.startsWith('guest-')"
                class="border px-4 py-2 disabled:bg-gray-100 disabled:text-gray-400"
                title="Only signed in users can play ranked"
            >
                Play Rated
            </button>
            
            <button 
                v-if="matchmakingStore.isInGame"
                @click="matchmakingStore.resetMatch"
                class="mt-4 text-xs text-red-500 hover:underline"
            >
                Leave Game / Reset
            </button>
        </div>
    </div>
</template>
