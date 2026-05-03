package kr.co.awesomelead.groupware_backend.domain.approval.repository;

import kr.co.awesomelead.groupware_backend.domain.approval.entity.ApprovalDocument;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ApprovalDocumentRepository extends JpaRepository<ApprovalDocument, Long> {

    List<ApprovalDocument> findByDrafterUserIdOrderByIdDesc(Long drafterUserId);
}
