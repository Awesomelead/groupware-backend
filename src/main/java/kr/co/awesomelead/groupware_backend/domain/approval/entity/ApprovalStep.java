package kr.co.awesomelead.groupware_backend.domain.approval.entity;

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
import jakarta.persistence.Table;

import kr.co.awesomelead.groupware_backend.domain.approval.enums.ApprovalStatus;
import kr.co.awesomelead.groupware_backend.domain.user.entity.User;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Table(name = "approval_steps")
public class ApprovalStep {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approval_id", nullable = false)
    private Approval approval; // 연결된 결재 문서 (결재)

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approver_id", nullable = false)
    private User approver; // 승인자

    @Column(nullable = false)
    private Integer sequence; // 단계 (1, 2, 3...)

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, columnDefinition = "ENUM('WAITING','PENDING','APPROVED','REJECTED','CANCELED')")
    private ApprovalStatus status; // 승인상태 (PENDING, APPROVED, REJECTED)

    private LocalDateTime processedAt; // 처리시간

    @Column(length = 1000)
    private String comment; // 의견 (반려 사유 등)

    public void approve(String comment) {
        this.status = ApprovalStatus.APPROVED;
        this.comment = comment;
        this.processedAt = LocalDateTime.now();
    }

    public void reject(String comment) {
        this.status = ApprovalStatus.REJECTED;
        this.comment = comment;
        this.processedAt = LocalDateTime.now();
    }
}
