package kr.co.awesomelead.groupware_backend.domain.user.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import kr.co.awesomelead.groupware_backend.domain.department.enums.DepartmentName;
import kr.co.awesomelead.groupware_backend.domain.user.entity.User;
import kr.co.awesomelead.groupware_backend.domain.user.enums.JobType;
import kr.co.awesomelead.groupware_backend.domain.user.enums.Position;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;

@Getter
@Builder
@Schema(description = "직원 요약 정보 응답 DTO")
public class UserSummaryResponseDto {

    @Schema(description = "직원 ID", example = "1")
    private Long userId;

    @Schema(description = "이름", example = "홍길동")
    private String name;

    @Schema(description = "직급", example = "대리")
    private Position position;

    @Schema(description = "부서명", example = "경영지원부")
    private DepartmentName departmentName;

    @Schema(description = "근무 직종", example = "MANAGEMENT")
    private JobType jobType;

    @Schema(description = "입사일", example = "2022-03-01")
    private LocalDate hireDate;

    @Schema(description = "퇴사일", example = "2025-12-31")
    private LocalDate resignationDate;

    public static UserSummaryResponseDto from(User user) {
        return UserSummaryResponseDto.builder()
                .userId(user.getId())
                .name(user.getDisplayName())
                .position(user.getPosition())
                .departmentName(
                        user.getDepartment() != null ? user.getDepartment().getName() : null)
                .jobType(user.getJobType())
                .hireDate(user.getHireDate())
                .resignationDate(user.getResignationDate())
                .build();
    }
}
