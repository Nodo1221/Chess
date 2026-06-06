<script setup lang="ts">
import { ref, computed } from 'vue';
import { useMatchmakingStore } from '@/stores/matchmaking';
import { useAuthStore } from '@/stores/auth';

const matchmakingStore = useMatchmakingStore();
const authStore = useAuthStore();

const isGuest = computed(() => authStore.guestId?.startsWith('guest-'));
const playRated = ref(false); // Toggle for signed-in users

const timeControls = [
    { label: '1 min', value: 60, icon: '⚡' },
    { label: '3 min', value: 180, icon: '🔥' },
    { label: '5 min', value: 300, icon: '⏱️' },
    { label: '10 min', value: 600, icon: '🐢' },
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
    <div class="border p-6 bg-white shadow-sm max-w-md mx-auto">
        <div class="flex justify-between items-center mb-6">
            <h3 class="text-xl font-bold">Quick Pairing</h3>
            
            <div v-if="!isGuest" class="flex items-center gap-2 text-sm">
                <span :class="{'text-gray-400': playRated}">Casual</span>
                <button 
                    @click="playRated = !playRated"
                    class="w-10 h-5 rounded-full relative transition-colors duration-200"
                    :class="playRated ? 'bg-green-500' : 'bg-gray-300'"
                >
                    <div 
                        class="w-3 h-3 bg-white rounded-full absolute top-1 transition-transform duration-200"
                        :style="{ left: playRated ? 'calc(100% - 16px)' : '4px' }"
                    ></div>
                </button>
                <span :class="{'text-gray-400': !playRated}">Rated</span>
            </div>
            <div v-else class="text-xs text-gray-500 italic">
                Unrated only
            </div>
        </div>

        <div v-if="matchmakingStore.isInQueue" class="text-center py-8">
            <div class="animate-pulse mb-4 text-2xl">⏳</div>
            <div class="font-bold mb-4">Searching for opponent...</div>
            <button 
                @click="matchmakingStore.resetMatch"
                class="text-sm text-red-500 hover:underline border px-4 py-2"
            >
                Cancel Search
            </button>
        </div>
        
        <div v-else class="grid grid-cols-2 gap-4">
            <button 
                v-for="tc in timeControls" 
                :key="tc.value"
                @click="quickPlay(tc.value)"
                class="border p-6 flex flex-col items-center gap-2 hover:bg-gray-50 hover:border-blue-500 transition-all cursor-pointer"
                :disabled="!matchmakingStore.isConnected"
            >
                <span class="text-2xl">{{ tc.icon }}</span>
                <span class="font-bold">{{ tc.label }}</span>
            </button>
        </div>
    </div>
</template>
