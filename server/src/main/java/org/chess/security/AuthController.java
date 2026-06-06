package org.chess.security;

import lombok.Builder;
import lombok.Data;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final JwtService jwtService;

    public AuthController(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @PostMapping("/guest")
    public GuestResponse loginAsGuest() {
        String guestId = "guest-" + UUID.randomUUID().toString().substring(0, 8);
        String nickname = "Guest_" + guestId.substring(6);
        String token = jwtService.generateGuestToken(guestId, nickname);

        return GuestResponse.builder()
                .guestId(guestId)
                .nickname(nickname)
                .token(token)
                .build();
    }

    @Data
    @Builder
    public static class GuestResponse {
        private String guestId;
        private String nickname;
        private String token;
    }
}
