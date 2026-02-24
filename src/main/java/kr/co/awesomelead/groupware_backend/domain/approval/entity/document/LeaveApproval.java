package kr.co.awesomelead.groupware_backend.domain.approval.entity.document;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;

import kr.co.awesomelead.groupware_backend.domain.approval.entity.Approval;
import kr.co.awesomelead.groupware_backend.domain.approval.enums.DocumentType;
import kr.co.awesomelead.groupware_backend.domain.approval.enums.LeaveDetailType;
import kr.co.awesomelead.groupware_backend.domain.approval.enums.LeaveType;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@DiscriminatorValue("LEAVE")
@Getter
@Setter
public class LeaveApproval extends Approval {

    private LocalDateTime startDate;
    private LocalDateTime endDate;

    @Enumerated(EnumType.STRING)
    @Column(columnDefinition = "ENUM('LEAVE','HALF_OFF','EDUCATION','TRAINING','OTHER')")
    private LeaveType leaveType;

    @Enumerated(EnumType.STRING)
    @Column(
            columnDefinition =
                    "ENUM('ANNUAL','FAMILY_EVENT','MENSTRUAL','PAID','UNPAID','AM','PM')")
    private LeaveDetailType leaveDetailType;

    private String reason; // 신청 사유
    private String emergencyContact; // 비상 연락처

    @Override
    public DocumentType getDocumentType() {
        return DocumentType.LEAVE;
    }
}
