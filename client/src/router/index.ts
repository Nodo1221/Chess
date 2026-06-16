import { createRouter, createWebHistory } from 'vue-router';
import HomeView from '@/views/HomeView.vue';
import GameView from '@/views/GameView.vue';
import { useMatchmakingStore } from '@/stores/matchmaking';

const router = createRouter({
    history: createWebHistory(import.meta.env.BASE_URL),
    routes: [
        {
            path: '/',
            name: 'home',
            component: HomeView,
        },
        {
            path: '/game/:id',
            name: 'game',
            component: GameView,
            props: true
        },
    ],
});

router.beforeEach((to, from) => {
    const matchmakingStore = useMatchmakingStore();
    console.log('DEBUG Router: Navigating to', to.path, 'InGame:', matchmakingStore.isInGame);
    
    // If user is in an active game (not over), force them to stay on the game page
    if (matchmakingStore.isInGame) {
        const gameId = matchmakingStore.matchFound.gameId;
        console.log('DEBUG Router: Sticky game active. Redirecting to', gameId);
        
        // Don't redirect if we're already going to that specific game page
        if (to.name === 'game' && to.params.id === gameId) {
            return true;
        }
        
        // Otherwise, force back to the game
        return { name: 'game', params: { id: gameId } };
    }
    
    return true;
});

export default router;
