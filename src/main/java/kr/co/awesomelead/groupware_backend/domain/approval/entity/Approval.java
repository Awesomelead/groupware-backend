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
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.List;
import kr.co.awesomelead.groupware_backend.domain.approval.enums.ApprovalStatus;
import kr.co.awesomelead.groupware_backend.domain.approval.enums.RetentionPeriod;
import kr.co.awesomelead.groupware_backend.domain.department.entity.Department;
import kr.co.awesomelead.groupware_backend.domain.user.entity.User;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@Inheritance(strategy = InheritanceType.JOINED) // 상속 조인 전략
@DiscriminatorColumn(name = "approval_type")    // 문서 구분 컬럼
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Table(name = "approvals")
public class Approval {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @Column(nullable = false, length = 200)
    private String title; // 제목

    @Lob
    @Column(columnDefinition = "TEXT", nullable = false)
    private String content; // 에디터 본문 (HTML 문자열 저장)

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ApprovalStatus status; // 상태: PENDING, APPROVED, REJECTED 등

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RetentionPeriod retentionPeriod; // 보존년한 (Enum 권장)

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "drafter_id", nullable = false, updatable = false)
    private User drafter;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "department_id", nullable = false, updatable = false)
    private Department draftDepartment; // 기안 시점의 부서 (스냅샷)

    @Builder.Default
    @OneToMany(mappedBy = "approval", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ApprovalParticipant> participants = new ArrayList<>(); // 참조 및 열람자

    @Builder.Default
    @OneToMany(mappedBy = "approval", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ApprovalAttachment> attachments = new ArrayList<>();
}
