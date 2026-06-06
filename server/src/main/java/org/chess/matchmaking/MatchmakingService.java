package org.chess.matchmaking;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;

@Service
public class MatchmakingService {

    private final ConcurrentLinkedQueue<Player> casualQueue = new ConcurrentLinkedQueue<>();
    private final ConcurrentLinkedQueue<Player> ratedQueue = new ConcurrentLinkedQueue<>();
    private final SimpMessagingTemplate messagingTemplate;

    public MatchmakingService(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    public void joinCasualQueue(String playerId, String nickname) {
        Player player = new Player(playerId, nickname);
        casualQueue.add(player);
        tryMatch(casualQueue, "/topic/casual-matches");
    }

    public void joinRatedQueue(String playerId, String nickname) {
        Player player = new Player(playerId, nickname);
        ratedQueue.add(player);
        tryMatch(ratedQueue, "/topic/rated-matches");
    }

    private synchronized void tryMatch(ConcurrentLinkedQueue<Player> queue, String topic) {
        while (queue.size() >= 2) {
            Player p1 = queue.poll();
            Player p2 = queue.poll();
            if (p1 != null && p2 != null) {
                String gameId = UUID.randomUUID().toString();
                MatchFoundResponse match = new MatchFoundResponse(gameId, p1, p2);
                
                // Notify both players. In a real app, you might use private /user/queue/match
                messagingTemplate.convertAndSend(topic, match);
            }
        }
    }

    @Data
    @AllArgsConstructor
    public static class Player {
        private String id;
        private String nickname;
    }

    @Data
    @AllArgsConstructor
    public static class MatchFoundResponse {
        private String gameId;
        private Player player1;
        private Player player2;
    }
}
