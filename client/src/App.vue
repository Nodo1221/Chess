<script setup lang="ts">
import { Input } from '@/components/ui/input';
import Board from './components/Board.vue';
</script>

<template>
    <h1 class="mb-4 mt-4 text-4xl font-bold tracking-tight text-center text-heading">
        🐴 Chess 🐴
    </h1>
    <p class="text-lg text-center">powered by Java</p>

    <Board class="mx-auto mt-10" />
</template>

<style scoped></style>
