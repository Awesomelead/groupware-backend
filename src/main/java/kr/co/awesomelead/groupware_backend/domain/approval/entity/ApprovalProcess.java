package kr.co.awesomelead.groupware_backend.domain.approval.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.List;
import kr.co.awesomelead.groupware_backend.domain.approval.enums.ApprovalStatus;
import kr.co.awesomelead.groupware_backend.domain.approval.enums.DocumentType;
import kr.co.awesomelead.groupware_backend.domain.user.entity.User;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
@Entity
@Table(name = "approval_process")
public class ApprovalProcess {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "requester_id", nullable = false)
    private User requester;

    // 어느 문서의 결재인지 구분 ex) "LEAVE", "MESSAGE"
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DocumentType documentType;

    // 해당 문서의 ID (LeaveRequest PK 또는 Message PK)
    @Column(nullable = false)
    private Long documentId;

    // 현재 결재 상태 (WAITING, COMPLETED, REJECTED)
    @Enumerated(EnumType.STRING)
    private ApprovalStatus status = ApprovalStatus.WAITING;

    // 현재 몇 번째 순서인지 (성능 최적화용)
    private int currentStepOrder = 1;

    // 구체적인 결재자 목록 (1:N)
    @OneToMany(mappedBy = "approvalProcess", cascade = CascadeType.ALL)
    @OrderBy("stepOrder ASC")
    private List<ApprovalStep> steps = new ArrayList<>();

}
