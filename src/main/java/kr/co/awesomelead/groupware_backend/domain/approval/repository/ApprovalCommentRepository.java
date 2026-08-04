package kr.co.awesomelead.groupware_backend.domain.approval.repository;

import kr.co.awesomelead.groupware_backend.domain.approval.entity.ApprovalComment;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ApprovalCommentRepository extends JpaRepository<ApprovalComment, Long> {

    List<ApprovalComment> findByDocumentIdOrderByCreatedAtAscIdAsc(Long documentId);

    void deleteByDocumentId(Long documentId);
}
