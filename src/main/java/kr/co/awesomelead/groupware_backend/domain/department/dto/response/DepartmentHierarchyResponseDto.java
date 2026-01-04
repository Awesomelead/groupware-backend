package kr.co.awesomelead.groupware_backend.domain.department.dto.response;

import kr.co.awesomelead.groupware_backend.domain.department.entity.Department;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
@AllArgsConstructor
public class DepartmentHierarchyResponseDto {

    private Long id;
    private String name;
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
