package kr.co.awesomelead.groupware_backend.domain.department.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import kr.co.awesomelead.groupware_backend.domain.department.entity.Department;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class DepartmentHierarchyResponseDto {

    @Schema(description = "부서 고유 ID", example = "1", required = true)
    private Long id;

    @Schema(description = "부서명", example = "경영지원부", required = true)
    private String name;

    @Schema(description = "하위 부서 목록 (재귀적 트리 구조)", required = true)
    private List<DepartmentHierarchyResponseDto> children;

    // 엔티티를 DTO로 변환하는 정적 팩토리 메서드
    public static DepartmentHierarchyResponseDto from(Department department) {
        return DepartmentHierarchyResponseDto.builder()
            .id(department.getId())
            .name(department.getName())
            .children(
                department.getChildren().stream()
                    .map(DepartmentHierarchyResponseDto::from) // 재귀 호출
                    .toList())
            .build();
    }
}
