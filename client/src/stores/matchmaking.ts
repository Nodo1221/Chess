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
    
    // Reactive ref for the last move received
    const lastMoveReceived = ref<any>(null);

    function connect() {
        if (stompClient.value?.connected) return;

        const client = new Client({
            brokerURL: 'ws://localhost:8080/ws',
            connectHeaders: {
                Authorization: `Bearer ${authStore.token}`,
            },
            onConnect: () => {
                isConnected.value = true;
                console.log('DEBUG: Connected to WebSocket');
                
                client.subscribe('/topic/casual-matches', (message) => {
                    const match = JSON.parse(message.body);
                    console.log('DEBUG: Match Found Event Received', match);
                    handleMatchFound(match, client); // Pass client directly to avoid null ref
                });

                client.subscribe('/topic/rated-matches', (message) => {
                    handleMatchFound(JSON.parse(message.body), client);
                });
            },
            onDisconnect: () => {
                isConnected.value = false;
                isInQueue.value = false;
                console.log('DEBUG: Disconnected from WebSocket');
            },
        });

        client.activate();
        stompClient.value = client;
    }

    function handleMatchFound(match: any, activeClient: Client) {
        if (match.whitePlayer.id === authStore.guestId || match.blackPlayer.id === authStore.guestId) {
            matchFound.value = match;
            isInQueue.value = false;

            console.log(`DEBUG: Subscribing to game topic: /topic/game/${match.gameId}`);
            activeClient.subscribe(`/topic/game/${match.gameId}`, (message) => {
                const moveData = JSON.parse(message.body);
                console.log('DEBUG: Move Received from Topic', moveData);
                lastMoveReceived.value = moveData;
            });
        }
    }

    function sendMove(gameId: string, move: any) {
        if (!stompClient.value?.connected) {
            console.error('DEBUG: Cannot send move, STOMP not connected');
            return;
        }
        console.log(`DEBUG: Sending move to /app/game/move/${gameId}`, move);
        stompClient.value.publish({
            destination: `/app/game/move/${gameId}`,
            body: JSON.stringify(move)
        });
    }

    function resetMatch() {
        matchFound.value = null;
        isInQueue.value = false;
        lastMoveReceived.value = null;
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
        lastMoveReceived,
        connect,
        joinCasualQueue,
        joinRatedQueue,
        sendMove,
        resetMatch
    };
});
