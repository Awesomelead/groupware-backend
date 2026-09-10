package kr.co.awesomelead.groupware_backend.domain.visit.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import kr.co.awesomelead.groupware_backend.domain.department.enums.Company;
import kr.co.awesomelead.groupware_backend.domain.department.enums.DepartmentName;
import kr.co.awesomelead.groupware_backend.domain.user.entity.User;
import kr.co.awesomelead.groupware_backend.domain.user.enums.Position;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@Schema(description = "내방객 담당직원 후보 응답 DTO")
public class VisitHostCandidateResponseDto {

    @Schema(description = "직원 ID", example = "1")
    private Long userId;

    @Schema(description = "이름", example = "홍길동")
    private String name;

    @Schema(description = "직급", example = "대리")
    private Position position;

    @Schema(description = "부서명", example = "경영지원부")
    private DepartmentName departmentName;

    @Schema(description = "근무 사업장", example = "AWESOME")
    private Company workLocation;

    public static VisitHostCandidateResponseDto from(User user) {
        return VisitHostCandidateResponseDto.builder()
                .userId(user.getId())
                .name(user.getDisplayName())
                .position(user.getPosition())
                .departmentName(
                        user.getDepartment() != null ? user.getDepartment().getName() : null)
                .workLocation(user.getWorkLocation())
                .build();
    }
}
