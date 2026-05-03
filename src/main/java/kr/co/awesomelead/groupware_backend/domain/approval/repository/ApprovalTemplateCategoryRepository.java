package kr.co.awesomelead.groupware_backend.domain.approval.repository;

import kr.co.awesomelead.groupware_backend.domain.approval.entity.ApprovalTemplateCategory;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ApprovalTemplateCategoryRepository
        extends JpaRepository<ApprovalTemplateCategory, Long> {

    Optional<ApprovalTemplateCategory> findByCode(String code);

    List<ApprovalTemplateCategory> findByIsActiveTrueOrderBySortOrderAscIdAsc();
}
