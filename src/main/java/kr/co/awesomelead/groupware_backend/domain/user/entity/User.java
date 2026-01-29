package kr.co.awesomelead.groupware_backend.domain.user.entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.CascadeType;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.ElementCollection;
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
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import kr.co.awesomelead.groupware_backend.domain.annualleave.entity.AnnualLeave;
import kr.co.awesomelead.groupware_backend.domain.checksheet.entity.CheckSheet;
import kr.co.awesomelead.groupware_backend.domain.department.entity.Department;
import kr.co.awesomelead.groupware_backend.domain.department.enums.Company;
import kr.co.awesomelead.groupware_backend.domain.leaverequest.entity.LeaveRequest;
import kr.co.awesomelead.groupware_backend.domain.message.entity.Message;
import kr.co.awesomelead.groupware_backend.domain.payslip.entity.Payslip;
import kr.co.awesomelead.groupware_backend.domain.user.enums.Authority;
import kr.co.awesomelead.groupware_backend.domain.user.enums.JobType;
import kr.co.awesomelead.groupware_backend.domain.user.enums.Position;
import kr.co.awesomelead.groupware_backend.domain.user.enums.Role;
import kr.co.awesomelead.groupware_backend.domain.user.enums.Status;
import kr.co.awesomelead.groupware_backend.domain.visit.entity.Visit;
import kr.co.awesomelead.groupware_backend.global.encryption.PhoneNumberEncryptor;
import kr.co.awesomelead.groupware_backend.global.encryption.RegistrationNumberEncryptor;
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
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // == 직원이 직접 입력하는 정보 == //
    @Column(unique = true, nullable = false, length = 100)
    private String email; // 이메일

    @Column(nullable = false, columnDefinition = "CHAR(60)")
    private String password; // 비밀번호

    @Column(nullable = false, length = 20)
    private String nameKor; // 한글 이름

    @Column(length = 50)
    private String nameEng; // 영문 이름

    @Column(length = 30)
    private String nationality; // 국적

    @Column(unique = true, nullable = false, length = 500)
    @Convert(converter = RegistrationNumberEncryptor.class)
    private String registrationNumber; // 주민등록번호 또는 외국인번호

    @Column(nullable = false, length = 64, unique = true)
    private String registrationNumberHash; // 주민번호 SHA-256 해시 (조회용)

    @Column(nullable = false, length = 300)
    @Convert(converter = PhoneNumberEncryptor.class)
    private String phoneNumber; // 전화번호

    @Column(nullable = false, length = 64, unique = true)
    private String phoneNumberHash; // SHA-256 해시 (조회용)

    // == 관리자가 입력/수정하는 정보 == //
    private LocalDate hireDate; // 입사일

    private LocalDate resignationDate; // 퇴사일

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private JobType jobType; // 근무 직종

    @Enumerated(EnumType.STRING)
    @Column(length = 30)
    private Position position;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Role role = Role.USER; // 역할 (USER, ADMIN)

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Status status; // 상태 (PENDING, AVAILABLE)

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private Company workLocation; // 근무지

    private LocalDate birthDate; // 생년월일

    @OneToOne(
        mappedBy = "user",
        cascade = CascadeType.ALL,
        orphanRemoval = true,
        fetch = FetchType.LAZY)
    @JsonManagedReference
    private AnnualLeave annualLeave;

    @Builder.Default
    @OneToMany(mappedBy = "user", fetch = FetchType.LAZY)
    @JsonManagedReference
    private List<Visit> visits = new ArrayList<>();

    @Builder.Default
    @OneToMany(mappedBy = "user", fetch = FetchType.LAZY)
    @JsonManagedReference
    private List<CheckSheet> checkSheets = new ArrayList<>();

    @Builder.Default
    @OneToMany(mappedBy = "user", fetch = FetchType.LAZY)
    @JsonManagedReference
    private List<LeaveRequest> leaveRequests = new ArrayList<>();

    @Builder.Default
    @OneToMany(mappedBy = "user", fetch = FetchType.LAZY)
    @JsonManagedReference
    private List<Payslip> payslips = new ArrayList<>();

    @Builder.Default
    @OneToMany(mappedBy = "sender", fetch = FetchType.LAZY)
    @JsonManagedReference
    private List<Message> sentMessages = new ArrayList<>();

    @Builder.Default
    @OneToMany(mappedBy = "receiver", fetch = FetchType.LAZY)
    @JsonManagedReference
    private List<Message> receivedMessages = new ArrayList<>();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "department_id")
    @JsonBackReference
    private Department department;

    @Builder.Default
    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "user_authorities", joinColumns = @JoinColumn(name = "user_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "authority")
    private Set<Authority> authorities = new HashSet<>();

    @PrePersist
    @PreUpdate
    public void onPrePersist() {
        // 1. 전화번호 해시 생성 (평문 상태에서)
        if (this.phoneNumber != null && this.phoneNumberHash == null) {
            this.phoneNumberHash = hashValue(this.phoneNumber);
        }

        // 2. 주민등록번호 해시 생성 및 생년월일 계산
        if (this.registrationNumber != null) {
            if (this.registrationNumberHash == null) {
                this.registrationNumberHash = hashValue(this.registrationNumber);
            }
            if (this.birthDate == null) {
                this.birthDate = calculateBirthDate(this.registrationNumber);
            }
        }
    }

    // 주민등록번호 수정 및 관련 필드 갱신
    public void updateRegistrationNumber(String newRegNum) {
        if (newRegNum != null && !newRegNum.equals(this.registrationNumber)) {
            this.registrationNumber = newRegNum;
            // 1. 해시 갱신 (조회용)
            this.registrationNumberHash = hashValue(newRegNum);
            // 2. 생년월일 갱신
            this.birthDate = calculateBirthDate(newRegNum);
        }
    }

    // 관리자가 전화번호 수정 시 호출
    public void updatePhoneNumber(String newPhoneNumber) {
        if (newPhoneNumber != null && !newPhoneNumber.equals(this.phoneNumber)) {
            this.phoneNumber = newPhoneNumber;
            // 전화번호가 변경될 때 해시도 함께 변경되도록 보장
            this.phoneNumberHash = hashValue(newPhoneNumber);
        }
    }

    // 권한 추가
    public void addAuthority(Authority authority) {
        this.authorities.add(authority);
    }

    public void removeAuthority(Authority authority) {
        this.authorities.remove(authority);
    }

    // 권한 확인
    public boolean hasAuthority(Authority authority) {
        return this.authorities.contains(authority);
    }

    public String getDisplayName() {
        return (nameKor != null && !nameKor.isBlank()) ? nameKor : nameEng;
    }

    private LocalDate calculateBirthDate(String regNum) {
        // 1. 앞 6자리 추출 (YYMMDD)
        String birthPart = regNum.substring(0, 6);

        // 2. 뒤 첫 번째 자리 추출 (하이픈 제외 포함된 14자리로 가정)
        char genderDigit = regNum.charAt(6);

        // 3. 세기 판단 (그룹화)
        String century =
            switch (genderDigit) {
                case '1', '2', '5', '6' -> "19";
                case '3', '4', '7', '8' -> "20";
                case '9', '0' -> "18"; // 혹시 모를 1800년대생 대비
                default -> "20"; // 기본값
            };

        // 4. LocalDate로 변환
        return LocalDate.parse(century + birthPart, DateTimeFormatter.ofPattern("yyyyMMdd"));
    }

    // 공통 해시 로직 (기존 hashPhoneNumber를 범용적으로 변경)
    public static String hashValue(String value) {
        if (value == null || value.isEmpty()) {
            return null;
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 알고리즘을 찾을 수 없습니다.", e);
        }
    }
}
