package kr.co.awesomelead.groupware_backend.domain.safetytraining.repository;

import kr.co.awesomelead.groupware_backend.domain.safetytraining.entity.SafetyTrainingSessionAttendee;
import kr.co.awesomelead.groupware_backend.domain.safetytraining.enums.SafetyTrainingAttendeeStatus;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface SafetyTrainingSessionAttendeeRepository
        extends JpaRepository<SafetyTrainingSessionAttendee, Long> {

    Optional<SafetyTrainingSessionAttendee> findBySessionIdAndUserId(Long sessionId, Long userId);

    long countBySessionIdAndStatus(Long sessionId, SafetyTrainingAttendeeStatus status);

    List<SafetyTrainingSessionAttendee> findAllBySessionIdAndStatus(
            Long sessionId, SafetyTrainingAttendeeStatus status);

    @Query(
            """
            SELECT a
            FROM SafetyTrainingSessionAttendee a
            JOIN FETCH a.user u
            LEFT JOIN FETCH u.department d
            WHERE a.session.id = :sessionId
            ORDER BY u.nameKor ASC
            """)
    List<SafetyTrainingSessionAttendee> findAllBySessionIdWithUser(
            @Param("sessionId") Long sessionId);

    @Query(
            """
            SELECT a.session.id
            FROM SafetyTrainingSessionAttendee a
            WHERE a.user.id = :userId
              AND a.session.id IN :sessionIds
              AND a.status = kr.co.awesomelead.groupware_backend.domain.safetytraining.enums.SafetyTrainingAttendeeStatus.SIGNED
            """)
    List<Long> findSignedSessionIdsByUserIdAndSessionIds(
            @Param("userId") Long userId, @Param("sessionIds") List<Long> sessionIds);

    void deleteBySessionId(Long sessionId);
}
