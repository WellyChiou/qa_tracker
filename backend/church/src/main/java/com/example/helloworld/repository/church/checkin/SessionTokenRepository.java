package com.example.helloworld.repository.church.checkin;

import com.example.helloworld.entity.church.checkin.SessionToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.Optional;

public interface SessionTokenRepository extends JpaRepository<SessionToken, Long> {
    Optional<SessionToken> findFirstBySessionIdAndTokenAndExpiresAtAfter(Long sessionId, String token, LocalDateTime now);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = "DELETE FROM session_tokens WHERE session_id = ?1 AND expires_at < ?2", nativeQuery = true)
    int deleteExpired(Long sessionId, LocalDateTime now);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value =
        "DELETE FROM session_tokens " +
        "WHERE session_id = ?1 " +
        "  AND id NOT IN (" +
        "    SELECT id FROM (" +
        "      SELECT id FROM session_tokens " +
        "      WHERE session_id = ?1 " +
        "      ORDER BY expires_at DESC " +
        "      LIMIT ?2" +
        "    ) t" +
        "  )", nativeQuery = true)
    int keepLatestN(Long sessionId, int n);
}

