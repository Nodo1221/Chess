package org.chess.game;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/game")
public class GameRestController {

    private final GameService gameService;

    public GameRestController(GameService gameService) {
        this.gameService = gameService;
    }

    @GetMapping("/{gameId}")
    public ResponseEntity<GameDataResponse> getGame(@PathVariable String gameId) {
        GameState game = gameService.getGame(gameId);
        if (game == null) {
            return ResponseEntity.notFound().build();
        }

        // Return a snapshot of the game
        return ResponseEntity.ok(new GameDataResponse(
                game.getGameId(),
                game.getWhitePlayerId(),
                game.getBlackPlayerId(),
                game.getTimeControlSeconds(),
                game.getWhiteTimeLeftMs(),
                game.getBlackTimeLeftMs(),
                game.getCurrentTurn(),
                game.isGameOver(),
                game.getWinner(),
                game.getMoves()
        ));
    }

    public record GameDataResponse(
            String gameId,
            String whitePlayerId,
            String blackPlayerId,
            int timeControlSeconds,
            long whiteTimeLeftMs,
            long blackTimeLeftMs,
            char currentTurn,
            boolean isGameOver,
            String winner,
            List<Object> moves
    ) {}
}
