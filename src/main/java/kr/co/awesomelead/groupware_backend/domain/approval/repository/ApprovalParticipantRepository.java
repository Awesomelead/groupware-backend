package kr.co.awesomelead.groupware_backend.domain.approval.repository;

import kr.co.awesomelead.groupware_backend.domain.approval.entity.ApprovalParticipant;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ApprovalParticipantRepository extends JpaRepository<ApprovalParticipant, Long> {

}
