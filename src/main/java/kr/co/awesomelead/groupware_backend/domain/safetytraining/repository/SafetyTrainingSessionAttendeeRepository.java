package kr.co.awesomelead.groupware_backend.domain.safetytraining.repository;

import kr.co.awesomelead.groupware_backend.domain.safetytraining.entity.SafetyTrainingSessionAttendee;
import kr.co.awesomelead.groupware_backend.domain.safetytraining.enums.SafetyTrainingAttendeeStatus;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SafetyTrainingSessionAttendeeRepository
        extends JpaRepository<SafetyTrainingSessionAttendee, Long> {

    Optional<SafetyTrainingSessionAttendee> findBySessionIdAndUserId(Long sessionId, Long userId);

    long countBySessionIdAndStatus(Long sessionId, SafetyTrainingAttendeeStatus status);
}
