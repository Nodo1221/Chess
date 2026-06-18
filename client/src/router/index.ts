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

router.beforeEach(async (to) => {
    const matchmakingStore = useMatchmakingStore();

    if (matchmakingStore.isInGame) {
        const gameId = matchmakingStore.matchFound.gameId;

        // Already heading to the right game — let it through
        if (to.name === 'game' && to.params.id === gameId) {
            return true;
        }

        // Verify the game still exists before forcing a redirect
        try {
            const resp = await fetch(`/api/game/${gameId}`);
            if (resp.ok) {
                const game = await resp.json();
                if (game.status === 'active') {
                    return { name: 'game', params: { id: gameId } };
                }
            }
        } catch { /* network error — fall through */ }

        // Game gone or finished — clear stale state and allow navigation
        matchmakingStore.resetMatch();
    }

    return true;
});

export default router;
