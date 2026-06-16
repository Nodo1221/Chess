package org.chess.game;

import org.chess.persistence.GameEntity;
import org.chess.persistence.GameRepository;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class GameService {

    private final Map<String, GameState> activeGames = new ConcurrentHashMap<>();
    private final SimpMessagingTemplate messagingTemplate;
    private final GameRepository gameRepository;

    public GameService(SimpMessagingTemplate messagingTemplate, GameRepository gameRepository) {
        this.messagingTemplate = messagingTemplate;
        this.gameRepository = gameRepository;
    }

    public void startGame(String gameId, String whiteId, String whiteNickname,
                          String blackId, String blackNickname, int timeControlSeconds) {
        GameState game = new GameState(gameId, whiteId, whiteNickname, blackId, blackNickname, timeControlSeconds);
        activeGames.put(gameId, game);
        gameRepository.save(new GameEntity(gameId, whiteId, whiteNickname, blackId, blackNickname, timeControlSeconds));
    }

    public GameState getGame(String gameId) {
        return activeGames.get(gameId);
    }

    public void processMove(String gameId, String playerId, Object moveData, boolean isCheckmate) {
        GameState game = activeGames.get(gameId);
        if (game == null) return;

        boolean validMove = game.applyMove(playerId, moveData);

        if (validMove) {
            gameRepository.findById(gameId).ifPresent(entity -> {
                entity.setMoves(game.getMoves());
                gameRepository.save(entity);
            });
            GameUpdate update = new GameUpdate(
                    moveData, playerId,
                    game.getWhiteTimeLeftMs(), game.getBlackTimeLeftMs(),
                    game.getCurrentTurn()
            );
            messagingTemplate.convertAndSend("/topic/game/" + gameId, update);

            if (isCheckmate) {
                // Turn has already flipped to the loser's side after applyMove
                String winner = game.getCurrentTurn() == 'b' ? "white" : "black";
                game.declareWinner(winner);
                broadcastGameOver(game, "checkmate");
            }
        } else if (game.isGameOver()) {
            broadcastGameOver(game, "timeout");
        }
    }

    public void resign(String gameId, String playerId) {
        GameState game = activeGames.get(gameId);
        if (game == null) return;
        String winner;
        if (playerId.equals(game.getWhitePlayerId())) {
            winner = "black";
        } else if (playerId.equals(game.getBlackPlayerId())) {
            winner = "white";
        } else {
            return;
        }
        game.declareWinner(winner);
        broadcastGameOver(game, "resignation");
    }

    @Scheduled(fixedRate = 500)
    public void checkTimeouts() {
        for (GameState game : activeGames.values()) {
            if (game.checkTimeout()) {
                broadcastGameOver(game, "timeout");
            }
        }
    }

    private void broadcastGameOver(GameState game, String reason) {
        activeGames.remove(game.getGameId());
        gameRepository.findById(game.getGameId()).ifPresent(entity -> {
            entity.setStatus("finished");
            entity.setResult(game.getWinner());
            entity.setResultReason(reason);
            entity.setEndedAt(Instant.now());
            entity.setMoves(game.getMoves());
            gameRepository.save(entity);
        });
        messagingTemplate.convertAndSend(
                "/topic/game/" + game.getGameId(),
                new GameOverMessage(game.getWinner(), reason)
        );
    }

    public record GameUpdate(Object move, String playerId, long whiteTimeLeftMs, long blackTimeLeftMs, char currentTurn) {}
    public record GameOverMessage(String winner, String reason) {}
}
