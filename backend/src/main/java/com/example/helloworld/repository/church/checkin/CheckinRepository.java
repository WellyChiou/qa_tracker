package com.example.helloworld.repository.church.checkin;

import com.example.helloworld.entity.church.checkin.Checkin;
import com.example.helloworld.dto.church.checkin.ManualCheckinRow;
import com.example.helloworld.dto.church.checkin.SessionCheckinRow;
import com.example.helloworld.dto.church.checkin.UncheckedPersonRow;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface CheckinRepository extends JpaRepository<Checkin, Long> {

    long countBySessionIdAndCanceledFalse(Long sessionId);

    Optional<Checkin> findBySessionIdAndMemberId(Long sessionId, Long memberId);

    @Query(value =
      "SELECT " +
      "  c.id as id, " +
      "  COALESCE(p.member_no, '') as memberNo, " +
      "  COALESCE(p.display_name, p.person_name, '') as memberName, " +
      "  c.checked_in_at as checkedInAt, " +
      "  CASE WHEN c.manual = 1 THEN 1 ELSE 0 END as manual, " +
      "  c.manual_by as manualBy, " +
      "  c.ip as ip, " +
      "  c.user_agent as userAgent, " +
      "  CASE WHEN c.canceled = 1 THEN 1 ELSE 0 END as canceled " +
      "FROM checkins c " +
      "LEFT JOIN persons p ON p.id = c.member_id " +
      "WHERE c.session_id = :sessionId " +
      "  AND (:includeCanceled = true OR c.canceled IS NULL OR c.canceled = 0) " +
      "ORDER BY c.checked_in_at DESC",
      nativeQuery = true)
    List<SessionCheckinRow> findSessionRows(@Param("sessionId") Long sessionId, @Param("includeCanceled") boolean includeCanceled);

    @Query(value =
      "SELECT " +
      "  c.id as id, " +
      "  c.session_id as sessionId, " +
      "  s.title as sessionTitle, " +
      "  DATE_FORMAT(s.session_date, '%Y-%m-%d') as sessionDate, " +
      "  p.member_no as memberNo, " +
      "  COALESCE(p.display_name, p.person_name) as memberName, " +
      "  c.checked_in_at as checkedInAt, " +
      "  c.ip as ip, " +
      "  c.user_agent as userAgent, " +
      "  c.manual_by as manualBy, " +
      "  c.manual_note as manualNote, " +
      "  CASE WHEN c.canceled = 1 THEN 1 ELSE 0 END as canceled, " +
      "  c.canceled_at as canceledAt, " +
      "  c.canceled_by as canceledBy, " +
      "  c.canceled_note as canceledNote " +
      "FROM checkins c " +
      "JOIN persons p ON p.id = c.member_id " +
      "JOIN sessions s ON s.id = c.session_id " +
      "WHERE c.manual = true " +
      "  AND (:includeCanceled = true OR c.canceled = false) " +
      "  AND (:q IS NULL OR :q = '' OR p.member_no LIKE CONCAT('%', :q, '%') OR p.person_name LIKE CONCAT('%', :q, '%') OR p.display_name LIKE CONCAT('%', :q, '%') OR s.title LIKE CONCAT('%', :q, '%')) " +
      "  AND (:fromTs IS NULL OR c.checked_in_at >= :fromTs) " +
      "  AND (:toTs IS NULL OR c.checked_in_at <= :toTs) " +
      "ORDER BY c.checked_in_at DESC",
      nativeQuery = true)
    List<ManualCheckinRow> findManualRows(
        @Param("q") String q,
        @Param("fromTs") LocalDateTime fromTs,
        @Param("toTs") LocalDateTime toTs,
        @Param("includeCanceled") boolean includeCanceled
    );

    @Query(value =
      "SELECT " +
      "  p.id as id, " +
      "  COALESCE(p.member_no, '') as memberNo, " +
      "  COALESCE(p.display_name, p.person_name, '') as memberName " +
      "FROM persons p " +
      "WHERE p.is_active = true " +
      "  AND p.id NOT IN (" +
      "    SELECT c.member_id FROM checkins c " +
      "    WHERE c.session_id = :sessionId " +
      "      AND (c.canceled IS NULL OR c.canceled = 0)" +
      "  ) " +
      "ORDER BY p.person_name ASC",
      nativeQuery = true)
    List<UncheckedPersonRow> findUncheckedPersons(@Param("sessionId") Long sessionId);
}

