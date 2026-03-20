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
@Schema(description = "직원 상세 정보 응답 DTO")
public class UserDetailResponseDto {

    @Schema(description = "이름", example = "홍길동")
    private String name;

    @Schema(description = "전화번호", example = "01012345678")
    private String phoneNumber;

    @Schema(description = "부서명", example = "경영지원부")
    private DepartmentName departmentName;

    @Schema(description = "근무 직종", example = "관리직")
    private JobType jobType;

    @Schema(description = "직급", example = "대리")
    private Position position;

    @Schema(description = "입사일", example = "2024-03-01")
    private LocalDate hireDate;

    public static UserDetailResponseDto from(User user) {
        return UserDetailResponseDto.builder()
                .name(user.getDisplayName())
                .phoneNumber(user.getPhoneNumber())
                .departmentName(
                        user.getDepartment() != null ? user.getDepartment().getName() : null)
                .jobType(user.getJobType())
                .position(user.getPosition())
                .hireDate(user.getHireDate())
                .build();
    }
}
