package org.chess.chat;

import org.chess.persistence.ChatMessageEntity;
import org.chess.persistence.ChatMessageRepository;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Controller;

import java.security.Principal;
import java.time.Instant;

@Controller
public class ChatController {

    private final SimpMessagingTemplate messagingTemplate;
    private final ChatMessageRepository chatMessageRepository;

    public ChatController(SimpMessagingTemplate messagingTemplate, ChatMessageRepository chatMessageRepository) {
        this.messagingTemplate = messagingTemplate;
        this.chatMessageRepository = chatMessageRepository;
    }

    @MessageMapping("/game/chat/{gameId}")
    public void handleChat(@DestinationVariable String gameId,
                           @Payload ChatRequest request,
                           Principal principal) {
        if (principal == null) return;
        String content = request.content().strip();
        if (content.isBlank() || content.length() > 500) return;

        String senderId = principal.getName();
        String senderNickname = nicknameOf(principal);

        ChatMessageEntity entity = chatMessageRepository.save(
                new ChatMessageEntity(gameId, senderId, senderNickname, content)
        );

        messagingTemplate.convertAndSend(
                "/topic/game/" + gameId + "/chat",
                new ChatMessage(senderId, senderNickname, content, entity.getSentAt())
        );
    }

    private String nicknameOf(Principal principal) {
        if (principal instanceof UsernamePasswordAuthenticationToken auth
                && auth.getDetails() instanceof String nickname) {
            return nickname;
        }
        return principal.getName();
    }

    public record ChatRequest(String content) {}
    public record ChatMessage(String senderId, String senderNickname, String content, Instant sentAt) {}
}
