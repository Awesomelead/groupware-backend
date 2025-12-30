package kr.co.awesomelead.groupware_backend.domain.visit.entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonManagedReference;
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
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import kr.co.awesomelead.groupware_backend.domain.user.entity.User;
import kr.co.awesomelead.groupware_backend.domain.visit.enums.VisitPurpose;
import kr.co.awesomelead.groupware_backend.domain.visit.enums.VisitType;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@Getter
@Entity
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class Visit {

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

    private LocalDateTime visitEndDate; // 방문 종료 일시, null 가능

    @Builder.Default
    @Column(nullable = false)
    private boolean additionalRequirements = false; // 보충적허가 필요여부 (기본값 false)

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private VisitType visitType; // 방문 유형 (사전/현장)

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

    @Builder.Default
    @OneToMany(mappedBy = "visit", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonManagedReference
    private List<Companion> companions = new ArrayList<>(); // 동행한 방문자들

    // 연관관계 편의 메서드
    public void addCompanion(Companion companion) {
        this.companions.add(companion);
        companion.setVisit(this);
    }

    // 사전 예약 -> 방문 완료 처리 메서드
    public void completeVisit() {
        this.visited = true;
        this.verified = true; // 신원 확인됨
    }

    public static Visit createBaseVisit(
        User host,
        Visitor visitor,
        String hostCompany,
        String visitorCompany,
        VisitPurpose purpose,
        String carNumber,
        LocalDateTime start) {
        Visit visit = new Visit();
        visit.user = host;
        visit.visitor = visitor;
        visit.hostCompany = hostCompany;
        visit.visitorCompany = visitorCompany;
        visit.purpose = purpose;
        visit.carNumber = carNumber;
        visit.visitStartDate = start;

        // 공통 초기 상태: 아직 방문 전(visited = false)
        visit.visited = false;
        visit.verified = false;
        visit.agreement = true;
        return visit;
    }

    public void checkIn() {
        this.visited = true;
        this.verified = true; // 신원 확인됨
        this.visitStartDate = LocalDateTime.now(); // 실제 들어오는 시점의 시간을 기록
    }

    public void checkOut() {
        this.visitEndDate = LocalDateTime.now(); // 실제 나가는 시점의 시간을 기록
    }
}
