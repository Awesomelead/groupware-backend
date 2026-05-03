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
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import kr.co.awesomelead.groupware_backend.domain.approval.enums.ApprovalStatus;
import kr.co.awesomelead.groupware_backend.domain.approval.enums.ApprovalType;
import kr.co.awesomelead.groupware_backend.domain.department.entity.Department;
import kr.co.awesomelead.groupware_backend.domain.user.entity.User;
import kr.co.awesomelead.groupware_backend.global.common.entity.BaseTimeEntity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "approval_documents")
public class ApprovalDocument extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "template_id", nullable = false)
    private ApprovalTemplate template;

    @Column(nullable = false, length = 150)
    private String templateNameSnapshot;

    @Column(nullable = false, length = 80)
    private String templateCodeSnapshot;

    @Column(nullable = false, length = 200)
    private String title;

    @Lob
    @Column(nullable = false, columnDefinition = "TEXT")
    private String contentDelta;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String contentHtml;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ApprovalType approvalType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private ApprovalStatus status = ApprovalStatus.DRAFT;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "drafter_user_id", nullable = false)
    private User drafterUser;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "drafter_department_id", nullable = false)
    private Department drafterDepartment;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "receiver_department_id")
    private Department receiverDepartment;

    private LocalDateTime submittedAt;

    private LocalDateTime completedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_document_id")
    private ApprovalDocument sourceDocument;

    @Builder.Default
    @OneToMany(mappedBy = "document")
    private List<ApprovalDocumentLine> lines = new ArrayList<>();

    @Builder.Default
    @OneToMany(mappedBy = "document")
    private List<ApprovalActionHistory> actionHistories = new ArrayList<>();

    @Builder.Default
    @OneToMany(mappedBy = "document")
    private List<ApprovalAttachment> attachments = new ArrayList<>();

    @Builder.Default
    @OneToMany(mappedBy = "document")
    private List<ApprovalDocumentRead> reads = new ArrayList<>();
}
