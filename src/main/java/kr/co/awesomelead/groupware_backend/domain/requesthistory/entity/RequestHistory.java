package kr.co.awesomelead.groupware_backend.domain.requesthistory.entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDate;
import kr.co.awesomelead.groupware_backend.domain.approval.enums.ApprovalStatus;
import kr.co.awesomelead.groupware_backend.domain.requesthistory.enums.RequestType;
import kr.co.awesomelead.groupware_backend.domain.user.entity.User;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;

@Entity
@Getter
@Setter
@Table(name = "request_histories")
public class RequestHistory {

    @Id
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @JsonBackReference
    private User user;

    // 증명서 종류 (재직/경력)
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private RequestType requestType;

    // 직급
    @Column(nullable = false, length = 20)
    private String position;

    // 이름
    @Column(nullable = false, length = 20)
    private String name;

    // 용도 (예: 은행 제출용, 관공서 제출용)
    @Column(nullable = false, length = 100)
    private String purpose;

    // 발급 희망일 (필수)
    @Column(nullable = false)
    private LocalDate wishDate;

    // 신청일 (자동 생성)
    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDate requestDate;

    // 승인 상태 (기본값: WAITING)
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ApprovalStatus approvalStatus = ApprovalStatus.WAITING;
}
