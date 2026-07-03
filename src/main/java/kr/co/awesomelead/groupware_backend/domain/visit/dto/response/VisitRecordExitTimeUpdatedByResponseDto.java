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
@Schema(description = "퇴실 시간 등록/수정자 정보")
public class VisitRecordExitTimeUpdatedByResponseDto {

    @Schema(description = "직원 ID", example = "1")
    private Long userId;

    @Schema(description = "이름", example = "김철수")
    private String name;

    @Schema(description = "부서", example = "영업부")
    private DepartmentName departmentName;

    @Schema(description = "직급", example = "대리")
    private Position position;
}
