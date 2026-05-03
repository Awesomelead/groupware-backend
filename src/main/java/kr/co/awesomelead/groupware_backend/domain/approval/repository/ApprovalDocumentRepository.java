package kr.co.awesomelead.groupware_backend.domain.approval.repository;

import kr.co.awesomelead.groupware_backend.domain.approval.entity.ApprovalDocument;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ApprovalDocumentRepository extends JpaRepository<ApprovalDocument, Long> {

    List<ApprovalDocument> findByDrafterUserIdOrderByIdDesc(Long drafterUserId);

    boolean existsByTemplateId(Long templateId);

    Optional<ApprovalDocument> findByIdAndDrafterUserId(Long id, Long drafterUserId);

    @Query(
            "select distinct d from ApprovalDocument d "
                    + "left join fetch d.lines "
                    + "where d.id = :id and d.drafterUser.id = :drafterUserId")
    Optional<ApprovalDocument> findByIdAndDrafterUserIdWithLines(
            @Param("id") Long id, @Param("drafterUserId") Long drafterUserId);
}
