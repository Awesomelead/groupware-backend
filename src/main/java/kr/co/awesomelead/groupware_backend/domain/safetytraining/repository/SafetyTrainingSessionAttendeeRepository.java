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

    void deleteBySessionId(Long sessionId);
}
