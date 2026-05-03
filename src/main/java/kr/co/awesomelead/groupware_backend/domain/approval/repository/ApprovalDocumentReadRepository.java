package kr.co.awesomelead.groupware_backend.domain.approval.repository;

import kr.co.awesomelead.groupware_backend.domain.approval.entity.ApprovalDocumentRead;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ApprovalDocumentReadRepository extends JpaRepository<ApprovalDocumentRead, Long> {

    List<ApprovalDocumentRead> findByDocumentIdOrderByIdAsc(Long documentId);
}
