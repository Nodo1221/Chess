<script setup lang="ts">
import { RouterView, useRouter } from 'vue-router';
import { useAuthStore } from './stores/auth';
import { useMatchmakingStore } from './stores/matchmaking';
import { onMounted, watch } from 'vue';

const authStore = useAuthStore();
const matchmakingStore = useMatchmakingStore();
const router = useRouter();

onMounted(async () => {
    if (authStore.isAuthenticated) {
        matchmakingStore.connect();
    }
});
</script>

<template>
    <div class="min-h-screen font-sans bg-white text-black">
        <header class="p-4 border-b flex justify-between items-center bg-gray-50">
            <h1 class="text-xl font-bold cursor-pointer" @click="router.push('/')">🐴 Chess App</h1>
            
            <div class="flex gap-6 items-center">
                <div class="flex items-center gap-2 text-sm">
                    <div :class="['w-2 h-2 rounded-full', matchmakingStore.isConnected ? 'bg-green-500' : 'bg-red-500']"></div>
                    <span class="text-gray-500">{{ matchmakingStore.isConnected ? 'Connected' : 'Offline' }}</span>
                </div>
                
                <div v-if="authStore.isAuthenticated" class="flex gap-4 items-center">
                    <span class="text-sm font-mono border px-2 py-1 bg-white">{{ authStore.nickname }}</span>
                    <button @click="authStore.logout(); router.push('/')" class="text-xs text-red-500 hover:underline">Logout</button>
                </div>
            </div>
        </header>

        <main>
            <RouterView />
        </main>
    </div>
</template>

<style>
body { 
    margin: 0; 
}
</style>
