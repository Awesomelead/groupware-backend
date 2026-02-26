package kr.co.awesomelead.groupware_backend.domain.admin.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;

import jakarta.validation.constraints.NotNull;

import kr.co.awesomelead.groupware_backend.domain.department.enums.Company;
import kr.co.awesomelead.groupware_backend.domain.user.enums.Authority;
import kr.co.awesomelead.groupware_backend.domain.user.enums.JobType;
import kr.co.awesomelead.groupware_backend.domain.user.enums.Position;
import kr.co.awesomelead.groupware_backend.domain.user.enums.Role;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
public class UserApprovalRequestDto {

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

    @Schema(description = "주민등록번호/외국인등록번호", example = "9001011234567")
    private String registrationNumber;

    @Schema(description = "전화번호", example = "01099998888")
    private String phoneNumber;

    @Schema(description = "근무사업장", example = "어썸리드")
    @NotNull(message = "근무사업장은 필수 항목입니다.")
    private Company workLocation;

    @Schema(description = "부서 ID", example = "1")
    @NotNull(message = "부서 ID는 필수 항목입니다.")
    private Long departmentId;

    @Schema(description = "직급", example = "사원")
    @NotNull(message = "직급은 필수 항목입니다.")
    private Position position;

    @Schema(description = "직무 유형", example = "관리직")
    @NotNull(message = "직무 유형은 필수 항목입니다.")
    private JobType jobType;

    @Schema(description = "권한부여 목록", implementation = Authority.class)
    private List<Authority> authorities;

    @Schema(description = "입사일", example = "2025-09-22")
    @NotNull(message = "입사일은 필수 항목입니다.")
    private LocalDate hireDate;

    @Schema(description = "퇴사일", example = "2026-12-31")
    private LocalDate resignationDate;

    @Schema(description = "역할", example = "일반 사용자")
    private Role role;
}
