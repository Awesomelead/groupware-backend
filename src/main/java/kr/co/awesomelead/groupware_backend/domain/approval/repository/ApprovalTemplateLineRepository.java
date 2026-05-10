package kr.co.awesomelead.groupware_backend.domain.approval.repository;

import kr.co.awesomelead.groupware_backend.domain.approval.entity.ApprovalTemplateLine;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ApprovalTemplateLineRepository extends JpaRepository<ApprovalTemplateLine, Long> {

    List<ApprovalTemplateLine> findByTemplateIdOrderBySequenceNoAscIdAsc(Long templateId);

    void deleteByTemplateId(Long templateId);
}
