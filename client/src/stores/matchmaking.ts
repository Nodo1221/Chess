import { defineStore } from 'pinia';
import { ref, computed } from 'vue';
import { Client } from '@stomp/stompjs';
import { useAuthStore } from './auth';
import router from '@/router';

export const useMatchmakingStore = defineStore('matchmaking', () => {
    const authStore = useAuthStore();
    const stompClient = ref<Client | null>(null);
    const isConnected = ref(false);
    const isInQueue = ref(false);
    const matchFound = ref<any>(null);
    const isInGame = computed(() => !!matchFound.value);
    
    // Reactive refs for game state
    const lastMoveReceived = ref<any>(null);
    const whiteTimeLeftMs = ref<number>(0);
    const blackTimeLeftMs = ref<number>(0);
    const currentTurn = ref<string>('w');
    const gameOver = ref<{winner: string, reason: string} | null>(null);

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

                client.subscribe('/user/queue/matches', (message) => {
                    const match = JSON.parse(message.body);
                    console.log('DEBUG: Match Found Event Received', match);
                    handleMatchFound(match, client);
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
        matchFound.value = match;
        isInQueue.value = false;
        gameOver.value = null;

        whiteTimeLeftMs.value = match.timeControlSeconds * 1000;
        blackTimeLeftMs.value = match.timeControlSeconds * 1000;
        currentTurn.value = 'w';

        console.log(`DEBUG: Subscribing to game topic: /topic/game/${match.gameId}`);
        activeClient.subscribe(`/topic/game/${match.gameId}`, (message) => {
            const data = JSON.parse(message.body);
            console.log('DEBUG: Message Received from Topic', data);

            if (data.winner) {
                gameOver.value = data;
            } else if (data.move) {
                whiteTimeLeftMs.value = data.whiteTimeLeftMs;
                blackTimeLeftMs.value = data.blackTimeLeftMs;
                currentTurn.value = data.currentTurn;
                lastMoveReceived.value = data;
            }
        });

        router.push(`/game/${match.gameId}`);
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
        gameOver.value = null;
    }

    function leaveQueue() {
        if (stompClient.value?.connected) {
            stompClient.value.publish({ destination: '/app/queue.leave', body: '' });
        }
        isInQueue.value = false;
    }

    function joinCasualQueue(timeControlSeconds: number = 180) {
        if (!stompClient.value?.connected || isInGame.value) return;
        stompClient.value.publish({
            destination: '/app/queue.join.casual',
            body: JSON.stringify({ timeControlSeconds })
        });
        isInQueue.value = true;
    }

    function joinRatedQueue(timeControlSeconds: number = 180) {
        if (!stompClient.value?.connected || isInGame.value) return;
        stompClient.value.publish({
            destination: '/app/queue.join.rated',
            body: JSON.stringify({ timeControlSeconds })
        });
        isInQueue.value = true;
    }

    return {
        isConnected,
        isInQueue,
        matchFound,
        isInGame,
        lastMoveReceived,
        whiteTimeLeftMs,
        blackTimeLeftMs,
        currentTurn,
        gameOver,
        connect,
        leaveQueue,
        joinCasualQueue,
        joinRatedQueue,
        sendMove,
        resetMatch
    };
});
