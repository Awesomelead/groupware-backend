package kr.co.awesomelead.groupware_backend.domain.approval.repository;

import kr.co.awesomelead.groupware_backend.domain.approval.entity.ApprovalAttachment;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ApprovalAttachmentRepository extends JpaRepository<ApprovalAttachment, Long> {

    List<ApprovalAttachment> findByDocumentIdOrderByIdAsc(Long documentId);
}
