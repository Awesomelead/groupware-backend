package kr.co.awesomelead.groupware_backend.domain.user.entity;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.time.LocalDate;
import kr.co.awesomelead.groupware_backend.domain.annualleave.entity.AnnualLeave;
import kr.co.awesomelead.groupware_backend.domain.user.enums.Role;
import kr.co.awesomelead.groupware_backend.domain.user.enums.Status;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
// DB 테이블 이름을 'users'로 지정
@Table(name = "users")
public class User {


    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // == 직원이 직접 입력하는 정보 == //
    @Column(unique = true, nullable = false)
    private String email; // 이메일

    @Column(nullable = false)
    private String password; // 비밀번호

    @Column(nullable = false)
    private String nameKor; // 한글 이름

    private String nameEng; // 영문 이름

    private String nationality; // 국적

    @Column(unique = true, nullable = false)
    private String registrationNumber; // 주민등록번호 또는 외국인번호

    @Column(nullable = false)
    private String phoneNumber; // 전화번호

    // == 관리자가 입력/수정하는 정보 == //
    private LocalDate hireDate; // 입사일

    private LocalDate resignationDate; // 퇴사일

    private String jobType; // 근무 직종

    private String position; // 직급

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role; // 역할 (USER, ADMIN)

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status; // 상태 (PENDING, AVAILABLE)

    private String workLocation; // 근무지

    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonManagedReference // 👇 [규칙 2] JSON 직렬화 시 순환 참조 방지 (정방향)
    private AnnualLeave annualLeave;

}
