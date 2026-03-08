package kr.co.awesomelead.groupware_backend.domain.requesthistory.entity;

import com.fasterxml.jackson.annotation.JsonBackReference;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import kr.co.awesomelead.groupware_backend.domain.requesthistory.enums.RequestHistoryStatus;
import kr.co.awesomelead.groupware_backend.domain.requesthistory.enums.RequestType;
import kr.co.awesomelead.groupware_backend.domain.user.entity.User;

import lombok.Getter;
import lombok.Setter;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDate;

@Entity
@Getter
@Setter
@Table(name = "request_histories")
@EntityListeners(AuditingEntityListener.class)
public class RequestHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @JsonBackReference("request-history-user")
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

    // 발급 부수
    @Column(nullable = false)
    private Integer copies;

    // 발급 희망일 (필수)
    @Column(nullable = false)
    private LocalDate wishDate;

    // 신청일 (자동 생성)
    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDate requestDate;

    // 제증명 신청 상태 (기본값: PENDING)
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RequestHistoryStatus approvalStatus = RequestHistoryStatus.PENDING;

    // 처리 관리자
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "processed_by")
    @JsonBackReference("request-history-processor")
    private User processedBy;

    // 발급 처리일
    @Column private LocalDate processedDate;
}
