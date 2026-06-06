package org.chess.security;

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

        return new GuestResponse(guestId, nickname, token);
    }

    public record GuestResponse(String guestId, String nickname, String token) {}
}
