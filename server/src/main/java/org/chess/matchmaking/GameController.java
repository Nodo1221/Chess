package org.chess.matchmaking;

import org.chess.game.GameService;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Controller;

import java.security.Principal;

@Controller
public class GameController {

    private final GameService gameService;

    public GameController(GameService gameService) {
        this.gameService = gameService;
    }

    @MessageMapping("/game/move/{gameId}")
    public void handleMove(@DestinationVariable String gameId, @Payload MoveMessage move, Principal principal) {
        gameService.processMove(gameId, move.playerId(), move.move());
    }

    public record MoveMessage(Object move, String playerId) {}
}
