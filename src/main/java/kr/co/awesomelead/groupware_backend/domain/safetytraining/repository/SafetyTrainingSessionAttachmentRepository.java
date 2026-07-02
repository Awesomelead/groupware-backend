package kr.co.awesomelead.groupware_backend.domain.safetytraining.repository;

import kr.co.awesomelead.groupware_backend.domain.safetytraining.entity.SafetyTrainingSessionAttachment;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SafetyTrainingSessionAttachmentRepository
        extends JpaRepository<SafetyTrainingSessionAttachment, Long> {

    List<SafetyTrainingSessionAttachment> findAllBySessionIdOrderByIdAsc(Long sessionId);

    void deleteBySessionId(Long sessionId);
}
