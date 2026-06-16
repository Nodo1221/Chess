package org.chess.game;

import org.chess.persistence.ChatMessageEntity;
import org.chess.persistence.ChatMessageRepository;
import org.chess.persistence.GameEntity;
import org.chess.persistence.GameRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/api/game")
public class GameRestController {

    private final GameService gameService;
    private final GameRepository gameRepository;
    private final ChatMessageRepository chatMessageRepository;

    public GameRestController(GameService gameService, GameRepository gameRepository,
                              ChatMessageRepository chatMessageRepository) {
        this.gameService = gameService;
        this.gameRepository = gameRepository;
        this.chatMessageRepository = chatMessageRepository;
    }

    @GetMapping("/{gameId}")
    public ResponseEntity<GameDataResponse> getGame(@PathVariable String gameId) {
        List<ChatMessageDto> recentChat = chatMessageRepository
                .findTop50ByGameIdOrderBySentAtAsc(gameId)
                .stream()
                .map(m -> new ChatMessageDto(m.getSenderId(), m.getSenderNickname(), m.getContent(), m.getSentAt()))
                .toList();

        GameState active = gameService.getGame(gameId);
        if (active != null) {
            return ResponseEntity.ok(new GameDataResponse(
                    active.getGameId(),
                    active.getWhitePlayerId(), active.getWhiteNickname(),
                    active.getBlackPlayerId(), active.getBlackNickname(),
                    active.getTimeControlSeconds(),
                    active.getWhiteTimeLeftMs(), active.getBlackTimeLeftMs(),
                    active.getCurrentTurn(), false, null, null, "active",
                    active.getMoves(), recentChat
            ));
        }

        return gameRepository.findById(gameId)
                .map(entity -> ResponseEntity.ok(finishedResponse(entity, recentChat)))
                .orElse(ResponseEntity.notFound().build());
    }

    private GameDataResponse finishedResponse(GameEntity e, List<ChatMessageDto> chat) {
        return new GameDataResponse(
                e.getId(),
                e.getWhitePlayerId(), e.getWhiteNickname(),
                e.getBlackPlayerId(), e.getBlackNickname(),
                e.getTimeControlSeconds(),
                0, 0, '-', true,
                e.getResult(), e.getResultReason(), "finished",
                e.getMoves(), chat
        );
    }

    public record ChatMessageDto(String senderId, String senderNickname, String content, Instant sentAt) {}

    public record GameDataResponse(
            String gameId,
            String whitePlayerId, String whiteNickname,
            String blackPlayerId, String blackNickname,
            int timeControlSeconds,
            long whiteTimeLeftMs, long blackTimeLeftMs,
            char currentTurn, boolean isGameOver,
            String winner, String resultReason, String status,
            List<Object> moves,
            List<ChatMessageDto> recentChat
    ) {}
}
