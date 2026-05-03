package kr.co.awesomelead.groupware_backend.domain.approval.repository;

import kr.co.awesomelead.groupware_backend.domain.approval.entity.ApprovalDocumentLine;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ApprovalDocumentLineRepository extends JpaRepository<ApprovalDocumentLine, Long> {

    List<ApprovalDocumentLine> findByDocumentIdOrderBySequenceNoAscIdAsc(Long documentId);
}
