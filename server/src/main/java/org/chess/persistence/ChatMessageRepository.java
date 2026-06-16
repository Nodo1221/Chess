package org.chess.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ChatMessageRepository extends JpaRepository<ChatMessageEntity, Long> {
    List<ChatMessageEntity> findTop50ByGameIdOrderBySentAtAsc(String gameId);
}
