<script setup lang="ts">
import { ref, computed } from 'vue';
import { useMatchmakingStore } from '@/stores/matchmaking';
import { useAuthStore } from '@/stores/auth';

const matchmakingStore = useMatchmakingStore();
const authStore = useAuthStore();

const isGuest = computed(() => authStore.guestId?.startsWith('guest-'));
const selectedTime = ref(180);

const timeControls = [
    { label: '1 min', value: 60 },
    { label: '3 min', value: 180 },
    { label: '5 min', value: 300 },
    { label: '10 min', value: 600 },
];
</script>

<template>
    <div class="border p-4">
        <h3>Matchmaking</h3>
        
        <div v-if="!matchmakingStore.isInGame" class="flex flex-col gap-4 mt-4">
            <div>
                <div class="grid grid-cols-2 gap-2">
                    <button 
                        v-for="tc in timeControls" 
                        :key="tc.value"
                        @click="selectedTime = tc.value"
                        :class="['border p-2 text-sm', selectedTime === tc.value ? 'bg-blue-100 border-blue-500 font-bold' : 'bg-white']"
                        :disabled="matchmakingStore.isInQueue"
                    >
                        {{ tc.label }}
                    </button>
                </div>
            </div>

            <div class="flex flex-col gap-2">
                <button 
                    @click="matchmakingStore.joinCasualQueue(selectedTime)"
                    :disabled="!matchmakingStore.isConnected || matchmakingStore.isInQueue"
                    class="border px-4 py-2 disabled:opacity-50 font-bold bg-gray-50 hover:bg-gray-100"
                >
                    {{ matchmakingStore.isInQueue ? 'Searching...' : `Play Casual (${selectedTime / 60}m)` }}
                </button>
                
                <button 
                    v-if="!isGuest"
                    @click="matchmakingStore.joinRatedQueue(selectedTime)"
                    :disabled="!matchmakingStore.isConnected || matchmakingStore.isInQueue"
                    class="border px-4 py-2 disabled:opacity-50 font-bold bg-gray-50 hover:bg-gray-100"
                >
                    Play Rated ({{ selectedTime / 60 }}m)
                </button>
                
                <div v-else class="text-center p-2 border border-dashed text-gray-500 text-sm">
                    Sign in to play Rated
                </div>
            </div>
        </div>

        <div v-else class="mt-4">
            <button 
                @click="matchmakingStore.resetMatch"
                class="text-sm text-red-500 hover:underline border p-2 w-full text-center"
            >
                Resign / Leave Game
            </button>
        </div>
    </div>
</template>
