package kr.co.awesomelead.groupware_backend.domain.approval.entity.document;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import java.time.LocalDateTime;
import kr.co.awesomelead.groupware_backend.domain.approval.entity.Approval;
import kr.co.awesomelead.groupware_backend.domain.approval.enums.DocumentType;

@Entity
@DiscriminatorValue("LEAVE")
public class LeaveApproval extends Approval {

    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private String leaveType; // 구분: 연차, 반차 등
    private String reason;    // 신청 사유
    private String emergencyContact; // 비상 연락처

    @Override
    public DocumentType getDocumentType() {
        return DocumentType.LEAVE;
    }
}
