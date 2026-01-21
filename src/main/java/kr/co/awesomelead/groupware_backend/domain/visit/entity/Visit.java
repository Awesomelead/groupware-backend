package kr.co.awesomelead.groupware_backend.domain.visit.entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
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
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import kr.co.awesomelead.groupware_backend.domain.department.enums.Company;
import kr.co.awesomelead.groupware_backend.domain.user.entity.User;
import kr.co.awesomelead.groupware_backend.domain.visit.enums.AdditionalPermissionType;
import kr.co.awesomelead.groupware_backend.domain.visit.enums.VisitPurpose;
import kr.co.awesomelead.groupware_backend.domain.visit.enums.VisitStatus;
import kr.co.awesomelead.groupware_backend.domain.visit.enums.VisitType;
import kr.co.awesomelead.groupware_backend.global.encryption.PhoneNumberEncryptor;
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

    // --- 내방객 정보  ---
    @Column(nullable = false, length = 50)
    private String visitorName;

    @Convert(converter = PhoneNumberEncryptor.class)
    @Column(nullable = false, length = 300)
    private String visitorPhoneNumber;

    @Column(nullable = false, length = 64)
    private String phoneNumberHash; // 조회용 해시

    @Column(length = 50)
    private String visitorCompany;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private Company hostCompany; // 내방객 방문 회사 (내방객 직접입력 X)

    @Column(length = 20)
    private String carNumber; // 차량 번호

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private VisitPurpose purpose; // 방문 목적

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private VisitStatus status;

    // -- 방문 기간 --
    @Column(nullable = false)
    private LocalDate startDate; // 시작일

    @Column(nullable = false)
    private LocalDate endDate; // 종료일 (하루면 시작일과 동일)

    @Column
    private LocalTime plannedEntryTime; // 신청 시 입실 예정 시간

    @Column
    private LocalTime plannedExitTime;  // 신청 시 퇴실 예정 시간

    @Column(nullable = false)
    private boolean isLongTerm; // 장기 여부 (DTO 분리 시 활용)

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @Builder.Default
    private AdditionalPermissionType permissionType = AdditionalPermissionType.NONE;

    @Column(length = 200)
    private String permissionDetail;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private VisitType visitType; // 방문 유형 (사전/현장)

    @Column(nullable = false)
    private boolean visited; // 방문 여부 (하루인 경우에만 유효)

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    @JsonBackReference
    private User user; // 담당 직원

    @Column(nullable = false, length = 60) // 4자리 비밀번호 가정
    private String password;

    @Builder.Default
    @OneToMany(mappedBy = "visit", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonManagedReference
    private List<VisitRecord> records = new ArrayList<>();

    @PrePersist
    @PreUpdate
    public void onPrePersist() {
        // 전화번호 해시 생성
        if (this.visitorPhoneNumber != null && this.phoneNumberHash == null) {
            this.phoneNumberHash = hashValue(this.visitorPhoneNumber);
        }

    }

    public static String hashValue(String value) {
        if (value == null || value.isEmpty()) {
            return null;
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 알고리즘 부재", e);
        }
    }
}
