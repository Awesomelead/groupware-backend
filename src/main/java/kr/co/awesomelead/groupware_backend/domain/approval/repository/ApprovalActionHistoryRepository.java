package kr.co.awesomelead.groupware_backend.domain.approval.repository;

import kr.co.awesomelead.groupware_backend.domain.approval.entity.ApprovalActionHistory;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ApprovalActionHistoryRepository
        extends JpaRepository<ApprovalActionHistory, Long> {

    List<ApprovalActionHistory> findByDocumentIdOrderByCreatedAtAscIdAsc(Long documentId);
}
