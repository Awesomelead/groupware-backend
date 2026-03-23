package kr.co.awesomelead.groupware_backend.domain.safetytraining.repository;

import kr.co.awesomelead.groupware_backend.domain.safetytraining.entity.SafetyTrainingSessionAttendee;

import org.springframework.data.jpa.repository.JpaRepository;

public interface SafetyTrainingSessionAttendeeRepository
        extends JpaRepository<SafetyTrainingSessionAttendee, Long> {}
