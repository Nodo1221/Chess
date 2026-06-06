package org.chess.matchmaking;

import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Controller;

import java.security.Principal;

@Controller
public class MatchmakingController {

    private final MatchmakingService matchmakingService;

    public MatchmakingController(MatchmakingService matchmakingService) {
        this.matchmakingService = matchmakingService;
    }

    @MessageMapping("/queue.join.casual")
    public void joinCasualQueue(Principal principal, @Payload JoinQueueRequest request) {
        if (principal == null) return;
        
        String playerId = principal.getName();
        String nickname = playerId; 
        
        matchmakingService.joinCasualQueue(playerId, nickname, request.timeControlSeconds());
    }

    @MessageMapping("/queue.join.rated")
    public void joinRatedQueue(Principal principal, @Payload JoinQueueRequest request) {
        if (principal == null) return;
        
        String playerId = principal.getName();
        String nickname = playerId;
        
        matchmakingService.joinRatedQueue(playerId, nickname, request.timeControlSeconds());
    }
    
    public record JoinQueueRequest(int timeControlSeconds) {}
}
