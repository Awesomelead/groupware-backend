package kr.co.awesomelead.groupware_backend.domain.approval.repository;

import kr.co.awesomelead.groupware_backend.domain.approval.entity.ApprovalTemplate;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ApprovalTemplateRepository extends JpaRepository<ApprovalTemplate, Long> {

    Optional<ApprovalTemplate> findByCode(String code);

    boolean existsByCode(String code);

    boolean existsByCategoryId(Long categoryId);

    List<ApprovalTemplate> findByCategoryIdAndIsActiveTrueOrderByIdAsc(Long categoryId);

    List<ApprovalTemplate> findByIsActiveTrueOrderByIdAsc();
}
