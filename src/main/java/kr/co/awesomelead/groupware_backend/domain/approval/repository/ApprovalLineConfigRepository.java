package kr.co.awesomelead.groupware_backend.domain.approval.repository;

import kr.co.awesomelead.groupware_backend.domain.approval.entity.ApprovalLineConfig;
import kr.co.awesomelead.groupware_backend.domain.approval.enums.DocumentType;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ApprovalLineConfigRepository
        extends JpaRepository<ApprovalLineConfig, DocumentType> {}
