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

// Connect automatically when logging in (fixes Electron fresh start)
watch(() => authStore.isAuthenticated, (isAuth) => {
    if (isAuth) {
        matchmakingStore.connect();
    }
});
</script>

<template>
    <div class="min-h-screen font-sans bg-white text-black">
        <header class="p-4 flex justify-between items-center">
            <h1 class="text-xl font-bold cursor-pointer" @click="router.push('/')">🐴 Chess</h1>
            
            <div class="flex gap-6 items-center">
                <div v-if="authStore.isAuthenticated" class="flex gap-4 items-center">
                    <span class="text-sm font-bold">{{ authStore.nickname }}</span>
                    <button v-if="!authStore.guestId?.startsWith('guest-')" @click="authStore.logout(); router.push('/')" class="text-xs text-red-500 hover:underline">Logout</button>
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
