<script setup lang="ts">
import { useAuthStore } from '@/stores/auth';
import Matchmaking from '@/components/Matchmaking.vue';

const authStore = useAuthStore();
</script>

<template>
    <div class="p-8 max-w-2xl mx-auto">
        <h2 class="text-2xl font-bold mb-6 text-center">Welcome to Chess</h2>
        
        <div v-if="!authStore.isAuthenticated" class="border p-6 mb-8 text-center bg-gray-50">
            <h3 class="font-bold mb-2">You are not logged in</h3>
            <p class="text-sm text-gray-600 mb-4">Play as a guest or sign in for rated games.</p>
            <button @click="authStore.loginAsGuest" class="border px-4 py-2 bg-white hover:bg-gray-100 font-bold">
                Play as Guest
            </button>
        </div>

        <div v-if="authStore.isAuthenticated">
            <Matchmaking />
        </div>
    </div>
</template>
