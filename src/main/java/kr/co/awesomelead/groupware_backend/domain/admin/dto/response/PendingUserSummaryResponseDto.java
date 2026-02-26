package kr.co.awesomelead.groupware_backend.domain.admin.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import kr.co.awesomelead.groupware_backend.domain.department.enums.Company;
import kr.co.awesomelead.groupware_backend.domain.department.enums.DepartmentName;
import kr.co.awesomelead.groupware_backend.domain.user.entity.User;
import kr.co.awesomelead.groupware_backend.domain.user.enums.Authority;
import kr.co.awesomelead.groupware_backend.domain.user.enums.JobType;
import kr.co.awesomelead.groupware_backend.domain.user.enums.Position;
import kr.co.awesomelead.groupware_backend.domain.user.enums.Role;
import kr.co.awesomelead.groupware_backend.domain.user.enums.Status;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.util.List;

@Getter
@Builder
@Schema(description = "회원가입 승인 대기 사용자 요약 응답")
public class PendingUserSummaryResponseDto {

    @Schema(description = "사용자 ID", example = "21")
    private Long userId;

    @Schema(description = "한글 이름", example = "홍길동")
    private String nameKor;

    @Schema(description = "영문 이름", example = "HONG GILDONG")
    private String nameEng;

    @Schema(description = "생년월일", example = "1990-01-01")
    private LocalDate birthDate;

    @Schema(description = "국적", example = "대한민국")
    private String nationality;

    @Schema(description = "우편번호", example = "06234")
    private String zipcode;

    @Schema(description = "주소1", example = "서울특별시 강남구 테헤란로 123")
    private String address1;

    @Schema(description = "주소2", example = "어썸리드빌딩 5층")
    private String address2;

    @Schema(description = "주민등록번호(원문)", example = "9001011234567")
    private String registrationNumber;

    @Schema(description = "전화번호", example = "01012345678")
    private String phoneNumber;

    @Schema(description = "이메일", example = "hong@test.com")
    private String email;

    @Schema(description = "근무사업장")
    private Company workLocation;

    @Schema(description = "부서명")
    private DepartmentName departmentName;

    @Schema(description = "직급")
    private Position position;

    @Schema(description = "근무직종")
    private JobType jobType;

    @Schema(description = "권한부여 목록")
    private List<Authority> authorities;

    @Schema(description = "입사일", example = "2025-09-22")
    private LocalDate hireDate;

    @Schema(description = "퇴사일", example = "2026-12-31")
    private LocalDate resignationDate;

    @Schema(description = "역할")
    private Role role;

    @Schema(description = "계정 상태", example = "PENDING")
    private Status status;

    public static PendingUserSummaryResponseDto from(User user) {
        return PendingUserSummaryResponseDto.builder()
                .userId(user.getId())
                .nameKor(user.getNameKor())
                .nameEng(user.getNameEng())
                .birthDate(user.getBirthDate())
                .nationality(user.getNationality())
                .zipcode(user.getZipcode())
                .address1(user.getAddress1())
                .address2(user.getAddress2())
                .registrationNumber(user.getRegistrationNumber())
                .phoneNumber(user.getPhoneNumber())
                .email(user.getEmail())
                .workLocation(user.getWorkLocation())
                .departmentName(
                        user.getDepartment() != null ? user.getDepartment().getName() : null)
                .position(user.getPosition())
                .jobType(user.getJobType())
                .authorities(
                        user.getAuthorities() == null
                                ? List.of()
                                : user.getAuthorities().stream().toList())
                .hireDate(user.getHireDate())
                .resignationDate(user.getResignationDate())
                .role(user.getRole())
                .status(user.getStatus())
                .build();
    }
}
