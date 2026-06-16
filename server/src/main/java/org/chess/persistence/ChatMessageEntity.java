package org.chess.persistence;

import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "chat_messages", indexes = @Index(columnList = "game_id"))
public class ChatMessageEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "game_id", nullable = false)
    private String gameId;

    @Column(name = "sender_id", nullable = false)
    private String senderId;

    @Column(name = "sender_nickname", nullable = false)
    private String senderNickname;

    @Column(nullable = false, length = 500)
    private String content;

    @Column(name = "sent_at", nullable = false)
    private Instant sentAt = Instant.now();

    public ChatMessageEntity() {}

    public ChatMessageEntity(String gameId, String senderId, String senderNickname, String content) {
        this.gameId = gameId;
        this.senderId = senderId;
        this.senderNickname = senderNickname;
        this.content = content;
    }

    public Long getId() { return id; }
    public String getGameId() { return gameId; }
    public String getSenderId() { return senderId; }
    public String getSenderNickname() { return senderNickname; }
    public String getContent() { return content; }
    public Instant getSentAt() { return sentAt; }
}
