package kr.co.awesomelead.groupware_backend.domain.user.entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.CascadeType;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
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
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import kr.co.awesomelead.groupware_backend.domain.annualleave.entity.AnnualLeave;
import kr.co.awesomelead.groupware_backend.domain.checksheet.entity.CheckSheet;
import kr.co.awesomelead.groupware_backend.domain.department.entity.Department;
import kr.co.awesomelead.groupware_backend.domain.leaverequest.entity.LeaveRequest;
import kr.co.awesomelead.groupware_backend.domain.message.entity.Message;
import kr.co.awesomelead.groupware_backend.domain.payslip.entity.Payslip;
import kr.co.awesomelead.groupware_backend.domain.user.enums.Authority;
import kr.co.awesomelead.groupware_backend.domain.user.enums.Role;
import kr.co.awesomelead.groupware_backend.domain.user.enums.Status;
import kr.co.awesomelead.groupware_backend.domain.visit.entity.VisitInfo;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
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

    @Column(unique = true, nullable = false, columnDefinition = "CHAR(14)")
    private String registrationNumber; // 주민등록번호 또는 외국인번호

    @Column(nullable = false, length = 15)
    private String phoneNumber; // 전화번호

    // == 관리자가 입력/수정하는 정보 == //
    private LocalDate hireDate; // 입사일

    private LocalDate resignationDate; // 퇴사일

    @Column(length = 20)
    private String jobType; // 근무 직종

    @Column(length = 20)
    private String position; // 직급

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Role role; // 역할 (USER, ADMIN)

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Status status; // 상태 (PENDING, AVAILABLE)

    @Column(length = 100)
    private String workLocation; // 근무지
    
    private LocalDate birthDate; // 생년월일

    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonManagedReference
    private AnnualLeave annualLeave;

    @OneToMany(mappedBy = "user", fetch = FetchType.LAZY)
    @JsonManagedReference
    private List<VisitInfo> visits = new ArrayList<>();

    @OneToMany(mappedBy = "user", fetch = FetchType.LAZY)
    @JsonManagedReference
    private List<CheckSheet> checkSheets = new ArrayList<>();

    @OneToMany(mappedBy = "user", fetch = FetchType.LAZY)
    @JsonManagedReference
    private List<LeaveRequest> leaveRequests = new ArrayList<>();

    @OneToMany(mappedBy = "user", fetch = FetchType.LAZY)
    @JsonManagedReference
    private List<Payslip> payslips = new ArrayList<>();

    @OneToMany(mappedBy = "sender", fetch = FetchType.LAZY)
    @JsonManagedReference
    private List<Message> sentMessages = new ArrayList<>();

    @OneToMany(mappedBy = "receiver", fetch = FetchType.LAZY)
    @JsonManagedReference
    private List<Message> receivedMessages = new ArrayList<>();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "department_id")
    @JsonBackReference
    private Department department;

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(
        name = "user_authorities",
        joinColumns = @JoinColumn(name = "user_id")
    )
    @Enumerated(EnumType.STRING)
    @Column(name = "authority")
    private Set<Authority> authorities = new HashSet<>();

    // 권한 추가
    public void addAuthority(Authority authority) {
        this.authorities.add(authority);
    }

    // 권한 확인
    public boolean hasAuthority(Authority authority) {
        return this.authorities.contains(authority);
    }
}
