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

import kr.co.awesomelead.groupware_backend.domain.approval.enums.ApprovalTargetType;
import kr.co.awesomelead.groupware_backend.domain.department.entity.Department;
import kr.co.awesomelead.groupware_backend.domain.user.entity.User;
import kr.co.awesomelead.groupware_backend.global.common.entity.BaseTimeEntity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "approval_personal_viewer_targets")
public class ApprovalPersonalViewerTarget extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "setting_id", nullable = false)
    private ApprovalPersonalSetting setting;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ApprovalTargetType targetType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "target_user_id")
    private User targetUser;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "target_department_id")
    private Department targetDepartment;

    @Column(nullable = false, length = 150)
    private String targetNameSnapshot;

    @Column(nullable = false)
    private Integer sortOrder;
}
