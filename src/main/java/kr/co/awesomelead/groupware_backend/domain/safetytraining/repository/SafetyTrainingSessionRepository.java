package kr.co.awesomelead.groupware_backend.domain.safetytraining.repository;

import kr.co.awesomelead.groupware_backend.domain.safetytraining.entity.SafetyTrainingSession;

import org.springframework.data.jpa.repository.JpaRepository;

public interface SafetyTrainingSessionRepository
        extends JpaRepository<SafetyTrainingSession, Long> {}
