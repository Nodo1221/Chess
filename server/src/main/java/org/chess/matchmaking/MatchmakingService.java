package org.chess.matchmaking;

import org.chess.game.GameService;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.Map;

@Service
public class MatchmakingService {

    private final Map<Integer, ConcurrentLinkedQueue<Player>> casualQueues = new ConcurrentHashMap<>();
    private final Map<Integer, ConcurrentLinkedQueue<Player>> ratedQueues = new ConcurrentHashMap<>();
    
    private final SimpMessagingTemplate messagingTemplate;
    private final GameService gameService;

    public MatchmakingService(SimpMessagingTemplate messagingTemplate, GameService gameService) {
        this.messagingTemplate = messagingTemplate;
        this.gameService = gameService;
    }

    public void joinCasualQueue(String playerId, String nickname, int timeControlSeconds) {
        Player player = new Player(playerId, nickname);
        var queue = casualQueues.computeIfAbsent(timeControlSeconds, k -> new ConcurrentLinkedQueue<>());
        queue.add(player);
        tryMatch(queue, "/topic/casual-matches", timeControlSeconds);
    }

    public void joinRatedQueue(String playerId, String nickname, int timeControlSeconds) {
        Player player = new Player(playerId, nickname);
        var queue = ratedQueues.computeIfAbsent(timeControlSeconds, k -> new ConcurrentLinkedQueue<>());
        queue.add(player);
        tryMatch(queue, "/topic/rated-matches", timeControlSeconds);
    }

    private synchronized void tryMatch(ConcurrentLinkedQueue<Player> queue, String topic, int timeControlSeconds) {
        while (queue.size() >= 2) {
            Player p1 = queue.poll();
            Player p2 = queue.poll();
            if (p1 != null && p2 != null) {
                String gameId = UUID.randomUUID().toString();
                
                MatchFoundResponse match;
                if (Math.random() > 0.5) {
                    match = new MatchFoundResponse(gameId, p1, p2, timeControlSeconds);
                    gameService.startGame(gameId, p1.id(), p2.id(), timeControlSeconds);
                } else {
                    match = new MatchFoundResponse(gameId, p2, p1, timeControlSeconds);
                    gameService.startGame(gameId, p2.id(), p1.id(), timeControlSeconds);
                }
                
                messagingTemplate.convertAndSend(topic, match);
            }
        }
    }

    public record Player(String id, String nickname) {}

    public record MatchFoundResponse(String gameId, Player whitePlayer, Player blackPlayer, int timeControlSeconds) {}
}
