package org.chess.game;

import java.util.ArrayList;
import java.util.List;

public class GameState {
    private final String gameId;
    private final String whitePlayerId;
    private final String blackPlayerId;
    
    private long whiteTimeLeftMs;
    private long blackTimeLeftMs;
    
    private char currentTurn = 'w';
    private long lastMoveTimeMs;
    
    private boolean isGameOver = false;
    private String winner = null;

    private final List<Object> moves = new ArrayList<>();
    private final int timeControlSeconds;

    public GameState(String gameId, String whitePlayerId, String blackPlayerId, int timeControlSeconds) {
        this.gameId = gameId;
        this.whitePlayerId = whitePlayerId;
        this.blackPlayerId = blackPlayerId;
        this.timeControlSeconds = timeControlSeconds;
        
        long timeMs = timeControlSeconds * 1000L;
        this.whiteTimeLeftMs = timeMs;
        this.blackTimeLeftMs = timeMs;
        
        // Timer starts when the game is created (or on first move, but let's start it immediately for simplicity)
        this.lastMoveTimeMs = System.currentTimeMillis();
    }

    public synchronized boolean applyMove(String playerId, Object moveData) {
        if (isGameOver) return false;

        // Check turn
        if (currentTurn == 'w' && !playerId.equals(whitePlayerId)) return false;
        if (currentTurn == 'b' && !playerId.equals(blackPlayerId)) return false;

        long now = System.currentTimeMillis();
        long elapsed = now - lastMoveTimeMs;

        if (currentTurn == 'w') {
            whiteTimeLeftMs -= elapsed;
            if (whiteTimeLeftMs <= 0) {
                whiteTimeLeftMs = 0;
                isGameOver = true;
                winner = "black";
                return false;
            }
            currentTurn = 'b';
        } else {
            blackTimeLeftMs -= elapsed;
            if (blackTimeLeftMs <= 0) {
                blackTimeLeftMs = 0;
                isGameOver = true;
                winner = "white";
                return false;
            }
            currentTurn = 'w';
        }

        moves.add(moveData);
        lastMoveTimeMs = now;
        return true;
    }

    public synchronized boolean checkTimeout() {
        if (isGameOver) return false;

        long now = System.currentTimeMillis();
        long elapsed = now - lastMoveTimeMs;

        if (currentTurn == 'w') {
            if (whiteTimeLeftMs - elapsed <= 0) {
                whiteTimeLeftMs = 0;
                isGameOver = true;
                winner = "black";
                return true;
            }
        } else {
            if (blackTimeLeftMs - elapsed <= 0) {
                blackTimeLeftMs = 0;
                isGameOver = true;
                winner = "white";
                return true;
            }
        }
        return false;
    }

    public String getGameId() { return gameId; }
    public String getWhitePlayerId() { return whitePlayerId; }
    public String getBlackPlayerId() { return blackPlayerId; }
    public int getTimeControlSeconds() { return timeControlSeconds; }
    public List<Object> getMoves() { return moves; }
    
    public long getWhiteTimeLeftMs() { 
        if (isGameOver || currentTurn != 'w') return whiteTimeLeftMs;
        return Math.max(0, whiteTimeLeftMs - (System.currentTimeMillis() - lastMoveTimeMs));
    }
    public long getBlackTimeLeftMs() { 
        if (isGameOver || currentTurn != 'b') return blackTimeLeftMs;
        return Math.max(0, blackTimeLeftMs - (System.currentTimeMillis() - lastMoveTimeMs));
    }
    public char getCurrentTurn() { return currentTurn; }
    public boolean isGameOver() { return isGameOver; }
    public String getWinner() { return winner; }
}
