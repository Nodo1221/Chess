import { defineStore } from 'pinia';
import { ref, computed, watch } from 'vue';
import { Client } from '@stomp/stompjs';
import { useAuthStore } from './auth';
import router from '@/router';

export const useMatchmakingStore = defineStore('matchmaking', () => {
    const authStore = useAuthStore();
    const stompClient = ref<Client | null>(null);
    const isConnected = ref(false);
    let connectResolvers: (() => void)[] = [];
    const isInQueue = ref(false);
    const matchFound = ref<any>(JSON.parse(localStorage.getItem('chess_match') || 'null'));
    const isInGame = computed(() => !!matchFound.value);

    // Active game state (for players)
    const lastMoveReceived = ref<any>(null);
    const whiteTimeLeftMs = ref<number>(matchFound.value?.timeControlSeconds * 1000 || 0);
    const blackTimeLeftMs = ref<number>(matchFound.value?.timeControlSeconds * 1000 || 0);
    const currentTurn = ref<string>('w');
    const gameOver = ref<{ winner: string; reason: string } | null>(
        JSON.parse(localStorage.getItem('chess_game_over') || 'null')
    );

    // Watch states and persist to localStorage
    watch(matchFound, (val) => {
        if (val) localStorage.setItem('chess_match', JSON.stringify(val));
        else localStorage.removeItem('chess_match');
    }, { deep: true });

    watch(gameOver, (val) => {
        if (val) localStorage.setItem('chess_game_over', JSON.stringify(val));
        else localStorage.removeItem('chess_game_over');
    }, { deep: true });

    const spectatorGame = ref<any>(null);
    const isSpectating = computed(() => !!spectatorGame.value && !isInGame.value);

    // Chat (shared by players and spectators)
    const chatMessages = ref<{ senderId: string; senderNickname: string; content: string; sentAt: string }[]>([]);

    function connect(): Promise<void> {
        if (stompClient.value?.connected) return Promise.resolve();
        // Already connecting — queue up behind the in-flight connection
        if (stompClient.value) {
            return new Promise(resolve => connectResolvers.push(resolve));
        }

        return new Promise((resolve) => {
            connectResolvers.push(resolve);
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

                    connectResolvers.forEach(r => r());
                    connectResolvers = [];
                },
                onDisconnect: () => {
                    isConnected.value = false;
                    isInQueue.value = false;
                    console.log('DEBUG: Disconnected from WebSocket');
                },
            });

            client.activate();
            stompClient.value = client;
        });
    }

    function handleMatchFound(match: any, activeClient: Client) {
        matchFound.value = match;
        isInQueue.value = false;
        gameOver.value = null;
        spectatorGame.value = null;

        whiteTimeLeftMs.value = match.timeControlSeconds * 1000;
        blackTimeLeftMs.value = match.timeControlSeconds * 1000;
        currentTurn.value = 'w';

        subscribeToGameTopic(match.gameId, activeClient);
        subscribeToChatTopic(match.gameId, activeClient);

        router.push(`/game/${match.gameId}`);
    }

    function subscribeToGameTopic(gameId: string, activeClient: Client) {
        console.log(`DEBUG: Subscribing to game topic: /topic/game/${gameId}`);
        activeClient.subscribe(`/topic/game/${gameId}`, (message) => {
            const data = JSON.parse(message.body);
            console.log('DEBUG: Game message received', data);

            if (data.winner) {
                gameOver.value = data;
                if (spectatorGame.value) spectatorGame.value = { ...spectatorGame.value, isGameOver: true, winner: data.winner };
            } else if (data.move) {
                whiteTimeLeftMs.value = data.whiteTimeLeftMs;
                blackTimeLeftMs.value = data.blackTimeLeftMs;
                currentTurn.value = data.currentTurn;
                lastMoveReceived.value = data;
                if (spectatorGame.value) {
                    spectatorGame.value = {
                        ...spectatorGame.value,
                        whiteTimeLeftMs: data.whiteTimeLeftMs,
                        blackTimeLeftMs: data.blackTimeLeftMs,
                        currentTurn: data.currentTurn,
                    };
                }
            }
        });
    }

    function subscribeToChatTopic(gameId: string, activeClient: Client) {
        activeClient.subscribe(`/topic/game/${gameId}/chat`, (message) => {
            const msg = JSON.parse(message.body);
            chatMessages.value = [...chatMessages.value, msg];
        });
    }

    async function joinAsSpectator(gameId: string): Promise<boolean> {
        const resp = await fetch(`http://localhost:8080/api/game/${gameId}`);
        if (!resp.ok) return false;

        const game = await resp.json();
        spectatorGame.value = game;
        chatMessages.value = game.recentChat ?? [];

        if (stompClient.value?.connected) {
            subscribeToGameTopic(gameId, stompClient.value);
            subscribeToChatTopic(gameId, stompClient.value);
        }
        return true;
    }

    function sendMove(gameId: string, move: any) {
        if (!stompClient.value?.connected) {
            console.error('DEBUG: Cannot send move, STOMP not connected');
            return;
        }
        stompClient.value.publish({
            destination: `/app/game/move/${gameId}`,
            body: JSON.stringify(move),
        });
    }

    function sendChatMessage(gameId: string, content: string) {
        if (!stompClient.value?.connected || !content.trim()) return;
        stompClient.value.publish({
            destination: `/app/game/chat/${gameId}`,
            body: JSON.stringify({ content }),
        });
    }

    function leaveQueue() {
        if (stompClient.value?.connected) {
            stompClient.value.publish({ destination: '/app/queue.leave', body: '' });
        }
        isInQueue.value = false;
    }

    function joinCasualQueue(timeControlSeconds: number = 180) {
        if (!stompClient.value?.connected) return;
        if (isInGame.value) {
            if (gameOver.value) resetMatch();
            else return;
        }
        stompClient.value.publish({
            destination: '/app/queue.join.casual',
            body: JSON.stringify({ timeControlSeconds }),
        });
        isInQueue.value = true;
    }

    function joinRatedQueue(timeControlSeconds: number = 180) {
        if (!stompClient.value?.connected) return;
        if (isInGame.value) {
            if (gameOver.value) resetMatch();
            else return;
        }
        stompClient.value.publish({
            destination: '/app/queue.join.rated',
            body: JSON.stringify({ timeControlSeconds }),
        });
        isInQueue.value = true;
    }

    function resetMatch() {
        matchFound.value = null;
        spectatorGame.value = null;
        isInQueue.value = false;
        lastMoveReceived.value = null;
        gameOver.value = null;
        chatMessages.value = [];
    }

    return {
        isConnected,
        isInQueue,
        matchFound,
        isInGame,
        spectatorGame,
        isSpectating,
        lastMoveReceived,
        whiteTimeLeftMs,
        blackTimeLeftMs,
        currentTurn,
        gameOver,
        chatMessages,
        connect,
        leaveQueue,
        joinCasualQueue,
        joinRatedQueue,
        joinAsSpectator,
        sendMove,
        sendChatMessage,
        resetMatch,
    };
});
