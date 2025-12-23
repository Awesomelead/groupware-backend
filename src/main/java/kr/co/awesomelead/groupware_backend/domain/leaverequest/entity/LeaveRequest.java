package kr.co.awesomelead.groupware_backend.domain.leaverequest.entity;

import com.fasterxml.jackson.annotation.JsonBackReference;

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
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

import kr.co.awesomelead.groupware_backend.domain.approval.entity.ApprovalProcess;
import kr.co.awesomelead.groupware_backend.domain.approval.enums.ApprovalStatus;
import kr.co.awesomelead.groupware_backend.domain.department.entity.Department;
import kr.co.awesomelead.groupware_backend.domain.leaverequest.enums.LeaveDetail;
import kr.co.awesomelead.groupware_backend.domain.leaverequest.enums.LeaveType;
import kr.co.awesomelead.groupware_backend.domain.user.entity.User;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Setter
@Getter
@Entity
@Table(name = "leave_requests")
public class LeaveRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    @JsonBackReference
    private User user; // 신청자

    @OneToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @JoinColumn(name = "approval_process_id")
    private ApprovalProcess approvalProcess;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private LeaveType type; // 신청 유형 대분류

    @Enumerated(EnumType.STRING)
    @Column(nullable = true, length = 20)
    private LeaveDetail detail; // 신청 유형 소분류

    @Column(nullable = false, length = 200)
    private String reason; // 사유 (직접 작성)

    @Column(nullable = false, length = 20)
    private String emergencyPhoneNumber; // 비상연락망

    @Column(nullable = false, length = 20)
    private String emergencyRelation; // 비상연락관계

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ref_dept_id", nullable = true)
    private Department refDepartment; // 참조 부서

    @Column(nullable = false)
    private LocalDateTime leaveStartDate; // 근태 시작 일시

    @Column(nullable = false)
    private LocalDateTime leaveEndDate; // 근태 종료 일시

    @Column(nullable = false)
    private LocalDateTime applicationDate; // 신청 일시

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ApprovalStatus status = ApprovalStatus.WAITING;
}
