package org.chess.matchmaking;

import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.security.Principal;

@Controller
public class GameController {

    private final SimpMessagingTemplate messagingTemplate;

    public GameController(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    @MessageMapping("/game/move/{gameId}")
    public void handleMove(@DestinationVariable String gameId, @Payload MoveMessage move, Principal principal) {
        // Broadcast the move to all subscribers of the game topic.
        messagingTemplate.convertAndSend("/topic/game/" + gameId, move);
    }

    public record MoveMessage(Object move, String playerId) {}
}
