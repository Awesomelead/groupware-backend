package kr.co.awesomelead.groupware_backend.domain.visit.entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonManagedReference;

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

import kr.co.awesomelead.groupware_backend.domain.user.entity.User;
import kr.co.awesomelead.groupware_backend.domain.visit.enums.VisitPurpose;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

@Setter
@Getter
@Entity
public class VisitInfo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50)
    private String hostCompany; // 내방객 방문 회사

    @Column(length = 50)
    private String visitorCompany; // 내방객 소속 회사

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private VisitPurpose purpose; // 방문 목적

    @Column(length = 20)
    private String carNumber; // 차량 번호

    @Column(nullable = false)
    private LocalDateTime visitStartDate; // 방문 시작 일시

    @Column(nullable = false)
    private LocalDateTime visitEndDate; // 방문 종료 일시

    @Column(nullable = false)
    private boolean additionalRequirements = false; // 보충적허가 필요여부 (기본값 false)

    @Column(nullable = false)
    private boolean visited; // 방문 여부

    private String signatureKey; // 서명 키 (S3 key)

    @Column(nullable = false)
    private boolean agreement; // 방문자 동의 여부

    @Column(nullable = false)
    private boolean verified; // 방문자 신원 확인 여부

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    @JsonBackReference
    private User user; // 담당 직원

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "visitor_id")
    @JsonBackReference
    private Visitor visitor; // 내방객

    @OneToMany(mappedBy = "visitInfo", fetch = FetchType.LAZY, orphanRemoval = true)
    @JsonManagedReference
    private List<Companion> companions; // 동행한 방문자들
}
