package kr.co.awesomelead.groupware_backend.domain.visit.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import kr.co.awesomelead.groupware_backend.domain.department.enums.DepartmentName;
import kr.co.awesomelead.groupware_backend.domain.user.enums.Position;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
@Schema(description = "방문 담당자 정보")
public class VisitHostResponseDto {

    @Schema(description = "담당자 ID", example = "1")
    private Long userId;

    @Schema(description = "담당자 이름", example = "김철수")
    private String name;

    @Schema(description = "담당자 직급", example = "과장")
    private Position position;

    @Schema(description = "담당 부서명", example = "경영지원부")
    private DepartmentName departmentName;
}
