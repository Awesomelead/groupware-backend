package kr.co.awesomelead.groupware_backend.domain.approval.repository;

import kr.co.awesomelead.groupware_backend.domain.approval.entity.Approval;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ApprovalRepository extends JpaRepository<Approval, Long> {}
