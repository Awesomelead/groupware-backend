package kr.co.awesomelead.groupware_backend.domain.admin.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;

import jakarta.validation.constraints.NotNull;

import kr.co.awesomelead.groupware_backend.domain.department.enums.Company;
import kr.co.awesomelead.groupware_backend.domain.user.enums.JobType;
import kr.co.awesomelead.groupware_backend.domain.user.enums.Position;
import kr.co.awesomelead.groupware_backend.domain.user.enums.Role;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class UserApprovalRequestDto {

    @Schema(description = "한글 이름 (수정 필요 시)", example = "홍길동")
    private String nameKor;

    @Schema(description = "영문 이름 (수정 필요 시)", example = "HONG GILDONG")
    private String nameEng;

    @Schema(description = "국적 (수정 필요 시)", example = "대한민국")
    private String nationality;

    @Schema(description = "전화번호 (수정 필요 시)", example = "01099998888")
    private String phoneNumber;

    @NotNull(message = "근무사업장은 필수 항목입니다.")
    @Schema(description = "근무사업장", example = "어썸리드")
    private Company workLocation;

    @NotNull(message = "부서 ID는 필수 항목입니다.")
    @Schema(description = "부서 ID", example = "1")
    private Long departmentId;

    @NotNull(message = "직무 유형은 필수 항목입니다.")
    @Schema(description = "직무 유형", example = "관리직")
    private JobType jobType;

    @Schema(description = "권한", example = "일반 사용자")
    private Role role;

    @NotNull(message = "직급은 필수 항목입니다.")
    @Schema(description = "직급", example = "사원")
    private Position position;

    @NotNull(message = "입사일은 필수 항목입니다.")
    @Schema(description = "입사일", example = "2025-09-22")
    private LocalDate hireDate;
}
