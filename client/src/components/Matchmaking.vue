<script setup lang="ts">
import { ref, computed } from 'vue';
import { useMatchmakingStore } from '@/stores/matchmaking';
import { useAuthStore } from '@/stores/auth';

const matchmakingStore = useMatchmakingStore();
const authStore = useAuthStore();

const isGuest = computed(() => authStore.guestId?.startsWith('guest-'));
const playRated = ref(false);

const timeControls = [
    { label: '1 min', value: 60 },
    { label: '3 min', value: 180 },
    { label: '5 min', value: 300 },
    { label: '10 min', value: 600 },
];

function quickPlay(seconds: number) {
    if (isGuest.value || !playRated.value) {
        matchmakingStore.joinCasualQueue(seconds);
    } else {
        matchmakingStore.joinRatedQueue(seconds);
    }
}
</script>

<template>
    <div class="border p-4">
        <div v-if="matchmakingStore.isInQueue" class="text-center py-4">
            <div>Searching for opponent...</div>
            <button 
                @click="matchmakingStore.resetMatch"
                class="mt-2 text-sm text-red-500 underline"
            >
                Cancel Search
            </button>
        </div>
        
        <div v-else class="grid grid-cols-2 gap-2">
            <button 
                v-for="tc in timeControls" 
                :key="tc.value"
                @click="quickPlay(tc.value)"
                class="border p-4 hover:bg-gray-100 cursor-pointer"
                :disabled="!matchmakingStore.isConnected"
            >
                {{ tc.label }}
            </button>
        </div>
    </div>
</template>
