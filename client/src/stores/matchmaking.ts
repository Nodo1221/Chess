import { defineStore } from 'pinia';
import { ref, computed } from 'vue';
import { Client } from '@stomp/stompjs';
import { useAuthStore } from './auth';

export const useMatchmakingStore = defineStore('matchmaking', () => {
    const authStore = useAuthStore();
    const stompClient = ref<Client | null>(null);
    const isConnected = ref(false);
    const isInQueue = ref(false);
    const matchFound = ref<any>(null);
    const isInGame = computed(() => !!matchFound.value);
    
    // Callback for moves - will be set by the Board component
    let onMoveReceived: ((move: any) => void) | null = null;

    function connect() {
        if (stompClient.value?.connected) return;

        const client = new Client({
            brokerURL: 'ws://localhost:8080/ws',
            connectHeaders: {
                Authorization: `Bearer ${authStore.token}`,
            },
            onConnect: () => {
                isConnected.value = true;
                
                client.subscribe('/topic/casual-matches', (message) => {
                    handleMatchFound(JSON.parse(message.body));
                });

                client.subscribe('/topic/rated-matches', (message) => {
                    handleMatchFound(JSON.parse(message.body));
                });
            },
            onDisconnect: () => {
                isConnected.value = false;
                isInQueue.value = false;
            },
        });

        client.activate();
        stompClient.value = client;
    }

    function handleMatchFound(match: any) {
        if (match.whitePlayer.id === authStore.guestId || match.blackPlayer.id === authStore.guestId) {
            matchFound.value = match;
            isInQueue.value = false;

            // Subscribe to the specific game topic for moves
            stompClient.value?.subscribe(`/topic/game/${match.gameId}`, (message) => {
                if (onMoveReceived) onMoveReceived(JSON.parse(message.body));
            });
        }
    }

    function sendMove(gameId: string, move: any) {
        if (!stompClient.value?.connected) return;
        stompClient.value.publish({
            destination: `/app/game/move/${gameId}`,
            body: JSON.stringify(move)
        });
    }

    function setMoveHandler(handler: (move: any) => void) {
        onMoveReceived = handler;
    }

    function resetMatch() {
        matchFound.value = null;
        isInQueue.value = false;
    }

    function joinCasualQueue() {
        if (!stompClient.value?.connected || isInGame.value) return;
        stompClient.value.publish({ destination: '/app/queue.join.casual' });
        isInQueue.value = true;
    }

    function joinRatedQueue() {
        if (!stompClient.value?.connected || isInGame.value) return;
        stompClient.value.publish({ destination: '/app/queue.join.rated' });
        isInQueue.value = true;
    }

    return {
        isConnected,
        isInQueue,
        matchFound,
        isInGame,
        connect,
        joinCasualQueue,
        joinRatedQueue,
        sendMove,
        setMoveHandler,
        resetMatch
    };
});
