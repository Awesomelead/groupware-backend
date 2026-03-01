package kr.co.awesomelead.groupware_backend.domain.approval.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorColumn;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import kr.co.awesomelead.groupware_backend.domain.approval.enums.ApprovalStatus;
import kr.co.awesomelead.groupware_backend.domain.approval.enums.DocumentType;
import kr.co.awesomelead.groupware_backend.domain.approval.enums.RetentionPeriod;
import kr.co.awesomelead.groupware_backend.domain.department.entity.Department;
import kr.co.awesomelead.groupware_backend.domain.user.entity.User;
import kr.co.awesomelead.groupware_backend.global.common.entity.BaseTimeEntity;
import kr.co.awesomelead.groupware_backend.global.error.CustomException;
import kr.co.awesomelead.groupware_backend.global.error.ErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.BatchSize;

@Entity
@Getter
@Setter
@Inheritance(strategy = InheritanceType.JOINED) // 상속 조인 전략
@DiscriminatorColumn(name = "document_type") // 문서 구분 컬럼
@NoArgsConstructor
@AllArgsConstructor
// @Builder
@Table(name = "approvals")
public abstract class Approval extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String title; // 제목

    @Column(length = 255)
    private String documentNumber; // 문서 번호 (예: 환경안전부 20250407-754)

    @Lob
    @Column(columnDefinition = "TEXT", nullable = false)
    private String content; // 에디터 본문 (HTML 문자열 저장)

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, columnDefinition = "ENUM('WAITING','PENDING','APPROVED','REJECTED','CANCELED')")
    private ApprovalStatus status; // 상태: PENDING, APPROVED, REJECTED 등

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, columnDefinition = "ENUM('FIVE_YEARS','PERMANENT')")
    private RetentionPeriod retentionPeriod; // 보존년한 (Enum 권장)

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "drafter_id", nullable = false, updatable = false)
    private User drafter;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "department_id", nullable = false, updatable = false)
    private Department draftDepartment; // 기안 시점의 부서 (스냅샷)

    @BatchSize(size = 100)
    @OneToMany(mappedBy = "approval", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("sequence ASC")
    private List<ApprovalStep> steps = new ArrayList<>();

    @BatchSize(size = 100)
    @OneToMany(mappedBy = "approval", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ApprovalParticipant> participants = new ArrayList<>(); // 참조 및 열람자

    @BatchSize(size = 100)
    @OneToMany(mappedBy = "approval", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ApprovalAttachment> attachments = new ArrayList<>();

    @Transient
    public abstract DocumentType getDocumentType();

    public void addStep(ApprovalStep step) {
        this.steps.add(step);
        if (step.getApproval() != this) {
            step.setApproval(this);
        }
    }

    public void approve(User approver, String comment) {
        ApprovalStep myStep = findMyStep(approver);
        validateStepPending(myStep);
        validateMyTurn(myStep);

        myStep.approve(comment);

        // 다음 단계가 있으면 WAITING → PENDING 전환
        activateNextStep(myStep.getSequence());

        // 모든 step이 APPROVED이면 문서 전체 승인 처리
        boolean allApproved = steps.stream()
            .allMatch(s -> s.getStatus() == ApprovalStatus.APPROVED);
        if (allApproved) {
            this.status = ApprovalStatus.APPROVED;
        }
    }

    public void reject(User approver, String comment) {
        ApprovalStep myStep = findMyStep(approver);
        validateStepPending(myStep);
        validateMyTurn(myStep);

        myStep.reject(comment);
        this.status = ApprovalStatus.REJECTED;
    }

    private ApprovalStep findMyStep(User approver) {
        return steps.stream()
            .filter(s -> s.getApprover().getId().equals(approver.getId()))
            .findFirst()
            .orElseThrow(() -> new CustomException(ErrorCode.NOT_APPROVER));
    }

    private void validateStepPending(ApprovalStep step) {
        if (step.getStatus() != ApprovalStatus.PENDING) {
            throw new CustomException(ErrorCode.ALREADY_PROCESSED_STEP);
        }
    }

    private void validateMyTurn(ApprovalStep myStep) {
        ApprovalStep currentStep = steps.stream()
            .filter(s -> s.getStatus() == ApprovalStatus.PENDING)
            .min(Comparator.comparingInt(ApprovalStep::getSequence))
            .orElseThrow(() -> new CustomException(ErrorCode.ALREADY_PROCESSED_STEP));

        if (!currentStep.getId().equals(myStep.getId())) {
            throw new CustomException(ErrorCode.NOT_YOUR_TURN);
        }
    }

    private void activateNextStep(int approvedSequence) {
        steps.stream()
            .filter(
                s -> s.getSequence() > approvedSequence
                    && s.getStatus() == ApprovalStatus.WAITING)
            .min(Comparator.comparingInt(ApprovalStep::getSequence))
            .ifPresent(next -> next.setStatus(ApprovalStatus.PENDING));
    }

    public ApprovalStatus getDisplayStatus(Long viewerId) {
        if (this.status == ApprovalStatus.PENDING) {
            boolean isMyTurn = this.steps.stream()
                .anyMatch(
                    s -> s.getApprover().getId().equals(viewerId)
                        && s.getStatus() == ApprovalStatus.PENDING);
            if (!isMyTurn) {
                return ApprovalStatus.IN_PROGRESS;
            }
        }
        return this.status;
    }
}
