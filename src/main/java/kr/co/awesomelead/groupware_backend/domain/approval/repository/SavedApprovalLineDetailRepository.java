package kr.co.awesomelead.groupware_backend.domain.approval.repository;

import kr.co.awesomelead.groupware_backend.domain.approval.entity.SavedApprovalLineDetail;

import org.springframework.data.jpa.repository.JpaRepository;

public interface SavedApprovalLineDetailRepository extends JpaRepository<SavedApprovalLineDetail, Long> {

    void deleteBySavedLineId(Long savedLineId);
}
