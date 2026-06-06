package org.chess.matchmaking;

import org.chess.security.JwtService;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.stereotype.Controller;

import java.security.Principal;

@Controller
public class MatchmakingController {

    private final MatchmakingService matchmakingService;
    private final JwtService jwtService;

    public MatchmakingController(MatchmakingService matchmakingService, JwtService jwtService) {
        this.matchmakingService = matchmakingService;
        this.jwtService = jwtService;
    }

    @MessageMapping("/queue.join.casual")
    public void joinCasualQueue(Principal principal, SimpMessageHeaderAccessor headerAccessor) {
        if (principal == null) return;
        
        String playerId = principal.getName();
        // For simplicity, we get nickname from JWT claim again if needed, 
        // or just use playerId as nickname for now.
        // In a real app, you might store nickname in the Principal object.
        String nickname = playerId; 
        
        matchmakingService.joinCasualQueue(playerId, nickname);
    }

    @MessageMapping("/queue.join.rated")
    public void joinRatedQueue(Principal principal) {
        if (principal == null) return;
        
        String playerId = principal.getName();
        String nickname = playerId;
        
        matchmakingService.joinRatedQueue(playerId, nickname);
    }
}
