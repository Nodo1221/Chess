package org.chess.game;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class GameService {

    private final Map<String, GameState> activeGames = new ConcurrentHashMap<>();
    private final SimpMessagingTemplate messagingTemplate;

    public GameService(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    public void startGame(String gameId, String whiteId, String blackId, int timeControlSeconds) {
        GameState game = new GameState(gameId, whiteId, blackId, timeControlSeconds);
        activeGames.put(gameId, game);
    }

    public void processMove(String gameId, String playerId, Object moveData) {
        GameState game = activeGames.get(gameId);
        if (game == null) return;

        boolean validMove = game.applyMove(playerId, moveData);
        
        if (validMove) {
            // Broadcast the move and the updated clocks
            GameUpdate update = new GameUpdate(moveData, playerId, game.getWhiteTimeLeftMs(), game.getBlackTimeLeftMs(), game.getCurrentTurn());
            messagingTemplate.convertAndSend("/topic/game/" + gameId, update);
        } else if (game.isGameOver()) {
            broadcastGameOver(game);
        }
    }

    @Scheduled(fixedRate = 500)
    public void checkTimeouts() {
        for (GameState game : activeGames.values()) {
            if (game.checkTimeout()) {
                broadcastGameOver(game);
            }
        }
    }

    private void broadcastGameOver(GameState game) {
        activeGames.remove(game.getGameId());
        GameOverMessage msg = new GameOverMessage(game.getWinner(), "timeout");
        messagingTemplate.convertAndSend("/topic/game/" + game.getGameId(), msg);
    }

    public record GameUpdate(Object move, String playerId, long whiteTimeLeftMs, long blackTimeLeftMs, char currentTurn) {}
    public record GameOverMessage(String winner, String reason) {}
}
