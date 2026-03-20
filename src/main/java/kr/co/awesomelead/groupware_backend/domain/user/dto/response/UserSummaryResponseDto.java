package kr.co.awesomelead.groupware_backend.domain.user.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import kr.co.awesomelead.groupware_backend.domain.department.enums.DepartmentName;
import kr.co.awesomelead.groupware_backend.domain.user.entity.User;
import kr.co.awesomelead.groupware_backend.domain.user.enums.Position;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@Schema(description = "직원 요약 정보 응답 DTO")
public class UserSummaryResponseDto {

    @Schema(description = "직원 ID", example = "1")
    private Long userId;

    @Schema(description = "한글 이름", example = "홍길동")
    private String nameKor;

    @Schema(description = "직급", example = "대리")
    private Position position;

    @Schema(description = "부서명", example = "경영지원부")
    private DepartmentName departmentName;

    public static UserSummaryResponseDto from(User user) {
        return UserSummaryResponseDto.builder()
                .userId(user.getId())
                .nameKor(user.getNameKor())
                .position(user.getPosition())
                .departmentName(user.getDepartment() != null ? user.getDepartment().getName() : null)
                .build();
    }
}
