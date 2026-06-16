package org.chess.matchmaking;

import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
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
        matchmakingService.joinCasualQueue(principal.getName(), nicknameOf(principal), request.timeControlSeconds());
    }

    @MessageMapping("/queue.join.rated")
    public void joinRatedQueue(Principal principal, @Payload JoinQueueRequest request) {
        if (principal == null) return;
        matchmakingService.joinRatedQueue(principal.getName(), nicknameOf(principal), request.timeControlSeconds());
    }

    @MessageMapping("/queue.leave")
    public void leaveQueue(Principal principal) {
        if (principal == null) return;
        matchmakingService.leaveQueue(principal.getName());
    }

    private String nicknameOf(Principal principal) {
        if (principal instanceof UsernamePasswordAuthenticationToken auth
                && auth.getDetails() instanceof String nickname) {
            return nickname;
        }
        return principal.getName();
    }

    public record JoinQueueRequest(int timeControlSeconds) {}
}
