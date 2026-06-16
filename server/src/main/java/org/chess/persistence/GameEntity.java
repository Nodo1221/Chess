package org.chess.persistence;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "games")
public class GameEntity {

    @Id
    private String id;

    @Column(name = "white_player_id", nullable = false)
    private String whitePlayerId;

    @Column(name = "white_nickname", nullable = false)
    private String whiteNickname;

    @Column(name = "black_player_id", nullable = false)
    private String blackPlayerId;

    @Column(name = "black_nickname", nullable = false)
    private String blackNickname;

    @Column(name = "time_control_seconds", nullable = false)
    private int timeControlSeconds;

    @Convert(converter = JsonListConverter.class)
    @Column(columnDefinition = "TEXT", nullable = false)
    private List<Object> moves = new ArrayList<>();

    @Column(nullable = false)
    private String status = "active";

    @Column
    private String result;

    @Column(name = "result_reason")
    private String resultReason;

    @Column(name = "started_at", nullable = false)
    private Instant startedAt = Instant.now();

    @Column(name = "ended_at")
    private Instant endedAt;

    // --- future: add nullable player_uuid FK columns here for rated accounts ---

    public GameEntity() {}

    public GameEntity(String id, String whitePlayerId, String whiteNickname,
                      String blackPlayerId, String blackNickname, int timeControlSeconds) {
        this.id = id;
        this.whitePlayerId = whitePlayerId;
        this.whiteNickname = whiteNickname;
        this.blackPlayerId = blackPlayerId;
        this.blackNickname = blackNickname;
        this.timeControlSeconds = timeControlSeconds;
    }

    public String getId() { return id; }
    public String getWhitePlayerId() { return whitePlayerId; }
    public String getWhiteNickname() { return whiteNickname; }
    public String getBlackPlayerId() { return blackPlayerId; }
    public String getBlackNickname() { return blackNickname; }
    public int getTimeControlSeconds() { return timeControlSeconds; }
    public List<Object> getMoves() { return moves; }
    public void setMoves(List<Object> moves) { this.moves = moves; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getResult() { return result; }
    public void setResult(String result) { this.result = result; }
    public String getResultReason() { return resultReason; }
    public void setResultReason(String resultReason) { this.resultReason = resultReason; }
    public Instant getStartedAt() { return startedAt; }
    public Instant getEndedAt() { return endedAt; }
    public void setEndedAt(Instant endedAt) { this.endedAt = endedAt; }
}
